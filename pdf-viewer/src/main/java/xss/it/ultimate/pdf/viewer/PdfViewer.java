package xss.it.ultimate.pdf.viewer;

import com.sun.internals.PageData;
import com.sun.internals.PdfDocument;
import com.sun.internals.controls.IntegerField;
import com.sun.internals.controls.ScalableScrollPane;
import com.sun.internals.document.Document;
import com.sun.internals.enums.Fit;
import com.sun.internals.text.SearchResult;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static xss.it.ultimate.pdf.viewer.Assets.*;

/**
 * @author XDSSWAR
 * Created on 01/23/2024
 */
public final class PdfViewer extends AnchorPane {
    private static final Logger LOGGER = Logger.getLogger(PdfViewer.class.getName());

    /**
     * The URL to the CSS stylesheet used for styling the PDF viewer.
     */
    private static final String STYLE_SHEET = Assets.load("/xss/it/ultimate/pdf/viewer/css/pdf-viewer.css")
            .toExternalForm();

    /**
     * The zoom increase/decrement value
     */
    private static final double ZOOM_DEC = .10;

    /**
     * Style class
     */
    private static final String STYLE_CLASS = "pdf-viewer";

    /**
     * The header container for the document viewer.
     */
    private final HBox header;

    /**
     * First horizontal container.
     */
    private final HBox h1;

    /**
     * Button for showing thumbnails.
     */
    private final Button thumbsButton;

    /**
     * SVG path for the thumbsButton icon.
     */
    private final SVGPath thumbsSvg;

    /**
     * Button for opening a document.
     */
    private final Button openButton;

    /**
     * SVG path for the openButton icon.
     */
    private final SVGPath openSvg;

    /**
     * Button for saving the document.
     */
    private final Button saveButton;

    /**
     * SVG path for the saveButton icon.
     */
    private final SVGPath saveSvg;

    /**
     * Second horizontal container.
     */
    private final HBox h2;

    /**
     * Button for navigating to the previous page.
     */
    private final Button prevButton;

    /**
     * Button for navigating to the first page.
     */
    private final Button firstPageBtn;

    /**
     * SVG path for the firstButton icon.
     */
    private final SVGPath firstSvg;

    /**
     * SVG path for the prevButton icon.
     */
    private final SVGPath prevSvg;

    /**
     * Text input field for page index.
     */
    private final IntegerField pageIndexInput;

    /**
     * Button for navigating to the next page.
     */
    private final Button nextButton;

    /**
     * Button for navigating to the next page.
     */
    private final Button lastPageBtn;

    /**
     * SVG path for the nextButton icon.
     */
    private final SVGPath nextSvg;

    /**
     * SVG path for the lastButton icon.
     */
    private final SVGPath lastSvg;

    /**
     * Label displaying the page count.
     */
    private final Label pageCountLabel;

    /**
     * Third horizontal container.
     */
    private final HBox h3;

    /**
     * Button for grabbing or moving the document.
     */
    private final Button fitToHeightBtn;

    /**
     * SVG path for the grabButton icon.
     */
    private final SVGPath fitToHeightSvg;

    /**
     * Button for zooming out.
     */
    private final Button zoomOutButton;

    /**
     * SVG path for the zoomOutButton icon.
     */
    private final SVGPath zoomOutSvg;

    /**
     * Button for zooming in.
     */
    private final Button zoomInButton;

    /**
     * SVG path for the zoomInButton icon.
     */
    private final SVGPath zoomInSvg;

    /**
     * Button for fitting the page.
     */
    private final Button fitToWidthBtn;

    /**
     * SVG path for the firPageButton icon.
     */
    private final SVGPath fitToWodthSvg;

    /**
     * Fourth horizontal container.
     */
    private final HBox h4;

    /**
     * Fifth horizontal container.
     */
    private final HBox hBox;

    /**
     * Button for printing the document.
     */
    private final Button printButton;

    /**
     * SVG path for the printButton icon.
     */
    private final SVGPath printSvg;

    /**
     * Button for exporting the document.
     */
    private final Button exportButton;

    /**
     * SVG path for the exportButton icon.
     */
    private final SVGPath exportSvg;

    /**
     * SplitPane serving as the base container.
     */
    private final AnchorPane basePane;

    /**
     * AnchorPane for containing thumbnails.
     */
    private final AnchorPane thumbsContainer;

    /**
     * VBox for containing document pages.
     */
    private final VBox pageContainer;
    /**
     * ScrollablePane with zoom
     */
    private final ScalableScrollPane scalableScrollPane;

    /**
     * A static Executor used for managing asynchronous tasks.
     * This field is initially set to null and should be initialized with an Executor instance when needed.
     */
    private static Executor EXECUTOR = null;

    /**
     * Constructor
     */
    public PdfViewer() {
        header = new HBox();
        h1 = new HBox();
        thumbsButton = new Button();
        thumbsSvg = new SVGPath();
        openButton = new Button();
        openSvg = new SVGPath();
        saveButton = new Button();
        saveSvg = new SVGPath();
        h2 = new HBox();
        prevButton = new Button();
        firstPageBtn = new Button();
        firstSvg = new SVGPath();
        prevSvg = new SVGPath();
        pageIndexInput = new IntegerField();
        nextButton = new Button();
        lastPageBtn = new Button();
        nextSvg = new SVGPath();
        lastSvg = new SVGPath();
        pageCountLabel = new Label();
        h3 = new HBox();
        fitToHeightBtn = new Button();
        fitToHeightSvg = new SVGPath();
        zoomOutButton = new Button();
        zoomOutSvg = new SVGPath();
        zoomInButton = new Button();
        zoomInSvg = new SVGPath();
        fitToWidthBtn = new Button();
        fitToWodthSvg = new SVGPath();
        h4 = new HBox();
        hBox = new HBox();
        printButton = new Button();
        printSvg = new SVGPath();
        exportButton = new Button();
        exportSvg = new SVGPath();
        basePane = new AnchorPane();
        thumbsContainer = new AnchorPane();
        pageContainer = new VBox();
        scalableScrollPane = new ScalableScrollPane(this);
        initialize();
        getStyleClass().add(STYLE_CLASS);
        getStylesheets().add(getUserAgentStylesheet());
    }

    /**
     * Initializes and sets up the document viewer's components and UI elements.
     */
    private void initialize() {
        AnchorPane.setLeftAnchor(header, 0.0);
        AnchorPane.setRightAnchor(header, 0.0);
        AnchorPane.setTopAnchor(header, 0.0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPrefHeight(40.0);
        header.setMaxHeight(40.0);
        header.getStyleClass().add("header");

        h1.setAlignment(Pos.CENTER_LEFT);
        h1.setMinWidth(160.0);

        thumbsButton.setMnemonicParsing(false);
        thumbsButton.getStyleClass().add("header-button");

        thumbsSvg.setContent(Assets.THUMB_SVG);
        thumbsSvg.getStyleClass().add("svg-icon");
        thumbsButton.setGraphic(thumbsSvg);
        HBox.setMargin(thumbsButton, new Insets(0.0, 0.0, 0.0, 10.0));

        openButton.setMnemonicParsing(false);
        openButton.getStyleClass().add("header-button");

        openSvg.setContent(Assets.OPEN_SVG);
        openSvg.setScaleX(1.2);
        openSvg.setScaleY(1.2);
        openSvg.getStyleClass().add("svg-icon");
        openButton.setGraphic(openSvg);
        HBox.setMargin(openButton, new Insets(0.0, 0.0, 0.0, 10.0));

        saveButton.setMnemonicParsing(false);
        saveButton.getStyleClass().add("header-button");

        saveSvg.setContent(Assets.SAVE_SVG);
        saveSvg.setScaleX(1.2);
        saveSvg.setScaleY(1.2);
        saveSvg.getStyleClass().add("svg-icon");
        saveButton.setGraphic(saveSvg);
        HBox.setMargin(saveButton, new Insets(0.0, 0.0, 0.0, 10.0));

        h2.setAlignment(Pos.CENTER_LEFT);
        h2.setMinWidth(280.0);
        h2.getStyleClass().add("left-border");

        prevButton.setMnemonicParsing(false);
        prevButton.getStyleClass().add("header-page-buttons");
        HBox.setMargin(prevButton, new Insets(0.0));

        firstPageBtn.setMnemonicParsing(false);
        firstPageBtn.getStyleClass().add("header-page-buttons");
        firstPageBtn.getStyleClass().add("header-page-buttons-prev");
        firstSvg.setContent(FIRST_SVG);
        firstSvg.getStyleClass().add("svg-icon-page");
        firstPageBtn.setGraphic(firstSvg);
        HBox.setMargin(firstPageBtn, new Insets(0.0, 0.0, 0.0, 10.0));

        prevSvg.setContent(PREV_SVG);
        prevSvg.getStyleClass().add("svg-icon-page");
        prevButton.setGraphic(prevSvg);

        pageIndexInput.setPrefHeight(25.0);
        pageIndexInput.setPrefWidth(80.0);
        pageIndexInput.getStyleClass().add("header-page-number-input");
        pageIndexInput.setText("");

        nextButton.setMnemonicParsing(false);
        nextButton.getStyleClass().add("header-page-buttons");

        lastPageBtn.setMnemonicParsing(false);
        lastPageBtn.getStyleClass().add("header-page-buttons");
        lastPageBtn.getStyleClass().add("header-page-buttons-next");

        lastSvg.setContent(LAST_SVG);
        lastSvg.getStyleClass().add("svg-icon-page");
        lastPageBtn.setGraphic(lastSvg);
        HBox.setMargin(lastPageBtn, new Insets(0.0));


        nextSvg.setContent(NEXT_SVG);
        nextSvg.getStyleClass().add("svg-icon-page");
        nextButton.setGraphic(nextSvg);
        HBox.setMargin(nextButton, new Insets(0.0));

        pageCountLabel.getStyleClass().add("header-page-number-label");
        pageCountLabel.setText("");
        HBox.setMargin(pageCountLabel, new Insets(0.0, 0.0, 0.0, 10.0));

        h3.setAlignment(Pos.CENTER_LEFT);
        h3.setMinWidth(200.0);
        h3.getStyleClass().add("left-border");

        fitToHeightBtn.setMnemonicParsing(false);
        fitToHeightBtn.getStyleClass().add("header-button");

        fitToHeightSvg.setContent(FIT_TO_HEIGHT_SVG);
        fitToHeightSvg.setScaleX(1.4);
        fitToHeightSvg.setScaleY(1.4);
        fitToHeightSvg.getStyleClass().add("svg-icon");
        fitToHeightBtn.setGraphic(fitToHeightSvg);
        HBox.setMargin(fitToHeightBtn, new Insets(0.0, 0.0, 0.0, 10.0));

        zoomOutButton.setMnemonicParsing(false);
        zoomOutButton.getStyleClass().add("header-button");

        zoomOutSvg.setContent(Assets.ZOOM_OUT_SVG);
        zoomOutSvg.setScaleX(1.4);
        zoomOutSvg.setScaleY(1.4);
        zoomOutSvg.getStyleClass().add("svg-icon");
        zoomOutButton.setGraphic(zoomOutSvg);
        HBox.setMargin(zoomOutButton, new Insets(0.0, 0.0, 0.0, 10.0));

        zoomInButton.setMnemonicParsing(false);
        zoomInButton.getStyleClass().add("header-button");

        zoomInSvg.setContent(Assets.ZOOM_IN_SVG);
        zoomInSvg.setScaleX(1.4);
        zoomInSvg.setScaleY(1.4);
        zoomInSvg.getStyleClass().add("svg-icon");
        zoomInButton.setGraphic(zoomInSvg);
        HBox.setMargin(zoomInButton, new Insets(0.0, 0.0, 0.0, 10.0));

        fitToWidthBtn.setMnemonicParsing(false);
        fitToWidthBtn.getStyleClass().add("header-button");

        fitToWodthSvg.setContent(FIT_TO_WIDTH_SVG);
        fitToWodthSvg.setScaleX(1.4);
        fitToWodthSvg.setScaleY(1.4);
        fitToWodthSvg.getStyleClass().add("svg-icon");
        fitToWidthBtn.setGraphic(fitToWodthSvg);
        HBox.setMargin(fitToWidthBtn, new Insets(0.0, 0.0, 0.0, 10.0));

        h4.setAlignment(Pos.CENTER_RIGHT);
        h4.setPrefWidth(20000);

        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.getStyleClass().add("left-border");

        printButton.setMnemonicParsing(false);
        printButton.getStyleClass().add("header-button");

        printSvg.setContent(Assets.PRINT_SVG);
        printSvg.setScaleX(1.4);
        printSvg.setScaleY(1.4);
        printSvg.getStyleClass().add("svg-icon");
        printButton.setGraphic(printSvg);
        HBox.setMargin(printButton, new Insets(0.0, 10.0, 0.0, 10.0));

        exportButton.setMnemonicParsing(false);
        exportButton.getStyleClass().add("header-button");

        exportSvg.setContent(Assets.EXPORT_SVG);
        exportSvg.setScaleX(1.4);
        exportSvg.setScaleY(1.4);
        exportSvg.getStyleClass().add("svg-icon");
        exportButton.setGraphic(exportSvg);
        HBox.setMargin(exportButton, new Insets(0.0, 10.0, 0.0, 0.0));

        AnchorPane.setBottomAnchor(basePane, 0.0);
        AnchorPane.setLeftAnchor(basePane, 0.0);
        AnchorPane.setRightAnchor(basePane, 0.0);
        AnchorPane.setTopAnchor(basePane, 40.0);



        AnchorPane.setBottomAnchor(thumbsContainer, 0.0);
        AnchorPane.setLeftAnchor(thumbsContainer, 0.0);
        AnchorPane.setTopAnchor(thumbsContainer, 0.0);
        thumbsContainer.setPrefWidth(0.0);//Modify tio 320
        thumbsContainer.getStyleClass().add("thumbnail-pane");

        AnchorPane.setBottomAnchor(pageContainer, 0.0);
        AnchorPane.setLeftAnchor(pageContainer, 0.0);
        AnchorPane.setRightAnchor(pageContainer, 0.0);
        AnchorPane.setTopAnchor(pageContainer, 0.0);//Modify to the width of thumbsContainer
        pageContainer.getStyleClass().add("page-container");
        pageContainer.setAlignment(Pos.CENTER);

        h1.getChildren().add(thumbsButton);
        h1.getChildren().add(openButton);
        h1.getChildren().add(saveButton);
        header.getChildren().add(h1);
        h2.getChildren().add(firstPageBtn);
        h2.getChildren().add(prevButton);
        h2.getChildren().add(pageIndexInput);
        h2.getChildren().add(nextButton);
        h2.getChildren().add(lastPageBtn);
        h2.getChildren().add(pageCountLabel);
        header.getChildren().add(h2);
        h3.getChildren().add(fitToWidthBtn);
        h3.getChildren().add(fitToHeightBtn);
        h3.getChildren().add(zoomOutButton);
        h3.getChildren().add(zoomInButton);

        header.getChildren().add(h3);
        hBox.getChildren().add(printButton);
        hBox.getChildren().add(exportButton);
        h4.getChildren().add(hBox);
        header.getChildren().add(h4);
        basePane.getChildren().addAll(thumbsContainer,pageContainer);
        VBox.setVgrow(scalableScrollPane, Priority.ALWAYS);
        pageContainer.getChildren().add(scalableScrollPane);
        getChildren().addAll(header, basePane);

        /*
         * Events
         */
        initEvents();
    }


    /**
     * A property representing whether to show thumbnails in the PDF viewer.
     */
    private BooleanProperty showThumbnails;

    /**
     * Gets the BooleanProperty representing whether to show thumbnails in the PDF viewer.
     *
     * @return The BooleanProperty for showing thumbnails.
     */
    public BooleanProperty showThumbnailsProperty() {
        if (showThumbnails == null) {
            showThumbnails = new SimpleBooleanProperty(PdfViewer.this, "showThumbnails", true);
        }
        return showThumbnails;
    }

    /**
     * Gets the value of whether to show thumbnails in the PDF viewer.
     *
     * @return True if thumbnails should be shown, false otherwise.
     */
    public boolean isShowThumbnails() {
        return this.showThumbnailsProperty().get();
    }

    /**
     * Sets whether to show thumbnails in the PDF viewer.
     *
     * @param showThumbnails True to show thumbnails, false to hide them.
     */
    public void setShowThumbnails(boolean showThumbnails) {
        this.showThumbnailsProperty().set(showThumbnails);
    }

    /**
     * A property representing whether to cache thumbnails in the PDF viewer.
     */
    private BooleanProperty cacheThumbnails;

    /**
     * Gets the BooleanProperty representing whether to cache thumbnails in the PDF viewer.
     *
     * @return The BooleanProperty for caching thumbnails.
     */
    public BooleanProperty cacheThumbnailsProperty() {
        if (cacheThumbnails == null) {
            cacheThumbnails = new SimpleBooleanProperty(PdfViewer.this, "cacheThumbnails", true);
        }
        return cacheThumbnails;
    }

    /**
     * Gets the value of whether to cache thumbnails in the PDF viewer.
     *
     * @return True if thumbnails should be cached, false otherwise.
     */
    public boolean isCacheThumbnails() {
        return this.cacheThumbnailsProperty().get();
    }

    /**
     * Sets whether to cache thumbnails in the PDF viewer.
     *
     * @param cacheThumbnails True to cache thumbnails, false to disable caching.
     */
    public void setCacheThumbnails(boolean cacheThumbnails) {
        this.cacheThumbnailsProperty().set(cacheThumbnails);
    }

    /**
     * A property representing the zoom factor for the PDF viewer.
     */
    private DoubleProperty minZoomFactor;

    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    public DoubleProperty minZoomFactorProperty() {
        if (minZoomFactor == null) {
            minZoomFactor = new SimpleDoubleProperty(PdfViewer.this, "minZoomFactor", .25);
        }
        return minZoomFactor;
    }

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    public double getMinZoomFactor() {
        return this.minZoomFactorProperty().get();
    }

    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param minZoomFactor The desired zoom factor.
     */
    public void setMinZoomFactor(double minZoomFactor) {
        this.minZoomFactorProperty().set(minZoomFactor);
    }

    /**
     * A property representing the zoom factor for the PDF viewer.
     */
    private DoubleProperty maxZoomFactor;

    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    public DoubleProperty maxZoomFactorProperty() {
        if (maxZoomFactor == null) {
            maxZoomFactor = new SimpleDoubleProperty(PdfViewer.this, "maxZoomFactor", 10);
        }
        return maxZoomFactor;
    }

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    public double getMaxZoomFactor() {
        return this.maxZoomFactorProperty().get();
    }

    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param maxZoomFactor The desired zoom factor.
     */
    public void setMaxZoomFactor(double maxZoomFactor) {
        this.maxZoomFactorProperty().set(maxZoomFactor);
    }

    /**
     * A property representing the zoom factor for the PDF viewer.
     */
    private DoubleProperty zoomFactor;

    /**
     * Gets the DoubleProperty representing the zoom factor for the PDF viewer.
     *
     * @return The DoubleProperty for the zoom factor.
     */
    public DoubleProperty zoomFactorProperty() {
        if (zoomFactor == null) {
            zoomFactor = new SimpleDoubleProperty(PdfViewer.this, "zoomFactor", 1);
        }
        return zoomFactor;
    }

    /**
     * Gets the value of the zoom factor for the PDF viewer.
     *
     * @return The current zoom factor.
     */
    public double getZoomFactor() {
        return this.zoomFactorProperty().get();
    }

    /**
     * Sets the zoom factor for the PDF viewer.
     *
     * @param zoomFactor The desired zoom factor.
     */
    public void setZoomFactor(double zoomFactor) {
        this.zoomFactorProperty().set(zoomFactor);
    }

    /**
     * A property representing the rotation angle for the PDF pages.
     */
    private DoubleProperty pageRotation;

    /**
     * Gets the DoubleProperty representing the rotation angle for the PDF pages.
     *
     * @return The DoubleProperty for the page rotation angle.
     */
    public DoubleProperty pageRotationProperty() {
        if (pageRotation == null) {
            pageRotation = new SimpleDoubleProperty(PdfViewer.this, "pageRotation") {
                @Override
                public void set(double newValue) {
                    // super.set(newValue % 360);
                    var rotation = Math.floor(newValue / 90) * 90;
                    if (rotation < 0) rotation += 360;
                    if (rotation >= 360) rotation -= 360;
                    super.set(rotation % 360.0);
                    if (getDocument()!=null){
                        getDocument().setPageRotation(getPage(),rotation);
                    }
                }
            };
        }
        return pageRotation;
    }

    /**
     * Gets the rotation angle for the PDF pages.
     *
     * @return The current rotation angle in degrees.
     */
    public double getPageRotation() {
        return this.pageRotationProperty().get();
    }

    /**
     * Sets the rotation angle for the PDF pages.
     *
     * @param pageRotation The desired rotation angle in degrees.
     */
    public void setPageRotation(double pageRotation) {
        this.pageRotationProperty().set(pageRotation);
    }

    /**
     * A property representing the current page number in the PDF viewer.
     */
    private IntegerProperty page;

    /**
     * Gets the IntegerProperty representing the current page number in the PDF viewer.
     *
     * @return The IntegerProperty for the page number.
     */
    public IntegerProperty pageProperty() {
        if (page == null) {
            page = new SimpleIntegerProperty(PdfViewer.this, "page"){
                @Override
                public void set(int newValue) {
                    if (getDocument()!=null && newValue>=0){
                        if (newValue<getDocument().getNumberOfPages()){
                            super.set(newValue);
                            PageData data = getDocument().getPageRotation(newValue);
                            setPageRotation(data.getRotationAngle());
                        }
                    }
                }
            };
        }
        return page;
    }

    /**
     * Gets the current page number in the PDF viewer.
     *
     * @return The current page number.
     */
    public int getPage() {
        return this.pageProperty().get();
    }

    /**
     * Sets the current page number in the PDF viewer.
     *
     * @param page The desired page number.
     */
    public void setPage(int page) {
        this.pageProperty().set(page);
    }

    /**
     * An ObjectProperty representing the fit mode for a PDF page.
     * It can be set to one of the Fit enum values (VERTICAL, HORIZONTAL, NONE).
     */
    private ObjectProperty<Fit> fit;

    /**
     * Getter for the fit mode of the PDF page. Lazily initializes it to Fit.NONE if null.
     *
     * @return The Fit enum value representing the fit mode.
     */
    public ObjectProperty<Fit> fitProperty() {
        if (fit == null) {
            fit = new SimpleObjectProperty<>(this, "fit", Fit.NONE);
        }
        return fit;
    }

    /**
     * Gets the fit mode for the PDF page.
     *
     * @return The Fit enum value representing the fit mode (VERTICAL, HORIZONTAL, NONE).
     */
    public Fit getFit() {
        return fitProperty().get();
    }

    /**
     * Sets the fit mode for the PDF page.
     *
     * @param fit The Fit enum value to set as the fit mode.
     */
    public void setFit(Fit fit) {
        fitProperty().set(fit);
    }


    /**
     * A FloatProperty representing the DPI (dots per inch) for thumbnail rendering.
     * The DPI value determines the quality and resolution of the rendered thumbnails.
     */
    private FloatProperty thumbnailRenderDpi;

    /**
     * Returns the FloatProperty for controlling the thumbnail rendering DPI.
     * If not initialized, a new FloatProperty is created with a default value of 72f.
     *
     * @return The thumbnailRenderDpi property.
     */
    public FloatProperty thumbnailRenderDpiProperty() {
        if (thumbnailRenderDpi == null) {
            thumbnailRenderDpi = new SimpleFloatProperty(PdfViewer.this, "thumbnailRenderDpi", 72f);
        }
        return thumbnailRenderDpi;
    }

    /**
     * Gets the current value of the thumbnailRenderDpi property.
     *
     * @return The DPI value for thumbnail rendering.
     */
    public float getThumbnailRenderDpi() {
        return this.thumbnailRenderDpiProperty().get();
    }

    /**
     * Sets the thumbnailRenderDpi property to the specified DPI value.
     *
     * @param thumbnailRenderDpi The new DPI value for thumbnail rendering.
     */
    public void setThumbnailRenderDpi(float thumbnailRenderDpi) {
        this.thumbnailRenderDpiProperty().set(thumbnailRenderDpi);
    }

    /**
     * A property representing the size of thumbnails in the PDF viewer.
     */
    private DoubleProperty thumbnailSize;

    /**
     * Gets the DoubleProperty representing the size of thumbnails in the PDF viewer.
     * If the property is null, it initializes it with a default value.
     *
     * @return The DoubleProperty for the thumbnail size.
     */
    public DoubleProperty thumbnailSizeProperty() {
        if (thumbnailSize == null) {
            thumbnailSize = new SimpleDoubleProperty(PdfViewer.this, "thumbnailSize", 200d);
        }
        return thumbnailSize;
    }

    /**
     * Gets the value of the thumbnail size in the PDF viewer.
     *
     * @return The current thumbnail size.
     */
    public double getThumbnailSize() {
        return this.thumbnailSizeProperty().get();
    }

    /**
     * Sets the size of thumbnails in the PDF viewer.
     *
     * @param thumbnailSize The desired thumbnail size.
     */
    public void setThumbnailSize(double thumbnailSize) {
        this.thumbnailSizeProperty().set(thumbnailSize);
    }

    /**
     * A FloatProperty representing the DPI (dots per inch) for page rendering.
     * The DPI value determines the quality and resolution of the rendered pages.
     */
    private FloatProperty pageRenderDpi;

    /**
     * Returns the FloatProperty for controlling the page rendering DPI.
     * If not initialized, a new FloatProperty is created with a default value of 300f.
     *
     * @return The pageRenderDpi property.
     */
    public FloatProperty pageRenderDpiProperty() {
        if (pageRenderDpi == null) {
            pageRenderDpi = new SimpleFloatProperty(PdfViewer.this, "pageRenderDpi", 300f);
        }
        return pageRenderDpi;
    }

    /**
     * Gets the current value of the pageRenderDpi property.
     *
     * @return The DPI value for page rendering.
     */
    public float getPageRenderDpi() {
        return this.pageRenderDpiProperty().get();
    }

    /**
     * Sets the pageRenderDpi property to the specified DPI value.
     *
     * @param pageRenderDpi The new DPI value for page rendering.
     */
    public void setPageRenderDpi(float pageRenderDpi) {
        this.pageRenderDpiProperty().set(pageRenderDpi);
    }

    /**
     * An object property representing the document to be displayed in the PDF viewer.
     */
    private ObjectProperty<Document> document;

    /**
     * Gets the ObjectProperty representing the document to be displayed in the PDF viewer.
     * If the property is null, it initializes it.
     *
     * @return The ObjectProperty for the document.
     */
    public ObjectProperty<Document> documentProperty() {
        if (document == null) {
            document = new SimpleObjectProperty<>(PdfViewer.this, "document", null);
        }
        return document;
    }

    /**
     * Gets the value of the document to be displayed in the PDF viewer.
     *
     * @return The current document.
     */
    public Document getDocument() {
        return this.documentProperty().get();
    }

    /**
     * Sets the document to be displayed in the PDF viewer.
     *
     * @param document The document to be displayed.
     */
    public void setDocument(Document document) {
        this.documentProperty().set(document);
    }


    /**
     * Represents the currently selected search result as an ObjectProperty.
     */
    private ObjectProperty<SearchResult> selectedSearchResult;

    /**
     * Gets the ObjectProperty for the currently selected search result.
     *
     * @return The ObjectProperty for the selected search result.
     */
    public ObjectProperty<SearchResult> selectedSearchResultObjectProperty() {
        if (selectedSearchResult == null) {
            selectedSearchResult = new SimpleObjectProperty<>(this, "selectedSearchResult");
        }
        return selectedSearchResult;
    }

    /**
     * Gets the currently selected search result.
     *
     * @return The selected search result.
     */
    public SearchResult getSelectedSearchResult() {
        return selectedSearchResultObjectProperty().get();
    }

    /**
     * Sets the currently selected search result.
     *
     * @param selectedSearchResult The search result to set as selected.
     */
    public void setSelectedSearchResult(SearchResult selectedSearchResult) {
        this.selectedSearchResultObjectProperty().set(selectedSearchResult);
    }

    /**
     * Represents a list of search results as a ListProperty.
     */
    private ListProperty<SearchResult> searchResults;

    /**
     * Gets the ListProperty for the list of search results.
     *
     * @return The ListProperty for the search results.
     */
    public ListProperty<SearchResult> searchResultsProperty() {
        if (searchResults == null) {
            searchResults = new SimpleListProperty<>(this, "searchResults", FXCollections.observableArrayList());
        }
        return searchResults;
    }

    /**
     * Gets the list of search results.
     *
     * @return The list of search results as an ObservableList.
     */
    public ObservableList<SearchResult> getSearchResults() {
        return searchResultsProperty().get();
    }

    /**
     * Sets the list of search results.
     *
     * @param searchResults The list of search results to set.
     */
    public void setSearchResults(ObservableList<SearchResult> searchResults) {
        this.searchResultsProperty().set(searchResults);
    }

    /**
     * Represents the color for displaying search results as an ObjectProperty.
     */
    private ObjectProperty<Color> searchResultColor;

    /**
     * Gets the ObjectProperty for the color used to display search results.
     *
     * @return The ObjectProperty for the search result color.
     */
    public ObjectProperty<Color> searchResultColorProperty() {
        if (searchResultColor == null) {
            searchResultColor = new SimpleObjectProperty<>(this, "searchResultColor", Color.RED);
        }
        return searchResultColor;
    }

    /**
     * Gets the color used for displaying search results.
     *
     * @return The search result color.
     */
    public Color getSearchResultColor() {
        return searchResultColorProperty().get();
    }

    /**
     * Sets the color for displaying search results.
     *
     * @param color The color to set for search results.
     */
    public void setSearchResultColor(Color color) {
        this.searchResultColorProperty().set(color);
    }

    /**
     * Represents the search text as a StringProperty.
     */
    private StringProperty searchText;

    /**
     * Gets the StringProperty for the search text.
     *
     * @return The StringProperty for the search text.
     */
    public StringProperty searchTextProperty() {
        if (searchText == null) {
            searchText = new SimpleStringProperty(this, "searchText");
        }
        return searchText;
    }

    /**
     * Gets the search text.
     *
     * @return The search text.
     */
    public String getSearchText() {
        return searchTextProperty().get();
    }

    /**
     * Sets the search text.
     *
     * @param searchText The text to set as the search text.
     */
    public void setSearchText(String searchText) {
        this.searchTextProperty().set(searchText);
    }


    /*
     * =====================================================================================================================
     *                                          END OF PROPERTIES
     * =====================================================================================================================
     */

    /**
     * Loads a document into the PDF viewer control.
     *
     * @param supplier A supplier providing the document to load.
     * @throws NullPointerException If the supplier is null.
     */
    public void load(Supplier<Document> supplier) {
        Objects.requireNonNull(supplier, "Supplier can not be null.");
        load(supplier.get());
    }

    /**
     * Loads a document from an InputStream into the PDF viewer control.
     *
     * @param stream The InputStream containing the document.
     */
    public void load(InputStream stream) {
        load(() -> {
            try {
                return new PdfDocument(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Loads a document from a File into the PDF viewer control.
     *
     * @param file The File representing the document.
     */
    public void load(File file) {
        load(() -> {
            try {
                return new PdfDocument(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Loads a document into the PDF viewer control.
     *
     * @param document The document to load.
     * @throws NullPointerException If the document is null.
     */
    public void load(Document document) {
        Objects.requireNonNull(document, "Document can not be null");
        setDocument(document);
    }

    /**
     * Unloads the current document from the PDF viewer control and resets various settings.
     */
    public void unload() {
        setDocument(null);
        setZoomFactor(1);
        setRotate(0);
        pageIndexInput.setText("");
    }

    /**
     * Gets the Executor instance used for managing asynchronous tasks.
     *
     * @return The Executor instance.
     */
    public Executor getExecutor() {
        if (EXECUTOR == null) {
            EXECUTOR= Executors.newSingleThreadExecutor(r->{
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            });
        }
        return EXECUTOR;
    }

    /**
     * Rotates the currently displayed page in the PDF viewer 90 degrees counterclockwise (to the left).
     */
    public void rotateLeft() {
        setPageRotation(getPageRotation() - 90);
    }

    /**
     * Rotates the currently displayed page in the PDF viewer 90 degrees clockwise (to the right).
     */
    public void rotateRight() {
        setPageRotation(getPageRotation() + 90);
    }

    /**
     * Navigates to the next page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    public boolean gotoNextPage() {
        if (getDocument()==null){
            return false;
        }
        int currentPage = getPage();
        setPage(Math.min(getDocument().getNumberOfPages() - 1, getPage() + 1));
        return currentPage != getPage();
    }

    /**
     * Navigates to the previous page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    public boolean gotoPreviousPage() {
        if (getDocument()==null){
            return false;
        }
        int currentPage = getPage();
        setPage(Math.max(0, getPage() - 1));
        return currentPage != getPage();
    }

    /**
     * Navigates to the last page in the PDF document.
     *
     * @return True if the page changed, false otherwise.
     */
    public boolean gotoLastPage() {
        if (getDocument()==null){
            return false;
        }
        int currentPage = getPage();
        setPage(getDocument().getNumberOfPages() - 1);
        return currentPage != getPage();
    }

    /**
     * Navigates to the first page of the document.
     *
     * @return True if the navigation to the first page was successful, false otherwise.
     */
    public boolean gotoFirstPage() {
        if (getDocument()==null){
            return false;
        }
        int currentPage = getPage();
        setPage(0);
        return currentPage != getPage();
    }


    /**
     * Opens a file dialog to select and open a PDF document.
     * Displays a file dialog, allows the user to choose a PDF file, and loads it if selected.
     */
    public void open(){
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Open");
        chooser.getExtensionFilters().add(Assets.PDF_EXTENSION_FILTER);
        final File file= chooser.showOpenDialog(getScene().getWindow());
        if (file == null) return;
        unload();
        load(file);
    }

    @Override
    public String getUserAgentStylesheet() {
        return STYLE_SHEET;
    }


    /*
     * =================================================================================================================
     *
     *                                   H E L P E R S
     *
     * =================================================================================================================
     */

    /**
     * An ObjectProperty representing the current viewport for displaying the content.
     * The viewport is defined by a Rectangle2D representing the visible region of the content.
     */
    private ObjectProperty<Rectangle2D> currentViewPort;

    /**
     * Returns the ObjectProperty for controlling the current viewport.
     * If not initialized, a new ObjectProperty is created with a default viewport.
     *
     * @return The currentViewPort property.
     */
    public ObjectProperty<Rectangle2D> currentViewPortProperty() {
        if (currentViewPort == null) {
            currentViewPort = new SimpleObjectProperty<>(
                    new Rectangle2D(0.0, 0.0, 1.0, 1.0)
            );
        }
        return currentViewPort;
    }

    /**
     * Gets the current viewport, represented as a Rectangle2D.
     *
     * @return The current viewport.
     */
    public Rectangle2D getCurrentViewPort() {
        return this.currentViewPortProperty().get();
    }

    /**
     * Sets the current viewport to the specified Rectangle2D.
     *
     * @param currentViewPort The new viewport to set.
     */
    public void setCurrentViewPort(Rectangle2D currentViewPort) {
        this.currentViewPortProperty().set(currentViewPort);
    }


    /**
     * Switches the viewport for the document being displayed.
     *
     * @param oldPage The old page being displayed (can be null).
     * @param newPage The new page to be displayed (can be null).
     */
    private void switchViewport(Integer oldPage, Integer newPage) {
        if (getDocument() == null){
            return;
        }
        //FIXED: When new doc added we can no longer set viewport for old pages
        if (oldPage != null && getDocument().getNumberOfPages()>oldPage) {
            getDocument().setViewport(oldPage, null);
        }
        if (newPage != null) {
            Rectangle2D vp = getCurrentViewPort();
            Rectangle2D vpn = new Rectangle2D(
                    Math.max(vp.getMinX(), 0.0),
                    Math.max(vp.getMinY(), 0.0),
                    Math.min(vp.getWidth(), 1.0),
                    Math.min(vp.getHeight(), 1.0)
            );
            if (!vpn.equals(new Rectangle2D(0.0, 0.0, 1.0, 1.0))) {
                getDocument().setViewport(newPage, vpn);
            } else {
                getDocument().setViewport(newPage, null);
            }
        }
    }

    /**
     * Initialize the events
     */
    private void initEvents(){
        /*
         * Navigation Buttons
         */
        firstPageBtn.setOnAction(event-> gotoFirstPage());
        prevButton.setOnAction(event -> gotoPreviousPage());
        nextButton.setOnAction(event -> gotoNextPage());
        lastPageBtn.setOnAction(event-> gotoLastPage());

        /*
         * Open
         */
        openButton.setOnAction(event -> open());


        /*
         * Set page by index from input
         */
        pageIndexInput.setOnKeyPressed(event -> {
            if (pageIndexInput.getText().startsWith("0")){
                pageIndexInput.setText(pageIndexInput.getText().replaceFirst("0",""));
            }
            if (pageIndexInput.getText().isEmpty() || getDocument() == null ){
                pageIndexInput.setInteger(1);
            }
            else  if (getDocument() != null && pageIndexInput.getInteger()> getDocument().getNumberOfPages()){
                pageIndexInput.setInteger(getDocument().getNumberOfPages());
            }

            if (event.getCode().equals(KeyCode.ENTER)){
                setPage(pageIndexInput.getInteger()-1);
            }
        });


        /*
         * Update page input
         */
        pageProperty().addListener((observable, oldValue, newValue) -> {
            switchViewport(oldValue.intValue(), newValue.intValue());
            pageIndexInput.setInteger(newValue.intValue()+1);
            PageData current=getDocument().getPagesList().get(newValue.intValue());
            log(Level.INFO, "Loaded Pdf page : "+ (current.getPageNumber()+1));
        });


        /*
         * Document changes
         */
        documentProperty().addListener((observable, oldValue, document) -> {
            if (oldValue!=null){
                try {
                    oldValue.close();
                } catch (IOException e) {
                    pageCountLabel.setText("");
                    pageIndexInput.setInteger(1);
                    //TODO alert here
                    throw new RuntimeException(e);
                }
            }
            if (document!=null){
                scalableScrollPane.reload();
                pageCountLabel.setText(String.format("/ %s",getDocument().getNumberOfPages()));
                gotoFirstPage();
                pageIndexInput.setText(String.format("%s", 1));
            }
            else {
                pageCountLabel.setText("");
            }
        });

        /*
         * Change viewPort
         */
        currentViewPortProperty().addListener((observable, oldValue, newValue) ->
                switchViewport(null, getPage()));

        /*
         * Fit setup
         */
        fitToWidthBtn.setOnAction(event -> setFit(Fit.HORIZONTAL));
        fitToHeightBtn.setOnAction(event-> setFit(Fit.VERTICAL));
        checkFit(getFit());
        fitProperty().addListener((obs, o, fit) -> checkFit(fit));

        /*
         * Zoom util
         */
        zoomFactorProperty().addListener((obs, ozf, zf) -> {
            double percent = zf.doubleValue() * 100;
            System.out.println(percent);
            updateZoomButtons();
        });

        zoomInButton.setOnAction(event->{
            setFit(Fit.NONE);
            double factor = getZoomFactor() + ZOOM_DEC;
            setZoomFactor(Math.min(factor, getMaxZoomFactor()));
        });

        zoomOutButton.setOnAction(event->{
            setFit(Fit.NONE);
            double factor = getZoomFactor() - ZOOM_DEC;
            setZoomFactor(Math.max(factor, getMinZoomFactor()));
        });


    }

    /**
     * Checks the fit mode and updates button states accordingly.
     *
     * @param fit The fit mode (VERTICAL, HORIZONTAL, or NONE).
     */
    private void checkFit(Fit fit){
        switch (fit){
            case VERTICAL -> {
                fitToHeightBtn.setDisable(true);
                fitToWidthBtn.setDisable(false);
            }
            case HORIZONTAL -> {
                fitToWidthBtn.setDisable(true);
                fitToHeightBtn.setDisable(false);
            }
            default -> {
                fitToHeightBtn.setDisable(false);
                fitToWidthBtn.setDisable(false);
            }
        }
    }

    /**
     * Updates the state of zoom-related buttons based on the current zoom level.
     * This method is called to reflect changes in zoom levels.
     */
    private void updateZoomButtons() {
        double currentZoomFactor = getZoomFactor();
        double maxZoomFactor = getMaxZoomFactor();
        double minZoomFactor = getMinZoomFactor();
        zoomInButton.setDisable(currentZoomFactor >= maxZoomFactor);
        zoomOutButton.setDisable(currentZoomFactor <= minZoomFactor);
    }

    /*
     * =================================================================================================================
     *
     *                             STUPID LOGGING UTIL. DO NOT PEN ATTENTION TO IT :)
     *
     * =================================================================================================================
     */
    /**
     * Logging
     */
    private static void log(Level level, Object msg){
        if (DEBUG) {
            LOGGER.log(level, String.format("%s", msg));
        }
    }
}
