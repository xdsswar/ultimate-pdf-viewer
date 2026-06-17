package com.xss.it.nfx.pdfium.ffm;

/**
 * Unchecked exception for native PDFium failures. Internal API.
 *
 * @author XDSSWAR
 */
public final class PdfiumException extends RuntimeException {
    public PdfiumException(String message) {
        super(message);
    }

    public PdfiumException(String message, Throwable cause) {
        super(message, cause);
    }
}
