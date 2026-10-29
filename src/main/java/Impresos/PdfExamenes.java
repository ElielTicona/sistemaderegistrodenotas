package Impresos;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.*;
import java.awt.geom.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

/** PDF A4 de un unico archivo: cuadernillos primero, fichas dobles despues. */
public final class PdfExamenes {
    private static final float MM = 210f, ALT = 297f;
    private static final int PIX = 1654; // 200 dpi en ancho A4
    private static final float QR_FICHA_MM = 40f; // 4,0 cm exterior, ~3,1 cm de codigo y quiet zone
    private static final float QR_CUADERNILLO_MM = 28f;

    // Dos columnas y alternativas adaptativas (4 por fila, 2 por fila o vertical).
    private static final float[] COL_X = {13f, 108f};
    private static final float COL_Q_WIDTH = 82f;
    private static final float COL_OP_WIDTH = 81f;
    private static final float FULL_BOTTOM = 283f;
    private static final float HALF_SECTION = 148.5f;
    private static final float HALF_BOTTOM_REL = 141f;
    private static final float Q_PT = 10.2f, A_PT = 9.1f;
    private static final float Q_STEP = 5.4f, A_STEP = 4.75f, GAP = 2.8f;
    private static final FontRenderContext FRC = new FontRenderContext(new AffineTransform(), true, true);

    private final List<byte[]> pages = new ArrayList<>();
    private BufferedImage img;
    private Graphics2D g;
    private String titulo, curso, alumno, codigo;
    public static final Path BASE = Path.of(System.getProperty("user.home"), "SistemaEvaluacion", "impresos");

    private static final class FilaAlternativas {
        final List<String> textos;
        final float[] desplazamientos;
        FilaAlternativas(List<String> textos, float[] desplazamientos) {
            this.textos = textos;
            this.desplazamientos = desplazamientos;
        }
    }

    private static final class BloquePregunta {
        final List<String> enunciado;
        final List<FilaAlternativas> filas;
        final float alto;
        BloquePregunta(List<String> enunciado, List<FilaAlternativas> filas) {
            this.enunciado = enunciado;
            this.filas = filas;
            this.alto = enunciado.size() * Q_STEP + filas.size() * A_STEP + GAP;
        }
    }

    private static final class PlanSeccion {
        final int division, fin;
        PlanSeccion(int division, int fin) {
            this.division = division;
            this.fin = fin;
        }
    }

    private void nuevaPagina() throws IOException {
        if (img != null) {
            pages.add(comprimirSinPerdida(img));
            img = null;
        }
        img = new BufferedImage(PIX, Math.round(PIX * ALT / MM), BufferedImage.TYPE_INT_RGB);
        g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale((double) PIX / MM, (double) PIX / MM);
        g.setColor(Color.BLACK);
    }

    private Font fuente(float pt, boolean negrita) {
        return new Font("SansSerif", negrita ? Font.BOLD : Font.PLAIN, 1).deriveFont(pt * 25.4f / 72f);
    }

    private void tipografia(float pt, boolean negrita) {
        g.setFont(fuente(pt, negrita));
    }

    private float anchoTexto(String texto, float pt, boolean negrita) {
        return (float) fuente(pt, negrita).getStringBounds(texto, FRC).getWidth();
    }

    private void txt(String s, float x, float y, float pt, boolean negrita) {
        tipografia(pt, negrita);
        g.setColor(new Color(34, 43, 50));
        g.drawString(s == null ? "" : s, x, y);
    }

    private void linea(float x, float y, float x2, float y2) {
        g.setColor(new Color(160, 170, 177));
        g.setStroke(new BasicStroke(.2f));
        g.draw(new Line2D.Float(x, y, x2, y2));
    }

    /** Saltos medidos en milimetros. Parte tambien palabras largas para no desbordar columnas. */
    private List<String> partir(String text, float ancho, float puntos, boolean negrita) {
        List<String> lineas = new ArrayList<>();
        if (text == null) return List.of("");
        for (String par : text.replace("\r", "").split("\n", -1)) {
            StringBuilder actual = new StringBuilder();
            for (String palabra : par.trim().split("\\s+")) {
                if (palabra.isEmpty()) continue;
                String candidato = actual.isEmpty() ? palabra : actual + " " + palabra;
                if (anchoTexto(candidato, puntos, negrita) <= ancho) {
                    actual.setLength(0);
                    actual.append(candidato);
                    continue;
                }
                if (!actual.isEmpty()) {
                    lineas.add(actual.toString());
                    actual.setLength(0);
                }
                // Enunciados con tokens sin espacios (URLs, formulas largas, etc.).
                for (int off = 0; off < palabra.length();) {
                    int cp = palabra.codePointAt(off);
                    String letra = new String(Character.toChars(cp));
                    String propuesto = actual + letra;
                    if (!actual.isEmpty() && anchoTexto(propuesto, puntos, negrita) > ancho) {
                        lineas.add(actual.toString());
                        actual.setLength(0);
                    }
                    actual.append(letra);
                    off += Character.charCount(cp);
                }
            }
            lineas.add(actual.toString());
        }
        return lineas;
    }

    private void qr(String token, float x, float y, float lado) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, token.matches("E3[0-9A-Z]{12}") ? ErrorCorrectionLevel.Q : ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 4);
        BitMatrix matrix = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 0, 0, hints);
        Object antialiasAnterior = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(Color.WHITE);
            g.fill(new Rectangle2D.Float(x, y, lado, lado));
            g.setColor(Color.BLACK);
            float unidadX = lado / matrix.getWidth(), unidadY = lado / matrix.getHeight();
            for (int fila = 0; fila < matrix.getHeight(); fila++) {
                for (int columna = 0; columna < matrix.getWidth(); columna++) {
                    if (matrix.get(columna, fila)) {
                        g.fill(new Rectangle2D.Float(x + columna * unidadX, y + fila * unidadY, unidadX, unidadY));
                    }
                }
            }
        } finally {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasAnterior);
        }
    }

    private void cabeceraCuadernillo(RepositorioImpresos.Ejemplar ex, RepositorioImpresos.Lote lote, int pagina, float baseY, boolean compacta) throws Exception {
        float alturaBarra = compacta ? 5f : 7f;
        float qrLado = compacta ? 22f : QR_CUADERNILLO_MM;
        g.setColor(new Color(24, 79, 68));
        g.fill(new Rectangle2D.Float(0, baseY, MM, alturaBarra));

        txt(curso.toUpperCase(Locale.ROOT), 12, baseY + (compacta ? 15f : 20f), compacta ? 11.5f : 14f, true);
        txt(titulo, 12, baseY + (compacta ? 22f : 28f), compacta ? 9.2f : 12f, true);
        txt("ESTUDIANTE: " + alumno, 12, baseY + (compacta ? 29f : 39f), compacta ? 7.8f : 10f, true);
        txt("CODIGO: " + (ex.codigoUniv() == null ? ex.dni() : ex.codigoUniv()), 12, baseY + (compacta ? 35f : 46f), compacta ? 7.4f : 9f, false);
        txt("EJEMPLAR: " + codigo + "  |  PAGINA: " + pagina, 12, baseY + (compacta ? 41f : 53f), compacta ? 6.8f : 8f, false);
        qr(ex.token(), compacta ? 174f : 166f, baseY + (compacta ? 10f : 12f), qrLado);
        float lineaY = baseY + (compacta ? 45f : 58f);
        linea(12, lineaY, 198, lineaY);
        txt("Marque sus respuestas solo en la hoja de respuestas correspondiente.", 12, baseY + (compacta ? 51f : 65f), compacta ? 7.5f : 9f, false);
    }

    private List<BloquePregunta> prepararBloques(RepositorioImpresos.Ejemplar ex) {
        List<BloquePregunta> bloques = new ArrayList<>();
        for (RepositorioImpresos.Pregunta q : ex.preguntas()) {
            List<String> enunciado = partir(q.numero() + ". " + q.texto(), COL_Q_WIDTH, Q_PT, true);
            List<String> alternativas = new ArrayList<>();
            for (RepositorioImpresos.Opcion op : q.opciones()) {
                alternativas.add(op.letra() + ") " + op.texto());
            }
            List<FilaAlternativas> filas = new ArrayList<>();
            float disponible = COL_OP_WIDTH - 1f;
            // Una fila de cuatro si son respuestas cortas, como 2, 5 o 14.
            if (alternativas.size() == 4 && alternativas.stream().allMatch(t -> anchoTexto(t, A_PT, false) <= (disponible - 3f) / 4f)) {
                float paso = disponible / 4f;
                filas.add(new FilaAlternativas(alternativas, new float[]{0, paso, 2 * paso, 3 * paso}));
            } else if (alternativas.size() == 4 && alternativas.stream().allMatch(t -> anchoTexto(t, A_PT, false) <= (disponible - 2f) / 2f)) {
                float paso = disponible / 2f;
                filas.add(new FilaAlternativas(alternativas.subList(0, 2), new float[]{0, paso}));
                filas.add(new FilaAlternativas(alternativas.subList(2, 4), new float[]{0, paso}));
            } else {
                // Alternativas largas: ancho completo, ajustadas a varias lineas.
                for (String op : alternativas) {
                    for (String l : partir(op, disponible, A_PT, false)) {
                        filas.add(new FilaAlternativas(List.of(l), new float[]{0}));
                    }
                }
            }
            bloques.add(new BloquePregunta(enunciado, filas));
        }
        return bloques;
    }

    private void dibujarBloque(BloquePregunta bloque, float x, float y) {
        float yy = y;
        for (String linea : bloque.enunciado) {
            txt(linea, x, yy, Q_PT, true);
            yy += Q_STEP;
        }
        for (FilaAlternativas fila : bloque.filas) {
            for (int j = 0; j < fila.textos.size(); j++) {
                txt(fila.textos.get(j), x + 2f + fila.desplazamientos[j], yy, A_PT, false);
            }
            yy += A_STEP;
        }
    }

    /** Elige el mayor numero de preguntas que entra, equilibrando alturas de columnas. */
    private PlanSeccion planificar(List<BloquePregunta> bloques, int desde, float capacidad) {
        int mejorFin = desde, mejorDivision = desde;
        float mejorDiferencia = Float.POSITIVE_INFINITY;
        float sumaIzquierda = 0f;
        for (int division = desde + 1; division <= bloques.size(); division++) {
            sumaIzquierda += bloques.get(division - 1).alto;
            if (sumaIzquierda > capacidad + .01f) break;
            float sumaDerecha = 0f;
            int fin = division;
            while (fin < bloques.size() && sumaDerecha + bloques.get(fin).alto <= capacidad + .01f) {
                sumaDerecha += bloques.get(fin).alto;
                fin++;
            }
            float diferencia = Math.abs(sumaIzquierda - sumaDerecha);
            if (fin > mejorFin || (fin == mejorFin && diferencia < mejorDiferencia)) {
                mejorFin = fin;
                mejorDivision = division;
                mejorDiferencia = diferencia;
            }
        }
        if (mejorFin == desde) {
            throw new IllegalArgumentException("La pregunta " + bloques.get(desde).enunciado.get(0)
                    + " es demasiado larga para una columna; acorte el enunciado o sus alternativas.");
        }
        return new PlanSeccion(mejorDivision, mejorFin);
    }

    private float[] limitesSeccion(boolean compacta, float baseY) {
        return new float[]{baseY + (compacta ? 58f : 75f), compacta ? baseY + HALF_BOTTOM_REL : FULL_BOTTOM};
    }

    private int renderSeccion(List<BloquePregunta> bloques, int startIdx,
            RepositorioImpresos.Ejemplar ex, RepositorioImpresos.Lote lote,
            float baseY, boolean compacta, int pagina) throws Exception {
        float[] limites = limitesSeccion(compacta, baseY);
        PlanSeccion plan = planificar(bloques, startIdx, limites[1] - limites[0]);
        cabeceraCuadernillo(ex, lote, pagina, baseY, compacta);
        float y = limites[0];
        for (int i = startIdx; i < plan.division; i++) {
            dibujarBloque(bloques.get(i), COL_X[0], y);
            y += bloques.get(i).alto;
        }
        y = limites[0];
        for (int i = plan.division; i < plan.fin; i++) {
            dibujarBloque(bloques.get(i), COL_X[1], y);
            y += bloques.get(i).alto;
        }
        return plan.fin;
    }

    private boolean cabeEnMediaHoja(List<BloquePregunta> bloques) {
        if (bloques.isEmpty()) return false;
        try {
            PlanSeccion plan = planificar(bloques, 0, HALF_BOTTOM_REL - 58f);
            return plan.fin == bloques.size();
        } catch (IllegalArgumentException noCabeEnMediaPagina) {
            return false;
        }
    }

    private void cuadernilloCompleto(RepositorioImpresos.Lote lote, RepositorioImpresos.Ejemplar ex) throws Exception {
        alumno = ex.nombre();
        codigo = ex.codigo();
        titulo = lote.titulo();
        curso = lote.curso();
        List<BloquePregunta> bloques = prepararBloques(ex);
        int pagina = 1;
        int idx = 0;
        while (idx < bloques.size()) {
            nuevaPagina();
            int nuevoIdx = renderSeccion(bloques, idx, ex, lote, 0f, false, pagina);
            if (nuevoIdx <= idx) throw new IllegalStateException("La paginacion no avanzo");
            idx = nuevoIdx;
            g.dispose();
            pagina++;
        }
    }

    private void cuadernillosDobles(RepositorioImpresos.Lote lote,
            RepositorioImpresos.Ejemplar arriba, RepositorioImpresos.Ejemplar abajo) throws Exception {
        nuevaPagina();
        alumno = arriba.nombre();
        codigo = arriba.codigo();
        titulo = lote.titulo();
        curso = lote.curso();
        List<BloquePregunta> a = prepararBloques(arriba);
        if (renderSeccion(a, 0, arriba, lote, 0f, true, 1) != a.size()) {
            throw new IllegalStateException("El cuadernillo superior no cabe completo");
        }
        g.setColor(new Color(90, 105, 112));
        g.setStroke(new BasicStroke(.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{2, 2}, 0));
        g.draw(new Line2D.Float(5, HALF_SECTION, 205, HALF_SECTION));
        alumno = abajo.nombre();
        codigo = abajo.codigo();
        List<BloquePregunta> b = prepararBloques(abajo);
        if (renderSeccion(b, 0, abajo, lote, HALF_SECTION, true, 1) != b.size()) {
            throw new IllegalStateException("El cuadernillo inferior no cabe completo");
        }
        g.dispose();
    }

    private void cuadernillos(RepositorioImpresos.Lote lote) throws Exception {
        List<RepositorioImpresos.Ejemplar> list = lote.ejemplares();
        int i = 0;
        while (i < list.size()) {
            RepositorioImpresos.Ejemplar actual = list.get(i);
            List<BloquePregunta> bloquesActual = prepararBloques(actual);
            if (cabeEnMediaHoja(bloquesActual) && i + 1 < list.size()
                    && cabeEnMediaHoja(prepararBloques(list.get(i + 1)))) {
                cuadernillosDobles(lote, actual, list.get(i + 1));
                i += 2;
            } else {
                cuadernilloCompleto(lote, actual);
                i++;
            }
        }
    }

    private void cuadrado(float centerX, float centerY) {
        g.setColor(Color.BLACK);
        g.fill(new Rectangle2D.Float(centerX - 2.65f, centerY - 2.65f, 5.3f, 5.3f));
    }

    /** Posiciones milimetricas usadas tambien por DetectorOMR para proyectar marcas. */
    public static float bubbleX(int pregunta, int alternativa) {
        return (pregunta < 15 ? 41f : 141f) + 13f * alternativa;
    }

    public static float bubbleY(int pregunta) {
        return 63f + (pregunta % 15) * 5.35f;
    }

    public static float[] marcadoresX = {9, 201, 9, 201};
    public static float[] marcadoresY = {9, 9, 139.5f, 139.5f};

    private void mediaHoja(RepositorioImpresos.Ejemplar ex, RepositorioImpresos.Lote lote, float desplazamiento) throws Exception {
        Graphics2D original = g;
        g = (Graphics2D) original.create();
        g.translate(0, desplazamiento);
        for (int i = 0; i < 4; i++) cuadrado(marcadoresX[i], marcadoresY[i]);
        txt("HOJA DE RESPUESTAS", 15, 18.5f, 12, true);
        txt(lote.curso() + "  |  " + lote.titulo(), 15, 25, 9, true);
        txt("ALUMNO: " + ex.nombre(), 15, 31.5f, 9, false);
        txt("CODIGO: " + (ex.codigoUniv() == null ? ex.dni() : ex.codigoUniv()), 15, 37, 10, true);
        txt("EJEMPLAR: " + ex.codigo(), 112, 37, 9, true);
        qr(ex.token(), 157.5f, 10.5f, QR_FICHA_MM);
        linea(13, 52.5f, 197, 52.5f);
        for (int col = 0; col < 2; col++) {
            float dx = col == 0 ? 0 : 100;
            txt("No.", 17 + dx, 57.5f, 7.5f, true);
            for (int a = 0; a < 4; a++) txt("" + "ABCD".charAt(a), bubbleX(col * 15, a) - 1.2f, 57.5f, 8, true);
        }
        for (int n = 0; n < ex.preguntas().size(); n++) {
            float yy = bubbleY(n);
            txt(String.format(Locale.ROOT, "%02d", n + 1), 17 + (n / 15) * 100, yy + 1.1f, 8, true);
            for (int a = 0; a < 4; a++) {
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(.3f));
                g.draw(new Ellipse2D.Float(bubbleX(n, a) - 2.05f, yy - 2.05f, 4.1f, 4.1f));
            }
        }
        txt("Rellene UN circulo por pregunta. Evite tachones y doblar la hoja.", 15, 145f, 7, false);
        g.dispose();
        g = original;
    }

    private void fichas(RepositorioImpresos.Lote lote) throws Exception {
        List<RepositorioImpresos.Ejemplar> list = lote.ejemplares();
        for (int i = 0; i < list.size(); i += 2) {
            nuevaPagina();
            mediaHoja(list.get(i), lote, 0);
            g.setColor(new Color(90, 105, 112));
            g.setStroke(new BasicStroke(.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{2, 2}, 0));
            g.draw(new Line2D.Float(5, 148.5f, 205, 148.5f));
            if (i + 1 < list.size()) mediaHoja(list.get(i + 1), lote, 148.5f);
            g.dispose();
        }
    }

    public static Path generar(RepositorioImpresos.Lote lote) throws Exception {
        if (lote.ejemplares().isEmpty()) throw new IllegalArgumentException("Lote sin ejemplares");
        PdfExamenes b = new PdfExamenes();
        b.cuadernillos(lote);
        b.fichas(lote);
        if (b.img != null) {
            b.pages.add(comprimirSinPerdida(b.img));
            b.img = null;
        }
        Files.createDirectories(BASE);
        Path dst = BASE.resolve("Evaluacion_" + lote.evaluacion() + "_Generacion_" + lote.numero() + "_Lote_" + lote.id() + ".pdf");
        Path temporal = Files.createTempFile(BASE, "creando_", ".pdf");
        try {
            escribirPDF(temporal, b.pages);
            Files.move(temporal, dst, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporal);
        }
        return dst;
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static void write(ByteArrayOutputStream o, String s) throws IOException {
        o.write(ascii(s));
    }

    /** RGB crudo comprimido con FlateDecode: evita artefactos JPEG en los QR. */
    private static byte[] comprimirSinPerdida(BufferedImage im) throws IOException {
        ByteArrayOutputStream resultado = new ByteArrayOutputStream();
        int ancho = im.getWidth();
        int[] pixels = new int[ancho];
        byte[] rgb = new byte[ancho * 3];
        try (DeflaterOutputStream deflactado = new DeflaterOutputStream(resultado)) {
            for (int yy = 0; yy < im.getHeight(); yy++) {
                im.getRGB(0, yy, ancho, 1, pixels, 0, ancho);
                for (int xx = 0; xx < ancho; xx++) {
                    int color = pixels[xx], pos = xx * 3;
                    rgb[pos] = (byte) (color >>> 16);
                    rgb[pos + 1] = (byte) (color >>> 8);
                    rgb[pos + 2] = (byte) color;
                }
                deflactado.write(rgb);
            }
        }
        return resultado.toByteArray();
    }

    private static void escribirPDF(Path ruta, List<byte[]> imgs) throws IOException {
        int total = 2 + imgs.size() * 3;
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        List<Integer> off = new ArrayList<>(total + 1);
        off.add(0);
        write(o, "%PDF-1.4\n");
        off.add(o.size());
        write(o, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < imgs.size(); i++) kids.append(3 + i * 3).append(" 0 R ");
        off.add(o.size());
        write(o, "2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + imgs.size() + " >>\nendobj\n");
        for (int i = 0; i < imgs.size(); i++) {
            byte[] data = imgs.get(i);
            int base = 3 + 3 * i;
            off.add(o.size());
            write(o, base + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595.276 841.89] /Resources << /XObject << /Im0 " + (base + 2) + " 0 R >> >> /Contents " + (base + 1) + " 0 R >>\nendobj\n");
            byte[] stream = ascii("q 595.276 0 0 841.89 0 0 cm /Im0 Do Q\n");
            off.add(o.size());
            write(o, (base + 1) + " 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
            o.write(stream);
            write(o, "endstream\nendobj\n");
            off.add(o.size());
            write(o, (base + 2) + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + PIX + " /Height " + Math.round(PIX * ALT / MM) + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length " + data.length + " >>\nstream\n");
            o.write(data);
            write(o, "\nendstream\nendobj\n");
        }
        int xref = o.size();
        write(o, "xref\n0 " + (total + 1) + "\n0000000000 65535 f \n");
        for (int i = 1; i <= total; i++) write(o, String.format(Locale.ROOT, "%010d 00000 n \n", off.get(i)));
        write(o, "trailer\n<< /Size " + (total + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        Files.write(ruta, o.toByteArray());
    }
}
