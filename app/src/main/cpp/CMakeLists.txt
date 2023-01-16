cmake_minimum_required(VERSION 3.10)
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
add_subdirectory(../../../../libs/dexkit/DexKit/Core dex_kit_static)
add_subdirectory(../../../../libs/silk silk)

add_library(qauxv SHARED
        qauxv_core/Natives.cpp
        qauxv_core/version.c
        # qauxv_core/NativeHookEntry.cc
        # qauxv_core/NativeMainHook.cc
        qauxv_core/shared_memory.cpp
        qauxv_core/SilkCodec.cc
        qauxv_core/auto_close_fd.cc
        ../../../../libs/dexkit/DexKit/Android/dexkit/src/main/cpp/native-bridge.cpp
        )

set_target_properties(qauxv PROPERTIES
        CXX_EXTENSIONS OFF
        POSITION_INDEPENDENT_CODE ON
        )

target_include_directories(qauxv PRIVATE ../../../../libs/linux-syscall-support)

target_compile_definitions(qauxv PRIVATE QAUXV_VERSION=\"${QAUXV_VERSION}\")
target_link_options(qauxv PRIVATE "-Wl,-e,__libqauxv_main")

target_link_libraries(qauxv mmkv dex_kit_static silk c dl z ${ANDROID_LIBS})
