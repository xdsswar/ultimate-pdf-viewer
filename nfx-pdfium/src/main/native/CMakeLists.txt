cmake_minimum_required(VERSION 3.20)
project(pdfviewer_shim C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

#
# Platform key (windows-x64, linux-x64, mac-arm64, ...). Defaults from the host
# but can be overridden for cross builds: -DPV_PLATFORM=linux-x64
#
if(NOT DEFINED PV_PLATFORM)
  if(WIN32)
    set(_pv_os "windows")
  elseif(APPLE)
    set(_pv_os "mac")
  else()
    set(_pv_os "linux")
  endif()
  if(CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64|arm64|ARM64")
    set(_pv_arch "arm64")
  else()
    set(_pv_arch "x64")
  endif()
  set(PV_PLATFORM "${_pv_os}-${_pv_arch}")
endif()
message(STATUS "pdfviewer_shim platform: ${PV_PLATFORM}")

#
# Vendored PDFium location. Layout matches bblanchon/pdfium-binaries:
#   <PDFIUM_DIR>/include/*.h
#   <PDFIUM_DIR>/bin/pdfium.dll          (Windows runtime)
#   <PDFIUM_DIR>/lib/pdfium.dll.lib      (Windows import lib)
#   <PDFIUM_DIR>/lib/libpdfium.so|.dylib (Linux/macOS runtime)
#
if(NOT DEFINED PDFIUM_DIR)
  set(PDFIUM_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/pdfium/${PV_PLATFORM}")
endif()
get_filename_component(PDFIUM_DIR "${PDFIUM_DIR}" ABSOLUTE)
message(STATUS "PDFium dir: ${PDFIUM_DIR}")

if(NOT EXISTS "${PDFIUM_DIR}/include/fpdfview.h")
  message(FATAL_ERROR "PDFium headers not found at ${PDFIUM_DIR}/include. "
                      "Run `./gradlew :nfx-pdfium:setupEnv` (or fetchPdfium) first.")
endif()

# Resolve the runtime library and (on Windows) the import lib to link against.
if(WIN32)
  set(PDFIUM_RUNTIME "${PDFIUM_DIR}/bin/pdfium.dll")
  if(MSVC)
    set(PDFIUM_LINK "${PDFIUM_DIR}/lib/pdfium.dll.lib")
  else()
    # MinGW/Clang can link directly against the DLL.
    set(PDFIUM_LINK "${PDFIUM_DIR}/bin/pdfium.dll")
  endif()
elseif(APPLE)
  set(PDFIUM_RUNTIME "${PDFIUM_DIR}/lib/libpdfium.dylib")
  set(PDFIUM_LINK "${PDFIUM_RUNTIME}")
else()
  set(PDFIUM_RUNTIME "${PDFIUM_DIR}/lib/libpdfium.so")
  set(PDFIUM_LINK "${PDFIUM_RUNTIME}")
endif()

add_library(pdfviewer_shim SHARED pdfium_shim.c)
target_include_directories(pdfviewer_shim PRIVATE "${PDFIUM_DIR}/include")
target_link_libraries(pdfviewer_shim PRIVATE "${PDFIUM_LINK}")

# Find libpdfium next to the shim at runtime (Linux/macOS).
if(APPLE)
  set_target_properties(pdfviewer_shim PROPERTIES
    INSTALL_RPATH "@loader_path"
    BUILD_WITH_INSTALL_RPATH ON)
elseif(NOT WIN32)
  set_target_properties(pdfviewer_shim PROPERTIES
    INSTALL_RPATH "$ORIGIN"
    BUILD_WITH_INSTALL_RPATH ON)
endif()

# Strip the lib prefix on *nix so names are predictable (pdfviewer_shim.so).
set_target_properties(pdfviewer_shim PROPERTIES PREFIX "")

#
# Stage both the shim and the PDFium runtime into the output dir so the Java
# resource-packaging step can pick them up from one place.
#
if(NOT DEFINED PV_STAGE_DIR)
  set(PV_STAGE_DIR "${CMAKE_CURRENT_BINARY_DIR}/stage")
endif()

add_custom_command(TARGET pdfviewer_shim POST_BUILD
  COMMAND ${CMAKE_COMMAND} -E make_directory "${PV_STAGE_DIR}"
  COMMAND ${CMAKE_COMMAND} -E copy "$<TARGET_FILE:pdfviewer_shim>" "${PV_STAGE_DIR}/"
  COMMAND ${CMAKE_COMMAND} -E copy "${PDFIUM_RUNTIME}" "${PV_STAGE_DIR}/"
  COMMENT "Staging shim + PDFium runtime into ${PV_STAGE_DIR}")
