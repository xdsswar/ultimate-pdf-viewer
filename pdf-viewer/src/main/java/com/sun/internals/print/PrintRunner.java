package com.sun.internals.print;

import com.sun.internals.AbstractViewer;
import com.sun.internals.print.PrintSettings.ColorMode;
import com.sun.internals.print.PrintSettings.MarginMode;
import com.sun.internals.print.SheetComposer.Banner;
import javafx.application.Platform;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrintColor;
import javafx.print.PrintSides;
import javafx.print.Printer;
import javafx.print.PrinterAttributes;
import javafx.print.PrinterJob;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import xss.it.nfx.pdfium.PdfDocument;
import xss.it.nfx.pdfium.PdfPage;
import xss.it.nfx.pdfium.render.PdfRenderer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Runs a print job from a {@link PrintSettings} snapshot using the JavaFX
 * {@link PrinterJob}.
 *
 * <p>Pages are grouped into sheets (honoring pages-per-sheet) and rasterized off
 * the FX thread one sheet at a time so memory stays bounded; each sheet is then
 * composed with {@link SheetComposer} — the very node the dialog preview shows —
 * and printed on the FX thread. Choosing a virtual "Microsoft Print to PDF"
 * (or "Adobe PDF") destination is how the dialog's "Save as PDF" works; no
 * separate PDF writer is needed.</p>
 *
 * @author XDSSWAR
 */
public final class PrintRunner {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    /**
     * A dedicated single daemon thread for print jobs, so a long job never ties up
     * the viewer's shared render pool (which serves the live preview and thumbnails).
     */
    private static final ExecutorService PRINT_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "nfx-print");
                t.setDaemon(true);
                return t;
            });

    private PrintRunner() {
    }

    /**
     * Builds the page layout for the given destination and settings. Shared with
     * the dialog preview so the on-screen sheet matches the printed one exactly.
     *
     * @param printer  the destination printer
     * @param settings the print settings
     * @return the page layout (margins, orientation and printable area)
     */
    public static PageLayout pageLayout(Printer printer, PrintSettings settings) {
        if (printer == null) {
            return null;
        }
        Paper paper = settings.paper() != null ? settings.paper() : Paper.NA_LETTER;
        PageOrientation orientation = settings.orientation() != null
                ? settings.orientation() : PageOrientation.PORTRAIT;
        return switch (settings.marginMode()) {
            case MINIMUM -> printer.createPageLayout(paper, orientation,
                    Printer.MarginType.HARDWARE_MINIMUM);
            case NONE -> printer.createPageLayout(paper, orientation, 0, 0, 0, 0);
            case CUSTOM -> {
                PrintSettings.Margins m = settings.customMargins();
                yield printer.createPageLayout(paper, orientation,
                        m.left() * 72, m.right() * 72, m.top() * 72, m.bottom() * 72);
            }
            default -> printer.createPageLayout(paper, orientation, Printer.MarginType.DEFAULT);
        };
    }

    /**
     * The number of physical sheets the settings will produce (the dialog's
     * "N sheets of paper" figure): page-groups, halved for two-sided, times the
     * copy count (copies are ignored for "Save as PDF").
     *
     * @param settings the print settings
     * @return the sheet count (&gt;= 0)
     */
    public static int sheetCount(PrintSettings settings) {
        int per = Math.max(1, settings.pagesPerSheet());
        int groups = (settings.pages().size() + per - 1) / per;
        if (settings.twoSided()) {
            groups = (groups + 1) / 2;
        }
        int copies = settings.isSaveAsPdf() ? 1 : Math.max(1, settings.copies());
        return groups * copies;
    }

    /**
     * Prints according to the given settings.
     *
     * @param viewer    the owning viewer (for the document and DPI)
     * @param settings  the gathered print settings
     * @param onSuccess run on the FX thread when the whole job finishes
     * @param onError   run on the FX thread if the job fails
     * @return a cancel action — run it (on the FX thread) to abort the job, halting
     *         further rendering and the spool — or {@code null} if it never started
     */
    public static Runnable run(AbstractViewer viewer, PrintSettings settings,
                               Runnable onSuccess, Consumer<Throwable> onError) {
        if (viewer.getDocument() == null) {
            onError.accept(new IllegalStateException("No document to print"));
            return null;
        }
        Printer printer = settings.printer();
        if (printer == null) {
            onError.accept(new IllegalStateException("No printer selected"));
            return null;
        }
        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job == null) {
            onError.accept(new IllegalStateException("Could not create a print job"));
            return null;
        }
        applyJobSettings(job, printer, settings);
        PageLayout layout = pageLayout(printer, settings);
        job.getJobSettings().setPageLayout(layout);

        PdfDocument pdf = viewer.getDocument().getPdfDocument();
        // Render at the viewer's page DPI (>= 72) so the print is crisp.
        double scale = Math.max(1.0, viewer.getPageRenderDpi() / 72.0);
        boolean landscape = settings.orientation() == PageOrientation.LANDSCAPE;
        boolean mono = settings.colorMode() == ColorMode.MONO;
        Color background = settings.backgroundGraphics() ? Color.WHITE : Color.TRANSPARENT;

        List<List<Integer>> sheets = SheetComposer.sheets(settings.pages(), settings.pagesPerSheet());
        int pageCount = pdf.getPageCount();
        String title = headerTitle(settings);
        String date = LocalDate.now().format(DATE);

        // Cancellation: the flag stops the render loop between pages/sheets, and
        // cancelJob() makes the next printPage fail fast — together they abort
        // promptly even mid-sheet, instead of rendering every remaining page.
        AtomicBoolean cancelled = new AtomicBoolean();
        try {
            PRINT_EXECUTOR.execute(() -> runJob(job, layout, sheets, pdf, pageCount, scale,
                    background, mono, landscape, title, date, settings, cancelled, onSuccess, onError));
        } catch (RuntimeException e) {
            // Executor rejected the task (e.g. JVM shutting down) — report, don't throw.
            safeCancel(job);
            onError.accept(e);
            return null;
        }
        return () -> {
            cancelled.set(true);
            safeCancel(job);
        };
    }

    /** Cancels a job, swallowing any driver exception. */
    private static void safeCancel(PrinterJob job) {
        try {
            job.cancelJob();
        } catch (RuntimeException ignored) {
            // already finishing or unsupported; nothing more to do
        }
    }

    /** The body of the print job, run on the dedicated print thread. */
    private static void runJob(PrinterJob job, PageLayout layout, List<List<Integer>> sheets,
                               PdfDocument pdf, int pageCount, double scale, Color background,
                               boolean mono, boolean landscape, String title, String date,
                               PrintSettings settings, AtomicBoolean cancelled,
                               Runnable onSuccess, Consumer<Throwable> onError) {
            try {
                for (int s = 0; s < sheets.size(); s++) {
                    if (cancelled.get()) {
                        return;     // aborted by the dialog — job already cancelled
                    }
                    List<Integer> sheet = sheets.get(s);
                    List<Image> images = new ArrayList<>(sheet.size());
                    for (int index : sheet) {
                        if (cancelled.get()) {
                            return; // bail between pages so an N-up sheet stops promptly
                        }
                        if (index < 0 || index >= pageCount) {
                            continue;
                        }
                        PdfPage page = pdf.getPage(index);
                        Image image = PdfRenderer.render(page, scale, 0, background);
                        images.add(mono ? ImageFx.toGrayscale(image) : image);
                    }
                    Banner banner = settings.headersFooters()
                            ? banner(title, date, sheet, s, sheets.size(), pageCount, settings)
                            : null;
                    int sheetNo = s + 1;
                    runAndWait(() -> printSheet(job, layout, images, settings, landscape, banner));
                    if (sheetNo % 25 == 0) {
                        // Yield occasionally on huge jobs (no-op for small ones).
                        Thread.yield();
                    }
                }
                runAndWait(job::endJob);
                Platform.runLater(onSuccess);
            } catch (Throwable t) {
                if (cancelled.get()) {
                    return;         // failure is the cancellation we requested
                }
                safeCancel(job);
                Platform.runLater(() -> onError.accept(t));
            }
    }

    /** Applies copies, color and two-sided to the job (guarded by printer support). */
    private static void applyJobSettings(PrinterJob job, Printer printer, PrintSettings settings) {
        job.getJobSettings().setCopies(settings.isSaveAsPdf() ? 1 : Math.max(1, settings.copies()));
        PrinterAttributes attr = printer.getPrinterAttributes();
        try {
            job.getJobSettings().setPrintColor(settings.colorMode() == ColorMode.MONO
                    ? PrintColor.MONOCHROME : PrintColor.COLOR);
        } catch (RuntimeException ignored) {
            // driver doesn't expose color control; the grayscale render still applies
        }
        if (settings.twoSided() && attr != null && !attr.getSupportedPrintSides().isEmpty()) {
            try {
                job.getJobSettings().setPrintSides(PrintSides.DUPLEX);
            } catch (RuntimeException ignored) {
                // duplex unsupported after all; print single-sided
            }
        }
    }

    /** Composes one sheet to the printable area and sends it to the printer. */
    private static void printSheet(PrinterJob job, PageLayout layout, List<Image> images,
                                   PrintSettings settings, boolean landscape, Banner banner) {
        double availW = layout.getPrintableWidth();
        double availH = layout.getPrintableHeight();
        Region content = SheetComposer.buildContent(availW, availH, images,
                settings.scaleMode(), settings.customScalePercent(),
                settings.pagesPerSheet(), landscape, settings.headersFooters(), banner);
        content.resize(availW, availH);
        content.layout();
        if (!job.printPage(layout, content)) {
            throw new IllegalStateException("The printer rejected a page");
        }
    }

    /** Builds the header/footer text for a sheet. */
    private static Banner banner(String title, String date, List<Integer> sheet, int sheetIndex,
                                 int sheetTotal, int pageCount, PrintSettings settings) {
        String footerRight;
        if (settings.pagesPerSheet() == 1 && !sheet.isEmpty()) {
            footerRight = "page " + (sheet.get(0) + 1) + " / " + pageCount;
        } else {
            footerRight = "sheet " + (sheetIndex + 1) + " / " + sheetTotal;
        }
        return new Banner(title, date, null, footerRight);
    }

    /** The header title: the document title, falling back to a generic label. */
    private static String headerTitle(PrintSettings settings) {
        String t = settings.documentTitle();
        return t == null || t.isBlank() ? "Document" : t;
    }

    /** Runs the action on the FX thread and blocks until it completes. */
    private static void runAndWait(Runnable action) throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = new Throwable[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        if (error[0] != null) {
            // Preserve the message AND cause so the dialog can show a real reason
            // (not "null") and the stack trace points at the actual failure.
            throw new RuntimeException(error[0].getMessage(), error[0]);
        }
    }
}
