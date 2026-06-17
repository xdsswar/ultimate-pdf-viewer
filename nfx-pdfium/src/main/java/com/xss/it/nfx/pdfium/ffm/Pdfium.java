package com.xss.it.nfx.pdfium.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

/**
 * Entry point for the native PDFium engine. Internal API.
 *
 * <p>Opening a document copies its bytes into native memory (PDFium keeps no
 * reference to the Java array), so the caller may discard the source buffer
 * immediately. The returned {@link PdfiumDocument} owns the native handle; its
 * memory is released automatically when it becomes unreachable (a Cleaner runs
 * the native close), or eagerly via {@link PdfiumDocument#close()}.</p>
 *
 * @author XDSSWAR
 */
public final class Pdfium {

    /** PDFium error code returned when a document needs a (correct) password. */
    public static final int ERR_PASSWORD = 4;

    private Pdfium() {
    }

    /** Triggers native library loading + initialization eagerly (optional). */
    public static void init() {
        PdfiumLibrary.ensureLoaded();
    }

    /**
     * Attempts to open a document, returning {@code null} on failure instead of
     * throwing — pair with {@link #lastError()} to tell a wrong/missing password
     * ({@link #ERR_PASSWORD}) apart from a corrupt file.
     *
     * @param bytes    the raw PDF bytes
     * @param password the password, or {@code null}
     * @return the open document, or {@code null} if it could not be opened
     */
    public static PdfiumDocument tryOpen(byte[] bytes, String password) {
        if (bytes == null || bytes.length == 0) {
            throw new PdfiumException("Empty PDF buffer");
        }
        PdfiumLibrary.ensureLoaded();
        synchronized (PdfiumLibrary.LOCK) {
            // Confined arena just for the open call; the shim copies the bytes.
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment data = arena.allocate(bytes.length);
                MemorySegment.copy(bytes, 0, data, JAVA_BYTE, 0, bytes.length);
                MemorySegment pwd = password == null
                        ? MemorySegment.NULL
                        : arena.allocateFrom(password);
                MemorySegment handle = (MemorySegment) PdfiumLibrary.PV_OPEN_MEMORY
                        .invokeExact(data, (long) bytes.length, pwd);
                return handle.address() == 0L ? null : new PdfiumDocument(handle);
            } catch (Throwable t) {
                throw new PdfiumException("pv_open_memory failed", t);
            }
        }
    }

    /**
     * The last PDFium error code (see {@link #ERR_PASSWORD}).
     *
     * @return the error code from the most recent native open
     */
    public static int lastError() {
        PdfiumLibrary.ensureLoaded();
        synchronized (PdfiumLibrary.LOCK) {
            try {
                return (int) PdfiumLibrary.PV_LAST_ERROR.invokeExact();
            } catch (Throwable t) {
                throw new PdfiumException("pv_last_error failed", t);
            }
        }
    }

    /**
     * Opens a document, throwing if it cannot be opened.
     *
     * @param bytes    the raw PDF bytes
     * @param password the password, or {@code null}
     * @return the open document
     * @throws PdfiumException if the document is corrupt or the password is wrong
     */
    public static PdfiumDocument open(byte[] bytes, String password) {
        PdfiumDocument doc = tryOpen(bytes, password);
        if (doc == null) {
            int err = lastError();
            throw new PdfiumException(err == ERR_PASSWORD
                    ? "PDFium failed to open document: password required or incorrect"
                    : "PDFium failed to open document (corrupt or unsupported), error " + err);
        }
        return doc;
    }

    /** Opens a document from a byte array with no password. */
    public static PdfiumDocument open(byte[] bytes) {
        return open(bytes, null);
    }
}
