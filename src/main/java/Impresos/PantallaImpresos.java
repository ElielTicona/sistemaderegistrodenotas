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
public final class PantallaImpresos extends JDialog {
    private final int docente;
    private final RepositorioImpresos repo=new RepositorioImpresos();
    private final JComboBox<RepositorioImpresos.Curso> cursos=new JComboBox<>();
    private final JComboBox<RepositorioImpresos.Evaluacion> examenes=new JComboBox<>();
    private final DefaultListModel<RepositorioImpresos.Alumno> modeloAlumnos=new DefaultListModel<>();
    private final JList<RepositorioImpresos.Alumno> alumnos=new JList<>(modeloAlumnos);
    private final DefaultListModel<RepositorioImpresos.LoteListado> modeloLotes=new DefaultListModel<>();
    private final JList<RepositorioImpresos.LoteListado> lotes=new JList<>(modeloLotes);
    private final JCheckBox preguntas=new JCheckBox("Mezclar preguntas",true);
    private final JCheckBox alternativas=new JCheckBox("Mezclar alternativas",true);
    private final JButton generar=new JButton("Generar PDF completo");
    private final JButton volverAbrir=new JButton("Recrear PDF / reimprimir (mismo QR)");
    private final JButton anular=new JButton("Anular generacion (si no tiene notas)");
    private final JButton iniciarEscaneo=new JButton("Activar escaneo desde celular");
    private final JButton cerrarEscaneo=new JButton("Detener escaneo");
    private final JTextArea salida=new JTextArea(5,30);
    private ServidorEscaneo servidor;
    private boolean actualizando=false;
    public PantallaImpresos(Window owner,int docente){
        super(owner,"Examenes impresos | Sistema de Evaluacion",ModalityType.APPLICATION_MODAL);
        this.docente=docente;setSize(995,710);setMinimumSize(new Dimension(800,540));setLocationRelativeTo(owner);
        setLayout(new BorderLayout(12,12));((JComponent)getContentPane()).setBorder(new EmptyBorder(16,17,16,17));
        getContentPane().setBackground(new Color(244,247,245));
        JLabel title=new JLabel("Examenes impresos, QR y escaneo movil");title.setFont(new Font("SansSerif",Font.BOLD,21));
        JLabel subt=new JLabel("Selecciona un examen existente. Se creara un ejemplar diferente para cada matriculado.");
        subt.setForeground(new Color(79,96,86));JPanel header=new JPanel(new GridLayout(2,1));header.setOpaque(false);header.add(title);header.add(subt);add(header,BorderLayout.NORTH);
        JPanel centro=new JPanel(new GridLayout(1,2,14,0));centro.setOpaque(false);
        JPanel izq=caja();izq.setLayout(new BorderLayout(6,7));
        JPanel sel=new JPanel(new GridLayout(4,1,2,3));sel.setOpaque(false);
        sel.add(etiqueta("Curso asignado al docente"));sel.add(cursos);sel.add(etiqueta("Examen previamente creado"));sel.add(examenes);izq.add(sel,BorderLayout.NORTH);
        JPanel estudiantes=new JPanel(new BorderLayout(6,5));estudiantes.setOpaque(false);
        estudiantes.add(etiqueta("Matriculados (Ctrl / Shift para seleccionar solo algunos)"),BorderLayout.NORTH);
        alumnos.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        alumnos.setVisibleRowCount(10);alumnos.setBorder(new EmptyBorder(4,4,4,4));estudiantes.add(new JScrollPane(alumnos),BorderLayout.CENTER);
        JButton todos=new JButton("Seleccionar todos los matriculados");todos.addActionListener(e->{if(modeloAlumnos.size()>0)alumnos.setSelectionInterval(0,modeloAlumnos.size()-1);});
        estudiantes.add(todos,BorderLayout.SOUTH);izq.add(estudiantes,BorderLayout.CENTER);
        JPanel config=new JPanel(new GridLayout(3,1,3,4));config.setOpaque(false);preguntas.setOpaque(false);alternativas.setOpaque(false);
        config.add(preguntas);config.add(alternativas);config.add(generar);izq.add(config,BorderLayout.SOUTH);
        JPanel der=caja();der.setLayout(new BorderLayout(6,7));
        JPanel hist=new JPanel(new BorderLayout(5,5));hist.setOpaque(false);
        hist.add(etiqueta("Generaciones anteriores (reimprimibles sin volver a mezclar)"),BorderLayout.NORTH);
        lotes.setVisibleRowCount(9);hist.add(new JScrollPane(lotes),BorderLayout.CENTER);
        JPanel acciones=new JPanel(new GridLayout(4,1,4,6));acciones.setOpaque(false);acciones.add(volverAbrir);acciones.add(anular);acciones.add(iniciarEscaneo);acciones.add(cerrarEscaneo);
        hist.add(acciones,BorderLayout.SOUTH);der.add(hist,BorderLayout.CENTER);
        salida.setLineWrap(true);salida.setWrapStyleWord(true);salida.setEditable(false);salida.setBackground(new Color(242,247,244));salida.setBorder(new EmptyBorder(8,9,8,9));
        der.add(new JScrollPane(salida),BorderLayout.SOUTH);
        centro.add(izq);centro.add(der);add(centro,BorderLayout.CENTER);
        cursos.addActionListener(e->cambiarCurso());examenes.addActionListener(e->cambiarExamen());
        generar.addActionListener(e->generarPdf());volverAbrir.addActionListener(e->reimprimir());
        iniciarEscaneo.addActionListener(e->iniciarServidor());cerrarEscaneo.addActionListener(e->detenerServidor());anular.addActionListener(e->anularSeleccionado());
        cerrarEscaneo.setEnabled(false);
        addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){detenerServidor();}});
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        cargar();
    }
    private static JPanel caja(){JPanel p=new JPanel();p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(222,232,226)),new EmptyBorder(12,12,12,12)));return p;}
    private static JLabel etiqueta(String s){JLabel l=new JLabel(s);l.setFont(new Font("SansSerif",Font.BOLD,12));return l;}
    private void info(String s){salida.append(s+"\n");salida.setCaretPosition(salida.getDocument().getLength());}
    private void cargar(){
        try {actualizando=true;for(var curso:repo.cursos(docente))cursos.addItem(curso);actualizando=false;cambiarCurso();}
        catch(Exception e){actualizando=false;error(e);}
    }
    private void cambiarCurso(){
        if(actualizando)return;
        actualizando=true;examenes.removeAllItems();modeloAlumnos.clear();modeloLotes.clear();
        try {
            var curso=(RepositorioImpresos.Curso)cursos.getSelectedItem();
            if(curso!=null){
                for(var x:repo.evaluaciones(curso.id(),docente))examenes.addItem(x);
                for(var x:repo.matriculados(curso.id(),docente))modeloAlumnos.addElement(x);
                if(modeloAlumnos.size()>0)alumnos.setSelectionInterval(0,modeloAlumnos.size()-1);
            }
        }catch(Exception e){error(e);}
        finally{actualizando=false;cambiarExamen();}
    }
    private void cambiarExamen(){
        if(actualizando)return;
        modeloLotes.clear();var eval=(RepositorioImpresos.Evaluacion)examenes.getSelectedItem();
        if(eval==null)return;
        try {for(var lote:repo.lotes(eval.id(),docente))modeloLotes.addElement(lote);}
        catch(Exception e){error(e);}
    }
    private void error(Exception e){JOptionPane.showMessageDialog(this,e.getMessage(),"No se pudo completar la operacion",JOptionPane.ERROR_MESSAGE);info("ERROR: "+e.getMessage());}
    private void generarPdf(){
        var ev=(RepositorioImpresos.Evaluacion)examenes.getSelectedItem();if(ev==null){info("Crea o selecciona primero un examen del curso.");return;}
        List<RepositorioImpresos.Alumno> seleccionados=alumnos.getSelectedValuesList();if(seleccionados.isEmpty()){info("Selecciona al menos un alumno.");return;}
        int resp=JOptionPane.showConfirmDialog(this,"Se generara una NUEVA version para "+seleccionados.size()+" alumno(s).\n"+
          "Reimprimir una version previa no requiere generar otra.\n¿Continuar?","Confirmar generacion",JOptionPane.YES_NO_OPTION);
        if(resp!=JOptionPane.YES_OPTION)return;
        List<String> dnis=seleccionados.stream().map(RepositorioImpresos.Alumno::dni).toList();
        generar.setEnabled(false);info("Generando "+dnis.size()+" ejemplares y preparando PDF...");
        new SwingWorker<Path,Void>(){
            long lote=0;
            @Override protected Path doInBackground() throws Exception {
                lote=repo.generar(ev.id(),docente,dnis,preguntas.isSelected(),alternativas.isSelected());
                try {
                    Path ruta=PdfExamenes.generar(repo.cargarLote(lote,docente));repo.marcarPdf(lote,docente,ruta);return ruta;
                }catch(Exception e){repo.errorPdf(lote,docente);throw e;}
            }
            @Override protected void done(){
                generar.setEnabled(true);
                try {Path ruta=get();info("PDF listo: "+ruta);cambiarExamen();abrir(ruta);}
                catch(Exception e){error(new Exception(e.getCause()==null?e.getMessage():e.getCause().getMessage()));cambiarExamen();}
            }
        }.execute();
    }
    private void anularSeleccionado(){
        var lote=lotes.getSelectedValue();if(lote==null){info("Selecciona una generacion.");return;}
        if(JOptionPane.showConfirmDialog(this,"¿Anular la generacion "+lote.numero()+"? Las hojas impresas dejaran de poder escanearse. No se borran documentos ni historial.","Anular generacion",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
        try{repo.anularLote(lote.id(),docente);info("Generacion anulada: "+lote.id());cambiarExamen();}
        catch(Exception e){error(e);}
    }
    private void reimprimir(){
        var seleccionado=lotes.getSelectedValue();if(seleccionado==null){info("Selecciona una generacion anterior.");return;}
        try {
            var lote=repo.cargarLote(seleccionado.id(),docente);
            // Regenerar tambien lotes antiguos: mismo token y respuestas, PDF con QR grande.
            if("PREPARANDO".equals(seleccionado.estado())||"ANULADO".equals(seleccionado.estado()))throw new SQLException("El lote no esta disponible para imprimir.");
            Path ruta=PdfExamenes.generar(lote);
            repo.marcarPdf(seleccionado.id(),docente,ruta);
            cambiarExamen();
            abrir(ruta);
        }catch(Exception e){error(e);}
    }
    private void abrir(Path p)throws Exception{
        info("Archivo: "+p);
        if(Desktop.isDesktopSupported()&&Desktop.getDesktop().isSupported(Desktop.Action.OPEN))Desktop.getDesktop().open(p.toFile());
        else JOptionPane.showMessageDialog(this,"PDF guardado en:\n"+p);
    }
    /** QR para ABRIR el escaner desde el celular; no es el QR impreso en los examenes. */
    private static ImageIcon imagenQrAcceso(String url)throws Exception{
        BitMatrix matriz=new QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,260,260);
        BufferedImage imagen=new BufferedImage(matriz.getWidth(),matriz.getHeight(),BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<matriz.getHeight();y++)for(int x=0;x<matriz.getWidth();x++)
            imagen.setRGB(x,y,matriz.get(x,y)?0xFF162E28:0xFFFFFFFF);
        return new ImageIcon(imagen);
    }
    private void iniciarServidor(){
        try {
            servidor=new ServidorEscaneo(docente);
            String urlInicial=servidor.iniciar();
            iniciarEscaneo.setEnabled(false);cerrarEscaneo.setEnabled(true);
            info("Servidor movil activo en TCP 8787. Enlace inicial: "+urlInicial);
            mostrarAccesoCelular(urlInicial);
        }catch(Exception e){
            if(servidor!=null)servidor.close();servidor=null;
            if(e instanceof java.net.BindException)error(new Exception("El puerto 8787 esta ocupado o bloqueado. Cierra el programa que lo utiliza y vuelve a intentar. Detalle: "+e.getMessage()));
            else error(e);
        }
    }
    private void mostrarAccesoCelular(String urlInicial){
        JPanel panel=new JPanel(new BorderLayout(7,10));
        panel.setBorder(new EmptyBorder(8,8,8,8));
        JLabel titulo=new JLabel("Escanea ESTE QR con la camara del celular para abrir el escaner.");
        titulo.setFont(new Font("SansSerif",Font.BOLD,13));
        panel.add(titulo,BorderLayout.NORTH);

        JPanel contenido=new JPanel(new BorderLayout(8,8));
        JLabel qr=new JLabel("",SwingConstants.CENTER);qr.setOpaque(true);qr.setBackground(Color.WHITE);
        qr.setPreferredSize(new Dimension(275,280));
        contenido.add(qr,BorderLayout.CENTER);

        JPanel inferior=new JPanel(new GridLayout(0,1,4,5));
        inferior.add(etiqueta("IP del PC (elige la IP de tu Wi-Fi o Ethernet):"));
        java.util.List<String> ips=ServidorEscaneo.ipsLocales();
        JComboBox<String> selector=new JComboBox<>(ips.toArray(new String[0]));
        selector.setEditable(true);
        String ipInicial=URI.create(urlInicial).getHost();
        selector.setSelectedItem(ipInicial);
        inferior.add(selector);
        JTextField enlace=new JTextField(urlInicial);enlace.setEditable(false);
        inferior.add(etiqueta("Enlace que debe abrirse en el navegador del telefono:"));
        inferior.add(enlace);
        JPanel botones=new JPanel(new GridLayout(1,3,5,0));
        JButton actualizar=new JButton("Actualizar QR");
        JButton copiar=new JButton("Copiar enlace");
        JButton probar=new JButton("Probar en PC");
        botones.add(actualizar);botones.add(copiar);botones.add(probar);
        inferior.add(botones);
        JTextArea ayuda=new JTextArea(
          "Si no abre: revisa que sea la IP real de Wi-Fi (ipconfig en Windows).\n"
         +"Permite Java TCP 8787 en Firewall de Windows, red Privada.\n"
         +"Si en la PC abre y en el celular no, revisa Firewall, IP y aislamiento Wi-Fi.\n"
         +"El QR impreso en el examen es DISTINTO: solo identifica la hoja.");
        ayuda.setLineWrap(true);ayuda.setWrapStyleWord(true);ayuda.setOpaque(false);
        ayuda.setEditable(false);ayuda.setFont(new Font("SansSerif",Font.PLAIN,12));
        ayuda.setRows(4);inferior.add(ayuda);
        contenido.add(inferior,BorderLayout.SOUTH);
        panel.add(contenido,BorderLayout.CENTER);

        Runnable generar=()->{
            try{
                String ip=Objects.toString(selector.getSelectedItem(),"").trim();
                String url=servidor.urlConIP(ip);
                enlace.setText(url);qr.setIcon(imagenQrAcceso(url));qr.setText("");
            }catch(Exception err){
                qr.setIcon(null);qr.setText("IP incorrecta");
                JOptionPane.showMessageDialog(this,err.getMessage(),"Revisar IP",JOptionPane.WARNING_MESSAGE);
            }
        };
        actualizar.addActionListener(e->generar.run());
        copiar.addActionListener(e->{
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(enlace.getText()),null);
            info("Enlace de acceso copiado. No lo compartas con estudiantes.");
        });
        probar.addActionListener(e->{
            try{Desktop.getDesktop().browse(URI.create(enlace.getText()));}
            catch(Exception err){error(new Exception("No se pudo abrir el navegador: "+err.getMessage()));}
        });
        generar.run();
        JOptionPane.showMessageDialog(this,panel,"Acceso al escaner desde celular",JOptionPane.PLAIN_MESSAGE);
        info("Si necesitas cambiar la IP, detén y vuelve a activar el escaner. El servidor sigue disponible hasta detenerlo.");
    }
    private void detenerServidor(){
        if(servidor!=null){servidor.close();servidor=null;info("Escaneo movil detenido.");}
        iniciarEscaneo.setEnabled(true);cerrarEscaneo.setEnabled(false);
    }
}
