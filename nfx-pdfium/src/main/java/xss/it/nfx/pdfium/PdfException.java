package xss.it.nfx.pdfium;

/**
 * Unchecked exception thrown when a PDF operation fails — opening a malformed or
 * password-protected document, rendering, or text extraction.
 *
 * @author XDSSWAR
 */
public final class PdfException extends RuntimeException {

    /**
     * Creates a new exception with the given message.
     *
     * @param message the detail message
     */
    public PdfException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public PdfException(String message, Throwable cause) {
        super(message, cause);
    }
}
