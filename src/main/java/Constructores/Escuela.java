/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Constructores;

/**
 *
 * @author KriZ
 */
public class Escuela {
    private int id_escuela;
    private String nombre_escuela;

    public Escuela() {}

    public Escuela(int id_escuela, String nombre_escuela) {
        this.id_escuela = id_escuela;
        this.nombre_escuela = nombre_escuela;
    }

    public int getId_escuela() {
        return id_escuela;
    }

    public void setId_escuela(int id_escuela) {
        this.id_escuela = id_escuela;
    }

    public String getNombre_escuela() {
        return nombre_escuela;
    }

    public void setNombre_escuela(String nombre_escuela) {
        this.nombre_escuela = nombre_escuela;
    }
    
    @Override
    public String toString() {
        return nombre_escuela;
    }
    
}
