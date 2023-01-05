cmake_minimum_required(VERSION 3.12)
project(qauxv)

enable_language(CXX C)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_C_STANDARD 11)

find_library( # Defines the name of the path variable that stores the
        # location of the NDK library.
        ANDROID_LIBS
        # Specifies the name of the NDK library that
        # CMake needs to locate.
        log)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=format-invalid-specifier -Werror=return-type -Wno-invalid-offsetof")
if ("${CMAKE_CXX_COMPILER_ID}" MATCHES "Clang")
    SET(CLANG_CXX_EXTRA_OPT "-Werror=unknown-warning-option -Werror=format-invalid-specifier -Werror=call-to-pure-virtual-from-ctor-dtor")
    SET(CLANG_C_EXTRA_OPT "-Werror=format-invalid-specifier")
else ()
    SET(CLANG_CXX_EXTRA_OPT "")
    SET(CLANG_C_EXTRA_OPT "")
endif ()
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${CLANG_CXX_EXTRA_OPT} -fPIC -Werror=delete-non-virtual-dtor -Werror=return-type -Werror=non-virtual-dtor -Wno-invalid-offsetof")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${CLANG_C_EXTRA_OPT} -fPIC -Werror=return-type")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--no-allow-shlib-undefined,--no-undefined")

add_subdirectory(../../../../libs/mmkv mmkv)
add_subdirectory(../../../../libs/dexkit dexkit)
add_subdirectory(../../../../libs/silk silk)

add_library(qauxv SHARED
        qauxv_core/Natives.cpp
        qauxv_core/version.c
        qauxv_core/NativeHookEntry.cc
        qauxv_core/NativeMainHook.cc
        qauxv_core/shared_memory.cpp
        qauxv_core/SilkCodec.cc
        qauxv_core/auto_close_fd.cc
        )

set_target_properties(qauxv PROPERTIES
        CXX_EXTENSIONS OFF
        POSITION_INDEPENDENT_CODE ON
        )

target_include_directories(qauxv PRIVATE ../../../../libs/linux-syscall-support)

target_compile_definitions(qauxv PRIVATE QAUXV_VERSION=\"${QAUXV_VERSION}\")

if (ANDROID_ABI STREQUAL "arm64-v8a")
    set(RUST_TARGET_NAME "aarch64-linux-android")
elseif (ANDROID_ABI STREQUAL "armeabi-v7a")
    set(RUST_TARGET_NAME "armv7-linux-androideabi")
elseif (ANDROID_ABI STREQUAL "x86")
    set(RUST_TARGET_NAME "i686-linux-android")
elseif (ANDROID_ABI STREQUAL "x86_64")
    set(RUST_TARGET_NAME "x86_64-linux-android")
else ()
    message(FATAL_ERROR "Unsupported ABI: ${ANDROID_ABI}")
endif ()

set(RUST_LIB_FULL_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../rust/target/${RUST_TARGET_NAME}/${RUST_LIB_PROFILE}/librust.a")

target_link_options(qauxv PRIVATE "-Wl,-e,__libqauxv_main" "-Wl,--whole-archive" "${RUST_LIB_FULL_PATH}" "-Wl,--no-whole-archive")

target_link_libraries(qauxv mmkv dexkit silk c dl z ${ANDROID_LIBS})
