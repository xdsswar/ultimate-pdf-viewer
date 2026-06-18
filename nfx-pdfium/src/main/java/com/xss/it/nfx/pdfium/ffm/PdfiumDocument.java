package com.xss.it.nfx.pdfium.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.ref.Cleaner;

import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * An open PDF document backed by a native PDFium handle. Internal API.
 *
 * <p>Memory management is automatic: the native handle is registered with a
 * {@link Cleaner}, so it is released when this object becomes unreachable — no
 * manual cleanup is required. {@link #close()} is available for eager,
 * deterministic release and is idempotent. All native access is serialized
 * through the library-wide lock.</p>
 *
 * @author XDSSWAR
 */
public final class PdfiumDocument implements AutoCloseable {

    /** Shared cleaner that runs native-close actions after GC. */
    private static final Cleaner CLEANER = Cleaner.create();

    /**
     * Holds the native handle and performs the native close exactly once. It
     * must not reference the owning {@link PdfiumDocument}, otherwise the object
     * could never become unreachable.
     */
    private static final class State implements Runnable {
        private final MemorySegment handle;
        private volatile boolean closed;

        State(MemorySegment handle) {
            this.handle = handle;
        }

        @Override
        public void run() {
            if (closed) {
                return;
            }
            closed = true;
            synchronized (PdfiumLibrary.LOCK) {
                try {
                    PdfiumLibrary.PV_CLOSE.invokeExact(handle);
                } catch (Throwable ignored) {
                    // Best-effort release during cleanup; nothing to recover.
                }
            }
        }
    }

    private final State state;
    private final Cleaner.Cleanable cleanable;
    private final int pageCount;

    PdfiumDocument(MemorySegment handle) {
        this.state = new State(handle);
        this.cleanable = CLEANER.register(this, state);
        synchronized (PdfiumLibrary.LOCK) {
            try {
                this.pageCount = (int) PdfiumLibrary.PV_PAGE_COUNT.invokeExact(handle);
            } catch (Throwable t) {
                throw new PdfiumException("pv_page_count failed", t);
            }
        }
        if (pageCount < 0) {
            throw new PdfiumException("Invalid document (page count " + pageCount + ")");
        }
    }

    /** Number of pages in the document. */
    public int pageCount() {
        return pageCount;
    }

    /**
     * Intrinsic size of a page in PDF points.
     *
     * @param index zero-based page index
     * @return the page size
     */
    public PageSize size(int index) {
        checkOpen();
        checkIndex(index);
        synchronized (PdfiumLibrary.LOCK) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment w = arena.allocate(JAVA_DOUBLE);
                MemorySegment h = arena.allocate(JAVA_DOUBLE);
                int rc = (int) PdfiumLibrary.PV_PAGE_SIZE.invokeExact(state.handle, index, w, h);
                if (rc != 0) {
                    throw new PdfiumException("pv_page_size failed for page " + index);
                }
                return new PageSize(w.get(JAVA_DOUBLE, 0), h.get(JAVA_DOUBLE, 0));
            } catch (PdfiumException e) {
                throw e;
            } catch (Throwable t) {
                throw new PdfiumException("pv_page_size failed", t);
            }
        }
    }

    /**
     * Renders a page into a caller-provided native buffer of BGRA (8888) pixels.
     * The buffer must hold at least {@code widthPx * heightPx * 4} bytes.
     *
     * @param index    zero-based page index
     * @param widthPx  target width in device pixels
     * @param heightPx target height in device pixels
     * @param rotation 0, 1, 2 or 3 = 0/90/180/270 degrees clockwise
     * @param argbBg   background fill as 0xAARRGGBB (use 0xFFFFFFFF for white)
     * @param buffer   native destination buffer (BGRA, stride widthPx*4)
     */
    public void render(int index, int widthPx, int heightPx, int rotation,
                       int argbBg, MemorySegment buffer) {
        checkOpen();
        checkIndex(index);
        if (widthPx <= 0 || heightPx <= 0) {
            throw new PdfiumException("Invalid render size " + widthPx + "x" + heightPx);
        }
        long needed = (long) widthPx * heightPx * 4L;
        if (buffer.byteSize() < needed) {
            throw new PdfiumException("Render buffer too small: " + buffer.byteSize() + " < " + needed);
        }
        synchronized (PdfiumLibrary.LOCK) {
            try {
                int rc = (int) PdfiumLibrary.PV_RENDER.invokeExact(
                        state.handle, index, widthPx, heightPx, rotation & 3, argbBg, buffer);
                if (rc != 0) {
                    throw new PdfiumException("pv_render failed for page " + index);
                }
            } catch (PdfiumException e) {
                throw e;
            } catch (Throwable t) {
                throw new PdfiumException("pv_render failed", t);
            }
        }
    }

    /**
     * Reads a metadata string from the document's Info dictionary.
     *
     * @param tag the metadata key (e.g. {@code "Title"}, {@code "Author"},
     *            {@code "Subject"}, {@code "Keywords"}, {@code "Creator"},
     *            {@code "Producer"}, {@code "CreationDate"}, {@code "ModDate"})
     * @return the value, or an empty string if the tag is absent
     */
    public String metaText(String tag) {
        checkOpen();
        synchronized (PdfiumLibrary.LOCK) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment tagSeg = arena.allocateFrom(tag);
                // First call with no buffer asks for the required byte length.
                int bytes = (int) PdfiumLibrary.PV_META_TEXT.invokeExact(
                        state.handle, tagSeg, MemorySegment.NULL, 0);
                // PDFium returns 2 (an empty UTF-16 NUL) when the value is absent.
                if (bytes <= 2) {
                    return "";
                }
                MemorySegment out = arena.allocate(bytes);
                int written = (int) PdfiumLibrary.PV_META_TEXT.invokeExact(
                        state.handle, tagSeg, out, bytes);
                if (written <= 2) {
                    return "";
                }
                char[] chars = new char[written / Character.BYTES - 1]; // drop NUL
                MemorySegment.copy(out, JAVA_CHAR, 0, chars, 0, chars.length);
                return new String(chars);
            } catch (Throwable t) {
                throw new PdfiumException("pv_meta_text failed for tag " + tag, t);
            }
        }
    }

    /**
     * The PDF file-format version as an integer, e.g. {@code 14} for 1.4 or
     * {@code 17} for 1.7.
     *
     * @return the version, or {@code -1} if it could not be determined
     */
    public int fileVersion() {
        checkOpen();
        synchronized (PdfiumLibrary.LOCK) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment v = arena.allocate(JAVA_INT);
                int rc = (int) PdfiumLibrary.PV_FILE_VERSION.invokeExact(state.handle, v);
                return rc == 0 ? v.get(JAVA_INT, 0) : -1;
            } catch (Throwable t) {
                throw new PdfiumException("pv_file_version failed", t);
            }
        }
    }

    /**
     * Loads the text page for a page index, for search/selection. The returned
     * text page also frees itself automatically, but may be closed eagerly.
     *
     * @param index zero-based page index
     * @return the text page
     */
    public PdfiumTextPage loadText(int index) {
        checkOpen();
        checkIndex(index);
        synchronized (PdfiumLibrary.LOCK) {
            try {
                MemorySegment text = (MemorySegment) PdfiumLibrary.PV_TEXT_LOAD.invokeExact(state.handle, index);
                if (text.address() == 0L) {
                    throw new PdfiumException("pv_text_load failed for page " + index);
                }
                return new PdfiumTextPage(text);
            } catch (PdfiumException e) {
                throw e;
            } catch (Throwable t) {
                throw new PdfiumException("pv_text_load failed", t);
            }
        }
    }

    /** Whether this document has been closed. */
    public boolean isClosed() {
        return state.closed;
    }

    /** Eagerly releases the native document. Idempotent; GC would do this anyway. */
    @Override
    public void close() {
        cleanable.clean();
    }

    private void checkOpen() {
        if (state.closed) {
            throw new PdfiumException("Document is closed");
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= pageCount) {
            throw new PdfiumException("Page index out of range: " + index + " (0.." + (pageCount - 1) + ")");
        }
    }
}
