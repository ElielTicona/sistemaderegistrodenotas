/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controlador;

import Conexion.Conexion;
import Modelo.Estud;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;

/**
 *
 * @author KriZ
 */
public class CtrlEstud {
    
    
    
    public boolean existeEstud(String crisDni, String crisCod) {
        String crisSql = "SELECT dni FROM estudiantes WHERE dni = ? OR codigo_univ = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setString(1, crisDni);
            crisPst.setString(2, crisCod);
            try (ResultSet crisRs = crisPst.executeQuery()) {
                return crisRs.next();
            }
        } catch (SQLException crisE) {
            System.out.println("Error al validar duplicados: " + crisE.getMessage());
            return false;
        }
    }

   public boolean guardar(Estud crisObj) {
        String crisSqlEst = "INSERT INTO estudiantes (dni, nombre_completo, codigo_univ, id_escuela, semestre, id_usuario, estado) VALUES (?,?,?,?,?,?,1)";

        try (Connection crisCn = Conexion.conectar()) {
            crisCn.setAutoCommit(false);
            String crisSqlUser = "INSERT INTO usuarios (username, password, rol, nombre_completo, estado) VALUES (?,?,'ESTUDIANTE',?,1)";
            int crisIdUsu = 0;
            try (PreparedStatement crisPstUser = crisCn.prepareStatement(crisSqlUser, Statement.RETURN_GENERATED_KEYS)) {
                crisPstUser.setString(1, crisObj.getCodUniv());
                crisPstUser.setString(2, crisObj.getCodUniv()); 
                crisPstUser.setString(3, crisObj.getNomCompleto());
                crisPstUser.executeUpdate();
                ResultSet crisRs = crisPstUser.getGeneratedKeys();
                if (crisRs.next()) crisIdUsu = crisRs.getInt(1);
            }

            try (PreparedStatement crisPstEst = crisCn.prepareStatement(crisSqlEst)) {
                crisPstEst.setString(1, crisObj.getCodUniv()); 
                crisPstEst.setString(2, crisObj.getNomCompleto());
                crisPstEst.setString(3, crisObj.getCodUniv()); 
                crisPstEst.setInt(4, crisObj.getIdEsc());
                crisPstEst.setString(5, crisObj.getSemestre());
                crisPstEst.setInt(6, crisIdUsu);
                crisPstEst.executeUpdate();
            }

            crisCn.commit();
            return true;
        } catch (SQLException crisE) {
            System.out.println("Error en guardado: " + crisE.getMessage());
            return false;
        }
    }

    public boolean actualizar(Estud crisObj) {
        String crisSql = "UPDATE estudiantes SET nombre_completo = ?, id_escuela = ?, semestre = ? WHERE dni = ?";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setString(1, crisObj.getNomCompleto());
            crisPst.setInt(2, crisObj.getIdEsc());
            crisPst.setString(3, crisObj.getSemestre());
            crisPst.setString(4, crisObj.getDni());
            
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al actualizar estudiante: " + crisE.getMessage());
            return false;
        }
    }
    
    public boolean eliminar(String crisDni) {
        String crisSql = "UPDATE estudiantes e "
                   + "JOIN usuarios u ON e.id_usuario = u.id_usuario "
                   + "SET e.estado = 0, u.estado = 0 "
                   + "WHERE e.dni = ?";
        
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setString(1, crisDni);
            return crisPst.executeUpdate() > 0;
        } catch (SQLException crisE) {
            System.out.println("Error al eliminar estudiante: " + crisE.getMessage());
            return false;
        }
    }

    public List<Estud> listarEstud(String crisValor) {
        List<Estud> crisLista = new ArrayList<>();
        String crisSql = "SELECT * FROM estudiantes WHERE (dni LIKE ? OR nombre_completo LIKE ?) AND estado = 1";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setString(1, "%" + crisValor + "%");
            crisPst.setString(2, "%" + crisValor + "%");
            try (ResultSet crisRs = crisPst.executeQuery()) {
                while (crisRs.next()) {
                    Estud crisEst = new Estud();
                    crisEst.setDni(crisRs.getString("dni"));
                    crisEst.setNomCompleto(crisRs.getString("nombre_completo"));
                    crisEst.setCodUniv(crisRs.getString("codigo_univ"));
                    crisEst.setIdEsc(crisRs.getInt("id_escuela"));
                    crisEst.setSemestre(crisRs.getString("semestre"));
                    crisLista.add(crisEst);
                }
            }
        } catch (SQLException crisE) {
            System.out.println("Error al listar: " + crisE.getMessage());
        }
        return crisLista;
    }

    public int buscarIdEscNom(String crisNomEsc) {
        int crisId = 0;
        String crisSql = "SELECT id_escuela FROM escuelas WHERE UPPER(nombre_escuela) = UPPER(?)";
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            crisPst.setString(1, crisNomEsc.trim());
            try (ResultSet crisRs = crisPst.executeQuery()) {
                if (crisRs.next()) crisId = crisRs.getInt("id_escuela");
            }
        } catch (SQLException crisE) {
            System.out.println("Error en búsqueda: " + crisE.getMessage());
        }
        return crisId;
    }


    public List<Estud> listarFiltro(int crisIdEsc, int crisSemestre) {
        List<Estud> crisLista = new ArrayList<>();
        String crisSql = "SELECT * FROM estudiantes WHERE id_escuela = ? AND semestre = ? AND estado = 1";
        
        try (Connection crisCn = Conexion.conectar();
             PreparedStatement crisPst = crisCn.prepareStatement(crisSql)) {
            
            crisPst.setInt(1, crisIdEsc);
            crisPst.setInt(2, crisSemestre);
            ResultSet crisRs = crisPst.executeQuery();
            
            while (crisRs.next()) {
                Estud crisEst = new Estud();
                crisEst.setDni(crisRs.getString("dni"));
                crisEst.setNomCompleto(crisRs.getString("nombre_completo"));
                crisEst.setCodUniv(crisRs.getString("codigo_univ"));
                crisLista.add(crisEst);
            }
        } catch (SQLException crisE) {
            System.out.println("Error al filtrar estudiantes: " + crisE.getMessage());
        }
        return crisLista;
    }
    
    public int[] importarExcel(String crisRutaArch) {
        int[] crisResult = {0, 0}; 
        try (FileInputStream crisFis = new FileInputStream(new File(crisRutaArch));
             Workbook crisWorkbook = new XSSFWorkbook(crisFis)) {

            Sheet crisSheet = crisWorkbook.getSheetAt(0);
            Iterator<Row> crisFilas = crisSheet.iterator();
            if (crisFilas.hasNext()) crisFilas.next();

            while (crisFilas.hasNext()) {
                Row crisFilaActual = crisFilas.next();
                try {
                    String crisCod   = leerCelda(crisFilaActual.getCell(0));
                    String crisNom   = leerCelda(crisFilaActual.getCell(1));
                    String crisEsc  = leerCelda(crisFilaActual.getCell(2));
                    String crisSemestre = leerCelda(crisFilaActual.getCell(3));
  
                    int crisIdEscReal = buscarIdEscNom(crisEsc);

                    if (!crisCod.isEmpty() && crisIdEscReal > 0 && !existeEstud(crisCod, crisCod)) {
                        Modelo.Estud crisEst = new Modelo.Estud();
                        crisEst.setCodUniv(crisCod);
                        crisEst.setDni(crisCod); 
                        crisEst.setNomCompleto(crisNom.toUpperCase());
                        crisEst.setIdEsc(crisIdEscReal);
                        crisEst.setSemestre(crisSemestre);

                        if (this.guardar(crisEst)) {
                            crisResult[0]++;
                        } else {
                            crisResult[1]++;
                        }
                    } else {
                        crisResult[1]++; 
                    }
                } catch (Exception crisE) { crisResult[1]++; }
            }
        } catch (Exception crisE) { 
            System.out.println("Error crítico en Excel: " + crisE.getMessage()); 
        }
        return crisResult;
    }

    private String leerCelda(Cell crisCell) {
        if (crisCell == null) return "";
        switch (crisCell.getCellType()) {
            case STRING: return crisCell.getStringCellValue();
            case NUMERIC: 
                if (DateUtil.isCellDateFormatted(crisCell)) return crisCell.getDateCellValue().toString();
                return String.valueOf((long) crisCell.getNumericCellValue());
            default: return "";
        }
    }
  
}
