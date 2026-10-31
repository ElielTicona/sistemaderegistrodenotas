package Impresos;

import Conexion.Conexion;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;

/** Operaciones transaccionales de ejemplares impresos, sin alterar el flujo digital. */
public final class RepoImpresos {
    public record Curso(int id, String nombre) { public String toString(){return nombre;} }
    public record Evaluacion(int id, String nombre){ public String toString(){return nombre;} }
    public record Alumno(String dni,String codigo,String nombre){ public String toString(){return nombre+"  ("+(codigo==null?dni:codigo)+")";} }
    public record Opcion(int id,String texto,boolean correcta,String letra){}
    public record Pregunta(long id,long idOriginal,int numero,String texto,double puntos,List<Opcion> opciones){}
    public record Ejemplar(long id,String token,String codigo,String dni,String nombre,String codigoUniv,List<Pregunta> preguntas){}
    public record Lote(long id,int evaluacion,int numero,String titulo,String curso,List<Ejemplar> ejemplares,String ruta){}
    public record LoteListado(long id,int numero,String titulo,String estado,String ruta,int alumnos) {
        public String toString(){return "Generacion "+numero+" | "+alumnos+" alumnos | "+estado+" | ID "+id;}
    }
    public record ResultadoMarca(long pregunta,int numero,String detectada,String estado,double confianza,String confirmada,double puntos,String correcta){}
    public record Revision(long escaneo,long ejemplar,String alumno,String codigo,String examen,String advertencia,List<ResultadoMarca> respuestas){}
    public record ResultadoNota(String alumno,String codigo,double nota,double maximo,int correctas,int total){}

    private static Connection conectar() throws SQLException {
        Connection crisC=Conexion.conectar();
        if(crisC==null) throw new SQLException("No se puede conectar a bd_evalinte. Verifica MariaDB y Conexion.java");
        return crisC;
    }
    public List<Curso> cursos(int crisDocente) throws SQLException {
        List<Curso> crisOut=new ArrayList<>();
        try(Connection crisC=conectar(); PreparedStatement crisP=crisC.prepareStatement(
          "SELECT c.id_curso,c.nombre_curso FROM cursos c JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE c.id_docente=? AND c.estado=1 ORDER BY c.nombre_curso")){
            crisP.setInt(1,crisDocente); try(ResultSet crisR=crisP.executeQuery()){while(crisR.next())crisOut.add(new Curso(crisR.getInt(1),crisR.getString(2)));}
        }
        return crisOut;
    }
    public List<Evaluacion> evaluaciones(int crisCurso,int crisDocente) throws SQLException {
        List<Evaluacion> crisOut=new ArrayList<>();
        try(Connection crisC=conectar();PreparedStatement crisP=crisC.prepareStatement(
          "SELECT e.id_evaluacion,e.titulo FROM evaluaciones e JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE c.id_docente=? AND c.id_curso=? AND e.tipo='EXAMEN' AND e.estado=1 ORDER BY e.id_evaluacion DESC")){
            crisP.setInt(1,crisDocente);crisP.setInt(2,crisCurso);try(ResultSet crisR=crisP.executeQuery()){while(crisR.next())crisOut.add(new Evaluacion(crisR.getInt(1),crisR.getString(2)));}
        }
        return crisOut;
    }
    public List<Alumno> matriculados(int crisCurso,int crisDocente) throws SQLException {
        List<Alumno> crisOut=new ArrayList<>();
        try(Connection crisC=conectar();PreparedStatement crisP=crisC.prepareStatement(
          "SELECT DISTINCT e.dni,e.codigo_univ,e.nombre_completo FROM estudiantes e JOIN matricula m ON m.dni_estudiante=e.dni JOIN cursos c ON c.id_curso=m.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE c.id_curso=? AND c.id_docente=? AND e.estado=1 ORDER BY e.nombre_completo")){
            crisP.setInt(1,crisCurso);crisP.setInt(2,crisDocente);try(ResultSet crisR=crisP.executeQuery()){while(crisR.next())crisOut.add(new Alumno(crisR.getString(1),crisR.getString(2),crisR.getString(3)));}
        }
        return crisOut;
    }
    public List<LoteListado> lotes(int crisEval,int crisDocente) throws SQLException {
        List<LoteListado> crisOut=new ArrayList<>();
        try(Connection crisC=conectar();PreparedStatement crisP=crisC.prepareStatement(
          "SELECT l.id_lote,l.numero_generacion,e.titulo,l.estado,l.ruta_pdf,COUNT(x.id_ejemplar) FROM lotes_examen_impreso l JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 LEFT JOIN ejemplares_examen x ON x.id_lote=l.id_lote WHERE l.id_evaluacion=? AND c.id_docente=? GROUP BY l.id_lote,l.numero_generacion,e.titulo,l.estado,l.ruta_pdf ORDER BY l.id_lote DESC")){
            crisP.setInt(1,crisEval);crisP.setInt(2,crisDocente);try(ResultSet crisR=crisP.executeQuery()){while(crisR.next())crisOut.add(new LoteListado(crisR.getLong(1),crisR.getInt(2),crisR.getString(3),crisR.getString(4),crisR.getString(5),crisR.getInt(6)));}
        }
        return crisOut;
    }
    /** Anulacion explicita: nunca destruye notas ni ejemplares ya calificados. */
    public void anularLote(long crisIdLote,int crisDocente)throws SQLException{
        try(Connection crisC=conectar()){
            crisC.setAutoCommit(false);
            try{
                try(PreparedStatement crisP=crisC.prepareStatement("SELECT l.id_lote,l.estado FROM lotes_examen_impreso l JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE l.id_lote=? AND c.id_docente=? FOR UPDATE")){
                    crisP.setLong(1,crisIdLote);crisP.setInt(2,crisDocente);
                    try(ResultSet crisR=crisP.executeQuery()){if(!crisR.next())throw new SQLException("Lote no autorizado.");if("ANULADO".equals(crisR.getString(2)))throw new SQLException("La generacion ya esta anulada.");}
                }
                try(PreparedStatement crisP=crisC.prepareStatement("SELECT COUNT(*) FROM ejemplares_examen WHERE id_lote=? AND estado='CALIFICADO'")){
                    crisP.setLong(1,crisIdLote);try(ResultSet crisR=crisP.executeQuery()){crisR.next();if(crisR.getInt(1)>0)throw new SQLException("Hay examenes calificados en la generacion. No se puede anular el lote completo.");}
                }
                try(PreparedStatement crisP=crisC.prepareStatement("UPDATE ejemplares_examen SET estado='ANULADO' WHERE id_lote=? AND estado='PENDIENTE'")){crisP.setLong(1,crisIdLote);crisP.executeUpdate();}
                try(PreparedStatement crisP=crisC.prepareStatement("UPDATE lotes_examen_impreso SET estado='ANULADO' WHERE id_lote=?")){crisP.setLong(1,crisIdLote);crisP.executeUpdate();}
                crisC.commit();
            }catch(Exception crisEx){crisC.rollback();if(crisEx instanceof SQLException x)throw x;throw new SQLException(crisEx);}
        }
    }
    private void validarDoc(Connection crisC,int crisEval,int crisDocente,boolean crisBloquear) throws SQLException {
        String crisSql="SELECT e.id_evaluacion FROM evaluaciones e JOIN cursos c ON c.id_curso=e.id_curso AND c.estado=1 JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE e.id_evaluacion=? AND e.tipo='EXAMEN' AND e.estado=1 AND c.id_docente=?"+(crisBloquear?" FOR UPDATE":"");
        try(PreparedStatement crisP=crisC.prepareStatement(crisSql)){
            crisP.setInt(1,crisEval);crisP.setInt(2,crisDocente);try(ResultSet crisR=crisP.executeQuery()){if(!crisR.next())throw new SQLException("El examen no pertenece a un curso activo del docente.");}
        }
    }
    private record PreguntaBase(int id,String texto,double puntos,List<Opcion> opciones){}
    private List<PreguntaBase> pregOrigen(Connection crisC,int crisEval) throws SQLException {
        List<PreguntaBase> crisOut=new ArrayList<>();
        try(PreparedStatement crisP=crisC.prepareStatement("SELECT id_pregunta,texto_pregunta,puntaje_max FROM preguntas WHERE id_evaluacion=? ORDER BY id_pregunta")){
            crisP.setInt(1,crisEval);try(ResultSet crisR=crisP.executeQuery()){
                while(crisR.next()){
                    int crisId=crisR.getInt(1);String crisTexto=crisR.getString(2);double crisPuntos=crisR.getDouble(3);
                    List<Opcion> crisOps=new ArrayList<>();
                    try(PreparedStatement crisPo=crisC.prepareStatement("SELECT id_opcion,texto_opcion,es_correcta FROM opciones WHERE id_pregunta=? ORDER BY id_opcion")){
                        crisPo.setInt(1,crisId);try(ResultSet crisRo=crisPo.executeQuery()){
                            while(crisRo.next())crisOps.add(new Opcion(crisRo.getInt(1),crisRo.getString(2),crisRo.getBoolean(3),""));
                        }
                    }
                    if(crisOps.size()!=4 || crisOps.stream().filter(Opcion::correcta).count()!=1) throw new SQLException("La pregunta ID "+crisId+" debe contener cuatro opciones y una sola correcta.");
                    if(crisTexto==null||crisTexto.isBlank()||crisPuntos<=0)throw new SQLException("La pregunta ID "+crisId+" tiene enunciado o puntaje invalido.");
                    crisOut.add(new PreguntaBase(crisId,crisTexto,crisPuntos,crisOps));
                }
            }
        }
        if(crisOut.isEmpty())throw new SQLException("El examen no tiene preguntas.");
        if(crisOut.size()>30)throw new SQLException("Para la hoja de respuestas media A4 se admiten hasta 30 preguntas. Divide la evaluacion en dos examenes.");
        return crisOut;
    }
    private static final SecureRandom crisRandom=new SecureRandom();
    /** 60 bits aleatorios, sin datos personales ni clave en el QR. Version 1 QR alfanumerico. */
    private static String crearTokenQR(){
        final String crisAlfabeto="0123456789ABCDEFGHJKMNPQRSTVWXYZ";
        StringBuilder crisCod=new StringBuilder("E3");
        for(int crisI=0;crisI<12;crisI++)crisCod.append(crisAlfabeto.charAt(crisRandom.nextInt(crisAlfabeto.length())));
        return crisCod.toString();
    }

    public long generar(int crisEval,int crisDocente,List<String> crisDnis,boolean crisMezclaPreg,boolean crisMezclaOps) throws SQLException {
        if(crisDnis==null||crisDnis.isEmpty())throw new SQLException("Selecciona al menos un estudiante.");
        if(new HashSet<>(crisDnis).size()!=crisDnis.size())throw new SQLException("Hay estudiantes repetidos.");
        try(Connection crisC=conectar()){
            crisC.setAutoCommit(false);
            try {
                validarDoc(crisC,crisEval,crisDocente,true);
                List<PreguntaBase> crisBase=pregOrigen(crisC,crisEval);
                // No generar impresos imposibles de calificar por una nota ya existente.
                try(PreparedStatement crisP=crisC.prepareStatement("SELECT 1 FROM notas_finales WHERE dni_estudiante=? AND id_evaluacion=? LIMIT 1")){
                    for(String crisDni:crisDnis){
                        crisP.setString(1,crisDni);crisP.setInt(2,crisEval);
                        try(ResultSet crisR=crisP.executeQuery()){if(crisR.next())throw new SQLException("El alumno "+crisDni+" ya tiene una nota en esta evaluacion. Crea una evaluacion nueva o regulariza la nota previamente.");}
                    }
                }
                int crisGeneracion=1;
                try(PreparedStatement crisP=crisC.prepareStatement("SELECT COALESCE(MAX(numero_generacion),0)+1 FROM lotes_examen_impreso WHERE id_evaluacion=?")){
                    crisP.setInt(1,crisEval);try(ResultSet crisR=crisP.executeQuery()){if(crisR.next())crisGeneracion=crisR.getInt(1);}
                }
                long crisLote;
                try(PreparedStatement crisP=crisC.prepareStatement("INSERT INTO lotes_examen_impreso(id_evaluacion,id_docente,numero_generacion,mezclar_preguntas,mezclar_opciones) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                    crisP.setInt(1,crisEval);crisP.setInt(2,crisDocente);crisP.setInt(3,crisGeneracion);crisP.setBoolean(4,crisMezclaPreg);crisP.setBoolean(5,crisMezclaOps);crisP.executeUpdate();
                    try(ResultSet crisR=crisP.getGeneratedKeys()){crisR.next();crisLote=crisR.getLong(1);}
                }
                for(String crisDni:crisDnis){
                    Alumno crisAl;
                    try(PreparedStatement crisP=crisC.prepareStatement("SELECT s.dni,s.codigo_univ,s.nombre_completo FROM estudiantes s JOIN matricula m ON m.dni_estudiante=s.dni JOIN evaluaciones e ON e.id_curso=m.id_curso WHERE e.id_evaluacion=? AND s.dni=? AND s.estado=1 LIMIT 1")){
                        crisP.setInt(1,crisEval);crisP.setString(2,crisDni);
                        try(ResultSet crisR=crisP.executeQuery()){if(!crisR.next())throw new SQLException("Estudiante no matriculado: "+crisDni);crisAl=new Alumno(crisR.getString(1),crisR.getString(2),crisR.getString(3));}
                    }
                    long crisEjemplar;String crisCod="EV-"+crisLote+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
                    String crisToken=crearTokenQR();
                    try(PreparedStatement crisP=crisC.prepareStatement("INSERT INTO ejemplares_examen(id_lote,dni_estudiante,codigo_visible,token_qr,nombre_impreso,codigo_univ_impreso) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                        crisP.setLong(1,crisLote);crisP.setString(2,crisDni);crisP.setString(3,crisCod);crisP.setString(4,crisToken);crisP.setString(5,crisAl.nombre());crisP.setString(6,crisAl.codigo());crisP.executeUpdate();
                        try(ResultSet crisR=crisP.getGeneratedKeys()){crisR.next();crisEjemplar=crisR.getLong(1);}
                    }
                    List<PreguntaBase> crisOrden=new ArrayList<>(crisBase);
                    if(crisMezclaPreg)Collections.shuffle(crisOrden,crisRandom);
                    int crisNum=0;
                    for(PreguntaBase crisPb:crisOrden){
                        long crisIdEp;
                        try(PreparedStatement crisP=crisC.prepareStatement("INSERT INTO ejemplar_preguntas(id_ejemplar,id_pregunta,numero_impreso,enunciado_impreso,puntaje_impreso) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                            crisP.setLong(1,crisEjemplar);crisP.setInt(2,crisPb.id());crisP.setInt(3,++crisNum);crisP.setString(4,crisPb.texto());crisP.setDouble(5,crisPb.puntos());crisP.executeUpdate();
                            try(ResultSet crisR=crisP.getGeneratedKeys()){crisR.next();crisIdEp=crisR.getLong(1);}
                        }
                        List<Opcion> crisOpciones=new ArrayList<>(crisPb.opciones());if(crisMezclaOps)Collections.shuffle(crisOpciones,crisRandom);
                        for(int crisI=0;crisI<crisOpciones.size();crisI++){
                            Opcion crisOp=crisOpciones.get(crisI);
                            try(PreparedStatement crisP=crisC.prepareStatement("INSERT INTO ejemplar_opciones(id_ejemplar_pregunta,id_opcion,letra_impresa,texto_impreso,es_correcta) VALUES(?,?,?,?,?)")){
                                crisP.setLong(1,crisIdEp);crisP.setInt(2,crisOp.id());crisP.setString(3,"ABCD".substring(crisI,crisI+1));crisP.setString(4,crisOp.texto());crisP.setBoolean(5,crisOp.correcta());crisP.executeUpdate();
                            }
                        }
                    }
                }
                crisC.commit();return crisLote;
            }catch(Exception crisEx){crisC.rollback();if(crisEx instanceof SQLException s)throw s;throw new SQLException("No se pudo generar el lote: "+crisEx.getMessage(),crisEx);}
        }
    }
    public Lote cargarLote(long crisLote,int crisDocente) throws SQLException {
        try(Connection crisC=conectar()){
            int crisEval,crisNum;String crisTitulo,crisCurso,crisRuta;
            try(PreparedStatement crisP=crisC.prepareStatement("SELECT l.id_evaluacion,l.numero_generacion,e.titulo,c.nombre_curso,l.ruta_pdf FROM lotes_examen_impreso l JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE l.id_lote=? AND c.id_docente=? AND l.estado<>'ANULADO'")){
                crisP.setLong(1,crisLote);crisP.setInt(2,crisDocente);try(ResultSet crisR=crisP.executeQuery()){
                    if(!crisR.next())throw new SQLException("Lote inexistente, anulado o no autorizado.");
                    crisEval=crisR.getInt(1);crisNum=crisR.getInt(2);crisTitulo=crisR.getString(3);crisCurso=crisR.getString(4);crisRuta=crisR.getString(5);
                }
            }
            List<Ejemplar> crisEjemplares=new ArrayList<>();
            try(PreparedStatement crisP=crisC.prepareStatement("SELECT id_ejemplar,token_qr,codigo_visible,dni_estudiante,nombre_impreso,codigo_univ_impreso FROM ejemplares_examen WHERE id_lote=? ORDER BY nombre_impreso,id_ejemplar")){
                crisP.setLong(1,crisLote);try(ResultSet crisR=crisP.executeQuery()){
                    while(crisR.next()){
                        long crisId=crisR.getLong(1);
                        crisEjemplares.add(new Ejemplar(crisId,crisR.getString(2),crisR.getString(3),crisR.getString(4),crisR.getString(5),crisR.getString(6),cargarPreg(crisC,crisId)));
                    }
                }
            }
            return new Lote(crisLote,crisEval,crisNum,crisTitulo,crisCurso,crisEjemplares,crisRuta);
        }
    }
    private List<Pregunta> cargarPreg(Connection crisC,long crisEjemplar) throws SQLException {
        List<Pregunta> crisOut=new ArrayList<>();
        try(PreparedStatement crisP=crisC.prepareStatement("SELECT id_ejemplar_pregunta,COALESCE(id_pregunta,0),numero_impreso,enunciado_impreso,puntaje_impreso FROM ejemplar_preguntas WHERE id_ejemplar=? ORDER BY numero_impreso")){
            crisP.setLong(1,crisEjemplar);try(ResultSet crisR=crisP.executeQuery()){
                while(crisR.next()){
                    long crisId=crisR.getLong(1);List<Opcion> crisOps=new ArrayList<>();
                    try(PreparedStatement crisPo=crisC.prepareStatement("SELECT COALESCE(id_opcion,0),texto_impreso,es_correcta,letra_impresa FROM ejemplar_opciones WHERE id_ejemplar_pregunta=? ORDER BY letra_impresa")){
                        crisPo.setLong(1,crisId);try(ResultSet crisRo=crisPo.executeQuery()){while(crisRo.next())crisOps.add(new Opcion(crisRo.getInt(1),crisRo.getString(2),crisRo.getBoolean(3),crisRo.getString(4)));}
                    }
                    crisOut.add(new Pregunta(crisId,crisR.getLong(2),crisR.getInt(3),crisR.getString(4),crisR.getDouble(5),crisOps));
                }
            }
        }
        return crisOut;
    }
    public void marcarPdf(long crisLote,int crisDocente,Path crisRuta) throws SQLException {
        try(Connection crisC=conectar();PreparedStatement crisP=crisC.prepareStatement("UPDATE lotes_examen_impreso l JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso SET l.ruta_pdf=?,l.estado='LISTO' WHERE l.id_lote=? AND c.id_docente=? AND l.estado IN ('PREPARANDO','ERROR','LISTO')")){
            crisP.setString(1,crisRuta.toAbsolutePath().toString());crisP.setLong(2,crisLote);crisP.setInt(3,crisDocente);if(crisP.executeUpdate()!=1)throw new SQLException("No se pudo registrar PDF.");
        }
    }
    public void errorPdf(long crisLote,int crisDocente){
        try(Connection crisC=conectar();PreparedStatement crisP=crisC.prepareStatement("UPDATE lotes_examen_impreso SET estado='ERROR' WHERE id_lote=? AND id_docente=? AND estado='PREPARANDO'")){
            crisP.setLong(1,crisLote);crisP.setInt(2,crisDocente);crisP.executeUpdate();
        }catch(SQLException crisIgnored){}
    }
    public record Identificacion(long ejemplar,long lote,String alumno,String codigo,String token,int evaluacion){}
    public Identificacion identificar(String crisToken,String crisCod,int crisDocente) throws SQLException {
        if((crisToken==null||crisToken.isBlank())&&(crisCod==null||crisCod.isBlank()))throw new SQLException("QR no legible. Introduce el codigo visible de la hoja.");
        String crisSql="SELECT x.id_ejemplar,x.id_lote,x.nombre_impreso,x.codigo_visible,x.token_qr,e.id_evaluacion FROM ejemplares_examen x JOIN lotes_examen_impreso l ON l.id_lote=x.id_lote JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE c.id_docente=? AND l.estado='LISTO' AND x.estado<>'ANULADO' AND "+((crisToken!=null&&!crisToken.isBlank())?"x.token_qr=?":"x.codigo_visible=?");
        try(Connection crisC=conectar();PreparedStatement crisP=crisC.prepareStatement(crisSql)){
            crisP.setInt(1,crisDocente);crisP.setString(2,crisToken!=null&&!crisToken.isBlank()?crisToken:crisCod);
            try(ResultSet crisR=crisP.executeQuery()){
                if(!crisR.next())throw new SQLException("La hoja no pertenece a un examen habilitado de este docente.");
                return new Identificacion(crisR.getLong(1),crisR.getLong(2),crisR.getString(3),crisR.getString(4),crisR.getString(5),crisR.getInt(6));
            }
        }
    }
    public long registrarEscan(long crisEjemplar,int crisDocente,String crisArch,String crisOcr,String crisAviso,List<DetectorOMR.Marca> crisMarcas) throws SQLException {
        try(Connection crisC=conectar()){
            crisC.setAutoCommit(false);
            try {
                try(PreparedStatement crisP=crisC.prepareStatement("SELECT x.estado FROM ejemplares_examen x JOIN lotes_examen_impreso l ON l.id_lote=x.id_lote JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE x.id_ejemplar=? AND c.id_docente=? AND l.estado='LISTO' FOR UPDATE")){
                    crisP.setLong(1,crisEjemplar);crisP.setInt(2,crisDocente);try(ResultSet crisR=crisP.executeQuery()){if(!crisR.next()||!"PENDIENTE".equals(crisR.getString(1)))throw new SQLException("Este ejemplar ya fue calificado, anulado o no esta disponible.");}
                }
                long crisScan;
                try(PreparedStatement crisP=crisC.prepareStatement("INSERT INTO escaneos_examen(id_ejemplar,id_docente,ruta_imagen_original,texto_ocr,advertencia) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                    crisP.setLong(1,crisEjemplar);crisP.setInt(2,crisDocente);crisP.setString(3,crisArch);crisP.setString(4,crisOcr);crisP.setString(5,crisAviso);crisP.executeUpdate();
                    try(ResultSet crisR=crisP.getGeneratedKeys()){crisR.next();crisScan=crisR.getLong(1);}
                }
                for(DetectorOMR.Marca crisM:crisMarcas){
                    try(PreparedStatement crisP=crisC.prepareStatement("INSERT INTO respuestas_escaneadas(id_escaneo,id_ejemplar_pregunta,letra_detectada,estado_lectura,confianza,letra_confirmada) VALUES(?,?,?,?,?,?)")){
                        crisP.setLong(1,crisScan);crisP.setLong(2,crisM.idPregunta());crisP.setString(3,crisM.letra());crisP.setString(4,crisM.estado());crisP.setDouble(5,crisM.confianza());crisP.setString(6,"VALIDA".equals(crisM.estado())?crisM.letra():null);crisP.executeUpdate();
                    }
                }
                crisC.commit();return crisScan;
            }catch(Exception crisEx){crisC.rollback();if(crisEx instanceof SQLException s)throw s;throw new SQLException(crisEx);}
        }
    }
    public Revision revision(long crisEscaneo,int crisDocente) throws SQLException {
        String crisSql="SELECT s.id_ejemplar,x.nombre_impreso,x.codigo_visible,e.titulo,s.advertencia FROM escaneos_examen s JOIN ejemplares_examen x ON x.id_ejemplar=s.id_ejemplar JOIN lotes_examen_impreso l ON l.id_lote=x.id_lote JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE s.id_escaneo=? AND c.id_docente=? AND s.estado='REVISION'";
        try(Connection crisC=conectar();PreparedStatement crisP=crisC.prepareStatement(crisSql)){
            crisP.setLong(1,crisEscaneo);crisP.setInt(2,crisDocente);try(ResultSet crisR=crisP.executeQuery()){
                if(!crisR.next())throw new SQLException("Revision no encontrada o ya confirmada.");
                long crisEjemplar=crisR.getLong(1);String crisNom=crisR.getString(2),crisCod=crisR.getString(3),crisTitulo=crisR.getString(4),crisAviso=crisR.getString(5);
                List<ResultadoMarca> crisResult=new ArrayList<>();
                try(PreparedStatement crisPo=crisC.prepareStatement("SELECT ep.id_ejemplar_pregunta,ep.numero_impreso,re.letra_detectada,re.estado_lectura,re.confianza,re.letra_confirmada,ep.puntaje_impreso,(SELECT letra_impresa FROM ejemplar_opciones WHERE id_ejemplar_pregunta=ep.id_ejemplar_pregunta AND es_correcta=1 LIMIT 1) correcta FROM respuestas_escaneadas re JOIN ejemplar_preguntas ep ON ep.id_ejemplar_pregunta=re.id_ejemplar_pregunta WHERE re.id_escaneo=? AND ep.id_ejemplar=? ORDER BY ep.numero_impreso")){
                    crisPo.setLong(1,crisEscaneo);crisPo.setLong(2,crisEjemplar);try(ResultSet crisRr=crisPo.executeQuery()){
                        while(crisRr.next())crisResult.add(new ResultadoMarca(crisRr.getLong(1),crisRr.getInt(2),crisRr.getString(3),crisRr.getString(4),crisRr.getDouble(5),crisRr.getString(6),crisRr.getDouble(7),crisRr.getString(8)));
                    }
                }
                return new Revision(crisEscaneo,crisEjemplar,crisNom,crisCod,crisTitulo,crisAviso,crisResult);
            }
        }
    }
    /** Confirmar UNA SOLA vez: guarda respuestas revisadas, nota y estado en una transaccion. */
    public ResultadoNota confirmar(long crisEscaneo,int crisDocente,Map<Long,String> crisConfirmadas) throws SQLException {
        try(Connection crisC=conectar()){
            crisC.setAutoCommit(false);
            try {
                long crisEjemplar;String crisAlumno,crisCod,crisDni;int crisEval;
                try(PreparedStatement crisP=crisC.prepareStatement("SELECT x.id_ejemplar,x.nombre_impreso,x.codigo_visible,x.dni_estudiante,e.id_evaluacion,x.estado FROM escaneos_examen s JOIN ejemplares_examen x ON x.id_ejemplar=s.id_ejemplar JOIN lotes_examen_impreso l ON l.id_lote=x.id_lote JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE s.id_escaneo=? AND s.id_docente=? AND s.estado='REVISION' AND l.estado='LISTO' FOR UPDATE")){
                    crisP.setLong(1,crisEscaneo);crisP.setInt(2,crisDocente);try(ResultSet crisR=crisP.executeQuery()){
                        if(!crisR.next())throw new SQLException("Escaneo ya confirmado o no autorizado.");
                        crisEjemplar=crisR.getLong(1);crisAlumno=crisR.getString(2);crisCod=crisR.getString(3);crisDni=crisR.getString(4);crisEval=crisR.getInt(5);
                        if(!"PENDIENTE".equals(crisR.getString(6)))throw new SQLException("El ejemplar ya tiene nota.");
                    }
                }
                try(PreparedStatement crisP=crisC.prepareStatement("SELECT COUNT(*) FROM notas_finales WHERE dni_estudiante=? AND id_evaluacion=?")){
                    crisP.setString(1,crisDni);crisP.setInt(2,crisEval);try(ResultSet crisR=crisP.executeQuery()){
                        crisR.next();if(crisR.getInt(1)>0)throw new SQLException("El estudiante ya tiene nota en esta evaluacion. Revisa sus registros antes de confirmar.");
                    }
                }
                double crisNota=0,crisMax=0;int crisCorrectas=0,crisTotal=0;
                try(PreparedStatement crisP=crisC.prepareStatement("SELECT ep.id_ejemplar_pregunta,ep.puntaje_impreso,(SELECT letra_impresa FROM ejemplar_opciones eo WHERE eo.id_ejemplar_pregunta=ep.id_ejemplar_pregunta AND eo.es_correcta=1) respuesta FROM ejemplar_preguntas ep WHERE ep.id_ejemplar=? ORDER BY ep.numero_impreso")){
                    crisP.setLong(1,crisEjemplar);try(ResultSet crisR=crisP.executeQuery()){
                        while(crisR.next()){
                            long crisQ=crisR.getLong(1);double crisPuntos=crisR.getDouble(2);String crisCorrecta=crisR.getString(3);crisMax+=crisPuntos;crisTotal++;
                            if(!crisConfirmadas.containsKey(crisQ))throw new SQLException("Falta confirmar la respuesta de la pregunta "+crisTotal);
                            String crisLetra=crisConfirmadas.get(crisQ);if(crisLetra==null)crisLetra="";
                            if(!crisLetra.isBlank()&&!List.of("A","B","C","D").contains(crisLetra))throw new SQLException("Respuesta invalida para pregunta "+crisTotal);
                            if(crisLetra.equals(crisCorrecta)){crisNota+=crisPuntos;crisCorrectas++;}
                            try(PreparedStatement crisUpd=crisC.prepareStatement("UPDATE respuestas_escaneadas SET letra_confirmada=?,corregida_manualmente=IF(COALESCE(letra_detectada,'')<>?,1,corregida_manualmente) WHERE id_escaneo=? AND id_ejemplar_pregunta=?")){
                                crisUpd.setString(1,crisLetra.isBlank()?null:crisLetra);crisUpd.setString(2,crisLetra);crisUpd.setLong(3,crisEscaneo);crisUpd.setLong(4,crisQ);
                                if(crisUpd.executeUpdate()!=1)throw new SQLException("Respuesta de escaneo incompleta.");
                            }
                        }
                    }
                }
                if(crisTotal==0)throw new SQLException("El ejemplar no tiene preguntas.");
                try(PreparedStatement crisP=crisC.prepareStatement("INSERT INTO notas_finales(dni_estudiante,id_evaluacion,puntaje_obtenido,id_ejemplar) VALUES(?,?,?,?)")){
                    crisP.setString(1,crisDni);crisP.setInt(2,crisEval);crisP.setDouble(3,crisNota);crisP.setLong(4,crisEjemplar);crisP.executeUpdate();
                }
                try(PreparedStatement crisP=crisC.prepareStatement("UPDATE escaneos_examen SET estado='CONFIRMADO' WHERE id_escaneo=? AND estado='REVISION'")){
                    crisP.setLong(1,crisEscaneo);if(crisP.executeUpdate()!=1)throw new SQLException("Escaneo ya procesado.");
                }
                try(PreparedStatement crisP=crisC.prepareStatement("UPDATE ejemplares_examen SET estado='CALIFICADO' WHERE id_ejemplar=? AND estado='PENDIENTE'")){
                    crisP.setLong(1,crisEjemplar);if(crisP.executeUpdate()!=1)throw new SQLException("Ejemplar ya procesado.");
                }
                crisC.commit();return new ResultadoNota(crisAlumno,crisCod,crisNota,crisMax,crisCorrectas,crisTotal);
            }catch(Exception crisE){crisC.rollback();if(crisE instanceof SQLException s)throw s;throw new SQLException(crisE);}
        }
    }
}
