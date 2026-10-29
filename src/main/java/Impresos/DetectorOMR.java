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
    public static String leerQR(BufferedImage b){
        String qr=decodificarQR(b);
        if(qr!=null)return qr;
        // Cuando la hoja se fotografio entera, la zona del QR representa pocos pixeles.
        // Aumentar un recorte conservando su quiet zone ayuda con camaras de gama baja.
        if(b.getWidth()>b.getHeight()*1.08){
            qr=decodificarRecorte(b,.69,.02,.99,.47);
            if(qr!=null)return qr;
        }
        return null;
    }
    private static String decodificarRecorte(BufferedImage im,double x1,double y1,double x2,double y2){
        int x=(int)(im.getWidth()*x1),y=(int)(im.getHeight()*y1);
        int w=Math.min(im.getWidth()-x,(int)(im.getWidth()*(x2-x1)));
        int h=Math.min(im.getHeight()-y,(int)(im.getHeight()*(y2-y1)));
        if(w<40||h<40)return null;
        BufferedImage parte=im.getSubimage(x,y,w,h);
        String qr=decodificarQR(parte);
        if(qr!=null)return qr;
        BufferedImage ampliada=new BufferedImage(w*2,h*2,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=ampliada.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(parte,0,0,w*2,h*2,null);g.dispose();
        return decodificarQR(ampliada);
    }
    private static String decodificarQR(BufferedImage img){
        try {
            BinaryBitmap bmp=new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));
            Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS,List.of(BarcodeFormat.QR_CODE));
            Result r=new MultiFormatReader().decode(bmp,hints);
            String codigo=r.getText();
            return codigo!=null&&codigo.matches("EVI1:[0-9a-fA-F-]{36}|EVI2-[0-9A-Z]{20}|E3[0-9A-Z]{12}")?codigo:null;
        } catch(Exception ignored){return null;}
    }
    private static BufferedImage reducir(BufferedImage original){
        int w=original.getWidth(),h=original.getHeight();double escala=Math.min(1.0,1800.0/Math.max(w,h));
        if(escala==1)return original;
        int nw=(int)Math.round(w*escala),nh=(int)Math.round(h*escala);
        BufferedImage im=new BufferedImage(nw,nh,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(original,0,0,nw,nh,null);g.dispose();return im;
    }
    private static BufferedImage girar90(BufferedImage b){
        BufferedImage out=new BufferedImage(b.getHeight(),b.getWidth(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=out.createGraphics();g.translate(out.getWidth(),0);g.rotate(Math.PI/2);g.drawImage(b,0,0,null);g.dispose();return out;
    }
    private static int luminosidad(int rgb){return ((rgb>>16)&255)*299/1000+((rgb>>8)&255)*587/1000+(rgb&255)*114/1000;}
    /**
     * Umbral local: las sombras del papel no se confunden con tinta y los
     * marcadores no tienen que ser negro RGB < 85 en toda la fotografia.
     */
    private static List<Candidato> componentesNegros(BufferedImage image){
        int w=image.getWidth(),h=image.getHeight(),len=w*h;
        int[] pixels=image.getRGB(0,0,w,h,null,0,w);
        byte[] mascara=new byte[len];
        int[] gris=new int[len];
        long[] integral=new long[(w+1)*(h+1)];
        for(int y=0;y<h;y++){
            long acum=0;
            for(int x=0;x<w;x++){
                int lum=luminosidad(pixels[y*w+x]);gris[y*w+x]=lum;acum+=lum;
                integral[(y+1)*(w+1)+x+1]=integral[y*(w+1)+x+1]+acum;
            }
        }
        int radio=Math.max(9,Math.min(w,h)/60);
        for(int y=0;y<h;y++){
            int y0=Math.max(0,y-radio),y1=Math.min(h,y+radio+1);
            for(int x=0;x<w;x++){
                int x0=Math.max(0,x-radio),x1=Math.min(w,x+radio+1);
                long suma=integral[y1*(w+1)+x1]-integral[y0*(w+1)+x1]-integral[y1*(w+1)+x0]+integral[y0*(w+1)+x0];
                int media=(int)(suma/((long)(x1-x0)*(y1-y0)));
                int v=gris[y*w+x];
                if(v<110 || (v<185 && v<media-29))mascara[y*w+x]=1;
            }
        }
        ArrayList<Candidato> all=new ArrayList<>();int[] cola=new int[len];
        int minSize=Math.max(6,(int)(Math.min(w,h)*.008));
        int maxSize=(int)(Math.min(w,h)*.12);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int start=y*w+x;if(mascara[start]!=1)continue;
            int first=0,last=0,area=0,xmin=x,xmax=x,ymin=y,ymax=y;
            cola[last++]=start;mascara[start]=2;
            while(first<last){
                int pos=cola[first++],py=pos/w,px=pos%w;area++;
                if(px<xmin)xmin=px;if(px>xmax)xmax=px;if(py<ymin)ymin=py;if(py>ymax)ymax=py;
                int n;
                if(px>0){n=pos-1;if(mascara[n]==1){mascara[n]=2;cola[last++]=n;}}
                if(px+1<w){n=pos+1;if(mascara[n]==1){mascara[n]=2;cola[last++]=n;}}
                if(py>0){n=pos-w;if(mascara[n]==1){mascara[n]=2;cola[last++]=n;}}
                if(py+1<h){n=pos+w;if(mascara[n]==1){mascara[n]=2;cola[last++]=n;}}
            }
            int cw=xmax-xmin+1,ch=ymax-ymin+1;
            if(cw<minSize||ch<minSize||cw>maxSize||ch>maxSize)continue;
            double proporcion=cw/(double)ch,densidad=area/(double)(cw*ch);
            if(proporcion>.56&&proporcion<1.74&&densidad>.55&&area>=minSize*minSize*.36)
                all.add(new Candidato((xmin+xmax)/2.0,(ymin+ymax)/2.0,cw,ch,area));
        }
        return all;
    }

    private static double dist(Punto a,Punto b){return Math.hypot(a.x-b.x,a.y-b.y);}
    private static double cuadrilateroArea(Punto[] p){
        Punto[] q={p[0],p[1],p[3],p[2]};double a=0;
        for(int i=0;i<4;i++){Punto u=q[i],v=q[(i+1)%4];a+=u.x*v.y-v.x*u.y;}
        return Math.abs(a)/2.0;
    }
    private static boolean geometriaAceptable(Punto[] p,int w,int h){
        if(p[0].x>=p[1].x||p[2].x>=p[3].x||p[0].y>=p[2].y||p[1].y>=p[3].y)return false;
        double sup=dist(p[0],p[1]),inf=dist(p[2],p[3]),izq=dist(p[0],p[2]),der=dist(p[1],p[3]);
        if(Math.min(sup,inf)<w*.54||Math.min(izq,der)<h*.50)return false;
        if(Math.max(sup,inf)/Math.min(sup,inf)>2.8||Math.max(izq,der)/Math.min(izq,der)>2.8)return false;
        return cuadrilateroArea(p)>(double)w*h*.15;
    }
    private static double puntuacion(Punto[] p,Candidato[] c,int w,int h){
        double sup=dist(p[0],p[1]),inf=dist(p[2],p[3]),izq=dist(p[0],p[2]),der=dist(p[1],p[3]);
        double tamMin=Double.POSITIVE_INFINITY,tamMax=0;
        for(Candidato cc:c){double tam=Math.sqrt(cc.area);tamMin=Math.min(tamMin,tam);tamMax=Math.max(tamMax,tam);}
        if(tamMax/tamMin>3.7)return Double.POSITIVE_INFINITY;
        double marco=(p[0].x+p[2].x)/w + (2*w-p[1].x-p[3].x)/w
            +(p[0].y+p[1].y)/h +(2*h-p[2].y-p[3].y)/h;
        double asim=Math.abs(Math.log(sup/inf))+Math.abs(Math.log(izq/der));
        // El dibujo usa el mismo tamaño de marcadores; las cuatro esquinas
        // deben tener escala parecida, con margen para perspectiva.
        return marco*.70 + asim*.40 + Math.log(tamMax/tamMin)*.32;
    }
    private static Punto[] marcadores(BufferedImage img){
        List<Candidato> cand=componentesNegros(img);
        int w=img.getWidth(),h=img.getHeight();
        if(cand.size()<4)throw new IllegalArgumentException("AJUSTE:No se distinguen las cuatro marcas. Toca sus centros sobre la foto.");
        @SuppressWarnings("unchecked") List<Candidato>[] region=new List[4];
        double[][] objetivos={{.06,.08},{.94,.08},{.06,.92},{.94,.92}};
        for(int i=0;i<4;i++){
            final double cx=objetivos[i][0],cy=objetivos[i][1];
            region[i]=new ArrayList<>();
            for(Candidato c:cand){
                double nx=c.x/w,ny=c.y/h;
                if(i%2==0 ? nx>.50 : nx<.50)continue;
                if(i<2 ? ny>.49 : ny<.51)continue;
                region[i].add(c);
            }
            region[i].sort(Comparator.comparingDouble(c->Math.hypot(c.x/w-cx,c.y/h-cy)));
            if(region[i].size()>9)region[i]=new ArrayList<>(region[i].subList(0,9));
            if(region[i].isEmpty())throw new IllegalArgumentException("AJUSTE:No se encuentran los cuatro cuadrados negros. Ajusta sus centros manualmente.");
        }
        Punto[] elegido=null;double mejor=Double.POSITIVE_INFINITY;
        for(Candidato tl:region[0])for(Candidato tr:region[1])for(Candidato bl:region[2])for(Candidato br:region[3]){
            Candidato[] cs={tl,tr,bl,br};
            if(tl==tr||tl==bl||tl==br||tr==bl||tr==br||bl==br)continue;
            Punto[] pts={new Punto(tl.x,tl.y),new Punto(tr.x,tr.y),new Punto(bl.x,bl.y),new Punto(br.x,br.y)};
            if(!geometriaAceptable(pts,w,h))continue;
            double score=puntuacion(pts,cs,w,h);
            if(score<mejor){elegido=pts;mejor=score;}
        }
        if(elegido==null)throw new IllegalArgumentException("AJUSTE:Se observan marcas, pero la perspectiva impide alinearlas. Ajusta las cuatro esquinas.");
        return elegido;
    }

    /** Cuatro centros normalizados en orden superior izq, superior der, inferior izq, inferior der. */
    private static Punto[] manuales(double[] normalizados,BufferedImage img){
        if(normalizados==null||normalizados.length!=8)throw new IllegalArgumentException("Debes marcar exactamente cuatro cuadrados.");
        Punto[] pts=new Punto[4];
        for(int i=0;i<4;i++){
            double x=normalizados[2*i],y=normalizados[2*i+1];
            if(!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>1||y<0||y>1)
                throw new IllegalArgumentException("Puntos manuales fuera de la fotografía.");
            pts[i]=new Punto(x*img.getWidth(),y*img.getHeight());
        }
        // El ajuste manual conserva el orden impreso TL, TR, BL, BR,
        // pero admite fotografias giradas 90 o 180 grados.
        double arriba=dist(pts[0],pts[1]),abajo=dist(pts[2],pts[3]);
        double izq=dist(pts[0],pts[2]),der=dist(pts[1],pts[3]);
        double minimo=Math.min(img.getWidth(),img.getHeight())*.25;
        if(Math.min(Math.min(arriba,abajo),Math.min(izq,der))<minimo
            ||cuadrilateroArea(pts)<(double)img.getWidth()*img.getHeight()*.12
            ||Math.max(arriba,abajo)/Math.min(arriba,abajo)>3.0
            ||Math.max(izq,der)/Math.min(izq,der)>3.0)
            throw new IllegalArgumentException("AJUSTE:Los puntos no forman una hoja completa. Comprueba el orden de los cuadrados.");
        // Rechaza cuatro puntos cruzados (cuadrilatero no convexo).
        Punto[] ciclo={pts[0],pts[1],pts[3],pts[2]};int signo=0;
        for(int i=0;i<4;i++){
            Punto a=ciclo[i],b=ciclo[(i+1)%4],c=ciclo[(i+2)%4];
            double cruz=(b.x-a.x)*(c.y-b.y)-(b.y-a.y)*(c.x-b.x);
            int actual=cruz>0?1:cruz<0?-1:0;
            if(actual==0||(signo!=0&&actual!=signo))
                throw new IllegalArgumentException("AJUSTE:Los puntos se cruzan. Sigue el orden de las cuatro marcas.");
            signo=actual;
        }
        // Ajuste fino opcional: si hay un cuadrado oscuro cercano al punto
        // tocado con el dedo, tomar su centro real. Si no, usar el punto manual.
        List<Candidato> candidatos=componentesNegros(img);
        Punto[] afinados=pts.clone();
        double radio=Math.min(img.getWidth(),img.getHeight())*.043;
        double areaMin=Math.pow(Math.min(img.getWidth(),img.getHeight())*.014,2);
        for(int i=0;i<4;i++){
            double distancia=radio;Candidato mejor=null;
            for(Candidato c:candidatos){
                if(c.area<areaMin)continue;
                double d=dist(pts[i],new Punto(c.x,c.y));
                if(d<distancia){distancia=d;mejor=c;}
            }
            if(mejor!=null)afinados[i]=new Punto(mejor.x,mejor.y);
        }
        return cuadrilateroArea(afinados)>=img.getWidth()*img.getHeight()*.12?afinados:pts;
    }

    /** Homografia milimetros de plantilla -> pixeles de camara. */
    private static double[] homografia(Punto[] encontrados){
        double[] mx={9,201,9,201},my={9,9,139.5,139.5};double[][] a=new double[8][9];
        for(int i=0;i<4;i++){
            double x=mx[i],y=my[i],u=encontrados[i].x(),v=encontrados[i].y();int j=i*2;
            a[j]=new double[]{x,y,1,0,0,0,-u*x,-u*y,u};
            a[j+1]=new double[]{0,0,0,x,y,1,-v*x,-v*y,v};
        }
        for(int i=0;i<8;i++){
            int piv=i;for(int j=i+1;j<8;j++)if(Math.abs(a[j][i])>Math.abs(a[piv][i]))piv=j;
            double[] tmp=a[i];a[i]=a[piv];a[piv]=tmp;
            if(Math.abs(a[i][i])<1e-10)throw new IllegalArgumentException("No se pudo calibrar la perspectiva.");
            double z=a[i][i];for(int k=i;k<9;k++)a[i][k]/=z;
            for(int j=0;j<8;j++)if(j!=i){z=a[j][i];for(int k=i;k<9;k++)a[j][k]-=z*a[i][k];}
        }
        double[] h=new double[9];for(int i=0;i<8;i++)h[i]=a[i][8];h[8]=1;return h;
    }
    private static Punto map(double[] h,double x,double y){
        double d=h[6]*x+h[7]*y+1;
        return new Punto((h[0]*x+h[1]*y+h[2])/d,(h[3]*x+h[4]*y+h[5])/d);
    }
    private static double relleno(BufferedImage img,double[] homografia,double x,double y){
        int osc=0,total=0;
        for(double dy=-1.35;dy<=1.35;dy+=.23)for(double dx=-1.35;dx<=1.35;dx+=.23){
            if(dx*dx+dy*dy>1.35*1.35)continue;
            Punto p=map(homografia,x+dx,y+dy);int px=(int)Math.round(p.x()),py=(int)Math.round(p.y());
            if(px<0||px>=img.getWidth()||py<0||py>=img.getHeight())continue;
            total++;if(luminosidad(img.getRGB(px,py))<145)osc++;
        }
        return total>0?(double)osc/total:0;
    }
    private static double densidadQR(BufferedImage img,double[] h){
        int oscuras=0,total=0;
        for(double y=15;y<=47;y+=1.1)for(double x=163;x<=193;x+=1.1){
            Punto p=map(h,x,y);int ix=(int)Math.round(p.x()),iy=(int)Math.round(p.y());
            if(ix>=0&&ix<img.getWidth()&&iy>=0&&iy<img.getHeight()){
                total++;if(luminosidad(img.getRGB(ix,iy))<140)oscuras++;
            }
        }
        return total==0?0:(double)oscuras/total;
    }
    /** Diferencia hojas antiguas y nuevas sin basarse en el token: un lote EVI1 se puede reimprimir. */
    private static boolean usaPlantillaGrande(BufferedImage img,double[] h){
        int total=0,oscuro=0;
        // Zona lateral que solo esta ocupada por el QR grande (el QR antiguo empezaba en x=174).
        for(double yy=18;yy<=43;yy+=1.25)for(double xx=164;xx<=171;xx+=1.0){
            Punto p=map(h,xx,yy);int x=(int)Math.round(p.x()),y=(int)Math.round(p.y());
            if(x>=0&&x<img.getWidth()&&y>=0&&y<img.getHeight()){
                total++;if(luminosidad(img.getRGB(x,y))<135)oscuro++;
            }
        }
        return total>0&&(double)oscuro/total>.11;
    }
    /**
     * Recorta y endereza exclusivamente la zona del codigo EJEMPLAR.
     * Los marcadores negros permiten mantener la zona incluso con perspectiva.
     * Funciona tanto con los QR grandes como con las fichas anteriores.
     * No interpreta el texto: lo entrega a ReconocedorCodigoOCR.
     */
    public static BufferedImage recortarCodigoEjemplar(BufferedImage input) {return recortarCodigoEjemplar(input,null);}

    public static BufferedImage recortarCodigoEjemplar(BufferedImage input,double[] ajuste) {
        if(input==null || input.getWidth()<450 || input.getHeight()<350)
            throw new IllegalArgumentException("Fotografia demasiado pequena para leer el codigo impreso.");
        BufferedImage candidato=reducir(input), mejor=null;
        double[] h=null;
        if(ajuste!=null){
            mejor=candidato;h=homografia(manuales(ajuste,mejor));
        }else{
            double scoreMejor=-1;
            for(int giro=0;giro<4;giro++){
                if(giro>0)candidato=girar90(candidato);
                if(candidato.getWidth()<candidato.getHeight()*1.10)continue;
                try {
                    Punto[] pts=marcadores(candidato);
                    double[] hh=homografia(pts);
                    double score=densidadQR(candidato,hh);
                    if(score>scoreMejor){scoreMejor=score;mejor=candidato;h=hh;}
                }catch(IllegalArgumentException ignored){}
            }
        }
        if(mejor==null)throw new IllegalArgumentException("AJUSTE:No se pudieron localizar los cuadrados para leer el código. Ajusta las cuatro esquinas.");
        // Campo EJEMPLAR de la plantilla: x=112 mm, y=37 mm.
        // Tambien se incluye el rotulo y un borde blanco para mejorar Tesseract.
        double x0=108,y0=30,x1=158,y1=41;
        final int pxPorMm=18;
        int ancho=(int)Math.round((x1-x0)*pxPorMm), alto=(int)Math.round((y1-y0)*pxPorMm);
        BufferedImage zona=new BufferedImage(ancho,alto,BufferedImage.TYPE_INT_RGB);
        for(int yy=0;yy<alto;yy++)for(int xx=0;xx<ancho;xx++){
            double x=x0+(xx+.5)/pxPorMm,y=y0+(yy+.5)/pxPorMm;
            Punto punto=map(h,x,y);
            int cx=(int)Math.round(punto.x()),cy=(int)Math.round(punto.y());
            zona.setRGB(xx,yy,cx>=0&&cy>=0&&cx<mejor.getWidth()&&cy<mejor.getHeight()?mejor.getRGB(cx,cy):Color.WHITE.getRGB());
        }
        return zona;
    }

    public static Lectura detectar(BufferedImage input,List<RepositorioImpresos.Pregunta> preguntas,String tokenEsperado,boolean permiteCodigoManual){
        return detectar(input,preguntas,tokenEsperado,permiteCodigoManual,null);
    }
    public static Lectura detectar(BufferedImage input,List<RepositorioImpresos.Pregunta> preguntas,String tokenEsperado,boolean permiteCodigoManual,double[] ajuste){
        if(input==null||input.getWidth()<450||input.getHeight()<350)throw new IllegalArgumentException("La fotografía tiene resolución insuficiente.");
        BufferedImage img=reducir(input),selected=null;
        Punto[] esquinas=null;
        String token=null;
        if(ajuste!=null){
            selected=img;
            esquinas=manuales(ajuste,selected);
        }else{
            BufferedImage candidato=img;
            double mayorPuntaje=-1;
            for(int giro=0;giro<4;giro++){
                if(giro>0)candidato=girar90(candidato);
                if(candidato.getWidth()<candidato.getHeight()*1.10)continue;
                try {
                    Punto[] pts=marcadores(candidato);
                    double[] h=homografia(pts);
                    double densidad=densidadQR(candidato,h);
                    // Un QR legible ayuda a escoger la orientación, pero no
                    // es obligatorio si el token ya fue verificado por el servidor.
                    String leido=leerQR(candidato);
                    if(leido!=null&&tokenEsperado!=null&&!tokenEsperado.equals(leido))continue;
                    double score=densidad+(leido!=null?.5:0);
                    if(score>mayorPuntaje){mayorPuntaje=score;selected=candidato;esquinas=pts;token=leido;}
                }catch(IllegalArgumentException ignored){}
            }
            if(selected==null)throw new IllegalArgumentException("AJUSTE:No se reconocieron las cuatro marcas. Usa Ajustar esquinas sobre esta misma foto.");
        }
        double[] h=homografia(esquinas);
        boolean plantillaGrande=usaPlantillaGrande(selected,h);
        List<Marca> out=new ArrayList<>();
        for(int i=0;i<preguntas.size();i++){
            RepositorioImpresos.Pregunta p=preguntas.get(i);
            double[] valores=new double[4];for(int a=0;a<4;a++)valores[a]=relleno(selected,h,PdfExamenes.bubbleX(i,a),plantillaGrande?PdfExamenes.bubbleY(i):49f+(i%15)*6f);
            int top=0;for(int a=1;a<4;a++)if(valores[a]>valores[top])top=a;
            double segundo=0;for(int a=0;a<4;a++)if(a!=top)segundo=Math.max(segundo,valores[a]);
            String estado,letra=null;double confianza=Math.min(1,Math.max(0,valores[top]-segundo));
            int fuertes=0;for(double v:valores)if(v>.29)fuertes++;
            if(fuertes>1)estado="MULTIPLE";
            else if(valores[top]<.13)estado="BLANCO";
            else if(valores[top]<.29||valores[top]-segundo<.12)estado="AMBIGUA";
            else{estado="VALIDA";letra="ABCD".substring(top,top+1);}
            out.add(new Marca(p.id(),p.numero(),letra,estado,confianza));
        }
        return new Lectura(token,selected,out,1.0);
    }
}
