package com.xss.it.nfx.pdfium.ffm;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * Loads the bundled PDFium runtime + shim and exposes the native entry points
 * as {@link MethodHandle}s via the Foreign Function &amp; Memory API.
 *
 * <p>PDFium is not thread-safe: every native call must be made while holding
 * {@link #LOCK}. This class is internal to the library and never exported.</p>
 *
 * @author XDSSWAR
 */
final class PdfiumLibrary {

    /** Global lock serializing all native PDFium access. */
    static final Object LOCK = new Object();

    /**
     * Version tag for the bundled natives. Forms part of the on-disk cache path
     * so a new release re-extracts instead of reusing stale binaries. Bump this
     * whenever the bundled PDFium build or the shim changes.
     */
    private static final String NATIVE_VERSION = "151.0.7891+shim3";

    private static final Linker LINKER = Linker.nativeLinker();

    // Lifecycle / documents
    static final MethodHandle PV_INIT;
    static final MethodHandle PV_OPEN_MEMORY;
    static final MethodHandle PV_CLOSE;
    static final MethodHandle PV_LAST_ERROR;
    static final MethodHandle PV_PAGE_COUNT;
    static final MethodHandle PV_PAGE_SIZE;
    static final MethodHandle PV_META_TEXT;
    static final MethodHandle PV_FILE_VERSION;
    static final MethodHandle PV_RENDER;

    // Text
    static final MethodHandle PV_TEXT_LOAD;
    static final MethodHandle PV_TEXT_CLOSE;
    static final MethodHandle PV_TEXT_COUNT_CHARS;
    static final MethodHandle PV_TEXT_CHAR_BOX;
    static final MethodHandle PV_TEXT_GET_TEXT;
    static final MethodHandle PV_TEXT_FIND_START;
    static final MethodHandle PV_TEXT_FIND_NEXT;
    static final MethodHandle PV_TEXT_FIND_RESULT_INDEX;
    static final MethodHandle PV_TEXT_FIND_RESULT_COUNT;
    static final MethodHandle PV_TEXT_FIND_CLOSE;

    static {
        try {
            loadNativeLibraries();
        } catch (IOException e) {
            throw new PdfiumException("Failed to extract bundled PDFium native libraries", e);
        }

        final SymbolLookup lookup = SymbolLookup.loaderLookup();

        PV_INIT = handle(lookup, "pv_init", FunctionDescriptor.of(JAVA_INT));
        PV_OPEN_MEMORY = handle(lookup, "pv_open_memory",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG, ADDRESS));
        PV_CLOSE = handle(lookup, "pv_close", FunctionDescriptor.ofVoid(ADDRESS));
        PV_LAST_ERROR = handle(lookup, "pv_last_error", FunctionDescriptor.of(JAVA_INT));
        PV_PAGE_COUNT = handle(lookup, "pv_page_count", FunctionDescriptor.of(JAVA_INT, ADDRESS));
        PV_PAGE_SIZE = handle(lookup, "pv_page_size",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));
        PV_META_TEXT = handle(lookup, "pv_meta_text",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT));
        PV_FILE_VERSION = handle(lookup, "pv_file_version",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        PV_RENDER = handle(lookup, "pv_render",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT,
                        JAVA_INT, JAVA_INT, ADDRESS));

        PV_TEXT_LOAD = handle(lookup, "pv_text_load",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
        PV_TEXT_CLOSE = handle(lookup, "pv_text_close", FunctionDescriptor.ofVoid(ADDRESS));
        PV_TEXT_COUNT_CHARS = handle(lookup, "pv_text_count_chars",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        PV_TEXT_CHAR_BOX = handle(lookup, "pv_text_char_box",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        PV_TEXT_GET_TEXT = handle(lookup, "pv_text_get_text",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));
        PV_TEXT_FIND_START = handle(lookup, "pv_text_find_start",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT));
        PV_TEXT_FIND_NEXT = handle(lookup, "pv_text_find_next",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        PV_TEXT_FIND_RESULT_INDEX = handle(lookup, "pv_text_find_result_index",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        PV_TEXT_FIND_RESULT_COUNT = handle(lookup, "pv_text_find_result_count",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        PV_TEXT_FIND_CLOSE = handle(lookup, "pv_text_find_close",
                FunctionDescriptor.ofVoid(ADDRESS));

        try {
            int rc = (int) PV_INIT.invokeExact();
            if (rc != 0) {
                throw new PdfiumException("pv_init failed: " + rc);
            }
        } catch (PdfiumException e) {
            throw e;
        } catch (Throwable t) {
            throw new PdfiumException("pv_init invocation failed", t);
        }
    }

    private PdfiumLibrary() {
    }

    /** Forces the static initializer to run (loads + initializes PDFium). */
    static void ensureLoaded() {
        // no-op; class load triggers the static block.
    }

    private static MethodHandle handle(SymbolLookup lookup, String name, FunctionDescriptor desc) {
        MemorySegment sym = lookup.find(name)
                .orElseThrow(() -> new PdfiumException("Native symbol not found: " + name));
        return LINKER.downcallHandle(sym, desc);
    }

    /**
     * Extracts the bundled shim + PDFium runtime for the current platform into
     * {@code ~/.nfx-pdfium/natives/<version>/<platform>/} (only if not already
     * present) and loads them — PDFium first so the shim can bind to it.
     */
    private static void loadNativeLibraries() throws IOException {
        String platform = platform();
        Path dir = Path.of(System.getProperty("user.home"), ".nfx-pdfium",
                "natives", NATIVE_VERSION, platform);
        Files.createDirectories(dir);

        // PDFium must be loaded before the shim that depends on it.
        Path pdfium = ensureExtracted(platform, runtimeName("pdfium"), dir);
        Path shim = ensureExtracted(platform, shimName(), dir);

        System.load(pdfium.toString());
        System.load(shim.toString());
    }

    /**
     * Returns the cached native library, extracting it from the jar resources
     * only if it is not already present on disk.
     */
    private static Path ensureExtracted(String platform, String fileName, Path dir) throws IOException {
        Path out = dir.resolve(fileName);
        if (Files.isRegularFile(out) && Files.size(out) > 0) {
            return out;
        }
        String resource = "/native/" + platform + "/" + fileName;
        try (InputStream in = PdfiumLibrary.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new PdfiumException("Bundled native library missing: " + resource
                        + " (platform " + platform + " may be unsupported)");
            }
            // Write to a temp file then move into place so a concurrently
            // starting JVM never loads a half-written library.
            Path tmp = Files.createTempFile(dir, fileName + ".", ".part");
            try {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.move(tmp, out, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException atomicUnsupported) {
                    Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }
            return out;
        }
    }

    /** Platform key used for the resource path, e.g. {@code windows-x64}. */
    static String platform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        String osKey;
        if (os.contains("win")) {
            osKey = "windows";
        } else if (os.contains("mac") || os.contains("darwin")) {
            osKey = "mac";
        } else {
            osKey = "linux";
        }
        String archKey = (arch.contains("aarch64") || arch.contains("arm64")) ? "arm64" : "x64";
        return osKey + "-" + archKey;
    }

    /** Platform-specific file name for the bundled PDFium runtime. */
    private static String runtimeName(String base) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return base + ".dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            return "lib" + base + ".dylib";
        }
        return "lib" + base + ".so";
    }

    /** Platform-specific file name for our shim (built with an empty prefix on *nix). */
    private static String shimName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "pdfviewer_shim.dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            return "pdfviewer_shim.dylib";
        }
        return "pdfviewer_shim.so";
    }
}
