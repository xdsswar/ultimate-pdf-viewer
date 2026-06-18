package com.sun.internals.print;

import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;

import java.util.List;

/**
 * Immutable snapshot of every choice made in the print dialog, handed to
 * {@link PrintRunner} to execute a job and used by the dialog's own preview to
 * compose a what-you-see-is-what-you-get sheet. Mirrors the controls of the
 * Chrome print dialog (destination, pages, copies, layout, color, paper size,
 * pages-per-sheet, margins, scale, two-sided, headers/footers, background
 * graphics).
 *
 * @param printer            the destination printer
 * @param pages              zero-based page indices to print, already filtered
 *                           for the Pages mode (All / Odd / Even / Custom)
 * @param copies             number of copies (&gt;= 1)
 * @param orientation        portrait or landscape paper orientation
 * @param colorMode          color or black-and-white output
 * @param paper              the paper size
 * @param marginMode         how page margins are derived
 * @param customMargins      custom margins in inches (used when {@code marginMode}
 *                           is {@link MarginMode#CUSTOM}); never {@code null}
 * @param scaleMode          how each page is scaled into its cell
 * @param customScalePercent custom scale percentage (used when {@code scaleMode}
 *                           is {@link ScaleMode#CUSTOM})
 * @param pagesPerSheet      N-up count (1, 2, 4, 6, 9 or 16)
 * @param twoSided           {@code true} to request duplex printing
 * @param headersFooters     {@code true} to draw title/date and page-number bands
 * @param backgroundGraphics {@code true} to keep page background fills (best-effort)
 * @param documentTitle      title used in the header band (may be {@code null})
 *
 * @author XDSSWAR
 */
public record PrintSettings(
        Printer printer,
        List<Integer> pages,
        int copies,
        PageOrientation orientation,
        ColorMode colorMode,
        Paper paper,
        MarginMode marginMode,
        Margins customMargins,
        ScaleMode scaleMode,
        double customScalePercent,
        int pagesPerSheet,
        boolean twoSided,
        boolean headersFooters,
        boolean backgroundGraphics,
        String documentTitle) {

    /** Output color mode. */
    public enum ColorMode {
        /** Full color. */
        COLOR,
        /** Black and white (rendered images are converted to grayscale). */
        MONO
    }

    /** How page margins are derived for the page layout. */
    public enum MarginMode {
        /** The printer's default margins. */
        DEFAULT,
        /** No margins (clamped to the hardware minimum). */
        NONE,
        /** The smallest margins the hardware supports. */
        MINIMUM,
        /** Explicit margins from {@link #customMargins()}. */
        CUSTOM
    }

    /** How each page is scaled into its sheet cell. */
    public enum ScaleMode {
        /** Fit the page within the printable area, preserving aspect. */
        DEFAULT,
        /**
         * Fit the page within the printable area, preserving aspect (Chrome's
         * "Fit to printable area"). Equivalent to {@link #DEFAULT} in practice,
         * since the page is rendered at print resolution (always &ge; cell size).
         */
        FIT,
        /** Scale by {@link #customScalePercent()} relative to the fitted size. */
        CUSTOM
    }

    /**
     * Page margins in inches.
     *
     * @param top    top margin in inches
     * @param bottom bottom margin in inches
     * @param left   left margin in inches
     * @param right  right margin in inches
     */
    public record Margins(double top, double bottom, double left, double right) {
        /** A conventional one-inch margin on every edge. */
        public static final Margins ONE_INCH = new Margins(1, 1, 1, 1);
    }

    /**
     * Whether the destination is a "Save as PDF" style virtual printer (its name
     * mentions PDF). Such printers ignore copies and color, which the dialog
     * reflects by hiding those controls.
     *
     * <p>Relies on the OS exposing a PDF printer: Windows ("Microsoft Print to
     * PDF" / "Adobe PDF") and Linux ("Cups-PDF" / "Print to File (PDF)") do; macOS
     * surfaces "Save as PDF" only through its native print panel, not as a
     * {@link Printer}, so this returns {@code false} there.</p>
     *
     * @return {@code true} when the printer name contains "pdf"
     */
    public boolean isSaveAsPdf() {
        return printer != null && printer.getName() != null
                && printer.getName().toLowerCase().contains("pdf");
    }
}
