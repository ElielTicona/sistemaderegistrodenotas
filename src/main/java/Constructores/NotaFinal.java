/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Constructores;

import java.sql.Timestamp;

/**
 *
 * @author KriZ
 */
public class NotaFinal {
    
    private int id_nota;
    private String dni_estudiante;
    private int id_evaluacion;
    private double puntaje_obtenido;
    private Timestamp fecha_registro;

    public NotaFinal() {}

    public int getId_nota() {
        return id_nota;
    }

    public void setId_nota(int id_nota) {
        this.id_nota = id_nota;
    }

    public String getDni_estudiante() {
        return dni_estudiante;
    }

    public void setDni_estudiante(String dni_estudiante) {
        this.dni_estudiante = dni_estudiante;
    }

    public int getId_evaluacion() {
        return id_evaluacion;
    }

    public void setId_evaluacion(int id_evaluacion) {
        this.id_evaluacion = id_evaluacion;
    }

    public double getPuntaje_obtenido() {
        return puntaje_obtenido;
    }

    public void setPuntaje_obtenido(double puntaje_obtenido) {
        this.puntaje_obtenido = puntaje_obtenido;
    }

    public Timestamp getFecha_registro() {
        return fecha_registro;
    }

    public void setFecha_registro(Timestamp fecha_registro) {
        this.fecha_registro = fecha_registro;
    }
    
    
    
}
