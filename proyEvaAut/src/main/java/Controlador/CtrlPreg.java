/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controlador;

import Conexion.Conexion;
import Modelo.Preg;
import Modelo.Opc;
import java.sql.*;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class CtrlPreg {
    
    public int guardarPreg(Preg crisObj) {
        int crisIdPreg = 0;
        String crisSql = "INSERT INTO preguntas (id_evaluacion, texto_pregunta, puntaje_max) VALUES (?,?,?)";
        
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql, Statement.RETURN_GENERATED_KEYS)) {
            
            crisPst.setInt(1, crisObj.getIdEval());
            crisPst.setString(2, crisObj.getTextoPreg());
            crisPst.setDouble(3, crisObj.getPuntajeMax());
            crisPst.executeUpdate();
            
            ResultSet crisRs = crisPst.getGeneratedKeys();
            if (crisRs.next()) crisIdPreg = crisRs.getInt(1);
        } catch (SQLException crisE) {
            System.out.println("Error al guardar pregunta: " + crisE.getMessage());
        }
        return crisIdPreg;
    }

    public boolean guardarOpc(Opc crisObj) {
        String crisSql = "INSERT INTO opciones (id_pregunta, texto_opcion, es_correcta) VALUES (?,?,?)";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setInt(1, crisObj.getIdPreg());
            crisPst.setString(2, crisObj.getTextoOpcion());
            crisPst.setBoolean(3, crisObj.isEsCorrecta());
            
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al guardar opción: " + crisE.getMessage());
            return false;
        }
    }
    

    public List<Modelo.Preg> listarPregEval(int crisIdEval) {
        List<Modelo.Preg> crisLista = new java.util.ArrayList<>();
        String crisSql = "SELECT * FROM preguntas WHERE id_evaluacion = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setInt(1, crisIdEval);
            ResultSet crisRs = crisPst.executeQuery();
            while (crisRs.next()) {
                Modelo.Preg crisP = new Modelo.Preg();
                crisP.setIdPreg(crisRs.getInt("id_pregunta"));
                crisP.setIdEval(crisRs.getInt("id_evaluacion"));
                crisP.setTextoPreg(crisRs.getString("texto_pregunta"));
                crisP.setPuntajeMax(crisRs.getDouble("puntaje_max"));
                crisLista.add(crisP);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al listar preguntas: " + crisE.getMessage());
        }
        return crisLista;
    }

    public List<Modelo.Opc> listarOpcPreg(int crisIdPreg) {
        List<Modelo.Opc> crisLista = new java.util.ArrayList<>();
        String crisSql = "SELECT * FROM opciones WHERE id_pregunta = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setInt(1, crisIdPreg);
            ResultSet crisRs = crisPst.executeQuery();
            while (crisRs.next()) {
                Modelo.Opc crisO = new Modelo.Opc();
                crisO.setIdOpcion(crisRs.getInt("id_opcion"));
                crisO.setIdPreg(crisRs.getInt("id_pregunta"));
                crisO.setTextoOpcion(crisRs.getString("texto_opcion"));
                crisO.setEsCorrecta(crisRs.getBoolean("es_correcta"));
                crisLista.add(crisO);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al listar opciones: " + crisE.getMessage());
        }
        return crisLista;
    }
}
