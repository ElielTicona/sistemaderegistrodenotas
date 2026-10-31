package Impresos;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import java.net.URI;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Ventana independiente para no alterar los .form generados por NetBeans. */
public final class PantImpresos extends JDialog {
    private final int crisDocente;
    private final RepoImpresos crisRepo=new RepoImpresos();
    private final JComboBox<RepoImpresos.Curso> crisCursos=new JComboBox<>();
    private final JComboBox<RepoImpresos.Evaluacion> crisExamenes=new JComboBox<>();
    private final DefaultListModel<RepoImpresos.Alumno> crisModAlumnos=new DefaultListModel<>();
    private final JList<RepoImpresos.Alumno> crisAlumnos=new JList<>(crisModAlumnos);
    private final DefaultListModel<RepoImpresos.LoteListado> crisModLotes=new DefaultListModel<>();
    private final JList<RepoImpresos.LoteListado> crisLotes=new JList<>(crisModLotes);
    private final JCheckBox crisPreg=new JCheckBox("Mezclar preguntas",true);
    private final JCheckBox crisAlts=new JCheckBox("Mezclar alternativas",true);
    private final JButton crisGenerar=new JButton("Generar PDF completo");
    private final JButton crisVolverAbrir=new JButton("Recrear PDF / reimprimir (mismo QR)");
    private final JButton crisAnular=new JButton("Anular generacion (si no tiene notas)");
    private final JButton crisIniciarEscaneo=new JButton("Activar escaneo desde celular");
    private final JButton crisCerrarEscaneo=new JButton("Detener escaneo");
    private final JTextArea crisSalida=new JTextArea(5,30);
    private ServEscaneo crisServ;
    private boolean crisActualizando=false;
    public PantImpresos(Window crisOwner,int crisDocente){
        super(crisOwner,"Examenes impresos | Sistema de Evaluacion",ModalityType.APPLICATION_MODAL);
        this.crisDocente=crisDocente;setSize(995,710);setMinimumSize(new Dimension(800,540));setLocationRelativeTo(crisOwner);
        setLayout(new BorderLayout(12,12));((JComponent)getContentPane()).setBorder(new EmptyBorder(16,17,16,17));
        getContentPane().setBackground(new Color(244,247,245));
        JLabel crisTitle=new JLabel("Examenes impresos, QR y escaneo movil");crisTitle.setFont(new Font("SansSerif",Font.BOLD,21));
        JLabel crisSubt=new JLabel("Selecciona un examen existente. Se creara un ejemplar diferente para cada matriculado.");
        crisSubt.setForeground(new Color(79,96,86));JPanel crisHeader=new JPanel(new GridLayout(2,1));crisHeader.setOpaque(false);crisHeader.add(crisTitle);crisHeader.add(crisSubt);add(crisHeader,BorderLayout.NORTH);
        JPanel crisCentro=new JPanel(new GridLayout(1,2,14,0));crisCentro.setOpaque(false);
        JPanel crisIzq=caja();crisIzq.setLayout(new BorderLayout(6,7));
        JPanel crisSel=new JPanel(new GridLayout(4,1,2,3));crisSel.setOpaque(false);
        crisSel.add(etiqueta("Curso asignado al docente"));crisSel.add(crisCursos);crisSel.add(etiqueta("Examen previamente creado"));crisSel.add(crisExamenes);crisIzq.add(crisSel,BorderLayout.NORTH);
        JPanel crisEstud=new JPanel(new BorderLayout(6,5));crisEstud.setOpaque(false);
        crisEstud.add(etiqueta("Matriculados (Ctrl / Shift para seleccionar solo algunos)"),BorderLayout.NORTH);
        crisAlumnos.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        crisAlumnos.setVisibleRowCount(10);crisAlumnos.setBorder(new EmptyBorder(4,4,4,4));crisEstud.add(new JScrollPane(crisAlumnos),BorderLayout.CENTER);
        JButton crisTodos=new JButton("Seleccionar todos los matriculados");crisTodos.addActionListener(crisE->{if(crisModAlumnos.size()>0)crisAlumnos.setSelectionInterval(0,crisModAlumnos.size()-1);});
        crisEstud.add(crisTodos,BorderLayout.SOUTH);crisIzq.add(crisEstud,BorderLayout.CENTER);
        JPanel crisConfig=new JPanel(new GridLayout(3,1,3,4));crisConfig.setOpaque(false);crisPreg.setOpaque(false);crisAlts.setOpaque(false);
        crisConfig.add(crisPreg);crisConfig.add(crisAlts);crisConfig.add(crisGenerar);crisIzq.add(crisConfig,BorderLayout.SOUTH);
        JPanel crisDer=caja();crisDer.setLayout(new BorderLayout(6,7));
        JPanel crisHist=new JPanel(new BorderLayout(5,5));crisHist.setOpaque(false);
        crisHist.add(etiqueta("Generaciones anteriores (reimprimibles sin volver a mezclar)"),BorderLayout.NORTH);
        crisLotes.setVisibleRowCount(9);crisHist.add(new JScrollPane(crisLotes),BorderLayout.CENTER);
        JPanel crisAcciones=new JPanel(new GridLayout(4,1,4,6));crisAcciones.setOpaque(false);crisAcciones.add(crisVolverAbrir);crisAcciones.add(crisAnular);crisAcciones.add(crisIniciarEscaneo);crisAcciones.add(crisCerrarEscaneo);
        crisHist.add(crisAcciones,BorderLayout.SOUTH);crisDer.add(crisHist,BorderLayout.CENTER);
        crisSalida.setLineWrap(true);crisSalida.setWrapStyleWord(true);crisSalida.setEditable(false);crisSalida.setBackground(new Color(242,247,244));crisSalida.setBorder(new EmptyBorder(8,9,8,9));
        crisDer.add(new JScrollPane(crisSalida),BorderLayout.SOUTH);
        crisCentro.add(crisIzq);crisCentro.add(crisDer);add(crisCentro,BorderLayout.CENTER);
        crisCursos.addActionListener(crisE->cambiarCurso());crisExamenes.addActionListener(crisE->cambiarExamen());
        crisGenerar.addActionListener(crisE->generarPdf());crisVolverAbrir.addActionListener(crisE->reimprimir());
        crisIniciarEscaneo.addActionListener(crisE->iniciarServ());crisCerrarEscaneo.addActionListener(crisE->detenerServ());crisAnular.addActionListener(crisE->anularSelec());
        crisCerrarEscaneo.setEnabled(false);
        addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent crisE){detenerServ();}});
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        cargar();
    }
    private static JPanel caja(){JPanel crisP=new JPanel();crisP.setBackground(Color.WHITE);crisP.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(222,232,226)),new EmptyBorder(12,12,12,12)));return crisP;}
    private static JLabel etiqueta(String crisS){JLabel crisL=new JLabel(crisS);crisL.setFont(new Font("SansSerif",Font.BOLD,12));return crisL;}
    private void info(String crisS){crisSalida.append(crisS+"\n");crisSalida.setCaretPosition(crisSalida.getDocument().getLength());}
    private void cargar(){
        try {crisActualizando=true;for(var crisCurso:crisRepo.cursos(crisDocente))crisCursos.addItem(crisCurso);crisActualizando=false;cambiarCurso();}
        catch(Exception crisE){crisActualizando=false;error(crisE);}
    }
    private void cambiarCurso(){
        if(crisActualizando)return;
        crisActualizando=true;crisExamenes.removeAllItems();crisModAlumnos.clear();crisModLotes.clear();
        try {
            var crisCurso=(RepoImpresos.Curso)crisCursos.getSelectedItem();
            if(crisCurso!=null){
                for(var crisX:crisRepo.evaluaciones(crisCurso.id(),crisDocente))crisExamenes.addItem(crisX);
                for(var crisX:crisRepo.matriculados(crisCurso.id(),crisDocente))crisModAlumnos.addElement(crisX);
                if(crisModAlumnos.size()>0)crisAlumnos.setSelectionInterval(0,crisModAlumnos.size()-1);
            }
        }catch(Exception crisE){error(crisE);}
        finally{crisActualizando=false;cambiarExamen();}
    }
    private void cambiarExamen(){
        if(crisActualizando)return;
        crisModLotes.clear();var crisEval=(RepoImpresos.Evaluacion)crisExamenes.getSelectedItem();
        if(crisEval==null)return;
        try {for(var crisLote:crisRepo.lotes(crisEval.id(),crisDocente))crisModLotes.addElement(crisLote);}
        catch(Exception crisE){error(crisE);}
    }
    private void error(Exception crisE){JOptionPane.showMessageDialog(this,crisE.getMessage(),"No se pudo completar la operacion",JOptionPane.ERROR_MESSAGE);info("ERROR: "+crisE.getMessage());}
    private void generarPdf(){
        var crisEv=(RepoImpresos.Evaluacion)crisExamenes.getSelectedItem();if(crisEv==null){info("Crea o selecciona primero un examen del curso.");return;}
        List<RepoImpresos.Alumno> crisSelecs=crisAlumnos.getSelectedValuesList();if(crisSelecs.isEmpty()){info("Selecciona al menos un alumno.");return;}
        int crisResp=JOptionPane.showConfirmDialog(this,"Se generara una NUEVA version para "+crisSelecs.size()+" alumno(s).\n"+
          "Reimprimir una version previa no requiere generar otra.\n¿Continuar?","Confirmar generacion",JOptionPane.YES_NO_OPTION);
        if(crisResp!=JOptionPane.YES_OPTION)return;
        List<String> crisDnis=crisSelecs.stream().map(RepoImpresos.Alumno::dni).toList();
        crisGenerar.setEnabled(false);info("Generando "+crisDnis.size()+" ejemplares y preparando PDF...");
        new SwingWorker<Path,Void>(){
            long lote=0;
            @Override protected Path doInBackground() throws Exception {
                lote=crisRepo.generar(crisEv.id(),crisDocente,crisDnis,crisPreg.isSelected(),crisAlts.isSelected());
                try {
                    Path crisRuta=PdfExamenes.generar(crisRepo.cargarLote(lote,crisDocente));crisRepo.marcarPdf(lote,crisDocente,crisRuta);return crisRuta;
                }catch(Exception crisE){crisRepo.errorPdf(lote,crisDocente);throw crisE;}
            }
            @Override protected void done(){
                crisGenerar.setEnabled(true);
                try {Path crisRuta=get();info("PDF listo: "+crisRuta);cambiarExamen();abrir(crisRuta);}
                catch(Exception crisE){error(new Exception(crisE.getCause()==null?crisE.getMessage():crisE.getCause().getMessage()));cambiarExamen();}
            }
        }.execute();
    }
    private void anularSelec(){
        var crisLote=crisLotes.getSelectedValue();if(crisLote==null){info("Selecciona una generacion.");return;}
        if(JOptionPane.showConfirmDialog(this,"¿Anular la generacion "+crisLote.numero()+"? Las hojas impresas dejaran de poder escanearse. No se borran documentos ni historial.","Anular generacion",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        try{crisRepo.anularLote(crisLote.id(),crisDocente);info("Generacion anulada: "+crisLote.id());cambiarExamen();}
        catch(Exception crisE){error(crisE);}
    }
    private void reimprimir(){
        var crisSelec=crisLotes.getSelectedValue();if(crisSelec==null){info("Selecciona una generacion anterior.");return;}
        try {
            var crisLote=crisRepo.cargarLote(crisSelec.id(),crisDocente);
            // Regenerar tambien lotes antiguos: mismo token y respuestas, PDF con QR grande.
            if("PREPARANDO".equals(crisSelec.estado())||"ANULADO".equals(crisSelec.estado()))throw new SQLException("El lote no esta disponible para imprimir.");
            Path crisRuta=PdfExamenes.generar(crisLote);
            crisRepo.marcarPdf(crisSelec.id(),crisDocente,crisRuta);
            cambiarExamen();
            abrir(crisRuta);
        }catch(Exception crisE){error(crisE);}
    }
    private void abrir(Path crisP)throws Exception{
        info("Archivo: "+crisP);
        if(Desktop.isDesktopSupported()&&Desktop.getDesktop().isSupported(Desktop.Action.OPEN))Desktop.getDesktop().open(crisP.toFile());
        else JOptionPane.showMessageDialog(this,"PDF guardado en:\n"+crisP);
    }
    /** QR para ABRIR el escaner desde el celular; no es el QR impreso en los examenes. */
    private static ImageIcon imagenQRAcceso(String crisUrl)throws Exception{
        BitMatrix crisMatriz=new QRCodeWriter().encode(crisUrl,BarcodeFormat.QR_CODE,260,260);
        BufferedImage crisImagen=new BufferedImage(crisMatriz.getWidth(),crisMatriz.getHeight(),BufferedImage.TYPE_INT_RGB);
        for(int crisY=0;crisY<crisMatriz.getHeight();crisY++)for(int crisX=0;crisX<crisMatriz.getWidth();crisX++)
            crisImagen.setRGB(crisX,crisY,crisMatriz.get(crisX,crisY)?0xFF162E28:0xFFFFFFFF);
        return new ImageIcon(crisImagen);
    }
    private void iniciarServ(){
        try {
            crisServ=new ServEscaneo(crisDocente);
            String crisUrlInicial=crisServ.iniciar();
            crisIniciarEscaneo.setEnabled(false);crisCerrarEscaneo.setEnabled(true);
            info("Servidor movil activo en TCP 8787. Enlace inicial: "+crisUrlInicial);
            mostrarAccesoCel(crisUrlInicial);
        }catch(Exception crisE){
            if(crisServ!=null)crisServ.close();crisServ=null;
            if(crisE instanceof java.net.BindException)error(new Exception("El puerto 8787 esta ocupado o bloqueado. Cierra el programa que lo utiliza y vuelve a intentar. Detalle: "+crisE.getMessage()));
            else error(crisE);
        }
    }
    private void mostrarAccesoCel(String crisUrlInicial){
        JPanel crisPanel=new JPanel(new BorderLayout(7,10));
        crisPanel.setBorder(new EmptyBorder(8,8,8,8));
        JLabel crisTitulo=new JLabel("Escanea ESTE QR con la camara del celular para abrir el escaner.");
        crisTitulo.setFont(new Font("SansSerif",Font.BOLD,13));
        crisPanel.add(crisTitulo,BorderLayout.NORTH);

        JPanel crisContenido=new JPanel(new BorderLayout(8,8));
        JLabel crisQr=new JLabel("",SwingConstants.CENTER);crisQr.setOpaque(true);crisQr.setBackground(Color.WHITE);
        crisQr.setPreferredSize(new Dimension(275,280));
        crisContenido.add(crisQr,BorderLayout.CENTER);

        JPanel crisInferior=new JPanel(new GridLayout(0,1,4,5));
        crisInferior.add(etiqueta("IP del PC (elige la IP de tu Wi-Fi o Ethernet):"));
        java.util.List<String> crisIps=ServEscaneo.ipsLocales();
        JComboBox<String> crisSelector=new JComboBox<>(crisIps.toArray(new String[0]));
        crisSelector.setEditable(true);
        String crisIpInicial=URI.create(crisUrlInicial).getHost();
        crisSelector.setSelectedItem(crisIpInicial);
        crisInferior.add(crisSelector);
        JTextField crisEnlace=new JTextField(crisUrlInicial);crisEnlace.setEditable(false);
        crisInferior.add(etiqueta("Enlace que debe abrirse en el navegador del telefono:"));
        crisInferior.add(crisEnlace);
        JPanel crisBotones=new JPanel(new GridLayout(1,3,5,0));
        JButton crisActualizar=new JButton("Actualizar QR");
        JButton crisCopiar=new JButton("Copiar enlace");
        JButton crisProbar=new JButton("Probar en PC");
        crisBotones.add(crisActualizar);crisBotones.add(crisCopiar);crisBotones.add(crisProbar);
        crisInferior.add(crisBotones);
        JTextArea crisAyuda=new JTextArea(
          "Si no abre: revisa que sea la IP real de Wi-Fi (ipconfig en Windows).\n"
         +"Permite Java TCP 8787 en Firewall de Windows, red Privada.\n"
         +"Si en la PC abre y en el celular no, revisa Firewall, IP y aislamiento Wi-Fi.\n"
         +"El QR impreso en el examen es DISTINTO: solo identifica la hoja.");
        crisAyuda.setLineWrap(true);crisAyuda.setWrapStyleWord(true);crisAyuda.setOpaque(false);
        crisAyuda.setEditable(false);crisAyuda.setFont(new Font("SansSerif",Font.PLAIN,12));
        crisAyuda.setRows(4);crisInferior.add(crisAyuda);
        crisContenido.add(crisInferior,BorderLayout.SOUTH);
        crisPanel.add(crisContenido,BorderLayout.CENTER);

        Runnable crisGenerar=()->{
            try{
                String crisIp=Objects.toString(crisSelector.getSelectedItem(),"").trim();
                String crisUrl=crisServ.urlConIP(crisIp);
                crisEnlace.setText(crisUrl);crisQr.setIcon(imagenQRAcceso(crisUrl));crisQr.setText("");
            }catch(Exception crisErr){
                crisQr.setIcon(null);crisQr.setText("IP incorrecta");
                JOptionPane.showMessageDialog(this,crisErr.getMessage(),"Revisar IP",JOptionPane.WARNING_MESSAGE);
            }
        };
        crisActualizar.addActionListener(crisE->crisGenerar.run());
        crisCopiar.addActionListener(crisE->{
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(crisEnlace.getText()),null);
            info("Enlace de acceso copiado. No lo compartas con estudiantes.");
        });
        crisProbar.addActionListener(crisE->{
            try{Desktop.getDesktop().browse(URI.create(crisEnlace.getText()));}
            catch(Exception crisErr){error(new Exception("No se pudo abrir el navegador: "+crisErr.getMessage()));}
        });
        crisGenerar.run();
        JOptionPane.showMessageDialog(this,crisPanel,"Acceso al escaner desde celular",JOptionPane.PLAIN_MESSAGE);
        info("Si necesitas cambiar la IP, detén y vuelve a activar el escaner. El servidor sigue disponible hasta detenerlo.");
    }
    private void detenerServ(){
        if(crisServ!=null){crisServ.close();crisServ=null;info("Escaneo movil detenido.");}
        crisIniciarEscaneo.setEnabled(true);crisCerrarEscaneo.setEnabled(false);
    }
}
