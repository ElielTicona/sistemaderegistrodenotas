/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controlador;

import Conexion.Conexion;
import Modelo.RubCrit;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author KriZ
 */
public class CtrlRub {
    
    public boolean guardarCrit(RubCrit crisObj) {
        String crisSql = "INSERT INTO rubricas_criterios (id_evaluacion, nombre_criterio, descripcion, puntaje_max) VALUES (?,?,?,?)";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setInt(1, crisObj.getIdEval());
            crisPst.setString(2, crisObj.getNomCriterio());
            crisPst.setString(3, crisObj.getDesc());
            crisPst.setDouble(4, crisObj.getPuntajeMax());
            
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error en Ctrl_Rubrica: " + crisE.getMessage());
            return false;
        }
    }

    public List<RubCrit> listarCrit(int crisIdEval) {
        List<RubCrit> crisLista = new ArrayList<>();
        String crisSql = "SELECT * FROM rubricas_criterios WHERE id_evaluacion = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setInt(1, crisIdEval);
            ResultSet crisRs = crisPst.executeQuery();
            while (crisRs.next()) {
                RubCrit crisRc = new RubCrit();
                crisRc.setIdCriterio(crisRs.getInt("id_criterio"));
                crisRc.setNomCriterio(crisRs.getString("nombre_criterio"));
                crisRc.setPuntajeMax(crisRs.getDouble("puntaje_max"));
                crisLista.add(crisRc);
            }
        } catch (SQLException crisE) { }
        return crisLista;
    }
    
}
