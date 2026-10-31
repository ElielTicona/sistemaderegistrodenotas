/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

/**
 *
 * @author KriZ
 */
public class Preg {
    
    private int crisIdPreg;
    private int crisIdEval;
    private String crisTextoPreg;
    private double crisPuntajeMax;

    public Preg() {}

    public int getIdPreg() {
        return crisIdPreg;
    }

    public void setIdPreg(int crisIdPreg) {
        this.crisIdPreg = crisIdPreg;
    }

    public int getIdEval() {
        return crisIdEval;
    }

    public void setIdEval(int crisIdEval) {
        this.crisIdEval = crisIdEval;
    }

    public String getTextoPreg() {
        return crisTextoPreg;
    }

    public void setTextoPreg(String crisTextoPreg) {
        this.crisTextoPreg = crisTextoPreg;
    }

    public double getPuntajeMax() {
        return crisPuntajeMax;
    }

    public void setPuntajeMax(double crisPuntajeMax) {
        this.crisPuntajeMax = crisPuntajeMax;
    }
    
    
    
}
