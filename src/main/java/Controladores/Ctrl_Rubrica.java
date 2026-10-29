/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controladores;

import Conexion.Conexion;
import Constructores.RubricaCriterio;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class Ctrl_Rubrica {
    
    public boolean guardarCriterio(RubricaCriterio objeto) {
        String sql = "INSERT INTO rubricas_criterios (id_evaluacion, nombre_criterio, descripcion, puntaje_max) VALUES (?,?,?,?)";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setInt(1, objeto.getId_evaluacion());
            pst.setString(2, objeto.getNombre_criterio());
            pst.setString(3, objeto.getDescripcion());
            pst.setDouble(4, objeto.getPuntaje_max());
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error en Ctrl_Rubrica: " + e.getMessage());
            return false;
        }
    }

    public List<RubricaCriterio> listarCriterios(int idEvaluacion) {
        List<RubricaCriterio> lista = new ArrayList<>();
        String sql = "SELECT * FROM rubricas_criterios WHERE id_evaluacion = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setInt(1, idEvaluacion);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                RubricaCriterio rc = new RubricaCriterio();
                rc.setId_criterio(rs.getInt("id_criterio"));
                rc.setNombre_criterio(rs.getString("nombre_criterio"));
                rc.setPuntaje_max(rs.getDouble("puntaje_max"));
                lista.add(rc);
            }
        } catch (SQLException e) { }
        return lista;
    }
    
}
