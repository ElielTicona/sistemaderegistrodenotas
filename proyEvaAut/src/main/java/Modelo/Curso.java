/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

/**
 *
 * @author KriZ
 */
public class Curso {
    
    private int crisIdCurso;
    private String crisNomCurso;
    private String crisCodCurso;
    private int crisIdDocente;
    private int crisIdEsc;
    private String crisSemestre;
    private int crisEstado;

    public Curso() {}

    public int getIdCurso() {
        return crisIdCurso;
    }

    public void setIdCurso(int crisIdCurso) {
        this.crisIdCurso = crisIdCurso;
    }

    public String getNomCurso() {
        return crisNomCurso;
    }

    public void setNomCurso(String crisNomCurso) {
        this.crisNomCurso = crisNomCurso;
    }

    public String getCodCurso() {
        return crisCodCurso;
    }

    public void setCodCurso(String crisCodCurso) {
        this.crisCodCurso = crisCodCurso;
    }

    public int getIdDocente() {
        return crisIdDocente;
    }

    public void setIdDocente(int crisIdDocente) {
        this.crisIdDocente = crisIdDocente;
    }

    public int getIdEsc() {
        return crisIdEsc;
    }

    public void setIdEsc(int crisIdEsc) {
        this.crisIdEsc = crisIdEsc;
    }

    public String getSemestre() {
        return crisSemestre;
    }

    public void setSemestre(String crisSemestre) {
        this.crisSemestre = crisSemestre;
    }

    public int getEstado() {
        return crisEstado;
    }

    public void setEstado(int crisEstado) {
        this.crisEstado = crisEstado;
    }
    
    @Override
    public String toString() {
        return crisNomCurso; 
    }
    
}
