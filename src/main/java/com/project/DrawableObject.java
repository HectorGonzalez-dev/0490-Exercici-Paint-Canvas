package com.project;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public abstract class DrawableObject {

    /** 
     * Tipos de figura que pueden existir en el lienzo.
     */
    public enum Type {
        FREESTROKE("Freestroke"),
        LINE("Line"),
        RECTANGLE("Rectangle");

        private final String label;

        Type(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    // Contador COMPARTIDO por todos los objetos.
    // Así cada objeto recibe un número distinto.
    private static int nextId = 1;

    private final int id;
    private final Type type;
    private final Color lineColor;
    private final double lineWidth;
    private final Color fillColor;

    public DrawableObject(Type type, Color lineColor, double lineWidth, Color fillColor) {
        this.id = nextId++;
        this.type = type;
        this.lineColor = lineColor;
        this.lineWidth = lineWidth;
        this.fillColor = fillColor;
    }

    public int getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public double getLineWidth() {
        return lineWidth;
    }

    public Color getFillColor() {
        return fillColor;
    }

    /**
     * Dibuja el objeto sobre el contexto gráfico.
     */
    public abstract void draw(GraphicsContext gc);

    /**
     * Rectángulo que envuelve el objeto (coordenadas del lienzo).
     */
    public abstract Rectangle2D getBounds();

    /**
     * Desplaza el objeto dx píxeles a la derecha y dy píxeles hacia abajo.
     */
    public abstract void move(double dx, double dy);

    /**
     * Aplica al contexto el estilo de línea del objeto (color y ancho).
     */
    public void applyLineStyle(GraphicsContext gc) {
        gc.setStroke(lineColor);
        gc.setLineWidth(lineWidth);
    }

    /**
     * Texto que aparece en la lista de objetos, por ejemplo "Línia 2".
     */
    @Override
    public String toString() {
        return type.getLabel() + " " + id;
    }
}
