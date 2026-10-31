/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controlador;

import Conexion.Conexion;
import Modelo.Usu;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class CtrlUsu {
    
    public Usu login(String crisUser, String crisPass) {
    Usu crisUsu = null;
        String crisSql = "SELECT * FROM usuarios WHERE username = ? AND password = ? AND estado = 1";

        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {

            crisPst.setString(1, crisUser);
            crisPst.setString(2, crisPass);
            ResultSet crisRs = crisPst.executeQuery();

            if (crisRs.next()) {
                crisUsu = new Usu();
                crisUsu.setIdUsu(crisRs.getInt("id_usuario"));
                crisUsu.setUsername(crisRs.getString("username"));
                crisUsu.setRol(crisRs.getString("rol"));
                crisUsu.setNomCompleto(crisRs.getString("nombre_completo"));
            }
        } catch (SQLException crisE) {
            System.out.println("Error en login: " + crisE.getMessage());
        }
        return crisUsu;
    }

    public List<Usu> listarDoc() {
        List<Usu> crisLista = new ArrayList<>();
        String crisSql = "SELECT id_usuario, nombre_completo FROM usuarios WHERE rol = 'DOCENTE' AND estado = 1";
        
        try (Connection crisCn = Conexion.conectar();
             Statement crisSt = crisCn.createStatement();
             ResultSet crisRs = crisSt.executeQuery(crisSql)) {
            
            while (crisRs.next()) {
                Usu crisDoc = new Usu();
                crisDoc.setIdUsu(crisRs.getInt("id_usuario"));
                crisDoc.setNomCompleto(crisRs.getString("nombre_completo"));
                crisLista.add(crisDoc);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al listar docentes: " + crisE.getMessage());
        }
        return crisLista;
    }
    
    public boolean guardar(Usu crisObj) {
        String crisSql = "INSERT INTO usuarios (username, password, rol, nombre_completo, estado) VALUES (?,?,?,?,1)";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setString(1, crisObj.getUsername());
            crisPst.setString(2, crisObj.getPassword());
            crisPst.setString(3, crisObj.getRol());
            crisPst.setString(4, crisObj.getNomCompleto());
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al guardar usuario: " + crisE.getMessage());
            return false;
        }
    }

    public boolean actualizar(Usu crisObj) {
        String crisSql = "UPDATE usuarios SET username = ?, password = ?, rol = ?, nombre_completo = ? WHERE id_usuario = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setString(1, crisObj.getUsername());
            crisPst.setString(2, crisObj.getPassword());
            crisPst.setString(3, crisObj.getRol());
            crisPst.setString(4, crisObj.getNomCompleto());
            crisPst.setInt(5, crisObj.getIdUsu());
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al actualizar usuario: " + crisE.getMessage());
            return false;
        }
    }

    public boolean eliminar(int crisId) {
        String crisSql = "UPDATE usuarios SET estado = 0 WHERE id_usuario = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setInt(1, crisId);
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al eliminar usuario: " + crisE.getMessage());
            return false;
        }
    }

    public List<Usu> listarUsuGest() {
        List<Usu> crisLista = new ArrayList<>();
        String crisSql = "SELECT * FROM usuarios WHERE rol IN ('ADMIN', 'DOCENTE') AND estado = 1";
        try (Connection crisCn = Conexion.conectar();
             Statement crisSt = crisCn.createStatement();
             ResultSet crisRs = crisSt.executeQuery(crisSql)) {
            while (crisRs.next()) {
                Usu crisUser = new Usu();
                crisUser.setIdUsu(crisRs.getInt("id_usuario"));
                crisUser.setUsername(crisRs.getString("username"));
                crisUser.setPassword(crisRs.getString("password"));
                crisUser.setRol(crisRs.getString("rol"));
                crisUser.setNomCompleto(crisRs.getString("nombre_completo"));
                crisLista.add(crisUser);
            }
        } catch (SQLException crisE) { 
            System.out.println("Error al listar usuarios: " + crisE.getMessage());
        }
        return crisLista;
    }

    
    
}
