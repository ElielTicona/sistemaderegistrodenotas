/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author KriZ
 */
public class Conexion {
    public static Connection conectar() {
        try {
            Connection crisCn = DriverManager.getConnection("jdbc:mysql://localhost:3306/bd_evalinte", "root", "");
            return crisCn;
        } catch (SQLException crisE) {
            System.out.println("Error en la conexion local " + crisE);
        }
        return null;
    }
    
}
