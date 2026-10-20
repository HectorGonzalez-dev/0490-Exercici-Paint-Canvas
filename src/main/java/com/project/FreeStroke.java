package com.project;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Trazo libre: una lista ordenada de puntos (x, y).
 */
public class FreeStroke extends DrawableObject {

    // Lista de puntos del trazo, en el orden en que se han ido dibujando
    private final List<Point2D> points = new ArrayList<>();

    public FreeStroke(Color lineColor, double lineWidth) {
        // El trazo libre no tiene color de relleno: se pasa null
        super(Type.FREESTROKE, lineColor, lineWidth, null);
    }

    /** Añade un punto nuevo al final del trazo. */
    public void addPoint(double x, double y) {
        points.add(new Point2D(x, y));
    }

    public List<Point2D> getPoints() {
        return points;
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (points.isEmpty()) {
            return;
        }

        // Color y ancho propios del objeto
        applyLineStyle(gc);

        // Unir cada punto con el siguiente
        gc.beginPath();
        gc.moveTo(points.get(0).getX(), points.get(0).getY());
        for (int i = 1; i < points.size(); i++) {
            Point2D p = points.get(i);
            gc.lineTo(p.getX(), p.getY());
        }
        gc.stroke();
    }

    /**
     * Rectángulo que envuelve el trazo: se calculan los valores mínimo y máximo
     * de X y de Y entre todos los puntos.
     */
    @Override
    public Rectangle2D getBounds() {
        if (points.isEmpty()) {
            return new Rectangle2D(0, 0, 0, 0);
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (Point2D p : points) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }

        // Rectangle2D = (x, y, ancho, alto)
        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Mueve todos los puntos del trazo.
     */
    @Override
    public void move(double dx, double dy) {
        for (int i = 0; i < points.size(); i++) {
            points.set(i, points.get(i).add(dx, dy));
        }
    }
}
