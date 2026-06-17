/**
 * nfx-pdfium — a standalone, cross-platform JavaFX PDF library backed by a
 * native PDFium engine (Foreign Function &amp; Memory API + a thin C shim).
 *
 * <p>Public API lives under {@code xss.it.nfx.pdfium}; the implementation under
 * {@code com.xss.it.nfx.pdfium} is never exported. The module loads native code
 * via the FFM API, so consumers must run with
 * {@code --enable-native-access=nfx.pdfium}.</p>
 *
 * @author XDSSWAR
 */
module nfx.pdfium {
    requires javafx.graphics;
    requires javafx.controls;

    // Public API, organized by category. Implementation packages
    // (com.xss.it.nfx.pdfium.*) are never exported.
    exports xss.it.nfx.pdfium;          // core: PdfDocument, PdfPage, password callback
    exports xss.it.nfx.pdfium.text;     // text model: PdfTextChar, PdfSearchResult
    exports xss.it.nfx.pdfium.render;   // PdfRenderer
    exports xss.it.nfx.pdfium.scene;    // JavaFX nodes: PdfPageView, PdfLayer
}
