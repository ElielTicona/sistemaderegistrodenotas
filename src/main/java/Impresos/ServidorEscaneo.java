package Impresos;

import com.sun.net.httpserver.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;

/** Servicio movil local con sesion efimera; no expone MariaDB al telefono. */
public final class ServidorEscaneo implements AutoCloseable {
    private final int docente;
    private final RepositorioImpresos repo=new RepositorioImpresos();
    private final String secreto;
    private HttpServer server;
    private static final int MAX_ARCHIVO=8*1024*1024;
    private static final class AjusteRequerido extends IOException {
        AjusteRequerido(String mensaje){super(mensaje);}
    }
    private static double[] esquinasManuales(String valor) throws IOException {
        if(valor==null||valor.isBlank())return null;
        String[] nums=valor.split(",",-1);
        if(nums.length!=8)throw new IOException("Marca los cuatro centros de alineación en la fotografía.");
        double[] p=new double[8];
        for(int i=0;i<8;i++){
            try{p[i]=Double.parseDouble(nums[i]);}catch(NumberFormatException e){throw new IOException("Coordenadas de alineación inválidas.");}
            if(!Double.isFinite(p[i])||p[i]<0||p[i]>1)throw new IOException("Punto de alineación fuera de la foto.");
        }
        return p;
    }
    public ServidorEscaneo(int docente){
        this.docente=docente;byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
        this.secreto=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public String iniciar() throws IOException {
        if(server!=null)throw new IOException("Servidor ya iniciado");
        server=HttpServer.create(new InetSocketAddress("0.0.0.0",8787),4);
        server.createContext("/",this::manejar);
        server.setExecutor(Executors.newFixedThreadPool(4,r->{Thread t=new Thread(r,"escaneo-movil");t.setDaemon(true);return t;}));
        server.start();
        return urlConIP(ipLocal());
    }
    /** IPv4 detectadas para mostrar al docente: nunca fijar una sola interfaz en Windows. */
    public static java.util.List<String> ipsLocales(){
        LinkedHashSet<String> opciones=new LinkedHashSet<>();
        // El adaptador de salida por defecto suele ser el Wi-Fi o Ethernet real.
        // Conectar un socket UDP sin enviar datos consulta la ruta del sistema operativo.
        try(DatagramSocket socket=new DatagramSocket()){
            socket.connect(InetAddress.getByName("1.1.1.1"),53);
            InetAddress ip=socket.getLocalAddress();
            if(ip instanceof Inet4Address && ip.isSiteLocalAddress())opciones.add(ip.getHostAddress());
        }catch(Exception ignored){}
        try {
            Enumeration<NetworkInterface> interfaces=NetworkInterface.getNetworkInterfaces();
            java.util.List<String> principales=new ArrayList<>(), secundarios=new ArrayList<>();
            while(interfaces!=null&&interfaces.hasMoreElements()){
                NetworkInterface red=interfaces.nextElement();
                if(!red.isUp()||red.isLoopback()||red.isVirtual())continue;
                String nombre=(red.getName()+" "+red.getDisplayName()).toLowerCase(Locale.ROOT);
                boolean auxiliar=nombre.matches(".*(virtual|vmware|virtualbox|vbox|wsl|hyper-v|vethernet|docker|tailscale|zerotier|vpn|tun|tap).*?");
                Enumeration<InetAddress> direcciones=red.getInetAddresses();
                while(direcciones.hasMoreElements()){
                    InetAddress ip=direcciones.nextElement();
                    if(ip instanceof Inet4Address && ip.isSiteLocalAddress()){
                        (auxiliar?secundarios:principales).add(ip.getHostAddress());
                    }
                }
            }
            opciones.addAll(principales);opciones.addAll(secundarios);
        }catch(Exception ignored){}
        return new ArrayList<>(opciones);
    }
    private static String ipLocal(){
        java.util.List<String> ips=ipsLocales();
        return ips.isEmpty()?"127.0.0.1":ips.get(0);
    }
    /** Permite seleccionar la IP correcta sin reiniciar el servicio ni cambiar el token. */
    public String urlConIP(String ip){
        if(ip==null||!ip.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}"))throw new IllegalArgumentException("Escribe una direccion IPv4, por ejemplo 192.168.1.10");
        for(String bloque:ip.split("\\."))if(Integer.parseInt(bloque)>255)throw new IllegalArgumentException("Direccion IP no valida");
        return "http://"+ip+":8787/?token="+secreto;
    }
    private static Map<String,String> params(String query){
        Map<String,String> ret=new LinkedHashMap<>();if(query==null)return ret;
        for(String item:query.split("&")){
            int ix=item.indexOf('=');if(ix<0)continue;
            try {ret.put(URLDecoder.decode(item.substring(0,ix),StandardCharsets.UTF_8),URLDecoder.decode(item.substring(ix+1),StandardCharsets.UTF_8));}
            catch(IllegalArgumentException ignored){}
        }
        return ret;
    }
    private static String j(String s){
        if(s==null)return "null";StringBuilder b=new StringBuilder("\"");
        for(char c:s.toCharArray()){
            if(c=='"'||c=='\\'){b.append('\\').append(c);}else if(c=='\n')b.append("\\n");
            else if(c=='\r')b.append("\\r");else if(c=='\t')b.append("\\t");
            else if(c<32)b.append('?');else b.append(c);
        }
        return b.append('"').toString();
    }
    private static void respuesta(HttpExchange ex,int status,String mime,String contenido)throws IOException{
        byte[] b=contenido.getBytes(StandardCharsets.UTF_8);
        Headers h=ex.getResponseHeaders();h.set("Content-Type",mime+"; charset=utf-8");h.set("Cache-Control","no-store, max-age=0");
        h.set("X-Content-Type-Options","nosniff");h.set("Referrer-Policy","no-referrer");
        h.set("Content-Security-Policy","default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src 'self' blob: data:; connect-src 'self'; form-action 'self'; base-uri 'none'");
        ex.sendResponseHeaders(status,b.length);try(OutputStream o=ex.getResponseBody()){o.write(b);}
    }
    private void manejar(HttpExchange ex)throws IOException{
        try {
            Map<String,String> p=params(ex.getRequestURI().getRawQuery());
            if(!secreto.equals(p.get("token"))){respuesta(ex,403,"application/json","{\"error\":\"Sesion no autorizada\"}");return;}
            String ruta=ex.getRequestURI().getPath();String metodo=ex.getRequestMethod();
            if("/".equals(ruta)&&"GET".equals(metodo)){respuesta(ex,200,"text/html",PAGINA);return;}
            if("/scan".equals(ruta)&&"POST".equals(metodo)){escanear(ex,p);return;}
            if("/confirm".equals(ruta)&&"POST".equals(metodo)){confirmar(ex);return;}
            respuesta(ex,404,"application/json","{\"error\":\"Ruta inexistente\"}");
        }catch(AjusteRequerido e){
            respuesta(ex,422,"application/json","{\"error\":"+j(e.getMessage())+",\"necesitaAjuste\":true}");
        }catch(Exception e){
            respuesta(ex,400,"application/json","{\"error\":"+j(e.getMessage()==null?"Error al procesar solicitud":e.getMessage())+"}");
        }finally{ex.close();}
    }
    private static BufferedImage abrirImagen(byte[] datos)throws IOException{
        try(ImageInputStream is=ImageIO.createImageInputStream(new ByteArrayInputStream(datos))){
            Iterator<ImageReader> readers=ImageIO.getImageReaders(is);if(!readers.hasNext())throw new IOException("Imagen JPEG/PNG no valida.");
            ImageReader rd=readers.next();try {
                rd.setInput(is,true,true);
                int w=rd.getWidth(0),h=rd.getHeight(0);
                if(w<450||h<350||w>6500||h>6500||(long)w*h>24_000_000L)throw new IOException("Tamano de fotografia no permitido.");
                return rd.read(0);
            }finally{rd.dispose();}
        }
    }
    private void escanear(HttpExchange ex,Map<String,String> p)throws Exception {
        String ct=ex.getRequestHeaders().getFirst("Content-Type");
        if(ct==null||!(ct.startsWith("image/jpeg")||ct.startsWith("image/png")||ct.startsWith("image/webp")))throw new IOException("Adjunta una imagen JPEG, PNG o WebP.");
        byte[] datos=ex.getRequestBody().readNBytes(MAX_ARCHIVO+1);
        if(datos.length>MAX_ARCHIVO)throw new IOException("Imagen mayor a 8MB.");
        BufferedImage imagen=abrirImagen(datos);
        double[] ajuste=esquinasManuales(p.get("pts"));
        String qr=null;
        BufferedImage rotar=imagen;
        for(int t=0;t<4;t++){
            if(t>0){BufferedImage copia=new BufferedImage(rotar.getHeight(),rotar.getWidth(),BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=copia.createGraphics();g.translate(copia.getWidth(),0);g.rotate(Math.PI/2);g.drawImage(rotar,0,0,null);g.dispose();rotar=copia;}
            qr=DetectorOMR.leerQR(rotar);if(qr!=null)break;
        }
        String codigoManual=p.getOrDefault("codigo","").trim().toUpperCase(Locale.ROOT);
        if(!codigoManual.isEmpty()&&!codigoManual.matches("EV-[0-9]{1,12}-[A-Z0-9]{8}"))
            throw new IOException("Codigo manual invalido. Copia exactamente el texto EJEMPLAR: EV-... de la hoja.");
        // Si el QR es legible tiene prioridad; no confundir el codigo del examen
        // impreso con el enlace web que abre el escaner desde el celular.
        String origen="QR",codigoBuscado=codigoManual;
        ReconocedorCodigoOCR.Resultado lecturaOCR=null;
        if(qr==null && codigoManual.isEmpty()){
            lecturaOCR=ReconocedorCodigoOCR.reconocer(imagen,ajuste);
            if(lecturaOCR.codigo()==null){
                if(ajuste==null)throw new AjusteRequerido(lecturaOCR.detalle()+" Puedes marcar los cuatro cuadrados y reintentar.");
                throw new IOException(lecturaOCR.detalle()+" Introduce el código EJEMPLAR para esta foto.");
            }
            codigoBuscado=lecturaOCR.codigo();origen="OCR";
        }else if(qr==null){origen="MANUAL";}
        RepositorioImpresos.Identificacion id;
        try {id=repo.identificar(qr,codigoBuscado,docente);}
        catch(java.sql.SQLException e){
            if("OCR".equals(origen))throw new IOException("El OCR leyo "+codigoBuscado+", pero no corresponde a un ejemplar disponible del docente. Comprueba el codigo impreso y escribelo manualmente.");
            throw e;
        }
        if(!codigoManual.isEmpty()&&!codigoManual.equals(id.codigo()))
            throw new IOException("El codigo manual y el QR no corresponden al mismo ejemplar. Revisa la hoja.");
        RepositorioImpresos.Lote lote=repo.cargarLote(id.lote(),docente);
        RepositorioImpresos.Ejemplar ej=lote.ejemplares().stream().filter(x->x.id()==id.ejemplar()).findFirst().orElseThrow();
        DetectorOMR.Lectura lectura;
        try{lectura=DetectorOMR.detectar(imagen,ej.preguntas(),qr,qr==null,ajuste);}
        catch(IllegalArgumentException e){
            String mensaje=e.getMessage()==null?"":e.getMessage();
            if(mensaje.startsWith("AJUSTE:"))throw new AjusteRequerido(mensaje.substring(7));
            throw e;
        }
        Path folder=PdfExamenes.BASE.resolve("escaneos");Files.createDirectories(folder);
        Path archivo=folder.resolve("ficha_"+id.ejemplar()+"_"+UUID.randomUUID()+".jpg");
        ImageIO.write(imagen,"jpg",archivo.toFile());
        String ocr=lecturaOCR==null?"":lecturaOCR.textoLeido();
        String aviso=switch(origen){
            case "OCR" -> "Identificacion automatica por OCR (el QR no se leyo). VERIFICA que el alumno y el codigo EJEMPLAR correspondan a la foto antes de guardar.";
            case "MANUAL" -> "Identificacion manual (el QR no se leyo). VERIFICA que el alumno y el codigo EJEMPLAR correspondan a la foto antes de guardar.";
            default -> "Identificacion automatica por QR. Verifica las marcas antes de guardar.";
        };
        long scan;
        try {scan=repo.registrarEscaneo(id.ejemplar(),docente,archivo.toString(),ocr,aviso,lectura.marcas());}
        catch(Exception e){Files.deleteIfExists(archivo);throw e;}
        RepositorioImpresos.Revision rev=repo.revision(scan,docente);
        respuesta(ex,200,"application/json",serializarRevision(rev,origen));
    }
    private static String serializarRevision(RepositorioImpresos.Revision r,String origen){
        StringBuilder b=new StringBuilder("{\"escaneo\":").append(r.escaneo()).append(",\"origen\":").append(j(origen)).append(",\"alumno\":").append(j(r.alumno())).append(",\"codigo\":").append(j(r.codigo())).append(",\"examen\":").append(j(r.examen())).append(",\"aviso\":").append(j(r.advertencia())).append(",\"respuestas\":[");
        for(int i=0;i<r.respuestas().size();i++){
            if(i>0)b.append(',');RepositorioImpresos.ResultadoMarca x=r.respuestas().get(i);
            b.append("{\"pregunta\":").append(x.pregunta()).append(",\"numero\":").append(x.numero()).append(",\"letra\":").append(j(x.confirmada()==null?"":x.confirmada())).append(",\"estado\":").append(j(x.estado())).append(",\"confianza\":").append(String.format(Locale.ROOT,"%.3f",x.confianza())).append("}");
        }
        return b.append("]}").toString();
    }
    private void confirmar(HttpExchange ex)throws Exception{
        String tipo=ex.getRequestHeaders().getFirst("Content-Type");if(tipo==null||!tipo.startsWith("application/x-www-form-urlencoded"))throw new IOException("Formato de confirmacion invalido.");
        byte[] bytes=ex.getRequestBody().readNBytes(100_001);if(bytes.length>100_000)throw new IOException("Formulario demasiado grande.");
        Map<String,String> args=params(new String(bytes,StandardCharsets.UTF_8));
        long escaneo=Long.parseLong(args.getOrDefault("escaneo","0"));
        RepositorioImpresos.Revision rev=repo.revision(escaneo,docente);
        Map<Long,String> marcas=new LinkedHashMap<>();
        for(RepositorioImpresos.ResultadoMarca r:rev.respuestas()){
            String a=args.get("a"+r.pregunta());if(a==null)throw new IOException("Selecciona la respuesta de cada pregunta (puede quedar en blanco).");
            if(!a.isEmpty()&&!List.of("A","B","C","D").contains(a))throw new IOException("Alternativa no valida.");
            marcas.put(r.pregunta(),a);
        }
        RepositorioImpresos.ResultadoNota res=repo.confirmar(escaneo,docente,marcas);
        respuesta(ex,200,"application/json","{\"ok\":true,\"alumno\":"+j(res.alumno())+",\"nota\":"+res.nota()+",\"maximo\":"+res.maximo()+",\"correctas\":"+res.correctas()+",\"total\":"+res.total()+"}");
    }
    @Override public void close(){if(server!=null){server.stop(0);server=null;}}
    private static final String PAGINA="""
<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Escáner de exámenes</title>
<style>
:root{color-scheme:light;font-family:system-ui,Arial,sans-serif;color:#20352e;background:#f3f6f5}*{box-sizing:border-box}
body{margin:0}main{max-width:600px;margin:0 auto;padding:17px 14px 45px}h1{font-size:24px;letter-spacing:-.4px;margin:9px 0}
h2{font-size:19px;margin:0 0 12px}.sub{color:#64776d;font-size:14px;line-height:1.48}
.card{background:#fff;border:1px solid #dce5df;border-radius:15px;padding:17px;margin:15px 0;box-shadow:0 3px 16px #10291c0e}
header{display:flex;align-items:center;justify-content:space-between;gap:10px}.pill{font-size:12px;background:#e0f1e8;color:#176345;border-radius:30px;padding:6px 9px;font-weight:700}
label{display:block;font-weight:650;font-size:14px;margin:16px 0 7px}
input,select{width:100%;padding:12px;border:1px solid #aebfb5;border-radius:9px;font-size:16px;background:#fff}
button{cursor:pointer;width:100%;border:0;border-radius:10px;background:#167053;color:#fff;font-size:16px;font-weight:700;padding:14px;margin-top:12px}
button.secondary{background:#eef3f0;color:#244f3e;border:1px solid #c5d9cd}button:disabled,input:disabled{opacity:.55;cursor:not-allowed}
#preview{width:100%;max-height:245px;object-fit:contain;background:#e5ece7;border-radius:10px;margin-top:12px}
#lienzo{width:100%;height:auto;border-radius:8px;touch-action:none;display:block;background:#dfe6e2;cursor:crosshair}
#ajustePanel{border:1px solid #bed4c7;background:#f5faf7;border-radius:12px;padding:12px;margin-top:14px}
#ajustePanel[hidden]{display:none}.ajusteAyuda{font-size:14px;line-height:1.5;color:#315b46}
#fotoRevision{width:100%;max-height:340px;object-fit:contain;background:#ecf0ed;border-radius:8px}
#ajustePanel .mini{font-size:13px;padding:10px;background:#e6efe9;color:#214a38}
.countWarn{color:#9a4b12;font-weight:700}
.row{display:flex;align-items:center;justify-content:space-between;gap:10px;border-bottom:1px solid #e8eee9;padding:10px 0}
.row select{width:132px}.ok{color:#117648}.warn{color:#a05c08}.err{color:#b72a2a}
.status{padding:11px;border-radius:9px;background:#f4f7f5;margin-top:12px;min-height:39px}
.panel{background:#f2f7f4;border-radius:10px;padding:12px;margin-bottom:10px}.identity{font-size:18px;font-weight:750}
small{color:#64776d}.history{padding-left:20px;font-size:14px}.history li{padding:4px 0}
</style></head><body><main>
<header><h1>Escanear exámenes</h1><span class="pill">Modo continuo</span></header>
<p class="sub">Fotografía <strong>una media hoja completa</strong>, con los cuatro cuadrados negros. Si falla la alineación automática, podrás marcar manualmente los centros sobre la misma foto, sin volver a tomarla.</p>
<div class="card" id="captura">
<h2>1 · Fotografiar hoja</h2>
<label for="foto">Seleccionar fotografía o abrir cámara</label>
<input type="file" id="foto" accept="image/*" capture="environment">
<img id="preview" alt="Vista previa de la hoja" hidden>
<label for="codigo">Código manual — solo si QR y OCR fallan</label>
<input id="codigo" placeholder="EV-123-XXXXXXXX" autocomplete="off" autocapitalize="characters">
<p class="sub">Es el código que aparece junto a <strong>EJEMPLAR</strong>, no el código universitario. Se borrará automáticamente al comenzar con otra hoja.</p>
<button id="leer" type="button">Analizar esta hoja</button>
<button type="button" class="secondary" id="mostrarAjuste">Ajustar los cuatro cuadrados manualmente</button>
<div id="ajustePanel" hidden>
 <h2>Ajustar alineación</h2>
 <p class="ajusteAyuda">Toca el <b>centro de cada cuadrado negro</b>, NO la esquina del papel, en este orden: <b>1. superior izquierda, 2. superior derecha, 3. inferior izquierda, 4. inferior derecha</b>. Si te equivocas, puedes arrastrar los puntos o reiniciar.</p>
 <canvas id="lienzo" aria-label="Fotografía con cuatro puntos de alineación"></canvas>
 <p id="progresoAjuste" class="status sub">Marca el primer cuadrado.</p>
 <button id="reiniciarPuntos" type="button" class="secondary mini">Reiniciar puntos</button>
 <button id="analizarAjuste" type="button" disabled>Analizar con estas cuatro esquinas</button>
</div>
<p id="estado" class="status sub" role="status" aria-live="polite">Elige la primera fotografía.</p>
</div>
<div id="revision" class="card" hidden></div>
<div id="finalizado" class="card" hidden></div>
<div class="card"><h2>Resumen de esta sesión</h2><p><strong id="contador">0</strong> nota(s) guardada(s) en esta sesión del navegador.</p>
<ol id="historial" class="history"></ol><p class="sub">El resumen es temporal en esta pantalla. Las notas confirmadas se guardan en la base de datos.</p></div>
</main><script>
const tok=new URLSearchParams(location.search).get('token'),$=id=>document.getElementById(id);
let previo=null,procesando=false,pendiente=false,guardados=0;
let puntos=[],modoAjuste=false,imagenAjuste=null,arrastre=-1;
const nombresPuntos=['superior izquierda','superior derecha','inferior izquierda','inferior derecha'];
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function estado(s,cls='sub'){$('estado').textContent=s;$('estado').className='status '+cls;}
function resetAjuste(){
 puntos=[];modoAjuste=false;imagenAjuste=null;arrastre=-1;
 $('ajustePanel').hidden=true;$('analizarAjuste').disabled=true;
 $('progresoAjuste').textContent='Marca el primer cuadrado.';
}
function limpiarFoto(){
 resetAjuste();
 $('foto').value='';$('codigo').value='';$('preview').hidden=true;
 if(previo){URL.revokeObjectURL(previo);previo=null;}
 $('preview').removeAttribute('src');
}
function habilitarCaptura(habilitar){$('foto').disabled=!habilitar;$('codigo').disabled=!habilitar;$('leer').disabled=!habilitar||procesando;}
function nuevaHoja(abrirCamara){
 pendiente=false;procesando=false;limpiarFoto();$('revision').hidden=true;$('revision').innerHTML='';
 $('finalizado').hidden=true;$('finalizado').innerHTML='';habilitarCaptura(true);
 estado('Lista para la siguiente hoja. No hay un código anterior cargado.','ok');
 if(abrirCamara)$('foto').click();
}
$('foto').addEventListener('change',()=>{
 if(pendiente)return;
 $('codigo').value=''; // Nunca heredar el codigo manual de la foto anterior.
 resetAjuste();
 if(previo){URL.revokeObjectURL(previo);previo=null;}
 $('finalizado').hidden=true;const f=$('foto').files[0];if(f){previo=URL.createObjectURL(f);$('preview').src=previo;$('preview').hidden=false;estado('Foto seleccionada. Pulsa Analizar esta hoja.');}
});
function dibujarAjuste(){
 const canvas=$('lienzo');if(!imagenAjuste)return;
 const max=1100,scale=Math.min(1,max/Math.max(imagenAjuste.naturalWidth,imagenAjuste.naturalHeight));
 canvas.width=Math.max(1,Math.round(imagenAjuste.naturalWidth*scale));
 canvas.height=Math.max(1,Math.round(imagenAjuste.naturalHeight*scale));
 const ctx=canvas.getContext('2d');ctx.drawImage(imagenAjuste,0,0,canvas.width,canvas.height);
 ctx.lineWidth=Math.max(2,canvas.width/320);ctx.font='bold '+Math.max(18,canvas.width/35)+'px system-ui';
 for(let i=0;i<puntos.length;i++){
  const p=puntos[i],x=p[0]*canvas.width,y=p[1]*canvas.height;
  ctx.beginPath();ctx.arc(x,y,Math.max(9,canvas.width/75),0,Math.PI*2);
  ctx.fillStyle='#ffdd38';ctx.fill();ctx.strokeStyle='#113e36';ctx.stroke();
  ctx.strokeStyle='#113e36';ctx.fillStyle='#113e36';ctx.fillText(String(i+1),x+13,y-10);
 }
 if(puntos.length===4){ctx.strokeStyle='#ffcc30';ctx.lineWidth=Math.max(2,canvas.width/320);ctx.beginPath();
  for(const i of [0,1,3,2,0]){const pp=puntos[i];ctx.lineTo(pp[0]*canvas.width,pp[1]*canvas.height);}ctx.stroke();}
 $('progresoAjuste').textContent=puntos.length<4?'Marca el centro del cuadrado '+(puntos.length+1)+' ('+nombresPuntos[puntos.length]+').':'Cuatro puntos marcados. Puedes arrastrarlos para corregirlos y analizar la hoja.';
 $('analizarAjuste').disabled=puntos.length!==4||procesando;
}
async function mostrarAjuste(){
 const file=$('foto').files[0];if(!file){estado('Selecciona primero la foto de la hoja.','err');return;}
 $('ajustePanel').hidden=false;modoAjuste=true;
 if(!imagenAjuste){imagenAjuste=new Image();
  imagenAjuste.onload=()=>{dibujarAjuste();$('ajustePanel').scrollIntoView({block:'start',behavior:'smooth'});};
  imagenAjuste.src=previo;}
 else {dibujarAjuste();$('ajustePanel').scrollIntoView({block:'start',behavior:'smooth'});}
}
$('mostrarAjuste').onclick=mostrarAjuste;
$('reiniciarPuntos').onclick=()=>{puntos=[];dibujarAjuste();};
const lienzo=$('lienzo');
function canvasCoords(e){const r=lienzo.getBoundingClientRect();return [Math.max(0,Math.min(1,(e.clientX-r.left)/r.width)),Math.max(0,Math.min(1,(e.clientY-r.top)/r.height))];}
lienzo.addEventListener('pointerdown',e=>{
 if(procesando||!imagenAjuste)return;e.preventDefault();const xy=canvasCoords(e);
 arrastre=-1;
 if(puntos.length===4){let mejor=.045;for(let i=0;i<4;i++){
  const d=Math.hypot((puntos[i][0]-xy[0])*1.2,puntos[i][1]-xy[1]);if(d<mejor){mejor=d;arrastre=i;}
 }}
 if(arrastre>=0){lienzo.setPointerCapture(e.pointerId);puntos[arrastre]=xy;}
 else if(puntos.length<4){puntos.push(xy);}
 dibujarAjuste();
});
lienzo.addEventListener('pointermove',e=>{if(arrastre<0||!lienzo.hasPointerCapture(e.pointerId))return;e.preventDefault();puntos[arrastre]=canvasCoords(e);dibujarAjuste();});
lienzo.addEventListener('pointerup',e=>{if(arrastre>=0&&lienzo.hasPointerCapture(e.pointerId))lienzo.releasePointerCapture(e.pointerId);arrastre=-1;});
lienzo.addEventListener('pointercancel',()=>{arrastre=-1;});
$('analizarAjuste').onclick=()=>analizar(true);
async function prepararFoto(f){
 const bmp=await createImageBitmap(f,{imageOrientation:'from-image'});
 const escalar=Math.min(1,2400/Math.max(bmp.width,bmp.height));const lienzo=document.createElement('canvas');
 lienzo.width=Math.max(1,Math.round(bmp.width*escalar));lienzo.height=Math.max(1,Math.round(bmp.height*escalar));
 lienzo.getContext('2d').drawImage(bmp,0,0,lienzo.width,lienzo.height);if(bmp.close)bmp.close();
 return new Promise((resolve,reject)=>lienzo.toBlob(b=>b?resolve(b):reject(Error('Error preparando la foto')),'image/jpeg',.9));
}
$('leer').addEventListener('click',()=>analizar(false));
async function analizar(usarAjuste){
 if(procesando||pendiente)return;
 if(usarAjuste&&puntos.length!==4){estado('Marca los cuatro cuadrados primero.','err');return;}
 const f=$('foto').files[0];if(!f){estado('Selecciona o toma una fotografia primero.','err');return;}
 procesando=true;habilitarCaptura(false);estado('Identificando ejemplar (QR / OCR) y leyendo marcas...');
 try{
  const img=await prepararFoto(f);
  const manual=$('codigo').value.trim().toUpperCase();
  const ajuste=usarAjuste?'&pts='+encodeURIComponent(puntos.flat().map(v=>v.toFixed(6)).join(',')):'';
  const resp=await fetch('/scan?token='+encodeURIComponent(tok)+'&codigo='+encodeURIComponent(manual)+ajuste,{method:'POST',headers:{'Content-Type':'image/jpeg'},body:img});
  const data=await resp.json();if(!resp.ok){const err=Error(data.error||'No se pudo analizar la foto.');err.necesitaAjuste=Boolean(data.necesitaAjuste);throw err;}
  pendiente=true;estado('Hoja identificada. Verifica alumno, código y respuestas antes de guardar.','ok');
  const origen={QR:'QR de la hoja',OCR:'OCR del código impreso',MANUAL:'Código escrito por docente'}[data.origen]||'Lectura de hoja';
  let html='<h2>2 · Revisar y confirmar</h2><div class="panel"><div class="identity">'+esc(data.alumno)+'</div><div>'+esc(data.examen)+'</div><div><b>Ejemplar: '+esc(data.codigo)+'</b></div><small>Identificado por: '+esc(origen)+'</small></div>';
  if(data.aviso)html+='<p class="warn">'+esc(data.aviso)+'</p>';
  const dudas=data.respuestas.filter(r=>r.estado!=='VALIDA').length;
  html+='<p class="sub">Compara los círculos con la foto antes de guardar. <span class="countWarn">'+dudas+' respuesta(s) sin lectura válida</span>. Si hay dudas, corrige con el selector.</p>';
  html+='<details><summary>Ver fotografía original para revisar las marcas</summary><img id="fotoRevision" alt="Fotografía de la hoja escaneada"></details>';
  for(const r of data.respuestas){
   html+='<div class="row"><div><b>Pregunta '+r.numero+'</b><br><small class="'+(r.estado==='VALIDA'?'ok':'warn')+'">'+esc(r.estado)+'</small></div><select data-p="'+r.pregunta+'">';
   for(const v of ['','A','B','C','D'])html+='<option value="'+v+'"'+(r.letra===v?' selected':'')+'>'+(v||'En blanco')+'</option>';
   html+='</select></div>';
  }
  html+='<button type="button" id="confirmar">Confirmar y guardar nota</button><button type="button" class="secondary" id="descartar">Descartar lectura y repetir foto</button>';
  $('revision').innerHTML=html;$('revision').hidden=false;$('fotoRevision').src=previo;
  $('descartar').onclick=()=>nuevaHoja(true);
  $('confirmar').onclick=async()=>{
   if(!confirm('¿Confirmas que la foto corresponde a '+data.alumno+' ('+data.codigo+') y que revisaste sus marcas?'))return;
   $('confirmar').disabled=true;$('descartar').disabled=true;
   try{
    const body=new URLSearchParams();body.set('escaneo',data.escaneo);
    for(const el of document.querySelectorAll('[data-p]'))body.set('a'+el.dataset.p,el.value);
    const r=await fetch('/confirm?token='+encodeURIComponent(tok),{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body});
    const res=await r.json();if(!r.ok)throw Error(res.error||'No se pudo guardar.');
    guardados++;$('contador').textContent=String(guardados);
    const li=document.createElement('li');li.textContent=res.alumno+' — '+res.nota+' / '+res.maximo+' ('+res.correctas+' de '+res.total+' correctas)';
    $('historial').prepend(li);
    pendiente=false;limpiarFoto();$('revision').hidden=true;$('revision').innerHTML='';
    $('finalizado').innerHTML='<h2 class="ok">Nota guardada</h2><p><b>'+esc(res.alumno)+'</b> · '+res.nota+' / '+res.maximo+'</p><p>'+res.correctas+' de '+res.total+' correctas</p><button type="button" id="siguiente">Escanear siguiente hoja</button>';
    $('finalizado').hidden=false;$('siguiente').onclick=()=>nuevaHoja(true);
    estado('Nota registrada. Puedes fotografiar la siguiente hoja; el codigo manual esta vacio.','ok');habilitarCaptura(true);
    $('finalizado').scrollIntoView({behavior:'smooth',block:'start'});
   }catch(e){alert(e.message);$('confirmar').disabled=false;$('descartar').disabled=false;}
  };
  $('revision').scrollIntoView({behavior:'smooth',block:'start'});
 }catch(e){estado(e.message,'err');if(e.necesitaAjuste&&!usarAjuste)await mostrarAjuste();}
 finally{procesando=false;if(!pendiente)habilitarCaptura(true);}
}
</script></body></html>
""";
}
