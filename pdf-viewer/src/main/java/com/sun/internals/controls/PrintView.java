package com.sun.internals.controls;

import com.sun.internals.AbstractViewer;
import com.sun.internals.flow.NfxCell;
import com.sun.internals.flow.NfxListView;
import com.sun.internals.print.PrintRunner;
import com.sun.internals.print.PrintSettings;
import com.sun.internals.print.PrintSettings.ColorMode;
import com.sun.internals.print.PrintSettings.MarginMode;
import com.sun.internals.print.PrintSettings.Margins;
import com.sun.internals.print.PrintSettings.ScaleMode;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterAttributes;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import xss.it.nfx.pdfium.PdfDocument;
import xss.it.nfx.pdfium.scene.PdfPageView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A print dialog modeled on Google Chrome's current print preview, shown centered
 * in an {@link OverlayPane}.
 *
 * <p>Left: a continuous, virtualized preview of the selected pages, each rendered
 * live by the engine's {@code PdfPageView} (real PDFium) inside an
 * {@code NfxListView}, so only the pages near the viewport are ever materialized —
 * memory stays bounded regardless of document size. Right: a settings panel with
 * Destination, Pages (All / Odd / Even / Custom), Copies, Layout, Color and a
 * collapsible "More settings" section (Paper size, Pages per sheet, Margins, Scale,
 * Two-sided and Options), plus Print/Save and Cancel. "Save as PDF" is achieved by
 * picking a virtual PDF printer (e.g. "Microsoft Print to PDF").</p>
 *
 * <p>The preview shows real 1-up pages fit to the chosen paper/orientation/margins;
 * N-up, scale percentage and header/footer bands are applied at print time only
 * (see {@code PrintRunner}/{@code SheetComposer}).</p>
 *
 * @author XDSSWAR
 */
public final class PrintView extends HBox {

    /** Upper bounds so the dialog stays compact on large screens. */
    private static final double MAX_WIDTH = 1120;
    private static final double MAX_HEIGHT = 860;

    /** Settings-panel width. */
    private static final double PANEL_MIN_WIDTH = 300;
    private static final double PANEL_PREF_WIDTH = 320;

    /** Default custom margin (inches) seeded into the custom-margin fields. */
    private static final double DEFAULT_CUSTOM_MARGIN = 0.5;

    private final AbstractViewer viewer;
    private final Runnable onClose;
    private final PdfDocument pdf;
    private final int pageCount;
    private final String documentTitle;

    private final NfxListView<PreviewItem> preview = new NfxListView<>();
    private final Label placeholder = new Label();
    private PrintSettings previewSettings;
    private double lastCellHeight = -1;
    private long previewGen;
    private final Label sheetsLabel = new Label();
    private final Label statusLabel = new Label();

    private final ComboBox<Printer> destination = new ComboBox<>();
    private final ComboBox<String> pagesMode = new ComboBox<>();
    private final TextField rangeField = new TextField();
    private final Spinner<Integer> copies = new Spinner<>(1, 999, 1);
    private final ComboBox<String> layout = new ComboBox<>();
    private final ComboBox<String> color = new ComboBox<>();
    private final ComboBox<Paper> paper = new ComboBox<>();
    private final ComboBox<Integer> perSheet = new ComboBox<>();
    private final ComboBox<String> margins = new ComboBox<>();
    private final ComboBox<String> scale = new ComboBox<>();
    private final TextField scalePercent = new TextField("100");
    private final TextField marginTop = new TextField();
    private final TextField marginBottom = new TextField();
    private final TextField marginLeft = new TextField();
    private final TextField marginRight = new TextField();
    private final CheckBox twoSided = new CheckBox("Print on both sides");
    private final CheckBox headersFooters = new CheckBox("Headers and footers");
    private final CheckBox backgroundGraphics = new CheckBox("Background graphics");

    private VBox copiesRow;
    private VBox rangeRow;
    private VBox customMarginsRow;
    private VBox scalePercentRow;
    private VBox twoSidedRow;

    private final Button primary = new Button("Print");
    private final Button cancel = new Button("Cancel");

    /** Guards setting-listeners from firing refreshes before the UI is built. */
    private boolean ready;

    /** Set once the dialog is closing, so async print callbacks become no-ops. */
    private boolean closed;

    /** Whether the dynamic cell-height inline style has been installed on the list. */
    private boolean inlineApplied;

    /** Coalesces rapid width changes (window resize) into a single height update. */
    private final PauseTransition cellHeightDebounce = new PauseTransition(Duration.millis(140));

    /** The most recently computed target row height, applied when the resize settles. */
    private double pendingCellH = -1;

    /** Cancel action for the in-flight print job (aborts rendering + spool), or null. */
    private Runnable activeCanceller;

    /**
     * Builds the print dialog for a viewer.
     *
     * @param viewer  the owning viewer (provides the document, executor, DPI)
     * @param onClose dismiss callback (typically {@code overlay::hide})
     */
    public PrintView(AbstractViewer viewer, Runnable onClose) {
        this.viewer = viewer;
        this.onClose = onClose;
        PdfDocument doc = viewer.getDocument() != null ? viewer.getDocument().getPdfDocument() : null;
        if (doc == null) {
            throw new IllegalStateException("PrintView requires a loaded document");
        }
        this.pdf = doc;
        this.pageCount = pdf.getPageCount();
        this.documentTitle = resolveTitle(pdf);
        getStyleClass().add("pdf-print-dialog");
        setFocusTraversable(true);

        setupPreview();
        HBox.setHgrow(preview, Priority.ALWAYS);
        getChildren().addAll(preview, buildPanel());

        // Responsive: scale with the viewer up to a cap, shrink on small windows.
        parentProperty().addListener((o, old, parent) -> {
            if (parent instanceof Region r) {
                prefWidthProperty().bind(Bindings.min(r.widthProperty().subtract(48), MAX_WIDTH));
                prefHeightProperty().bind(Bindings.min(r.heightProperty().subtract(48), MAX_HEIGHT));
                maxWidthProperty().bind(prefWidthProperty());
                maxHeightProperty().bind(prefHeightProperty());
                Platform.runLater(this::requestFocus);
            }
            if (parent == null) {
                // Unbind so the listeners we registered on the (long-lived) overlay's
                // width/height properties are released — otherwise the closed dialog
                // (and its document) leaks until the overlay is next resized.
                prefWidthProperty().unbind();
                prefHeightProperty().unbind();
                maxWidthProperty().unbind();
                maxHeightProperty().unbind();
                closed = true;
                cancelActiveJob();
                previewDispose();
            }
        });

        // ESC closes the dialog (the scrim click-out also works).
        addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });

        ready = true;
        Printer initial = Printer.getDefaultPrinter();
        if (initial == null && !destination.getItems().isEmpty()) {
            initial = destination.getItems().get(0);
        }
        destination.setValue(initial);
        onDestinationChanged(initial);
    }

    /* ------------------------------------------------------------- panel */

    /** Builds the right-hand settings panel. */
    private VBox buildPanel() {
        Label title = new Label("Print");
        title.getStyleClass().add("pdf-print-title");
        sheetsLabel.getStyleClass().add("pdf-print-count");
        VBox header = new VBox(2, title, sheetsLabel);
        header.getStyleClass().add("pdf-print-header");

        // Destination.
        destination.setConverter(converter(p -> p == null ? "" : p.getName()));
        destination.setItems(FXCollections.observableArrayList(Printer.getAllPrinters()));
        destination.setMaxWidth(Double.MAX_VALUE);
        destination.valueProperty().addListener((o, a, b) -> onDestinationChanged(b));

        // Pages.
        pagesMode.setItems(FXCollections.observableArrayList(
                "All", "Odd pages only", "Even pages only", "Custom"));
        pagesMode.setValue("All");
        pagesMode.setMaxWidth(Double.MAX_VALUE);
        rangeField.setPromptText("e.g. 1-5, 8, 11-13");
        rangeField.getStyleClass().add("pdf-print-range");
        pagesMode.valueProperty().addListener((o, a, mode) -> {
            setRowVisible(rangeRow, "Custom".equals(mode));
            refresh();
        });
        rangeField.textProperty().addListener((o, a, b) -> refresh());
        rangeRow = row("Pages range", rangeField);
        VBox pagesBox = new VBox(8, pagesMode, rangeRow);

        // Copies.
        copies.setEditable(true);
        copies.setMaxWidth(Double.MAX_VALUE);
        copies.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
        copies.valueProperty().addListener((o, a, b) -> refresh());
        copiesRow = row("Copies", copies);

        // Layout.
        layout.setItems(FXCollections.observableArrayList("Portrait", "Landscape"));
        layout.setValue("Portrait");
        layout.setMaxWidth(Double.MAX_VALUE);
        layout.valueProperty().addListener((o, a, b) -> refresh());

        // Color.
        color.setItems(FXCollections.observableArrayList("Color", "Black and white"));
        color.setValue("Color");
        color.setMaxWidth(Double.MAX_VALUE);
        color.valueProperty().addListener((o, a, b) -> refresh());

        VBox rows = new VBox(
                row("Destination", destination),
                row("Pages", pagesBox),
                copiesRow,
                row("Layout", layout),
                row("Color", color));
        rows.getStyleClass().add("pdf-print-rows");

        // More settings (collapsed by default).
        VBox more = buildMoreSettings();
        more.setManaged(false);
        more.setVisible(false);
        // Disclosure caret drawn by CSS (-fx-shape) rather than a literal glyph,
        // so it renders identically on every platform regardless of font coverage.
        Region moreArrow = new Region();
        moreArrow.getStyleClass().add("pdf-print-arrow");
        Button moreToggle = new Button("More settings", moreArrow);
        moreToggle.getStyleClass().add("pdf-print-more-toggle");
        moreToggle.setContentDisplay(ContentDisplay.RIGHT);
        moreToggle.setMaxWidth(Double.MAX_VALUE);
        moreToggle.setOnAction(e -> {
            boolean show = !more.isVisible();
            more.setManaged(show);
            more.setVisible(show);
            moreToggle.setText(show ? "Fewer settings" : "More settings");
            moreArrow.setRotate(show ? 180 : 0);   // caret points up when expanded
        });

        // Footer.
        primary.getStyleClass().add("pdf-modal-button");
        primary.setDefaultButton(true);
        primary.setOnAction(e -> doPrint());
        cancel.getStyleClass().add("pdf-modal-button-secondary");
        cancel.setOnAction(e -> close());
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(8, cancel, primary);
        footer.getStyleClass().add("pdf-print-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);
        statusLabel.getStyleClass().add("pdf-print-status");

        // Scrollable middle so expanding "More settings" scrolls instead of
        // growing the dialog (Chrome keeps the window fixed and scrolls).
        VBox middle = new VBox(16, rows, moreToggle, more);
        middle.getStyleClass().add("pdf-print-scroll-content");
        ScrollPane scroller = new ScrollPane(middle);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.getStyleClass().add("pdf-print-scroll");
        VBox.setVgrow(scroller, Priority.ALWAYS);

        VBox panel = new VBox(header, scroller, statusLabel, footer);
        panel.getStyleClass().add("pdf-print-panel");
        panel.setMinWidth(PANEL_MIN_WIDTH);
        panel.setPrefWidth(PANEL_PREF_WIDTH);
        panel.setMaxWidth(PANEL_PREF_WIDTH);
        return panel;
    }

    /** Builds the collapsible "More settings" controls. */
    private VBox buildMoreSettings() {
        // Paper size.
        paper.setConverter(converter(p -> p == null ? "" : p.getName()));
        paper.setMaxWidth(Double.MAX_VALUE);
        paper.valueProperty().addListener((o, a, b) -> refresh());

        // Pages per sheet (N-up).
        perSheet.setItems(FXCollections.observableArrayList(1, 2, 4, 6, 9, 16));
        perSheet.setValue(1);
        perSheet.setMaxWidth(Double.MAX_VALUE);
        perSheet.valueProperty().addListener((o, a, b) -> refresh());

        // Margins.
        margins.setItems(FXCollections.observableArrayList("Default", "None", "Minimum", "Custom"));
        margins.setValue("Default");
        margins.setMaxWidth(Double.MAX_VALUE);
        margins.valueProperty().addListener((o, a, mode) -> {
            setRowVisible(customMarginsRow, "Custom".equals(mode));
            refresh();
        });
        customMarginsRow = buildCustomMarginsRow();

        // Scale.
        scale.setItems(FXCollections.observableArrayList(
                "Default", "Fit to printable area", "Custom"));
        scale.setValue("Default");
        scale.setMaxWidth(Double.MAX_VALUE);
        scale.valueProperty().addListener((o, a, mode) -> {
            setRowVisible(scalePercentRow, "Custom".equals(mode));
            refresh();
        });
        scalePercent.getStyleClass().add("pdf-print-range");
        scalePercent.textProperty().addListener((o, a, b) -> refresh());
        scalePercentRow = row("Scale (%)", scalePercent);
        VBox scaleBox = new VBox(8, scale, scalePercentRow);

        // Two-sided.
        twoSided.getStyleClass().add("pdf-print-check");
        twoSided.selectedProperty().addListener((o, a, b) -> refresh());
        twoSidedRow = new VBox(twoSided);

        // Options.
        headersFooters.getStyleClass().add("pdf-print-check");
        headersFooters.selectedProperty().addListener((o, a, b) -> refresh());
        backgroundGraphics.getStyleClass().add("pdf-print-check");
        backgroundGraphics.selectedProperty().addListener((o, a, b) -> refresh());
        Label optionsLabel = new Label("Options");
        optionsLabel.getStyleClass().add("pdf-print-label");
        VBox optionsBox = new VBox(8, optionsLabel,
                new VBox(8, headersFooters, backgroundGraphics));

        VBox box = new VBox(
                row("Paper size", paper),
                row("Pages per sheet", perSheet),
                row("Margins", new VBox(8, margins, customMarginsRow)),
                row("Scale", scaleBox),
                twoSidedRow,
                optionsBox);
        box.getStyleClass().addAll("pdf-print-rows", "pdf-print-more");

        // Hidden sub-rows start collapsed.
        setRowVisible(rangeRow, false);
        setRowVisible(customMarginsRow, false);
        setRowVisible(scalePercentRow, false);
        return box;
    }

    /** A 2x2 set of inch fields for custom margins (Top/Bottom/Left/Right). */
    private VBox buildCustomMarginsRow() {
        String def = trim(DEFAULT_CUSTOM_MARGIN);
        marginTop.setText(def);
        marginBottom.setText(def);
        marginLeft.setText(def);
        marginRight.setText(def);
        for (TextField f : List.of(marginTop, marginBottom, marginLeft, marginRight)) {
            f.getStyleClass().add("pdf-print-range");
            f.textProperty().addListener((o, a, b) -> refresh());
        }
        HBox topRow = new HBox(8, marginField("Top", marginTop), marginField("Bottom", marginBottom));
        HBox bottomRow = new HBox(8, marginField("Left", marginLeft), marginField("Right", marginRight));
        VBox box = new VBox(8, topRow, bottomRow);
        box.getStyleClass().add("pdf-print-margins");
        return box;
    }

    /** A captioned mini field used inside the custom-margins grid. */
    private VBox marginField(String caption, TextField field) {
        field.setMaxWidth(Double.MAX_VALUE);
        Label label = new Label(caption);
        label.getStyleClass().add("pdf-print-sublabel");
        VBox box = new VBox(4, label, field);
        box.setMinWidth(0);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    /** A labeled settings row (caption above the control). */
    private VBox row(String caption, Region control) {
        Label label = new Label(caption);
        label.getStyleClass().add("pdf-print-label");
        VBox box = new VBox(6, label, control);
        box.getStyleClass().add("pdf-print-row");
        return box;
    }

    /** Shows or hides a sub-row (both managed and visible). */
    private static void setRowVisible(Region row, boolean show) {
        if (row != null) {
            row.setManaged(show);
            row.setVisible(show);
        }
    }

    /* ------------------------------------------------------------- logic */

    /** Refreshes the paper list, copies/two-sided visibility and button label. */
    private void onDestinationChanged(Printer printer) {
        if (printer == null) {
            paper.setItems(FXCollections.observableArrayList());
            primary.setDisable(true);
            refresh();
            return;
        }
        PrinterAttributes attr = printer.getPrinterAttributes();
        List<Paper> papers = attr != null
                ? new ArrayList<>(attr.getSupportedPapers()) : new ArrayList<>();
        paper.setItems(FXCollections.observableArrayList(papers));
        paper.setValue(attr != null ? attr.getDefaultPaper()
                : (papers.isEmpty() ? null : papers.get(0)));

        boolean pdfPrinter = printer.getName() != null
                && printer.getName().toLowerCase().contains("pdf");
        primary.setText(pdfPrinter ? "Save" : "Print");
        // Chrome hides Copies for "Save as PDF" and Two-sided when unsupported.
        setRowVisible(copiesRow, !pdfPrinter);
        boolean duplex = attr != null && !attr.getSupportedPrintSides().isEmpty();
        setRowVisible(twoSidedRow, duplex);
        if (!duplex) {
            twoSided.setSelected(false);
        }
        refresh();
    }

    /** The currently selected zero-based page indices (empty if none/invalid). */
    private List<Integer> currentPages() {
        String mode = pagesMode.getValue();
        if ("Custom".equals(mode)) {
            return parseRange(rangeField.getText(), pageCount);
        }
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            boolean odd = (i % 2) == 0;          // page 1 (index 0) is odd
            if ("Odd pages only".equals(mode) && !odd) {
                continue;
            }
            if ("Even pages only".equals(mode) && odd) {
                continue;
            }
            result.add(i);
        }
        return result;
    }

    /** The paper to use, falling back to the printer default then to Letter. */
    private Paper currentPaper() {
        Paper selected = paper.getValue();
        if (selected != null) {
            return selected;
        }
        Printer printer = destination.getValue();
        if (printer != null && printer.getPrinterAttributes() != null) {
            Paper def = printer.getPrinterAttributes().getDefaultPaper();
            if (def != null) {
                return def;
            }
        }
        return Paper.NA_LETTER;
    }

    /** Gathers the current control values into an immutable settings snapshot. */
    private PrintSettings buildSettings() {
        PageOrientation orientation = "Landscape".equals(layout.getValue())
                ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT;
        ColorMode colorMode = "Black and white".equals(color.getValue())
                ? ColorMode.MONO : ColorMode.COLOR;
        MarginMode marginMode = switch (margins.getValue() == null ? "Default" : margins.getValue()) {
            case "None" -> MarginMode.NONE;
            case "Minimum" -> MarginMode.MINIMUM;
            case "Custom" -> MarginMode.CUSTOM;
            default -> MarginMode.DEFAULT;
        };
        ScaleMode scaleMode = switch (scale.getValue() == null ? "Default" : scale.getValue()) {
            case "Fit to printable area" -> ScaleMode.FIT;
            case "Custom" -> ScaleMode.CUSTOM;
            default -> ScaleMode.DEFAULT;
        };
        Margins custom = new Margins(
                parseInches(marginTop), parseInches(marginBottom),
                parseInches(marginLeft), parseInches(marginRight));
        return new PrintSettings(
                destination.getValue(), currentPages(),
                copies.getValue() == null ? 1 : copies.getValue(),
                orientation, colorMode, currentPaper(), marginMode, custom,
                scaleMode, parsePercent(scalePercent), perSheet.getValue() == null ? 1 : perSheet.getValue(),
                twoSided.isSelected(), headersFooters.isSelected(),
                backgroundGraphics.isSelected(), documentTitle);
    }

    /** Rebuilds the preview and the "N sheets of paper" figure. */
    private void refresh() {
        if (!ready || closed) {
            return;
        }
        PrintSettings settings = buildSettings();
        int sheets = PrintRunner.sheetCount(settings);
        sheetsLabel.setText(sheets == 1 ? "1 sheet of paper" : sheets + " sheets of paper");
        primary.setDisable(settings.pages().isEmpty() || settings.printer() == null);
        previewUpdate(settings);
    }

    /** Gathers the settings and starts the print job. */
    private void doPrint() {
        if (closed) {
            return;
        }
        commitCopies();
        PrintSettings settings = buildSettings();
        if (settings.pages().isEmpty() || settings.printer() == null) {
            return;
        }
        setBusy(true);
        activeCanceller = PrintRunner.run(viewer, settings,
                () -> {
                    activeCanceller = null;
                    if (closed) {
                        return;
                    }
                    setBusy(false);
                    close();
                },
                t -> {
                    activeCanceller = null;
                    if (closed) {
                        return;
                    }
                    setBusy(false);
                    statusLabel.setText("Print failed: " + messageOf(t));
                });
    }

    /** Marks the dialog closed (so async callbacks no-op), aborts any job, dismisses. */
    private void close() {
        closed = true;
        cancelActiveJob();
        onClose.run();
    }

    /** Aborts an in-flight print job (stops further rendering and the spool). */
    private void cancelActiveJob() {
        if (activeCanceller != null) {
            activeCanceller.run();
            activeCanceller = null;
        }
    }

    /** Commits the editable copies spinner so a typed-but-uncommitted value is used. */
    private void commitCopies() {
        try {
            int v = Integer.parseInt(copies.getEditor().getText().trim());
            copies.getValueFactory().setValue(Math.max(1, Math.min(999, v)));
        } catch (NumberFormatException ignored) {
            // keep the last committed value
        }
    }

    /** Toggles the busy state while a job is in flight. */
    private void setBusy(boolean busy) {
        primary.setDisable(busy);
        cancel.setDisable(busy);
        destination.setDisable(busy);
        statusLabel.setText(busy ? "Printing…" : "");
    }

    /* ------------------------------------------------------------ helpers */

    private static String messageOf(Throwable t) {
        // Walk to the first cause carrying a real message (the executor wraps the
        // original failure), so the status shows a reason rather than "null".
        Throwable c = t;
        while ((c.getMessage() == null || c.getMessage().isBlank()) && c.getCause() != null) {
            c = c.getCause();
        }
        return c.getMessage() != null && !c.getMessage().isBlank()
                ? c.getMessage() : c.getClass().getSimpleName();
    }

    /** The document title from metadata, or {@code null} if unavailable. */
    private static String resolveTitle(PdfDocument pdf) {
        try {
            if (pdf.getMetadata() != null) {
                String t = pdf.getMetadata().title();
                if (t != null && !t.isBlank()) {
                    return t;
                }
            }
        } catch (RuntimeException ignored) {
            // metadata is optional; fall back to a generic header title
        }
        return null;
    }

    private static String trim(double inches) {
        return inches == Math.rint(inches)
                ? String.valueOf((int) inches) : String.valueOf(inches);
    }

    /** Parses an inch value from a field, clamped to a sane non-negative range. */
    private static double parseInches(TextField field) {
        try {
            return Math.max(0, Math.min(5, Double.parseDouble(decimal(field))));
        } catch (NumberFormatException e) {
            return DEFAULT_CUSTOM_MARGIN;
        }
    }

    /** Parses a scale percentage, clamped to Chrome's 10–200 range. */
    private static double parsePercent(TextField field) {
        try {
            return Math.max(10, Math.min(200, Double.parseDouble(decimal(field))));
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    /**
     * The field's text normalized for {@link Double#parseDouble} (which is locale
     * independent and only accepts a dot): a comma decimal separator — as typed in
     * many locales, e.g. {@code "0,5"} — is converted to a dot.
     */
    private static String decimal(TextField field) {
        return field.getText().trim().replace(',', '.');
    }

    /** A one-way {@link StringConverter} from a to-string function. */
    private static <T> StringConverter<T> converter(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return toText.apply(value);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }

    /**
     * Parses a page range like {@code "1-5, 8, 11-13"} (1-based, inclusive) into
     * sorted, de-duplicated zero-based indices clamped to {@code [0, count)}.
     * Returns an empty list when nothing valid is present.
     */
    static List<Integer> parseRange(String text, int count) {
        List<Integer> result = new ArrayList<>();
        if (text == null || text.isBlank() || count <= 0) {
            return result;
        }
        boolean[] picked = new boolean[count];
        for (String part : text.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            try {
                int dash = token.indexOf('-');
                if (dash >= 0) {
                    int from = Integer.parseInt(token.substring(0, dash).trim());
                    int to = Integer.parseInt(token.substring(dash + 1).trim());
                    if (from > to) {
                        int tmp = from;
                        from = to;
                        to = tmp;
                    }
                    for (int p = from; p <= to; p++) {
                        if (p >= 1 && p <= count) {
                            picked[p - 1] = true;
                        }
                    }
                } else {
                    int p = Integer.parseInt(token);
                    if (p >= 1 && p <= count) {
                        picked[p - 1] = true;
                    }
                }
            } catch (NumberFormatException ignored) {
                // skip malformed tokens; the rest of the range still applies
            }
        }
        for (int i = 0; i < count; i++) {
            if (picked[i]) {
                result.add(i);
            }
        }
        return result;
    }

    /* ----------------------------------------------------------- preview */

    /** Vertical gap between page sheets in the preview. */
    private static final double PREVIEW_GAP = 18;
    /** Horizontal inset on each side of a page sheet in the preview. */
    private static final double PREVIEW_SIDE_INSET = 28;

    /**
     * One preview row. The {@code gen} field changes whenever any setting changes,
     * so re-issuing the items forces every visible cell to rebind (and thus reflect
     * the new orientation, paper, margins or color).
     *
     * @param pageIndex zero-based page index to render
     * @param mono      whether to preview in black-and-white
     * @param gen       the settings generation this item was built for
     */
    private record PreviewItem(int pageIndex, boolean mono, long gen) {
    }

    /** Configures the virtualized {@link NfxListView} preview and its cell factory. */
    private void setupPreview() {
        preview.getStyleClass().add("pdf-print-list");
        preview.setLeftGap(0);
        preview.setRightGap(0);
        // The preview is read-only: swallow clicks so a cell never gets selected.
        // Selecting toggles a CSS pseudo-class, which kicks off a CSS pass that
        // reverts the list's cell height and rebinds every cell (a visible reload).
        // Wheel/drag scrolling is unaffected (those aren't MOUSE_CLICKED).
        preview.addEventFilter(MouseEvent.MOUSE_CLICKED, Event::consume);
        // Column count AND row height are applied as an inline style on the inner
        // list node (see recomputeCellHeight): inline styles outrank the list's own
        // stylesheet and survive every CSS pass, so a stray CSS re-resolve can't
        // revert the height and trigger a cell rebuild (which reloads the pages).
        preview.setMinHeight(0);
        preview.setMaxHeight(Double.MAX_VALUE);
        placeholder.getStyleClass().add("pdf-print-empty");
        placeholder.setText("No pages selected");
        preview.setPlaceHolder(placeholder);
        preview.setCellFactory(list -> new NfxCell<>(list) {
            private final SheetCell sheetCell = new SheetCell();

            {
                setGraphics(sheetCell);
            }

            @Override
            public void update(PreviewItem item) {
                super.update(item);
                if (item != null) {
                    sheetCell.bind(item.pageIndex(), item.mono());
                } else {
                    sheetCell.release();
                }
                setText(null);
            }
        });
        // The sheet height tracks the preview width (uniform paper => uniform rows).
        cellHeightDebounce.setOnFinished(e -> applyCellHeight());
        preview.widthProperty().addListener((o, a, b) -> recomputeCellHeight());
    }

    /** Pushes the current settings into the preview (items + uniform row height). */
    private void previewUpdate(PrintSettings s) {
        previewSettings = s;
        if (s == null || s.printer() == null || s.paper() == null || s.pages().isEmpty()) {
            placeholder.setText(s != null && s.printer() == null
                    ? "No printer available" : "No pages selected");
            preview.setItems(FXCollections.observableArrayList());
            return;
        }
        // Apply the (possibly new) row height FIRST, synchronously, so the item
        // rebuild below lands at the final height in one pass — applying it after
        // setItems would rebuild every visible page a second time (a visible flash).
        recomputeCellHeight(true);
        long gen = ++previewGen;
        boolean mono = s.colorMode() == ColorMode.MONO;
        ObservableList<PreviewItem> items = FXCollections.observableArrayList();
        for (int idx : s.pages()) {
            items.add(new PreviewItem(idx, mono, gen));
        }
        preview.setItems(items);
    }

    /** Detaches the preview from the document (frees native renders). */
    private void previewDispose() {
        previewSettings = null;
        cellHeightDebounce.stop();
        preview.setItems(FXCollections.observableArrayList());
    }

    /** Page width/height in points at the current orientation. */
    private double[] orientedPaper(PrintSettings s) {
        Paper p = s.paper();
        boolean landscape = s.orientation() == PageOrientation.LANDSCAPE;
        return new double[]{
                landscape ? p.getHeight() : p.getWidth(),
                landscape ? p.getWidth() : p.getHeight()};
    }

    /** Recomputes the uniform sheet (row) height for the current width and paper. */
    private void recomputeCellHeight() {
        recomputeCellHeight(false);          // width-resize path: debounced
    }

    /**
     * Recomputes the uniform sheet (row) height.
     *
     * @param immediate {@code true} for a deliberate change (settings / first show)
     *                  — apply now; {@code false} for a resize — debounce it
     */
    private void recomputeCellHeight(boolean immediate) {
        PrintSettings s = previewSettings;
        if (s == null || s.printer() == null || s.paper() == null) {
            return;
        }
        double[] pg = orientedPaper(s);
        if (pg[0] <= 0 || pg[1] <= 0) {
            return;
        }
        double w = preview.getWidth();
        if (w <= 0) {
            return;
        }
        // Approximate the cell width (list width less the scrollbar and side insets).
        double sheetW = Math.max(40, w - 16 - 2 * PREVIEW_SIDE_INSET);
        pendingCellH = sheetW * (pg[1] / pg[0]) + PREVIEW_GAP;
        if (immediate || !inlineApplied) {
            applyCellHeight();              // settings change / first install: now
        } else if (Math.abs(pendingCellH - lastCellHeight) > 0.5) {
            // Each cell-height change rebuilds the list's cells (reloading every
            // visible page), so coalesce a resize gesture into a single update once
            // the width stops changing — the cells just re-fit smoothly meanwhile.
            cellHeightDebounce.playFromStart();
        }
    }

    /** Applies the pending row height as an inline style (debounced on resize). */
    private void applyCellHeight() {
        double cellH = pendingCellH;
        if (cellH <= 0 || (inlineApplied && Math.abs(cellH - lastCellHeight) <= 0.5)) {
            return;
        }
        lastCellHeight = cellH;
        // Inline style (highest priority) so a CSS pass can't revert the height; the
        // single column is pinned separately by CSS (.pdf-print-list .nfx-list-view).
        // Falls back to the API before the skin's inner node exists; a later tick
        // installs the inline style (inlineApplied stays false until it does).
        Node listNode = preview.lookup(".nfx-list-view");
        if (listNode != null) {
            listNode.setStyle("-nfx-cell-height: " + cellH + ";");
            inlineApplied = true;
        } else {
            preview.setCellHeight(cellH);
        }
    }

    /**
     * A single preview cell: a white paper sheet holding one {@link PdfPageView},
     * fit to the current paper/orientation/margins. The page view holds a native
     * render only while the cell is on-screen — the list caches a cell per visited
     * page, so releasing the render when the cell scrolls off keeps memory bounded
     * on long documents (it re-renders when the cell scrolls back into view).
     */
    private final class SheetCell extends StackPane {

        private final StackPane sheet = new StackPane();
        private final PdfPageView pv = new PdfPageView();
        private int pageIndex = -1;
        private boolean boundMono;
        private double lastZoom = -1;

        SheetCell() {
            setAlignment(Pos.CENTER);
            setPadding(new Insets(PREVIEW_GAP / 2, PREVIEW_SIDE_INSET,
                    PREVIEW_GAP / 2, PREVIEW_SIDE_INSET));
            sheet.getStyleClass().add("pdf-print-page");
            sheet.setAlignment(Pos.CENTER);
            pv.setDpi(72);
            pv.setTextSelectable(false);
            pv.setContextMenuEnabled(false);
            sheet.getChildren().add(pv);
            getChildren().add(sheet);
            // On-screen → render; off-screen (removed from the flow) → free the render.
            sceneProperty().addListener((o, a, scene) -> materialize());
        }

        /** Points this cell at a page (in the requested color mode). */
        void bind(int index, boolean mono) {
            if (index == pageIndex && mono == boundMono) {
                return;          // already showing this page — no spurious re-render
            }
            this.pageIndex = index;
            this.boundMono = mono;
            pv.setEffect(mono ? new ColorAdjust(0, -1, 0, 0) : null);
            materialize();
            lastZoom = -1;
            requestLayout();
        }

        /** Renders only while in the scene; frees the native render when off-screen. */
        private void materialize() {
            if (getScene() != null && pageIndex >= 0 && pageIndex < pageCount) {
                pv.setDocument(pdf);
                pv.setPageIndex(pageIndex);
            } else {
                pv.setDocument(null);
            }
        }

        /** Detaches the page view from the document (frees its native render). */
        void release() {
            pageIndex = -1;
            pv.setDocument(null);
        }

        @Override
        protected void layoutChildren() {
            fit();
            super.layoutChildren();
        }

        /** Sizes the sheet to the paper aspect and fits the page within the margins. */
        private void fit() {
            PrintSettings s = previewSettings;
            if (s == null || s.printer() == null || s.paper() == null
                    || pageIndex < 0 || pageIndex >= pageCount) {
                return;
            }
            double[] pg = orientedPaper(s);
            if (pg[0] <= 0 || pg[1] <= 0) {
                return;
            }
            Insets in = getInsets();
            double availW = getWidth() - in.getLeft() - in.getRight();
            double availH = getHeight() - in.getTop() - in.getBottom();
            if (availW <= 1 || availH <= 1) {
                return;
            }
            double aspect = pg[1] / pg[0];
            double sheetW = availW;
            double sheetH = sheetW * aspect;
            if (sheetH > availH) {
                sheetH = availH;
                sheetW = sheetH / aspect;
            }
            sheet.setMinSize(sheetW, sheetH);
            sheet.setPrefSize(sheetW, sheetH);
            sheet.setMaxSize(sheetW, sheetH);

            double mL = 0;
            double mR = 0;
            double mT = 0;
            double mB = 0;
            PageLayout pl = PrintRunner.pageLayout(s.printer(), s);
            if (pl != null && pl.getPrintableWidth() > 0 && pl.getPrintableHeight() > 0) {
                mL = pl.getLeftMargin();
                mR = pl.getRightMargin();
                mT = pl.getTopMargin();
                mB = pl.getBottomMargin();
            }
            double sp = sheetW / pg[0];
            sheet.setPadding(new Insets(mT * sp, mR * sp, mB * sp, mL * sp));

            double availPW = Math.max(1, sheetW - (mL + mR) * sp);
            double availPH = Math.max(1, sheetH - (mT + mB) * sp);
            double pgW;
            double pgH;
            try {
                pgW = pdf.getPage(pageIndex).getWidth();
                pgH = pdf.getPage(pageIndex).getHeight();
            } catch (RuntimeException e) {
                return;                       // document changed/closed under us
            }
            if (pgW <= 0 || pgH <= 0) {
                return;
            }
            // The page view renders at DPI 72 (1 px per point at zoom 1).
            double zoom = Math.max(0.01, Math.min(availPW / pgW, availPH / pgH));
            if (Math.abs(zoom - lastZoom) > 0.0005) {
                lastZoom = zoom;
                pv.setRotation(0);
                pv.setZoom(zoom);
            }
        }
    }
}
