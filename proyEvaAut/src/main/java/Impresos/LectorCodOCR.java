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
public final class LectorCodOCR {
    private LectorCodOCR(){}
    public record Resultado(String codigo,String detalle,String textoLeido){}
    private static final Pattern crisCod=Pattern.compile("EV-([0-9]{1,12})-([A-Z0-9]{8})");
    private static final Pattern crisCompacto=Pattern.compile("EV([0-9]{1,12})([A-Z0-9]{8})");

    public static String normalizarCod(String crisTexto){
        if(crisTexto==null)return null;
        String crisMay=crisTexto.toUpperCase(Locale.ROOT).replaceAll("[\\u2010-\\u2015\\u2212]","-");
        Matcher crisDirecto=crisCod.matcher(crisMay.replaceAll("\\s*[-:]\\s*","-"));
        if(crisDirecto.find())return "EV-"+crisDirecto.group(1)+"-"+crisDirecto.group(2);
        String crisLimpio=crisMay.replaceAll("[^A-Z0-9]","");
        Matcher crisM=crisCompacto.matcher(crisLimpio);
        if(crisM.find())return "EV-"+crisM.group(1)+"-"+crisM.group(2);
        return null;
    }
    private static String ejecutable(){
        String crisConfiguracion=System.getProperty("evaluacion.tesseract");
        if(crisConfiguracion==null||crisConfiguracion.isBlank())crisConfiguracion=System.getenv("TESSERACT_CMD");
        if(crisConfiguracion!=null&&!crisConfiguracion.isBlank())return crisConfiguracion;
        Path crisWindows=Path.of("C:/Program Files/Tesseract-OCR/tesseract.exe");
        if(Files.isRegularFile(crisWindows))return crisWindows.toString();
        return "tesseract";
    }
    private static String ejecutar(BufferedImage crisRecorte,int crisPsm) throws Exception {
        Path crisTmp=null;
        try{
            crisTmp=Files.createTempFile("evaluacion_ocr_codigo_", ".png");
            if(!ImageIO.write(crisRecorte,"png",crisTmp.toFile()))throw new IOException("PNG no disponible");
            ProcessBuilder crisPb=new ProcessBuilder(ejecutable(),crisTmp.toString(),"stdout","--psm",Integer.toString(crisPsm),"-l","eng");
            crisPb.redirectErrorStream(true);
            Process crisProceso=crisPb.start();
            if(!crisProceso.waitFor(7,TimeUnit.SECONDS)){
                crisProceso.destroyForcibly();throw new IOException("Tesseract no respondió a tiempo.");
            }
            return new String(crisProceso.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();
        }finally{if(crisTmp!=null)try{Files.deleteIfExists(crisTmp);}catch(IOException crisIgnored){}}
    }
    private static BufferedImage girar180(BufferedImage crisIm){
        BufferedImage crisNueva=new BufferedImage(crisIm.getWidth(),crisIm.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D crisGg=crisNueva.createGraphics();crisGg.rotate(Math.PI,crisIm.getWidth()/2.0,crisIm.getHeight()/2.0);
        crisGg.drawImage(crisIm,0,0,null);crisGg.dispose();return crisNueva;
    }
    private static BufferedImage cabeceraSinMarcas(BufferedImage crisOriginal,boolean crisGirada){
        BufferedImage crisIm=crisGirada?girar180(crisOriginal):crisOriginal;
        // OCR de emergencia: lee las primeras lineas aunque no localice fiduciales.
        // Se limita el ancho para que Tesseract no procese una foto enorme.
        double crisRatio=Math.min(1.0,1700.0/Math.max(crisIm.getWidth(),crisIm.getHeight()));
        int crisAncho=Math.max(1,(int)(crisIm.getWidth()*crisRatio));
        int crisAlto=Math.max(1,(int)(crisIm.getHeight()*.46*crisRatio));
        BufferedImage crisResult=new BufferedImage(crisAncho,crisAlto,BufferedImage.TYPE_INT_RGB);
        Graphics2D crisG=crisResult.createGraphics();
        crisG.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        crisG.drawImage(crisIm,0,0,crisAncho,crisAlto,0,0,crisIm.getWidth(),(int)(crisIm.getHeight()*.46),null);
        crisG.dispose();return crisResult;
    }
    public static Resultado reconocer(BufferedImage crisFoto){return reconocer(crisFoto,null);}
    public static Resultado reconocer(BufferedImage crisFoto,double[] crisPuntos){
        String crisUltimo="";
        try{
            BufferedImage crisZona=DetectorOMR.recortarCodEjemp(crisFoto,crisPuntos);
            crisUltimo=ejecutar(crisZona,7);
            String crisCod=normalizarCod(crisUltimo);
            if(crisCod!=null)return new Resultado(crisCod,"Código identificado con OCR en la casilla EJEMPLAR.",crisUltimo);
        }catch(Exception crisIgnored){}
        // Independiente de los 4 cuadrados. Cuando el usuario marca esquinas
        // pero el texto está muy borroso, puede escribir el código manual.
        for(boolean crisGiro:new boolean[]{false,true}){
            try{
                String crisTexto=ejecutar(cabeceraSinMarcas(crisFoto,crisGiro),6);
                String crisCod=normalizarCod(crisTexto);
                if(crisCod!=null)return new Resultado(crisCod,"Código reconocido leyendo la cabecera sin marcas.",crisTexto);
                if(!crisTexto.isBlank())crisUltimo=crisTexto;
            }catch(Exception crisIgnored){}
        }
        return new Resultado(null,"El OCR no pudo leer el código EJEMPLAR. Ajusta las cuatro marcas o escribe el código de la hoja.",crisUltimo);
    }
}
