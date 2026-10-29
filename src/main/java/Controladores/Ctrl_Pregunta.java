/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controladores;

import Conexion.Conexion;
import Constructores.Pregunta;
import Constructores.Opcion;
import java.sql.*;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class Ctrl_Pregunta {
    
    public int guardarPregunta(Pregunta objeto) {
        int idPregunta = 0;
        String sql = "INSERT INTO preguntas (id_evaluacion, texto_pregunta, puntaje_max) VALUES (?,?,?)";
        
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pst.setInt(1, objeto.getId_evaluacion());
            pst.setString(2, objeto.getTexto_pregunta());
            pst.setDouble(3, objeto.getPuntaje_max());
            pst.executeUpdate();
            
            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) idPregunta = rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Error al guardar pregunta: " + e.getMessage());
        }
        return idPregunta;
    }

    public boolean guardarOpcion(Opcion objeto) {
        String sql = "INSERT INTO opciones (id_pregunta, texto_opcion, es_correcta) VALUES (?,?,?)";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setInt(1, objeto.getId_pregunta());
            pst.setString(2, objeto.getTexto_opcion());
            pst.setBoolean(3, objeto.isEs_correcta());
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al guardar opción: " + e.getMessage());
            return false;
        }
    }
    

    public List<Constructores.Pregunta> listarPreguntasPorEvaluacion(int idEvaluacion) {
        List<Constructores.Pregunta> lista = new java.util.ArrayList<>();
        String sql = "SELECT * FROM preguntas WHERE id_evaluacion = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setInt(1, idEvaluacion);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Constructores.Pregunta p = new Constructores.Pregunta();
                p.setId_pregunta(rs.getInt("id_pregunta"));
                p.setId_evaluacion(rs.getInt("id_evaluacion"));
                p.setTexto_pregunta(rs.getString("texto_pregunta"));
                p.setPuntaje_max(rs.getDouble("puntaje_max"));
                lista.add(p);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar preguntas: " + e.getMessage());
        }
        return lista;
    }

    public List<Constructores.Opcion> listarOpcionesPorPregunta(int idPregunta) {
        List<Constructores.Opcion> lista = new java.util.ArrayList<>();
        String sql = "SELECT * FROM opciones WHERE id_pregunta = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setInt(1, idPregunta);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Constructores.Opcion o = new Constructores.Opcion();
                o.setId_opcion(rs.getInt("id_opcion"));
                o.setId_pregunta(rs.getInt("id_pregunta"));
                o.setTexto_opcion(rs.getString("texto_opcion"));
                o.setEs_correcta(rs.getBoolean("es_correcta"));
                lista.add(o);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar opciones: " + e.getMessage());
        }
        return lista;
    }
}
