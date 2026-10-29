/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Constructores;

/**
 *
 * @author KriZ
 */
public class Usuario {
    private int id_usuario;
    private String username;
    private String password;
    private String rol;
    private String nombre_completo;
    private int estado;

    public Usuario() {}

    public Usuario(int id_usuario, String username, String password, String rol, String nombre_completo, int estado) {
        this.id_usuario = id_usuario;
        this.username = username;
        this.password = password;
        this.rol = rol;
        this.nombre_completo = nombre_completo;
        this.estado = estado;
    }

    public int getId_usuario() {
        return id_usuario;
    }

    public void setId_usuario(int id_usuario) {
        this.id_usuario = id_usuario;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getNombre_completo() {
        return nombre_completo;
    }

    public void setNombre_completo(String nombre_completo) {
        this.nombre_completo = nombre_completo;
    }

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }
    
    @Override
    public String toString() {
        return nombre_completo;
    }
    
}
