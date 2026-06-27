/*
 * nfx-svg - native Skia shim implementation.
 * See svg_shim.h for the public C ABI contract.
 *
 * ---------------------------------------------------------------------------
 * What this file does
 * ---------------------------------------------------------------------------
 * It bridges Java (over the Foreign Function & Memory API) to Skia's C++ SVG
 * module. The render pipeline is:
 *
 *     SVG bytes  ->  SkMemoryStream  ->  SkSVGDOM (parse the SVG into a DOM)
 *                ->  SkSurfaces::WrapPixels (a raster canvas backed by the
 *                    caller's pixel buffer)
 *                ->  SkSVGDOM::render(canvas)  ->  BGRA pixels in that buffer
 *
 * The key property: the DOM is re-rasterized FRESH at whatever device-pixel size
 * Java asks for on every call. Because an SVG is vector data, drawing it at a
 * larger size produces genuinely more detail - never an upscaled (blurry)
 * bitmap. That is what makes SvgView crisp at any zoom.
 *
 * Threading: each parsed document and each render uses independent objects, so
 * the native side is re-entrant. The Java layer still serializes calls behind a
 * lock to stay conservative; this code does not assume that lock.
 * ---------------------------------------------------------------------------
 */
#include "svg_shim.h"

#include <cstring>   // std::strtof
#include <string>    // std::string (error messages, viewBox scan)

// Skia core: streams, color/data, surfaces + canvas, image info, ref counting.
#include "include/core/SkBlendMode.h"
#include "include/core/SkCanvas.h"
#include "include/core/SkColor.h"
#include "include/core/SkData.h"
#include "include/core/SkFontMgr.h"
#include "include/core/SkImage.h"
#include "include/core/SkImageInfo.h"
#include "include/core/SkPaint.h"
#include "include/core/SkRefCnt.h"
#include "include/core/SkSamplingOptions.h"
#include "include/core/SkSize.h"
#include "include/core/SkStream.h"
#include "include/core/SkSurface.h"
// Text shaping factory (needed by the SVG DOM to lay out <text> runs).
#include "modules/skshaper/include/SkShaper_factory.h"
// The SVG module itself: parses markup into a renderable DOM.
#include "modules/svg/include/SkSVGDOM.h"

// The font manager is platform specific: each OS has a native font backend.
// Pulling the right port in lets <text> elements resolve real system fonts.
#if defined(_WIN32)
  #include "include/ports/SkTypeface_win.h"      // SkFontMgr_New_DirectWrite
#elif defined(__APPLE__)
  #include "include/ports/SkFontMgr_mac_ct.h"    // SkFontMgr_New_CoreText
#else
  #include "include/ports/SkFontMgr_fontconfig.h" // SkFontMgr_New_FontConfig
#endif

namespace {

/*
 * Per the SVG/CSS spec, a document that declares neither an intrinsic size
 * (width/height) nor a viewBox falls back to a 300x150 viewport. We use the same
 * default so such documents still render at a sensible size instead of 0x0.
 */
constexpr float kDefaultWidth = 300.0f;
constexpr float kDefaultHeight = 150.0f;

/*
 * The font manager is expensive to build and safe to share, so we create exactly
 * one for the whole process in sv_init() and reuse it for every parse.
 */
sk_sp<SkFontMgr> gFontMgr;

/*
 * Last error message. Thread-local so two threads that each fail a call get their
 * own message back from sv_last_error() rather than racing on a shared string.
 */
thread_local std::string gLastError;

/*
 * Records (or clears, when msg is null) the current thread's last error.
 */
void set_error(const char* msg) {
    gLastError = msg ? msg : "";
}

/*
 * Native representation of one parsed SVG. We keep the Skia DOM alive (via its
 * smart pointer) together with the intrinsic size we resolved once at load time,
 * so renders don't have to recompute it. The opaque void* handed back to Java is
 * a pointer to one of these, allocated with new and freed in sv_close().
 */
struct SvgDoc {
    sk_sp<SkSVGDOM> dom;   // the parsed, renderable SVG document
    float width;           // intrinsic width  in user units (CSS px)
    float height;          // intrinsic height in user units (CSS px)
};

/*
 * Builds the platform-native font manager (DirectWrite / CoreText / Fontconfig).
 */
sk_sp<SkFontMgr> make_font_mgr() {
#if defined(_WIN32)
    return SkFontMgr_New_DirectWrite();
#elif defined(__APPLE__)
    return SkFontMgr_New_CoreText(nullptr);
#else
    return SkFontMgr_New_FontConfig(nullptr);
#endif
}

/*
 * Resolves an SVG's size from its viewBox by scanning the raw markup.
 *
 * Why scan the bytes instead of asking Skia? Skia's containerSize() only reports
 * a size when the document declares an explicit width/height. Many real-world
 * SVGs (icons especially) declare only a viewBox, for which Skia returns 0x0. To
 * get a correct aspect ratio for those we read the viewBox ourselves.
 *
 * The scan is intentionally minimal: it finds the first `viewBox="..."` (or
 * '...') attribute - which in any well-formed document is the root <svg>'s - and
 * parses its four numbers "minX minY width height" (space- or comma-separated).
 * On success it writes width/height into the out-params w and h and returns true.
 */
bool parse_viewbox(const char* data, size_t size, float* w, float* h) {
    static const char kKey[] = "viewBox";
    const std::string hay(data, size);
    size_t at = hay.find(kKey);
    while (at != std::string::npos) {
        // Find the '=' after the attribute name, then the opening quote char.
        size_t eq = hay.find('=', at + sizeof(kKey) - 1);
        if (eq != std::string::npos) {
            size_t q = hay.find_first_of("\"'", eq);
            if (q != std::string::npos) {
                // The value runs until the matching closing quote (same char).
                size_t end = hay.find(hay[q], q + 1);
                if (end != std::string::npos) {
                    std::string v = hay.substr(q + 1, end - q - 1);
                    // Parse up to four floats: minX minY width height. We skip any
                    // run of separators (space, comma, tab, CR, LF) between them.
                    float vals[4];
                    int n = 0;
                    const char* p = v.c_str();
                    char* next = nullptr;
                    while (n < 4) {
                        while (*p == ' ' || *p == ',' || *p == '\t' || *p == '\n' || *p == '\r') {
                            ++p;
                        }
                        if (*p == '\0') {
                            break;
                        }
                        vals[n] = std::strtof(p, &next);
                        if (next == p) {
                            break;  // not a number - malformed; stop.
                        }
                        p = next;
                        ++n;
                    }
                    // Only accept a complete, positive width/height.
                    if (n == 4 && vals[2] > 0 && vals[3] > 0) {
                        *w = vals[2];
                        *h = vals[3];
                        return true;
                    }
                }
            }
        }
        // This match wasn't usable; look for another "viewBox" later in the text.
        at = hay.find(kKey, at + sizeof(kKey) - 1);
    }
    return false;
}

/*
 * Maps an SV_MODE_* code (see svg_shim.h) to a Skia blend mode. Anything out of
 * range falls back to kSrcOver.
 */
SkBlendMode map_mode(int code) {
    switch (code) {
        case SV_MODE_SRC_OVER:    return SkBlendMode::kSrcOver;
        case SV_MODE_SRC_IN:      return SkBlendMode::kSrcIn;
        case SV_MODE_SRC_ATOP:    return SkBlendMode::kSrcATop;
        case SV_MODE_MODULATE:    return SkBlendMode::kModulate;
        case SV_MODE_MULTIPLY:    return SkBlendMode::kMultiply;
        case SV_MODE_SCREEN:      return SkBlendMode::kScreen;
        case SV_MODE_OVERLAY:     return SkBlendMode::kOverlay;
        case SV_MODE_DARKEN:      return SkBlendMode::kDarken;
        case SV_MODE_LIGHTEN:     return SkBlendMode::kLighten;
        case SV_MODE_COLOR_DODGE: return SkBlendMode::kColorDodge;
        case SV_MODE_COLOR_BURN:  return SkBlendMode::kColorBurn;
        case SV_MODE_HARD_LIGHT:  return SkBlendMode::kHardLight;
        case SV_MODE_SOFT_LIGHT:  return SkBlendMode::kSoftLight;
        case SV_MODE_DIFFERENCE:  return SkBlendMode::kDifference;
        case SV_MODE_EXCLUSION:   return SkBlendMode::kExclusion;
        case SV_MODE_HUE:         return SkBlendMode::kHue;
        case SV_MODE_SATURATION:  return SkBlendMode::kSaturation;
        case SV_MODE_COLOR:       return SkBlendMode::kColor;
        case SV_MODE_LUMINOSITY:  return SkBlendMode::kLuminosity;
        case SV_MODE_PLUS:        return SkBlendMode::kPlus;
        default:                  return SkBlendMode::kSrcOver;
    }
}

}  // namespace

extern "C" {

/*
 * One-time initialization: create the shared font manager. Idempotent - calling
 * it again is a no-op. Returns 0 (there is nothing here that can fail; a null
 * font manager simply means <text> falls back to no system fonts).
 */
int sv_init(void) {
    if (!gFontMgr) {
        gFontMgr = make_font_mgr();
    }
    return 0;
}

/*
 * Parses an SVG from an in-memory buffer and returns an opaque SvgDoc* handle
 * (or nullptr on failure, with sv_last_error() set).
 */
void* sv_load(const void* data, size_t size) {
    set_error("");
    if (data == nullptr || size == 0) {
        set_error("empty SVG buffer");
        return nullptr;
    }

    // Copy the bytes into Skia-owned memory: SkMemoryStream reads from the SkData
    // we hand it, and the DOM may reference it during parsing. Copying means the
    // Java caller is free to release its buffer the instant this returns.
    sk_sp<SkData> bytes = SkData::MakeWithCopy(data, size);
    SkMemoryStream stream(bytes);

    // Parse. The font manager resolves <text> typefaces; the text-shaping factory
    // turns text runs into positioned glyphs. Primitive() is the lightweight
    // shaper - enough for simple (e.g. Latin) text without the full HarfBuzz path.
    sk_sp<SkSVGDOM> dom = SkSVGDOM::Builder()
            .setFontManager(gFontMgr)
            .setTextShapingFactory(SkShapers::Primitive::Factory())
            .make(stream);
    if (!dom) {
        set_error("failed to parse SVG");
        return nullptr;
    }

    // Resolve the intrinsic size once, in priority order:
    //   1. the document's declared container size (explicit width/height), else
    //   2. its viewBox (scanned from the markup), else
    //   3. the SVG spec default viewport (300x150).
    float w = 0, h = 0;
    SkSize cs = dom->containerSize();
    if (cs.width() > 0 && cs.height() > 0) {
        w = cs.width();
        h = cs.height();
    } else if (parse_viewbox(static_cast<const char*>(data), size, &w, &h)) {
        // viewBox-derived size resolved into w/h.
    } else {
        w = kDefaultWidth;
        h = kDefaultHeight;
    }

    // Hand ownership of the DOM to a heap SvgDoc; Java holds the pointer until
    // sv_close(). std::move avoids bumping/dropping the DOM's refcount needlessly.
    SvgDoc* doc = new SvgDoc{std::move(dom), w, h};
    return doc;
}

/*
 * Frees a document handle. NULL-safe (deleting nullptr is a no-op), and freeing
 * the SvgDoc drops the last reference to its SkSVGDOM, releasing it too.
 */
void sv_close(void* dom) {
    delete static_cast<SvgDoc*>(dom);
}

/*
 * Reports the intrinsic size resolved at load time. Returns -1 on a null handle
 * or null out-params, else 0 with the size written into out_w and out_h.
 */
int sv_intrinsic_size(void* dom, float* out_w, float* out_h) {
    if (dom == nullptr || out_w == nullptr || out_h == nullptr) {
        return -1;
    }
    SvgDoc* doc = static_cast<SvgDoc*>(dom);
    *out_w = doc->width;
    *out_h = doc->height;
    return 0;
}

/*
 * Renders the document into the caller's pixel buffer at exactly width x height
 * device pixels. Returns 0 on success, -1 (with sv_last_error() set) otherwise.
 */
int sv_render(void* dom, int width, int height,
              unsigned int bg_argb,
              unsigned int tint_argb, int tint_mode, void* buffer) {
    set_error("");
    if (dom == nullptr || buffer == nullptr || width <= 0 || height <= 0) {
        set_error("invalid render arguments");
        return -1;
    }
    SvgDoc* doc = static_cast<SvgDoc*>(dom);

    // Describe the caller's buffer to Skia: BGRA 8888, premultiplied alpha. This
    // exact layout is what JavaFX's PixelFormat.getByteBgraPreInstance() expects,
    // so the Java side can upload the pixels into a WritableImage with no
    // per-pixel conversion.
    SkImageInfo info = SkImageInfo::Make(width, height,
                                         kBGRA_8888_SkColorType,
                                         kPremul_SkAlphaType);
    // Wrap the buffer as a raster surface (no copy): Skia draws straight into it.
    // The stride is width*4 bytes (4 bytes per pixel, tightly packed).
    sk_sp<SkSurface> surface =
            SkSurfaces::WrapPixels(info, buffer, static_cast<size_t>(width) * 4);
    if (!surface) {
        set_error("failed to wrap pixel buffer");
        return -1;
    }

    SkCanvas* canvas = surface->getCanvas();
    // Start from the requested background (0x00000000 = fully transparent). clear()
    // takes an unpremultiplied SkColor and writes premultiplied pixels for us.
    canvas->clear(static_cast<SkColor>(bg_argb));

    // Map the SVG's intrinsic coordinate space onto the target rectangle:
    //   - tell the DOM its container is the intrinsic size, then
    //   - scale the canvas so that intrinsic box fills width x height pixels.
    // The scale multiplies the VECTOR geometry (paths, text), not a bitmap, so
    // the result is sharp at any size. A non-uniform scale (different X and Y) is
    // honored too, which is how SvgView's non-preserve-ratio stretch stays crisp.
    doc->dom->setContainerSize(SkSize::Make(doc->width, doc->height));
    canvas->scale(static_cast<float>(width) / doc->width,
                  static_cast<float>(height) / doc->height);
    doc->dom->render(canvas);

    /*
     * Optional tint. We want: recolor/blend only the SVG's painted pixels, never
     * the transparent area around them, for ANY blend mode. The recipe:
     *   1. snapshot the rendered SVG (it carries the original alpha shape);
     *   2. blend the tint color over the whole canvas with the chosen mode -
     *      this also (wrongly) paints the transparent area;
     *   3. multiply the result back by the snapshot's alpha (kDstIn), which masks
     *      it to the SVG's real shape - so the surrounding area stays transparent.
     * The matrix is reset first so the snapshot maps 1:1 to the device pixels.
     */
    if (tint_mode >= 0) {
        sk_sp<SkImage> svg = surface->makeImageSnapshot();
        if (svg) {
            canvas->resetMatrix();

            SkPaint blendPaint;
            blendPaint.setColor(static_cast<SkColor>(tint_argb));
            blendPaint.setBlendMode(map_mode(tint_mode));
            canvas->drawPaint(blendPaint);

            SkPaint maskPaint;
            maskPaint.setBlendMode(SkBlendMode::kDstIn);
            canvas->drawImage(svg, 0, 0, SkSamplingOptions(), &maskPaint);
        }
    }
    return 0;
}

/*
 * Returns this thread's last error string (empty if none). Owned by the shim.
 */
const char* sv_last_error(void) {
    return gLastError.c_str();
}

}  // extern "C"
