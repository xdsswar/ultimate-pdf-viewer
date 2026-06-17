package com.sun.internals.controls;

import com.sun.internals.AbstractViewer;
import com.sun.internals.PdfDocumentImpl;
import com.sun.internals.document.Document;
import com.sun.internals.enums.Operation;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.geometry.Point2D;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import xss.it.nfx.pdfium.PdfPage;
import xss.it.nfx.pdfium.scene.PdfPageView;
import xss.it.ultimate.pdf.viewer.Assets;
import xss.it.ultimate.pdf.viewer.controls.PageView;
import xss.it.ultimate.pdf.viewer.enums.Fit;

/**
 * Single-page view: one {@link PdfPageView} centered in a {@link ScrollPane}.
 * Inherits the page node's crisp pixel-perfect rendering, text selection (drag,
 * Ctrl+C, Ctrl+A) and per-page loader. Zoom is mouse-anchored on Ctrl+scroll;
 * fit targets the current page; both scrollbars behave like the continuous view.
 *
 * @author XDSSWAR
 * Created on 01/26/2024
 */
public final class SinglePageViewer extends ScrollPane implements PageView {

    private static final String STYLE_CLASS = "single-page-viewer";
    private static final double ZOOM_DELTA = 0.1;
    private static final double FIT_EPSILON = 0.002;

    /**
     * Approximate scrollbar thickness reserved in fit modes (matches the CSS
     * {@code -fx-pref-width}/{@code -fx-pref-height} on the scroll bars).
     */
    private static final double SCROLLBAR_BREADTH = 14;

    /**
     * Slack (logical px) the content must exceed the viewport by before a cross-axis
     * scrollbar is considered needed, so a page sitting right at the edge does not
     * flip-flop the scrollbar on and off.
     */
    private static final double FIT_OVERFLOW_SLACK = 1.0;

    /** Tolerance for detecting that the scroll is at the top/bottom edge. */
    private static final double EDGE_EPS = 1e-3;

    /** Wheel notches at an edge required before flipping to the next/prev page. */
    private static final int EDGE_HITS = 2;

    private int topHits;
    private int bottomHits;

    /** When true, the next page change lands at the bottom (used for prev-page). */
    private boolean landAtBottom;

    /**
     * Guards the fit feedback loop: applying a fit changes the zoom, which relayouts
     * and may toggle a scrollbar, which fires {@code viewportBounds} and would re-enter
     * {@link #applyFit}. Re-entrant calls are skipped so a fit settles in one pass.
     */
    private boolean applyingFit;

    private final AbstractViewer viewer;

    /** Logical pixels per point at 100% zoom (screen DPI / 72). */
    private final double screenScale;

    private final PdfPageView pageView = new PdfPageView();
    private final PageFrame frame = new PageFrame();
    private final Holder holder = new Holder();

    /**
     * Constructs a single-page view for the given viewer.
     *
     * @param viewer the owning viewer
     */
    public SinglePageViewer(AbstractViewer viewer) {
        this.viewer = viewer;
        getStyleClass().add(STYLE_CLASS);
        getStylesheets().add(getUserAgentStylesheet());
        this.screenScale = Screen.getPrimary().getDpi() / PdfDocumentImpl.DPI;

        // zoom == 1 shows the page at screen DPI; zoom tracks the viewer.
        pageView.setDpi(Screen.getPrimary().getDpi());
        pageView.zoomProperty().bind(viewer.zoomFactorProperty());
        // The single view always shows the current page, whose rotation is mirrored
        // by the viewer's pageRotation property.
        pageView.rotationProperty().bind(viewer.pageRotationProperty());
        // Frame sits behind the page (it only shows a 1px ring around it).
        holder.getChildren().addAll(frame, pageView);

        setContent(holder);
        setFitToWidth(false);
        setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setCache(false); // keep the page pixel-perfect (no cached-bitmap scaling)
        setFocusTraversable(true);
        setPannable(true); // drag pans when text selection isn't active

        applyOperation(viewer.getOperation());
        bindDocument();
        showPage(viewer.getPage());

        viewer.documentProperty().addListener((o, a, b) -> bindDocument());
        viewer.pageProperty().addListener((o, a, b) -> showPage(b.intValue()));
        viewer.zoomFactorProperty().addListener((o, a, b) -> holder.requestLayout());
        viewportBoundsProperty().addListener((o, a, b) -> {
            holder.requestLayout();
            if (!isNotActive() && viewer.getFit() != Fit.NONE) {
                applyFit(viewer.getFit());
            }
        });
        viewer.fitProperty().addListener((o, a, fit) -> {
            if (!isNotActive()) {
                applyFit(fit);
            }
        });
        viewer.pageRotationProperty().addListener((o, a, b) -> {
            // Rotating 90/270 swaps the page's effective size: relayout (and refit).
            holder.requestLayout();
            setHvalue(0);
            setVvalue(0);
            if (!isNotActive() && viewer.getFit() != Fit.NONE) {
                applyFit(viewer.getFit());
            }
        });
        viewer.operationProperty().addListener((o, a, op) -> applyOperation(op));

        addEventFilter(ScrollEvent.SCROLL, this::onScroll);
        setOnKeyPressed(this::onKeyPressed);
    }

    /* -------------------------------------------------------------- model */

    private void bindDocument() {
        Document d = viewer.getDocument();
        pageView.setDocument(d != null ? d.getPdfDocument() : null);
        holder.requestLayout();
        // A new document can change the page size, so re-fit (and refresh the zoom
        // readout) even when the page index and fit mode are unchanged.
        if (d != null && !isNotActive() && viewer.getFit() != Fit.NONE) {
            applyFit(viewer.getFit());
        }
    }

    private void showPage(int index) {
        Document d = viewer.getDocument();
        if (d == null || index < 0 || index >= d.getNumberOfPages()) {
            return;
        }
        boolean bottom = landAtBottom;
        landAtBottom = false;
        pageView.setPageIndex(index);
        holder.requestLayout();
        setHvalue(0);
        // vvalue is a ratio (0 = top, 1 = bottom), preserved as the page lays out,
        // so landing "at bottom" works regardless of zoom / page height.
        setVvalue(bottom ? 1.0 : 0.0);
        if (viewer.getFit() != Fit.NONE && !isNotActive()) {
            applyFit(viewer.getFit());
        }
    }

    private double displayScale() {
        return screenScale * viewer.getZoomFactor();
    }

    /** Live device-pixel scale of the host window (HiDPI aware). */
    private double outputScale() {
        if (getScene() != null && getScene().getWindow() != null) {
            double s = getScene().getWindow().getOutputScaleX();
            if (s > 0) {
                return s;
            }
        }
        return Screen.getPrimary().getOutputScaleX();
    }

    /** Snaps a logical length to a whole number of device pixels (crisp 1:1). */
    private double snap(double logical) {
        double s = outputScale();
        return Math.round(logical * s) / s;
    }

    /** Whether the current rotation is a quarter turn (90/270), swapping width/height. */
    private boolean quarterRotated() {
        return ((((int) Math.round(viewer.getPageRotation() / 90.0)) % 2) + 2) % 2 != 0;
    }

    private double pageWidthPts() {
        Document d = viewer.getDocument();
        int i = viewer.getPage();
        if (d == null || i < 0 || i >= d.getNumberOfPages()) {
            return 0;
        }
        PdfPage p = d.getPdfDocument().getPage(i);
        return quarterRotated() ? p.getHeight() : p.getWidth();
    }

    private double pageHeightPts() {
        Document d = viewer.getDocument();
        int i = viewer.getPage();
        if (d == null || i < 0 || i >= d.getNumberOfPages()) {
            return 0;
        }
        PdfPage p = d.getPdfDocument().getPage(i);
        return quarterRotated() ? p.getWidth() : p.getHeight();
    }

    /* -------------------------------------------------------------- input */

    private void onScroll(ScrollEvent event) {
        if (!event.isControlDown()) {
            if (!event.isInertia()) {
                edgeNavigate(event); // scroll past top/bottom flips page
            }
            return; // otherwise normal scrolling
        }
        event.consume();
        double oldZoom = viewer.getZoomFactor();
        double delta = event.getDeltaY() > 0 ? ZOOM_DELTA : -ZOOM_DELTA;
        double newZoom = clamp(oldZoom + delta);
        if (newZoom == oldZoom) {
            return;
        }
        if (viewer.getFit() != Fit.NONE) {
            viewer.setFit(Fit.NONE); // free zoom once the user zooms manually
        }

        Bounds vp = getViewportBounds();
        double vpW = vp != null ? vp.getWidth() : 0;
        double vpH = vp != null ? vp.getHeight() : 0;
        double oldW = Math.max(pageWidthPts() * displayScale(), vpW);
        double oldH = Math.max(pageHeightPts() * displayScale(), vpH);
        double offX = getHvalue() * Math.max(0.0, oldW - vpW);
        double offY = getVvalue() * Math.max(0.0, oldH - vpH);

        Point2D inContent = holder.sceneToLocal(event.getSceneX(), event.getSceneY());
        double contentX = inContent.getX();
        double contentY = inContent.getY();
        double screenX = contentX - offX;
        double screenY = contentY - offY;
        double f = newZoom / oldZoom;

        viewer.setZoomFactor(newZoom);
        layout(); // resize the surface now so the re-scroll maps correctly

        double newW = Math.max(pageWidthPts() * displayScale(), vpW);
        double newH = Math.max(pageHeightPts() * displayScale(), vpH);
        double maxX = Math.max(0.0, newW - vpW);
        double maxY = Math.max(0.0, newH - vpH);
        setHvalue(maxX > 0 ? clamp01((contentX * f - screenX) / maxX) : 0.0);
        setVvalue(maxY > 0 ? clamp01((contentY * f - screenY) / maxY) : 0.0);
    }

    /**
     * Flips to the next/previous page when the user keeps scrolling past the
     * bottom/top edge (or immediately when the page fits without scrolling).
     * Requires {@link #EDGE_HITS} notches at the edge to avoid accidental flips.
     */
    private void edgeNavigate(ScrollEvent event) {
        double dy = event.getDeltaY();
        if (dy == 0) {
            return;
        }
        // The ScrollPane's vmin/vmax are always 0/1, so detect real scrollability
        // from the content vs viewport height (page fits => cannot scroll).
        Bounds vp = getViewportBounds();
        double viewportH = vp != null ? vp.getHeight() : 0;
        boolean canScroll = holder.getHeight() - viewportH > 1.0;
        boolean atBottom = !canScroll || getVvalue() >= getVmax() - EDGE_EPS;
        boolean atTop = !canScroll || getVvalue() <= getVmin() + EDGE_EPS;

        if (dy < 0 && atBottom) {
            topHits = 0;
            if (++bottomHits >= EDGE_HITS) {
                bottomHits = 0;
                if (viewer.gotoNextPage()) {
                    event.consume();
                }
            }
        } else if (dy > 0 && atTop) {
            bottomHits = 0;
            if (++topHits >= EDGE_HITS) {
                topHits = 0;
                landAtBottom = true; // arrive at the bottom of the previous page
                if (!viewer.gotoPreviousPage()) {
                    landAtBottom = false; // already on the first page
                }
                event.consume();
            }
        } else {
            topHits = 0;
            bottomHits = 0;
        }
    }

    private void onKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        switch (code) {
            case PAGE_DOWN -> {
                if (viewer.gotoNextPage()) {
                    event.consume();
                }
            }
            case PAGE_UP -> {
                if (viewer.gotoPreviousPage()) {
                    event.consume();
                }
            }
            case HOME -> {
                if (viewer.gotoFirstPage()) {
                    event.consume();
                }
            }
            case END -> {
                if (viewer.gotoLastPage()) {
                    event.consume();
                }
            }
            default -> { }
        }
    }

    /* ---------------------------------------------------------------- fit */

    /**
     * Fits the current page to the viewport in the given direction and sets the zoom
     * factor accordingly (so the toolbar's zoom readout reflects the fit).
     *
     * <p>The cross-axis scrollbar (the vertical bar for {@link Fit#HORIZONTAL}, the
     * horizontal bar for {@link Fit#VERTICAL}) steals space along the fit axis, which
     * with an {@code AS_NEEDED} policy makes the page flip-flop: the fit zoom shows the
     * bar, the bar shrinks the viewport, the re-fit hides the bar, and so on — a fast
     * visual oscillation at certain window sizes. To avoid it, whether that bar is
     * needed is decided once from the <em>full</em> viewport (a value that does not
     * depend on the current bar state), then the bar is pinned {@code ALWAYS} or
     * {@code NEVER} — never {@code AS_NEEDED} — so the fit converges in one pass.</p>
     *
     * @param fit the fit mode (or {@link Fit#NONE} to restore free scrolling)
     */
    private void applyFit(Fit fit) {
        if (fit == Fit.NONE) {
            // Free zoom: let both scrollbars come and go as the content needs.
            setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
            setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
            return;
        }
        if (applyingFit) {
            return; // re-entered via our own relayout; the in-flight pass handles it
        }
        double wPts = pageWidthPts();
        double hPts = pageHeightPts();
        Bounds vp = getViewportBounds();
        double vpW = vp != null ? vp.getWidth() : 0;
        double vpH = vp != null ? vp.getHeight() : 0;
        if (wPts <= 0 || hPts <= 0 || vpW <= 0 || vpH <= 0) {
            return; // not laid out / no page yet — a later viewport change re-fits
        }
        double ss = screenScale;
        applyingFit = true;
        try {
            if (fit == Fit.HORIZONTAL) {
                // Fit width. A vertical bar would reduce the width: decide it from the
                // full width (with no vbar), then pin the policy and re-fit precisely.
                double fullW = vpW + (getVbarPolicy() == ScrollBarPolicy.ALWAYS ? SCROLLBAR_BREADTH : 0);
                double pageHAtFullW = hPts * ss * (fullW / (wPts * ss));
                boolean needVbar = pageHAtFullW > vpH + FIT_OVERFLOW_SLACK;
                setHbarPolicy(ScrollBarPolicy.NEVER);
                setVbarPolicy(needVbar ? ScrollBarPolicy.ALWAYS : ScrollBarPolicy.NEVER);
                layout(); // apply the policy now so the viewport reflects the reserved bar
                Bounds settled = getViewportBounds();
                double w = settled != null && settled.getWidth() > 0 ? settled.getWidth() : fullW;
                setZoomIfChanged(w / (wPts * ss));
            } else { // VERTICAL
                // Fit height. A horizontal bar would reduce the height: decide it from
                // the full height (with no hbar), then pin the policy and re-fit.
                double fullH = vpH + (getHbarPolicy() == ScrollBarPolicy.ALWAYS ? SCROLLBAR_BREADTH : 0);
                double pageWAtFullH = wPts * ss * (fullH / (hPts * ss));
                boolean needHbar = pageWAtFullH > vpW + FIT_OVERFLOW_SLACK;
                setVbarPolicy(ScrollBarPolicy.NEVER);
                setHbarPolicy(needHbar ? ScrollBarPolicy.ALWAYS : ScrollBarPolicy.NEVER);
                layout();
                Bounds settled = getViewportBounds();
                double h = settled != null && settled.getHeight() > 0 ? settled.getHeight() : fullH;
                setZoomIfChanged(h / (hPts * ss));
            }
        } finally {
            applyingFit = false;
        }
    }

    private void setZoomIfChanged(double zoom) {
        double clamped = clamp(zoom);
        if (Math.abs(clamped - viewer.getZoomFactor()) > FIT_EPSILON * clamped) {
            viewer.setZoomFactor(clamped);
        }
    }

    private double clamp(double zoom) {
        return Math.max(viewer.getMinZoomFactor(), Math.min(viewer.getMaxZoomFactor(), zoom));
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    /**
     * Applies the current tool: in PAN mode dragging pans (text selection off);
     * otherwise (SELECT / NONE) dragging selects text. Ctrl+drag always pans
     * (handled in the text layer, which yields when Ctrl is down).
     *
     * @param operation the active operation
     */
    private void applyOperation(Operation operation) {
        boolean pan = operation == Operation.PAN;
        pageView.setTextSelectable(!pan);
        pageView.setCursor(pan ? Cursor.OPEN_HAND : Cursor.DEFAULT);
        if (pan && pageView.getSelectionModel() != null) {
            pageView.getSelectionModel().clear(); // entering pan mode drops the selection
        }
    }

    /** Whether this view is NOT the viewer's active page view (guards stale callbacks). */
    private boolean isNotActive() {
        return viewer.getPageView() != this;
    }

    @Override
    public void reload() {
        pageView.requestRender();
    }

    @Override
    public String getUserAgentStylesheet() {
        return Assets.load("/xss/it/ultimate/pdf/viewer/css/single-page-view.css").toExternalForm();
    }

    /* --------------------------------------------------------- content pane */

    /**
     * Centers the single page; sizes to the page (so a scrollbar appears when the
     * page is larger than the viewport) or to the viewport (to center it when
     * smaller).
     */
    private final class Holder extends Pane {

        Holder() {
            getStyleClass().add("view-base");
            setCache(false);
        }

        @Override
        protected double computePrefWidth(double height) {
            Bounds vp = getViewportBounds();
            double vpW = vp != null ? vp.getWidth() : 0;
            return Math.max(snap(pageWidthPts() * displayScale()), vpW);
        }

        @Override
        protected double computePrefHeight(double width) {
            Bounds vp = getViewportBounds();
            double vpH = vp != null ? vp.getHeight() : 0;
            return Math.max(snap(pageHeightPts() * displayScale()), vpH);
        }

        @Override
        protected void layoutChildren() {
            double s = displayScale();
            double w = snap(pageWidthPts() * s);
            double h = snap(pageHeightPts() * s);
            double x = Math.max(0, snap((getWidth() - w) / 2.0));
            double y = Math.max(0, snap((getHeight() - h) / 2.0));
            pageView.resizeRelocate(x, y, w, h);
            double b = PageFrame.BORDER;
            frame.resizeRelocate(x - b, y - b, w + 2 * b, h + 2 * b);
        }
    }
}
