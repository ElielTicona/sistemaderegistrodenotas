/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

/**
 *
 * @author KriZ
 */
public class Opc {
    private int crisIdOpcion;
    private int crisIdPreg;
    private String crisTextoOpcion;
    private boolean crisEsCorrecta;

    public Opc() {}

    public int getIdOpcion() {
        return crisIdOpcion;
    }

    public void setIdOpcion(int crisIdOpcion) {
        this.crisIdOpcion = crisIdOpcion;
    }

    public int getIdPreg() {
        return crisIdPreg;
    }

    public void setIdPreg(int crisIdPreg) {
        this.crisIdPreg = crisIdPreg;
    }

    public String getTextoOpcion() {
        return crisTextoOpcion;
    }

    public void setTextoOpcion(String crisTextoOpcion) {
        this.crisTextoOpcion = crisTextoOpcion;
    }

    public boolean isEsCorrecta() {
        return crisEsCorrecta;
    }

    public void setEsCorrecta(boolean crisEsCorrecta) {
        this.crisEsCorrecta = crisEsCorrecta;
    }
    
    
    
}
