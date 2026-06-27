package xss.it.nfx.svg;

/**
 * How a {@link xss.it.nfx.svg.scene.SvgView#fillProperty() fill} color is blended
 * over an SVG when tinting it.
 *
 * <p>The fill is composited onto the SVG with the chosen blend mode and then
 * masked back to the SVG's own shape, so only the painted pixels are affected -
 * never the transparent area around them. {@link #SRC_OVER} (with an opaque
 * color) is a flat recolor; modes like {@link #MULTIPLY}, {@link #SCREEN} or
 * {@link #OVERLAY} blend with the original colors for shaded tints.</p>
 *
 * <p>These are full Porter-Duff / separable blend modes (including a true
 * {@link #SRC_IN}), implemented natively in Skia - unlike JavaFX's
 * {@code BlendMode}, which lacks several of them.</p>
 *
 * @author XDSSWAR
 */
public enum SvgFillMode {

    /** No tint - the SVG keeps its own colors (the default). */
    NONE(-1),
    /** Paint the fill over the SVG; an opaque fill fully replaces its colors. */
    SRC_OVER(0),
    /** Keep the fill only where the SVG is painted (flat recolor). */
    SRC_IN(1),
    /** Paint the fill atop the SVG, keeping the SVG's alpha. */
    SRC_ATOP(2),
    /** Multiply the fill and SVG color channels (a darkening modulate). */
    MODULATE(3),
    /** Multiply blend - darkens, like overlapping inks. */
    MULTIPLY(4),
    /** Screen blend - lightens. */
    SCREEN(5),
    /** Overlay blend - multiply or screen depending on the base. */
    OVERLAY(6),
    /** Keep the darker of fill and SVG. */
    DARKEN(7),
    /** Keep the lighter of fill and SVG. */
    LIGHTEN(8),
    /** Brighten the SVG to reflect the fill (color dodge). */
    COLOR_DODGE(9),
    /** Darken the SVG to reflect the fill (color burn). */
    COLOR_BURN(10),
    /** Hard light blend. */
    HARD_LIGHT(11),
    /** Soft light blend. */
    SOFT_LIGHT(12),
    /** Absolute difference of fill and SVG. */
    DIFFERENCE(13),
    /** Like difference but lower contrast. */
    EXCLUSION(14),
    /** Apply the fill's hue, keeping the SVG's saturation and luminosity. */
    HUE(15),
    /** Apply the fill's saturation. */
    SATURATION(16),
    /** Apply the fill's hue and saturation. */
    COLOR(17),
    /** Apply the fill's luminosity. */
    LUMINOSITY(18),
    /** Additive blend (clamped). */
    PLUS(19);

    /** The native blend-mode code passed to the shim (matches SV_MODE_* in svg_shim.h). */
    private final int code;

    SvgFillMode(int code) {
        this.code = code;
    }

    /**
     * The native blend-mode code for this mode.
     *
     * @return the code passed to the native renderer
     */
    public int code() {
        return code;
    }
}
