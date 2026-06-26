package xss.it.nfx.svg;

/**
 * Unchecked exception thrown when an SVG operation fails - parsing a malformed
 * document or rendering it.
 *
 * @author XDSSWAR
 */
public class SvgException extends RuntimeException {

    /**
     * Creates a new exception with the given message.
     *
     * @param message the detail message
     */
    public SvgException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public SvgException(String message, Throwable cause) {
        super(message, cause);
    }
}
