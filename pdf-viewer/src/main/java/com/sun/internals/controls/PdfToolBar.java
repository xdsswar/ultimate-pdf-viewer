package com.sun.internals.controls;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import xss.it.ultimate.pdf.viewer.Assets;
import xss.it.ultimate.pdf.viewer.Viewer;

/**
 * @author XDSSWAR
 * Created on 01/25/2024
 */
public final class PdfToolBar extends HBox {
    /**
     * Container for the left side of the header.
     */
    private final HBox headerLeftContainer;

    /**
     * Button for toggling thumbnails view.
     */
    private final Button thumbnailsButton;

    /**
     * SVG icon for the thumbnails button.
     */
    private final SVGPath thumbnailsSvg;

    /**
     * Separator between header components.
     */
    private final AnchorPane sep1;

    /**
     * Container for navigation buttons.
     */
    private final HBox navBox;

    /**
     * Button to navigate to the first page.
     */
    private final Button firstPageBtn;

    /**
     * SVG icon for the first page button.
     */
    private final SVGPath fpSvg;

    /**
     * Button to navigate to the previous page.
     */
    private final Button prevPageBtn;

    /**
     * SVG icon for the previous page button.
     */
    private final SVGPath ppSvg;

    /**
     * Input field for page number.
     */
    private final TextField pageInput;

    /**
     * Button to navigate to the next page.
     */
    private final Button nextPageBtn;

    /**
     * SVG icon for the next page button.
     */
    private final SVGPath npSvg;

    /**
     * Button to navigate to the last page.
     */
    private final Button lastPageSvg;

    /**
     * SVG icon for the last page button.
     */
    private final SVGPath lpSvg;

    /**
     * Label displaying page numbers.
     */
    private final Label pagesLabel;

    /**
     * Separator between header components.
     */
    private final AnchorPane sep2;

    /**
     * Container for zoom-related components.
     */
    private final HBox zoomBox;

    /**
     * Input field for zoom percentage.
     */
    private final TextField zoomInput;

    /**
     * Button to open zoom options menu.
     */
    private final Button zoomMenuBtn;

    /**
     * SVG icon for the zoom menu button.
     */
    private final SVGPath zmSvg;

    /**
     * Button to zoom in.
     */
    private final Button zoomInBtn;

    /**
     * SVG icon for the zoom in button.
     */
    private final SVGPath ziSvg;

    /**
     * Button to zoom out.
     */
    private final Button zoomOutBtn;

    /**
     * SVG icon for the zoom out button.
     */
    private final SVGPath zoSvg;

    /**
     * Container for the right side of the header.
     */
    private final HBox headerRightContainer;

    /**
     * Separator between header components.
     */
    private final AnchorPane sep3;

    /**
     * Button to open the viewer menu.
     */
    private final Button menuBtn;

    /**
     * SVG icon for the menu button.
     */
    private final SVGPath mSvg;

    /**
     * The main viewer component.
     */
    private final Viewer viewer;

    /**
     * Constructs a PDF toolbar associated with the specified viewer.
     *
     * @param viewer The viewer to which this toolbar belongs.
     */
    public PdfToolBar(Viewer viewer) {
        this.viewer = viewer;
        headerLeftContainer = new HBox();
        thumbnailsButton = new Button();
        thumbnailsSvg = new SVGPath();
        sep1 = new AnchorPane();
        navBox = new HBox();
        firstPageBtn = new Button();
        fpSvg = new SVGPath();
        prevPageBtn = new Button();
        ppSvg = new SVGPath();
        pageInput = new TextField();
        nextPageBtn = new Button();
        npSvg = new SVGPath();
        lastPageSvg = new Button();
        lpSvg = new SVGPath();
        pagesLabel = new Label();
        sep2 = new AnchorPane();
        zoomBox = new HBox();
        zoomInput = new TextField();
        zoomMenuBtn = new Button();
        zmSvg = new SVGPath();
        zoomInBtn = new Button();
        ziSvg = new SVGPath();
        zoomOutBtn = new Button();
        zoSvg = new SVGPath();
        headerRightContainer = new HBox();
        sep3 = new AnchorPane();
        menuBtn = new Button();
        mSvg = new SVGPath();

        /*
         * Initialize
         */
        initialize();

        /*
         * Events
         */
        initializeEvents();
    }

    /**
     * Initializes event handlers and listeners for the PdfToolBar.
     */
    private void initializeEvents(){

    }

    /**
     * Initializes the PdfToolBar by setting up its layout and components.
     */
    private void initialize(){
        getStyleClass().add("pdf-toolbar");
        getStylesheets().add(getUserAgentStylesheet());
        setMaxHeight(USE_PREF_SIZE);
        setMaxWidth(USE_PREF_SIZE);
        setMinHeight(USE_PREF_SIZE);
        setMinWidth(USE_PREF_SIZE);
        setPrefHeight(50.0);
        setMinHeight(50);
        setMaxHeight(50);

        headerLeftContainer.setAlignment(Pos.CENTER_LEFT);
        headerLeftContainer.setPrefWidth(10000.0);

        thumbnailsButton.setMnemonicParsing(false);
        thumbnailsButton.getStyleClass().add("pdf-toolbar-button");
        HBox.setMargin(thumbnailsButton, new Insets(0.0, 0.0, 0.0, 20.0));

        thumbnailsSvg.setContent(this.viewer.getIconsBundle().getString("pdf.thumbnails.svg"));
        thumbnailsSvg.getStyleClass().add("pdf-toolbar-button-icon");
        thumbnailsButton.setGraphic(thumbnailsSvg);

        sep1.setPrefHeight(40.0);
        sep1.getStyleClass().add("pdf-toolbar-divider");
        HBox.setMargin(sep1, new Insets(0.0, 0.0, 0.0, 20.0));

        navBox.setAlignment(Pos.CENTER);
        navBox.getStyleClass().add("pdf-toolbar-nav-box");
        HBox.setMargin(navBox, new Insets(0.0, 0.0, 0.0, 20.0));

        firstPageBtn.setMnemonicParsing(false);
        firstPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button");
        firstPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button-start");

        fpSvg.setContent(this.viewer.getIconsBundle().getString("pdf.first.last.page.svg"));
        fpSvg.setRotate(180.0);
        fpSvg.getStyleClass().add("pdf-toolbar-button-icon");
        firstPageBtn.setGraphic(fpSvg);

        prevPageBtn.setLayoutX(11.0);
        prevPageBtn.setLayoutY(11.0);
        prevPageBtn.setMnemonicParsing(false);
        prevPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button");

        ppSvg.setContent(this.viewer.getIconsBundle().getString("pdf.prev.next.page.svg"));
        ppSvg.setRotate(180.0);
        ppSvg.getStyleClass().add("pdf-toolbar-button-icon");
        prevPageBtn.setGraphic(ppSvg);

        pageInput.setPrefHeight(25.0);
        pageInput.setPrefWidth(60.0);
        pageInput.getStyleClass().add("pdf-toolbar-nav-box-input");

        nextPageBtn.setMnemonicParsing(false);
        nextPageBtn.getStyleClass().add("pdf-toolbar-nav-box-button");

        npSvg.setContent(this.viewer.getIconsBundle().getString("pdf.prev.next.page.svg"));
        npSvg.getStyleClass().add("pdf-toolbar-button-icon");
        nextPageBtn.setGraphic(npSvg);

        lastPageSvg.setMnemonicParsing(false);
        lastPageSvg.getStyleClass().add("pdf-toolbar-nav-box-button");
        lastPageSvg.getStyleClass().add("pdf-toolbar-nav-box-button-end");

        lpSvg.setContent(this.viewer.getIconsBundle().getString("pdf.first.last.page.svg"));
        lpSvg.getStyleClass().add("pdf-toolbar-button-icon");
        lastPageSvg.setGraphic(lpSvg);

        pagesLabel.getStyleClass().add("pdf-toolbar-nav-box-label");
        pagesLabel.setText("of 2000");
        HBox.setMargin(pagesLabel, new Insets(0.0, 0.0, 0.0, 5.0));

        sep2.setPrefHeight(40.0);
        sep2.getStyleClass().add("pdf-toolbar-divider");
        HBox.setMargin(sep2, new Insets(0.0, 0.0, 0.0, 20.0));

        zoomBox.setAlignment(Pos.BOTTOM_LEFT);
        zoomBox.setPrefHeight(100.0);
        zoomBox.getStyleClass().add("pdf-toolbar-nav-box");
        HBox.setMargin(zoomBox, new Insets(0.0, 0.0, 0.0, 20.0));

        zoomInput.setPrefHeight(25.0);
        zoomInput.setPrefWidth(60.0);
        zoomInput.getStyleClass().add("pdf-toolbar-nav-zoom-input");
        zoomInput.setText("1000%");

        zoomMenuBtn.setMnemonicParsing(false);
        zoomMenuBtn.getStyleClass().add("pdf-toolbar-nav-zoom-button");

        zmSvg.setContent(this.viewer.getIconsBundle().getString("pdf.zoom.menu.svg"));
        zmSvg.getStyleClass().add("pdf-toolbar-button-icon");
        zoomMenuBtn.setGraphic(zmSvg);

        zoomInBtn.setLayoutX(71.0);
        zoomInBtn.setLayoutY(11.0);
        zoomInBtn.setMnemonicParsing(false);
        zoomInBtn.getStyleClass().add("pdf-toolbar-nav-zoom-button");

        ziSvg.setContent(this.viewer.getIconsBundle().getString("pdf.zoom.in.svg"));
        ziSvg.getStyleClass().add("pdf-toolbar-button-icon");
        zoomInBtn.setGraphic(ziSvg);

        zoomOutBtn.setLayoutX(101.0);
        zoomOutBtn.setLayoutY(11.0);
        zoomOutBtn.setMnemonicParsing(false);
        zoomOutBtn.getStyleClass().add("pdf-toolbar-nav-box-button");
        zoomOutBtn.getStyleClass().add("pdf-toolbar-nav-box-button-end");


        zoSvg.setContent(this.viewer.getIconsBundle().getString("pdf.zoom.out.svg"));
        zoSvg.getStyleClass().add("pdf-toolbar-button-icon");
        zoomOutBtn.setGraphic(zoSvg);

        headerRightContainer.setAlignment(Pos.CENTER_RIGHT);
        headerRightContainer.setMinWidth(80.0);
        headerRightContainer.setPrefHeight(100.0);
        headerRightContainer.setPrefWidth(200.0);

        sep3.setPrefHeight(40.0);
        sep3.getStyleClass().add("pdf-toolbar-divider");
        HBox.setMargin(sep3, new Insets(0.0, 20.0, 0.0, 0.0));

        menuBtn.setMnemonicParsing(false);
        menuBtn.getStyleClass().add("pdf-toolbar-button");

        mSvg.setContent(this.viewer.getIconsBundle().getString("pdf.menu.svg"));
        mSvg.getStyleClass().add("pdf-toolbar-button-icon");
        menuBtn.setGraphic(mSvg);
        HBox.setMargin(menuBtn, new Insets(0.0, 20.0, 0.0, 0.0));

        headerLeftContainer.getChildren().add(thumbnailsButton);
        headerLeftContainer.getChildren().add(sep1);
        navBox.getChildren().add(firstPageBtn);
        navBox.getChildren().add(prevPageBtn);
        navBox.getChildren().add(pageInput);
        navBox.getChildren().add(nextPageBtn);
        navBox.getChildren().add(lastPageSvg);
        headerLeftContainer.getChildren().add(navBox);
        headerLeftContainer.getChildren().add(pagesLabel);
        headerLeftContainer.getChildren().add(sep2);
        zoomBox.getChildren().add(zoomInput);
        zoomBox.getChildren().add(zoomMenuBtn);
        zoomBox.getChildren().add(zoomInBtn);
        zoomBox.getChildren().add(zoomOutBtn);
        headerLeftContainer.getChildren().add(zoomBox);
        getChildren().add(headerLeftContainer);
        headerRightContainer.getChildren().add(sep3);
        headerRightContainer.getChildren().add(menuBtn);
        getChildren().add(headerRightContainer);

    }

    /**
     * Gets the user agent stylesheet for the PdfToolBar.
     * @return The URL of the user agent stylesheet.
     */
    @Override
    public String getUserAgentStylesheet() {
        return Assets.load("/xss/it/ultimate/pdf/viewer/css/toolbar.css").toExternalForm();
    }
}
