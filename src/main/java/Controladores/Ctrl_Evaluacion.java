/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controladores;

import Conexion.Conexion;
import Constructores.Evaluacion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class Ctrl_Evaluacion {
    
    public int guardar(Evaluacion objeto) {
        int idGenerado = 0;
        String sql = "INSERT INTO evaluaciones (id_curso, titulo, tipo, fecha_examen, estado) VALUES (?,?,?,?,1)";

        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setInt(1, objeto.getId_curso());
            pst.setString(2, objeto.getTitulo());
            pst.setString(3, objeto.getTipo());
            pst.setString(4, objeto.getFecha_examen()); 

            pst.executeUpdate();
            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                idGenerado = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error al crear evaluación: " + e.getMessage());
        }
        return idGenerado; 
    }


    public List<Evaluacion> listarPorCurso(int idCurso) {
        List<Evaluacion> lista = new ArrayList<>();
        String sql = "SELECT * FROM evaluaciones WHERE id_curso = ? AND estado = 1";
        
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setInt(1, idCurso);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                Evaluacion ev = new Evaluacion();
                ev.setId_evaluacion(rs.getInt("id_evaluacion"));
                ev.setTitulo(rs.getString("titulo"));
                ev.setTipo(rs.getString("tipo"));
                ev.setFecha_creacion(rs.getTimestamp("fecha_creacion"));
                lista.add(ev);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar evaluaciones: " + e.getMessage());
        }
        return lista;
    }
    
    public boolean actualizar(Evaluacion objeto) {
        String sql = "UPDATE evaluaciones SET titulo = ?, tipo = ?, fecha_examen = ? WHERE id_evaluacion = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setString(1, objeto.getTitulo());
            pst.setString(2, objeto.getTipo());
            pst.setString(3, objeto.getFecha_examen());
            pst.setInt(4, objeto.getId_evaluacion());
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar evaluación: " + e.getMessage());
            return false;
        }
    }
    
    public boolean actualizarDificultad(int idEvaluacion, String dificultad) {
        String sql = "UPDATE evaluaciones SET dificultad = ? WHERE id_evaluacion = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setString(1, dificultad);
            pst.setInt(2, idEvaluacion);
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar dificultad: " + e.getMessage());
            return false;
        }
    }
    
}
