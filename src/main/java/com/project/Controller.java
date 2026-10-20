package com.project;

import java.net.URL;
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
import javafx.scene.control.Slider;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;


public class Controller implements Initializable {

    // La lista de objetos (la del ListView)
    @FXML private ListView<DrawableObject> objectList;

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

    // El objeto que se está dibujando ahora mismo.
    // Solo uno de los tres puede estar activo a la vez.
    private DrawableObject selectedObject = null;
    private FreeStroke currentStroke = null;
    private LineObject currentLine = null;
    private RectangleObject currentRect = null;

    // Punto inicial del arrastre
    private double startX = 0;
    private double startY = 0;

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

        // Solo se puede seleccionar un objeto a la vez
        objectList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Al seleccionar una entrada guardamos el objeto y redibujamos el lienzo.
        objectList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedObject = newValue;
            redraw();
        });

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
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        // Puede ser null si no hay ninguna herramienta activa
        DrawableObject.Type tool = getSelectedTool();
        if (tool == null) {
            return;
        }

        double x = event.getX();
        double y = event.getY();

        switch (tool) {
            case FREESTROKE:
                // Empieza un trazo nuevo con el estilo actual
                currentStroke = new FreeStroke(getLineColor(), getLineWidth());
                currentStroke.addPoint(x, y);
                break;

            case LINE:
                // La línea nace con los dos extremos en el mismo punto
                // El extremo final se actualizará mientras se arrastra
                startX = x;
                startY = y;
                currentLine = new LineObject(x, y, x, y, getLineColor(), getLineWidth());
                break;

            case RECTANGLE:
                // El rectángulo nace con tamaño 0 y se ajustará al arrastrar
                startX = x;
                startY = y;
                currentRect = new RectangleObject(x, y, 0, 0, getLineColor(), getLineWidth(), getFillColor());
                break;
        }

        redraw();
    }

    private void onMouseDragged(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        if (currentStroke != null) {
            // Trazo libre: se añade el punto y se sigue
            currentStroke.addPoint(x, y);
        } else if (currentLine != null) {
            // Línea: se actualiza el extremo final
            currentLine.setEnd(x, y);
        } else if (currentRect != null) {
            // Rectángulo: se recalcula la caja con el punto inicial y el actual
            updateRectangle(x, y);
        }

        redraw();
    }

    private void onMouseReleased(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();

        if (currentStroke != null) {
            // El trazo está terminado: se guarda la posición final y pasa a la memoria
            currentStroke.addPoint(x, y);
            objectList.getItems().add(currentStroke);
            currentStroke = null;
        } else if (currentLine != null) {
            // La línea está terminada: se guarda la posición final y pasa a la memoria
            currentLine.setEnd(x, y);
            objectList.getItems().add(currentLine);
            currentLine = null;
        } else if (currentRect != null) {
            // El rectángulo está terminado: se ajusta al punto final y pasa a la memoria
            updateRectangle(x, y);
            // Solo se guarda si el rectángulo tiene algo de tamaño
            if (currentRect.getW() > 0 || currentRect.getH() > 0) {
                objectList.getItems().add(currentRect);
            }
            currentRect = null;
        }

        redraw();
    }

    private void redraw() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Objetos guardados en orden de creación
        for (DrawableObject obj : objectList.getItems()) {
            obj.draw(gc);
        }

        // Se dibujan encima de los objetos existentes,
        // pero todavía no forman parte de la lista
        if (currentStroke != null) {
            currentStroke.draw(gc);
        }
        if (currentLine != null) {
            currentLine.draw(gc);
        }
        if (currentRect != null) {
            currentRect.draw(gc);
        }
    }

    private void updateRectangle(double x, double y) {
        double rectX = Math.min(startX, x);
        double rectY = Math.min(startY, y);
        double rectW = Math.abs(x - startX);
        double rectH = Math.abs(y - startY);

        currentRect.setBounds(rectX, rectY, rectW, rectH);
    }
}
