package com.project;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Rectángulo definido por su esquina superior izquierda y su tamaño.
 */
public class RectangleObject extends DrawableObject {

    private double x;
    private double y;
    private double width;
    private double height;

    public RectangleObject(double x, double y, double width, double height, Color lineColor, double lineWidth, Color fillColor) {
        super(Type.RECTANGLE, lineColor, lineWidth, fillColor);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getW() { return width; }
    public double getH() { return height; }

    /**
     * Cambia la posición y el tamaño del rectángulo.
     */
    public void setBounds(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }


    @Override
    public void draw(GraphicsContext gc) {
        // Primero el relleno (si lo hay)
        if (getFillColor() != null) {
            gc.setFill(getFillColor());
            gc.fillRect(x, y, width, height);
        }

        // Después el contorno, con el color y el ancho de línea del objeto
        applyLineStyle(gc);
        gc.strokeRect(x, y, width, height);
    }

    @Override
    public Rectangle2D getBounds() {
        return new Rectangle2D(x, y, width, height);
    }

    /** Mueve el rectángulo. */
    @Override
    public void move(double dx, double dy) {
        x += dx;
        y += dy;
    }
}
