package Impresos;

import Conexion.Conexion;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;

/** Operaciones transaccionales de ejemplares impresos, sin alterar el flujo digital. */
public final class RepositorioImpresos {
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
        Connection c=Conexion.conectar();
        if(c==null) throw new SQLException("No se puede conectar a bd_evalinte. Verifica MariaDB y Conexion.java");
        return c;
    }
    public List<Curso> cursos(int docente) throws SQLException {
        List<Curso> out=new ArrayList<>();
        try(Connection c=conectar(); PreparedStatement p=c.prepareStatement(
          "SELECT c.id_curso,c.nombre_curso FROM cursos c JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE c.id_docente=? AND c.estado=1 ORDER BY c.nombre_curso")){
            p.setInt(1,docente); try(ResultSet r=p.executeQuery()){while(r.next())out.add(new Curso(r.getInt(1),r.getString(2)));}
        }
        return out;
    }
    public List<Evaluacion> evaluaciones(int curso,int docente) throws SQLException {
        List<Evaluacion> out=new ArrayList<>();
        try(Connection c=conectar();PreparedStatement p=c.prepareStatement(
          "SELECT e.id_evaluacion,e.titulo FROM evaluaciones e JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE c.id_docente=? AND c.id_curso=? AND e.tipo='EXAMEN' AND e.estado=1 ORDER BY e.id_evaluacion DESC")){
            p.setInt(1,docente);p.setInt(2,curso);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new Evaluacion(r.getInt(1),r.getString(2)));}
        }
        return out;
    }
    public List<Alumno> matriculados(int curso,int docente) throws SQLException {
        List<Alumno> out=new ArrayList<>();
        try(Connection c=conectar();PreparedStatement p=c.prepareStatement(
          "SELECT DISTINCT e.dni,e.codigo_univ,e.nombre_completo FROM estudiantes e JOIN matricula m ON m.dni_estudiante=e.dni JOIN cursos c ON c.id_curso=m.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE c.id_curso=? AND c.id_docente=? AND e.estado=1 ORDER BY e.nombre_completo")){
            p.setInt(1,curso);p.setInt(2,docente);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new Alumno(r.getString(1),r.getString(2),r.getString(3)));}
        }
        return out;
    }
    public List<LoteListado> lotes(int evaluacion,int docente) throws SQLException {
        List<LoteListado> out=new ArrayList<>();
        try(Connection c=conectar();PreparedStatement p=c.prepareStatement(
          "SELECT l.id_lote,l.numero_generacion,e.titulo,l.estado,l.ruta_pdf,COUNT(x.id_ejemplar) FROM lotes_examen_impreso l JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 LEFT JOIN ejemplares_examen x ON x.id_lote=l.id_lote WHERE l.id_evaluacion=? AND c.id_docente=? GROUP BY l.id_lote,l.numero_generacion,e.titulo,l.estado,l.ruta_pdf ORDER BY l.id_lote DESC")){
            p.setInt(1,evaluacion);p.setInt(2,docente);try(ResultSet r=p.executeQuery()){while(r.next())out.add(new LoteListado(r.getLong(1),r.getInt(2),r.getString(3),r.getString(4),r.getString(5),r.getInt(6)));}
        }
        return out;
    }
    /** Anulacion explicita: nunca destruye notas ni ejemplares ya calificados. */
    public void anularLote(long idLote,int docente)throws SQLException{
        try(Connection c=conectar()){
            c.setAutoCommit(false);
            try{
                try(PreparedStatement p=c.prepareStatement("SELECT l.id_lote,l.estado FROM lotes_examen_impreso l JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE l.id_lote=? AND c.id_docente=? FOR UPDATE")){
                    p.setLong(1,idLote);p.setInt(2,docente);
                    try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Lote no autorizado.");if("ANULADO".equals(r.getString(2)))throw new SQLException("La generacion ya esta anulada.");}
                }
                try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM ejemplares_examen WHERE id_lote=? AND estado='CALIFICADO'")){
                    p.setLong(1,idLote);try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(1)>0)throw new SQLException("Hay examenes calificados en la generacion. No se puede anular el lote completo.");}
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE ejemplares_examen SET estado='ANULADO' WHERE id_lote=? AND estado='PENDIENTE'")){p.setLong(1,idLote);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("UPDATE lotes_examen_impreso SET estado='ANULADO' WHERE id_lote=?")){p.setLong(1,idLote);p.executeUpdate();}
                c.commit();
            }catch(Exception ex){c.rollback();if(ex instanceof SQLException x)throw x;throw new SQLException(ex);}
        }
    }
    private void comprobarDocente(Connection c,int eval,int docente,boolean bloquear) throws SQLException {
        String sql="SELECT e.id_evaluacion FROM evaluaciones e JOIN cursos c ON c.id_curso=e.id_curso AND c.estado=1 JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE e.id_evaluacion=? AND e.tipo='EXAMEN' AND e.estado=1 AND c.id_docente=?"+(bloquear?" FOR UPDATE":"");
        try(PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,eval);p.setInt(2,docente);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("El examen no pertenece a un curso activo del docente.");}
        }
    }
    private record PreguntaBase(int id,String texto,double puntos,List<Opcion> opciones){}
    private List<PreguntaBase> preguntasOrigen(Connection c,int eval) throws SQLException {
        List<PreguntaBase> out=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement("SELECT id_pregunta,texto_pregunta,puntaje_max FROM preguntas WHERE id_evaluacion=? ORDER BY id_pregunta")){
            p.setInt(1,eval);try(ResultSet r=p.executeQuery()){
                while(r.next()){
                    int id=r.getInt(1);String texto=r.getString(2);double puntos=r.getDouble(3);
                    List<Opcion> ops=new ArrayList<>();
                    try(PreparedStatement po=c.prepareStatement("SELECT id_opcion,texto_opcion,es_correcta FROM opciones WHERE id_pregunta=? ORDER BY id_opcion")){
                        po.setInt(1,id);try(ResultSet ro=po.executeQuery()){
                            while(ro.next())ops.add(new Opcion(ro.getInt(1),ro.getString(2),ro.getBoolean(3),""));
                        }
                    }
                    if(ops.size()!=4 || ops.stream().filter(Opcion::correcta).count()!=1) throw new SQLException("La pregunta ID "+id+" debe contener cuatro opciones y una sola correcta.");
                    if(texto==null||texto.isBlank()||puntos<=0)throw new SQLException("La pregunta ID "+id+" tiene enunciado o puntaje invalido.");
                    out.add(new PreguntaBase(id,texto,puntos,ops));
                }
            }
        }
        if(out.isEmpty())throw new SQLException("El examen no tiene preguntas.");
        if(out.size()>30)throw new SQLException("Para la hoja de respuestas media A4 se admiten hasta 30 preguntas. Divide la evaluacion en dos examenes.");
        return out;
    }
    private static final SecureRandom RANDOM=new SecureRandom();
    /** 60 bits aleatorios, sin datos personales ni clave en el QR. Version 1 QR alfanumerico. */
    private static String nuevoTokenQR(){
        final String alfabeto="0123456789ABCDEFGHJKMNPQRSTVWXYZ";
        StringBuilder codigo=new StringBuilder("E3");
        for(int i=0;i<12;i++)codigo.append(alfabeto.charAt(RANDOM.nextInt(alfabeto.length())));
        return codigo.toString();
    }

    public long generar(int evaluacion,int docente,List<String> dnis,boolean mezclaPreg,boolean mezclaOps) throws SQLException {
        if(dnis==null||dnis.isEmpty())throw new SQLException("Selecciona al menos un estudiante.");
        if(new HashSet<>(dnis).size()!=dnis.size())throw new SQLException("Hay estudiantes repetidos.");
        try(Connection c=conectar()){
            c.setAutoCommit(false);
            try {
                comprobarDocente(c,evaluacion,docente,true);
                List<PreguntaBase> base=preguntasOrigen(c,evaluacion);
                // No generar impresos imposibles de calificar por una nota ya existente.
                try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM notas_finales WHERE dni_estudiante=? AND id_evaluacion=? LIMIT 1")){
                    for(String dni:dnis){
                        p.setString(1,dni);p.setInt(2,evaluacion);
                        try(ResultSet r=p.executeQuery()){if(r.next())throw new SQLException("El alumno "+dni+" ya tiene una nota en esta evaluacion. Crea una evaluacion nueva o regulariza la nota previamente.");}
                    }
                }
                int generacion=1;
                try(PreparedStatement p=c.prepareStatement("SELECT COALESCE(MAX(numero_generacion),0)+1 FROM lotes_examen_impreso WHERE id_evaluacion=?")){
                    p.setInt(1,evaluacion);try(ResultSet r=p.executeQuery()){if(r.next())generacion=r.getInt(1);}
                }
                long lote;
                try(PreparedStatement p=c.prepareStatement("INSERT INTO lotes_examen_impreso(id_evaluacion,id_docente,numero_generacion,mezclar_preguntas,mezclar_opciones) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                    p.setInt(1,evaluacion);p.setInt(2,docente);p.setInt(3,generacion);p.setBoolean(4,mezclaPreg);p.setBoolean(5,mezclaOps);p.executeUpdate();
                    try(ResultSet r=p.getGeneratedKeys()){r.next();lote=r.getLong(1);}
                }
                for(String dni:dnis){
                    Alumno al;
                    try(PreparedStatement p=c.prepareStatement("SELECT s.dni,s.codigo_univ,s.nombre_completo FROM estudiantes s JOIN matricula m ON m.dni_estudiante=s.dni JOIN evaluaciones e ON e.id_curso=m.id_curso WHERE e.id_evaluacion=? AND s.dni=? AND s.estado=1 LIMIT 1")){
                        p.setInt(1,evaluacion);p.setString(2,dni);
                        try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Estudiante no matriculado: "+dni);al=new Alumno(r.getString(1),r.getString(2),r.getString(3));}
                    }
                    long ejemplar;String codigo="EV-"+lote+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
                    String token=nuevoTokenQR();
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO ejemplares_examen(id_lote,dni_estudiante,codigo_visible,token_qr,nombre_impreso,codigo_univ_impreso) VALUES(?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                        p.setLong(1,lote);p.setString(2,dni);p.setString(3,codigo);p.setString(4,token);p.setString(5,al.nombre());p.setString(6,al.codigo());p.executeUpdate();
                        try(ResultSet r=p.getGeneratedKeys()){r.next();ejemplar=r.getLong(1);}
                    }
                    List<PreguntaBase> orden=new ArrayList<>(base);
                    if(mezclaPreg)Collections.shuffle(orden,RANDOM);
                    int num=0;
                    for(PreguntaBase pb:orden){
                        long idEp;
                        try(PreparedStatement p=c.prepareStatement("INSERT INTO ejemplar_preguntas(id_ejemplar,id_pregunta,numero_impreso,enunciado_impreso,puntaje_impreso) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                            p.setLong(1,ejemplar);p.setInt(2,pb.id());p.setInt(3,++num);p.setString(4,pb.texto());p.setDouble(5,pb.puntos());p.executeUpdate();
                            try(ResultSet r=p.getGeneratedKeys()){r.next();idEp=r.getLong(1);}
                        }
                        List<Opcion> opciones=new ArrayList<>(pb.opciones());if(mezclaOps)Collections.shuffle(opciones,RANDOM);
                        for(int i=0;i<opciones.size();i++){
                            Opcion op=opciones.get(i);
                            try(PreparedStatement p=c.prepareStatement("INSERT INTO ejemplar_opciones(id_ejemplar_pregunta,id_opcion,letra_impresa,texto_impreso,es_correcta) VALUES(?,?,?,?,?)")){
                                p.setLong(1,idEp);p.setInt(2,op.id());p.setString(3,"ABCD".substring(i,i+1));p.setString(4,op.texto());p.setBoolean(5,op.correcta());p.executeUpdate();
                            }
                        }
                    }
                }
                c.commit();return lote;
            }catch(Exception ex){c.rollback();if(ex instanceof SQLException s)throw s;throw new SQLException("No se pudo generar el lote: "+ex.getMessage(),ex);}
        }
    }
    public Lote cargarLote(long lote,int docente) throws SQLException {
        try(Connection c=conectar()){
            int eval,num;String titulo,curso,ruta;
            try(PreparedStatement p=c.prepareStatement("SELECT l.id_evaluacion,l.numero_generacion,e.titulo,c.nombre_curso,l.ruta_pdf FROM lotes_examen_impreso l JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE l.id_lote=? AND c.id_docente=? AND l.estado<>'ANULADO'")){
                p.setLong(1,lote);p.setInt(2,docente);try(ResultSet r=p.executeQuery()){
                    if(!r.next())throw new SQLException("Lote inexistente, anulado o no autorizado.");
                    eval=r.getInt(1);num=r.getInt(2);titulo=r.getString(3);curso=r.getString(4);ruta=r.getString(5);
                }
            }
            List<Ejemplar> ejemplares=new ArrayList<>();
            try(PreparedStatement p=c.prepareStatement("SELECT id_ejemplar,token_qr,codigo_visible,dni_estudiante,nombre_impreso,codigo_univ_impreso FROM ejemplares_examen WHERE id_lote=? ORDER BY nombre_impreso,id_ejemplar")){
                p.setLong(1,lote);try(ResultSet r=p.executeQuery()){
                    while(r.next()){
                        long id=r.getLong(1);
                        ejemplares.add(new Ejemplar(id,r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),cargarPreguntas(c,id)));
                    }
                }
            }
            return new Lote(lote,eval,num,titulo,curso,ejemplares,ruta);
        }
    }
    private List<Pregunta> cargarPreguntas(Connection c,long ejemplar) throws SQLException {
        List<Pregunta> out=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement("SELECT id_ejemplar_pregunta,COALESCE(id_pregunta,0),numero_impreso,enunciado_impreso,puntaje_impreso FROM ejemplar_preguntas WHERE id_ejemplar=? ORDER BY numero_impreso")){
            p.setLong(1,ejemplar);try(ResultSet r=p.executeQuery()){
                while(r.next()){
                    long id=r.getLong(1);List<Opcion> ops=new ArrayList<>();
                    try(PreparedStatement po=c.prepareStatement("SELECT COALESCE(id_opcion,0),texto_impreso,es_correcta,letra_impresa FROM ejemplar_opciones WHERE id_ejemplar_pregunta=? ORDER BY letra_impresa")){
                        po.setLong(1,id);try(ResultSet ro=po.executeQuery()){while(ro.next())ops.add(new Opcion(ro.getInt(1),ro.getString(2),ro.getBoolean(3),ro.getString(4)));}
                    }
                    out.add(new Pregunta(id,r.getLong(2),r.getInt(3),r.getString(4),r.getDouble(5),ops));
                }
            }
        }
        return out;
    }
    public void marcarPdf(long lote,int docente,Path ruta) throws SQLException {
        try(Connection c=conectar();PreparedStatement p=c.prepareStatement("UPDATE lotes_examen_impreso l JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso SET l.ruta_pdf=?,l.estado='LISTO' WHERE l.id_lote=? AND c.id_docente=? AND l.estado IN ('PREPARANDO','ERROR','LISTO')")){
            p.setString(1,ruta.toAbsolutePath().toString());p.setLong(2,lote);p.setInt(3,docente);if(p.executeUpdate()!=1)throw new SQLException("No se pudo registrar PDF.");
        }
    }
    public void errorPdf(long lote,int docente){
        try(Connection c=conectar();PreparedStatement p=c.prepareStatement("UPDATE lotes_examen_impreso SET estado='ERROR' WHERE id_lote=? AND id_docente=? AND estado='PREPARANDO'")){
            p.setLong(1,lote);p.setInt(2,docente);p.executeUpdate();
        }catch(SQLException ignored){}
    }
    public record Identificacion(long ejemplar,long lote,String alumno,String codigo,String token,int evaluacion){}
    public Identificacion identificar(String token,String codigo,int docente) throws SQLException {
        if((token==null||token.isBlank())&&(codigo==null||codigo.isBlank()))throw new SQLException("QR no legible. Introduce el codigo visible de la hoja.");
        String sql="SELECT x.id_ejemplar,x.id_lote,x.nombre_impreso,x.codigo_visible,x.token_qr,e.id_evaluacion FROM ejemplares_examen x JOIN lotes_examen_impreso l ON l.id_lote=x.id_lote JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE c.id_docente=? AND l.estado='LISTO' AND x.estado<>'ANULADO' AND "+((token!=null&&!token.isBlank())?"x.token_qr=?":"x.codigo_visible=?");
        try(Connection c=conectar();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,docente);p.setString(2,token!=null&&!token.isBlank()?token:codigo);
            try(ResultSet r=p.executeQuery()){
                if(!r.next())throw new SQLException("La hoja no pertenece a un examen habilitado de este docente.");
                return new Identificacion(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getString(5),r.getInt(6));
            }
        }
    }
    public long registrarEscaneo(long ejemplar,int docente,String archivo,String ocr,String aviso,List<DetectorOMR.Marca> marcas) throws SQLException {
        try(Connection c=conectar()){
            c.setAutoCommit(false);
            try {
                try(PreparedStatement p=c.prepareStatement("SELECT x.estado FROM ejemplares_examen x JOIN lotes_examen_impreso l ON l.id_lote=x.id_lote JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE x.id_ejemplar=? AND c.id_docente=? AND l.estado='LISTO' FOR UPDATE")){
                    p.setLong(1,ejemplar);p.setInt(2,docente);try(ResultSet r=p.executeQuery()){if(!r.next()||!"PENDIENTE".equals(r.getString(1)))throw new SQLException("Este ejemplar ya fue calificado, anulado o no esta disponible.");}
                }
                long scan;
                try(PreparedStatement p=c.prepareStatement("INSERT INTO escaneos_examen(id_ejemplar,id_docente,ruta_imagen_original,texto_ocr,advertencia) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                    p.setLong(1,ejemplar);p.setInt(2,docente);p.setString(3,archivo);p.setString(4,ocr);p.setString(5,aviso);p.executeUpdate();
                    try(ResultSet r=p.getGeneratedKeys()){r.next();scan=r.getLong(1);}
                }
                for(DetectorOMR.Marca m:marcas){
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO respuestas_escaneadas(id_escaneo,id_ejemplar_pregunta,letra_detectada,estado_lectura,confianza,letra_confirmada) VALUES(?,?,?,?,?,?)")){
                        p.setLong(1,scan);p.setLong(2,m.idPregunta());p.setString(3,m.letra());p.setString(4,m.estado());p.setDouble(5,m.confianza());p.setString(6,"VALIDA".equals(m.estado())?m.letra():null);p.executeUpdate();
                    }
                }
                c.commit();return scan;
            }catch(Exception ex){c.rollback();if(ex instanceof SQLException s)throw s;throw new SQLException(ex);}
        }
    }
    public Revision revision(long escaneo,int docente) throws SQLException {
        String sql="SELECT s.id_ejemplar,x.nombre_impreso,x.codigo_visible,e.titulo,s.advertencia FROM escaneos_examen s JOIN ejemplares_examen x ON x.id_ejemplar=s.id_ejemplar JOIN lotes_examen_impreso l ON l.id_lote=x.id_lote JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE s.id_escaneo=? AND c.id_docente=? AND s.estado='REVISION'";
        try(Connection c=conectar();PreparedStatement p=c.prepareStatement(sql)){
            p.setLong(1,escaneo);p.setInt(2,docente);try(ResultSet r=p.executeQuery()){
                if(!r.next())throw new SQLException("Revision no encontrada o ya confirmada.");
                long ejemplar=r.getLong(1);String nombre=r.getString(2),codigo=r.getString(3),titulo=r.getString(4),aviso=r.getString(5);
                List<ResultadoMarca> resultados=new ArrayList<>();
                try(PreparedStatement po=c.prepareStatement("SELECT ep.id_ejemplar_pregunta,ep.numero_impreso,re.letra_detectada,re.estado_lectura,re.confianza,re.letra_confirmada,ep.puntaje_impreso,(SELECT letra_impresa FROM ejemplar_opciones WHERE id_ejemplar_pregunta=ep.id_ejemplar_pregunta AND es_correcta=1 LIMIT 1) correcta FROM respuestas_escaneadas re JOIN ejemplar_preguntas ep ON ep.id_ejemplar_pregunta=re.id_ejemplar_pregunta WHERE re.id_escaneo=? AND ep.id_ejemplar=? ORDER BY ep.numero_impreso")){
                    po.setLong(1,escaneo);po.setLong(2,ejemplar);try(ResultSet rr=po.executeQuery()){
                        while(rr.next())resultados.add(new ResultadoMarca(rr.getLong(1),rr.getInt(2),rr.getString(3),rr.getString(4),rr.getDouble(5),rr.getString(6),rr.getDouble(7),rr.getString(8)));
                    }
                }
                return new Revision(escaneo,ejemplar,nombre,codigo,titulo,aviso,resultados);
            }
        }
    }
    /** Confirmar UNA SOLA vez: guarda respuestas revisadas, nota y estado en una transaccion. */
    public ResultadoNota confirmar(long escaneo,int docente,Map<Long,String> confirmadas) throws SQLException {
        try(Connection c=conectar()){
            c.setAutoCommit(false);
            try {
                long ejemplar;String alumno,codigo,dni;int evaluacion;
                try(PreparedStatement p=c.prepareStatement("SELECT x.id_ejemplar,x.nombre_impreso,x.codigo_visible,x.dni_estudiante,e.id_evaluacion,x.estado FROM escaneos_examen s JOIN ejemplares_examen x ON x.id_ejemplar=s.id_ejemplar JOIN lotes_examen_impreso l ON l.id_lote=x.id_lote JOIN evaluaciones e ON e.id_evaluacion=l.id_evaluacion JOIN cursos c ON c.id_curso=e.id_curso JOIN usuarios u ON u.id_usuario=c.id_docente AND u.rol='DOCENTE' AND u.estado=1 WHERE s.id_escaneo=? AND s.id_docente=? AND s.estado='REVISION' AND l.estado='LISTO' FOR UPDATE")){
                    p.setLong(1,escaneo);p.setInt(2,docente);try(ResultSet r=p.executeQuery()){
                        if(!r.next())throw new SQLException("Escaneo ya confirmado o no autorizado.");
                        ejemplar=r.getLong(1);alumno=r.getString(2);codigo=r.getString(3);dni=r.getString(4);evaluacion=r.getInt(5);
                        if(!"PENDIENTE".equals(r.getString(6)))throw new SQLException("El ejemplar ya tiene nota.");
                    }
                }
                try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM notas_finales WHERE dni_estudiante=? AND id_evaluacion=?")){
                    p.setString(1,dni);p.setInt(2,evaluacion);try(ResultSet r=p.executeQuery()){
                        r.next();if(r.getInt(1)>0)throw new SQLException("El estudiante ya tiene nota en esta evaluacion. Revisa sus registros antes de confirmar.");
                    }
                }
                double nota=0,max=0;int correctas=0,total=0;
                try(PreparedStatement p=c.prepareStatement("SELECT ep.id_ejemplar_pregunta,ep.puntaje_impreso,(SELECT letra_impresa FROM ejemplar_opciones eo WHERE eo.id_ejemplar_pregunta=ep.id_ejemplar_pregunta AND eo.es_correcta=1) respuesta FROM ejemplar_preguntas ep WHERE ep.id_ejemplar=? ORDER BY ep.numero_impreso")){
                    p.setLong(1,ejemplar);try(ResultSet r=p.executeQuery()){
                        while(r.next()){
                            long q=r.getLong(1);double puntos=r.getDouble(2);String correcta=r.getString(3);max+=puntos;total++;
                            if(!confirmadas.containsKey(q))throw new SQLException("Falta confirmar la respuesta de la pregunta "+total);
                            String letra=confirmadas.get(q);if(letra==null)letra="";
                            if(!letra.isBlank()&&!List.of("A","B","C","D").contains(letra))throw new SQLException("Respuesta invalida para pregunta "+total);
                            if(letra.equals(correcta)){nota+=puntos;correctas++;}
                            try(PreparedStatement upd=c.prepareStatement("UPDATE respuestas_escaneadas SET letra_confirmada=?,corregida_manualmente=IF(COALESCE(letra_detectada,'')<>?,1,corregida_manualmente) WHERE id_escaneo=? AND id_ejemplar_pregunta=?")){
                                upd.setString(1,letra.isBlank()?null:letra);upd.setString(2,letra);upd.setLong(3,escaneo);upd.setLong(4,q);
                                if(upd.executeUpdate()!=1)throw new SQLException("Respuesta de escaneo incompleta.");
                            }
                        }
                    }
                }
                if(total==0)throw new SQLException("El ejemplar no tiene preguntas.");
                try(PreparedStatement p=c.prepareStatement("INSERT INTO notas_finales(dni_estudiante,id_evaluacion,puntaje_obtenido,id_ejemplar) VALUES(?,?,?,?)")){
                    p.setString(1,dni);p.setInt(2,evaluacion);p.setDouble(3,nota);p.setLong(4,ejemplar);p.executeUpdate();
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE escaneos_examen SET estado='CONFIRMADO' WHERE id_escaneo=? AND estado='REVISION'")){
                    p.setLong(1,escaneo);if(p.executeUpdate()!=1)throw new SQLException("Escaneo ya procesado.");
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE ejemplares_examen SET estado='CALIFICADO' WHERE id_ejemplar=? AND estado='PENDIENTE'")){
                    p.setLong(1,ejemplar);if(p.executeUpdate()!=1)throw new SQLException("Ejemplar ya procesado.");
                }
                c.commit();return new ResultadoNota(alumno,codigo,nota,max,correctas,total);
            }catch(Exception e){c.rollback();if(e instanceof SQLException s)throw s;throw new SQLException(e);}
        }
    }
}
