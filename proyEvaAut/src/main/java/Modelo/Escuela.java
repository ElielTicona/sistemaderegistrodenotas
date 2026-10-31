/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

/**
 *
 * @author KriZ
 */
public class Escuela {
    private int crisIdEsc;
    private String crisNomEsc;

    public Escuela() {}

    public Escuela(int crisIdEsc, String crisNomEsc) {
        this.crisIdEsc = crisIdEsc;
        this.crisNomEsc = crisNomEsc;
    }

    public int getIdEsc() {
        return crisIdEsc;
    }

    public void setIdEsc(int crisIdEsc) {
        this.crisIdEsc = crisIdEsc;
    }

    public String getNomEsc() {
        return crisNomEsc;
    }

    public void setNomEsc(String crisNomEsc) {
        this.crisNomEsc = crisNomEsc;
    }
    
    @Override
    public String toString() {
        return crisNomEsc;
    }
    
}
