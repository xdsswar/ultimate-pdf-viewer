/**
 * @author XDSSWAR
 * Created on 01/27/2024
 */
module ultimate.pdf {

    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    // Public API exposes xss.it.nfx.pdfium.PdfDocument, so re-export it.
    requires transitive nfx.pdfium;

    exports xss.it.ultimate.pdf.viewer.enums;
    opens xss.it.ultimate.pdf.viewer.enums;
    opens xss.it.ultimate.pdf.viewer.controls;
    exports xss.it.ultimate.pdf.viewer.controls;
    opens xss.it.ultimate.pdf.viewer.text;
    exports xss.it.ultimate.pdf.viewer.text;


    exports xss.it.ultimate.pdf.viewer;
    opens xss.it.ultimate.pdf.viewer;
}
