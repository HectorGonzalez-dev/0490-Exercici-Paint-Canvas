package com.project;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Línea recta definida por dos extremos: (x1, y1) y (x2, y2).
 */
public class LineObject extends DrawableObject {

    private double x1;
    private double y1;
    private double x2;
    private double y2;

    public LineObject(double x1, double y1, double x2, double y2, Color lineColor, double lineWidth) {
        // La línea no tiene color de relleno: se pasa null
        super(Type.LINE, lineColor, lineWidth, null);
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getX2() { return x2; }
    public double getY2() { return y2; }

    @Override
    public void draw(GraphicsContext gc) {
        applyLineStyle(gc);
        gc.strokeLine(x1, y1, x2, y2);
    }

    /**
     * Rectángulo que envuelve la línea: mínimo y máximo de las dos coordenadas
     * X y de las dos coordenadas Y.
     */
    @Override
    public Rectangle2D getBounds() {
        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double minY = Math.min(y1, y2);
        double maxY = Math.max(y1, y2);

        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }

    /** Mueve la línea: se desplazan los dos extremos. */
    @Override
    public void move(double dx, double dy) {
        x1 += dx;
        y1 += dy;
        x2 += dx;
        y2 += dy;
    }
}
