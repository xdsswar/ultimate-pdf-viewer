/*
 * Ultimate PDF Viewer - native PDFium shim implementation.
 * See pdfium_shim.h for the contract.
 */
#include "pdfium_shim.h"

#include <stdlib.h>
#include <string.h>

#include "fpdfview.h"
#include "fpdf_text.h"

/* Document handle: PDFium document plus the copied source bytes it points at.
 * FPDF_LoadMemDocument does not copy the buffer, so we own it for the doc's
 * lifetime. */
typedef struct {
    FPDF_DOCUMENT doc;
    void*         data;
} pv_doc;

int pv_init(void) {
    FPDF_LIBRARY_CONFIG config;
    memset(&config, 0, sizeof(config));
    config.version = 2;
    config.m_pUserFontPaths = NULL;
    config.m_pIsolate = NULL;
    config.m_v8EmbedderSlot = 0;
    FPDF_InitLibraryWithConfig(&config);
    return 0;
}

void pv_destroy(void) {
    FPDF_DestroyLibrary();
}

void* pv_open_memory(const void* data, size_t size, const char* password) {
    if (data == NULL || size == 0) {
        return NULL;
    }
    void* copy = malloc(size);
    if (copy == NULL) {
        return NULL;
    }
    memcpy(copy, data, size);

    FPDF_DOCUMENT doc = FPDF_LoadMemDocument(copy, (int) size, password);
    if (doc == NULL) {
        free(copy);
        return NULL;
    }

    pv_doc* handle = (pv_doc*) malloc(sizeof(pv_doc));
    if (handle == NULL) {
        FPDF_CloseDocument(doc);
        free(copy);
        return NULL;
    }
    handle->doc = doc;
    handle->data = copy;
    return handle;
}

void pv_close(void* doc) {
    if (doc == NULL) {
        return;
    }
    pv_doc* handle = (pv_doc*) doc;
    if (handle->doc != NULL) {
        FPDF_CloseDocument(handle->doc);
    }
    free(handle->data);
    free(handle);
}

int pv_last_error(void) {
    return (int) FPDF_GetLastError();
}

int pv_page_count(void* doc) {
    if (doc == NULL) {
        return -1;
    }
    return FPDF_GetPageCount(((pv_doc*) doc)->doc);
}

int pv_page_size(void* doc, int index, double* out_w, double* out_h) {
    if (doc == NULL || out_w == NULL || out_h == NULL) {
        return -1;
    }
    FS_SIZEF size;
    if (!FPDF_GetPageSizeByIndexF(((pv_doc*) doc)->doc, index, &size)) {
        return -1;
    }
    *out_w = size.width;
    *out_h = size.height;
    return 0;
}

int pv_render(void* doc, int index, int width, int height,
              int rotation, unsigned int bg_color, void* buffer) {
    if (doc == NULL || buffer == NULL || width <= 0 || height <= 0) {
        return -1;
    }
    FPDF_PAGE page = FPDF_LoadPage(((pv_doc*) doc)->doc, index);
    if (page == NULL) {
        return -1;
    }

    FPDF_BITMAP bmp = FPDFBitmap_CreateEx(width, height, FPDFBitmap_BGRA,
                                          buffer, width * 4);
    if (bmp == NULL) {
        FPDF_ClosePage(page);
        return -1;
    }

    FPDFBitmap_FillRect(bmp, 0, 0, width, height, bg_color);

    int flags = FPDF_ANNOT | FPDF_LCD_TEXT;
    FPDF_RenderPageBitmap(bmp, page, 0, 0, width, height, rotation, flags);

    FPDFBitmap_Destroy(bmp);
    FPDF_ClosePage(page);
    return 0;
}

void* pv_text_load(void* doc, int index) {
    if (doc == NULL) {
        return NULL;
    }
    FPDF_PAGE page = FPDF_LoadPage(((pv_doc*) doc)->doc, index);
    if (page == NULL) {
        return NULL;
    }
    FPDF_TEXTPAGE text = FPDFText_LoadPage(page);
    /* The text page keeps its own reference to page geometry; we can close the
     * page now since callers only use text APIs through the text handle. */
    FPDF_ClosePage(page);
    return text;
}

void pv_text_close(void* text) {
    if (text != NULL) {
        FPDFText_ClosePage((FPDF_TEXTPAGE) text);
    }
}

int pv_text_count_chars(void* text) {
    if (text == NULL) {
        return -1;
    }
    return FPDFText_CountChars((FPDF_TEXTPAGE) text);
}

int pv_text_char_box(void* text, int char_index,
                     double* left, double* right,
                     double* bottom, double* top) {
    if (text == NULL || left == NULL || right == NULL ||
        bottom == NULL || top == NULL) {
        return -1;
    }
    if (!FPDFText_GetCharBox((FPDF_TEXTPAGE) text, char_index,
                             left, right, bottom, top)) {
        return -1;
    }
    return 0;
}

int pv_text_get_text(void* text, int start, int count, unsigned short* out) {
    if (text == NULL || out == NULL) {
        return -1;
    }
    return FPDFText_GetText((FPDF_TEXTPAGE) text, start, count,
                            (unsigned short*) out);
}

void* pv_text_find_start(void* text, const unsigned short* needle,
                         unsigned int flags, int start_index) {
    if (text == NULL || needle == NULL) {
        return NULL;
    }
    return FPDFText_FindStart((FPDF_TEXTPAGE) text,
                              (FPDF_WIDESTRING) needle,
                              (unsigned long) flags, start_index);
}

int pv_text_find_next(void* find) {
    if (find == NULL) {
        return 0;
    }
    return FPDFText_FindNext((FPDF_SCHHANDLE) find) ? 1 : 0;
}

int pv_text_find_result_index(void* find) {
    if (find == NULL) {
        return -1;
    }
    return FPDFText_GetSchResultIndex((FPDF_SCHHANDLE) find);
}

int pv_text_find_result_count(void* find) {
    if (find == NULL) {
        return -1;
    }
    return FPDFText_GetSchCount((FPDF_SCHHANDLE) find);
}

void pv_text_find_close(void* find) {
    if (find != NULL) {
        FPDFText_FindClose((FPDF_SCHHANDLE) find);
    }
}
