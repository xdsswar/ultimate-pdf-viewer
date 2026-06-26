package com.xss.it.nfx.svg.ffm;

/**
 * Unchecked exception for native Skia failures. Internal API.
 *
 * @author XDSSWAR
 */
public final class SvgFfmException extends RuntimeException {

    /**
     * Creates a new exception with the given message.
     *
     * @param message the detail message
     */
    public SvgFfmException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public SvgFfmException(String message, Throwable cause) {
        super(message, cause);
    }
}
