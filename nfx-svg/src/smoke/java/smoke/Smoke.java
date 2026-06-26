package smoke;

import com.xss.it.nfx.svg.ffm.Svg;
import com.xss.it.nfx.svg.ffm.SvgNative;
import xss.it.nfx.svg.SvgDocument;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Native Skia SVG smoke test (parse + render). If an SVG path is passed (the
 * build points it at the repo-root {@code sample.svg}) it exercises that file;
 * it also always runs strict checks against a tiny embedded SVG.
 *
 * <p>Headless by design: it renders straight into a native BGRA buffer and counts
 * painted pixels, so no JavaFX toolkit is required.</p>
 */
public final class Smoke {

    /** A minimal SVG with opaque shapes so a correct render is never blank. */
    private static final String EMBEDDED =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"60\" "
                    + "viewBox=\"0 0 100 60\">"
                    + "<rect width=\"100\" height=\"60\" fill=\"#ff0000\"/>"
                    + "<circle cx=\"50\" cy=\"30\" r=\"20\" fill=\"#00ff00\"/></svg>";

    public static void main(String[] args) throws Exception {
        Path sample = args.length > 0 ? Path.of(args[0]) : null;
        if (sample != null && Files.isRegularFile(sample)) {
            System.out.println("== Real sample: " + sample + " ==");
            runReal(Files.readAllBytes(sample));
        } else {
            System.out.println("== No sample.svg found, embedded only ==");
        }

        System.out.println("== Embedded strict checks ==");
        runStrict(EMBEDDED.getBytes(StandardCharsets.UTF_8));

        System.out.println("== Public API checks ==");
        runPublic(EMBEDDED);

        System.out.println("SMOKE OK");
    }

    /** Generic checks against an arbitrary SVG document. */
    private static void runReal(byte[] bytes) {
        try (SvgNative doc = Svg.load(bytes)) {
            System.out.println("intrinsic=" + doc.width() + "x" + doc.height());
            long painted = renderPainted(doc, 3.0);
            System.out.println("render(3x) paintedPixels=" + painted);
            if (painted == 0) {
                throw new IllegalStateException("blank render");
            }
        }
    }

    /** Strict checks against the embedded SVG. */
    private static void runStrict(byte[] bytes) {
        try (SvgNative doc = Svg.load(bytes)) {
            if (doc.width() <= 0 || doc.height() <= 0) {
                throw new IllegalStateException("bad intrinsic size " + doc.width() + "x" + doc.height());
            }
            long painted = renderPainted(doc, 4.0);
            System.out.println("intrinsic=" + doc.width() + "x" + doc.height()
                    + " render(4x) paintedPixels=" + painted);
            if (painted == 0) {
                throw new IllegalStateException("blank render");
            }
        }
    }

    /** Exercises the public engine API (load from string content, size, close). */
    private static void runPublic(String svg) {
        try (SvgDocument doc = SvgDocument.loadContent(svg)) {
            System.out.println("public: size=" + doc.getWidth() + "x" + doc.getHeight()
                    + " closed=" + doc.isClosed());
            if (doc.getWidth() <= 0 || doc.getHeight() <= 0) {
                throw new IllegalStateException("public API size failed");
            }
        }
    }

    /**
     * Renders the document at the given scale onto a transparent background and
     * returns the number of painted (non-transparent) pixels.
     */
    private static long renderPainted(SvgNative doc, double scale) {
        int w = (int) Math.round(doc.width() * scale);
        int h = (int) Math.round(doc.height() * scale);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate((long) w * h * 4L);
            doc.render(w, h, 0x00000000, buf); // transparent background
            long total = (long) w * h;
            long painted = 0;
            for (long i = 0; i < total; i++) {
                int px = buf.get(JAVA_INT, i * 4L);
                // BGRA premultiplied, little-endian int -> alpha is the high byte.
                if ((px & 0xFF000000) != 0) {
                    painted++;
                }
            }
            return painted;
        }
    }
}
