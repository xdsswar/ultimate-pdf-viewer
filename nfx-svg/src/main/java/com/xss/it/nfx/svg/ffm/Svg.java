package com.xss.it.nfx.svg.ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

/**
 * Entry point for the native Skia SVG engine. Internal API.
 *
 * <p>Parsing an SVG copies the source bytes into native memory (Skia keeps no
 * reference to the Java array), so the caller may discard the source buffer
 * immediately. The returned {@link SvgNative} owns the native handle; its memory
 * is released automatically when it becomes unreachable (a Cleaner runs the
 * native close), or eagerly via {@link SvgNative#close()}.</p>
 *
 * @author XDSSWAR
 */
public final class Svg {

    private Svg() {
    }

    /** Triggers native library loading + initialization eagerly (optional). */
    public static void init() {
        SvgLibrary.ensureLoaded();
    }

    /**
     * Attempts to parse an SVG document, returning {@code null} on failure
     * instead of throwing - pair with {@link #lastError()} for a description.
     *
     * @param bytes the raw SVG bytes
     * @return the parsed document, or {@code null} if it could not be parsed
     */
    public static SvgNative tryLoad(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new SvgFfmException("Empty SVG buffer");
        }
        SvgLibrary.ensureLoaded();
        synchronized (SvgLibrary.LOCK) {
            // Confined arena just for the load call; the shim copies the bytes.
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment data = arena.allocate(bytes.length);
                MemorySegment.copy(bytes, 0, data, JAVA_BYTE, 0, bytes.length);
                MemorySegment handle = (MemorySegment) SvgLibrary.SV_LOAD
                        .invokeExact(data, (long) bytes.length);
                return handle.address() == 0L ? null : new SvgNative(handle);
            } catch (SvgFfmException e) {
                throw e;
            } catch (Throwable t) {
                throw new SvgFfmException("sv_load failed", t);
            }
        }
    }

    /**
     * Parses an SVG document, throwing if it cannot be parsed.
     *
     * @param bytes the raw SVG bytes
     * @return the parsed document
     * @throws SvgFfmException if the SVG is malformed or unsupported
     */
    public static SvgNative load(byte[] bytes) {
        SvgNative doc = tryLoad(bytes);
        if (doc == null) {
            throw new SvgFfmException("Failed to parse SVG: " + lastError());
        }
        return doc;
    }

    /**
     * A human-readable description of the last native failure on this thread.
     *
     * @return the error message, or an empty string if none
     */
    public static String lastError() {
        SvgLibrary.ensureLoaded();
        synchronized (SvgLibrary.LOCK) {
            try {
                MemorySegment ptr = (MemorySegment) SvgLibrary.SV_LAST_ERROR.invokeExact();
                if (ptr.address() == 0L) {
                    return "";
                }
                return ptr.reinterpret(Long.MAX_VALUE)
                        .getString(0, StandardCharsets.UTF_8);
            } catch (Throwable t) {
                return "";
            }
        }
    }
}
