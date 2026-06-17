# nfx-pdfium

A standalone, cross-platform **JavaFX PDF library** powered by a native
[PDFium](https://pdfium.googlesource.com/pdfium/) engine, bound through the Java
**Foreign Function & Memory API** (Panama) plus a thin C shim. No AWT/Swing, no
PDFBox — crisp, pixel-perfect rendering with text extraction, search, selection
and an extensible page node.

- **Public API:** `xss.it.nfx.pdfium.*`
- **Internal (never exported):** `com.xss.it.nfx.pdfium.*`
- **JPMS module:** `nfx.pdfium` — run apps with `--enable-native-access=nfx.pdfium`
- **Requires:** JDK 22+ (developed on 25), JavaFX 25

## Quick start

### Render / read / search (headless-capable)

```java
import xss.it.nfx.pdfium.*;
import xss.it.nfx.pdfium.text.*;
import javafx.scene.image.Image;

try (PdfDocument doc = PdfDocument.open(Path.of("file.pdf"))) {
    int pages = doc.getPageCount();
    PdfPage page = doc.getPage(0);

    Image image = page.render(2.0, 0);          // 144 DPI, no rotation
    String text = page.getText();
    List<PdfTextChar> chars = page.getChars();  // glyph boxes (points, top-left)
    List<PdfSearchResult> hits = doc.search("invoice");
}
```

### Encrypted documents

```java
PdfDocument doc = PdfDocument.open(file, attempt ->
        promptForPassword(attempt == 0 ? "Password:" : "Wrong password, try again:"));
// return null from the callback to cancel
```

### The reactive node

```java
import xss.it.nfx.pdfium.scene.PdfPageView;

PdfPageView view = new PdfPageView();
view.setDocument(doc);
view.setPageIndex(0);
view.zoomProperty().bind(zoomSlider.valueProperty()); // stays pixel-perfect
// text selection + Ctrl+C copy + Ctrl+A select-all are built in
// search highlights:
view.getHighlights().setAll(doc.search("total"));
```

CSS-styleable colors:

```css
.pdf-page-view {
    -fx-page-background: white;
    -fx-selection-color: rgba(51,153,255,0.4);
    -fx-highlight-color: rgba(255,210,0,0.5);
}
```

### Custom overlays (annotations, drawing, …)

Subclass `PdfLayer` and add it via `view.getLayers().add(myLayer)`. Each layer is
notified through `onPageUpdated(page, displayScale, rotation)` and positions its
content in display coordinates (`pagePoint * displayScale`).

## Public API map

| Package | Types |
|---|---|
| `xss.it.nfx.pdfium` | `PdfDocument`, `PdfPage`, `PdfPasswordCallback`, `PdfException` |
| `xss.it.nfx.pdfium.text` | `PdfTextChar`, `PdfSearchResult` |
| `xss.it.nfx.pdfium.render` | `PdfRenderer` |
| `xss.it.nfx.pdfium.scene` | `PdfPageView`, `PdfLayer` |

## Memory & threading

- **No manual cleanup needed.** Native handles are freed automatically by a
  `Cleaner` when documents/pages become unreachable; render buffers use an
  auto (GC-managed) `Arena`. `close()` exists for eager, deterministic release.
- PDFium is not thread-safe; all native calls are serialized internally.
  `PdfPageView` renders off the FX thread on a configurable `Executor`.

## Native build

PDFium binaries are vendored (prebuilt, from
[bblanchon/pdfium-binaries](https://github.com/bblanchon/pdfium-binaries)); only
the thin shim is compiled per platform via CMake.

```bash
./gradlew :nfx-pdfium:fetchPdfium   # vendor PDFium for all platforms -> third_party/
./gradlew :nfx-pdfium:buildNative   # build + stage the shim for the host OS
./gradlew :nfx-pdfium:buildAll      # native + jar
./gradlew :nfx-pdfium:smoke         # headless render/text/search check (uses ../demo.pdf)
```

Bundled libraries (shim + PDFium) are packaged under `/native/<os>-<arch>/` and
extracted at first run to `~/.nfx-pdfium/natives/<version>/<platform>/` (only if
missing). Toolchains: Windows (MSVC or MinGW gcc), Linux (gcc), macOS (clang,
x64 + arm64). The shim must be compiled on each target OS — see the CI matrix in
`.github/workflows/native.yml`.

## Roadmap

- Concrete `DrawLayer` for freehand/shape drawing on the view.
- Annotation **editing** persisted into the PDF (needs a native write surface:
  `FPDFAnnot` / `FPDFPageObj` / `FPDF_SaveAsCopy`).
