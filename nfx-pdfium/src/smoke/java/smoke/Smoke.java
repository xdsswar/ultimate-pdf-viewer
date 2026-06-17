package smoke;

import com.xss.it.nfx.pdfium.ffm.PageSize;
import com.xss.it.nfx.pdfium.ffm.Pdfium;
import com.xss.it.nfx.pdfium.ffm.PdfiumDocument;
import com.xss.it.nfx.pdfium.ffm.PdfiumTextPage;
import xss.it.nfx.pdfium.PdfDocument;
import xss.it.nfx.pdfium.PdfPage;
import xss.it.nfx.pdfium.text.PdfSearchResult;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Native PDFium smoke test. If a PDF path is passed (the build points it at the
 * repo-root {@code demo.pdf}) it exercises that real document; otherwise it
 * falls back to a tiny embedded PDF with strict assertions.
 */
public final class Smoke {

    public static void main(String[] args) throws Exception {
        Path sample = args.length > 0 ? Path.of(args[0]) : null;
        if (sample != null && Files.isRegularFile(sample)) {
            System.out.println("== Real sample: " + sample + " ==");
            runReal(Files.readAllBytes(sample));
        } else {
            System.out.println("== No demo.pdf found, embedded only ==");
        }
        System.out.println("== Embedded strict checks ==");
        runStrict(MiniPdf.build());

        System.out.println("== Public API checks ==");
        runPublic(MiniPdf.build(), "Hello", true);
        if (sample != null && Files.isRegularFile(sample)) {
            runPublic(Files.readAllBytes(sample), "the", false);
        }
        System.out.println("SMOKE OK");
    }

    /** Exercises the public engine API (no JavaFX toolkit needed for text/search). */
    private static void runPublic(byte[] bytes, String query, boolean strict) {
        try (PdfDocument doc = PdfDocument.open(bytes)) {
            PdfPage page = doc.getPage(0);
            int chars = page.getChars().size();
            List<PdfSearchResult> hits = doc.search(query);
            System.out.println("public: pages=" + doc.getPageCount()
                    + " page0=" + page.getWidth() + "x" + page.getHeight()
                    + " chars=" + chars + " search('" + query + "')=" + hits.size());
            if (strict && (chars <= 0 || hits.isEmpty())) {
                throw new IllegalStateException("public API text/search failed");
            }
        }
    }

    /** Generic checks against an arbitrary document. */
    private static void runReal(byte[] bytes) {
        try (PdfiumDocument doc = Pdfium.open(bytes)) {
            int pages = doc.pageCount();
            PageSize size = doc.size(0);
            System.out.println("pages=" + pages + " page0=" + size);
            if (pages <= 0) {
                throw new IllegalStateException("no pages");
            }
            long nonWhite = renderNonWhite(doc, 0, 2);
            System.out.println("page0 render nonWhitePixels=" + nonWhite);
            if (nonWhite == 0) {
                throw new IllegalStateException("blank render");
            }
            try (PdfiumTextPage text = doc.loadText(0)) {
                String snippet = text.text();
                if (snippet.length() > 80) {
                    snippet = snippet.substring(0, 80) + "...";
                }
                System.out.println("page0 chars=" + text.charCount() + " text='"
                        + snippet.replace('\n', ' ').trim() + "'");
            }
        }
    }

    /** Strict checks against the embedded "Hello PDF" document. */
    private static void runStrict(byte[] bytes) {
        try (PdfiumDocument doc = Pdfium.open(bytes)) {
            if (doc.pageCount() != 1) {
                throw new IllegalStateException("expected 1 page");
            }
            long nonWhite = renderNonWhite(doc, 0, 3);
            System.out.println("render nonWhitePixels=" + nonWhite);
            if (nonWhite == 0) {
                throw new IllegalStateException("blank render");
            }
            try (PdfiumTextPage text = doc.loadText(0)) {
                System.out.println("charCount=" + text.charCount() + " text='" + text.text().trim() + "'");
                List<PdfiumTextPage.Match> hits = text.find("Hello", false, false);
                System.out.println("find('Hello') -> " + hits);
                if (text.charCount() <= 0 || hits.isEmpty()) {
                    throw new IllegalStateException("text/search failed");
                }
            }
        }
    }

    /** Renders a page at the given scale and returns the non-white pixel count. */
    private static long renderNonWhite(PdfiumDocument doc, int page, int scale) {
        PageSize size = doc.size(page);
        int w = (int) Math.round(size.width() * scale);
        int h = (int) Math.round(size.height() * scale);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate((long) w * h * 4L);
            doc.render(page, w, h, 0, 0xFFFFFFFF, buf);
            long total = (long) w * h;
            long nonWhite = 0;
            for (long i = 0; i < total; i++) {
                int px = buf.get(JAVA_INT, i * 4L);
                if ((px & 0x00FFFFFF) != 0x00FFFFFF) {
                    nonWhite++;
                }
            }
            return nonWhite;
        }
    }
}
