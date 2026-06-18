package smoke;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a tiny, byte-correct single-page PDF (with the text "Hello PDF") so the
 * smoke test has valid input without any third-party dependency.
 */
final class MiniPdf {

    private MiniPdf() {
    }

    static byte[] build() {
        return build("1.4", null);
    }

    /**
     * Builds the same single-page PDF but with an Info dictionary, for metadata
     * tests. The {@code info} string is the body of object 6 (e.g.
     * {@code "<</Title(...)/Author(...)>>"}) and is referenced from the trailer.
     */
    static byte[] buildWithInfo(String version, String info) {
        return build(version, info);
    }

    private static byte[] build(String version, String info) {
        String content = "BT /F1 24 Tf 50 100 Td (Hello PDF) Tj ET\n";
        List<String> objects = new ArrayList<>();
        objects.add("<</Type/Catalog/Pages 2 0 R>>");
        objects.add("<</Type/Pages/Kids[3 0 R]/Count 1>>");
        objects.add("<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]"
                + "/Resources<</Font<</F1 4 0 R>>>>/Contents 5 0 R>>");
        objects.add("<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>");
        objects.add("<</Length " + content.length() + ">>\nstream\n" + content + "endstream");
        if (info != null) {
            objects.add(info); // object 6: the Info dictionary
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int pos = write(out, "%PDF-" + version + "\n");

        int n = objects.size();
        int[] offset = new int[n + 1];
        for (int i = 0; i < n; i++) {
            offset[i + 1] = pos;
            pos += write(out, (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n");
        }

        int xref = pos;
        StringBuilder x = new StringBuilder();
        x.append("xref\n0 ").append(n + 1).append("\n");
        x.append("0000000000 65535 f \n");
        for (int i = 1; i <= n; i++) {
            x.append(String.format("%010d 00000 n \n", offset[i]));
        }
        x.append("trailer\n<</Size ").append(n + 1).append("/Root 1 0 R");
        if (info != null) {
            x.append("/Info ").append(n).append(" 0 R");
        }
        x.append(">>\n");
        x.append("startxref\n").append(xref).append("\n%%EOF");
        write(out, x.toString());

        return out.toByteArray();
    }

    private static int write(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
        out.writeBytes(b);
        return b.length;
    }
}
