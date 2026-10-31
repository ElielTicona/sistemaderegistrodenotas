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
    private static final float crisMm = 210f, crisAlt = 297f;
    private static final int crisPix = 1654; // 200 dpi en ancho A4
    private static final float crisQrFichaMm = 40f; // 4,0 cm exterior, ~3,1 cm de codigo y quiet zone
    private static final float crisQrCuadernilloMm = 28f;

    // Dos columnas y alternativas adaptativas (4 por fila, 2 por fila o vertical).
    private static final float[] crisColX = {13f, 108f};
    private static final float crisColQWidth = 82f;
    private static final float crisColOpWidth = 81f;
    private static final float crisFullBottom = 283f;
    private static final float crisHalfSection = 148.5f;
    private static final float crisHalfBottomRel = 141f;
    private static final float crisQPt = 10.2f, crisAPt = 9.1f;
    private static final float crisQStep = 5.4f, crisAStep = 4.75f, crisGap = 2.8f;
    private static final FontRenderContext crisFrc = new FontRenderContext(new AffineTransform(), true, true);

    private final List<byte[]> crisPages = new ArrayList<>();
    private BufferedImage crisImg;
    private Graphics2D crisG;
    private String crisTitulo, crisCurso, crisAlumno, crisCod;
    public static final Path BASE = Path.of(System.getProperty("user.home"), "SistemaEvaluacion", "impresos");

    private static final class FilaAlternativas {
        final List<String> textos;
        final float[] desplazamientos;
        FilaAlternativas(List<String> crisTextos, float[] crisDesplazamientos) {
            this.textos = crisTextos;
            this.desplazamientos = crisDesplazamientos;
        }
    }

    private static final class BloquePregunta {
        final List<String> enunciado;
        final List<FilaAlternativas> filas;
        final float alto;
        BloquePregunta(List<String> crisEnunciado, List<FilaAlternativas> crisFilas) {
            this.enunciado = crisEnunciado;
            this.filas = crisFilas;
            this.alto = crisEnunciado.size() * crisQStep + crisFilas.size() * crisAStep + crisGap;
        }
    }

    private static final class PlanSeccion {
        final int division, fin;
        PlanSeccion(int crisDivision, int crisFin) {
            this.division = crisDivision;
            this.fin = crisFin;
        }
    }

    private void nuevaPagina() throws IOException {
        if (crisImg != null) {
            crisPages.add(comprimirLossless(crisImg));
            crisImg = null;
        }
        crisImg = new BufferedImage(crisPix, Math.round(crisPix * crisAlt / crisMm), BufferedImage.TYPE_INT_RGB);
        crisG = crisImg.createGraphics();
        crisG.setColor(Color.WHITE);
        crisG.fillRect(0, 0, crisImg.getWidth(), crisImg.getHeight());
        crisG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        crisG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        crisG.scale((double) crisPix / crisMm, (double) crisPix / crisMm);
        crisG.setColor(Color.BLACK);
    }

    private Font fuente(float crisPt, boolean crisNegrita) {
        return new Font("SansSerif", crisNegrita ? Font.BOLD : Font.PLAIN, 1).deriveFont(crisPt * 25.4f / 72f);
    }

    private void tipografia(float crisPt, boolean crisNegrita) {
        crisG.setFont(fuente(crisPt, crisNegrita));
    }

    private float anchoTexto(String crisTexto, float crisPt, boolean crisNegrita) {
        return (float) fuente(crisPt, crisNegrita).getStringBounds(crisTexto, crisFrc).getWidth();
    }

    private void txt(String crisS, float crisX, float crisY, float crisPt, boolean crisNegrita) {
        tipografia(crisPt, crisNegrita);
        crisG.setColor(new Color(34, 43, 50));
        crisG.drawString(crisS == null ? "" : crisS, crisX, crisY);
    }

    private void linea(float crisX, float crisY, float crisX2, float crisY2) {
        crisG.setColor(new Color(160, 170, 177));
        crisG.setStroke(new BasicStroke(.2f));
        crisG.draw(new Line2D.Float(crisX, crisY, crisX2, crisY2));
    }

    /** Saltos medidos en milimetros. Parte tambien palabras largas para no desbordar columnas. */
    private List<String> partir(String crisText, float crisAncho, float crisPuntos, boolean crisNegrita) {
        List<String> crisLineas = new ArrayList<>();
        if (crisText == null) return List.of("");
        for (String crisPar : crisText.replace("\r", "").split("\n", -1)) {
            StringBuilder crisActual = new StringBuilder();
            for (String crisPalabra : crisPar.trim().split("\\s+")) {
                if (crisPalabra.isEmpty()) continue;
                String crisCandidato = crisActual.isEmpty() ? crisPalabra : crisActual + " " + crisPalabra;
                if (anchoTexto(crisCandidato, crisPuntos, crisNegrita) <= crisAncho) {
                    crisActual.setLength(0);
                    crisActual.append(crisCandidato);
                    continue;
                }
                if (!crisActual.isEmpty()) {
                    crisLineas.add(crisActual.toString());
                    crisActual.setLength(0);
                }
                // Enunciados con tokens sin espacios (URLs, formulas largas, etc.).
                for (int crisOff = 0; crisOff < crisPalabra.length();) {
                    int crisCp = crisPalabra.codePointAt(crisOff);
                    String crisLetra = new String(Character.toChars(crisCp));
                    String crisPropuesto = crisActual + crisLetra;
                    if (!crisActual.isEmpty() && anchoTexto(crisPropuesto, crisPuntos, crisNegrita) > crisAncho) {
                        crisLineas.add(crisActual.toString());
                        crisActual.setLength(0);
                    }
                    crisActual.append(crisLetra);
                    crisOff += Character.charCount(crisCp);
                }
            }
            crisLineas.add(crisActual.toString());
        }
        return crisLineas;
    }

    private void qr(String crisToken, float crisX, float crisY, float crisLado) throws Exception {
        Map<EncodeHintType, Object> crisHints = new EnumMap<>(EncodeHintType.class);
        crisHints.put(EncodeHintType.ERROR_CORRECTION, crisToken.matches("E3[0-9A-Z]{12}") ? ErrorCorrectionLevel.Q : ErrorCorrectionLevel.M);
        crisHints.put(EncodeHintType.MARGIN, 4);
        BitMatrix crisMatrix = new QRCodeWriter().encode(crisToken, BarcodeFormat.QR_CODE, 0, 0, crisHints);
        Object crisAntialiasAnterior = crisG.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        try {
            crisG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            crisG.setColor(Color.WHITE);
            crisG.fill(new Rectangle2D.Float(crisX, crisY, crisLado, crisLado));
            crisG.setColor(Color.BLACK);
            float crisUnidadX = crisLado / crisMatrix.getWidth(), crisUnidadY = crisLado / crisMatrix.getHeight();
            for (int crisFila = 0; crisFila < crisMatrix.getHeight(); crisFila++) {
                for (int crisColumna = 0; crisColumna < crisMatrix.getWidth(); crisColumna++) {
                    if (crisMatrix.get(crisColumna, crisFila)) {
                        crisG.fill(new Rectangle2D.Float(crisX + crisColumna * crisUnidadX, crisY + crisFila * crisUnidadY, crisUnidadX, crisUnidadY));
                    }
                }
            }
        } finally {
            crisG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, crisAntialiasAnterior);
        }
    }

    private void cabeceraCuadernillo(RepoImpresos.Ejemplar crisEx, RepoImpresos.Lote crisLote, int crisPagina, float crisBaseY, boolean crisCompacta) throws Exception {
        float crisAlturaBarra = crisCompacta ? 5f : 7f;
        float crisQrLado = crisCompacta ? 22f : crisQrCuadernilloMm;
        crisG.setColor(new Color(24, 79, 68));
        crisG.fill(new Rectangle2D.Float(0, crisBaseY, crisMm, crisAlturaBarra));

        txt(crisCurso.toUpperCase(Locale.ROOT), 12, crisBaseY + (crisCompacta ? 15f : 20f), crisCompacta ? 11.5f : 14f, true);
        txt(crisTitulo, 12, crisBaseY + (crisCompacta ? 22f : 28f), crisCompacta ? 9.2f : 12f, true);
        txt("ESTUDIANTE: " + crisAlumno, 12, crisBaseY + (crisCompacta ? 29f : 39f), crisCompacta ? 7.8f : 10f, true);
        txt("CODIGO: " + (crisEx.codigoUniv() == null ? crisEx.dni() : crisEx.codigoUniv()), 12, crisBaseY + (crisCompacta ? 35f : 46f), crisCompacta ? 7.4f : 9f, false);
        txt("EJEMPLAR: " + crisCod + "  |  PAGINA: " + crisPagina, 12, crisBaseY + (crisCompacta ? 41f : 53f), crisCompacta ? 6.8f : 8f, false);
        qr(crisEx.token(), crisCompacta ? 174f : 166f, crisBaseY + (crisCompacta ? 10f : 12f), crisQrLado);
        float crisLineaY = crisBaseY + (crisCompacta ? 45f : 58f);
        linea(12, crisLineaY, 198, crisLineaY);
        txt("Marque sus respuestas solo en la hoja de respuestas correspondiente.", 12, crisBaseY + (crisCompacta ? 51f : 65f), crisCompacta ? 7.5f : 9f, false);
    }

    private List<BloquePregunta> prepararBloques(RepoImpresos.Ejemplar crisEx) {
        List<BloquePregunta> crisBloques = new ArrayList<>();
        for (RepoImpresos.Pregunta crisQ : crisEx.preguntas()) {
            List<String> crisEnunciado = partir(crisQ.numero() + ". " + crisQ.texto(), crisColQWidth, crisQPt, true);
            List<String> crisAlts = new ArrayList<>();
            for (RepoImpresos.Opcion crisOp : crisQ.opciones()) {
                crisAlts.add(crisOp.letra() + ") " + crisOp.texto());
            }
            List<FilaAlternativas> crisFilas = new ArrayList<>();
            float crisDisponible = crisColOpWidth - 1f;
            // Una fila de cuatro si son respuestas cortas, como 2, 5 o 14.
            if (crisAlts.size() == 4 && crisAlts.stream().allMatch(crisT -> anchoTexto(crisT, crisAPt, false) <= (crisDisponible - 3f) / 4f)) {
                float crisPaso = crisDisponible / 4f;
                crisFilas.add(new FilaAlternativas(crisAlts, new float[]{0, crisPaso, 2 * crisPaso, 3 * crisPaso}));
            } else if (crisAlts.size() == 4 && crisAlts.stream().allMatch(crisT -> anchoTexto(crisT, crisAPt, false) <= (crisDisponible - 2f) / 2f)) {
                float crisPaso = crisDisponible / 2f;
                crisFilas.add(new FilaAlternativas(crisAlts.subList(0, 2), new float[]{0, crisPaso}));
                crisFilas.add(new FilaAlternativas(crisAlts.subList(2, 4), new float[]{0, crisPaso}));
            } else {
                // Alternativas largas: ancho completo, ajustadas a varias lineas.
                for (String crisOp : crisAlts) {
                    for (String crisL : partir(crisOp, crisDisponible, crisAPt, false)) {
                        crisFilas.add(new FilaAlternativas(List.of(crisL), new float[]{0}));
                    }
                }
            }
            crisBloques.add(new BloquePregunta(crisEnunciado, crisFilas));
        }
        return crisBloques;
    }

    private void dibujarBloque(BloquePregunta crisBloque, float crisX, float crisY) {
        float crisYy = crisY;
        for (String crisLinea : crisBloque.enunciado) {
            txt(crisLinea, crisX, crisYy, crisQPt, true);
            crisYy += crisQStep;
        }
        for (FilaAlternativas crisFila : crisBloque.filas) {
            for (int crisJ = 0; crisJ < crisFila.textos.size(); crisJ++) {
                txt(crisFila.textos.get(crisJ), crisX + 2f + crisFila.desplazamientos[crisJ], crisYy, crisAPt, false);
            }
            crisYy += crisAStep;
        }
    }

    /** Elige el mayor numero de preguntas que entra, equilibrando alturas de columnas. */
    private PlanSeccion crearPlan(List<BloquePregunta> crisBloques, int crisDesde, float crisCapacidad) {
        int crisMejorFin = crisDesde, crisMejorDivision = crisDesde;
        float crisMejorDiferencia = Float.POSITIVE_INFINITY;
        float crisSumaIzquierda = 0f;
        for (int crisDivision = crisDesde + 1; crisDivision <= crisBloques.size(); crisDivision++) {
            crisSumaIzquierda += crisBloques.get(crisDivision - 1).alto;
            if (crisSumaIzquierda > crisCapacidad + .01f) break;
            float crisSumaDerecha = 0f;
            int crisFin = crisDivision;
            while (crisFin < crisBloques.size() && crisSumaDerecha + crisBloques.get(crisFin).alto <= crisCapacidad + .01f) {
                crisSumaDerecha += crisBloques.get(crisFin).alto;
                crisFin++;
            }
            float crisDiferencia = Math.abs(crisSumaIzquierda - crisSumaDerecha);
            if (crisFin > crisMejorFin || (crisFin == crisMejorFin && crisDiferencia < crisMejorDiferencia)) {
                crisMejorFin = crisFin;
                crisMejorDivision = crisDivision;
                crisMejorDiferencia = crisDiferencia;
            }
        }
        if (crisMejorFin == crisDesde) {
            throw new IllegalArgumentException("La pregunta " + crisBloques.get(crisDesde).enunciado.get(0)
                    + " es demasiado larga para una columna; acorte el enunciado o sus alternativas.");
        }
        return new PlanSeccion(crisMejorDivision, crisMejorFin);
    }

    private float[] limitesSeccion(boolean crisCompacta, float crisBaseY) {
        return new float[]{crisBaseY + (crisCompacta ? 58f : 75f), crisCompacta ? crisBaseY + crisHalfBottomRel : crisFullBottom};
    }

    private int renderSeccion(List<BloquePregunta> crisBloques, int crisStartIdx,
            RepoImpresos.Ejemplar crisEx, RepoImpresos.Lote crisLote,
            float crisBaseY, boolean crisCompacta, int crisPagina) throws Exception {
        float[] crisLimites = limitesSeccion(crisCompacta, crisBaseY);
        PlanSeccion crisPlan = crearPlan(crisBloques, crisStartIdx, crisLimites[1] - crisLimites[0]);
        cabeceraCuadernillo(crisEx, crisLote, crisPagina, crisBaseY, crisCompacta);
        float crisY = crisLimites[0];
        for (int crisI = crisStartIdx; crisI < crisPlan.division; crisI++) {
            dibujarBloque(crisBloques.get(crisI), crisColX[0], crisY);
            crisY += crisBloques.get(crisI).alto;
        }
        crisY = crisLimites[0];
        for (int crisI = crisPlan.division; crisI < crisPlan.fin; crisI++) {
            dibujarBloque(crisBloques.get(crisI), crisColX[1], crisY);
            crisY += crisBloques.get(crisI).alto;
        }
        return crisPlan.fin;
    }

    private boolean cabeEnMediaHoja(List<BloquePregunta> crisBloques) {
        if (crisBloques.isEmpty()) return false;
        try {
            PlanSeccion crisPlan = crearPlan(crisBloques, 0, crisHalfBottomRel - 58f);
            return crisPlan.fin == crisBloques.size();
        } catch (IllegalArgumentException crisNoCabeEnMediaPagina) {
            return false;
        }
    }

    private void cuadernilloCompleto(RepoImpresos.Lote crisLote, RepoImpresos.Ejemplar crisEx) throws Exception {
        crisAlumno = crisEx.nombre();
        crisCod = crisEx.codigo();
        crisTitulo = crisLote.titulo();
        crisCurso = crisLote.curso();
        List<BloquePregunta> crisBloques = prepararBloques(crisEx);
        int crisPagina = 1;
        int crisIdx = 0;
        while (crisIdx < crisBloques.size()) {
            nuevaPagina();
            int crisNuevoIdx = renderSeccion(crisBloques, crisIdx, crisEx, crisLote, 0f, false, crisPagina);
            if (crisNuevoIdx <= crisIdx) throw new IllegalStateException("La paginacion no avanzo");
            crisIdx = crisNuevoIdx;
            crisG.dispose();
            crisPagina++;
        }
    }

    private void cuadernillosDobles(RepoImpresos.Lote crisLote,
            RepoImpresos.Ejemplar crisArriba, RepoImpresos.Ejemplar crisAbajo) throws Exception {
        nuevaPagina();
        crisAlumno = crisArriba.nombre();
        crisCod = crisArriba.codigo();
        crisTitulo = crisLote.titulo();
        crisCurso = crisLote.curso();
        List<BloquePregunta> crisA = prepararBloques(crisArriba);
        if (renderSeccion(crisA, 0, crisArriba, crisLote, 0f, true, 1) != crisA.size()) {
            throw new IllegalStateException("El cuadernillo superior no cabe completo");
        }
        crisG.setColor(new Color(90, 105, 112));
        crisG.setStroke(new BasicStroke(.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{2, 2}, 0));
        crisG.draw(new Line2D.Float(5, crisHalfSection, 205, crisHalfSection));
        crisAlumno = crisAbajo.nombre();
        crisCod = crisAbajo.codigo();
        List<BloquePregunta> crisB = prepararBloques(crisAbajo);
        if (renderSeccion(crisB, 0, crisAbajo, crisLote, crisHalfSection, true, 1) != crisB.size()) {
            throw new IllegalStateException("El cuadernillo inferior no cabe completo");
        }
        crisG.dispose();
    }

    private void cuadernillos(RepoImpresos.Lote crisLote) throws Exception {
        List<RepoImpresos.Ejemplar> crisList = crisLote.ejemplares();
        int crisI = 0;
        while (crisI < crisList.size()) {
            RepoImpresos.Ejemplar crisActual = crisList.get(crisI);
            List<BloquePregunta> crisBloquesActual = prepararBloques(crisActual);
            if (cabeEnMediaHoja(crisBloquesActual) && crisI + 1 < crisList.size()
                    && cabeEnMediaHoja(prepararBloques(crisList.get(crisI + 1)))) {
                cuadernillosDobles(crisLote, crisActual, crisList.get(crisI + 1));
                crisI += 2;
            } else {
                cuadernilloCompleto(crisLote, crisActual);
                crisI++;
            }
        }
    }

    private void cuadrado(float crisCenterX, float crisCenterY) {
        crisG.setColor(Color.BLACK);
        crisG.fill(new Rectangle2D.Float(crisCenterX - 2.65f, crisCenterY - 2.65f, 5.3f, 5.3f));
    }

    /** Posiciones milimetricas usadas tambien por DetectorOMR para proyectar marcas. */
    public static float bubbleX(int crisPreg, int crisAlt) {
        return (crisPreg < 15 ? 41f : 141f) + 13f * crisAlt;
    }

    public static float bubbleY(int crisPreg) {
        return 63f + (crisPreg % 15) * 5.35f;
    }

    public static float[] marcadoresX = {9, 201, 9, 201};
    public static float[] marcadoresY = {9, 9, 139.5f, 139.5f};

    private void mediaHoja(RepoImpresos.Ejemplar crisEx, RepoImpresos.Lote crisLote, float crisDesplazamiento) throws Exception {
        Graphics2D crisOriginal = crisG;
        crisG = (Graphics2D) crisOriginal.create();
        crisG.translate(0, crisDesplazamiento);
        for (int crisI = 0; crisI < 4; crisI++) cuadrado(marcadoresX[crisI], marcadoresY[crisI]);
        txt("HOJA DE RESPUESTAS", 15, 18.5f, 12, true);
        txt(crisLote.curso() + "  |  " + crisLote.titulo(), 15, 25, 9, true);
        txt("ALUMNO: " + crisEx.nombre(), 15, 31.5f, 9, false);
        txt("CODIGO: " + (crisEx.codigoUniv() == null ? crisEx.dni() : crisEx.codigoUniv()), 15, 37, 10, true);
        txt("EJEMPLAR: " + crisEx.codigo(), 112, 37, 9, true);
        qr(crisEx.token(), 157.5f, 10.5f, crisQrFichaMm);
        linea(13, 52.5f, 197, 52.5f);
        for (int crisCol = 0; crisCol < 2; crisCol++) {
            float crisDx = crisCol == 0 ? 0 : 100;
            txt("No.", 17 + crisDx, 57.5f, 7.5f, true);
            for (int crisA = 0; crisA < 4; crisA++) txt("" + "ABCD".charAt(crisA), bubbleX(crisCol * 15, crisA) - 1.2f, 57.5f, 8, true);
        }
        for (int crisN = 0; crisN < crisEx.preguntas().size(); crisN++) {
            float crisYy = bubbleY(crisN);
            txt(String.format(Locale.ROOT, "%02d", crisN + 1), 17 + (crisN / 15) * 100, crisYy + 1.1f, 8, true);
            for (int crisA = 0; crisA < 4; crisA++) {
                crisG.setColor(Color.BLACK);
                crisG.setStroke(new BasicStroke(.3f));
                crisG.draw(new Ellipse2D.Float(bubbleX(crisN, crisA) - 2.05f, crisYy - 2.05f, 4.1f, 4.1f));
            }
        }
        txt("Rellene UN circulo por pregunta. Evite tachones y doblar la hoja.", 15, 145f, 7, false);
        crisG.dispose();
        crisG = crisOriginal;
    }

    private void fichas(RepoImpresos.Lote crisLote) throws Exception {
        List<RepoImpresos.Ejemplar> crisList = crisLote.ejemplares();
        for (int crisI = 0; crisI < crisList.size(); crisI += 2) {
            nuevaPagina();
            mediaHoja(crisList.get(crisI), crisLote, 0);
            crisG.setColor(new Color(90, 105, 112));
            crisG.setStroke(new BasicStroke(.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{2, 2}, 0));
            crisG.draw(new Line2D.Float(5, 148.5f, 205, 148.5f));
            if (crisI + 1 < crisList.size()) mediaHoja(crisList.get(crisI + 1), crisLote, 148.5f);
            crisG.dispose();
        }
    }

    public static Path generar(RepoImpresos.Lote crisLote) throws Exception {
        if (crisLote.ejemplares().isEmpty()) throw new IllegalArgumentException("Lote sin ejemplares");
        PdfExamenes crisB = new PdfExamenes();
        crisB.cuadernillos(crisLote);
        crisB.fichas(crisLote);
        if (crisB.crisImg != null) {
            crisB.crisPages.add(comprimirLossless(crisB.crisImg));
            crisB.crisImg = null;
        }
        Files.createDirectories(BASE);
        Path crisDst = BASE.resolve("Evaluacion_" + crisLote.evaluacion() + "_Generacion_" + crisLote.numero() + "_Lote_" + crisLote.id() + ".pdf");
        Path crisTemporal = Files.createTempFile(BASE, "creando_", ".pdf");
        try {
            escribirPDF(crisTemporal, crisB.crisPages);
            Files.move(crisTemporal, crisDst, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(crisTemporal);
        }
        return crisDst;
    }

    private static byte[] ascii(String crisS) {
        return crisS.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static void write(ByteArrayOutputStream crisO, String crisS) throws IOException {
        crisO.write(ascii(crisS));
    }

    /** RGB crudo comprimido con FlateDecode: evita artefactos JPEG en los QR. */
    private static byte[] comprimirLossless(BufferedImage crisIm) throws IOException {
        ByteArrayOutputStream crisResult = new ByteArrayOutputStream();
        int crisAncho = crisIm.getWidth();
        int[] crisPixels = new int[crisAncho];
        byte[] crisRgb = new byte[crisAncho * 3];
        try (DeflaterOutputStream crisDeflactado = new DeflaterOutputStream(crisResult)) {
            for (int crisYy = 0; crisYy < crisIm.getHeight(); crisYy++) {
                crisIm.getRGB(0, crisYy, crisAncho, 1, crisPixels, 0, crisAncho);
                for (int crisXx = 0; crisXx < crisAncho; crisXx++) {
                    int crisColor = crisPixels[crisXx], crisPos = crisXx * 3;
                    crisRgb[crisPos] = (byte) (crisColor >>> 16);
                    crisRgb[crisPos + 1] = (byte) (crisColor >>> 8);
                    crisRgb[crisPos + 2] = (byte) crisColor;
                }
                crisDeflactado.write(crisRgb);
            }
        }
        return crisResult.toByteArray();
    }

    private static void escribirPDF(Path crisRuta, List<byte[]> crisImgs) throws IOException {
        int crisTotal = 2 + crisImgs.size() * 3;
        ByteArrayOutputStream crisO = new ByteArrayOutputStream();
        List<Integer> crisOff = new ArrayList<>(crisTotal + 1);
        crisOff.add(0);
        write(crisO, "%PDF-1.4\n");
        crisOff.add(crisO.size());
        write(crisO, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        StringBuilder crisKids = new StringBuilder();
        for (int crisI = 0; crisI < crisImgs.size(); crisI++) crisKids.append(3 + crisI * 3).append(" 0 R ");
        crisOff.add(crisO.size());
        write(crisO, "2 0 obj\n<< /Type /Pages /Kids [" + crisKids + "] /Count " + crisImgs.size() + " >>\nendobj\n");
        for (int crisI = 0; crisI < crisImgs.size(); crisI++) {
            byte[] crisData = crisImgs.get(crisI);
            int crisBase = 3 + 3 * crisI;
            crisOff.add(crisO.size());
            write(crisO, crisBase + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595.276 841.89] /Resources << /XObject << /Im0 " + (crisBase + 2) + " 0 R >> >> /Contents " + (crisBase + 1) + " 0 R >>\nendobj\n");
            byte[] crisStream = ascii("q 595.276 0 0 841.89 0 0 cm /Im0 Do Q\n");
            crisOff.add(crisO.size());
            write(crisO, (crisBase + 1) + " 0 obj\n<< /Length " + crisStream.length + " >>\nstream\n");
            crisO.write(crisStream);
            write(crisO, "endstream\nendobj\n");
            crisOff.add(crisO.size());
            write(crisO, (crisBase + 2) + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + crisPix + " /Height " + Math.round(crisPix * crisAlt / crisMm) + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length " + crisData.length + " >>\nstream\n");
            crisO.write(crisData);
            write(crisO, "\nendstream\nendobj\n");
        }
        int crisXref = crisO.size();
        write(crisO, "xref\n0 " + (crisTotal + 1) + "\n0000000000 65535 f \n");
        for (int crisI = 1; crisI <= crisTotal; crisI++) write(crisO, String.format(Locale.ROOT, "%010d 00000 n \n", crisOff.get(crisI)));
        write(crisO, "trailer\n<< /Size " + (crisTotal + 1) + " /Root 1 0 R >>\nstartxref\n" + crisXref + "\n%%EOF\n");
        Files.write(crisRuta, crisO.toByteArray());
    }
}
