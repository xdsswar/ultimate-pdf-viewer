package xss.it.nfx.pdfium.scene;

import javafx.scene.layout.Region;
import xss.it.nfx.pdfium.PdfPage;

/**
 * Extension point for overlay content drawn on top of a {@link PdfPageView}.
 *
 * <p>Each layer is a {@link Region} stacked over the rendered page bitmap and
 * sized to the page's displayed (logical) bounds. Subclass this to add custom
 * content — annotations, drawing, watermarks, form fields — without touching the
 * core. The host calls {@link #onPageUpdated} whenever the page changes or its
 * geometry (zoom / dpi / rotation) changes; position your children in display
 * coordinates: {@code displayCoordinate = pagePointCoordinate * displayScale}.</p>
 *
 * <p>Layers do not capture mouse events by default ({@code pickOnBounds=false}),
 * so clicks fall through to lower layers unless a child node consumes them.</p>
 *
 * @author XDSSWAR
 */
public abstract class PdfLayer extends Region {

    /**
     * Creates a transparent, click-through layer.
     */
    protected PdfLayer() {
        setPickOnBounds(false);
        getStyleClass().add("pdf-layer");
    }

    /**
     * Invoked by the host whenever the page or its display geometry changes.
     * Override to position your content; do not call this yourself.
     *
     * @param page         the current page, or {@code null} if none is shown
     * @param displayScale points-to-logical-pixels factor ({@code zoom * dpi/72});
     *                     multiply page-point coordinates by this to place content
     * @param rotation     the page rotation in degrees (clockwise)
     */
    protected abstract void onPageUpdated(PdfPage page, double displayScale, double rotation);

    /**
     * Host entry point that dispatches to {@link #onPageUpdated}. Called by
     * {@link PdfPageView}; subclasses and consumers should not call it directly.
     *
     * @param page         the current page, or {@code null}
     * @param displayScale points-to-logical-pixels factor
     * @param rotation     the page rotation in degrees
     */
    public final void pageUpdated(PdfPage page, double displayScale, double rotation) {
        onPageUpdated(page, displayScale, rotation);
    }
}
