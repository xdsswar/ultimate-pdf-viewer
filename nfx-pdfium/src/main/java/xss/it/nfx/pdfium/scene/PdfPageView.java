package xss.it.nfx.pdfium.scene;

import com.xss.it.nfx.pdfium.scene.NfxCircularLoader;
import com.xss.it.nfx.pdfium.scene.RenderLayer;
import com.xss.it.nfx.pdfium.scene.TextLayer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleConverter;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;
import xss.it.nfx.pdfium.PdfDocument;
import xss.it.nfx.pdfium.PdfPage;
import xss.it.nfx.pdfium.render.PdfRenderer;
import xss.it.nfx.pdfium.text.PdfSearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A reactive JavaFX node that displays a single PDF page, crisply, at any zoom.
 *
 * <p>Set a {@link #documentProperty() document} and {@link #pageIndexProperty()
 * page index}, then drive {@link #zoomProperty() zoom}, {@link #dpiProperty()
 * dpi} and {@link #rotationProperty() rotation}. The page is rasterized at the
 * exact device-pixel size for the current zoom (effective render scale =
 * {@code zoom * dpi/72 * screenOutputScale}), so it stays pixel-perfect. Changes
 * are debounced and rendered off the FX thread; call {@link #requestRender()} to
 * force an immediate render.</p>
 *
 * <p>The node is a stack of layers: the page bitmap, a text layer (selection,
 * copy, select-all, search highlights) and any custom {@link PdfLayer}s you add
 * via {@link #getLayers()} — for annotations, drawing, overlays, etc.</p>
 *
 * <p>CSS-styleable colors: {@code -fx-page-background}, {@code -fx-selection-color},
 * {@code -fx-highlight-color}.</p>
 *
 * @author XDSSWAR
 */
public class PdfPageView extends Region {

    private static final String STYLE_CLASS = "pdf-page-view";

    /** PDF base resolution (points per inch). */
    private static final double POINTS_DPI = 72.0;

    /** Debounce window before a crisp re-render after a property change. */
    private static final Duration DEBOUNCE = Duration.millis(140);

    /** Grace period before a slow render reveals the per-page loader. */
    private static final Duration LOADER_DELAY = Duration.millis(180);

    /**
     * Largest bitmap dimension (pixels) rendered for a single page. Kept well
     * below the GPU texture limit (16384) so several pages can coexist without
     * exhausting video memory at high zoom — an over-large/too-many texture set
     * makes the driver hand back a null texture and crashes the Prism pipeline.
     * A page only softens slightly beyond this zoom instead of crashing.
     */
    private static final double MAX_TEXTURE_PX = 4096.0;

    /** Shared daemon pool used for background rendering by default. */
    private static final Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "nfx-pdfium-render");
        t.setDaemon(true);
        return t;
    });

    /* ------------------------------------------------------------ children */

    private final RenderLayer renderLayer = new RenderLayer();
    // Constructed in the constructor body (after the styleable color + highlight
    // fields exist) since TextLayer reads them.
    private final TextLayer textLayer;
    private final ObservableList<PdfLayer> layers = FXCollections.observableArrayList();

    /** The animated, CSS-styleable loader shown inside the overlay. */
    private final NfxCircularLoader spinner = new NfxCircularLoader();
    /** Top-most overlay showing the loader while a slow render is in flight. */
    private final StackPane loader = buildLoader(spinner);
    private final PauseTransition loaderDelay = new PauseTransition(LOADER_DELAY);
    /** Fast fade played when a page's bitmap first appears (e.g. scrolled in). */
    private final FadeTransition imageFade = new FadeTransition(Duration.millis(120), renderLayer);

    /* -------------------------------------------------------------- state */

    private final PauseTransition debounce = new PauseTransition(DEBOUNCE);
    private final AtomicLong renderSeq = new AtomicLong();
    private PdfPage currentPage;
    /** Whether a rendered bitmap is currently shown (drives loader timing). */
    private boolean hasImage;

    /** Re-renders when the host window's device scale changes. */
    private final ChangeListener<Number> outputScaleListener = (s, a, b) -> requestRender();
    /** The window we currently listen to (so the listener can be removed on detach). */
    private Window listenedWindow;

    /**
     * Creates an empty page view. Assign a {@link #documentProperty() document}
     * to show content.
     */
    public PdfPageView() {
        getStyleClass().add(STYLE_CLASS);
        // Now that all color + highlight fields are initialized, build the text layer.
        this.textLayer = new TextLayer(this);
        getChildren().addAll(renderLayer, textLayer, loader);

        debounce.setOnFinished(e -> doRender());
        // The loader only appears if a render outlasts the grace period.
        loaderDelay.setOnFinished(e -> showLoader());

        // Custom layers stack above the text layer; the loader stays on top.
        layers.addListener((ListChangeListener<PdfLayer>) c -> {
            getChildren().setAll(renderLayer, textLayer);
            getChildren().addAll(layers);
            getChildren().add(loader);
            notifyLayers();
            requestLayout();
        });

        // Document / page changes: resolve the page and render immediately.
        documentProperty().addListener((o, a, b) -> onPageSourceChanged());
        pageIndexProperty().addListener((o, a, b) -> onPageSourceChanged());

        // Geometry changes: relayout now (instant stretch), re-render crisp soon.
        zoomProperty().addListener((o, a, b) -> onGeometryChanged());
        dpiProperty().addListener((o, a, b) -> onGeometryChanged());
        rotationProperty().addListener((o, a, b) -> onGeometryChanged());

        // Re-render once attached to a window (correct output scale) and whenever
        // the window's device scale changes (e.g. dragged to another monitor).
        // When detached (e.g. virtualized out of a continuous list), stop the
        // loader animation and timers: a running JavaFX animation is held alive by
        // the animation system, so leaving it running keeps the node consuming
        // pulses and prevents it from being garbage-collected.
        sceneProperty().addListener((o, oldScene, scene) -> {
            if (scene == null) {
                debounce.stop();
                loaderDelay.stop();
                imageFade.stop();
                renderLayer.setOpacity(1.0); // reset baseline for reuse
                hideLoader();
                detachWindowListener();
                return;
            }
            attachWindowListener(scene.getWindow());
            requestRender();
        });

        onPageSourceChanged();
    }

    /* --------------------------------------------------------- properties */

    private final ObjectProperty<PdfDocument> document = new SimpleObjectProperty<>(this, "document");

    /** The document to display. */
    public final ObjectProperty<PdfDocument> documentProperty() {
        return document;
    }

    public final PdfDocument getDocument() {
        return document.get();
    }

    public final void setDocument(PdfDocument value) {
        document.set(value);
    }

    private final IntegerProperty pageIndex = new SimpleIntegerProperty(this, "pageIndex", 0);

    /** The zero-based index of the displayed page. */
    public final IntegerProperty pageIndexProperty() {
        return pageIndex;
    }

    public final int getPageIndex() {
        return pageIndex.get();
    }

    public final void setPageIndex(int value) {
        pageIndex.set(value);
    }

    private final DoubleProperty zoom = new SimpleDoubleProperty(this, "zoom", 1.0);

    /** Display zoom factor ({@code 1.0} = 100%). */
    public final DoubleProperty zoomProperty() {
        return zoom;
    }

    public final double getZoom() {
        return zoom.get();
    }

    public final void setZoom(double value) {
        zoom.set(value);
    }

    private final DoubleProperty dpi = new SimpleDoubleProperty(this, "dpi", POINTS_DPI);

    /** Base render resolution in DPI (default 72 = 1 point per pixel at zoom 1). */
    public final DoubleProperty dpiProperty() {
        return dpi;
    }

    public final double getDpi() {
        return dpi.get();
    }

    public final void setDpi(double value) {
        dpi.set(value);
    }

    private final DoubleProperty rotation = new SimpleDoubleProperty(this, "rotation", 0);

    /** Page rotation in degrees (a multiple of 90, clockwise). */
    public final DoubleProperty rotationProperty() {
        return rotation;
    }

    public final double getRotation() {
        return rotation.get();
    }

    public final void setRotation(double value) {
        rotation.set(value);
    }

    private final BooleanProperty textSelectable = new SimpleBooleanProperty(this, "textSelectable", true);

    /** Whether the user can select text on the page. */
    public final BooleanProperty textSelectableProperty() {
        return textSelectable;
    }

    public final boolean isTextSelectable() {
        return textSelectable.get();
    }

    public final void setTextSelectable(boolean value) {
        textSelectable.set(value);
    }

    private final ObjectProperty<Executor> renderExecutor =
            new SimpleObjectProperty<>(this, "renderExecutor", DEFAULT_EXECUTOR);

    /** Executor used for background rendering (defaults to a shared daemon pool). */
    public final ObjectProperty<Executor> renderExecutorProperty() {
        return renderExecutor;
    }

    public final Executor getRenderExecutor() {
        Executor e = renderExecutor.get();
        return e != null ? e : DEFAULT_EXECUTOR;
    }

    public final void setRenderExecutor(Executor value) {
        renderExecutor.set(value);
    }

    /* ------------------------------------------------------ styleable colors */

    private final StyleableObjectProperty<Color> pageBackground =
            new SimpleStyleableObjectProperty<>(StyleableProperties.PAGE_BACKGROUND, this, "pageBackground", Color.WHITE) {
                @Override
                protected void invalidated() {
                    requestRender();
                }
            };

    /** Page background fill ({@code -fx-page-background}). */
    public final StyleableObjectProperty<Color> pageBackgroundProperty() {
        return pageBackground;
    }

    public final Color getPageBackground() {
        return pageBackground.get();
    }

    public final void setPageBackground(Color value) {
        pageBackground.set(value);
    }

    private final StyleableObjectProperty<Color> selectionColor =
            new SimpleStyleableObjectProperty<>(StyleableProperties.SELECTION_COLOR, this, "selectionColor",
                    Color.web("#3399ff", 0.40));

    /** Text-selection fill ({@code -fx-selection-color}). */
    public final StyleableObjectProperty<Color> selectionColorProperty() {
        return selectionColor;
    }

    public final Color getSelectionColor() {
        return selectionColor.get();
    }

    public final void setSelectionColor(Color value) {
        selectionColor.set(value);
    }

    private final StyleableObjectProperty<Color> highlightColor =
            new SimpleStyleableObjectProperty<>(StyleableProperties.HIGHLIGHT_COLOR, this, "highlightColor",
                    Color.web("#ffd200", 0.50));

    /** Search-highlight fill ({@code -fx-highlight-color}). */
    public final StyleableObjectProperty<Color> highlightColorProperty() {
        return highlightColor;
    }

    public final Color getHighlightColor() {
        return highlightColor.get();
    }

    public final void setHighlightColor(Color value) {
        highlightColor.set(value);
    }

    /* ------------------------------------------------------ highlights/layers */

    private final ObservableList<PdfSearchResult> highlights = FXCollections.observableArrayList();

    /** Search results to highlight on this page (filtered by page index). */
    public final ObservableList<PdfSearchResult> getHighlights() {
        return highlights;
    }

    /** Custom overlay layers, stacked above the text layer. */
    public final ObservableList<PdfLayer> getLayers() {
        return layers;
    }

    private final ObjectProperty<PdfSelectionModel> selectionModel =
            new SimpleObjectProperty<>(this, "selectionModel", new PdfSelectionModel());

    /**
     * The text-selection model. Defaults to a per-view model; assign a shared
     * model to several page views (e.g. in a continuous viewer) to make a single
     * selection span pages.
     *
     * @return the selection-model property
     */
    public final ObjectProperty<PdfSelectionModel> selectionModelProperty() {
        return selectionModel;
    }

    public final PdfSelectionModel getSelectionModel() {
        return selectionModel.get();
    }

    public final void setSelectionModel(PdfSelectionModel model) {
        selectionModel.set(model != null ? model : new PdfSelectionModel());
    }

    /** Copies the current (possibly multi-page) text selection to the clipboard. */
    public void copySelection() {
        String text = getSelectedText();
        if (!text.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    /** Selects all text in the document. */
    public void selectAll() {
        getSelectionModel().selectAll();
    }

    /** The currently selected text across all spanned pages (may be empty). */
    public String getSelectedText() {
        return getSelectionModel().selectedText(getDocument());
    }

    /* ----------------------------------------------------------- rendering */

    /** Forces an immediate (non-debounced) re-render of the current page. */
    public final void requestRender() {
        debounce.stop();
        doRender();
    }

    /**
     * Ensures this page has a rendered bitmap: triggers a render only if one isn't
     * already shown. Safe to call repeatedly (a no-op once the page has rendered) —
     * use it to recover pages whose render was dropped or failed.
     */
    public final void ensureRendered() {
        if (currentPage != null && !hasImage) {
            requestRender();
        }
    }

    private void onPageSourceChanged() {
        PdfDocument doc = getDocument();
        int idx = getPageIndex();
        currentPage = (doc != null && !doc.isClosed() && idx >= 0 && idx < doc.getPageCount())
                ? doc.getPage(idx) : null;
        PdfSelectionModel sm = getSelectionModel();
        if (sm != null) {
            sm.setPageCount(doc != null ? doc.getPageCount() : 0);
        }
        // The page changed (e.g. a recycled node repointed to another page): drop
        // the stale bitmap and show the loader immediately rather than briefly
        // displaying the previous page's content.
        hasImage = false;
        renderLayer.setImage(null);
        recompute();
        notifyLayers();
        requestRender();
    }

    private void onGeometryChanged() {
        recompute();        // instant: stretch existing bitmap to the new size
        notifyLayers();
        debounce.playFromStart();   // crisp re-render once changes settle
    }

    /** Points-to-logical-pixels factor for the current zoom/dpi. */
    private double displayScale() {
        return getZoom() * (getDpi() / POINTS_DPI);
    }

    /** Updates the preferred size from the page and current display scale. */
    private void recompute() {
        double w = 0, h = 0;
        if (currentPage != null) {
            double ds = displayScale();
            double pw = snapToDevice(currentPage.getWidth() * ds);
            double ph = snapToDevice(currentPage.getHeight() * ds);
            boolean swap = quarterTurns(getRotation()) % 2 == 1;
            w = swap ? ph : pw;
            h = swap ? pw : ph;
        }
        setPrefSize(w, h);
        requestLayout();
    }

    /**
     * Snaps a logical length so it lands on a whole number of device pixels.
     * Keeping the node size device-aligned makes the bitmap (rendered at exactly
     * that many device pixels) fill the node 1:1 — crisp, no soft scaling.
     *
     * @param logical a logical length
     * @return the device-aligned logical length
     */
    public double snapToDevice(double logical) {
        double s = outputScale();
        return Math.round(logical * s) / s;
    }

    private void notifyLayers() {
        double ds = displayScale();
        double rot = getRotation();
        textLayer.pageUpdated(currentPage, ds, rot);
        for (PdfLayer layer : layers) {
            layer.pageUpdated(currentPage, ds, rot);
        }
    }

    private void doRender() {
        PdfPage page = currentPage;
        if (page == null) {
            loaderDelay.stop();
            hasImage = false;
            hideLoader();
            renderLayer.setImage(null);
            return;
        }
        double scale = outputScale();
        double renderScale = displayScale() * scale;
        // Clamp so neither bitmap dimension exceeds the GPU texture limit: an
        // over-large texture fails to allocate (its D3D/GL resource comes back
        // null) and crashes the Prism pipeline, besides wasting huge memory. At
        // extreme zoom the page softens slightly instead of crashing.
        double maxDim = Math.max(page.getWidth(), page.getHeight()) * renderScale;
        if (maxDim > MAX_TEXTURE_PX) {
            renderScale *= MAX_TEXTURE_PX / maxDim;
        }
        final double finalScale = renderScale;
        // First appearance of this page's bitmap (vs. a zoom re-render) -> fade in.
        final boolean fadeIn = !hasImage;
        int rotationDegrees = (int) Math.round(getRotation());
        Color bg = getPageBackground();
        long seq = renderSeq.incrementAndGet();
        if (hasImage) {
            // Re-render (e.g. zoom/rotation): keep the current image visible and only
            // reveal the loader if this render outlasts the grace period (flicker-free).
            loaderDelay.playFromStart();
        } else {
            // First paint of this page: nothing to show yet, so show the loader
            // immediately and keep it until the bitmap arrives.
            showLoader();
        }
        getRenderExecutor().execute(() -> {
            try {
                Image image = PdfRenderer.render(page, finalScale, rotationDegrees, bg);
                Platform.runLater(() -> {
                    if (seq == renderSeq.get()) {
                        loaderDelay.stop();
                        hideLoader();
                        renderLayer.setImage(image); // fills the device-aligned node 1:1
                        hasImage = image != null;
                        if (fadeIn && image != null) {
                            imageFade.stop();
                            renderLayer.setOpacity(0.0);
                            imageFade.setFromValue(0.0);
                            imageFade.setToValue(1.0);
                            imageFade.playFromStart();
                        }
                    }
                });
            } catch (Throwable ignored) {
                // A stale or failed render is dropped; the next one supersedes it.
            }
        });
    }

    /**
     * The live device-pixel scale of the window this node is shown on, so renders
     * match the actual display (HiDPI / scaled / secondary monitors). Falls back
     * to the primary screen, then 1.0, before the node is in a window.
     *
     * @return the output (render) scale
     */
    private double outputScale() {
        if (getScene() != null && getScene().getWindow() != null) {
            double s = getScene().getWindow().getOutputScaleX();
            if (s > 0) {
                return s;
            }
        }
        return Screen.getPrimary().getOutputScaleX();
    }

    /** Listens to the given window's device scale (replacing any previous one). */
    private void attachWindowListener(Window window) {
        if (window == listenedWindow) {
            return;
        }
        detachWindowListener();
        listenedWindow = window;
        if (window != null) {
            window.outputScaleXProperty().addListener(outputScaleListener);
        }
    }

    /** Stops listening to the previously tracked window's device scale. */
    private void detachWindowListener() {
        if (listenedWindow != null) {
            listenedWindow.outputScaleXProperty().removeListener(outputScaleListener);
            listenedWindow = null;
        }
    }

    /** Quarter-turn count (0..3) for a rotation in degrees. */
    private static int quarterTurns(double degrees) {
        int q = (int) Math.round(degrees / 90.0);
        return ((q % 4) + 4) % 4;
    }

    /* ------------------------------------------------------------- layout */

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        renderLayer.resizeRelocate(0, 0, w, h);
        textLayer.resizeRelocate(0, 0, w, h);
        for (PdfLayer layer : layers) {
            layer.resizeRelocate(0, 0, w, h);
        }
        loader.resizeRelocate(0, 0, w, h);
    }

    /** Shows the overlay and starts the loader animation. */
    private void showLoader() {
        loader.setVisible(true);
        spinner.setAutoStart(true);
    }

    /** Stops the loader animation and hides the overlay. */
    private void hideLoader() {
        spinner.setAutoStart(false);
        loader.setVisible(false);
    }

    /** Builds the centered, mouse-transparent loading overlay around the given loader. */
    private static StackPane buildLoader(NfxCircularLoader spinner) {
        // Idle until a slow render reveals it (no animation while hidden).
        spinner.setAutoStart(false);
        StackPane pane = new StackPane(spinner);
        pane.getStyleClass().add("pdf-loader");
        pane.setVisible(false);
        pane.setManaged(false);
        pane.setMouseTransparent(true);
        return pane;
    }

    /* ---------------------------------------------------------------- CSS */

    private static final class StyleableProperties {
        private static final CssMetaData<PdfPageView, Color> PAGE_BACKGROUND =
                new CssMetaData<>("-fx-page-background", StyleConverter.getColorConverter(), Color.WHITE) {
                    @Override
                    public boolean isSettable(PdfPageView n) {
                        return !n.pageBackground.isBound();
                    }

                    @Override
                    public StyleableProperty<Color> getStyleableProperty(PdfPageView n) {
                        return n.pageBackground;
                    }
                };

        private static final CssMetaData<PdfPageView, Color> SELECTION_COLOR =
                new CssMetaData<>("-fx-selection-color", StyleConverter.getColorConverter(), Color.web("#3399ff", 0.40)) {
                    @Override
                    public boolean isSettable(PdfPageView n) {
                        return !n.selectionColor.isBound();
                    }

                    @Override
                    public StyleableProperty<Color> getStyleableProperty(PdfPageView n) {
                        return n.selectionColor;
                    }
                };

        private static final CssMetaData<PdfPageView, Color> HIGHLIGHT_COLOR =
                new CssMetaData<>("-fx-highlight-color", StyleConverter.getColorConverter(), Color.web("#ffd200", 0.50)) {
                    @Override
                    public boolean isSettable(PdfPageView n) {
                        return !n.highlightColor.isBound();
                    }

                    @Override
                    public StyleableProperty<Color> getStyleableProperty(PdfPageView n) {
                        return n.highlightColor;
                    }
                };

        private static final List<CssMetaData<? extends Styleable, ?>> CSS;

        static {
            List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(Region.getClassCssMetaData());
            Collections.addAll(list, PAGE_BACKGROUND, SELECTION_COLOR, HIGHLIGHT_COLOR);
            CSS = Collections.unmodifiableList(list);
        }
    }

    /**
     * The CSS metadata for this class (includes Region's plus the page colors).
     *
     * @return the class CSS metadata
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.CSS;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }
}
