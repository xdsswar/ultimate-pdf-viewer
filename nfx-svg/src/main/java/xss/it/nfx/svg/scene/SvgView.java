package xss.it.nfx.svg.scene;

import com.xss.it.nfx.svg.SvgDocumentImpl;
import com.xss.it.nfx.svg.SvgImages;
import com.xss.it.nfx.svg.scene.RenderLayer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleConverter;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;
import xss.it.nfx.svg.SvgDocument;
import xss.it.nfx.svg.SvgException;
import xss.it.nfx.svg.SvgFillMode;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A reactive JavaFX node that displays a single SVG document - like an
 * {@link javafx.scene.image.ImageView}, but resolution-independent.
 *
 * <p>Where {@code ImageView} scales a fixed raster (and softens when enlarged),
 * {@code SvgView} re-rasterizes the vector geometry fresh at the exact
 * device-pixel size it is shown at, so it is <b>pixel-perfect and crisp at any
 * size</b>. It is fully DPI-aware: the effective render resolution is
 * {@code displaySize * window.outputScaleX}, snapped to the device-pixel grid,
 * and it re-renders automatically when the window's scale changes (HiDPI / moved
 * between monitors).</p>
 *
 * <p>Size it the same way you size an {@code ImageView}:</p>
 * <ul>
 *   <li>{@link #fitWidthProperty() fitWidth} / {@link #fitHeightProperty()
 *       fitHeight} - fit the SVG into a box ({@link #preserveRatioProperty()
 *       preserveRatio} on by default);</li>
 *   <li>{@link #zoomProperty() zoom} - a scale multiplier on top of the base
 *       size (default the SVG's intrinsic size);</li>
 *   <li>neither set - the SVG's intrinsic size.</li>
 * </ul>
 *
 * <p>Rendering happens off the FX thread, is debounced, drops superseded frames,
 * and caches the current bitmap so identical sizes never re-render - so dragging
 * a zoom slider stays smooth.</p>
 *
 * <p>CSS-styleable colors: {@code -svg-background} (fill behind the SVG, default
 * transparent) and {@code -svg-fill} (an optional tint that recolors the whole
 * SVG; default unset, so the SVG keeps its own colors).</p>
 *
 * @author XDSSWAR
 */
public class SvgView extends Region {

    private static final String STYLE_CLASS = "svg-view";

    /**
     * Fallback display size (logical px) used when no document is loaded yet, so
     * the node has a visible footprint in layouts and in SceneBuilder instead of
     * collapsing to 0 x 0. Once a document is set the SVG's own size takes over.
     */
    private static final double DEFAULT_SIZE = 100.0;

    /** Debounce window before a crisp re-render after a property change. */
    private static final Duration DEBOUNCE = Duration.millis(140);

    /**
     * Largest bitmap dimension (pixels) rendered. Kept below the GPU texture
     * limit (16384) so the Prism pipeline never gets handed a null texture. The
     * SVG only softens slightly beyond this size instead of crashing; tiled
     * rendering past this limit is a future enhancement.
     */
    private static final double MAX_TEXTURE_PX = 8192.0;

    /** Shared daemon pool used for background rendering by default. */
    private static final Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "nfx-svg-render");
        t.setDaemon(true);
        return t;
    });

    // ------------------------------------------------------------ children

    private final RenderLayer renderLayer = new RenderLayer();
    /** Fade played when the bitmap first appears. */
    private final FadeTransition imageFade = new FadeTransition(Duration.millis(120), renderLayer);

    // -------------------------------------------------------------- state

    private final PauseTransition debounce = new PauseTransition(DEBOUNCE);
    private final AtomicLong renderSeq = new AtomicLong();
    /** Whether a rendered bitmap is currently shown. */
    private boolean hasImage;
    /** Device-pixel size + background of the currently shown bitmap (render cache). */
    private int lastWpx = -1;
    private int lastHpx = -1;
    private Color lastBg;
    private int lastTintArgb;
    private int lastTintMode = -1;

    /** Re-renders when the host window's device scale changes. */
    private final ChangeListener<Number> outputScaleListener = (s, a, b) -> requestRender();
    /** The window we currently listen to (so the listener can be removed on detach). */
    private Window listenedWindow;
    /** The scene we currently listen to for its window appearing/changing. */
    private Scene listenedScene;
    /**
     * Re-attaches to the window once the scene actually gets one (a scene is
     * often set before it is shown in a window) and re-renders at the correct
     * device scale. Also detaches if the window goes away.
     */
    private final ChangeListener<Window> sceneWindowListener = (o, oldWin, newWin) -> {
        attachWindowListener(newWin);
        if (newWin != null) {
            requestRender();
        }
    };

    /**
     * Creates an empty view. Assign a {@link #documentProperty() document} to
     * show content.
     */
    public SvgView() {
        getStyleClass().add(STYLE_CLASS);
        getChildren().add(renderLayer);

        debounce.setOnFinished(e -> doRender());

        documentProperty().addListener((o, a, b) -> onDocumentChanged());
        // A location string (path / URL / classpath) loads the document - makes
        // the view usable from FXML and visible in SceneBuilder.
        locationProperty().addListener((o, a, b) -> onLocationChanged(b));
        // Geometry changes: relayout now (instant stretch), re-render crisp soon.
        ChangeListener<Object> geometry = (o, a, b) -> onGeometryChanged();
        zoomProperty().addListener(geometry);
        fitWidthProperty().addListener(geometry);
        fitHeightProperty().addListener(geometry);
        preserveRatioProperty().addListener(geometry);

        /*
         * The actual laid-out size is what we rasterize to, so re-render (crisply)
         * whenever the node is resized. This is what lets the SVG adapt to - and
         * fill - whatever size a parent gives it, instead of being a fixed-size
         * bitmap that the layout would stretch and blur. layoutChildren keeps the
         * previous frame stretched to the new size until the crisp render lands.
         */
        widthProperty().addListener((o, a, b) -> debounce.playFromStart());
        heightProperty().addListener((o, a, b) -> debounce.playFromStart());

        // Re-render once attached to a window (correct output scale) and whenever
        // the window's device scale changes. A scene may exist before it has a
        // window (set, then shown later), so also listen for the scene's window
        // appearing. When detached, stop timers/animation so the node can be
        // garbage-collected.
        sceneProperty().addListener((o, oldScene, scene) -> {
            if (listenedScene != null) {
                listenedScene.windowProperty().removeListener(sceneWindowListener);
                listenedScene = null;
            }
            if (scene == null) {
                debounce.stop();
                imageFade.stop();
                renderLayer.setOpacity(1.0);
                detachWindowListener();
                return;
            }
            listenedScene = scene;
            scene.windowProperty().addListener(sceneWindowListener);
            attachWindowListener(scene.getWindow());   // may be null until shown
            requestRender();
        });

        onDocumentChanged();
    }

    /**
     * Creates a view showing the given document.
     *
     * @param document the document to display
     */
    public SvgView(SvgDocument document) {
        this();
        setDocument(document);
    }

    /**
     * Creates a view showing the SVG at the given location - a file path, a URL,
     * or a classpath resource - resolved as described by {@link #locationProperty()}.
     *
     * @param location the SVG location
     */
    public SvgView(String location) {
        this();
        setLocation(location);
    }

    // --------------------------------------------------------- properties

    private final ObjectProperty<SvgDocument> document = new SimpleObjectProperty<>(this, "document");

    /** The document to display. */
    public final ObjectProperty<SvgDocument> documentProperty() {
        return document;
    }

    public final SvgDocument getDocument() {
        return document.get();
    }

    public final void setDocument(SvgDocument value) {
        document.set(value);
    }

    private final StringProperty location = new SimpleStringProperty(this, "location");

    /**
     * A location string to load the SVG from - a file path, a URL (file, http,
     * https, jar, ...), or a classpath resource (a leading {@code /} is optional).
     * Setting it loads the document and assigns {@link #documentProperty()};
     * setting it blank or {@code null} clears the document. Exposed as a String
     * property so the view is usable from FXML and visible in SceneBuilder.
     *
     * @return the location property
     */
    public final StringProperty locationProperty() {
        return location;
    }

    /**
     * The current SVG location string.
     *
     * @return the location, or {@code null} if none
     */
    public final String getLocation() {
        return location.get();
    }

    /**
     * Sets the SVG location string (path, URL, or classpath resource).
     *
     * @param value the location, or {@code null}/blank to clear
     */
    public final void setLocation(String value) {
        location.set(value);
    }

    private final DoubleProperty zoom = new SimpleDoubleProperty(this, "zoom", 1.0);

    /** Display zoom factor ({@code 1.0} = the base display size). */
    public final DoubleProperty zoomProperty() {
        return zoom;
    }

    public final double getZoom() {
        return zoom.get();
    }

    public final void setZoom(double value) {
        zoom.set(value);
    }

    private final DoubleProperty fitWidth = new SimpleDoubleProperty(this, "fitWidth", 0);

    /**
     * Target width to fit the SVG into, in logical px (like
     * {@link javafx.scene.image.ImageView#fitWidthProperty()}). {@code 0}
     * (default) means unset. Combined with {@link #preserveRatioProperty()}.
     *
     * @return the fit-width property
     */
    public final DoubleProperty fitWidthProperty() {
        return fitWidth;
    }

    public final double getFitWidth() {
        return fitWidth.get();
    }

    public final void setFitWidth(double value) {
        fitWidth.set(value);
    }

    private final DoubleProperty fitHeight = new SimpleDoubleProperty(this, "fitHeight", 0);

    /**
     * Target height to fit the SVG into, in logical px (like
     * {@link javafx.scene.image.ImageView#fitHeightProperty()}). {@code 0}
     * (default) means unset.
     *
     * @return the fit-height property
     */
    public final DoubleProperty fitHeightProperty() {
        return fitHeight;
    }

    public final double getFitHeight() {
        return fitHeight.get();
    }

    public final void setFitHeight(double value) {
        fitHeight.set(value);
    }

    private final BooleanProperty preserveRatio = new SimpleBooleanProperty(this, "preserveRatio", true);

    /**
     * Whether to preserve the SVG's aspect ratio when fitting to
     * {@link #fitWidthProperty()}/{@link #fitHeightProperty()} (default
     * {@code true}). When {@code false} the SVG stretches to fill both - still
     * crisp, since the stretch is applied to the vectors, not a bitmap.
     *
     * @return the preserve-ratio property
     */
    public final BooleanProperty preserveRatioProperty() {
        return preserveRatio;
    }

    public final boolean isPreserveRatio() {
        return preserveRatio.get();
    }

    public final void setPreserveRatio(boolean value) {
        preserveRatio.set(value);
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

    // ------------------------------------------------------ styleable color

    private final ObjectProperty<Color> background =
            new SimpleStyleableObjectProperty<>(StyleableProperties.BACKGROUND, this, "background", Color.TRANSPARENT) {
                @Override
                protected void invalidated() {
                    requestRender();
                }
            };

    /** Background fill behind the SVG ({@code -svg-background}; default transparent). */
    public final ObjectProperty<Color> backgroundFillProperty() {
        return background;
    }

    public final Color getBackgroundFill() {
        return background.get();
    }

    public final void setBackgroundFill(Color value) {
        background.set(value);
    }

    private final ObjectProperty<Color> fill =
            new SimpleStyleableObjectProperty<>(StyleableProperties.FILL, this, "fill") {
                @Override
                protected void invalidated() {
                    requestRender();
                }
            };

    /**
     * An optional override color for the whole SVG ({@code -svg-fill}).
     *
     * <p>By default this is {@code null}, so the SVG draws with its own authored
     * colors. Set it to recolor the entire graphic to a single color while keeping
     * its shape (alpha) - ideal for tinting monochrome icons. Because it is a flat
     * recolor it replaces all internal colors, so it suits single-color art rather
     * than multi-color illustrations. Clearing it (back to {@code null}) restores
     * the original colors.</p>
     *
     * @return the fill (tint) property
     */
    public final ObjectProperty<Color> fillProperty() {
        return fill;
    }

    public final Color getFill() {
        return fill.get();
    }

    public final void setFill(Color value) {
        fill.set(value);
    }

    private final ObjectProperty<SvgFillMode> fillMode =
            new SimpleStyleableObjectProperty<>(StyleableProperties.FILL_MODE, this, "fillMode", SvgFillMode.NONE) {
                @Override
                protected void invalidated() {
                    requestRender();
                }
            };

    /**
     * The blend mode used to apply the {@link #fillProperty() fill} tint over the
     * SVG ({@code -svg-fill-mode}; default {@link SvgFillMode#NONE}).
     *
     * <p>{@link SvgFillMode#NONE} (the default) leaves the SVG's own colors
     * untouched even if a {@code fill} is set. With any other mode - while
     * {@code fill} is set - the tint is composited in the native Skia render and
     * masked to the SVG's own shape, so it never affects the transparent area
     * around the graphic. {@code SRC_OVER} (with an opaque color) is a flat
     * recolor; {@code MULTIPLY}, {@code SCREEN}, {@code OVERLAY}, etc. blend with
     * the original colors for shaded tints.</p>
     *
     * @return the fill blend-mode property
     */
    public final ObjectProperty<SvgFillMode> fillModeProperty() {
        return fillMode;
    }

    public final SvgFillMode getFillMode() {
        return fillMode.get();
    }

    public final void setFillMode(SvgFillMode value) {
        fillMode.set(value);
    }

    // ----------------------------------------------------------- rendering

    /** Forces an immediate (non-debounced) re-render of the current document. */
    public final void requestRender() {
        debounce.stop();
        doRender();
    }

    /**
     * Ensures this view has a rendered bitmap: triggers a render only if one
     * isn't already shown. Safe to call repeatedly.
     */
    public final void ensureRendered() {
        if (currentDoc() != null && !hasImage) {
            requestRender();
        }
    }

    private void onDocumentChanged() {
        // The document changed: drop the stale bitmap and render fresh.
        hasImage = false;
        lastWpx = lastHpx = -1;
        renderLayer.setImage(null);
        recompute();
        requestRender();
    }

    /**
     * Loads (or clears) the document when {@link #locationProperty()} changes.
     *
     * @param loc the new location string, or {@code null}/blank to clear
     */
    private void onLocationChanged(String loc) {
        if (loc == null || loc.isBlank()) {
            setDocument(null);
            return;
        }
        setDocument(loadLocation(loc));
    }

    /**
     * Resolves a location string to a document, trying (in order): an existing
     * file path, then a URL of any scheme, then a classpath resource.
     *
     * @param location the location string
     * @return the loaded document
     * @throws SvgException if the location cannot be resolved or read
     */
    private static SvgDocument loadLocation(String location) {
        try {
            Path p = Path.of(location);
            if (Files.isRegularFile(p)) {
                return SvgDocument.load(p);
            }
        } catch (InvalidPathException ignore) {
            // Not a filesystem path (e.g. a URL string); fall through.
        }
        try {
            URI uri = URI.create(location);
            if (uri.getScheme() != null) {
                return SvgDocument.load(uri.toURL());
            }
        } catch (IllegalArgumentException | MalformedURLException ignore) {
            // Not a URL; fall through to the classpath.
        }
        URL res = SvgView.class.getResource(location.startsWith("/") ? location : "/" + location);
        if (res != null) {
            return SvgDocument.load(res);
        }
        throw new SvgException("SVG location not found: " + location);
    }

    private void onGeometryChanged() {
        recompute();                // instant: stretch existing bitmap to the new size
        debounce.playFromStart();   // crisp re-render once changes settle
    }

    /** Base (un-zoomed) display size in logical px, honoring fitWidth/fitHeight. */
    private double[] baseDisplaySize(SvgDocument doc) {
        double bw = doc.getWidth();
        double bh = doc.getHeight();
        double fw = getFitWidth();
        double fh = getFitHeight();
        if (fw <= 0 && fh <= 0) {
            return new double[]{bw, bh};
        }
        if (isPreserveRatio()) {
            double availW = fw > 0 ? fw : Double.MAX_VALUE;
            double availH = fh > 0 ? fh : Double.MAX_VALUE;
            double s = Math.min(availW / bw, availH / bh);
            return new double[]{bw * s, bh * s};
        }
        return new double[]{fw > 0 ? fw : bw, fh > 0 ? fh : bh};
    }

    /**
     * The node's actual content size in logical px. Before the first layout pass
     * (size still 0) it falls back to the preferred size (fit/zoom) so the first
     * paint is already correct.
     *
     * @param doc the current document
     * @return {@code [width, height]} in logical px
     */
    private double[] contentSize(SvgDocument doc) {
        double w = getWidth();
        double h = getHeight();
        if (w > 0 && h > 0) {
            return new double[]{w, h};
        }
        double[] base = baseDisplaySize(doc);
        double z = getZoom();
        return new double[]{base[0] * z, base[1] * z};
    }

    /**
     * The size the SVG is actually drawn at inside a {@code boxW x boxH} content
     * box: fitted (preserving aspect, centered/letterboxed) by default, or
     * stretched to fill the box when {@link #preserveRatioProperty()} is false.
     *
     * @param doc  the current document (for its intrinsic aspect)
     * @param boxW the content box width in logical px
     * @param boxH the content box height in logical px
     * @return {@code [width, height]} the SVG is drawn at, in logical px
     */
    private double[] fittedRect(SvgDocument doc, double boxW, double boxH) {
        if (boxW <= 0 || boxH <= 0) {
            return new double[]{0, 0};
        }
        if (!isPreserveRatio()) {
            return new double[]{boxW, boxH};
        }
        double iw = doc.getWidth();
        double ih = doc.getHeight();
        double s = Math.min(boxW / iw, boxH / ih);
        return new double[]{iw * s, ih * s};
    }

    /** Updates the preferred size from the document, fit and zoom. */
    private void recompute() {
        // No document yet: keep a default footprint so the node is visible in
        // layouts and SceneBuilder instead of collapsing to 0 x 0.
        double w = DEFAULT_SIZE, h = DEFAULT_SIZE;
        SvgDocument doc = currentDoc();
        if (doc != null) {
            double[] base = baseDisplaySize(doc);
            double z = getZoom();
            w = snapToDevice(base[0] * z);
            h = snapToDevice(base[1] * z);
        }
        setPrefSize(w, h);
        requestLayout();
    }

    /**
     * Snaps a logical length so it lands on a whole number of device pixels, so
     * the bitmap (rendered at exactly that many device pixels) fills the node
     * 1:1 - crisp, no soft scaling.
     *
     * @param logical a logical length
     * @return the device-aligned logical length
     */
    public double snapToDevice(double logical) {
        double s = outputScale();
        return Math.round(logical * s) / s;
    }

    private void doRender() {
        SvgDocument doc = currentDoc();
        if (!(doc instanceof SvgDocumentImpl impl)) {
            hasImage = false;
            lastWpx = lastHpx = -1;
            renderLayer.setImage(null);
            return;
        }
        double os = outputScale();
        /*
         * Render to the SVG's fitted size inside the node's ACTUAL content box, at
         * the window's device scale: DPI-aware and pixel-exact at whatever size the
         * node currently is. This is the core of "adapt to the desired size" - the
         * bitmap always matches the area it fills, so it never has to be upscaled.
         */
        double[] box = contentSize(doc);
        double[] fit = fittedRect(doc, box[0], box[1]);
        double dpw = fit[0] * os;
        double dph = fit[1] * os;
        // Clamp proportionally so neither dimension exceeds the texture limit.
        double maxDim = Math.max(dpw, dph);
        if (maxDim > MAX_TEXTURE_PX) {
            double f = MAX_TEXTURE_PX / maxDim;
            dpw *= f;
            dph *= f;
        }
        int wpx = (int) Math.round(dpw);
        int hpx = (int) Math.round(dph);
        if (wpx <= 0 || hpx <= 0) {
            hasImage = false;
            lastWpx = lastHpx = -1;
            renderLayer.setImage(null);
            return;
        }
        Color bg = getBackgroundFill();
        // The tint runs only when a fill is set AND the mode isn't NONE; otherwise
        // tintMode < 0 tells the native renderer to leave the SVG's colors alone.
        Color f = getFill();
        SvgFillMode mode = getFillMode();
        boolean tinted = f != null && f.getOpacity() > 0.0 && mode != null && mode != SvgFillMode.NONE;
        int tintArgb = tinted ? SvgImages.argb(f) : 0;
        int tintMode = tinted ? mode.code() : -1;
        // Cache: skip if the bitmap already matches this size + background + tint.
        if (hasImage && wpx == lastWpx && hpx == lastHpx && Objects.equals(bg, lastBg)
                && tintArgb == lastTintArgb && tintMode == lastTintMode) {
            return;
        }

        final boolean fadeIn = !hasImage;
        final int argbBg = SvgImages.argb(bg);
        final int fTintArgb = tintArgb;
        final int fTintMode = tintMode;
        final int fw = wpx, fh = hpx;
        long seq = renderSeq.incrementAndGet();
        getRenderExecutor().execute(() -> {
            try {
                Image image = SvgImages.render(impl.native_(), fw, fh, argbBg, fTintArgb, fTintMode);
                Platform.runLater(() -> {
                    if (seq == renderSeq.get()) {
                        renderLayer.setImage(image); // fills the fitted rect 1:1 (crisp)
                        hasImage = image != null;
                        lastWpx = fw;
                        lastHpx = fh;
                        lastBg = bg;
                        lastTintArgb = fTintArgb;
                        lastTintMode = fTintMode;
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

    /** The current document if it is usable (non-null and open), else {@code null}. */
    private SvgDocument currentDoc() {
        SvgDocument doc = getDocument();
        return (doc != null && !doc.isClosed()) ? doc : null;
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

    // ------------------------------------------------------------- layout

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        SvgDocument doc = currentDoc();
        if (doc == null) {
            renderLayer.resizeRelocate(0, 0, w, h);
            return;
        }
        /*
         * Place the bitmap at its fitted size, centered. The ImageView fills this
         * rect, so the bitmap (rendered at exactly this many device pixels) maps
         * 1:1 = crisp. On a resize this runs immediately and stretches the previous
         * frame to the new rect, giving instant feedback before the crisp render.
         * When the node's aspect differs from the SVG's, the surrounding margin is
         * left transparent (preserveRatio letterboxing).
         */
        double[] fit = fittedRect(doc, w, h);
        double rw = fit[0];
        double rh = fit[1];
        renderLayer.resizeRelocate((w - rw) / 2.0, (h - rh) / 2.0, rw, rh);
    }

    // ---------------------------------------------------------------- CSS

    private static final class StyleableProperties {
        private static final CssMetaData<SvgView, Color> BACKGROUND =
                new CssMetaData<>("-svg-background", StyleConverter.getColorConverter(), Color.TRANSPARENT) {
                    @Override
                    public boolean isSettable(SvgView n) {
                        return !n.background.isBound();
                    }

                    @SuppressWarnings("all")
                    @Override
                    public StyleableProperty<Color> getStyleableProperty(SvgView n) {
                        return (StyleableProperty<Color>) n.background;
                    }
                };

        private static final CssMetaData<SvgView, Color> FILL =
                new CssMetaData<>("-svg-fill", StyleConverter.getColorConverter(), null) {
                    @Override
                    public boolean isSettable(SvgView n) {
                        return !n.fill.isBound();
                    }

                    @SuppressWarnings("all")
                    @Override
                    public StyleableProperty<Color> getStyleableProperty(SvgView n) {
                        return (StyleableProperty<Color>) n.fill;
                    }
                };

        private static final CssMetaData<SvgView, SvgFillMode> FILL_MODE =
                new CssMetaData<>("-svg-fill-mode",
                        StyleConverter.getEnumConverter(SvgFillMode.class), SvgFillMode.NONE) {
                    @Override
                    public boolean isSettable(SvgView n) {
                        return !n.fillMode.isBound();
                    }

                    @SuppressWarnings("all")
                    @Override
                    public StyleableProperty<SvgFillMode> getStyleableProperty(SvgView n) {
                        return (StyleableProperty<SvgFillMode>) n.fillMode;
                    }
                };

        private static final List<CssMetaData<? extends Styleable, ?>> CSS;

        static {
            List<CssMetaData<? extends Styleable, ?>> list = new ArrayList<>(Region.getClassCssMetaData());
            Collections.addAll(list, BACKGROUND, FILL, FILL_MODE);
            CSS = Collections.unmodifiableList(list);
        }
    }

    /**
     * The CSS metadata for this class (includes Region's plus the background and
     * fill colors).
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
