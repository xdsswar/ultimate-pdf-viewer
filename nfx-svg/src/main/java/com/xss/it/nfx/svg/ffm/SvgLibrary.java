package com.xss.it.nfx.svg.ffm;

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
 * Loads the bundled Skia SVG shim and exposes the native entry points as
 * {@link MethodHandle}s via the Foreign Function &amp; Memory API.
 *
 * <p>Each parsed SVG document is independent, but to stay conservative every
 * native call is made while holding {@link #LOCK}. This class is internal to the
 * library and never exported.</p>
 *
 * @author XDSSWAR
 */
final class SvgLibrary {

    /** Global lock serializing all native Skia access. */
    static final Object LOCK = new Object();

    /**
     * Version tag for the bundled natives. Forms part of the on-disk cache path
     * so a new release re-extracts instead of reusing stale binaries. Bump this
     * whenever the bundled Skia build or the shim changes.
     */
    private static final String NATIVE_VERSION = "m144+shim1";

    private static final Linker LINKER = Linker.nativeLinker();

    static final MethodHandle SV_INIT;
    static final MethodHandle SV_LOAD;
    static final MethodHandle SV_CLOSE;
    static final MethodHandle SV_INTRINSIC_SIZE;
    static final MethodHandle SV_RENDER;
    static final MethodHandle SV_LAST_ERROR;

    static {
        try {
            loadNativeLibraries();
        } catch (IOException e) {
            throw new SvgFfmException("Failed to extract bundled Skia native library", e);
        }

        final SymbolLookup lookup = SymbolLookup.loaderLookup();

        SV_INIT = handle(lookup, "sv_init", FunctionDescriptor.of(JAVA_INT));
        SV_LOAD = handle(lookup, "sv_load",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG));
        SV_CLOSE = handle(lookup, "sv_close", FunctionDescriptor.ofVoid(ADDRESS));
        SV_INTRINSIC_SIZE = handle(lookup, "sv_intrinsic_size",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
        SV_RENDER = handle(lookup, "sv_render",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));
        SV_LAST_ERROR = handle(lookup, "sv_last_error", FunctionDescriptor.of(ADDRESS));

        try {
            int rc = (int) SV_INIT.invokeExact();
            if (rc != 0) {
                throw new SvgFfmException("sv_init failed: " + rc);
            }
        } catch (SvgFfmException e) {
            throw e;
        } catch (Throwable t) {
            throw new SvgFfmException("sv_init invocation failed", t);
        }
    }

    private SvgLibrary() {
    }

    /** Forces the static initializer to run (loads + initializes the shim). */
    static void ensureLoaded() {
        // no-op; class load triggers the static block.
    }

    private static MethodHandle handle(SymbolLookup lookup, String name, FunctionDescriptor desc) {
        MemorySegment sym = lookup.find(name)
                .orElseThrow(() -> new SvgFfmException("Native symbol not found: " + name));
        return LINKER.downcallHandle(sym, desc);
    }

    /**
     * Extracts the bundled shim for the current platform into
     * {@code ~/.nfx-svg/natives/<version>/<platform>/} (only if not already
     * present) and loads it. Skia is static-linked into the shim, so there is a
     * single library to load.
     */
    private static void loadNativeLibraries() throws IOException {
        String platform = platform();
        Path dir = Path.of(System.getProperty("user.home"), ".nfx-svg",
                "natives", NATIVE_VERSION, platform);
        Files.createDirectories(dir);

        Path shim = ensureExtracted(platform, shimName(), dir);
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
        try (InputStream in = SvgLibrary.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new SvgFfmException("Bundled native library missing: " + resource
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

    /** Platform-specific file name for our shim (empty prefix on *nix). */
    private static String shimName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "nfxsvg_shim.dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            return "nfxsvg_shim.dylib";
        }
        return "nfxsvg_shim.so";
    }
}
