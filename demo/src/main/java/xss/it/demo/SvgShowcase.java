package xss.it.demo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import xss.it.nfx.svg.SvgDocument;
import xss.it.nfx.svg.scene.SvgView;

import java.io.File;

/**
 * Demo pane that showcases {@link SvgView}.
 *
 * <p>It renders an embedded sample SVG and exposes a zoom slider so you can scale
 * the graphic up and down and watch it stay perfectly crisp - the whole point of
 * the node. Because {@code SvgView} re-rasterizes the vector geometry at the
 * exact device-pixel size for the current zoom (instead of stretching a fixed
 * bitmap like {@code ImageView}), edges never blur no matter how far you zoom in.</p>
 *
 * @author XDSSWAR
 */
public final class SvgShowcase extends BorderPane {

    /*
     * An embedded sample SVG. It mixes rounded rectangles, a circle, a triangle
     * and a line of text so the demo exercises shapes, fills and the font path
     * (text shaping) all at once. Kept inline so the demo has no file/asset
     * dependency and works straight from `./gradlew :demo:run`.
     */
    private static final String SAMPLE_SVG =
            """
            <svg xmlns="http://www.w3.org/2000/svg" width="240" height="160" viewBox="0 0 240 160">
              <rect x="0" y="0" width="240" height="160" rx="16" fill="#1e88e5"/>
              <circle cx="80" cy="80" r="48" fill="#ffca28"/>
              <polygon points="160,32 208,128 112,128" fill="#e53935"/>
              <text x="120" y="150" font-family="sans-serif" font-size="18"
                    text-anchor="middle" fill="#ffffff">nfx-svg</text>
            </svg>
            """;

    /**
     * Builds the showcase: the SVG centered in a scroll pane, with a zoom slider
     * and a couple of toggles along the bottom.
     */
    public SvgShowcase() {
        /*
         * Parse the sample once. The document owns a native Skia handle; the
         * SvgView keeps it alive for the lifetime of this pane, and the handle is
         * released automatically by a Cleaner when both become unreachable (or
         * eagerly via SvgDocument.close()).
         */
        SvgDocument document = SvgDocument.loadContent(SAMPLE_SVG);

        /*
         * The star of the demo. Start at the SVG's intrinsic size (zoom 1.0); the
         * slider below drives the zoom. The node renders off the FX thread and
         * caches the current bitmap, so dragging the slider stays smooth.
         */
        SvgView svgView = new SvgView(document);
        /*
         * Let the zoom slider drive the size: cap the view at its preferred
         * (zoom-scaled) size so the surrounding StackPane centers it rather than
         * stretching it to fill. SvgView renders crisply at whatever size it ends
         * up - capping here just makes "zoom" mean "change the size".
         */
        svgView.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        /*
         * A checkerboard-ish backdrop behind the (transparent-by-default) SVG so
         * it is obvious the graphic itself has no opaque background - useful when
         * verifying alpha. The SvgView is centered on top of it.
         */
        StackPane stage = new StackPane(svgView);
        stage.setStyle("-fx-background-color: white;"
                + "-fx-background-image: null;");
        stage.setPadding(new Insets(24));

        /*
         * Put the stage in a scroll pane: when you zoom past the viewport the
         * graphic stays navigable rather than being clipped.
         */
        ScrollPane scroll = new ScrollPane(stage);
        scroll.setPannable(true);
        /*
         * Keep the SVG centered while it is smaller than the viewport, and let it
         * scroll once you zoom past the viewport. We do NOT use setFitToWidth /
         * setFitToHeight: those stretch the content to the viewport, which would
         * blow the (vector) graphic up without telling the SvgView to re-render at
         * that size. Instead we grow the stage's minimum size to the viewport, so
         * the StackPane centers the SvgView at its own (zoom-driven) size.
         */
        scroll.viewportBoundsProperty().addListener((o, a, b) ->
                stage.setMinSize(b.getWidth(), b.getHeight()));
        setCenter(scroll);

        // --- Controls --------------------------------------------------------

        /*
         * Zoom slider: 0.1x .. 10x. Bind the view's zoom directly to the slider
         * value so moving the thumb re-renders the SVG crisply at the new scale.
         */
        Slider zoom = new Slider(0.1, 10.0, 3.0);
        zoom.setPrefWidth(360);
        svgView.zoomProperty().bind(zoom.valueProperty());

        // Live read-out of the current zoom as a percentage.
        Label zoomLabel = new Label();
        zoomLabel.textProperty().bind(
                zoom.valueProperty().multiply(100).asString("Zoom: %.0f%%"));

        /*
         * "Dark background" toggle: drives the styleable background fill of the
         * SvgView (the -svg-background CSS color) to show it compositing over a
         * non-white surface.
         */
        CheckBox dark = new CheckBox("Dark background");
        dark.selectedProperty().addListener((o, was, on) -> {
            svgView.setBackgroundFill(on ? Color.web("#202124") : Color.TRANSPARENT);
            stage.setStyle("-fx-background-color: " + (on ? "#202124" : "white") + ";");
        });

        /*
         * "Open SVG..." lets you point the view at any .svg file on disk to test
         * it. We load via SvgDocument.load(File) and swap it onto the view; the
         * previously shown document is released automatically by its Cleaner once
         * unreferenced. Parse/IO failures are surfaced in a simple alert.
         */
        Button open = new Button("Open SVG...");
        open.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Open SVG");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("SVG files", "*.svg", "*.svgz"));
            File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                try {
                    svgView.setDocument(SvgDocument.load(file));
                } catch (RuntimeException ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Could not load SVG:\n" + ex.getMessage()).showAndWait();
                }
            }
        });

        HBox controls = new HBox(16, open, zoomLabel, zoom, dark);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(12, 16, 12, 16));
        setBottom(controls);
    }
}
