/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controladores;

import Conexion.Conexion;
import Constructores.Curso;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class Ctrl_Curso {
    
    public boolean guardar(Curso objeto) {
        String sql = "INSERT INTO cursos (nombre_curso, codigo_curso, id_docente, id_escuela, semestre, estado) VALUES (?,?,?,?,?,1)";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setString(1, objeto.getNombre_curso());
            pst.setString(2, objeto.getCodigo_curso());
            pst.setInt(3, objeto.getId_docente());
            pst.setInt(4, objeto.getId_escuela());
            pst.setString(5, objeto.getSemestre());
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al guardar curso: " + e.getMessage());
            return false;
        }
    }

    public List<Object[]> listarCursosTabla(String valor) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT c.id_curso, c.nombre_curso, u.nombre_completo, e.nombre_escuela, c.semestre " +
                     "FROM cursos c " +
                     "LEFT JOIN usuarios u ON c.id_docente = u.id_usuario " +
                     "LEFT JOIN escuelas e ON c.id_escuela = e.id_escuela " +
                     "WHERE c.nombre_curso LIKE ? AND c.estado = 1";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setString(1, "%" + valor + "%");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Object[] fila = new Object[5];
                fila[0] = rs.getInt("id_curso");     
                fila[1] = rs.getString("nombre_curso");
                fila[2] = rs.getString("nombre_completo") == null ? "No asignado" : rs.getString("nombre_completo");
                fila[3] = rs.getString("nombre_escuela") == null ? "No asignado" : rs.getString("nombre_escuela");
                fila[4] = rs.getString("semestre");
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
        return lista;
    }
    
    public boolean actualizar(Curso objeto) {
       
        String sql = "UPDATE cursos SET nombre_curso = ?, id_docente = ?, id_escuela = ?, semestre = ? WHERE id_curso = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setString(1, objeto.getNombre_curso());
            pst.setInt(2, objeto.getId_docente());
            pst.setInt(3, objeto.getId_escuela());
            pst.setString(4, objeto.getSemestre()); 
            pst.setInt(5, objeto.getId_curso());
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar curso: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int idCurso) {
        String sql = "UPDATE cursos SET estado = 0 WHERE id_curso = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setInt(1, idCurso);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al eliminar: " + e.getMessage());
            return false;
        }
    }

    public boolean asignarEstudiante(int idCurso, String dniEstudiante) {
        String sql = "INSERT INTO matricula (id_curso, dni_estudiante) VALUES (?,?)";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setInt(1, idCurso);
            pst.setString(2, dniEstudiante);
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al asignar estudiante: " + e.getMessage());
            return false;
        }
    }

    public boolean quitarEstudiante(int idCurso, String dniEstudiante) {
        String sql = "DELETE FROM matricula WHERE id_curso = ? AND dni_estudiante = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setInt(1, idCurso);
            pst.setString(2, dniEstudiante);
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al quitar estudiante: " + e.getMessage());
            return false;
        }
    }

    public List<Object[]> listarAsignados(int idCurso) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT e.dni, e.nombre_completo " +
                     "FROM estudiantes e " +
                     "JOIN matricula m ON e.dni = m.dni_estudiante " +
                     "WHERE m.id_curso = ? AND e.estado = 1";
        
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setInt(1, idCurso);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                Object[] fila = new Object[2];
                fila[0] = rs.getString(1); 
                fila[1] = rs.getString(2); 
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar asignados: " + e.getMessage());
        }
        return lista;
    }
    
    public List<Curso> listarCursosPorDocente(int idDocente) {
        List<Curso> lista = new ArrayList<>();
        String sql = "SELECT id_curso, nombre_curso FROM cursos WHERE id_docente = ? AND estado = 1";

        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {

            pst.setInt(1, idDocente);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Curso curso = new Curso();
                curso.setId_curso(rs.getInt("id_curso"));
                curso.setNombre_curso(rs.getString("nombre_curso"));
                lista.add(curso);
            }
        } catch (SQLException e) {
            System.out.println("Error al filtrar cursos por docente: " + e.getMessage());
        }
        return lista;
     
    }
    
    public List<Object[]> listarEstudiantesDisponibles(int idEscuela, String semestre, int idCurso) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT codigo_univ, nombre_completo FROM estudiantes " +
                     "WHERE id_escuela = ? AND semestre = ? AND estado = 1 " +
                     "AND codigo_univ NOT IN (SELECT dni_estudiante FROM matricula WHERE id_curso = ?)";

        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {

            pst.setInt(1, idEscuela);
            pst.setString(2, semestre);
            pst.setInt(3, idCurso);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Object[] fila = new Object[2];
                fila[0] = rs.getString(1); 
                fila[1] = rs.getString(2);
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar disponibles: " + e.getMessage());
        }
        return lista;
    }
    
    public boolean existeCurso(String nombre) {
        String sql = "SELECT id_curso FROM cursos WHERE nombre_curso = ? AND estado = 1";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setString(1, nombre);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
    
}


