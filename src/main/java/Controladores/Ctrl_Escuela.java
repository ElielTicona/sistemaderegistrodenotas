/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controladores;

import Conexion.Conexion;
import Constructores.Escuela;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class Ctrl_Escuela {
    
    public List<Escuela> consultarEscuelas() {
        List<Escuela> lista = new ArrayList<>();
        String sql = "SELECT * FROM escuelas";
        
        try (Connection cn = Conexion.conectar();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                Escuela esc = new Escuela();
                esc.setId_escuela(rs.getInt("id_escuela"));
                esc.setNombre_escuela(rs.getString("nombre_escuela"));
                lista.add(esc);
            }
        } catch (SQLException e) {
            System.out.println("Error al consultar escuelas: " + e.getMessage());
        }
        return lista;
    }
    
    public boolean guardar(Escuela objeto) {
        String sql = "INSERT INTO escuelas (nombre_escuela) VALUES (?)";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setString(1, objeto.getNombre_escuela());
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al guardar escuela: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Escuela objeto) {
        String sql = "UPDATE escuelas SET nombre_escuela = ? WHERE id_escuela = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setString(1, objeto.getNombre_escuela());
            pst.setInt(2, objeto.getId_escuela());
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar escuela: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM escuelas WHERE id_escuela = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setInt(1, id);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al eliminar escuela: " + e.getMessage());
            return false;
        }
    }

    
}
