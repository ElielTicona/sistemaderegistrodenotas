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
public class Eval {
    private int crisIdEval;
    private int crisIdCurso;
    private String crisTitulo;
    private String crisTipo; 
    private Timestamp crisFechaCreacion;
    private String crisFechaExamen; 
    private int crisEstado;

    public Eval() {}

    public int getIdEval() {
        return crisIdEval;
    }

    public void setIdEval(int crisIdEval) {
        this.crisIdEval = crisIdEval;
    }

    public int getIdCurso() {
        return crisIdCurso;
    }

    public void setIdCurso(int crisIdCurso) {
        this.crisIdCurso = crisIdCurso;
    }

    public String getTitulo() {
        return crisTitulo;
    }

    public void setTitulo(String crisTitulo) {
        this.crisTitulo = crisTitulo;
    }

    public String getTipo() {
        return crisTipo;
    }

    public void setTipo(String crisTipo) {
        this.crisTipo = crisTipo;
    }

    public Timestamp getFechaCreacion() {
        return crisFechaCreacion;
    }

    public void setFechaCreacion(Timestamp crisFechaCreacion) {
        this.crisFechaCreacion = crisFechaCreacion;
    }

    public String getFechaExamen() {
        return crisFechaExamen;
    }

    public void setFechaExamen(String crisFechaExamen) {
        this.crisFechaExamen = crisFechaExamen;
    }

    public int getEstado() {
        return crisEstado;
    }

    public void setEstado(int crisEstado) {
        this.crisEstado = crisEstado;
    }

    
    
    
    
}
