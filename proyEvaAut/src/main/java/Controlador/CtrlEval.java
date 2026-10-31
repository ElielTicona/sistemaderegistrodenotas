/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controlador;

import Conexion.Conexion;
import Modelo.Eval;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class CtrlEval {
    
    public int guardar(Eval crisObj) {
        int crisIdGenerado = 0;
        String crisSql = "INSERT INTO evaluaciones (id_curso, titulo, tipo, fecha_examen, estado) VALUES (?,?,?,?,1)";

        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql, Statement.RETURN_GENERATED_KEYS)) {

            crisPst.setInt(1, crisObj.getIdCurso());
            crisPst.setString(2, crisObj.getTitulo());
            crisPst.setString(3, crisObj.getTipo());
            crisPst.setString(4, crisObj.getFechaExamen()); 

            crisPst.executeUpdate();
            ResultSet crisRs = crisPst.getGeneratedKeys();
            if (crisRs.next()) {
                crisIdGenerado = crisRs.getInt(1);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al crear evaluación: " + crisE.getMessage());
        }
        return crisIdGenerado; 
    }


    public List<Eval> listarPorCurso(int crisIdCurso) {
        List<Eval> crisLista = new ArrayList<>();
        String crisSql = "SELECT * FROM evaluaciones WHERE id_curso = ? AND estado = 1";
        
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setInt(1, crisIdCurso);
            ResultSet crisRs = crisPst.executeQuery();
            
            while (crisRs.next()) {
                Eval crisEv = new Eval();
                crisEv.setIdEval(crisRs.getInt("id_evaluacion"));
                crisEv.setTitulo(crisRs.getString("titulo"));
                crisEv.setTipo(crisRs.getString("tipo"));
                crisEv.setFechaCreacion(crisRs.getTimestamp("fecha_creacion"));
                crisLista.add(crisEv);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al listar evaluaciones: " + crisE.getMessage());
        }
        return crisLista;
    }
    
    public boolean actualizar(Eval crisObj) {
        String crisSql = "UPDATE evaluaciones SET titulo = ?, tipo = ?, fecha_examen = ? WHERE id_evaluacion = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setString(1, crisObj.getTitulo());
            crisPst.setString(2, crisObj.getTipo());
            crisPst.setString(3, crisObj.getFechaExamen());
            crisPst.setInt(4, crisObj.getIdEval());
            
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al actualizar evaluación: " + crisE.getMessage());
            return false;
        }
    }
    
    public boolean actualizarDif(int crisIdEval, String crisDificultad) {
        String crisSql = "UPDATE evaluaciones SET dificultad = ? WHERE id_evaluacion = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setString(1, crisDificultad);
            crisPst.setInt(2, crisIdEval);
            
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al actualizar dificultad: " + crisE.getMessage());
            return false;
        }
    }
    
}
