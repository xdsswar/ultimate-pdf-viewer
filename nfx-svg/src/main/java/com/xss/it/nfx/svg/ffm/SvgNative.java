package com.xss.it.nfx.svg.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.ref.Cleaner;

import static java.lang.foreign.ValueLayout.JAVA_FLOAT;

/**
 * A parsed SVG document backed by a native Skia handle. Internal API.
 *
 * <p>Memory management is automatic: the native handle is registered with a
 * {@link Cleaner}, so it is released when this object becomes unreachable - no
 * manual cleanup is required. {@link #close()} is available for eager,
 * deterministic release and is idempotent. All native access is serialized
 * through the library-wide lock.</p>
 *
 * @author XDSSWAR
 */
public final class SvgNative implements AutoCloseable {

    /** Shared cleaner that runs native-close actions after GC. */
    private static final Cleaner CLEANER = Cleaner.create();

    /**
     * Holds the native handle and performs the native close exactly once. It
     * must not reference the owning {@link SvgNative}, otherwise the object
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
            synchronized (SvgLibrary.LOCK) {
                try {
                    SvgLibrary.SV_CLOSE.invokeExact(handle);
                } catch (Throwable ignored) {
                    // Best-effort release during cleanup; nothing to recover.
                }
            }
        }
    }

    private final State state;
    private final Cleaner.Cleanable cleanable;
    private final float width;
    private final float height;

    SvgNative(MemorySegment handle) {
        this.state = new State(handle);
        this.cleanable = CLEANER.register(this, state);
        synchronized (SvgLibrary.LOCK) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment w = arena.allocate(JAVA_FLOAT);
                MemorySegment h = arena.allocate(JAVA_FLOAT);
                int rc = (int) SvgLibrary.SV_INTRINSIC_SIZE.invokeExact(handle, w, h);
                if (rc != 0) {
                    throw new SvgFfmException("sv_intrinsic_size failed");
                }
                this.width = w.get(JAVA_FLOAT, 0);
                this.height = h.get(JAVA_FLOAT, 0);
            } catch (SvgFfmException e) {
                throw e;
            } catch (Throwable t) {
                throw new SvgFfmException("sv_intrinsic_size failed", t);
            }
        }
        if (width <= 0 || height <= 0) {
            throw new SvgFfmException("Invalid SVG size " + width + "x" + height);
        }
    }

    /** Intrinsic width in user units (CSS px). */
    public double width() {
        return width;
    }

    /** Intrinsic height in user units (CSS px). */
    public double height() {
        return height;
    }

    /**
     * Renders the document into a caller-provided native buffer of BGRA (8888)
     * premultiplied pixels. The buffer must hold at least
     * {@code widthPx * heightPx * 4} bytes. The document is scaled to fill the
     * target rectangle.
     *
     * @param widthPx  target width in device pixels
     * @param heightPx target height in device pixels
     * @param argbBg   background fill as 0xAARRGGBB (0 = transparent)
     * @param tintArgb tint color as 0xAARRGGBB (only used when {@code tintMode >= 0})
     * @param tintMode native blend-mode code, or a negative value for no tint
     * @param buffer   native destination buffer (BGRA, stride widthPx*4)
     */
    public void render(int widthPx, int heightPx, int argbBg,
                       int tintArgb, int tintMode, MemorySegment buffer) {
        checkOpen();
        if (widthPx <= 0 || heightPx <= 0) {
            throw new SvgFfmException("Invalid render size " + widthPx + "x" + heightPx);
        }
        long needed = (long) widthPx * heightPx * 4L;
        if (buffer.byteSize() < needed) {
            throw new SvgFfmException("Render buffer too small: " + buffer.byteSize() + " < " + needed);
        }
        synchronized (SvgLibrary.LOCK) {
            try {
                int rc = (int) SvgLibrary.SV_RENDER.invokeExact(
                        state.handle, widthPx, heightPx, argbBg, tintArgb, tintMode, buffer);
                if (rc != 0) {
                    throw new SvgFfmException("sv_render failed");
                }
            } catch (SvgFfmException e) {
                throw e;
            } catch (Throwable t) {
                throw new SvgFfmException("sv_render failed", t);
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
            throw new SvgFfmException("Document is closed");
        }
    }
}
