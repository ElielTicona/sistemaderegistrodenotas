/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controladores;

import Conexion.Conexion;
import Constructores.Usuario;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class Ctrl_Usuario {
    
    public Usuario login(String user, String pass) {
    Usuario usuario = null;
        String sql = "SELECT * FROM usuarios WHERE username = ? AND password = ? AND estado = 1";

        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {

            pst.setString(1, user);
            pst.setString(2, pass);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                usuario = new Usuario();
                usuario.setId_usuario(rs.getInt("id_usuario"));
                usuario.setUsername(rs.getString("username"));
                usuario.setRol(rs.getString("rol"));
                usuario.setNombre_completo(rs.getString("nombre_completo"));
            }
        } catch (SQLException e) {
            System.out.println("Error en login: " + e.getMessage());
        }
        return usuario;
    }

    public List<Usuario> listarDocentes() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id_usuario, nombre_completo FROM usuarios WHERE rol = 'DOCENTE' AND estado = 1";
        
        try (Connection cn = Conexion.conectar();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                Usuario doc = new Usuario();
                doc.setId_usuario(rs.getInt("id_usuario"));
                doc.setNombre_completo(rs.getString("nombre_completo"));
                lista.add(doc);
            }
        } catch (SQLException e) {
            System.out.println("Error al listar docentes: " + e.getMessage());
        }
        return lista;
    }
    
    public boolean guardar(Usuario objeto) {
        String sql = "INSERT INTO usuarios (username, password, rol, nombre_completo, estado) VALUES (?,?,?,?,1)";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setString(1, objeto.getUsername());
            pst.setString(2, objeto.getPassword());
            pst.setString(3, objeto.getRol());
            pst.setString(4, objeto.getNombre_completo());
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al guardar usuario: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Usuario objeto) {
        String sql = "UPDATE usuarios SET username = ?, password = ?, rol = ?, nombre_completo = ? WHERE id_usuario = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setString(1, objeto.getUsername());
            pst.setString(2, objeto.getPassword());
            pst.setString(3, objeto.getRol());
            pst.setString(4, objeto.getNombre_completo());
            pst.setInt(5, objeto.getId_usuario());
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar usuario: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "UPDATE usuarios SET estado = 0 WHERE id_usuario = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setInt(1, id);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al eliminar usuario: " + e.getMessage());
            return false;
        }
    }

    public List<Usuario> listarUsuariosGestion() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE rol IN ('ADMIN', 'DOCENTE') AND estado = 1";
        try (Connection cn = Conexion.conectar();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Usuario user = new Usuario();
                user.setId_usuario(rs.getInt("id_usuario"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRol(rs.getString("rol"));
                user.setNombre_completo(rs.getString("nombre_completo"));
                lista.add(user);
            }
        } catch (SQLException e) { 
            System.out.println("Error al listar usuarios: " + e.getMessage());
        }
        return lista;
    }

    
    
}
