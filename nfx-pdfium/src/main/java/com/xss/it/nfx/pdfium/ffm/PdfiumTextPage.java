package com.xss.it.nfx.pdfium.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.ref.Cleaner;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;

/**
 * Text content of a single page, for search and selection. Internal API.
 *
 * <p>Character boxes are returned in PDF point space with a bottom-left origin
 * (PDFium convention). The pdf-viewer layer maps these to device/scene space.</p>
 *
 * @author XDSSWAR
 */
public final class PdfiumTextPage implements AutoCloseable {

    /** Bounding box of a glyph in PDF points, origin bottom-left. */
    public record CharBox(double left, double bottom, double right, double top) {
        public double width() {
            return right - left;
        }

        public double height() {
            return top - bottom;
        }
    }

    /** A search hit: a contiguous run of characters. */
    public record Match(int start, int count) {
    }

    /** Shared cleaner that frees native text pages after GC. */
    private static final Cleaner CLEANER = Cleaner.create();

    /** Holds the native handle and closes it exactly once (no back-reference). */
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
                    PdfiumLibrary.PV_TEXT_CLOSE.invokeExact(handle);
                } catch (Throwable ignored) {
                    // Best-effort release during cleanup.
                }
            }
        }
    }

    private final State state;
    private final Cleaner.Cleanable cleanable;
    private final int charCount;

    PdfiumTextPage(MemorySegment handle) {
        this.state = new State(handle);
        this.cleanable = CLEANER.register(this, state);
        synchronized (PdfiumLibrary.LOCK) {
            try {
                this.charCount = (int) PdfiumLibrary.PV_TEXT_COUNT_CHARS.invokeExact(handle);
            } catch (Throwable t) {
                throw new PdfiumException("pv_text_count_chars failed", t);
            }
        }
    }

    /** Number of characters on the page (including whitespace). */
    public int charCount() {
        return Math.max(0, charCount);
    }

    /** Bounding box of a single character. */
    public CharBox charBox(int charIndex) {
        checkOpen();
        synchronized (PdfiumLibrary.LOCK) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment l = arena.allocate(JAVA_DOUBLE);
                MemorySegment r = arena.allocate(JAVA_DOUBLE);
                MemorySegment b = arena.allocate(JAVA_DOUBLE);
                MemorySegment t = arena.allocate(JAVA_DOUBLE);
                int rc = (int) PdfiumLibrary.PV_TEXT_CHAR_BOX.invokeExact(state.handle, charIndex, l, r, b, t);
                if (rc != 0) {
                    throw new PdfiumException("pv_text_char_box failed for char " + charIndex);
                }
                return new CharBox(l.get(JAVA_DOUBLE, 0), b.get(JAVA_DOUBLE, 0),
                        r.get(JAVA_DOUBLE, 0), t.get(JAVA_DOUBLE, 0));
            } catch (PdfiumException e) {
                throw e;
            } catch (Throwable t) {
                throw new PdfiumException("pv_text_char_box failed", t);
            }
        }
    }

    /** Extracts a run of text as a String. */
    public String text(int start, int count) {
        checkOpen();
        if (count <= 0) {
            return "";
        }
        synchronized (PdfiumLibrary.LOCK) {
            try (Arena arena = Arena.ofConfined()) {
                // FPDFText_GetText writes count+1 UTF-16 units (incl. NUL terminator).
                MemorySegment out = arena.allocate((long) (count + 1) * Character.BYTES);
                int written = (int) PdfiumLibrary.PV_TEXT_GET_TEXT.invokeExact(state.handle, start, count, out);
                if (written <= 1) {
                    return "";
                }
                char[] chars = new char[written - 1]; // drop trailing NUL
                MemorySegment.copy(out, JAVA_CHAR, 0, chars, 0, chars.length);
                return new String(chars);
            } catch (Throwable t) {
                throw new PdfiumException("pv_text_get_text failed", t);
            }
        }
    }

    /** Convenience: whole-page text. */
    public String text() {
        return text(0, charCount());
    }

    /**
     * Finds all matches of {@code needle} on the page.
     *
     * @param needle    text to search for
     * @param matchCase whether the search is case-sensitive
     * @param wholeWord whether to match whole words only
     */
    public List<Match> find(String needle, boolean matchCase, boolean wholeWord) {
        checkOpen();
        List<Match> matches = new ArrayList<>();
        if (needle == null || needle.isEmpty()) {
            return matches;
        }
        int flags = (matchCase ? 1 : 0) | (wholeWord ? 2 : 0);
        synchronized (PdfiumLibrary.LOCK) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment needleSeg = allocateUtf16(arena, needle);
                MemorySegment find = (MemorySegment) PdfiumLibrary.PV_TEXT_FIND_START
                        .invokeExact(state.handle, needleSeg, flags, 0);
                if (find.address() == 0L) {
                    return matches;
                }
                try {
                    while ((int) PdfiumLibrary.PV_TEXT_FIND_NEXT.invokeExact(find) == 1) {
                        int idx = (int) PdfiumLibrary.PV_TEXT_FIND_RESULT_INDEX.invokeExact(find);
                        int cnt = (int) PdfiumLibrary.PV_TEXT_FIND_RESULT_COUNT.invokeExact(find);
                        matches.add(new Match(idx, cnt));
                    }
                } finally {
                    PdfiumLibrary.PV_TEXT_FIND_CLOSE.invokeExact(find);
                }
                return matches;
            } catch (Throwable t) {
                throw new PdfiumException("pv_text_find failed", t);
            }
        }
    }

    /** Eagerly releases the native text page. Idempotent; GC would do this anyway. */
    @Override
    public void close() {
        cleanable.clean();
    }

    /** Allocates a NUL-terminated UTF-16LE string for FPDFText_FindStart. */
    private static MemorySegment allocateUtf16(Arena arena, String s) {
        byte[] utf16 = s.getBytes(StandardCharsets.UTF_16LE);
        MemorySegment seg = arena.allocate(utf16.length + 2L); // + UTF-16 NUL
        MemorySegment.copy(utf16, 0, seg, JAVA_BYTE, 0, utf16.length);
        return seg;
    }

    private void checkOpen() {
        if (state.closed) {
            throw new PdfiumException("Text page is closed");
        }
    }
}
