/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controladores;

import Conexion.Conexion;
import Constructores.Estudiante;
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
public class Ctrl_Estudiante {
    
    
    
    public boolean existeEstudiante(String dni, String codigo) {
        String sql = "SELECT dni FROM estudiantes WHERE dni = ? OR codigo_univ = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setString(1, dni);
            pst.setString(2, codigo);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Error al validar duplicados: " + e.getMessage());
            return false;
        }
    }

   public boolean guardar(Estudiante objeto) {
        String sqlEst = "INSERT INTO estudiantes (dni, nombre_completo, codigo_univ, id_escuela, semestre, id_usuario, estado) VALUES (?,?,?,?,?,?,1)";

        try (Connection cn = Conexion.conectar()) {
            cn.setAutoCommit(false);
            String sqlUser = "INSERT INTO usuarios (username, password, rol, nombre_completo, estado) VALUES (?,?,'ESTUDIANTE',?,1)";
            int idUsuario = 0;
            try (PreparedStatement pstUser = cn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                pstUser.setString(1, objeto.getCodigo_univ());
                pstUser.setString(2, objeto.getCodigo_univ()); 
                pstUser.setString(3, objeto.getNombre_completo());
                pstUser.executeUpdate();
                ResultSet rs = pstUser.getGeneratedKeys();
                if (rs.next()) idUsuario = rs.getInt(1);
            }

            try (PreparedStatement pstEst = cn.prepareStatement(sqlEst)) {
                pstEst.setString(1, objeto.getCodigo_univ()); 
                pstEst.setString(2, objeto.getNombre_completo());
                pstEst.setString(3, objeto.getCodigo_univ()); 
                pstEst.setInt(4, objeto.getId_escuela());
                pstEst.setString(5, objeto.getSemestre());
                pstEst.setInt(6, idUsuario);
                pstEst.executeUpdate();
            }

            cn.commit();
            return true;
        } catch (SQLException e) {
            System.out.println("Error en guardado: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Estudiante objeto) {
        String sql = "UPDATE estudiantes SET nombre_completo = ?, id_escuela = ?, semestre = ? WHERE dni = ?";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setString(1, objeto.getNombre_completo());
            pst.setInt(2, objeto.getId_escuela());
            pst.setString(3, objeto.getSemestre());
            pst.setString(4, objeto.getDni());
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al actualizar estudiante: " + e.getMessage());
            return false;
        }
    }
    
    public boolean eliminar(String dni) {
        String sql = "UPDATE estudiantes e "
                   + "JOIN usuarios u ON e.id_usuario = u.id_usuario "
                   + "SET e.estado = 0, u.estado = 0 "
                   + "WHERE e.dni = ?";
        
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setString(1, dni);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al eliminar estudiante: " + e.getMessage());
            return false;
        }
    }

    public List<Estudiante> listarEstudiantes(String valor) {
        List<Estudiante> lista = new ArrayList<>();
        String sql = "SELECT * FROM estudiantes WHERE (dni LIKE ? OR nombre_completo LIKE ?) AND estado = 1";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setString(1, "%" + valor + "%");
            pst.setString(2, "%" + valor + "%");
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Estudiante est = new Estudiante();
                    est.setDni(rs.getString("dni"));
                    est.setNombre_completo(rs.getString("nombre_completo"));
                    est.setCodigo_univ(rs.getString("codigo_univ"));
                    est.setId_escuela(rs.getInt("id_escuela"));
                    est.setSemestre(rs.getString("semestre"));
                    lista.add(est);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
        return lista;
    }

    public int obtenerIdEscuelaPorNombre(String nombreEscuela) {
        int id = 0;
        String sql = "SELECT id_escuela FROM escuelas WHERE UPPER(nombre_escuela) = UPPER(?)";
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setString(1, nombreEscuela.trim());
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) id = rs.getInt("id_escuela");
            }
        } catch (SQLException e) {
            System.out.println("Error en búsqueda: " + e.getMessage());
        }
        return id;
    }


    public List<Estudiante> listarPorFiltro(int idEscuela, int semestre) {
        List<Estudiante> lista = new ArrayList<>();
        String sql = "SELECT * FROM estudiantes WHERE id_escuela = ? AND semestre = ? AND estado = 1";
        
        try (Connection cn = Conexion.conectar();
             PreparedStatement pst = cn.prepareStatement(sql)) {
            
            pst.setInt(1, idEscuela);
            pst.setInt(2, semestre);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                Estudiante est = new Estudiante();
                est.setDni(rs.getString("dni"));
                est.setNombre_completo(rs.getString("nombre_completo"));
                est.setCodigo_univ(rs.getString("codigo_univ"));
                lista.add(est);
            }
        } catch (SQLException e) {
            System.out.println("Error al filtrar estudiantes: " + e.getMessage());
        }
        return lista;
    }
    
    public int[] importarExcel(String rutaArchivo) {
        int[] resultados = {0, 0}; 
        try (FileInputStream fis = new FileInputStream(new File(rutaArchivo));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> filas = sheet.iterator();
            if (filas.hasNext()) filas.next();

            while (filas.hasNext()) {
                Row filaActual = filas.next();
                try {
                    String codigo   = getCellValue(filaActual.getCell(0));
                    String nombre   = getCellValue(filaActual.getCell(1));
                    String escuela  = getCellValue(filaActual.getCell(2));
                    String semestre = getCellValue(filaActual.getCell(3));
  
                    int idEscuelaReal = obtenerIdEscuelaPorNombre(escuela);

                    if (!codigo.isEmpty() && idEscuelaReal > 0 && !existeEstudiante(codigo, codigo)) {
                        Constructores.Estudiante est = new Constructores.Estudiante();
                        est.setCodigo_univ(codigo);
                        est.setDni(codigo); 
                        est.setNombre_completo(nombre.toUpperCase());
                        est.setId_escuela(idEscuelaReal);
                        est.setSemestre(semestre);

                        if (this.guardar(est)) {
                            resultados[0]++;
                        } else {
                            resultados[1]++;
                        }
                    } else {
                        resultados[1]++; 
                    }
                } catch (Exception e) { resultados[1]++; }
            }
        } catch (Exception e) { 
            System.out.println("Error crítico en Excel: " + e.getMessage()); 
        }
        return resultados;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: 
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                return String.valueOf((long) cell.getNumericCellValue());
            default: return "";
        }
    }
  
}
