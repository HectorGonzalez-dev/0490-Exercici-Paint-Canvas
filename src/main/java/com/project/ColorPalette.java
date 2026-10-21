package com.project;

import java.util.function.Consumer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class ColorPalette {

    private static final Color[][] COLORS = {
        {
            Color.web("#000000"),
            Color.web("#7A1D0E"),
            Color.web("#844E19"),
            Color.web("#94902C"),
            Color.web("#588525"),
            Color.web("#3B8624"),
            Color.web("#39824F"),
            Color.web("#3B868B"),
            Color.web("#1F4D8D"),
            Color.web("#05178A"),
            Color.web("#491D92"),
            Color.web("#86268F"),
            Color.web("#81214D")
        },
        {
            Color.web("#595959"),
            Color.web("#973D2F"),
            Color.web("#976732"),
            Color.web("#9C993E"),
            Color.web("#6D9639"),
            Color.web("#489433"),
            Color.web("#499464"),
            Color.web("#4B9699"),
            Color.web("#36649A"),
            Color.web("#26369D"),
            Color.web("#61379C"),
            Color.web("#943E99"),
            Color.web("#90355E")
        },
        {
            Color.web("#A6A6A6"),
            Color.web("#E5563A"),
            Color.web("#ECA146"),
            Color.web("#F9F75F"),
            Color.web("#ABF256"),
            Color.web("#70EA4F"),
            Color.web("#73EEA3"),
            Color.web("#77F2F6"),
            Color.web("#509DF1"),
            Color.web("#304CF1"),
            Color.web("#9753EF"),
            Color.web("#E665F0"),
            Color.web("#E35398")
        },
        {
            Color.web("#FFFFFF"),
            Color.web("#EB9088"),
            Color.web("#F5D68F"),
            Color.web("#FAF995"),
            Color.web("#DAF791"),
            Color.web("#95F08B"),
            Color.web("#99F4D5"),
            Color.web("#99F4F7"),
            Color.web("#94D2F5"),
            Color.web("#858BF2"),
            Color.web("#CA8FF2"),
            Color.web("#EA95F3"),
            Color.web("#E995D3")
        }
    };

    // Separación horizontal entre el centro de dos lápices contiguos
    private static final double SPACING_X = 19;

    // Separación vertical entre el centro de dos tonos de la misma familia
    private static final double SPACING_Y = 26;

    // Anchura del cuerpo del lápiz
    private static final double PENCIL_W = 15;

    // Altura de la punta (triángulo) del lápiz
    private static final double TIP_H = 7;

    // Altura del cuerpo del lápiz
    private static final double BODY_H = 11;

    // Cuánto sube el lápiz con el color seleccionado respecto al resto de su fila
    private static final double ELEVATION = 6;

    // Y de la punta del primer lápiz (deja sitio para que el lápiz elevado no se corte)
    private static final double TOP = 12;

    private final Canvas canvas;
    private final GraphicsContext gc;

    // Se llama cuando el usuario elige un color de la rejilla.
    // Sirve para sincronizar el color con otro control.
    private final Consumer<Color> onColorSelected;

    // Color marcado en la paleta (null = ningún color marcado)
    private Color selectedColor;

    public ColorPalette(Canvas canvas, Consumer<Color> onColorSelected) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.onColorSelected = onColorSelected;

        // Por defecto: el rojo en su tono medio.
        this.selectedColor = COLORS[2][1];

        canvas.setOnMouseClicked(this::onMouseClicked);
        draw();
    }

    /** Color marcado en la paleta. */
    public Color getSelectedColor() {
        return selectedColor;
    }

    /**
     * Marca el color indicado como seleccionado y redibuja la paleta.
     * Si el color no está en la rejilla, no queda ningún color marcado.
     */
    public void setSelectedColor(Color color) {
        if (color == null ? selectedColor == null : color.equals(selectedColor)) {
            return;
        }

        this.selectedColor = color;
        draw();
    }

    private int getRows() {
        return COLORS.length;
    }

    private int getColumns() {
        return COLORS[0].length;
    }

    /** Centro X del lápiz de la columna col */
    private double getCenterX(int col) {
        return SPACING_X / 2 + col * SPACING_X;
    }

    /** Y de la punta del lápiz de la fila row */
    private double getTopY(int row) {
        return TOP + row * SPACING_Y;
    }

    /** True si ese color es el seleccionado */
    private boolean isSelected(Color color) {
        return selectedColor == null ? color == null : selectedColor.equals(color);
    }

    /**
     * True si el punto (x, y) cae dentro del lápiz de esa fila y columna.
     * El lápiz seleccionado está desplazado hacia arriba, y su zona también.
     */
    private boolean isInside(int row, int col, double x, double y) {
        double cx = getCenterX(col);
        double half = PENCIL_W / 2;
        double top = getTopY(row) - (isSelected(COLORS[row][col]) ? ELEVATION : 0);

        if (x < cx - half || x > cx + half) {
            return false;
        }
        return y >= top && y <= top + TIP_H + BODY_H;
    }

    private void onMouseClicked(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        // Buscamos el lápiz pulsado
        for (int row = 0; row < getRows(); row++) {
            for (int col = 0; col < getColumns(); col++) {
                if (isInside(row, col, x, y)) {
                    selectedColor = COLORS[row][col];
                    draw();

                    // Avisamos al resto de la aplicación del color elegido
                    if (onColorSelected != null) {
                        onColorSelected.accept(selectedColor);
                    }
                    return;
                }
            }
        }
    }

    private void draw() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int row = 0; row < getRows(); row++) {
            for (int col = 0; col < getColumns(); col++) {
                drawPencil(row, col, COLORS[row][col], isSelected(COLORS[row][col]));
            }
        }
    }

    /**
     * Dibuja un lápiz: la punta (triángulo) y el cuerpo (rectángulo),
     * con un degradado horizontal que simula el cilindro del lápiz.
     */
    private void drawPencil(int row, int col, Color color, boolean selected) {
        double cx = getCenterX(col);
        double half = PENCIL_W / 2;

        // El lápiz con el color seleccionado se dibuja más ARRIBA que el resto de su fila
        double top = getTopY(row) - (selected ? ELEVATION : 0);

        // Cuerpo del lápiz: degradado horizontal
        gc.setFill(new LinearGradient(
                cx - half, top, cx + half, top,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0.00, darken(color, 0.65)),
                new Stop(0.35, lighten(color, 1.15)),
                new Stop(0.70, color),
                new Stop(1.00, darken(color, 0.70))));
        gc.fillRect(cx - half, top + TIP_H, PENCIL_W, BODY_H);

        // Punta del lápiz: triángulo del color seleccionado
        gc.setFill(color);
        gc.fillPolygon(
                new double[] { cx, cx - half, cx + half },
                new double[] { top, top + TIP_H, top + TIP_H },
                3);

        // Contorno del lápiz: separa los lápices contiguos
        gc.setStroke(darken(color, 0.55));
        gc.setLineWidth(1);
        gc.strokeRect(cx - half, top + TIP_H, PENCIL_W, BODY_H);
        gc.beginPath();
        gc.moveTo(cx - half, top + TIP_H);
        gc.lineTo(cx, top);
        gc.lineTo(cx + half, top + TIP_H);
        gc.stroke();

        // Marca del color seleccionado: un contorno negro alrededor del lápiz entero
        if (selected) {
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.5);
            gc.beginPath();
            gc.moveTo(cx - half, top + TIP_H);
            gc.lineTo(cx, top);
            gc.lineTo(cx + half, top + TIP_H);
            gc.lineTo(cx + half, top + TIP_H + BODY_H);
            gc.lineTo(cx - half, top + TIP_H + BODY_H);
            gc.closePath();
            gc.stroke();
        }
    }

    /** Color oscurecido: el factor va de 0 (negro) a 1 (sin cambios). */
    private static Color darken(Color color, double factor) {
        return Color.color(
                color.getRed() * factor,
                color.getGreen() * factor,
                color.getBlue() * factor,
                color.getOpacity());
    }

    /** Color aclarado: el factor va de 1 (sin cambios) a 2 (blanco). */
    private static Color lighten(Color color, double factor) {
        return Color.color(
                Math.min(1.0, color.getRed() * factor),
                Math.min(1.0, color.getGreen() * factor),
                Math.min(1.0, color.getBlue() * factor),
                color.getOpacity());
    }
}
