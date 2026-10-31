/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

/**
 *
 * @author KriZ
 */
public class Usu {
    private int crisIdUsu;
    private String crisUsername;
    private String crisPassword;
    private String crisRol;
    private String crisNomCompleto;
    private int crisEstado;

    public Usu() {}

    public Usu(int crisIdUsu, String crisUsername, String crisPassword, String crisRol, String crisNomCompleto, int crisEstado) {
        this.crisIdUsu = crisIdUsu;
        this.crisUsername = crisUsername;
        this.crisPassword = crisPassword;
        this.crisRol = crisRol;
        this.crisNomCompleto = crisNomCompleto;
        this.crisEstado = crisEstado;
    }

    public int getIdUsu() {
        return crisIdUsu;
    }

    public void setIdUsu(int crisIdUsu) {
        this.crisIdUsu = crisIdUsu;
    }

    public String getUsername() {
        return crisUsername;
    }

    public void setUsername(String crisUsername) {
        this.crisUsername = crisUsername;
    }

    public String getPassword() {
        return crisPassword;
    }

    public void setPassword(String crisPassword) {
        this.crisPassword = crisPassword;
    }

    public String getRol() {
        return crisRol;
    }

    public void setRol(String crisRol) {
        this.crisRol = crisRol;
    }

    public String getNomCompleto() {
        return crisNomCompleto;
    }

    public void setNomCompleto(String crisNomCompleto) {
        this.crisNomCompleto = crisNomCompleto;
    }

    public int getEstado() {
        return crisEstado;
    }

    public void setEstado(int crisEstado) {
        this.crisEstado = crisEstado;
    }
    
    @Override
    public String toString() {
        return crisNomCompleto;
    }
    
}
