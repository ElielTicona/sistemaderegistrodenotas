package Impresos;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/** Lectura QR y de marcas sobre la plantilla media A4 de PdfExamenes. */
public final class DetectorOMR {
    public record Marca(long idPregunta,int numero,String letra,String estado,double confianza){}
    public record Lectura(String token,BufferedImage imagen,List<Marca> marcas,double alineacion){}
    private record Punto(double x,double y){}
    private record Candidato(double x,double y,int w,int h,int area){}
    /** Leer QR de imagen completa o recorte ampliado: una foto de la hoja no es una foto del QR. */
    public static String leerQR(BufferedImage crisB){
        String crisQr=decodQR(crisB);
        if(crisQr!=null)return crisQr;
        // Cuando la hoja se fotografio entera, la zona del QR representa pocos pixeles.
        // Aumentar un recorte conservando su quiet zone ayuda con camaras de gama baja.
        if(crisB.getWidth()>crisB.getHeight()*1.08){
            crisQr=decodRecorte(crisB,.69,.02,.99,.47);
            if(crisQr!=null)return crisQr;
        }
        return null;
    }
    private static String decodRecorte(BufferedImage crisIm,double crisX1,double crisY1,double crisX2,double crisY2){
        int crisX=(int)(crisIm.getWidth()*crisX1),crisY=(int)(crisIm.getHeight()*crisY1);
        int crisW=Math.min(crisIm.getWidth()-crisX,(int)(crisIm.getWidth()*(crisX2-crisX1)));
        int crisH=Math.min(crisIm.getHeight()-crisY,(int)(crisIm.getHeight()*(crisY2-crisY1)));
        if(crisW<40||crisH<40)return null;
        BufferedImage crisParte=crisIm.getSubimage(crisX,crisY,crisW,crisH);
        String crisQr=decodQR(crisParte);
        if(crisQr!=null)return crisQr;
        BufferedImage crisAmpliada=new BufferedImage(crisW*2,crisH*2,BufferedImage.TYPE_INT_RGB);
        Graphics2D crisG=crisAmpliada.createGraphics();
        crisG.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        crisG.drawImage(crisParte,0,0,crisW*2,crisH*2,null);crisG.dispose();
        return decodQR(crisAmpliada);
    }
    private static String decodQR(BufferedImage crisImg){
        try {
            BinaryBitmap crisBmp=new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(crisImg)));
            Map<DecodeHintType,Object> crisHints=new EnumMap<>(DecodeHintType.class);
            crisHints.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);
            crisHints.put(DecodeHintType.POSSIBLE_FORMATS,List.of(BarcodeFormat.QR_CODE));
            Result crisR=new MultiFormatReader().decode(crisBmp,crisHints);
            String crisCod=crisR.getText();
            return crisCod!=null&&crisCod.matches("EVI1:[0-9a-fA-F-]{36}|EVI2-[0-9A-Z]{20}|E3[0-9A-Z]{12}")?crisCod:null;
        } catch(Exception crisIgnored){return null;}
    }
    private static BufferedImage reducir(BufferedImage crisOriginal){
        int crisW=crisOriginal.getWidth(),crisH=crisOriginal.getHeight();double crisEscala=Math.min(1.0,1800.0/Math.max(crisW,crisH));
        if(crisEscala==1)return crisOriginal;
        int crisNw=(int)Math.round(crisW*crisEscala),crisNh=(int)Math.round(crisH*crisEscala);
        BufferedImage crisIm=new BufferedImage(crisNw,crisNh,BufferedImage.TYPE_INT_RGB);
        Graphics2D crisG=crisIm.createGraphics();crisG.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);crisG.drawImage(crisOriginal,0,0,crisNw,crisNh,null);crisG.dispose();return crisIm;
    }
    private static BufferedImage girar90(BufferedImage crisB){
        BufferedImage crisOut=new BufferedImage(crisB.getHeight(),crisB.getWidth(),BufferedImage.TYPE_INT_RGB);
        Graphics2D crisG=crisOut.createGraphics();crisG.translate(crisOut.getWidth(),0);crisG.rotate(Math.PI/2);crisG.drawImage(crisB,0,0,null);crisG.dispose();return crisOut;
    }
    private static int luminosidad(int crisRgb){return ((crisRgb>>16)&255)*299/1000+((crisRgb>>8)&255)*587/1000+(crisRgb&255)*114/1000;}
    /**
     * Umbral local: las sombras del papel no se confunden con tinta y los
     * marcadores no tienen que ser negro RGB < 85 en toda la fotografia.
     */
    private static List<Candidato> compNegros(BufferedImage crisImage){
        int crisW=crisImage.getWidth(),crisH=crisImage.getHeight(),crisLen=crisW*crisH;
        int[] crisPixels=crisImage.getRGB(0,0,crisW,crisH,null,0,crisW);
        byte[] crisMascara=new byte[crisLen];
        int[] crisGris=new int[crisLen];
        long[] crisIntegral=new long[(crisW+1)*(crisH+1)];
        for(int crisY=0;crisY<crisH;crisY++){
            long crisAcum=0;
            for(int crisX=0;crisX<crisW;crisX++){
                int crisLum=luminosidad(crisPixels[crisY*crisW+crisX]);crisGris[crisY*crisW+crisX]=crisLum;crisAcum+=crisLum;
                crisIntegral[(crisY+1)*(crisW+1)+crisX+1]=crisIntegral[crisY*(crisW+1)+crisX+1]+crisAcum;
            }
        }
        int crisRadio=Math.max(9,Math.min(crisW,crisH)/60);
        for(int crisY=0;crisY<crisH;crisY++){
            int crisY0=Math.max(0,crisY-crisRadio),crisY1=Math.min(crisH,crisY+crisRadio+1);
            for(int crisX=0;crisX<crisW;crisX++){
                int crisX0=Math.max(0,crisX-crisRadio),crisX1=Math.min(crisW,crisX+crisRadio+1);
                long crisSuma=crisIntegral[crisY1*(crisW+1)+crisX1]-crisIntegral[crisY0*(crisW+1)+crisX1]-crisIntegral[crisY1*(crisW+1)+crisX0]+crisIntegral[crisY0*(crisW+1)+crisX0];
                int crisMedia=(int)(crisSuma/((long)(crisX1-crisX0)*(crisY1-crisY0)));
                int crisV=crisGris[crisY*crisW+crisX];
                if(crisV<110 || (crisV<185 && crisV<crisMedia-29))crisMascara[crisY*crisW+crisX]=1;
            }
        }
        ArrayList<Candidato> crisAll=new ArrayList<>();int[] crisCola=new int[crisLen];
        int crisMinSize=Math.max(6,(int)(Math.min(crisW,crisH)*.008));
        int crisMaxSize=(int)(Math.min(crisW,crisH)*.12);
        for(int crisY=0;crisY<crisH;crisY++)for(int crisX=0;crisX<crisW;crisX++){
            int crisStart=crisY*crisW+crisX;if(crisMascara[crisStart]!=1)continue;
            int crisFirst=0,crisLast=0,crisArea=0,crisXmin=crisX,crisXmax=crisX,crisYmin=crisY,crisYmax=crisY;
            crisCola[crisLast++]=crisStart;crisMascara[crisStart]=2;
            while(crisFirst<crisLast){
                int crisPos=crisCola[crisFirst++],crisPy=crisPos/crisW,crisPx=crisPos%crisW;crisArea++;
                if(crisPx<crisXmin)crisXmin=crisPx;if(crisPx>crisXmax)crisXmax=crisPx;if(crisPy<crisYmin)crisYmin=crisPy;if(crisPy>crisYmax)crisYmax=crisPy;
                int crisN;
                if(crisPx>0){crisN=crisPos-1;if(crisMascara[crisN]==1){crisMascara[crisN]=2;crisCola[crisLast++]=crisN;}}
                if(crisPx+1<crisW){crisN=crisPos+1;if(crisMascara[crisN]==1){crisMascara[crisN]=2;crisCola[crisLast++]=crisN;}}
                if(crisPy>0){crisN=crisPos-crisW;if(crisMascara[crisN]==1){crisMascara[crisN]=2;crisCola[crisLast++]=crisN;}}
                if(crisPy+1<crisH){crisN=crisPos+crisW;if(crisMascara[crisN]==1){crisMascara[crisN]=2;crisCola[crisLast++]=crisN;}}
            }
            int crisCw=crisXmax-crisXmin+1,crisCh=crisYmax-crisYmin+1;
            if(crisCw<crisMinSize||crisCh<crisMinSize||crisCw>crisMaxSize||crisCh>crisMaxSize)continue;
            double crisProporcion=crisCw/(double)crisCh,crisDensidad=crisArea/(double)(crisCw*crisCh);
            if(crisProporcion>.56&&crisProporcion<1.74&&crisDensidad>.55&&crisArea>=crisMinSize*crisMinSize*.36)
                crisAll.add(new Candidato((crisXmin+crisXmax)/2.0,(crisYmin+crisYmax)/2.0,crisCw,crisCh,crisArea));
        }
        return crisAll;
    }

    private static double dist(Punto crisA,Punto crisB){return Math.hypot(crisA.x-crisB.x,crisA.y-crisB.y);}
    private static double cuadrilateroArea(Punto[] crisP){
        Punto[] crisQ={crisP[0],crisP[1],crisP[3],crisP[2]};double crisA=0;
        for(int crisI=0;crisI<4;crisI++){Punto crisU=crisQ[crisI],crisV=crisQ[(crisI+1)%4];crisA+=crisU.x*crisV.y-crisV.x*crisU.y;}
        return Math.abs(crisA)/2.0;
    }
    private static boolean geomValida(Punto[] crisP,int crisW,int crisH){
        if(crisP[0].x>=crisP[1].x||crisP[2].x>=crisP[3].x||crisP[0].y>=crisP[2].y||crisP[1].y>=crisP[3].y)return false;
        double crisSup=dist(crisP[0],crisP[1]),crisInf=dist(crisP[2],crisP[3]),crisIzq=dist(crisP[0],crisP[2]),crisDer=dist(crisP[1],crisP[3]);
        if(Math.min(crisSup,crisInf)<crisW*.54||Math.min(crisIzq,crisDer)<crisH*.50)return false;
        if(Math.max(crisSup,crisInf)/Math.min(crisSup,crisInf)>2.8||Math.max(crisIzq,crisDer)/Math.min(crisIzq,crisDer)>2.8)return false;
        return cuadrilateroArea(crisP)>(double)crisW*crisH*.15;
    }
    private static double puntaje(Punto[] crisP,Candidato[] crisC,int crisW,int crisH){
        double crisSup=dist(crisP[0],crisP[1]),crisInf=dist(crisP[2],crisP[3]),crisIzq=dist(crisP[0],crisP[2]),crisDer=dist(crisP[1],crisP[3]);
        double crisTamMin=Double.POSITIVE_INFINITY,crisTamMax=0;
        for(Candidato crisCc:crisC){double crisTam=Math.sqrt(crisCc.area);crisTamMin=Math.min(crisTamMin,crisTam);crisTamMax=Math.max(crisTamMax,crisTam);}
        if(crisTamMax/crisTamMin>3.7)return Double.POSITIVE_INFINITY;
        double crisMarco=(crisP[0].x+crisP[2].x)/crisW + (2*crisW-crisP[1].x-crisP[3].x)/crisW
            +(crisP[0].y+crisP[1].y)/crisH +(2*crisH-crisP[2].y-crisP[3].y)/crisH;
        double crisAsim=Math.abs(Math.log(crisSup/crisInf))+Math.abs(Math.log(crisIzq/crisDer));
        // El dibujo usa el mismo tamaño de marcadores; las cuatro esquinas
        // deben tener escala parecida, con margen para perspectiva.
        return crisMarco*.70 + crisAsim*.40 + Math.log(crisTamMax/crisTamMin)*.32;
    }
    private static Punto[] marcadores(BufferedImage crisImg){
        List<Candidato> crisCand=compNegros(crisImg);
        int crisW=crisImg.getWidth(),crisH=crisImg.getHeight();
        if(crisCand.size()<4)throw new IllegalArgumentException("AJUSTE:No se distinguen las cuatro marcas. Toca sus centros sobre la foto.");
        @SuppressWarnings("unchecked") List<Candidato>[] crisRegion=new List[4];
        double[][] crisObjetivos={{.06,.08},{.94,.08},{.06,.92},{.94,.92}};
        for(int crisI=0;crisI<4;crisI++){
            final double crisCx=crisObjetivos[crisI][0],crisCy=crisObjetivos[crisI][1];
            crisRegion[crisI]=new ArrayList<>();
            for(Candidato crisC:crisCand){
                double crisNx=crisC.x/crisW,crisNy=crisC.y/crisH;
                if(crisI%2==0 ? crisNx>.50 : crisNx<.50)continue;
                if(crisI<2 ? crisNy>.49 : crisNy<.51)continue;
                crisRegion[crisI].add(crisC);
            }
            crisRegion[crisI].sort(Comparator.comparingDouble(crisC->Math.hypot(crisC.x/crisW-crisCx,crisC.y/crisH-crisCy)));
            if(crisRegion[crisI].size()>9)crisRegion[crisI]=new ArrayList<>(crisRegion[crisI].subList(0,9));
            if(crisRegion[crisI].isEmpty())throw new IllegalArgumentException("AJUSTE:No se encuentran los cuatro cuadrados negros. Ajusta sus centros manualmente.");
        }
        Punto[] crisElegido=null;double crisMejor=Double.POSITIVE_INFINITY;
        for(Candidato crisTl:crisRegion[0])for(Candidato crisTr:crisRegion[1])for(Candidato crisBl:crisRegion[2])for(Candidato crisBr:crisRegion[3]){
            Candidato[] crisCs={crisTl,crisTr,crisBl,crisBr};
            if(crisTl==crisTr||crisTl==crisBl||crisTl==crisBr||crisTr==crisBl||crisTr==crisBr||crisBl==crisBr)continue;
            Punto[] crisPts={new Punto(crisTl.x,crisTl.y),new Punto(crisTr.x,crisTr.y),new Punto(crisBl.x,crisBl.y),new Punto(crisBr.x,crisBr.y)};
            if(!geomValida(crisPts,crisW,crisH))continue;
            double crisScore=puntaje(crisPts,crisCs,crisW,crisH);
            if(crisScore<crisMejor){crisElegido=crisPts;crisMejor=crisScore;}
        }
        if(crisElegido==null)throw new IllegalArgumentException("AJUSTE:Se observan marcas, pero la perspectiva impide alinearlas. Ajusta las cuatro esquinas.");
        return crisElegido;
    }

    /** Cuatro centros normalizados en orden superior izq, superior der, inferior izq, inferior der. */
    private static Punto[] manuales(double[] crisNormalizados,BufferedImage crisImg){
        if(crisNormalizados==null||crisNormalizados.length!=8)throw new IllegalArgumentException("Debes marcar exactamente cuatro cuadrados.");
        Punto[] crisPts=new Punto[4];
        for(int crisI=0;crisI<4;crisI++){
            double crisX=crisNormalizados[2*crisI],crisY=crisNormalizados[2*crisI+1];
            if(!Double.isFinite(crisX)||!Double.isFinite(crisY)||crisX<0||crisX>1||crisY<0||crisY>1)
                throw new IllegalArgumentException("Puntos manuales fuera de la fotografía.");
            crisPts[crisI]=new Punto(crisX*crisImg.getWidth(),crisY*crisImg.getHeight());
        }
        // El ajuste manual conserva el orden impreso TL, TR, BL, BR,
        // pero admite fotografias giradas 90 o 180 grados.
        double crisArriba=dist(crisPts[0],crisPts[1]),crisAbajo=dist(crisPts[2],crisPts[3]);
        double crisIzq=dist(crisPts[0],crisPts[2]),crisDer=dist(crisPts[1],crisPts[3]);
        double crisMinimo=Math.min(crisImg.getWidth(),crisImg.getHeight())*.25;
        if(Math.min(Math.min(crisArriba,crisAbajo),Math.min(crisIzq,crisDer))<crisMinimo
            ||cuadrilateroArea(crisPts)<(double)crisImg.getWidth()*crisImg.getHeight()*.12
            ||Math.max(crisArriba,crisAbajo)/Math.min(crisArriba,crisAbajo)>3.0
            ||Math.max(crisIzq,crisDer)/Math.min(crisIzq,crisDer)>3.0)
            throw new IllegalArgumentException("AJUSTE:Los puntos no forman una hoja completa. Comprueba el orden de los cuadrados.");
        // Rechaza cuatro puntos cruzados (cuadrilatero no convexo).
        Punto[] crisCiclo={crisPts[0],crisPts[1],crisPts[3],crisPts[2]};int crisSigno=0;
        for(int crisI=0;crisI<4;crisI++){
            Punto crisA=crisCiclo[crisI],crisB=crisCiclo[(crisI+1)%4],crisC=crisCiclo[(crisI+2)%4];
            double crisCruz=(crisB.x-crisA.x)*(crisC.y-crisB.y)-(crisB.y-crisA.y)*(crisC.x-crisB.x);
            int crisActual=crisCruz>0?1:crisCruz<0?-1:0;
            if(crisActual==0||(crisSigno!=0&&crisActual!=crisSigno))
                throw new IllegalArgumentException("AJUSTE:Los puntos se cruzan. Sigue el orden de las cuatro marcas.");
            crisSigno=crisActual;
        }
        // Ajuste fino opcional: si hay un cuadrado oscuro cercano al punto
        // tocado con el dedo, tomar su centro real. Si no, usar el punto manual.
        List<Candidato> crisCandidatos=compNegros(crisImg);
        Punto[] crisAfinados=crisPts.clone();
        double crisRadio=Math.min(crisImg.getWidth(),crisImg.getHeight())*.043;
        double crisAreaMin=Math.pow(Math.min(crisImg.getWidth(),crisImg.getHeight())*.014,2);
        for(int crisI=0;crisI<4;crisI++){
            double crisDistancia=crisRadio;Candidato crisMejor=null;
            for(Candidato crisC:crisCandidatos){
                if(crisC.area<crisAreaMin)continue;
                double crisD=dist(crisPts[crisI],new Punto(crisC.x,crisC.y));
                if(crisD<crisDistancia){crisDistancia=crisD;crisMejor=crisC;}
            }
            if(crisMejor!=null)crisAfinados[crisI]=new Punto(crisMejor.x,crisMejor.y);
        }
        return cuadrilateroArea(crisAfinados)>=crisImg.getWidth()*crisImg.getHeight()*.12?crisAfinados:crisPts;
    }

    /** Homografia milimetros de plantilla -> pixeles de camara. */
    private static double[] homografia(Punto[] crisEncontrados){
        double[] crisMx={9,201,9,201},crisMy={9,9,139.5,139.5};double[][] crisA=new double[8][9];
        for(int crisI=0;crisI<4;crisI++){
            double crisX=crisMx[crisI],crisY=crisMy[crisI],crisU=crisEncontrados[crisI].x(),crisV=crisEncontrados[crisI].y();int crisJ=crisI*2;
            crisA[crisJ]=new double[]{crisX,crisY,1,0,0,0,-crisU*crisX,-crisU*crisY,crisU};
            crisA[crisJ+1]=new double[]{0,0,0,crisX,crisY,1,-crisV*crisX,-crisV*crisY,crisV};
        }
        for(int crisI=0;crisI<8;crisI++){
            int crisPiv=crisI;for(int crisJ=crisI+1;crisJ<8;crisJ++)if(Math.abs(crisA[crisJ][crisI])>Math.abs(crisA[crisPiv][crisI]))crisPiv=crisJ;
            double[] crisTmp=crisA[crisI];crisA[crisI]=crisA[crisPiv];crisA[crisPiv]=crisTmp;
            if(Math.abs(crisA[crisI][crisI])<1e-10)throw new IllegalArgumentException("No se pudo calibrar la perspectiva.");
            double crisZ=crisA[crisI][crisI];for(int crisK=crisI;crisK<9;crisK++)crisA[crisI][crisK]/=crisZ;
            for(int crisJ=0;crisJ<8;crisJ++)if(crisJ!=crisI){crisZ=crisA[crisJ][crisI];for(int crisK=crisI;crisK<9;crisK++)crisA[crisJ][crisK]-=crisZ*crisA[crisI][crisK];}
        }
        double[] crisH=new double[9];for(int crisI=0;crisI<8;crisI++)crisH[crisI]=crisA[crisI][8];crisH[8]=1;return crisH;
    }
    private static Punto map(double[] crisH,double crisX,double crisY){
        double crisD=crisH[6]*crisX+crisH[7]*crisY+1;
        return new Punto((crisH[0]*crisX+crisH[1]*crisY+crisH[2])/crisD,(crisH[3]*crisX+crisH[4]*crisY+crisH[5])/crisD);
    }
    private static double relleno(BufferedImage crisImg,double[] crisHomografia,double crisX,double crisY){
        int crisOsc=0,crisTotal=0;
        for(double crisDy=-1.35;crisDy<=1.35;crisDy+=.23)for(double crisDx=-1.35;crisDx<=1.35;crisDx+=.23){
            if(crisDx*crisDx+crisDy*crisDy>1.35*1.35)continue;
            Punto crisP=map(crisHomografia,crisX+crisDx,crisY+crisDy);int crisPx=(int)Math.round(crisP.x()),crisPy=(int)Math.round(crisP.y());
            if(crisPx<0||crisPx>=crisImg.getWidth()||crisPy<0||crisPy>=crisImg.getHeight())continue;
            crisTotal++;if(luminosidad(crisImg.getRGB(crisPx,crisPy))<145)crisOsc++;
        }
        return crisTotal>0?(double)crisOsc/crisTotal:0;
    }
    private static double densQR(BufferedImage crisImg,double[] crisH){
        int crisOscuras=0,crisTotal=0;
        for(double crisY=15;crisY<=47;crisY+=1.1)for(double crisX=163;crisX<=193;crisX+=1.1){
            Punto crisP=map(crisH,crisX,crisY);int crisIx=(int)Math.round(crisP.x()),crisIy=(int)Math.round(crisP.y());
            if(crisIx>=0&&crisIx<crisImg.getWidth()&&crisIy>=0&&crisIy<crisImg.getHeight()){
                crisTotal++;if(luminosidad(crisImg.getRGB(crisIx,crisIy))<140)crisOscuras++;
            }
        }
        return crisTotal==0?0:(double)crisOscuras/crisTotal;
    }
    /** Diferencia hojas antiguas y nuevas sin basarse en el token: un lote EVI1 se puede reimprimir. */
    private static boolean usaPlantillaGran(BufferedImage crisImg,double[] crisH){
        int crisTotal=0,crisOscuro=0;
        // Zona lateral que solo esta ocupada por el QR grande (el QR antiguo empezaba en x=174).
        for(double crisYy=18;crisYy<=43;crisYy+=1.25)for(double crisXx=164;crisXx<=171;crisXx+=1.0){
            Punto crisP=map(crisH,crisXx,crisYy);int crisX=(int)Math.round(crisP.x()),crisY=(int)Math.round(crisP.y());
            if(crisX>=0&&crisX<crisImg.getWidth()&&crisY>=0&&crisY<crisImg.getHeight()){
                crisTotal++;if(luminosidad(crisImg.getRGB(crisX,crisY))<135)crisOscuro++;
            }
        }
        return crisTotal>0&&(double)crisOscuro/crisTotal>.11;
    }
    /**
     * Recorta y endereza exclusivamente la zona del codigo EJEMPLAR.
     * Los marcadores negros permiten mantener la zona incluso con perspectiva.
     * Funciona tanto con los QR grandes como con las fichas anteriores.
     * No interpreta el texto: lo entrega a ReconocedorCodigoOCR.
     */
    public static BufferedImage recortarCodEjemp(BufferedImage crisInput) {return recortarCodEjemp(crisInput,null);}

    public static BufferedImage recortarCodEjemp(BufferedImage crisInput,double[] crisAjuste) {
        if(crisInput==null || crisInput.getWidth()<450 || crisInput.getHeight()<350)
            throw new IllegalArgumentException("Fotografia demasiado pequena para leer el codigo impreso.");
        BufferedImage crisCandidato=reducir(crisInput), crisMejor=null;
        double[] crisH=null;
        if(crisAjuste!=null){
            crisMejor=crisCandidato;crisH=homografia(manuales(crisAjuste,crisMejor));
        }else{
            double crisScoreMejor=-1;
            for(int crisGiro=0;crisGiro<4;crisGiro++){
                if(crisGiro>0)crisCandidato=girar90(crisCandidato);
                if(crisCandidato.getWidth()<crisCandidato.getHeight()*1.10)continue;
                try {
                    Punto[] crisPts=marcadores(crisCandidato);
                    double[] crisHh=homografia(crisPts);
                    double crisScore=densQR(crisCandidato,crisHh);
                    if(crisScore>crisScoreMejor){crisScoreMejor=crisScore;crisMejor=crisCandidato;crisH=crisHh;}
                }catch(IllegalArgumentException crisIgnored){}
            }
        }
        if(crisMejor==null)throw new IllegalArgumentException("AJUSTE:No se pudieron localizar los cuadrados para leer el código. Ajusta las cuatro esquinas.");
        // Campo EJEMPLAR de la plantilla: x=112 mm, y=37 mm.
        // Tambien se incluye el rotulo y un borde blanco para mejorar Tesseract.
        double crisX0=108,crisY0=30,crisX1=158,crisY1=41;
        final int crisPxPorMm=18;
        int crisAncho=(int)Math.round((crisX1-crisX0)*crisPxPorMm), crisAlto=(int)Math.round((crisY1-crisY0)*crisPxPorMm);
        BufferedImage crisZona=new BufferedImage(crisAncho,crisAlto,BufferedImage.TYPE_INT_RGB);
        for(int crisYy=0;crisYy<crisAlto;crisYy++)for(int crisXx=0;crisXx<crisAncho;crisXx++){
            double crisX=crisX0+(crisXx+.5)/crisPxPorMm,crisY=crisY0+(crisYy+.5)/crisPxPorMm;
            Punto crisPunto=map(crisH,crisX,crisY);
            int crisCx=(int)Math.round(crisPunto.x()),crisCy=(int)Math.round(crisPunto.y());
            crisZona.setRGB(crisXx,crisYy,crisCx>=0&&crisCy>=0&&crisCx<crisMejor.getWidth()&&crisCy<crisMejor.getHeight()?crisMejor.getRGB(crisCx,crisCy):Color.WHITE.getRGB());
        }
        return crisZona;
    }

    public static Lectura detectar(BufferedImage crisInput,List<RepoImpresos.Pregunta> crisPreg,String crisTokenEsperado,boolean crisPermiteCodManual){
        return detectar(crisInput,crisPreg,crisTokenEsperado,crisPermiteCodManual,null);
    }
    public static Lectura detectar(BufferedImage crisInput,List<RepoImpresos.Pregunta> crisPreg,String crisTokenEsperado,boolean crisPermiteCodManual,double[] crisAjuste){
        if(crisInput==null||crisInput.getWidth()<450||crisInput.getHeight()<350)throw new IllegalArgumentException("La fotografía tiene resolución insuficiente.");
        BufferedImage crisImg=reducir(crisInput),crisSelected=null;
        Punto[] crisEsquinas=null;
        String crisToken=null;
        if(crisAjuste!=null){
            crisSelected=crisImg;
            crisEsquinas=manuales(crisAjuste,crisSelected);
        }else{
            BufferedImage crisCandidato=crisImg;
            double crisMayorPuntaje=-1;
            for(int crisGiro=0;crisGiro<4;crisGiro++){
                if(crisGiro>0)crisCandidato=girar90(crisCandidato);
                if(crisCandidato.getWidth()<crisCandidato.getHeight()*1.10)continue;
                try {
                    Punto[] crisPts=marcadores(crisCandidato);
                    double[] crisH=homografia(crisPts);
                    double crisDensidad=densQR(crisCandidato,crisH);
                    // Un QR legible ayuda a escoger la orientación, pero no
                    // es obligatorio si el token ya fue verificado por el servidor.
                    String crisLeido=leerQR(crisCandidato);
                    if(crisLeido!=null&&crisTokenEsperado!=null&&!crisTokenEsperado.equals(crisLeido))continue;
                    double crisScore=crisDensidad+(crisLeido!=null?.5:0);
                    if(crisScore>crisMayorPuntaje){crisMayorPuntaje=crisScore;crisSelected=crisCandidato;crisEsquinas=crisPts;crisToken=crisLeido;}
                }catch(IllegalArgumentException crisIgnored){}
            }
            if(crisSelected==null)throw new IllegalArgumentException("AJUSTE:No se reconocieron las cuatro marcas. Usa Ajustar esquinas sobre esta misma foto.");
        }
        double[] crisH=homografia(crisEsquinas);
        boolean crisPlantillaGrande=usaPlantillaGran(crisSelected,crisH);
        List<Marca> crisOut=new ArrayList<>();
        for(int crisI=0;crisI<crisPreg.size();crisI++){
            RepoImpresos.Pregunta crisP=crisPreg.get(crisI);
            double[] crisValores=new double[4];for(int crisA=0;crisA<4;crisA++)crisValores[crisA]=relleno(crisSelected,crisH,PdfExamenes.bubbleX(crisI,crisA),crisPlantillaGrande?PdfExamenes.bubbleY(crisI):49f+(crisI%15)*6f);
            int crisTop=0;for(int crisA=1;crisA<4;crisA++)if(crisValores[crisA]>crisValores[crisTop])crisTop=crisA;
            double crisSegundo=0;for(int crisA=0;crisA<4;crisA++)if(crisA!=crisTop)crisSegundo=Math.max(crisSegundo,crisValores[crisA]);
            String crisEstado,crisLetra=null;double crisConfianza=Math.min(1,Math.max(0,crisValores[crisTop]-crisSegundo));
            int crisFuertes=0;for(double crisV:crisValores)if(crisV>.29)crisFuertes++;
            if(crisFuertes>1)crisEstado="MULTIPLE";
            else if(crisValores[crisTop]<.13)crisEstado="BLANCO";
            else if(crisValores[crisTop]<.29||crisValores[crisTop]-crisSegundo<.12)crisEstado="AMBIGUA";
            else{crisEstado="VALIDA";crisLetra="ABCD".substring(crisTop,crisTop+1);}
            crisOut.add(new Marca(crisP.id(),crisP.numero(),crisLetra,crisEstado,crisConfianza));
        }
        return new Lectura(crisToken,crisSelected,crisOut,1.0);
    }
}
