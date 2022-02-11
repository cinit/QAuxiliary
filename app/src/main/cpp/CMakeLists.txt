cmake_minimum_required(VERSION 3.10)
project(qauxv)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

find_library( # Defines the name of the path variable that stores the
        # location of the NDK library.
        ANDROID_LIBS
        # Specifies the name of the NDK library that
        # CMake needs to locate.
        log)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=format-invalid-specifier -Werror=return-type -Wno-invalid-offsetof")
set(CMAKE_LINK_LIBRARY_FLAG " -Wl,-e,__libqauxv_main ${CMAKE_LINK_LIBRARY_FLAG}")

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

add_library(qauxv SHARED qauxv_core/Natives.cpp qauxv_core/version.c
        qauxv_core/NativeHookEntry.cc qauxv_core/NativeMainHook.cc)

set_target_properties(qauxv PROPERTIES
        CXX_EXTENSIONS OFF
        POSITION_INDEPENDENT_CODE ON
        )
target_compile_definitions(qauxv PRIVATE QAUXV_VERSION=\"${QAUXV_VERSION}\")

target_link_libraries(qauxv mmkv c dl ${ANDROID_LIBS})
