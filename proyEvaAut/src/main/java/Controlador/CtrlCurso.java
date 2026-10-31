/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controlador;

import Conexion.Conexion;
import Modelo.Curso;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class CtrlCurso {
    
    public boolean guardar(Curso crisObj) {
        String crisSql = "INSERT INTO cursos (nombre_curso, codigo_curso, id_docente, id_escuela, semestre, estado) VALUES (?,?,?,?,?,1)";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setString(1, crisObj.getNomCurso());
            crisPst.setString(2, crisObj.getCodCurso());
            crisPst.setInt(3, crisObj.getIdDocente());
            crisPst.setInt(4, crisObj.getIdEsc());
            crisPst.setString(5, crisObj.getSemestre());
            
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al guardar curso: " + crisE.getMessage());
            return false;
        }
    }

    public List<Object[]> listarCursTabla(String crisValor) {
        List<Object[]> crisLista = new ArrayList<>();
        String crisSql = "SELECT c.id_curso, c.nombre_curso, u.nombre_completo, e.nombre_escuela, c.semestre " +
                     "FROM cursos c " +
                     "LEFT JOIN usuarios u ON c.id_docente = u.id_usuario " +
                     "LEFT JOIN escuelas e ON c.id_escuela = e.id_escuela " +
                     "WHERE c.nombre_curso LIKE ? AND c.estado = 1";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setString(1, "%" + crisValor + "%");
            ResultSet crisRs = crisPst.executeQuery();
            while (crisRs.next()) {
                Object[] crisFila = new Object[5];
                crisFila[0] = crisRs.getInt("id_curso");     
                crisFila[1] = crisRs.getString("nombre_curso");
                crisFila[2] = crisRs.getString("nombre_completo") == null ? "No asignado" : crisRs.getString("nombre_completo");
                crisFila[3] = crisRs.getString("nombre_escuela") == null ? "No asignado" : crisRs.getString("nombre_escuela");
                crisFila[4] = crisRs.getString("semestre");
                crisLista.add(crisFila);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al listar: " + crisE.getMessage());
        }
        return crisLista;
    }
    
    public boolean actualizar(Curso crisObj) {
       
        String crisSql = "UPDATE cursos SET nombre_curso = ?, id_docente = ?, id_escuela = ?, semestre = ? WHERE id_curso = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setString(1, crisObj.getNomCurso());
            crisPst.setInt(2, crisObj.getIdDocente());
            crisPst.setInt(3, crisObj.getIdEsc());
            crisPst.setString(4, crisObj.getSemestre()); 
            crisPst.setInt(5, crisObj.getIdCurso());
            
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al actualizar curso: " + crisE.getMessage());
            return false;
        }
    }

    public boolean eliminar(int crisIdCurso) {
        String crisSql = "UPDATE cursos SET estado = 0 WHERE id_curso = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setInt(1, crisIdCurso);
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al eliminar: " + crisE.getMessage());
            return false;
        }
    }

    public boolean asignarEstud(int crisIdCurso, String crisDniEstud) {
        String crisSql = "INSERT INTO matricula (id_curso, dni_estudiante) VALUES (?,?)";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setInt(1, crisIdCurso);
            crisPst.setString(2, crisDniEstud);
            
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al asignar estudiante: " + crisE.getMessage());
            return false;
        }
    }

    public boolean quitarEstud(int crisIdCurso, String crisDniEstud) {
        String crisSql = "DELETE FROM matricula WHERE id_curso = ? AND dni_estudiante = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setInt(1, crisIdCurso);
            crisPst.setString(2, crisDniEstud);
            
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al quitar estudiante: " + crisE.getMessage());
            return false;
        }
    }

    public List<Object[]> listarAsignados(int crisIdCurso) {
        List<Object[]> crisLista = new ArrayList<>();
        String crisSql = "SELECT e.dni, e.nombre_completo " +
                     "FROM estudiantes e " +
                     "JOIN matricula m ON e.dni = m.dni_estudiante " +
                     "WHERE m.id_curso = ? AND e.estado = 1";
        
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setInt(1, crisIdCurso);
            ResultSet crisRs = crisPst.executeQuery();
            
            while (crisRs.next()) {
                Object[] crisFila = new Object[2];
                crisFila[0] = crisRs.getString(1); 
                crisFila[1] = crisRs.getString(2); 
                crisLista.add(crisFila);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al listar asignados: " + crisE.getMessage());
        }
        return crisLista;
    }
    
    public List<Curso> listarCursDoc(int crisIdDocente) {
        List<Curso> crisLista = new ArrayList<>();
        String crisSql = "SELECT id_curso, nombre_curso FROM cursos WHERE id_docente = ? AND estado = 1";

        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {

            crisPst.setInt(1, crisIdDocente);
            ResultSet crisRs = crisPst.executeQuery();

            while (crisRs.next()) {
                Curso crisCurso = new Curso();
                crisCurso.setIdCurso(crisRs.getInt("id_curso"));
                crisCurso.setNomCurso(crisRs.getString("nombre_curso"));
                crisLista.add(crisCurso);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al filtrar cursos por docente: " + crisE.getMessage());
        }
        return crisLista;
     
    }
    
    public List<Object[]> listarEstudDisp(int crisIdEsc, String crisSemestre, int crisIdCurso) {
        List<Object[]> crisLista = new ArrayList<>();
        String crisSql = "SELECT codigo_univ, nombre_completo FROM estudiantes " +
                     "WHERE id_escuela = ? AND semestre = ? AND estado = 1 " +
                     "AND codigo_univ NOT IN (SELECT dni_estudiante FROM matricula WHERE id_curso = ?)";

        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {

            crisPst.setInt(1, crisIdEsc);
            crisPst.setString(2, crisSemestre);
            crisPst.setInt(3, crisIdCurso);
            ResultSet crisRs = crisPst.executeQuery();

            while (crisRs.next()) {
                Object[] crisFila = new Object[2];
                crisFila[0] = crisRs.getString(1); 
                crisFila[1] = crisRs.getString(2);
                crisLista.add(crisFila);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al listar disponibles: " + crisE.getMessage());
        }
        return crisLista;
    }
    
    public boolean existeCurso(String crisNom) {
        String crisSql = "SELECT id_curso FROM cursos WHERE nombre_curso = ? AND estado = 1";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setString(1, crisNom);
            ResultSet crisRs = crisPst.executeQuery();
            return crisRs.next();
        } catch (SQLException crisE) {
            return false;
        }
    }
    
}


