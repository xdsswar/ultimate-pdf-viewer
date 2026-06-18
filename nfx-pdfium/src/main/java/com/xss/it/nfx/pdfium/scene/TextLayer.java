package com.xss.it.nfx.pdfium.scene;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import xss.it.nfx.pdfium.PdfPage;
import xss.it.nfx.pdfium.scene.PdfLayer;
import xss.it.nfx.pdfium.scene.PdfPageView;
import xss.it.nfx.pdfium.scene.PdfSelectionModel;
import xss.it.nfx.pdfium.text.PdfSearchResult;
import xss.it.nfx.pdfium.text.PdfTextChar;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal layer that draws search highlights and supports text selection,
 * copy and select-all on top of the rendered page.
 *
 * <p>Selection state lives in the host's shared {@link PdfSelectionModel}, so a
 * selection can span pages: this layer reports its page-local char index to the
 * model and renders whatever slice of the (possibly multi-page) selection falls
 * on its page. Geometry comes from {@link PdfPage#getChars()} (top-left point
 * coordinates) scaled by the display scale. Applies to unrotated pages; under
 * rotation the overlay is suppressed (the bitmap still rotates).</p>
 *
 * @author XDSSWAR
 */
public final class TextLayer extends PdfLayer {

    /**
     * Hard cap on the overlay canvas size (pixels). A Canvas allocates a
     * render-to-texture buffer of its full size; keeping it small (and only as
     * big as the drawn region) avoids exceeding the GPU texture limit / video
     * memory at high zoom, which otherwise yields a null RTTexture and crashes.
     */
    private static final double MAX_CANVAS_PX = 4096.0;

    /**
     * Extra padding (in page points) added on each side of every search-highlight
     * rect so the fill sits a little proud of the glyphs rather than tight against
     * them. Applied in point space, so it scales naturally with zoom. Selection
     * rects are left tight (they read better edge-to-edge).
     */
    private static final double HIGHLIGHT_PADDING = 1.5;

    private final PdfPageView view;
    private final Canvas canvas = new Canvas();

    private PdfPage page;
    /** Page text, loaded lazily on first hit-test/selection (null = not loaded). */
    private List<PdfTextChar> chars;
    private double displayScale = 1.0;
    private double rotation;

    /**
     * Pending selection anchor from the last primary press (char index, or -1).
     * Selection only begins once the pointer actually drags, so a plain click
     * doesn't select a character — see {@link #onPressed}/{@link #onDragged}.
     */
    private int pressChar = -1;
    /** Whether a drag selection has begun since the last press. */
    private boolean dragSelecting;

    /**
     * Drives the "flash zoom" emphasis on the active match: 1 at the start of the
     * flash (rect scaled up + bright overlay), easing back to 0 (settled). Each
     * frame triggers a redraw so the pulse animates.
     */
    private final DoubleProperty pulse = new SimpleDoubleProperty(0);

    /** Plays the flash whenever the active match changes to one on this page. */
    private final Timeline flash = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(pulse, 1.0, Interpolator.EASE_OUT)),
            new KeyFrame(Duration.millis(450), new KeyValue(pulse, 0.0, Interpolator.EASE_OUT)));

    /** Redraws whenever the shared selection changes. */
    private final InvalidationListener selectionListener = o -> redraw();

    /**
     * Creates a text layer bound to its host view (for colors, highlights and the
     * shared selection model).
     *
     * @param view the owning page view
     */
    public TextLayer(PdfPageView view) {
        this.view = view;
        getStyleClass().add("pdf-text-layer");
        canvas.setManaged(false);
        getChildren().add(canvas);
        setPickOnBounds(true);     // capture drags for selection
        setFocusTraversable(true);

        setOnMousePressed(this::onPressed);
        setOnMouseDragged(this::onDragged);
        setOnKeyPressed(e -> {
            if (e.isShortcutDown() && e.getCode() == KeyCode.C) {
                view.copySelection();
                e.consume();
            } else if (e.isShortcutDown() && e.getCode() == KeyCode.A) {
                view.selectAll();
                e.consume();
            }
        });

        view.selectionColorProperty().addListener(o -> redraw());
        view.highlightColorProperty().addListener(o -> redraw());
        view.activeHighlightColorProperty().addListener(o -> redraw());
        view.getHighlights().addListener((ListChangeListener<PdfSearchResult>) c -> redraw());
        pulse.addListener(o -> redraw());
        // Flash-zoom the active match whenever it becomes (a new) one on this page.
        view.activeHighlightProperty().addListener((o, a, b) -> {
            if (b != null && b.pageIndex() == view.getPageIndex()) {
                flash.playFromStart();
            } else {
                flash.stop();
                pulse.set(0);
            }
            redraw();
        });

        // I-beam cursor only when actual text is under the pointer (and selectable);
        // otherwise the cursor is cleared so it inherits (e.g. the pan cursor).
        setOnMouseMoved(this::onMoved);
        view.textSelectableProperty().addListener((o, a, b) -> {
            if (!view.isTextSelectable()) {
                setCursor(null);
            }
        });

        // Track the (possibly swapped) shared selection model.
        if (view.getSelectionModel() != null) {
            view.getSelectionModel().revisionProperty().addListener(selectionListener);
        }
        view.selectionModelProperty().addListener((o, oldModel, newModel) -> {
            if (oldModel != null) {
                oldModel.revisionProperty().removeListener(selectionListener);
            }
            if (newModel != null) {
                newModel.revisionProperty().addListener(selectionListener);
            }
            redraw();
        });
    }

    @Override
    protected void onPageUpdated(PdfPage page, double displayScale, double rotation) {
        boolean pageChanged = this.page != page;
        this.page = page;
        this.displayScale = displayScale;
        this.rotation = rotation;
        if (pageChanged) {
            // Don't extract text here — that native loop is what stalls scrolling.
            // It loads lazily via chars() only when this page is hovered/selected.
            chars = null;
        }
        redraw();
    }

    /**
     * The page's text characters, extracted lazily on first use (hit-testing or
     * drawing a selection) and cached. Plain scrolling never triggers it.
     *
     * @return the characters (empty if there is no page)
     */
    private List<PdfTextChar> chars() {
        if (chars == null) {
            chars = (page == null) ? List.of() : page.getChars();
        }
        return chars;
    }

    @Override
    protected void layoutChildren() {
        // The canvas is sized on demand in redraw() (only as large as the drawn
        // region), so a full-page-sized backing texture is never allocated.
        redraw();
    }

    /* --------------------------------------------------------------- input */

    private void onPressed(MouseEvent e) {
        if (!view.isTextSelectable()) {
            return;
        }
        // Primary only; Ctrl+drag and right/middle click pass through (pan / menu).
        if (e.getButton() != MouseButton.PRIMARY || e.isControlDown()) {
            return;
        }
        PdfSelectionModel model = view.getSelectionModel();
        if (model == null) {
            return;
        }
        int index = charAt(e.getX(), e.getY());
        focusWithoutScrolling();
        dragSelecting = false;
        if (e.isShiftDown()) {
            // Shift-click extends the existing selection to the clicked glyph.
            if (index >= 0) {
                model.extendTo(view.getPageIndex(), index);
            }
            pressChar = -1;
        } else {
            // A plain press only clears any selection and remembers where a drag
            // would anchor — it does NOT begin a (one-character) selection, so a
            // click without dragging selects nothing. The drag begins it.
            model.clear();
            pressChar = index;
        }
        e.consume();
    }

    /**
     * Focuses this layer (so Ctrl+C / Ctrl+A reach it) without letting an
     * enclosing {@link ScrollPane} auto-scroll the newly-focused, oversized page
     * node into view — which would shift the content out from under the pointer
     * mid-selection. The affected axis depends on the fit: fit-to-width makes the
     * page taller than the viewport, so it snaps to the <em>top</em>; fit-to-height
     * makes it wider, so it snaps to the <em>left</em>.
     *
     * <p>The ScrollPane performs that scroll-into-view either synchronously on
     * focus or during the layout pass that follows it, so a single restore can
     * lose the race (this is why fit-to-height kept jumping after the first fix).
     * We therefore snapshot the scroll position of <em>every</em> enclosing
     * ScrollPane and re-assert it immediately and across the next two pulses,
     * which covers both the synchronous and the deferred (layout-time) cases on
     * both axes, for the single-page and continuous views alike.</p>
     */
    private void focusWithoutScrolling() {
        List<ScrollPane> panes = enclosingScrollPanes();
        if (panes.isEmpty()) {
            requestFocus();
            return;
        }
        int n = panes.size();
        double[] hs = new double[n];
        double[] vs = new double[n];
        for (int i = 0; i < n; i++) {
            hs[i] = panes.get(i).getHvalue();
            vs[i] = panes.get(i).getVvalue();
        }
        Runnable restore = () -> {
            for (int i = 0; i < n; i++) {
                ScrollPane sp = panes.get(i);
                sp.setHvalue(hs[i]);
                sp.setVvalue(vs[i]);
            }
        };

        requestFocus();
        restore.run();                                       // synchronous scroll-into-view
        Platform.runLater(() -> {                            // next pulse
            restore.run();
            Platform.runLater(restore);                      // and the pulse after the layout pass
        });
    }

    /** Every enclosing {@link ScrollPane}, nearest first (empty if none). */
    private List<ScrollPane> enclosingScrollPanes() {
        List<ScrollPane> result = new ArrayList<>(2);
        for (Parent p = getParent(); p != null; p = p.getParent()) {
            if (p instanceof ScrollPane sp) {
                result.add(sp);
            }
        }
        return result;
    }

    private void onDragged(MouseEvent e) {
        // Only the PRIMARY button drives selection — a drag event reports its held
        // button via isPrimaryButtonDown() (getButton() is NONE during a drag), so a
        // right/middle-button drag (menu / pan) must not extend the selection.
        if (!view.isTextSelectable() || e.isControlDown() || !e.isPrimaryButtonDown()) {
            return;
        }
        PdfSelectionModel model = view.getSelectionModel();
        if (model == null) {
            return;
        }
        int idx = charAt(e.getX(), e.getY());
        if (idx < 0) {
            e.consume();
            return;
        }
        if (!dragSelecting) {
            // First drag movement: now begin the selection at the press anchor
            // (or this point if the press wasn't on a glyph).
            model.begin(view.getPageIndex(), pressChar >= 0 ? pressChar : idx);
            dragSelecting = true;
        }
        model.extendTo(view.getPageIndex(), idx);
        e.consume();
    }

    /** Sets the I-beam cursor only when a glyph is under the pointer. */
    private void onMoved(MouseEvent e) {
        boolean overText = view.isTextSelectable() && isOverText(e.getX(), e.getY());
        setCursor(overText ? Cursor.TEXT : null);
    }

    /** Whether a glyph (with a small tolerance) lies under the local point. */
    private boolean isOverText(double localX, double localY) {
        if (displayScale <= 0 || page == null || Math.round(rotation) % 360 != 0) {
            return false;
        }
        List<PdfTextChar> cs = chars();
        if (cs.isEmpty()) {
            return false;
        }
        double px = localX / displayScale;
        double py = localY / displayScale;
        double pad = 1.0; // points of tolerance to bridge tiny inter-glyph gaps
        for (PdfTextChar c : cs) {
            Rectangle2D b = c.bounds();
            if (b.getWidth() <= 0 && b.getHeight() <= 0) {
                continue; // skip glyph-less chars (e.g. spaces)
            }
            if (px >= b.getMinX() - pad && px <= b.getMaxX() + pad
                    && py >= b.getMinY() - pad && py <= b.getMaxY() + pad) {
                return true;
            }
        }
        return false;
    }

    /** Index of the character at (or nearest to) a local point, or -1. */
    private int charAt(double localX, double localY) {
        if (displayScale <= 0) {
            return -1;
        }
        List<PdfTextChar> cs = chars();
        if (cs.isEmpty()) {
            return -1;
        }
        double px = localX / displayScale;
        double py = localY / displayScale;
        int nearest = -1;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < cs.size(); i++) {
            Rectangle2D b = cs.get(i).bounds();
            if (b.contains(px, py)) {
                return i;
            }
            double cx = b.getMinX() + b.getWidth() / 2;
            double cy = b.getMinY() + b.getHeight() / 2;
            double d = (cx - px) * (cx - px) + (cy - py) * (cy - py);
            if (d < best) {
                best = d;
                nearest = i;
            }
        }
        return nearest;
    }

    /* --------------------------------------------------------------- drawing */

    private void redraw() {
        if (page == null || Math.round(rotation) % 360 != 0) {
            shrinkCanvas(); // overlay geometry only valid for unrotated pages
            return;
        }

        // Collect everything to draw in canvas (display) space first, so the
        // canvas can be sized to just the drawn region (or zero when empty).
        // The active match is drawn separately (its own color + flash zoom), so
        // it is excluded from the regular highlights here.
        int pageIndex = view.getPageIndex();
        PdfSearchResult active = view.getActiveHighlight();
        Color hi = view.getHighlightColor();
        List<Rectangle2D> hiRects = new ArrayList<>();
        if (hi != null) {
            for (PdfSearchResult r : view.getHighlights()) {
                if (r.pageIndex() != pageIndex || r.equals(active)) {
                    continue;
                }
                for (Rectangle2D q : r.quads()) {
                    hiRects.add(scale(pad(q)));
                }
            }
        }

        // Active match rects (drawn on top, with a brightness flash). The rect
        // geometry stays CONSTANT during the flash (only its brightness pulses), so
        // the canvas never resizes/relocates mid-animation and can leave no residue.
        double p = pulse.get();
        List<Rectangle2D> activeRects = new ArrayList<>();
        if (active != null && active.pageIndex() == pageIndex) {
            for (Rectangle2D q : active.quads()) {
                activeRects.add(scale(pad(q)));
            }
        }

        Color sel = view.getSelectionColor();
        List<Rectangle2D> selRects = new ArrayList<>();
        PdfSelectionModel model = view.getSelectionModel();
        // Only load this page's text when the selection actually covers it.
        if (sel != null && model != null && model.coversPage(view.getPageIndex())) {
            List<PdfTextChar> cs = chars();
            int[] range = model.rangeOnPage(view.getPageIndex(), cs.size());
            if (range != null) {
                for (Rectangle2D q : selectionQuads(range[0], range[1])) {
                    selRects.add(scale(q));
                }
            }
        }

        // Nothing to show: release any backing texture (no RTTexture allocation).
        if (hiRects.isEmpty() && selRects.isEmpty() && activeRects.isEmpty()) {
            shrinkCanvas();
            return;
        }

        // Bounding box of everything, clamped to the node and to a GPU-safe size.
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = 0;
        double maxY = 0;
        for (Rectangle2D r : hiRects) {
            minX = Math.min(minX, r.getMinX());
            minY = Math.min(minY, r.getMinY());
            maxX = Math.max(maxX, r.getMaxX());
            maxY = Math.max(maxY, r.getMaxY());
        }
        for (Rectangle2D r : activeRects) {
            minX = Math.min(minX, r.getMinX());
            minY = Math.min(minY, r.getMinY());
            maxX = Math.max(maxX, r.getMaxX());
            maxY = Math.max(maxY, r.getMaxY());
        }
        for (Rectangle2D r : selRects) {
            minX = Math.min(minX, r.getMinX());
            minY = Math.min(minY, r.getMinY());
            maxX = Math.max(maxX, r.getMaxX());
            maxY = Math.max(maxY, r.getMaxY());
        }
        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        maxX = Math.min(getWidth(), maxX);
        maxY = Math.min(getHeight(), maxY);
        double cw = Math.min(MAX_CANVAS_PX, Math.max(0, maxX - minX));
        double ch = Math.min(MAX_CANVAS_PX, Math.max(0, maxY - minY));

        // Wipe the FULL current buffer before resizing/relocating: during the flash
        // animation the canvas shrinks frame to frame, and stale pixels outside the
        // new (smaller) region would otherwise linger as "traces" of the old frame.
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        canvas.setWidth(cw);
        canvas.setHeight(ch);
        canvas.relocate(minX, minY);

        // Draw offset by the canvas origin.
        g.clearRect(0, 0, cw, ch);
        if (!hiRects.isEmpty()) {
            g.setFill(hi);
            for (Rectangle2D r : hiRects) {
                g.fillRect(r.getMinX() - minX, r.getMinY() - minY, r.getWidth(), r.getHeight());
            }
        }
        if (!activeRects.isEmpty()) {
            Color activeColor = view.getActiveHighlightColor();
            if (activeColor != null) {
                g.setFill(activeColor);
                for (Rectangle2D r : activeRects) {
                    g.fillRect(r.getMinX() - minX, r.getMinY() - minY, r.getWidth(), r.getHeight());
                }
                // Bright flash overlay at the peak of the pulse, fading to nothing.
                if (p > 0) {
                    g.setGlobalAlpha(Math.min(1.0, p * 0.7));
                    g.setFill(activeColor.brighter().brighter());
                    for (Rectangle2D r : activeRects) {
                        g.fillRect(r.getMinX() - minX, r.getMinY() - minY, r.getWidth(), r.getHeight());
                    }
                    g.setGlobalAlpha(1.0);
                }
            }
        }
        if (!selRects.isEmpty()) {
            g.setFill(sel);
            for (Rectangle2D r : selRects) {
                g.fillRect(r.getMinX() - minX, r.getMinY() - minY, r.getWidth(), r.getHeight());
            }
        }
    }


    /** Releases the canvas backing texture when there is nothing to draw. */
    private void shrinkCanvas() {
        if (canvas.getWidth() != 0 || canvas.getHeight() != 0) {
            canvas.setWidth(0);
            canvas.setHeight(0);
        }
    }

    /** Expands a point-space rectangle by {@link #HIGHLIGHT_PADDING} on every side. */
    private static Rectangle2D pad(Rectangle2D r) {
        return new Rectangle2D(
                r.getMinX() - HIGHLIGHT_PADDING,
                r.getMinY() - HIGHLIGHT_PADDING,
                r.getWidth() + 2 * HIGHLIGHT_PADDING,
                r.getHeight() + 2 * HIGHLIGHT_PADDING);
    }

    /** Scales a point-space rectangle into canvas (display) space. */
    private Rectangle2D scale(Rectangle2D r) {
        return new Rectangle2D(r.getMinX() * displayScale, r.getMinY() * displayScale,
                r.getWidth() * displayScale, r.getHeight() * displayScale);
    }

    /** Builds per-line selection rectangles from the selected char range. */
    private List<Rectangle2D> selectionQuads(int from, int to) {
        List<PdfTextChar> cs = chars();
        List<Rectangle2D> quads = new ArrayList<>();
        Rectangle2D run = null;
        for (int i = from; i <= to && i < cs.size(); i++) {
            Rectangle2D b = cs.get(i).bounds();
            if (b.getWidth() <= 0 && b.getHeight() <= 0) {
                continue;
            }
            if (run == null) {
                run = b;
            } else if (sameLine(run, b)) {
                run = union(run, b);
            } else {
                quads.add(run);
                run = b;
            }
        }
        if (run != null) {
            quads.add(run);
        }
        return quads;
    }

    private static boolean sameLine(Rectangle2D a, Rectangle2D b) {
        double top = Math.max(a.getMinY(), b.getMinY());
        double bottom = Math.min(a.getMaxY(), b.getMaxY());
        double minH = Math.min(a.getHeight(), b.getHeight());
        return minH > 0 && (bottom - top) > 0.5 * minH;
    }

    private static Rectangle2D union(Rectangle2D a, Rectangle2D b) {
        double minX = Math.min(a.getMinX(), b.getMinX());
        double minY = Math.min(a.getMinY(), b.getMinY());
        double maxX = Math.max(a.getMaxX(), b.getMaxX());
        double maxY = Math.max(a.getMaxY(), b.getMaxY());
        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }
}
