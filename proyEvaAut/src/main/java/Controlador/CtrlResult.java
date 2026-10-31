/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controlador;

import Conexion.Conexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author KriZ
 */
public class CtrlResult {
    
    public boolean calificarExam(String crisDni, int crisIdEval, Map<Integer, Integer> crisResp) {
        Connection crisCn = Conexion.conectar();
        double crisNotaFinal = 0;

        try {
            crisCn.setAutoCommit(false);
            for (Map.Entry<Integer, Integer> crisEntry : crisResp.entrySet()) {
                int crisIdPreg = crisEntry.getKey();
                int crisIdOpcion = crisEntry.getValue();

                String crisSqlVal = "SELECT o.es_correcta, p.puntaje_max FROM opciones o " +
                                "JOIN preguntas p ON o.id_pregunta = p.id_pregunta " +
                                "WHERE o.id_opcion = ? AND o.id_pregunta = ?";
                
                PreparedStatement crisPstVal = crisCn.prepareStatement(crisSqlVal);
                crisPstVal.setInt(1, crisIdOpcion);
                crisPstVal.setInt(2, crisIdPreg);
                ResultSet crisRs = crisPstVal.executeQuery();

                if (crisRs.next()) {
                    if (crisRs.getBoolean("es_correcta")) {
                        crisNotaFinal += crisRs.getDouble("puntaje_max");
                    }
                }
                String crisSqlDet = "INSERT INTO detalle_respuestas_examen (dni_estudiante, id_pregunta, id_opcion_elegida) VALUES (?,?,?)";
                PreparedStatement crisPstDet = crisCn.prepareStatement(crisSqlDet);
                crisPstDet.setString(1, crisDni);
                crisPstDet.setInt(2, crisIdPreg);
                crisPstDet.setInt(3, crisIdOpcion);
                crisPstDet.executeUpdate();
            }
            String crisSqlNota = "INSERT INTO notas_finales (dni_estudiante, id_evaluacion, puntaje_obtenido) VALUES (?,?,?)";
            PreparedStatement crisPstNota = crisCn.prepareStatement(crisSqlNota);
            crisPstNota.setString(1, crisDni);
            crisPstNota.setInt(2, crisIdEval);
            crisPstNota.setDouble(3, crisNotaFinal);
            crisPstNota.executeUpdate();

            crisCn.commit();
            return true;
        } catch (SQLException crisE) {
            try { crisCn.rollback(); } catch (SQLException crisEx) {}
            System.out.println("Error en Calificación: " + crisE.getMessage());
            return false;
        } finally {
            try { crisCn.close(); } catch (SQLException crisE) {}
        }
    }
    
    public boolean guardarNotaRub(String crisDni, int crisIdEval, double crisNota) {
        String crisSql = "INSERT INTO notas_finales (dni_estudiante, id_evaluacion, puntaje_obtenido) VALUES (?,?,?)";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setString(1, crisDni);
            crisPst.setInt(2, crisIdEval);
            crisPst.setDouble(3, crisNota);
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) { return false; }
    }
    
    public java.util.List<Object[]> listarResultCurso(int crisIdCurso) {
        List<Object[]> crisLista = new ArrayList<>();
        String crisSql = "SELECT e.dni, e.nombre_completo, ev.titulo, ev.tipo, IFNULL(n.puntaje_obtenido, 0) as nota " +
                     "FROM estudiantes e " +
                     "JOIN matricula m ON e.dni = m.dni_estudiante " +
                     "JOIN evaluaciones ev ON m.id_curso = ev.id_curso " +
                     "LEFT JOIN notas_finales n ON e.dni = n.dni_estudiante AND ev.id_evaluacion = n.id_evaluacion " +
                     "WHERE m.id_curso = ? " +
                     "ORDER BY e.nombre_completo ASC, ev.titulo ASC";
                     
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
             
            crisPst.setInt(1, crisIdCurso);
            ResultSet crisRs = crisPst.executeQuery();
            
            while (crisRs.next()) {
                Object[] crisFila = new Object[5];
                crisFila[0] = crisRs.getString("dni");
                crisFila[1] = crisRs.getString("nombre_completo");
                crisFila[2] = crisRs.getString("titulo");
                crisFila[3] = crisRs.getString("tipo");
                crisFila[4] = crisRs.getDouble("nota");
                crisLista.add(crisFila);
            }
        } catch (SQLException crisE) {
            System.out.println("Error de ejecución " + crisE.getMessage());
        }
        return crisLista;
    }
    
    public java.util.List<Object[]> listarNotasEstud(String crisDni, int crisIdCurso) {
        java.util.List<Object[]> crisLista = new java.util.ArrayList<>();
        String crisSql = "SELECT ev.titulo, ev.tipo, n.puntaje_obtenido, ev.fecha_examen " +
                     "FROM notas_finales n " +
                     "JOIN evaluaciones ev ON n.id_evaluacion = ev.id_evaluacion " +
                     "WHERE n.dni_estudiante = ? AND ev.id_curso = ? " +
                     "ORDER BY ev.fecha_examen DESC";
                     
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
             
            crisPst.setString(1, crisDni);
            crisPst.setInt(2, crisIdCurso);
            ResultSet crisRs = crisPst.executeQuery();
            
            while (crisRs.next()) {
                Object[] crisFila = new Object[4];
                crisFila[0] = crisRs.getString("titulo");
                crisFila[1] = crisRs.getString("tipo");
                crisFila[2] = crisRs.getDouble("puntaje_obtenido");
                crisFila[3] = crisRs.getString("fecha_examen");
                crisLista.add(crisFila);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al extraer notas del estudiante: " + crisE.getMessage());
        }
        return crisLista;
    }
}
