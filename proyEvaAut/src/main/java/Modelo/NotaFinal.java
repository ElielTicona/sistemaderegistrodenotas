/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

import java.sql.Timestamp;

/**
 *
 * @author KriZ
 */
public class NotaFinal {
    
    private int crisIdNota;
    private String crisDniEstud;
    private int crisIdEval;
    private double crisPuntajeObtenido;
    private Timestamp crisFechaRegistro;

    public NotaFinal() {}

    public int getIdNota() {
        return crisIdNota;
    }

    public void setIdNota(int crisIdNota) {
        this.crisIdNota = crisIdNota;
    }

    public String getDniEstud() {
        return crisDniEstud;
    }

    public void setDniEstud(String crisDniEstud) {
        this.crisDniEstud = crisDniEstud;
    }

    public int getIdEval() {
        return crisIdEval;
    }

    public void setIdEval(int crisIdEval) {
        this.crisIdEval = crisIdEval;
    }

    public double getPuntajeObtenido() {
        return crisPuntajeObtenido;
    }

    public void setPuntajeObtenido(double crisPuntajeObtenido) {
        this.crisPuntajeObtenido = crisPuntajeObtenido;
    }

    public Timestamp getFechaRegistro() {
        return crisFechaRegistro;
    }

    public void setFechaRegistro(Timestamp crisFechaRegistro) {
        this.crisFechaRegistro = crisFechaRegistro;
    }
    
    
    
}
