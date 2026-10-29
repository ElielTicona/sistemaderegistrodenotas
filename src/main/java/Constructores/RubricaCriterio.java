/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Constructores;

/**
 *
 * @author KriZ
 */
public class RubricaCriterio {
    private int id_criterio;
    private int id_evaluacion;
    private String nombre_criterio;
    private String descripcion;
    private double puntaje_max;

    public RubricaCriterio() {}

    public int getId_criterio() {
        return id_criterio;
    }

    public void setId_criterio(int id_criterio) {
        this.id_criterio = id_criterio;
    }

    public int getId_evaluacion() {
        return id_evaluacion;
    }

    public void setId_evaluacion(int id_evaluacion) {
        this.id_evaluacion = id_evaluacion;
    }

    public String getNombre_criterio() {
        return nombre_criterio;
    }

    public void setNombre_criterio(String nombre_criterio) {
        this.nombre_criterio = nombre_criterio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPuntaje_max() {
        return puntaje_max;
    }

    public void setPuntaje_max(double puntaje_max) {
        this.puntaje_max = puntaje_max;
    }
    
    
    
}
