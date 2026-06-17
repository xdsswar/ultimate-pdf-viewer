/*
 * Copyright © 2024. XTREME SOFTWARE SOLUTIONS
 *
 * All rights reserved. Unauthorized use, reproduction, or distribution
 * of this software or any portion of it is strictly prohibited and may
 * result in severe civil and criminal penalties. This code is the sole
 * proprietary of XTREME SOFTWARE SOLUTIONS.
 *
 * Commercialization, redistribution, and use without explicit permission
 * from XTREME SOFTWARE SOLUTIONS, are expressly forbidden.
 */

package com.sun.internals.controls;

import com.sun.internals.AbstractViewer;
import com.sun.internals.PdfDocumentImpl;
import com.sun.internals.document.Document;
import com.sun.internals.enums.Operation;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.util.Duration;
import xss.it.nfx.pdfium.PdfDocument;
import xss.it.nfx.pdfium.scene.PdfPageView;
import xss.it.nfx.pdfium.scene.PdfSelectionModel;
import xss.it.ultimate.pdf.viewer.controls.PageView;
import xss.it.ultimate.pdf.viewer.enums.Fit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/**
 * A zoomable, continuously-scrolling page view: one page per row, each rendered
 * pixel-perfect by a {@link PdfPageView} at the current zoom. Behaves like
 * {@link SinglePageViewer} (both scrollbars, horizontal scroll when zoomed wider
 * than the viewport) but flows every page one after another.
 *
 * <p>Pages are virtualized: only those near the viewport are materialized and
 * rendered; the rest are reserved as empty space so layout and scrolling stay
 * stable for large documents.</p>
 *
 * @author XDSSWAR
 * Created on 04/07/2024
 */
public final class ContinuousPageViewer extends ScrollPane implements PageView {

    private static final String STYLE_CLASS = "continuous-page-viewer";

    /**
     * Vertical gap between pages, in POINTS, so it scales with zoom. Keeping the
     * gap proportional makes the whole content scale uniformly with zoom, which
     * is what lets pointer-anchored zoom stay exact (no drift).
     */
    private static final double GAP_POINTS = 12.0;

    /** Zoom step for Ctrl+scroll. */
    private static final double ZOOM_DELTA = 0.1;

    /** Relative zoom change below which a fit is treated as already satisfied. */
    private static final double FIT_EPSILON = 0.002;

    private final AbstractViewer viewer;

    /** Logical pixels per point at 100% zoom (screen DPI / 72). */
    private final double screenScale;

    /** Content surface that lays out the (virtualized) pages. */
    private final Pages pages = new Pages();

    /** Intrinsic page sizes in points. */
    private double[] pageW = new double[0];
    private double[] pageH = new double[0];

    /**
     * Cached, device-snapped layout geometry for the current zoom. Recomputed only
     * when the zoom, document or device scale changes — never per scroll tick — so
     * scrolling stays O(log n) instead of O(n^2).
     */
    private double[] pageTopPx = new double[0];
    private double[] pageHeightPx = new double[0];
    private double totalHeightPx;
    private double maxWidthPx;
    private double gapPx;

    /**
     * Currently materialized page views, keyed by page index. Concurrent so the
     * render threads can cheaply check whether a page is still wanted (and skip
     * stale renders) without touching the FX thread.
     */
    private final Map<Integer, PdfPageView> active = new ConcurrentHashMap<>();

    /**
     * Throttles page (re)materialization while scrolling so a fast scroll does
     * little work per frame — the motion stays buttery and rendering catches up.
     */
    private final PauseTransition renderThrottle = new PauseTransition(Duration.millis(110));
    private boolean renderTrailing;

    /**
     * Fires once scrolling pauses. The thumbnail panel follows the current page
     * only here — never per frame — so scrolling never storms thumbnail renders.
     */
    private final PauseTransition pageSettle = new PauseTransition(Duration.millis(160));

    /** Frame ring drawn around each materialized page, keyed by page index. */
    private final Map<Integer, PageFrame> frames = new HashMap<>();

    /**
     * Recycled, ready-to-reuse page views and frames. Reusing nodes (repointing
     * them with {@code setPageIndex}) avoids constructing a {@link PdfPageView}
     * (loader animation, text layer, listeners) on every scroll — the main cost
     * and GC churn of fast scrolling a large document.
     */
    private final Deque<PdfPageView> pagePool = new ArrayDeque<>();
    private final Deque<PageFrame> framePool = new ArrayDeque<>();

    /** Upper bound on pooled nodes (a few viewports' worth is plenty). */
    private static final int MAX_POOL = 12;

    /** One selection shared by every page cell, so a selection spans pages. */
    private final PdfSelectionModel selection = new PdfSelectionModel();

    /** Bounded, priority render scheduler shared by every materialized page. */
    private final RenderScheduler scheduler = new RenderScheduler();

    /** Guards the page<->scroll feedback loop while syncing the current page. */
    private boolean syncing;

    /**
     * True while the view is scrolling itself to a page (thumbnail/toolbar
     * selection). During such a programmatic scroll the resulting scroll events
     * must NOT re-sync the current page — otherwise the settle would recompute the
     * center page and fight the page you just picked (the feedback loop).
     */
    private boolean programmaticScroll;

    /** True until the view has positioned itself at the viewer's current page. */
    private boolean initialScroll = true;

    /** True once the min-thumb skin has been installed on the scrollbars. */
    private boolean minThumbInstalled;

    /**
     * True while the user is actively scrolling (until the scroll settles). The fit
     * and the page-change scroll-snap must NOT run during this window, otherwise the
     * view would yank itself to the top of each page as it scrolls past — destroying
     * smooth scrolling.
     */
    private boolean userScrolling;

    /** Guards the fit -> relayout -> viewport-change -> fit re-entrancy. */
    private boolean applyingFit;

    /**
     * Constructs a continuous viewer for the given viewer.
     *
     * @param viewer the owning viewer
     */
    public ContinuousPageViewer(AbstractViewer viewer) {
        this.viewer = viewer;
        this.screenScale = Screen.getPrimary().getDpi() / PdfDocumentImpl.DPI;
        getStyleClass().add(STYLE_CLASS);

        setContent(pages);
        setFitToWidth(false); // keep page width so a horizontal scrollbar can appear
        setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        // No bitmap caching — pages must stay pixel-perfect at every zoom.
        setCache(false);
        pages.setCache(false);
        setPannable(true); // drag pans when the Pan tool is active (text selection off)

        loadDocument();

        viewer.documentProperty().addListener((o, a, b) -> loadDocument());
        viewer.zoomFactorProperty().addListener((o, a, b) -> onZoomChanged());
        // Page (re)materialization is throttled while scrolling (buttery motion);
        // the thumbnail follow waits until scrolling pauses (no mid-scroll storm).
        renderThrottle.setOnFinished(e -> {
            if (renderTrailing) {
                renderTrailing = false;
                updateVisible();
                renderThrottle.playFromStart();
            }
        });
        pageSettle.setOnFinished(e -> {
            userScrolling = false; // scrolling has settled; fit/sync may run again
            syncCurrentPage();
            ensureVisibleRendered(); // recover any page whose render was dropped
        });
        vvalueProperty().addListener((o, a, b) -> onScrolled());
        hvalueProperty().addListener((o, a, b) -> onScrolled());
        viewportBoundsProperty().addListener((o, a, b) -> {
            // The scrollbars exist once laid out: enforce a minimum thumb size so it
            // stays visible on huge documents (CSS -fx-min-height isn't honored).
            if (!minThumbInstalled) {
                minThumbInstalled = installMinThumbSkin();
            }
            // The device scale may change when first shown / moved between monitors;
            // refresh the cached geometry (cheap, not per-scroll).
            recomputeLayout();
            pages.requestLayout();
            // Re-fit on genuine viewport changes (resize), but never mid-scroll: a
            // fit recomputes the zoom and recenters the page, which would fight the
            // scroll and snap to the current page's top on every page.
            if (!isNotActive() && viewer.getFit() != Fit.NONE && !userScrolling) {
                applyFit(viewer.getFit());
            }
            // First time we have a real viewport, jump to the viewer's current page
            // (so switching into continuous view keeps your place, not page 1).
            if (initialScroll && pageH.length > 0 && b != null && b.getHeight() > 0) {
                initialScroll = false;
                scrollToPage(viewer.getPage());
            }
            updateVisible();
        });
        viewer.pageProperty().addListener((o, a, b) -> {
            updateSelectedFrames();
            // Only jump to a page when the change came from outside (thumbnail/toolbar
            // selection). A scroll-driven page change (syncing) or any change while the
            // user is scrolling must not snap the scroll position.
            if (!syncing && !userScrolling) {
                scrollToPage(b.intValue());
            }
        });
        // Fit applies to the CURRENT visible page only (not the whole document).
        viewer.fitProperty().addListener((o, a, fit) -> {
            if (!isNotActive()) {
                applyFit(fit);
            }
        });
        // Rotating the current page changes its effective size; defer to a pulse so
        // the document's per-page rotation is already updated, then relayout.
        viewer.pageRotationProperty().addListener((o, a, b) -> Platform.runLater(this::onPageRotated));
        // Pan tool: drag pans (text selection off); otherwise dragging selects text.
        viewer.operationProperty().addListener((o, a, op) -> applyOperation(op));

        // Ctrl+scroll zooms (anchored on the viewer zoom); plain scroll scrolls.
        addEventFilter(ScrollEvent.SCROLL, this::onScroll);
    }

    /* ----------------------------------------------------------- data model */

    /** (Re)reads page sizes from the current document and resets the surface. */
    private void loadDocument() {
        // Detach every live/pooled page so nothing from the old document leaks.
        for (PdfPageView pv : active.values()) {
            disposePage(pv);
        }
        for (PdfPageView pv : pagePool) {
            disposePage(pv);
        }
        active.clear();
        frames.clear();
        pagePool.clear();
        framePool.clear();
        scheduler.clearPending();
        pages.getChildren().clear();
        selection.clear();
        initialScroll = true; // position at the current page once laid out

        Document document = viewer.getDocument();
        if (document == null) {
            pageW = new double[0];
            pageH = new double[0];
            selection.setPageCount(0);
            recomputeLayout();
            pages.requestLayout();
            return;
        }
        PdfDocument doc = document.getPdfDocument();
        int n = doc.getPageCount();
        selection.setPageCount(n);
        pageW = new double[n];
        pageH = new double[n];
        for (int i = 0; i < n; i++) {
            pageW[i] = doc.getPage(i).getWidth();
            pageH[i] = doc.getPage(i).getHeight();
        }
        recomputeLayout();
        pages.requestLayout();
        updateVisible();
    }

    /** Logical pixels per point at the current zoom. */
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

    /**
     * Snaps a logical length to a whole number of device pixels so each page cell
     * matches its rendered bitmap exactly (crisp 1:1, no soft scaling).
     */
    private double snap(double logical) {
        double s = outputScale();
        return Math.round(logical * s) / s;
    }

    /** Gap between pages at the current zoom, device-aligned, in logical pixels. */
    private double gap() {
        return gapPx;
    }

    /** Total content height for all pages plus gaps, in logical pixels (cached). */
    private double contentHeight() {
        return totalHeightPx;
    }

    /** Widest page at the current zoom, in logical pixels (cached). */
    private double contentWidth() {
        return maxWidthPx;
    }

    /** Top y of page {@code i} in content coordinates, in logical pixels (cached). */
    private double pageTop(int i) {
        return (i >= 0 && i < pageTopPx.length) ? pageTopPx[i] : 0;
    }

    /**
     * Recomputes the cached, device-snapped per-page offsets/heights and the total
     * content size for the current zoom. O(n), run only when the zoom, document or
     * device scale changes — never per scroll tick (keeps scrolling smooth).
     */
    private void recomputeLayout() {
        int n = pageH.length;
        double s = displayScale();
        gapPx = snap(GAP_POINTS * s);
        pageTopPx = new double[n];
        pageHeightPx = new double[n];
        double y = gapPx;
        double maxW = 0;
        for (int i = 0; i < n; i++) {
            double h = snap(effectiveHeight(i) * s);
            pageHeightPx[i] = h;
            pageTopPx[i] = y;
            y += h + gapPx;
            maxW = Math.max(maxW, snap(effectiveWidth(i) * s));
        }
        totalHeightPx = y;
        maxWidthPx = maxW;
    }

    /** The global rotation (degrees) applied to every page. */
    private double pageRotation() {
        return viewer.getPageRotation();
    }

    /** Whether the global rotation is a quarter turn (90/270), swapping width/height. */
    private boolean quarterRotated() {
        return ((((int) Math.round(pageRotation() / 90.0)) % 2) + 2) % 2 != 0;
    }

    /** Page {@code i}'s on-screen width in points, accounting for the global rotation. */
    private double effectiveWidth(int i) {
        return quarterRotated() ? pageH[i] : pageW[i];
    }

    /** Page {@code i}'s on-screen height in points, accounting for the global rotation. */
    private double effectiveHeight(int i) {
        return quarterRotated() ? pageW[i] : pageH[i];
    }

    /**
     * Applies the new global rotation to every materialized page, recomputes the
     * (now-swapped) geometry, relays out and keeps the current page in view.
     */
    private void onPageRotated() {
        if (pageH.length == 0) {
            return;
        }
        double rotation = pageRotation();
        for (PdfPageView pv : active.values()) {
            pv.setRotation(rotation);
        }
        recomputeLayout();
        pages.requestLayout();
        // Rotation swaps each page's width/height, so re-fit to recompute the zoom for
        // the new orientation (when fitting).
        if (!isNotActive() && viewer.getFit() != Fit.NONE) {
            applyFit(viewer.getFit());
        }
        // Always end positioned on the selected page (rescroll after rotate).
        layout();
        scrollToPage(viewer.getPage());
        updateVisible();
        ensureVisibleRendered();
    }

    /**
     * Index of the first page whose bottom edge reaches {@code yBound} (binary
     * search over the sorted, cached offsets), or the page count if none.
     *
     * @param yBound a content-space y, in logical pixels
     * @return the first page index at or past {@code yBound}
     */
    private int firstPageReaching(double yBound) {
        int lo = 0;
        int hi = pageTopPx.length; // exclusive
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (pageTopPx[mid] + pageHeightPx[mid] >= yBound) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }

    /* ------------------------------------------------------ virtualization */

    private void onZoomChanged() {
        // Page views have their zoom bound, so they re-render; we recompute the
        // cached geometry for the new zoom, resize the surface and refresh which
        // pages are materialized.
        recomputeLayout();
        pages.requestLayout();
        updateVisible();
    }

    /**
     * Handles a scroll-position change. Page (re)materialization is throttled
     * (leading + trailing) so fast scrolling does little work per frame; the
     * thumbnail panel follows only after scrolling pauses, so it never renders a
     * thumbnail for every page that flies past.
     */
    private void onScrolled() {
        if (renderThrottle.getStatus() == Animation.Status.RUNNING) {
            renderTrailing = true; // a refresh is due when the cooldown ends
        } else {
            updateVisible();
            renderThrottle.playFromStart();
        }
        // Only a USER scroll updates the current page (and the thumbnail selection);
        // a programmatic scroll-to-page must not, or it would fight the target page.
        if (!programmaticScroll) {
            userScrolling = true; // suppress fit/snap until the scroll settles
            pageSettle.playFromStart();
        }
    }

    /**
     * Installs {@link MinThumbScrollBarSkin} on this view's scroll bars so the thumb
     * never shrinks below the CSS {@code -fx-min-height}/{@code -fx-min-width} (keeps
     * it visible on huge documents).
     *
     * @return {@code true} once the scroll bars were found and skinned
     */
    private boolean installMinThumbSkin() {
        boolean found = false;
        for (Node n : lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar sb) {
                if (!(sb.getSkin() instanceof MinThumbScrollBarSkin)) {
                    sb.setSkin(new MinThumbScrollBarSkin(sb));
                }
                found = true;
            }
        }
        return found;
    }

    /** Materializes pages near the viewport and releases the rest. */
    private void updateVisible() {
        if (pageW.length == 0) {
            return;
        }
        Bounds vp = getViewportBounds();
        double viewportH = vp != null ? vp.getHeight() : 0;
        double total = contentHeight();
        double scrollY = getVvalue() * Math.max(0.0, total - viewportH);

        // Two bands with hysteresis: materialize within the (narrow) ACQUIRE band,
        // but only release once a page leaves the (wider) KEEP band. This stops a
        // page from being released and immediately re-acquired while scrolling near
        // the boundary — which would clear+re-render+fade it and look like a flick.
        double acquireTop = scrollY - viewportH * 0.5;
        double acquireBottom = scrollY + viewportH * 1.5;
        double keepTop = scrollY - viewportH * 1.0;
        double keepBottom = scrollY + viewportH * 2.0;

        // Binary-search the first page in range, then walk until past the bottom
        // (sorted offsets => O(log n + visible), not O(n)).
        Set<Integer> needed = new HashSet<>();
        for (int i = firstPageReaching(acquireTop); i < pageHeightPx.length; i++) {
            if (pageTopPx[i] > acquireBottom) {
                break;
            }
            needed.add(i);
        }
        Set<Integer> keep = new HashSet<>();
        for (int i = firstPageReaching(keepTop); i < pageHeightPx.length; i++) {
            if (pageTopPx[i] > keepBottom) {
                break;
            }
            keep.add(i);
        }

        // Nothing to release (all active still within keep) and nothing new to
        // materialize (all needed already active)? Then there is no work to do.
        boolean releaseNeeded = false;
        for (int i : active.keySet()) {
            if (!keep.contains(i)) {
                releaseNeeded = true;
                break;
            }
        }
        boolean acquireNeeded = false;
        for (int i : needed) {
            if (!active.containsKey(i)) {
                acquireNeeded = true;
                break;
            }
        }
        if (!releaseNeeded && !acquireNeeded) {
            return;
        }

        // Release pages that left the keep band (frees their native render buffers).
        active.keySet().removeIf(i -> {
            if (!keep.contains(i)) {
                releasePage(active.get(i));
                PageFrame f = frames.remove(i);
                if (f != null) {
                    pages.getChildren().remove(f);
                    releaseFrame(f);
                }
                return true;
            }
            return false;
        });

        // Materialize newly visible pages (reusing pooled nodes where possible).
        Document document = viewer.getDocument();
        PdfDocument doc = document != null ? document.getPdfDocument() : null;
        if (doc == null) {
            return;
        }
        for (int i : needed) {
            if (active.containsKey(i)) {
                continue;
            }
            // Frame first (behind the page); it shows a 1px ring around it.
            PageFrame frame = acquireFrame();
            frame.setSelected(i == viewer.getPage());
            frames.put(i, frame);
            pages.getChildren().add(frame);

            // Register the page as active BEFORE triggering its render, so the
            // skip-stale guard never drops the very first render of a new page.
            PdfPageView pv = obtainPage();
            active.put(i, pv);
            pv.setRenderExecutor(scheduler.forPage(i));
            pv.setRotation(pageRotation()); // global rotation, applies to all pages
            applyOperationTo(pv, viewer.getOperation()); // pan vs text-select
            pv.setDocument(doc);
            pv.setPageIndex(i);
            pages.getChildren().add(pv);
        }
        pages.requestLayout();
    }

    /**
     * Re-renders any visible page that has no bitmap yet (its render was dropped
     * or failed). Called when scrolling settles, so blank pages always recover.
     */
    private void ensureVisibleRendered() {
        for (PdfPageView pv : active.values()) {
            pv.ensureRendered();
        }
    }

    /**
     * Applies the active tool to every materialized page: in PAN mode dragging
     * pans the view (text selection off, open-hand cursor); otherwise dragging
     * selects text.
     *
     * @param operation the active operation
     */
    private void applyOperation(Operation operation) {
        if (operation == Operation.PAN) {
            selection.clear(); // entering pan mode drops any text selection
        }
        for (PdfPageView pv : active.values()) {
            applyOperationTo(pv, operation);
        }
    }

    /** Applies the active tool to a single page view. */
    private void applyOperationTo(PdfPageView pv, Operation operation) {
        boolean pan = operation == Operation.PAN;
        pv.setTextSelectable(!pan);
        pv.setCursor(pan ? Cursor.OPEN_HAND : Cursor.DEFAULT);
    }

    /* ------------------------------------------------------ node pooling */

    /** Reuses a pooled page view, or builds one. The caller points it at a page. */
    private PdfPageView obtainPage() {
        PdfPageView pv = pagePool.pollFirst();
        if (pv == null) {
            pv = new PdfPageView();
            pv.setDpi(Screen.getPrimary().getDpi()); // zoom 1 == screen DPI
            pv.zoomProperty().bind(viewer.zoomFactorProperty());
            pv.setSelectionModel(selection); // shared -> selection spans pages
        }
        return pv;
    }

    /** Removes a page from the surface and recycles it (or disposes if the pool is full). */
    private void releasePage(PdfPageView pv) {
        // Removing from the scene stops its loader animation/timers (see PdfPageView).
        pages.getChildren().remove(pv);
        if (pagePool.size() < MAX_POOL) {
            pagePool.addLast(pv); // kept bound to zoom/selection, ready to reuse
        } else {
            disposePage(pv);
        }
    }

    /** Fully detaches a page view so it can be garbage-collected. */
    private void disposePage(PdfPageView pv) {
        pv.zoomProperty().unbind();
        pv.setSelectionModel(null);
        pv.setDocument(null);
    }

    /** Reuses a pooled frame (or builds one). */
    private PageFrame acquireFrame() {
        PageFrame f = framePool.pollFirst();
        return f != null ? f : new PageFrame();
    }

    /** Recycles a frame for reuse (capped). */
    private void releaseFrame(PageFrame f) {
        f.setSelected(false);
        if (framePool.size() < MAX_POOL) {
            framePool.addLast(f);
        }
    }

    /* --------------------------------------------------------------- input */

    private void onScroll(ScrollEvent event) {
        if (!event.isControlDown()) {
            return; // plain scroll = normal vertical scrolling
        }
        event.consume();
        if (viewer.getFit() != Fit.NONE) {
            viewer.setFit(Fit.NONE); // free zoom once the user zooms manually
        }
        double oldZoom = viewer.getZoomFactor();
        double delta = event.getDeltaY() > 0 ? ZOOM_DELTA : -ZOOM_DELTA;
        double newZoom = Math.max(viewer.getMinZoomFactor(),
                Math.min(viewer.getMaxZoomFactor(), oldZoom + delta));
        if (newZoom == oldZoom) {
            return;
        }

        // Keep the content point under the cursor fixed (zoom toward the pointer).
        Bounds vp = getViewportBounds();
        double vpW = vp != null ? vp.getWidth() : 0;
        double vpH = vp != null ? vp.getHeight() : 0;
        double oldW = Math.max(contentWidth(), vpW);
        double oldH = contentHeight();
        double offX = getHvalue() * Math.max(0.0, oldW - vpW);
        double offY = getVvalue() * Math.max(0.0, oldH - vpH);

        // Content point under the cursor (old scale) and its on-screen position.
        Point2D inContent = pages.sceneToLocal(event.getSceneX(), event.getSceneY());
        double contentX = inContent.getX();
        double contentY = inContent.getY();
        double screenX = contentX - offX;
        double screenY = contentY - offY;
        double f = newZoom / oldZoom;

        viewer.setZoomFactor(newZoom); // resizes the surface (zoom is bound on pages)

        // Force the surface to resize to the new zoom NOW, so the scroll offset
        // maps correctly and the anchor is exact (no drift / page loss).
        layout();

        // Content scales uniformly with zoom (gaps scale too), so the same point
        // is now at contentX*f / contentY*f. Re-scroll so it stays under the cursor.
        double newW = Math.max(contentWidth(), vpW);
        double newH = contentHeight();
        double maxX = Math.max(0.0, newW - vpW);
        double maxY = Math.max(0.0, newH - vpH);
        setHvalue(maxX > 0 ? clamp01((contentX * f - screenX) / maxX) : 0.0);
        setVvalue(maxY > 0 ? clamp01((contentY * f - screenY) / maxY) : 0.0);
        updateVisible();
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    /**
     * Updates the viewer's current page to the one under the viewport center as
     * the user scrolls (so the thumbnail highlights and the thumbs list scrolls).
     * Guarded so it doesn't trigger a scroll-back.
     */
    private void syncCurrentPage() {
        if (syncing || pageH.length == 0) {
            return;
        }
        int idx = currentPageIndex();
        if (idx != viewer.getPage()) {
            syncing = true;
            try {
                viewer.setPage(idx);
            } finally {
                syncing = false;
            }
        }
    }

    /** Marks the frame of the current page as selected (blue), the rest as normal. */
    private void updateSelectedFrames() {
        int cur = viewer.getPage();
        for (Map.Entry<Integer, PageFrame> e : frames.entrySet()) {
            e.getValue().setSelected(e.getKey() == cur);
        }
    }

    /** Scrolls so the top of page {@code index} is visible (without re-syncing). */
    private void scrollToPage(int index) {
        if (index < 0 || index >= pageH.length) {
            return;
        }
        Bounds vp = getViewportBounds();
        double viewportH = vp != null ? vp.getHeight() : 0;
        double max = Math.max(0.0, contentHeight() - viewportH);
        // Flag this as a programmatic scroll so onScrolled() doesn't sync the page
        // back (breaking the thumbnail<->pageview feedback loop).
        programmaticScroll = true;
        try {
            setVvalue(max > 0 ? Math.min(1.0, pageTop(index) / max) : 0.0);
        } finally {
            programmaticScroll = false;
        }
    }

    /* ------------------------------------------------------------------ fit */

    /** Whether this view is NOT the viewer's active page view (guards stale callbacks). */
    private boolean isNotActive() {
        return viewer.getPageView() != this;
    }

    /** Index of the page nearest the vertical center of the viewport. */
    private int currentPageIndex() {
        if (pageH.length == 0) {
            return 0;
        }
        Bounds vp = getViewportBounds();
        double vpH = vp != null ? vp.getHeight() : 0;
        double center = getVvalue() * Math.max(0.0, contentHeight() - vpH) + vpH / 2.0;
        return Math.min(firstPageReaching(center), pageH.length - 1);
    }

    /**
     * Fits the CURRENT visible page (only) to the viewport in the given
     * direction, then keeps that page in view.
     *
     * @param fit the fit mode
     */
    private void applyFit(Fit fit) {
        if (pageW.length == 0 || applyingFit) {
            return;
        }
        applyingFit = true;
        try {
            applyScrollBarPolicy(fit);
            int i = currentPageIndex();
            Bounds vp = getViewportBounds();
            double vpW = vp != null ? vp.getWidth() : 0;
            double vpH = vp != null ? vp.getHeight() : 0;
            switch (fit) {
                case HORIZONTAL -> {
                    if (vpW > 0) {
                        setZoomIfChanged(vpW / (effectiveWidth(i) * screenScale));
                    }
                }
                case VERTICAL -> {
                    if (vpH > 0) {
                        setZoomIfChanged(vpH / (effectiveHeight(i) * screenScale));
                    }
                }
                default -> {
                    return;
                }
            }
            layout();
            scrollToPage(i);
            updateVisible();
        } finally {
            applyingFit = false;
        }
    }

    /** Sets the zoom only if it differs meaningfully (keeps fit from oscillating). */
    private void setZoomIfChanged(double zoom) {
        double clamped = Math.max(viewer.getMinZoomFactor(),
                Math.min(viewer.getMaxZoomFactor(), zoom));
        if (Math.abs(clamped - viewer.getZoomFactor()) > FIT_EPSILON * clamped) {
            viewer.setZoomFactor(clamped);
        }
    }

    /** Stabilizes scrollbars for the active fit (see SinglePageViewer for rationale). */
    private void applyScrollBarPolicy(Fit fit) {
        switch (fit) {
            case HORIZONTAL -> {
                setHbarPolicy(ScrollBarPolicy.NEVER);
                setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
            }
            case VERTICAL -> {
                setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
                setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
            }
            default -> {
                setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
                setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
            }
        }
    }

    @Override
    public void reload() {
        loadDocument();
    }

    /* --------------------------------------------------------- scheduling */

    /**
     * Bounded, priority render scheduler shared by every materialized page.
     *
     * <p>At most two worker threads run at once, so a zoom never pins every core
     * and starves the JavaFX thread (no UI freeze). A priority queue runs the page
     * nearest the viewport center first: the page you are looking at sharpens
     * immediately while its neighbors follow outward as capacity frees up — and as
     * you scroll, newly visible pages are prioritized by proximity. Only the
     * materialized (near-viewport) pages ever render, so memory stays bounded.</p>
     */
    private final class RenderScheduler {
        private final ThreadPoolExecutor pool;
        private final AtomicLong order = new AtomicLong();

        RenderScheduler() {
            // A few platform (non-virtual) threads: half the cores, less a margin,
            // floored at 1 — leaves plenty of CPU for the JavaFX thread.
            int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2 - 2);
            pool = new ThreadPoolExecutor(
                    threads, threads, 20, TimeUnit.SECONDS,
                    new PriorityBlockingQueue<>(),
                    r -> {
                        Thread t = new Thread(r, "nfx-continuous-render");
                        t.setDaemon(true);
                        // Below normal so heavy rasterization yields to the JavaFX
                        // thread — the UI stays responsive while pages catch up.
                        t.setPriority(Thread.MIN_PRIORITY);
                        return t;
                    });
            pool.allowCoreThreadTimeOut(true);
        }

        /**
         * Returns an executor for page {@code index}: each render it submits is
         * tagged with the page's distance from the current viewport center
         * (computed now, on the FX thread) so the closest page runs first.
         *
         * @param index the page index this executor serves
         * @return a per-page executor that enqueues renders by viewport proximity
         */
        Executor forPage(int index) {
            return body -> pool.execute(new PageRender(
                    Math.abs(index - currentPageIndex()), order.incrementAndGet(), body,
                    // Skip if the page was released (scrolled away) before it ran —
                    // this drains the backlog cheaply on a fast scroll instead of
                    // rendering hundreds of pages no longer on screen.
                    () -> !active.containsKey(index)));
        }

        /** Drops queued (not-yet-started) renders, e.g. when the document changes. */
        void clearPending() {
            pool.getQueue().clear();
        }
    }

    /** A render task ordered by viewport proximity, then submission order (FIFO). */
    private static final class PageRender implements Runnable, Comparable<PageRender> {
        private final int distance;
        private final long order;
        private final Runnable body;
        private final BooleanSupplier stale;

        PageRender(int distance, long order, Runnable body, BooleanSupplier stale) {
            this.distance = distance;
            this.order = order;
            this.body = body;
            this.stale = stale;
        }

        @Override
        public void run() {
            if (stale.getAsBoolean()) {
                return; // page scrolled away before we got to it — drop the render
            }
            body.run();
        }

        @Override
        public int compareTo(PageRender o) {
            int d = Integer.compare(distance, o.distance);
            return d != 0 ? d : Long.compare(order, o.order);
        }
    }

    /* --------------------------------------------------------- content pane */

    /**
     * Content surface: reports the full (virtual) content size and positions the
     * materialized pages at their computed offsets, centered horizontally.
     */
    private final class Pages extends Pane {

        Pages() {
            getStyleClass().add("pages");
        }

        @Override
        protected double computePrefWidth(double height) {
            Bounds vp = getViewportBounds();
            double viewportW = vp != null ? vp.getWidth() : 0;
            // At least the viewport width so narrow pages center; wider content
            // grows the surface so a horizontal scrollbar appears.
            return Math.max(contentWidth(), viewportW);
        }

        @Override
        protected double computePrefHeight(double width) {
            return contentHeight();
        }

        @Override
        protected void layoutChildren() {
            double s = displayScale();
            double paneW = getWidth();
            double b = PageFrame.BORDER;
            for (Map.Entry<Integer, PdfPageView> e : active.entrySet()) {
                int i = e.getKey();
                PdfPageView pv = e.getValue();
                double w = snap(effectiveWidth(i) * s);
                double h = snap(effectiveHeight(i) * s);
                double x = Math.max(0, snap((paneW - w) / 2.0)); // center horizontally
                double top = pageTop(i);
                pv.resizeRelocate(x, top, w, h);
                PageFrame f = frames.get(i);
                if (f != null) {
                    f.resizeRelocate(x - b, top - b, w + 2 * b, h + 2 * b);
                }
            }
        }
    }
}
