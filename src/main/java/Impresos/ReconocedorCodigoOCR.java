package Impresos;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import javax.imageio.ImageIO;

/**
 * OCR auxiliar para identificar el EJEMPLAR cuando el QR no pudo leerse.
 * No adivina codigos ni alumnos: solo acepta el formato exacto EV-lote-8caracteres.
 * La existencia y propiedad del ejemplar se verifican siempre en MariaDB.
 */
public final class ReconocedorCodigoOCR {
    private ReconocedorCodigoOCR(){}
    public record Resultado(String codigo,String detalle,String textoLeido){}
    private static final Pattern CODIGO=Pattern.compile("EV-([0-9]{1,12})-([A-Z0-9]{8})");
    private static final Pattern COMPACTO=Pattern.compile("EV([0-9]{1,12})([A-Z0-9]{8})");

    public static String normalizarCodigo(String texto){
        if(texto==null)return null;
        String may=texto.toUpperCase(Locale.ROOT).replaceAll("[\\u2010-\\u2015\\u2212]","-");
        Matcher directo=CODIGO.matcher(may.replaceAll("\\s*[-:]\\s*","-"));
        if(directo.find())return "EV-"+directo.group(1)+"-"+directo.group(2);
        String limpio=may.replaceAll("[^A-Z0-9]","");
        Matcher m=COMPACTO.matcher(limpio);
        if(m.find())return "EV-"+m.group(1)+"-"+m.group(2);
        return null;
    }
    private static String ejecutable(){
        String configuracion=System.getProperty("evaluacion.tesseract");
        if(configuracion==null||configuracion.isBlank())configuracion=System.getenv("TESSERACT_CMD");
        if(configuracion!=null&&!configuracion.isBlank())return configuracion;
        Path windows=Path.of("C:/Program Files/Tesseract-OCR/tesseract.exe");
        if(Files.isRegularFile(windows))return windows.toString();
        return "tesseract";
    }
    private static String ejecutar(BufferedImage recorte,int psm) throws Exception {
        Path tmp=null;
        try{
            tmp=Files.createTempFile("evaluacion_ocr_codigo_", ".png");
            if(!ImageIO.write(recorte,"png",tmp.toFile()))throw new IOException("PNG no disponible");
            ProcessBuilder pb=new ProcessBuilder(ejecutable(),tmp.toString(),"stdout","--psm",Integer.toString(psm),"-l","eng");
            pb.redirectErrorStream(true);
            Process proceso=pb.start();
            if(!proceso.waitFor(7,TimeUnit.SECONDS)){
                proceso.destroyForcibly();throw new IOException("Tesseract no respondió a tiempo.");
            }
            return new String(proceso.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();
        }finally{if(tmp!=null)try{Files.deleteIfExists(tmp);}catch(IOException ignored){}}
    }
    private static BufferedImage girar180(BufferedImage im){
        BufferedImage nueva=new BufferedImage(im.getWidth(),im.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D gg=nueva.createGraphics();gg.rotate(Math.PI,im.getWidth()/2.0,im.getHeight()/2.0);
        gg.drawImage(im,0,0,null);gg.dispose();return nueva;
    }
    private static BufferedImage cabeceraSinMarcas(BufferedImage original,boolean girada){
        BufferedImage im=girada?girar180(original):original;
        // OCR de emergencia: lee las primeras lineas aunque no localice fiduciales.
        // Se limita el ancho para que Tesseract no procese una foto enorme.
        double ratio=Math.min(1.0,1700.0/Math.max(im.getWidth(),im.getHeight()));
        int ancho=Math.max(1,(int)(im.getWidth()*ratio));
        int alto=Math.max(1,(int)(im.getHeight()*.46*ratio));
        BufferedImage result=new BufferedImage(ancho,alto,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(im,0,0,ancho,alto,0,0,im.getWidth(),(int)(im.getHeight()*.46),null);
        g.dispose();return result;
    }
    public static Resultado reconocer(BufferedImage foto){return reconocer(foto,null);}
    public static Resultado reconocer(BufferedImage foto,double[] puntos){
        String ultimo="";
        try{
            BufferedImage zona=DetectorOMR.recortarCodigoEjemplar(foto,puntos);
            ultimo=ejecutar(zona,7);
            String codigo=normalizarCodigo(ultimo);
            if(codigo!=null)return new Resultado(codigo,"Código identificado con OCR en la casilla EJEMPLAR.",ultimo);
        }catch(Exception ignored){}
        // Independiente de los 4 cuadrados. Cuando el usuario marca esquinas
        // pero el texto está muy borroso, puede escribir el código manual.
        for(boolean giro:new boolean[]{false,true}){
            try{
                String texto=ejecutar(cabeceraSinMarcas(foto,giro),6);
                String codigo=normalizarCodigo(texto);
                if(codigo!=null)return new Resultado(codigo,"Código reconocido leyendo la cabecera sin marcas.",texto);
                if(!texto.isBlank())ultimo=texto;
            }catch(Exception ignored){}
        }
        return new Resultado(null,"El OCR no pudo leer el código EJEMPLAR. Ajusta las cuatro marcas o escribe el código de la hoja.",ultimo);
    }
}
