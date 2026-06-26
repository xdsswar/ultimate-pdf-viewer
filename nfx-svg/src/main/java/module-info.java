/**
 * nfx-svg - a standalone, cross-platform JavaFX SVG library backed by a native
 * Skia engine (Foreign Function &amp; Memory API + a thin C++ shim).
 *
 * <p>Public API lives under {@code xss.it.nfx.svg}; the implementation under
 * {@code com.xss.it.nfx.svg} is never exported. The module loads native code via
 * the FFM API, so consumers must run with
 * {@code --enable-native-access=nfx.svg}.</p>
 *
 * @author XDSSWAR
 */
module nfx.svg {
    requires javafx.graphics;
    requires javafx.controls;

    // Public API. Implementation packages (com.xss.it.nfx.svg.*) are never
    // exported, and rendering is intentionally not exposed - consume an SVG
    // through the SvgView node.
    exports xss.it.nfx.svg;          // core: SvgDocument, SvgException
    exports xss.it.nfx.svg.scene;    // JavaFX node: SvgView
}
