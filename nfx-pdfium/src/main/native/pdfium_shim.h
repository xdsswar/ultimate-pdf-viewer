/*
 * Ultimate PDF Viewer - native PDFium shim.
 *
 * Thin C ABI over PDFium for binding from Java via the Foreign Function &
 * Memory API (Panama). Keeps the awkward parts (FPDF_FILEACCESS, bitmap
 * buffers, struct-based text APIs) on the C side so the Java layer only ever
 * deals with primitives and opaque pointers.
 *
 * All functions are C-linkage and exported. PDFium is NOT thread-safe; callers
 * must serialize every call (the Java layer holds a global lock).
 */
#ifndef PDFIUM_SHIM_H
#define PDFIUM_SHIM_H

#include <stddef.h>

#if defined(_WIN32)
  #define PV_EXPORT __declspec(dllexport)
#else
  #define PV_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* Lifecycle ---------------------------------------------------------------- */

/* Initializes the PDFium library. Returns 0 on success. Idempotent. */
PV_EXPORT int pv_init(void);

/* Tears down the PDFium library. */
PV_EXPORT void pv_destroy(void);

/* Documents ---------------------------------------------------------------- */

/*
 * Opens a document from an in-memory buffer. The shim copies the bytes, so the
 * caller may free `data` immediately after this returns. `password` may be NULL.
 * Returns an opaque document handle, or NULL on failure.
 */
PV_EXPORT void* pv_open_memory(const void* data, size_t size, const char* password);

/* Closes a document handle and frees the copied buffer. NULL-safe. */
PV_EXPORT void pv_close(void* doc);

/*
 * Returns the last PDFium error code (FPDF_GetLastError). Notably
 * 4 == FPDF_ERR_PASSWORD: an open failed because a password is required or the
 * supplied one was wrong.
 */
PV_EXPORT int pv_last_error(void);

/* Returns the number of pages, or -1 if `doc` is NULL. */
PV_EXPORT int pv_page_count(void* doc);

/*
 * Writes the page size in PDF points (1/72 inch) into *out_w / *out_h.
 * Returns 0 on success.
 */
PV_EXPORT int pv_page_size(void* doc, int index, double* out_w, double* out_h);

/* Rendering ---------------------------------------------------------------- */

/*
 * Renders page `index` into `buffer`, which must hold width*height*4 bytes of
 * BGRA (8888) pixels with stride width*4. `rotation` is 0/1/2/3 = 0/90/180/270
 * degrees clockwise. `bg_color` is an 0xAARRGGBB fill applied before drawing
 * (use 0xFFFFFFFF for an opaque white page). Returns 0 on success.
 */
PV_EXPORT int pv_render(void* doc, int index, int width, int height,
                        int rotation, unsigned int bg_color, void* buffer);

/* Text (loaded lazily; used by search + selection) ------------------------- */

/* Loads the text page for `index`. Returns an opaque text handle or NULL. */
PV_EXPORT void* pv_text_load(void* doc, int index);

/* Closes a text handle. NULL-safe. */
PV_EXPORT void pv_text_close(void* text);

/* Returns the number of characters on the text page, or -1. */
PV_EXPORT int pv_text_count_chars(void* text);

/*
 * Writes the bounding box of character `char_index` in PDF points into the four
 * out-params (left/right/bottom/top, origin bottom-left). Returns 0 on success.
 */
PV_EXPORT int pv_text_char_box(void* text, int char_index,
                               double* left, double* right,
                               double* bottom, double* top);

/*
 * Copies up to `count` UTF-16 code units starting at `start` into `out`
 * (caller-allocated, count+1 units for the NUL). Returns the number of units
 * written including the terminating NUL, matching FPDFText_GetText.
 */
PV_EXPORT int pv_text_get_text(void* text, int start, int count, unsigned short* out);

/*
 * Starts a search for the NUL-terminated UTF-16 `needle`. `flags` mirrors
 * PDFium (1=match case, 2=match whole word). `start_index` is the character to
 * begin from (-1 = from start). Returns an opaque find handle or NULL.
 */
PV_EXPORT void* pv_text_find_start(void* text, const unsigned short* needle,
                                   unsigned int flags, int start_index);

/* Advances to the next match. Returns 1 if found, 0 otherwise. */
PV_EXPORT int pv_text_find_next(void* find);

/* Returns the starting char index of the current match, or -1. */
PV_EXPORT int pv_text_find_result_index(void* find);

/* Returns the char count of the current match, or -1. */
PV_EXPORT int pv_text_find_result_count(void* find);

/* Closes a find handle. NULL-safe. */
PV_EXPORT void pv_text_find_close(void* find);

#ifdef __cplusplus
}
#endif

#endif /* PDFIUM_SHIM_H */
