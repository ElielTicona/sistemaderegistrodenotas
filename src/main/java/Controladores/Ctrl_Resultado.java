/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controladores;

import Conexion.Conexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author KriZ
 */
public class Ctrl_Resultado {
    
    public boolean calificarExamen(String dni, int idEval, Map<Integer, Integer> respuestas) {
        Connection cn = Conexion.conectar();
        double notaFinal = 0;

        try {
            cn.setAutoCommit(false);
            for (Map.Entry<Integer, Integer> entry : respuestas.entrySet()) {
                int idPregunta = entry.getKey();
                int idOpcion = entry.getValue();

                String sqlVal = "SELECT o.es_correcta, p.puntaje_max FROM opciones o " +
                                "JOIN preguntas p ON o.id_pregunta = p.id_pregunta " +
                                "WHERE o.id_opcion = ? AND o.id_pregunta = ?";
                
                PreparedStatement pstVal = cn.prepareStatement(sqlVal);
                pstVal.setInt(1, idOpcion);
                pstVal.setInt(2, idPregunta);
                ResultSet rs = pstVal.executeQuery();

                if (rs.next()) {
                    if (rs.getBoolean("es_correcta")) {
                        notaFinal += rs.getDouble("puntaje_max");
                    }
                }
                String sqlDet = "INSERT INTO detalle_respuestas_examen (dni_estudiante, id_pregunta, id_opcion_elegida) VALUES (?,?,?)";
                PreparedStatement pstDet = cn.prepareStatement(sqlDet);
                pstDet.setString(1, dni);
                pstDet.setInt(2, idPregunta);
                pstDet.setInt(3, idOpcion);
                pstDet.executeUpdate();
            }
            String sqlNota = "INSERT INTO notas_finales (dni_estudiante, id_evaluacion, puntaje_obtenido) VALUES (?,?,?)";
            PreparedStatement pstNota = cn.prepareStatement(sqlNota);
            pstNota.setString(1, dni);
            pstNota.setInt(2, idEval);
            pstNota.setDouble(3, notaFinal);
            pstNota.executeUpdate();

            cn.commit();
            return true;
        } catch (SQLException e) {
            try { cn.rollback(); } catch (SQLException ex) {}
            System.out.println("Error en Calificación: " + e.getMessage());
            return false;
        } finally {
            try { cn.close(); } catch (SQLException e) {}
        }
    }
    
    public boolean guardarNotaRubrica(String dni, int idEval, double nota) {
        String sql = "INSERT INTO notas_finales (dni_estudiante, id_evaluacion, puntaje_obtenido) VALUES (?,?,?)";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setString(1, dni);
            pst.setInt(2, idEval);
            pst.setDouble(3, nota);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
    
    public java.util.List<Object[]> listarResultadosPorCurso(int idCurso) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT e.dni, e.nombre_completo, ev.titulo, ev.tipo, IFNULL(n.puntaje_obtenido, 0) as nota " +
                     "FROM estudiantes e " +
                     "JOIN matricula m ON e.dni = m.dni_estudiante " +
                     "JOIN evaluaciones ev ON m.id_curso = ev.id_curso " +
                     "LEFT JOIN notas_finales n ON e.dni = n.dni_estudiante AND ev.id_evaluacion = n.id_evaluacion " +
                     "WHERE m.id_curso = ? " +
                     "ORDER BY e.nombre_completo ASC, ev.titulo ASC";
                     
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
             
            pst.setInt(1, idCurso);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                Object[] fila = new Object[5];
                fila[0] = rs.getString("dni");
                fila[1] = rs.getString("nombre_completo");
                fila[2] = rs.getString("titulo");
                fila[3] = rs.getString("tipo");
                fila[4] = rs.getDouble("nota");
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.out.println("Error de ejecución " + e.getMessage());
        }
        return lista;
    }
    
    public java.util.List<Object[]> listarNotasEstudiante(String dni, int idCurso) {
        java.util.List<Object[]> lista = new java.util.ArrayList<>();
        String sql = "SELECT ev.titulo, ev.tipo, n.puntaje_obtenido, ev.fecha_examen " +
                     "FROM notas_finales n " +
                     "JOIN evaluaciones ev ON n.id_evaluacion = ev.id_evaluacion " +
                     "WHERE n.dni_estudiante = ? AND ev.id_curso = ? " +
                     "ORDER BY ev.fecha_examen DESC";
                     
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
             
            pst.setString(1, dni);
            pst.setInt(2, idCurso);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                Object[] fila = new Object[4];
                fila[0] = rs.getString("titulo");
                fila[1] = rs.getString("tipo");
                fila[2] = rs.getDouble("puntaje_obtenido");
                fila[3] = rs.getString("fecha_examen");
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.out.println("Error al extraer notas del estudiante: " + e.getMessage());
        }
        return lista;
    }
}
