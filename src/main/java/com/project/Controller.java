package com.project;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.RadioButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.CheckBox;
import javafx.geometry.Rectangle2D;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

public class Controller implements Initializable {

    // Color de relleno inicial de los nuevos objetos.
    private static final Color DEFAULT_FILL_COLOR = Color.web("#94D2F5");

    // La lista de objetos (la del ListView)
    @FXML private ListView<DrawableObject> objectList;

    @FXML private Canvas canvas;
    @FXML private Canvas colorsCanvas;

    // Selector de herramienta
    @FXML private RadioButton freeStrokeRadio;
    @FXML private RadioButton lineRadio;
    @FXML private RadioButton rectRadio;

    @FXML private ToggleGroup toolGroup;

    // Formulario de estilo
    @FXML private ColorPicker lineColorPicker;
    @FXML private ColorPicker fillColorPicker;
    @FXML private CheckBox noFillCheck;
    @FXML private Slider widthSlider;
    @FXML private Label widthLabel;

    // Botón para eliminar el objeto seleccionado
    @FXML private Button deleteButton;

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

    // Modo mover: true mientras se está arrastrando el objeto seleccionado
    private boolean moving = false;

    // Última posición del ratón aplicada al movimiento.
    private double lastX = 0;
    private double lastY = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gc = canvas.getGraphicsContext2D();
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        // The predefined color palette: it handles its own drawing and clicks.
        // When a preset is clicked it updates the line ColorPicker.
        colorPalette = new ColorPalette(colorsCanvas, this::onPresetColorSelected);

        // The ColorPickers are the source of truth for the line and fill colors.
        // The line color starts with the color marked in the predefined palette.
        lineColorPicker.setValue(colorPalette.getSelectedColor());
        fillColorPicker.setValue(DEFAULT_FILL_COLOR);

        // When the line color changes, the palette marks the matching preset (if any)
        lineColorPicker.valueProperty().addListener((obs, oldValue, newValue) ->
                colorPalette.setSelectedColor(newValue));

        // "No fill" disables the fill picker: the rectangles are drawn without filling
        noFillCheck.selectedProperty().addListener((obs, oldValue, newValue) ->
                fillColorPicker.setDisable(newValue));

        // The label shows the current width (linked to the slider)
        widthLabel.textProperty().bind(widthSlider.valueProperty().asString("%.0f"));

        // Drawing canvas events
        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);
        canvas.setOnMouseReleased(this::onMouseReleased);

        // El botón solo se activa si hay un objeto seleccionado.
        // El enlace lo mantiene actualizado solo.
        deleteButton.disableProperty().bind(objectList.getSelectionModel().selectedItemProperty().isNull());

        // Solo se puede seleccionar un objeto a la vez
        objectList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Al seleccionar una entrada guardamos el objeto y redibujamos el lienzo.
        objectList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedObject = newValue;
            if (newValue != null) {
                // Hay un objeto seleccionado -> desmarcamos las herramientas de dibujo
                clearToolSelection();
            }
            redraw();
        });

        // Escuchar las teclas
        Platform.runLater(() -> {
            canvas.getScene().addEventFilter(KeyEvent.ANY, event -> {
                if (event.getCode() == KeyCode.DELETE) {
                    onDeleteButton(null);
                }
            });
        });

        // Cuando el usuario elige una herramienta de dibujo, se desmarca el objeto
        // seleccionado y se vuelve al modo de dibujo
        toolGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                objectList.getSelectionModel().clearSelection();
            }
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

    /**
     * Color de la línea o del contorno de los nuevos objetos.
     * Lo elige el usuario en el selector de color de JavaFX.
     */
    private Color getLineColor() {
        return lineColorPicker.getValue();
    }

    /**
     * Color de relleno de los nuevos objetos.
     * Es null si está marcado el "No fill": el rectángulo se dibuja solo con el contorno.
     */
    private Color getFillColor() {
        if (noFillCheck.isSelected()) {
            return null;
        }
        return fillColorPicker.getValue();
    }

    /**
     * Se llama cuando el usuario elige un color de la paleta de colores predefinidos.
     * El color elegido se aplica al color de línea del formulario.
     */
    private void onPresetColorSelected(Color color) {
        lineColorPicker.setValue(color);
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

        double x = event.getX();
        double y = event.getY();

        // Empezamos el arrastre si el ratón ha pulsado dentro del requadro del objeto seleccionado
        moving = false;
        if (selectedObject != null && isInsideSelection(x, y)) {
            moving = true;
            lastX = x;
            lastY = y;
            return;
        }

        // Puede ser null si no hay ninguna herramienta activa
        DrawableObject.Type tool = getSelectedTool();
        if (tool == null) {
            return;
        }

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

        // Modo mover: trasladamos el objeto seleccionado
        if (moving) {
            // Desplazamiento incremental: lo que el ratón ha avanzado desde el último evento.
            selectedObject.move(x - lastX, y - lastY);
            lastX = x;
            lastY = y;

            redraw();
            return;
        }

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

        // El objeto conserva su nueva posición y sigue seleccionado
        if (moving) {
            // Aplicamos el último trocito
            selectedObject.move(x - lastX, y - lastY);
            lastX = x;
            lastY = y;

            moving = false;
            redraw();
            return;
        }

        if (currentStroke != null) {
            // El trazo está terminado: se guarda la posición final y pasa a la lista
            currentStroke.addPoint(x, y);
            objectList.getItems().add(currentStroke);
            currentStroke = null;
        } else if (currentLine != null) {
            // La línea está terminada: se guarda la posición final y pasa a la lista
            currentLine.setEnd(x, y);
            objectList.getItems().add(currentLine);
            currentLine = null;
        } else if (currentRect != null) {
            // El rectángulo está terminado: se ajusta al punto final y pasa a la lista
            updateRectangle(x, y);
            // Solo se guarda si el rectángulo tiene algo de tamaño
            if (currentRect.getW() > 0 || currentRect.getH() > 0) {
                objectList.getItems().add(currentRect);
            }
            currentRect = null;
        }

        redraw();
    }

    /**
     * Elimina el objeto seleccionado: desaparece del dibujo y de la lista.
     */
    @FXML
    private void onDeleteButton(ActionEvent event) {
        DrawableObject obj = selectedObject;

        // Si no hay nada seleccionado, no hay nada que eliminar
        if (obj == null) {
            return;
        }

        // Quitar el objeto de la lista: como la lista es el modelo,
        // también desaparece del dibujo al redibujar
        objectList.getItems().remove(obj);

        // Limpiar la selección: el requadro desaparece y el botón se desactiva
        objectList.getSelectionModel().clearSelection();
        selectedObject = null;

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

        // Requadro del objeto seleccionado: se dibuja al final, con su propio estilo.
        // Es un indicador de selección, no un objeto del dibujo.
        if (selectedObject != null) {
            drawSelectionBox(selectedObject);
        }
    }

    private void updateRectangle(double x, double y) {
        double rectX = Math.min(startX, x);
        double rectY = Math.min(startY, y);
        double rectW = Math.abs(x - startX);
        double rectH = Math.abs(y - startY);

        currentRect.setBounds(rectX, rectY, rectW, rectH);
    }

    /**
     * Rectángulo que envuelve al objeto seleccionado, con un margen.
     */
    private Rectangle2D getSelectionBounds(DrawableObject obj) {
        Rectangle2D bounds = obj.getBounds();
        double margin = obj.getLineWidth() / 2 + 4;

        return new Rectangle2D(
                bounds.getMinX() - margin,
                bounds.getMinY() - margin,
                bounds.getWidth() + margin * 2,
                bounds.getHeight() + margin * 2);
    }

    /**
     * Dibuja el requadro discontinuo que marca el objeto seleccionado.
     */
    private void drawSelectionBox(DrawableObject obj) {
        Rectangle2D box = getSelectionBounds(obj);

        // Estilo propio del requadro
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);

        // Trazo discontinuo: 5 píxeles de raya, 5 de hueco
        gc.setLineDashes(5, 5);

        gc.strokeRect(box.getMinX(), box.getMinY(), box.getWidth(), box.getHeight());

        // Restablecemos el discontinuo: si no, los próximos trazos saldrían también discontinuos
        gc.setLineDashes(null);
    }

    /**
     * Desmarca las herramientas de dibujo.
     */
    private void clearToolSelection() {
        freeStrokeRadio.setSelected(false);
        lineRadio.setSelected(false);
        rectRadio.setSelected(false);
    }

    /**
     * True si el punto (x, y) está dentro del requadro del objeto seleccionado.
     */
    private boolean isInsideSelection(double x, double y) {
        if (selectedObject == null) {
            return false;
        }

        Rectangle2D box = getSelectionBounds(selectedObject);
        return box.contains(x, y);
    }

}
