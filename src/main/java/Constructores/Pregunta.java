/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Constructores;

/**
 *
 * @author KriZ
 */
public class Pregunta {
    
    private int id_pregunta;
    private int id_evaluacion;
    private String texto_pregunta;
    private double puntaje_max;

    public Pregunta() {}

    public int getId_pregunta() {
        return id_pregunta;
    }

    public void setId_pregunta(int id_pregunta) {
        this.id_pregunta = id_pregunta;
    }

    public int getId_evaluacion() {
        return id_evaluacion;
    }

    public void setId_evaluacion(int id_evaluacion) {
        this.id_evaluacion = id_evaluacion;
    }

    public String getTexto_pregunta() {
        return texto_pregunta;
    }

    public void setTexto_pregunta(String texto_pregunta) {
        this.texto_pregunta = texto_pregunta;
    }

    public double getPuntaje_max() {
        return puntaje_max;
    }

    public void setPuntaje_max(double puntaje_max) {
        this.puntaje_max = puntaje_max;
    }
    
    
    
}
