cmake_minimum_required(VERSION 3.20)
project(nfxsvg_shim CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Skia's prebuilt MSVC libraries link the STATIC CRT (/MT). Match it, or the
# linker reports RuntimeLibrary mismatches (libcmt vs msvcprt). CMP0091 (NEW
# since CMake 3.15) lets us select it abstractly.
if(MSVC)
  set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
endif()

#
# Platform key (windows-x64, linux-x64, mac-arm64, ...). Defaults from the host
# but can be overridden for cross builds: -DSV_PLATFORM=linux-x64
#
if(NOT DEFINED SV_PLATFORM)
  if(WIN32)
    set(_sv_os "windows")
  elseif(APPLE)
    set(_sv_os "mac")
  else()
    set(_sv_os "linux")
  endif()
  if(CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64|arm64|ARM64")
    set(_sv_arch "arm64")
  else()
    set(_sv_arch "x64")
  endif()
  set(SV_PLATFORM "${_sv_os}-${_sv_arch}")
endif()
message(STATUS "nfxsvg_shim platform: ${SV_PLATFORM}")

#
# Vendored Skia (prebuilt upstream C++ with the SVG module). Layout: a Skia
# root that contains the public headers (include/, modules/) and the static
# archives (anywhere beneath SKIA_DIR; they are globbed below).
#
if(NOT DEFINED SKIA_DIR)
  set(SKIA_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../third_party/skia/${SV_PLATFORM}")
endif()
get_filename_component(SKIA_DIR "${SKIA_DIR}" ABSOLUTE)
message(STATUS "Skia dir: ${SKIA_DIR}")

# Header root: the directory that makes `#include "include/core/SkCanvas.h"` and
# `#include "modules/svg/include/SkSVGDOM.h"` resolve. Overridable for archives
# that nest the tree differently: -DSKIA_INCLUDE_DIR=...
if(NOT DEFINED SKIA_INCLUDE_DIR)
  set(SKIA_INCLUDE_DIR "${SKIA_DIR}")
endif()

if(NOT EXISTS "${SKIA_INCLUDE_DIR}/include/core/SkCanvas.h")
  message(FATAL_ERROR "Skia headers not found under ${SKIA_INCLUDE_DIR}/include. "
                      "Run `./gradlew :nfx-svg:setupEnv` (or fetchSkia) first.")
endif()

# Collect every static archive shipped in the Skia distribution and link them
# all (skia, svg, skshaper, skunicode, skparagraph, ...). Globbing avoids
# hardcoding names that vary between Skia releases.
if(WIN32)
  file(GLOB_RECURSE SKIA_LIBS "${SKIA_DIR}/*.lib")
else()
  file(GLOB_RECURSE SKIA_LIBS "${SKIA_DIR}/*.a")
endif()
list(LENGTH SKIA_LIBS _skia_lib_count)
if(_skia_lib_count EQUAL 0)
  message(FATAL_ERROR "No Skia static libraries found under ${SKIA_DIR}.")
endif()
message(STATUS "Skia static libs: ${_skia_lib_count}")

add_library(nfxsvg_shim SHARED svg_shim.cpp)
target_include_directories(nfxsvg_shim PRIVATE "${SKIA_INCLUDE_DIR}")

#
# Link the Skia archives plus the per-OS system libraries Skia's static build
# pulls in. On GNU/Clang wrap the archives in a link group so inter-archive
# symbol order doesn't matter.
#
if(WIN32)
  target_link_libraries(nfxsvg_shim PRIVATE
    ${SKIA_LIBS}
    opengl32 user32 gdi32 ole32 oleaut32 advapi32 shell32 usp10 fontsub dwrite)
elseif(APPLE)
  target_link_libraries(nfxsvg_shim PRIVATE
    -Wl,-all_load ${SKIA_LIBS}
    "-framework CoreFoundation" "-framework CoreGraphics"
    "-framework CoreText" "-framework CoreServices")
else()
  target_link_libraries(nfxsvg_shim PRIVATE
    -Wl,--start-group ${SKIA_LIBS} -Wl,--end-group
    fontconfig freetype pthread dl GL)
endif()

# Predictable output name on every OS (nfxsvg_shim.{dll,so,dylib}).
set_target_properties(nfxsvg_shim PROPERTIES PREFIX "")

#
# Stage the shim into the output dir so the Java resource-packaging step can
# pick it up. Skia is static-linked in, so there is no separate runtime to ship.
#
if(NOT DEFINED SV_STAGE_DIR)
  set(SV_STAGE_DIR "${CMAKE_CURRENT_BINARY_DIR}/stage")
endif()

add_custom_command(TARGET nfxsvg_shim POST_BUILD
  COMMAND ${CMAKE_COMMAND} -E make_directory "${SV_STAGE_DIR}"
  COMMAND ${CMAKE_COMMAND} -E copy "$<TARGET_FILE:nfxsvg_shim>" "${SV_STAGE_DIR}/"
  COMMENT "Staging shim into ${SV_STAGE_DIR}")
