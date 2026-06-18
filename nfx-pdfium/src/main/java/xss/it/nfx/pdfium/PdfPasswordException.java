package xss.it.nfx.pdfium;

/**
 * Thrown when a document cannot be opened because it is password-protected and
 * no password (or an incorrect one) was supplied.
 *
 * <p>Distinct from a generic {@link PdfException} so callers can tell "needs a
 * password" apart from "corrupt/unsupported" and react by prompting the user.</p>
 *
 * @author XDSSWAR
 */
public class PdfPasswordException extends PdfException {

    /**
     * Creates a new exception with the given message.
     *
     * @param message the detail message
     */
    public PdfPasswordException(String message) {
        super(message);
    }
}
