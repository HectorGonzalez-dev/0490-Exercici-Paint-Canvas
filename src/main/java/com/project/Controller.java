package com.project;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.RadioButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;

public class Controller implements Initializable {

    @FXML private Canvas canvas;
    @FXML private Canvas colorsCanvas;

    // Selector de herramienta
    @FXML private RadioButton freeStrokeRadio;
    @FXML private RadioButton lineRadio;
    @FXML private RadioButton rectRadio;

    // Formulario de estilo
    @FXML private ColorPicker lineColorPicker;
    @FXML private ColorPicker fillColorPicker;
    @FXML private Slider widthSlider;
    @FXML private Label widthLabel;

    private GraphicsContext gc;
    private ColorPalette colorPalette;

    // Drawing state
    private final List<DrawableObject> objects = new ArrayList<>();
    private FreeStroke currentStroke = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gc = canvas.getGraphicsContext2D();
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        // The palette handles its own drawing and clicks
        colorPalette = new ColorPalette(colorsCanvas);

        // The label shows the current width (linked to the slider)
        widthLabel.textProperty().bind(widthSlider.valueProperty().asString("%.0f"));

        // The line color starts with the color selected in the palette
        lineColorPicker.setValue(colorPalette.getSelectedColor());

        // The fill color starts as a light gray
        fillColorPicker.setValue(Color.color(0.85, 0.85, 0.85));

        // Clicking a predefined color changes the line color of the ColorPicker
        colorPalette.setOnColorSelected(c -> lineColorPicker.setValue(c));


        // Drawing canvas events
        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);
        canvas.setOnMouseReleased(this::onMouseReleased);

        redraw();
    }

    /**
     * Devuelve el tipo de herramienta seleccionado en el formulario.
     * Null significa que no hay ninguna herramienta activa.
     */
    private DrawableObject.Type getSelectedTool() {
        if (freeStrokeRadio.isSelected()) {
            return DrawableObject.Type.FREESTROKE;
        }
        if (lineRadio.isSelected()) {
            return DrawableObject.Type.LINE;
        }
        if (rectRadio.isSelected()) {
            return DrawableObject.Type.RECTANGLE;
        }
        return null;
    }

    /** Color de la línea o del contorno de los nuevos objetos. */
    private Color getLineColor() {
        return lineColorPicker.getValue();
    }

    /**
     * Color de relleno de los nuevos objetos.
     * Puede ser null si se elige el botón "sin color" del ColorPicker.
     */
    private Color getFillColor() {
        return fillColorPicker.getValue();
    }

    /** Ancho de la línea de los nuevos objetos. */
    private double getLineWidth() {
        return widthSlider.getValue();
    }

    // Drawing canvas

    private void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;

        // Start a new stroke with the current style
        currentStroke = new FreeStroke(getLineColor(), getLineWidth());
        currentStroke.addPoint(event.getX(), event.getY());
        redraw();
    }

    private void onMouseDragged(MouseEvent event) {
        if (currentStroke == null) return;

        currentStroke.addPoint(event.getX(), event.getY());
        redraw();
    }

    private void onMouseReleased(MouseEvent event) {
        if (currentStroke == null) return;

        // The stroke is finished: keep it in memory
        currentStroke.addPoint(event.getX(), event.getY());
        objects.add(currentStroke);
        currentStroke = null;
        redraw();
    }

    private void redraw() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Objetos guardados en orden de creación
        for (DrawableObject obj : objects) {
            obj.draw(gc);
        }

        // Stroke being drawn right now (preview)
        if (currentStroke != null) {
            currentStroke.draw(gc);
        }
    }
}
