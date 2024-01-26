package com.sun.internals.controls;

import com.sun.internals.enums.Fit;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import xss.it.ultimate.pdf.viewer.Assets;
import xss.it.ultimate.pdf.viewer.PdfViewer;

import java.util.function.UnaryOperator;

import static xss.it.ultimate.pdf.viewer.Assets.*;

/**
 * @author XDSSWAR
 * Created on 01/25/2024
 */
public final class ZoomControl extends HBox {
    //private final MenuButton menuButton;
    private final Button menu;
    private final TextField valueField;
    private final Button zoomInBtn;
    private final Button zoomOutBtn;
    private final SVGPath ziSvg;
    private final SVGPath zoSvg;
    private final MenuItem fitToWidth;
    private final MenuItem fitToHeight;
    private final SVGPath ftwSvg;
    private final SVGPath fthSvg;
    private final PdfViewer pdfViewer;

    /**
     * The zoom increase/decrement value
     */
    private static final double ZOOM_DEC = .25;

    /**
     * Constructs a ZoomControl to manage zooming within a PDF viewer.
     *
     * @param pdfViewer The PDF viewer associated with this ZoomControl.
     */
    public ZoomControl(PdfViewer pdfViewer) {
        this.pdfViewer = pdfViewer;
       // this.menuButton = new MenuButton();
        this.menu = new Button("▼");
        this.valueField = new TextField();
        this.valueField.setText(String.format("%s%s", (int)(this.pdfViewer.getZoomFactor()*100),"%"));
        this.zoomInBtn = new Button();
        this.zoomOutBtn = new Button();
        this.ziSvg = new SVGPath();
        this.zoSvg = new SVGPath();
        this.fitToWidth = new MenuItem("  Fit to Width");
        this.fitToHeight = new MenuItem("  Fit to Height");
        this.ftwSvg = new SVGPath();
        this.fthSvg = new SVGPath();
        initialize();
    }

    /**
     * Initializes the JavaFX controller or component.
     * This method is automatically called when the component is loaded.
     * You can perform any necessary setup tasks here.
     */
    private void initialize(){
        getStylesheets().add(getUserAgentStylesheet());
        getStyleClass().add("pdf-zoom-box");
        this.setAlignment(Pos.CENTER);
        this.setMaxHeight(20);

        ziSvg.setContent(ZOOM_IN_ZVG);
        ziSvg.setScaleX(.9);
        ziSvg.setScaleY(.9);
        ziSvg.getStyleClass().add("pdf-zoom-svg-icon");
        zoSvg.setContent(ZOOM_OUT_ZVG);
        zoSvg.setScaleX(.9);
        zoSvg.setScaleY(.9);
        zoSvg.getStyleClass().add("pdf-zoom-svg-icon");
        ftwSvg.setContent(FIT_TO_WIDTH);
        ftwSvg.getStyleClass().add("pdf-zoom-svg-icon");
        fthSvg.setContent(FIT_TO_HEIGHT);
        fthSvg.getStyleClass().add("pdf-zoom-svg-icon");
        //this.menuButton.setMaxWidth(96);
        //this.menu.setMaxWidth(96);
        this.valueField.setMaxWidth(60);

        //this.menuButton.setGraphic(valueField);
        this.menu.setGraphic(valueField);
        this.fitToHeight.setGraphic(fthSvg);
        this.fitToWidth.setGraphic(ftwSvg);

        /*
         * Add s separator
         */
        final SeparatorMenuItem separator = new SeparatorMenuItem();
        //this.menuButton.getItems().addAll(fitToWidth, fitToHeight, separator);

        /*
         * Add remaining needed items automatically
         */
        int inc =25;//Increase val
        for (int i = inc ; i <= 1000; i=i+inc) {
            if (i == 200){
                inc = 100;
            }
           // this.menuButton.getItems().add(buildForPercent(String.format(" %s%s",i,"%")));
        }

        zoomInBtn.setGraphic(ziSvg);
        zoomOutBtn.setGraphic(zoSvg);

        getChildren().addAll(menu, zoomOutBtn, zoomInBtn);

        setUpValueInput();

        setEvents();
    }

    /**
     * Sets up event handlers and listeners for user interactions in the JavaFX application.
     * This method is responsible for registering event handlers for buttons, mouse actions,
     * keyboard inputs, and other user interactions.
     */
    private void setEvents(){
        /*
         * Zoom actions
         */
        this.pdfViewer.zoomFactorProperty().addListener((obs, ozf, zf) -> {
            int percent = (int) (zf.doubleValue() * 100);
            this.valueField.setText(String.format("%s%s", percent,"%"));
            updateZoomButtons();
        });

        this.zoomInBtn.setOnAction(event->{
            this.pdfViewer.setFit(Fit.NONE);
            double factor = this.pdfViewer.getZoomFactor() + ZOOM_DEC;
            this.pdfViewer.setZoomFactor(Math.min(factor, this.pdfViewer.getMaxZoomFactor()));
        });

        this.zoomOutBtn.setOnAction(event->{
            this.pdfViewer.setFit(Fit.NONE);
            double factor = this.pdfViewer.getZoomFactor() - ZOOM_DEC;
            this.pdfViewer.setZoomFactor(Math.max(factor, this.pdfViewer.getMinZoomFactor()));
        });


        /*
         * Fit setup
         */
        this.pdfViewer.fitProperty().addListener((obs, o, fit) -> checkFit(fit));
        this.fitToWidth.setOnAction(event->{
            this.pdfViewer.setFit(Fit.HORIZONTAL);
        });

        this.fitToHeight.setOnAction(event->{
            this.pdfViewer.setFit(Fit.VERTICAL);
        });

        /*
         * When we enter a number value in the value input and hit enter,
         * we need to update the zoom and suppress the fire event from menuButton
         */
        this.valueField.setOnKeyPressed(event -> {
            if(event.getCode().equals(KeyCode.ENTER)){
                event.consume();
                int percent = Integer.parseInt(this.valueField.getText().replace("%",""));
                this.pdfViewer.setZoomFactor((double) percent /100);
                /*
                 * This is a bit of hacking to skip the menuButton from catching the keyEvent and showing the
                 * context menu
                 */
                KeyEvent newEvent = new KeyEvent(
                        KeyEvent.KEY_PRESSED,
                        "",
                        "",
                        KeyCode.ENTER,
                        false,
                        false,
                        false,
                        false
                );

                /*
                 * Now we can fire the new event and get the desired result
                 */
                //this.menuButton.getParent().fireEvent(newEvent);
            }
        });


    }

    /**
     * Set up the value input mechanism.
     * This method is responsible for configuring and initializing the value input component.
     */
    private void setUpValueInput(){
       final UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            /*
             * Allow digits and a single percent symbol
             */
            if (newText.matches("[0-9]*%")) {
                return change;
            }
            return null;
        };

       final TextFormatter<String> formatter = new TextFormatter<>(filter);
       this.valueField.setTextFormatter(formatter);
    }

    /**
     * Updates the state of zoom-related buttons based on the current zoom level.
     * This method is called to reflect changes in zoom levels.
     */
    private void updateZoomButtons() {
        double currentZoomFactor = this.pdfViewer.getZoomFactor();
        double maxZoomFactor = this.pdfViewer.getMaxZoomFactor();
        double minZoomFactor = this.pdfViewer.getMinZoomFactor();
        zoomInBtn.setDisable(currentZoomFactor >= maxZoomFactor);
        zoomOutBtn.setDisable(currentZoomFactor <= minZoomFactor);
    }


    /**
     * Checks the fit mode and updates button states accordingly.
     *
     * @param fit The fit mode (VERTICAL, HORIZONTAL, or NONE).
     */
    private void checkFit(Fit fit){
        switch (fit){
            case VERTICAL -> {
                this.fitToHeight.setDisable(true);
                this.fitToWidth.setDisable(false);
            }
            case HORIZONTAL -> {
                this.fitToWidth.setDisable(true);
                this.fitToHeight.setDisable(false);
            }
            default -> {
                this.fitToHeight.setDisable(false);
                this.fitToWidth.setDisable(false);
            }
        }
    }

    /**
     * Build a MenuItem for a given percentage text.
     *
     * @param text The text representing a zoom percentage (e.g., "200%").
     * @return A MenuItem configured to set the zoom factor when selected.
     */
    private MenuItem buildForPercent(String text){
        MenuItem m = new MenuItem(text);
        m.setOnAction(actionEvent -> {
            /*
             * W can not forget to set fit to NONE
             */
            this.pdfViewer.setFit(Fit.NONE);
            int p = Integer.parseInt(text.replace("%", "").replaceAll("\\s+", ""));
            pdfViewer.setZoomFactor((double) p /100);
        });
        return m;
    }

    private static ContextMenu findContextMenuInContainer(Region container) {
        for (Node node : container.getChildrenUnmodifiable()) {
            if (node instanceof Control control) {
                ContextMenu menu = control.getContextMenu();
                if (menu != null) {
                    return menu;
                }
            } else if (node instanceof Pane) {
                ContextMenu menu = findContextMenuInContainer((Pane) node);
                if (menu != null) {
                    return menu;
                }
            }
        }
        return null;
    }

    /**
     * Gets the URL of the user agent stylesheet for this custom control.
     * This stylesheet defines the default CSS styling for the control.
     *
     * @return The URL of the user agent stylesheet or an empty string if none is provided.
     */
    @Override
    public String getUserAgentStylesheet() {
        return Assets.load("/xss/it/ultimate/pdf/viewer/css/zoom-control.css").toExternalForm();
    }

}
