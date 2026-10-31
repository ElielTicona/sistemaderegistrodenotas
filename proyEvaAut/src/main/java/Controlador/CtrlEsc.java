/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controlador;

import Conexion.Conexion;
import Modelo.Escuela;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class CtrlEsc {
    
    public List<Escuela> consultarEsc() {
        List<Escuela> crisLista = new ArrayList<>();
        String crisSql = "SELECT * FROM escuelas";
        
        try (Connection crisCn = Conexion.conectar();
             Statement crisSt = crisCn.createStatement();
             ResultSet crisRs = crisSt.executeQuery(crisSql)) {
            
            while (crisRs.next()) {
                Escuela crisEsc = new Escuela();
                crisEsc.setIdEsc(crisRs.getInt("id_escuela"));
                crisEsc.setNomEsc(crisRs.getString("nombre_escuela"));
                crisLista.add(crisEsc);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al consultar escuelas: " + crisE.getMessage());
        }
        return crisLista;
    }
    
    public boolean guardar(Escuela crisObj) {
        String crisSql = "INSERT INTO escuelas (nombre_escuela) VALUES (?)";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setString(1, crisObj.getNomEsc());
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al guardar escuela: " + crisE.getMessage());
            return false;
        }
    }

    public boolean actualizar(Escuela crisObj) {
        String crisSql = "UPDATE escuelas SET nombre_escuela = ? WHERE id_escuela = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setString(1, crisObj.getNomEsc());
            crisPst.setInt(2, crisObj.getIdEsc());
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al actualizar escuela: " + crisE.getMessage());
            return false;
        }
    }

    public boolean eliminar(int crisId) {
        String crisSql = "DELETE FROM escuelas WHERE id_escuela = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setInt(1, crisId);
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al eliminar escuela: " + crisE.getMessage());
            return false;
        }
    }

    
}
