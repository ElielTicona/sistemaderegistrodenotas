/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

/**
 *
 * @author KriZ
 */
public class RubCrit {
    private int crisIdCriterio;
    private int crisIdEval;
    private String crisNomCriterio;
    private String crisDesc;
    private double crisPuntajeMax;

    public RubCrit() {}

    public int getIdCriterio() {
        return crisIdCriterio;
    }

    public void setIdCriterio(int crisIdCriterio) {
        this.crisIdCriterio = crisIdCriterio;
    }

    public int getIdEval() {
        return crisIdEval;
    }

    public void setIdEval(int crisIdEval) {
        this.crisIdEval = crisIdEval;
    }

    public String getNomCriterio() {
        return crisNomCriterio;
    }

    public void setNomCriterio(String crisNomCriterio) {
        this.crisNomCriterio = crisNomCriterio;
    }

    public String getDesc() {
        return crisDesc;
    }

    public void setDesc(String crisDesc) {
        this.crisDesc = crisDesc;
    }

    public double getPuntajeMax() {
        return crisPuntajeMax;
    }

    public void setPuntajeMax(double crisPuntajeMax) {
        this.crisPuntajeMax = crisPuntajeMax;
    }
    
    
    
}
