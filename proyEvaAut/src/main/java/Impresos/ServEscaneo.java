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
public final class ServEscaneo implements AutoCloseable {
    private final int crisDocente;
    private final RepoImpresos crisRepo=new RepoImpresos();
    private final String crisSecreto;
    private HttpServer crisServer;
    private static final int crisMaxArch=8*1024*1024;
    private static final class AjusteRequerido extends IOException {
        AjusteRequerido(String crisMensaje){super(crisMensaje);}
    }
    private static double[] esquinasManual(String crisValor) throws IOException {
        if(crisValor==null||crisValor.isBlank())return null;
        String[] crisNums=crisValor.split(",",-1);
        if(crisNums.length!=8)throw new IOException("Marca los cuatro centros de alineación en la fotografía.");
        double[] crisP=new double[8];
        for(int crisI=0;crisI<8;crisI++){
            try{crisP[crisI]=Double.parseDouble(crisNums[crisI]);}catch(NumberFormatException crisE){throw new IOException("Coordenadas de alineación inválidas.");}
            if(!Double.isFinite(crisP[crisI])||crisP[crisI]<0||crisP[crisI]>1)throw new IOException("Punto de alineación fuera de la foto.");
        }
        return crisP;
    }
    public ServEscaneo(int crisDocente){
        this.crisDocente=crisDocente;byte[] crisBytes=new byte[32];new SecureRandom().nextBytes(crisBytes);
        this.crisSecreto=Base64.getUrlEncoder().withoutPadding().encodeToString(crisBytes);
    }
    public String iniciar() throws IOException {
        if(crisServer!=null)throw new IOException("Servidor ya iniciado");
        crisServer=HttpServer.create(new InetSocketAddress("0.0.0.0",8787),4);
        crisServer.createContext("/",this::manejar);
        crisServer.setExecutor(Executors.newFixedThreadPool(4,crisR->{Thread crisT=new Thread(crisR,"escaneo-movil");crisT.setDaemon(true);return crisT;}));
        crisServer.start();
        return urlConIP(ipLocal());
    }
    /** IPv4 detectadas para mostrar al docente: nunca fijar una sola interfaz en Windows. */
    public static java.util.List<String> ipsLocales(){
        LinkedHashSet<String> crisOpciones=new LinkedHashSet<>();
        // El adaptador de salida por defecto suele ser el Wi-Fi o Ethernet real.
        // Conectar un socket UDP sin enviar datos consulta la ruta del sistema operativo.
        try(DatagramSocket crisSocket=new DatagramSocket()){
            crisSocket.connect(InetAddress.getByName("1.1.1.1"),53);
            InetAddress crisIp=crisSocket.getLocalAddress();
            if(crisIp instanceof Inet4Address && crisIp.isSiteLocalAddress())crisOpciones.add(crisIp.getHostAddress());
        }catch(Exception crisIgnored){}
        try {
            Enumeration<NetworkInterface> crisInterfaces=NetworkInterface.getNetworkInterfaces();
            java.util.List<String> crisPrincipales=new ArrayList<>(), crisSecundarios=new ArrayList<>();
            while(crisInterfaces!=null&&crisInterfaces.hasMoreElements()){
                NetworkInterface crisRed=crisInterfaces.nextElement();
                if(!crisRed.isUp()||crisRed.isLoopback()||crisRed.isVirtual())continue;
                String crisNom=(crisRed.getName()+" "+crisRed.getDisplayName()).toLowerCase(Locale.ROOT);
                boolean crisAuxiliar=crisNom.matches(".*(virtual|vmware|virtualbox|vbox|wsl|hyper-v|vethernet|docker|tailscale|zerotier|vpn|tun|tap).*?");
                Enumeration<InetAddress> crisDirecciones=crisRed.getInetAddresses();
                while(crisDirecciones.hasMoreElements()){
                    InetAddress crisIp=crisDirecciones.nextElement();
                    if(crisIp instanceof Inet4Address && crisIp.isSiteLocalAddress()){
                        (crisAuxiliar?crisSecundarios:crisPrincipales).add(crisIp.getHostAddress());
                    }
                }
            }
            crisOpciones.addAll(crisPrincipales);crisOpciones.addAll(crisSecundarios);
        }catch(Exception crisIgnored){}
        return new ArrayList<>(crisOpciones);
    }
    private static String ipLocal(){
        java.util.List<String> crisIps=ipsLocales();
        return crisIps.isEmpty()?"127.0.0.1":crisIps.get(0);
    }
    /** Permite seleccionar la IP correcta sin reiniciar el servicio ni cambiar el token. */
    public String urlConIP(String crisIp){
        if(crisIp==null||!crisIp.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}"))throw new IllegalArgumentException("Escribe una direccion IPv4, por ejemplo 192.168.1.10");
        for(String crisBloque:crisIp.split("\\."))if(Integer.parseInt(crisBloque)>255)throw new IllegalArgumentException("Direccion IP no valida");
        return "http://"+crisIp+":8787/?token="+crisSecreto;
    }
    private static Map<String,String> params(String crisQuery){
        Map<String,String> crisRet=new LinkedHashMap<>();if(crisQuery==null)return crisRet;
        for(String crisItem:crisQuery.split("&")){
            int crisIx=crisItem.indexOf('=');if(crisIx<0)continue;
            try {crisRet.put(URLDecoder.decode(crisItem.substring(0,crisIx),StandardCharsets.UTF_8),URLDecoder.decode(crisItem.substring(crisIx+1),StandardCharsets.UTF_8));}
            catch(IllegalArgumentException crisIgnored){}
        }
        return crisRet;
    }
    private static String j(String crisS){
        if(crisS==null)return "null";StringBuilder crisB=new StringBuilder("\"");
        for(char crisC:crisS.toCharArray()){
            if(crisC=='"'||crisC=='\\'){crisB.append('\\').append(crisC);}else if(crisC=='\n')crisB.append("\\n");
            else if(crisC=='\r')crisB.append("\\r");else if(crisC=='\t')crisB.append("\\t");
            else if(crisC<32)crisB.append('?');else crisB.append(crisC);
        }
        return crisB.append('"').toString();
    }
    private static void respuesta(HttpExchange crisEx,int crisStatus,String crisMime,String crisContenido)throws IOException{
        byte[] crisB=crisContenido.getBytes(StandardCharsets.UTF_8);
        Headers crisH=crisEx.getResponseHeaders();crisH.set("Content-Type",crisMime+"; charset=utf-8");crisH.set("Cache-Control","no-store, max-age=0");
        crisH.set("X-Content-Type-Options","nosniff");crisH.set("Referrer-Policy","no-referrer");
        crisH.set("Content-Security-Policy","default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src 'self' blob: data:; connect-src 'self'; form-action 'self'; base-uri 'none'");
        crisEx.sendResponseHeaders(crisStatus,crisB.length);try(OutputStream crisO=crisEx.getResponseBody()){crisO.write(crisB);}
    }
    private void manejar(HttpExchange crisEx)throws IOException{
        try {
            Map<String,String> crisP=params(crisEx.getRequestURI().getRawQuery());
            if(!crisSecreto.equals(crisP.get("token"))){respuesta(crisEx,403,"application/json","{\"error\":\"Sesion no autorizada\"}");return;}
            String crisRuta=crisEx.getRequestURI().getPath();String crisMetodo=crisEx.getRequestMethod();
            if("/".equals(crisRuta)&&"GET".equals(crisMetodo)){respuesta(crisEx,200,"text/html",crisPagina);return;}
            if("/scan".equals(crisRuta)&&"POST".equals(crisMetodo)){escanear(crisEx,crisP);return;}
            if("/confirm".equals(crisRuta)&&"POST".equals(crisMetodo)){confirmar(crisEx);return;}
            respuesta(crisEx,404,"application/json","{\"error\":\"Ruta inexistente\"}");
        }catch(AjusteRequerido crisE){
            respuesta(crisEx,422,"application/json","{\"error\":"+j(crisE.getMessage())+",\"necesitaAjuste\":true}");
        }catch(Exception crisE){
            respuesta(crisEx,400,"application/json","{\"error\":"+j(crisE.getMessage()==null?"Error al procesar solicitud":crisE.getMessage())+"}");
        }finally{crisEx.close();}
    }
    private static BufferedImage abrirImagen(byte[] crisDatos)throws IOException{
        try(ImageInputStream crisIs=ImageIO.createImageInputStream(new ByteArrayInputStream(crisDatos))){
            Iterator<ImageReader> crisReaders=ImageIO.getImageReaders(crisIs);if(!crisReaders.hasNext())throw new IOException("Imagen JPEG/PNG no valida.");
            ImageReader crisRd=crisReaders.next();try {
                crisRd.setInput(crisIs,true,true);
                int crisW=crisRd.getWidth(0),crisH=crisRd.getHeight(0);
                if(crisW<450||crisH<350||crisW>6500||crisH>6500||(long)crisW*crisH>24_000_000L)throw new IOException("Tamano de fotografia no permitido.");
                return crisRd.read(0);
            }finally{crisRd.dispose();}
        }
    }
    private void escanear(HttpExchange crisEx,Map<String,String> crisP)throws Exception {
        String crisCt=crisEx.getRequestHeaders().getFirst("Content-Type");
        if(crisCt==null||!(crisCt.startsWith("image/jpeg")||crisCt.startsWith("image/png")||crisCt.startsWith("image/webp")))throw new IOException("Adjunta una imagen JPEG, PNG o WebP.");
        byte[] crisDatos=crisEx.getRequestBody().readNBytes(crisMaxArch+1);
        if(crisDatos.length>crisMaxArch)throw new IOException("Imagen mayor a 8MB.");
        BufferedImage crisImagen=abrirImagen(crisDatos);
        double[] crisAjuste=esquinasManual(crisP.get("pts"));
        String crisQr=null;
        BufferedImage crisRotar=crisImagen;
        for(int crisT=0;crisT<4;crisT++){
            if(crisT>0){BufferedImage crisCopia=new BufferedImage(crisRotar.getHeight(),crisRotar.getWidth(),BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D crisG=crisCopia.createGraphics();crisG.translate(crisCopia.getWidth(),0);crisG.rotate(Math.PI/2);crisG.drawImage(crisRotar,0,0,null);crisG.dispose();crisRotar=crisCopia;}
            crisQr=DetectorOMR.leerQR(crisRotar);if(crisQr!=null)break;
        }
        String crisCodManual=crisP.getOrDefault("codigo","").trim().toUpperCase(Locale.ROOT);
        if(!crisCodManual.isEmpty()&&!crisCodManual.matches("EV-[0-9]{1,12}-[A-Z0-9]{8}"))
            throw new IOException("Codigo manual invalido. Copia exactamente el texto EJEMPLAR: EV-... de la hoja.");
        // Si el QR es legible tiene prioridad; no confundir el codigo del examen
        // impreso con el enlace web que abre el escaner desde el celular.
        String crisOrigen="QR",crisCodBuscado=crisCodManual;
        LectorCodOCR.Resultado crisLecturaOCR=null;
        if(crisQr==null && crisCodManual.isEmpty()){
            crisLecturaOCR=LectorCodOCR.reconocer(crisImagen,crisAjuste);
            if(crisLecturaOCR.codigo()==null){
                if(crisAjuste==null)throw new AjusteRequerido(crisLecturaOCR.detalle()+" Puedes marcar los cuatro cuadrados y reintentar.");
                throw new IOException(crisLecturaOCR.detalle()+" Introduce el código EJEMPLAR para esta foto.");
            }
            crisCodBuscado=crisLecturaOCR.codigo();crisOrigen="OCR";
        }else if(crisQr==null){crisOrigen="MANUAL";}
        RepoImpresos.Identificacion crisId;
        try {crisId=crisRepo.identificar(crisQr,crisCodBuscado,crisDocente);}
        catch(java.sql.SQLException crisE){
            if("OCR".equals(crisOrigen))throw new IOException("El OCR leyo "+crisCodBuscado+", pero no corresponde a un ejemplar disponible del docente. Comprueba el codigo impreso y escribelo manualmente.");
            throw crisE;
        }
        if(!crisCodManual.isEmpty()&&!crisCodManual.equals(crisId.codigo()))
            throw new IOException("El codigo manual y el QR no corresponden al mismo ejemplar. Revisa la hoja.");
        RepoImpresos.Lote crisLote=crisRepo.cargarLote(crisId.lote(),crisDocente);
        RepoImpresos.Ejemplar crisEj=crisLote.ejemplares().stream().filter(crisX->crisX.id()==crisId.ejemplar()).findFirst().orElseThrow();
        DetectorOMR.Lectura crisLectura;
        try{crisLectura=DetectorOMR.detectar(crisImagen,crisEj.preguntas(),crisQr,crisQr==null,crisAjuste);}
        catch(IllegalArgumentException crisE){
            String crisMensaje=crisE.getMessage()==null?"":crisE.getMessage();
            if(crisMensaje.startsWith("AJUSTE:"))throw new AjusteRequerido(crisMensaje.substring(7));
            throw crisE;
        }
        Path crisFolder=PdfExamenes.BASE.resolve("escaneos");Files.createDirectories(crisFolder);
        Path crisArch=crisFolder.resolve("ficha_"+crisId.ejemplar()+"_"+UUID.randomUUID()+".jpg");
        ImageIO.write(crisImagen,"jpg",crisArch.toFile());
        String crisOcr=crisLecturaOCR==null?"":crisLecturaOCR.textoLeido();
        String crisAviso=switch(crisOrigen){
            case "OCR" -> "Identificacion automatica por OCR (el QR no se leyo). VERIFICA que el alumno y el codigo EJEMPLAR correspondan a la foto antes de guardar.";
            case "MANUAL" -> "Identificacion manual (el QR no se leyo). VERIFICA que el alumno y el codigo EJEMPLAR correspondan a la foto antes de guardar.";
            default -> "Identificacion automatica por QR. Verifica las marcas antes de guardar.";
        };
        long crisScan;
        try {crisScan=crisRepo.registrarEscan(crisId.ejemplar(),crisDocente,crisArch.toString(),crisOcr,crisAviso,crisLectura.marcas());}
        catch(Exception crisE){Files.deleteIfExists(crisArch);throw crisE;}
        RepoImpresos.Revision crisRev=crisRepo.revision(crisScan,crisDocente);
        respuesta(crisEx,200,"application/json",serializarRevision(crisRev,crisOrigen));
    }
    private static String serializarRevision(RepoImpresos.Revision crisR,String crisOrigen){
        StringBuilder crisB=new StringBuilder("{\"escaneo\":").append(crisR.escaneo()).append(",\"origen\":").append(j(crisOrigen)).append(",\"alumno\":").append(j(crisR.alumno())).append(",\"codigo\":").append(j(crisR.codigo())).append(",\"examen\":").append(j(crisR.examen())).append(",\"aviso\":").append(j(crisR.advertencia())).append(",\"respuestas\":[");
        for(int crisI=0;crisI<crisR.respuestas().size();crisI++){
            if(crisI>0)crisB.append(',');RepoImpresos.ResultadoMarca crisX=crisR.respuestas().get(crisI);
            crisB.append("{\"pregunta\":").append(crisX.pregunta()).append(",\"numero\":").append(crisX.numero()).append(",\"letra\":").append(j(crisX.confirmada()==null?"":crisX.confirmada())).append(",\"estado\":").append(j(crisX.estado())).append(",\"confianza\":").append(String.format(Locale.ROOT,"%.3f",crisX.confianza())).append("}");
        }
        return crisB.append("]}").toString();
    }
    private void confirmar(HttpExchange crisEx)throws Exception{
        String crisTipo=crisEx.getRequestHeaders().getFirst("Content-Type");if(crisTipo==null||!crisTipo.startsWith("application/x-www-form-urlencoded"))throw new IOException("Formato de confirmacion invalido.");
        byte[] crisBytes=crisEx.getRequestBody().readNBytes(100_001);if(crisBytes.length>100_000)throw new IOException("Formulario demasiado grande.");
        Map<String,String> crisArgs=params(new String(crisBytes,StandardCharsets.UTF_8));
        long crisEscaneo=Long.parseLong(crisArgs.getOrDefault("escaneo","0"));
        RepoImpresos.Revision crisRev=crisRepo.revision(crisEscaneo,crisDocente);
        Map<Long,String> crisMarcas=new LinkedHashMap<>();
        for(RepoImpresos.ResultadoMarca crisR:crisRev.respuestas()){
            String crisA=crisArgs.get("a"+crisR.pregunta());if(crisA==null)throw new IOException("Selecciona la respuesta de cada pregunta (puede quedar en blanco).");
            if(!crisA.isEmpty()&&!List.of("A","B","C","D").contains(crisA))throw new IOException("Alternativa no valida.");
            crisMarcas.put(crisR.pregunta(),crisA);
        }
        RepoImpresos.ResultadoNota crisRes=crisRepo.confirmar(crisEscaneo,crisDocente,crisMarcas);
        respuesta(crisEx,200,"application/json","{\"ok\":true,\"alumno\":"+j(crisRes.alumno())+",\"nota\":"+crisRes.nota()+",\"maximo\":"+crisRes.maximo()+",\"correctas\":"+crisRes.correctas()+",\"total\":"+crisRes.total()+"}");
    }
    @Override public void close(){if(crisServer!=null){crisServer.stop(0);crisServer=null;}}
    private static final String crisPagina="""
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
