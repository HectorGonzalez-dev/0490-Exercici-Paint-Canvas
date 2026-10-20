package com.project;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class ColorPalette {

    // Rejilla de colores: COLORS[fila][columna]
    private static final Color[][] COLORS = {
        // Fila 0: tonos oscuros
        {
            Color.web("#7F1D1D"), Color.web("#7C2D12"), Color.web("#713F12"), Color.web("#14532D"),
            Color.web("#164E63"), Color.web("#1E3A8A"), Color.web("#4C1D95"), Color.web("#701A75")
        },
        // Fila 1: tonos medios
        {
            Color.web("#DC2626"), Color.web("#EA580C"), Color.web("#CA8A04"), Color.web("#16A34A"),
            Color.web("#0891B2"), Color.web("#2563EB"), Color.web("#7C3AED"), Color.web("#C026D3")
        },
        // Fila 2: tonos claros
        {
            Color.web("#F87171"), Color.web("#FB923C"), Color.web("#FACC15"), Color.web("#4ADE80"),
            Color.web("#22D3EE"), Color.web("#60A5FA"), Color.web("#A78BFA"), Color.web("#E879F9")
        },
        // Fila 3: tonos muy claros
        {
            Color.web("#FECACA"), Color.web("#FED7AA"), Color.web("#FEF08A"), Color.web("#BBF7D0"),
            Color.web("#A5F3FC"), Color.web("#BFDBFE"), Color.web("#DDD6FE"), Color.web("#F5D0FE")
        }
    };

    // Separación entre el centro de dos colores contiguos
    private static final double SPACING = 28;

    // Radio de cada círculo
    private static final double RADIUS = 10;

    // Cuánto sube el color seleccionado respecto al resto de su fila
    private static final double ELEVATION = 8;

    // Y del centro de la primera fila (deja sitio para que el color elevado no se corte)
    private static final double TOP = 26;

    private final Canvas canvas;
    private final GraphicsContext gc;

    // Si es true, la paleta añade el swatch "sin color" (a la derecha de la rejilla)
    private final boolean noneOption;

    // Color marcado en la paleta (null = "sin color")
    private Color selectedColor;

    public ColorPalette(Canvas canvas) {
        this(canvas, false);
    }

    public ColorPalette(Canvas canvas, boolean noneOption) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.noneOption = noneOption;

        // Por defecto: un color de la rejilla. En la paleta de relleno, "sin color".
        this.selectedColor = noneOption ? null : COLORS[1][0];

        canvas.setOnMouseClicked(this::onMouseClicked);
        draw();
    }

    /** Color elegido en la paleta (null si está en "sin color"). */
    public Color getSelectedColor() {
        return selectedColor;
    }

    private int getRows() {
        return COLORS.length;
    }

    private int getColumns() {
        return COLORS[0].length;
    }

    /** Centro X del color de la columna col */
    private double getCenterX(int col) {
        return SPACING / 2 + col * SPACING;
    }

    /** Centro Y del color de la fila row, teniendo en cuenta si está elevado */
    private double getCenterY(int row, boolean elevated) {
        return TOP + row * SPACING - (elevated ? ELEVATION : 0);
    }

    /** True si ese color es el seleccionado */
    private boolean isSelected(Color color) {
        return selectedColor == null ? color == null : selectedColor.equals(color);
    }

    /** True si el swatch "sin color" está seleccionado */
    private boolean isNoneSelected() {
        return selectedColor == null;
    }

    private void onMouseClicked(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        // El swatch "sin color" (solo en la paleta de relleno) está a la derecha de la rejilla
        if (noneOption) {
            double dx = x - getCenterX(getColumns());
            double dy = y - getCenterY(0, isNoneSelected());

            if (Math.sqrt(dx * dx + dy * dy) <= RADIUS) {
                selectedColor = null;
                draw();
                return;
            }
        }

        // Buscamos el círculo pulsado
        for (int row = 0; row < getRows(); row++) {
            for (int col = 0; col < getColumns(); col++) {
                Color color = COLORS[row][col];

                double dx = x - getCenterX(col);
                double dy = y - getCenterY(row, isSelected(color));

                // Dentro del círculo si la distancia al centro es menor que el radio
                if (Math.sqrt(dx * dx + dy * dy) <= RADIUS) {
                    selectedColor = color;
                    draw();
                    return;
                }
            }
        }
    }

    private void draw() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for (int row = 0; row < getRows(); row++) {
            for (int col = 0; col < getColumns(); col++) {
                Color color = COLORS[row][col];
                boolean selected = isSelected(color);

                double cx = getCenterX(col);
                // El color seleccionado se dibuja más ARRIBA que el resto de su fila
                double cy = getCenterY(row, selected);

                gc.setFill(color);
                gc.fillOval(cx - RADIUS, cy - RADIUS, RADIUS * 2, RADIUS * 2);

                // Marca extra: un anillo fino.
                if (selected) {
                    gc.setStroke(Color.BLACK);
                    gc.setLineWidth(1.5);
                    gc.strokeOval(cx - RADIUS - 1, cy - RADIUS - 1, (RADIUS + 1) * 2, (RADIUS + 1) * 2);
                }
            }
        }

        if (noneOption) {
            drawNoneSwatch();
        }
    }

    /**
     * Swatch "sin color": un círculo blanco con una diagonal roja.
     * Es el símbolo universal de "sin relleno".
     */
    private void drawNoneSwatch() {
        double cx = getCenterX(getColumns());
        double cy = getCenterY(0, isNoneSelected());

        // Círculo blanco
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - RADIUS, cy - RADIUS, RADIUS * 2, RADIUS * 2);

        // Contorno gris
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1);
        gc.strokeOval(cx - RADIUS, cy - RADIUS, RADIUS * 2, RADIUS * 2);

        // Diagonal roja
        gc.setStroke(Color.web("#DC2626"));
        gc.setLineWidth(2);
        gc.strokeLine(cx - RADIUS * 0.7, cy + RADIUS * 0.7, cx + RADIUS * 0.7, cy - RADIUS * 0.7);

        // Anillo negro del color seleccionado
        if (isNoneSelected()) {
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.5);
            gc.strokeOval(cx - RADIUS - 1, cy - RADIUS - 1, (RADIUS + 1) * 2, (RADIUS + 1) * 2);
        }
    }
}
