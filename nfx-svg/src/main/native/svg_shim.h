/*
 * nfx-svg - native Skia shim.
 *
 * Thin C ABI over Skia's SVG module (SkSVGDOM) for binding from Java via the
 * Foreign Function & Memory API (Panama). Keeps the C++ Skia types (sk_sp,
 * SkSurface, SkCanvas, SkSVGDOM) on the native side so the Java layer only ever
 * deals with primitives and opaque pointers.
 *
 * SVG is vector: the Java layer asks for an exact pixel size (zoom x dpi x
 * output-scale) and the shim rasterizes the document fresh at that resolution,
 * so output is crisp at any zoom with no upscaling of a cached bitmap.
 *
 * All functions are C-linkage and exported. Each parsed document is independent,
 * but the Java layer serializes calls through a global lock to stay conservative.
 */
#ifndef NFX_SVG_SHIM_H
#define NFX_SVG_SHIM_H

#include <stddef.h>

#if defined(_WIN32)
  #define SV_EXPORT __declspec(dllexport)
#else
  #define SV_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

// Lifecycle ----------------------------------------------------------------

/*
 * Initializes the shim (creates the platform font manager). Returns 0 on
 * success. Idempotent.
 */
SV_EXPORT int sv_init(void);

// Documents ----------------------------------------------------------------

/*
 * Parses an SVG document from an in-memory buffer. The shim copies what it
 * needs, so the caller may free `data` immediately after this returns. Returns
 * an opaque document handle, or NULL if the SVG could not be parsed.
 */
SV_EXPORT void* sv_load(const void* data, size_t size);

/*
 * Closes a document handle and frees its resources. NULL-safe.
 */
SV_EXPORT void sv_close(void* dom);

/*
 * Writes the document's intrinsic size in user units (CSS px) into *out_w /
 * *out_h. Resolved from the SVG's width/height, else its viewBox, else the
 * SVG default (300x150). Returns 0 on success, -1 if `dom`/out-params are NULL.
 */
SV_EXPORT int sv_intrinsic_size(void* dom, float* out_w, float* out_h);

// Rendering ----------------------------------------------------------------

/*
 * Renders the document into `buffer`, which must hold width*height*4 bytes of
 * BGRA (8888) premultiplied pixels with stride width*4 (matching JavaFX's
 * PixelFormat.getByteBgraPreInstance). `bg_argb` is a 0xAARRGGBB fill applied
 * before drawing (use 0x00000000 for a transparent background). The document is
 * scaled to fill the target rectangle.
 *
 * Optional tint: when `tint_mode >= 0`, `tint_argb` (0xAARRGGBB) is blended over
 * the rendered SVG using the blend mode `tint_mode` (see the sv_mode_* codes
 * below), then masked back to the SVG's own alpha so only its painted shape is
 * recolored - never the transparent area around it. Pass `tint_mode < 0` for no
 * tint. Returns 0 on success.
 */
SV_EXPORT int sv_render(void* dom, int width, int height,
                        unsigned int bg_argb,
                        unsigned int tint_argb, int tint_mode,
                        void* buffer);

/*
 * Blend-mode codes for sv_render's tint (kept stable across releases; the Java
 * SvgFillMode enum mirrors them). Anything out of range falls back to SrcOver.
 */
#define SV_MODE_SRC_OVER    0
#define SV_MODE_SRC_IN      1
#define SV_MODE_SRC_ATOP    2
#define SV_MODE_MODULATE    3
#define SV_MODE_MULTIPLY    4
#define SV_MODE_SCREEN      5
#define SV_MODE_OVERLAY     6
#define SV_MODE_DARKEN      7
#define SV_MODE_LIGHTEN     8
#define SV_MODE_COLOR_DODGE 9
#define SV_MODE_COLOR_BURN  10
#define SV_MODE_HARD_LIGHT  11
#define SV_MODE_SOFT_LIGHT  12
#define SV_MODE_DIFFERENCE  13
#define SV_MODE_EXCLUSION   14
#define SV_MODE_HUE         15
#define SV_MODE_SATURATION  16
#define SV_MODE_COLOR       17
#define SV_MODE_LUMINOSITY  18
#define SV_MODE_PLUS        19

/*
 * Returns a human-readable description of the last failure on the calling
 * thread, or an empty string if none. The pointer is owned by the shim.
 */
SV_EXPORT const char* sv_last_error(void);

#ifdef __cplusplus
}
#endif

#endif /* NFX_SVG_SHIM_H */
