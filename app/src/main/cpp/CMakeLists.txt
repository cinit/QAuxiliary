cmake_minimum_required(VERSION 3.10)
project(qauxv)

macro(SET_OPTION option value)
    set(${option} ${value} CACHE INTERNAL "" FORCE)
endmacro()

enable_language(CXX C)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_C_STANDARD 11)

SET_OPTION(DOBBY_GENERATE_SHARED OFF)
SET_OPTION(Plugin.SymbolResolver OFF)
add_subdirectory(../../../../libs/Dobby dobby)

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

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${CLANG_CXX_EXTRA_OPT} -fPIC -Werror=non-virtual-dtor -Werror=delete-non-virtual-dtor -Werror=return-type -Wno-invalid-offsetof")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${CLANG_C_EXTRA_OPT} -fPIC -Werror=return-type")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--no-allow-shlib-undefined,--no-undefined")

add_subdirectory(../../../../libs/mmkv mmkv)
add_subdirectory(../../../../libs/dexkit/DexKit/Core dex_kit_static)
add_subdirectory(../../../../libs/silk silk)

add_library(qauxv SHARED
        misc/version.c

        ../../../../libs/dexkit/DexKit/dexkit/src/main/cpp/native-bridge.cpp

        qauxv_core/Natives.cpp
        qauxv_core/SilkCodec.cc
        qauxv_core/HostInfo.cc
        qauxv_core/NativeCoreBridge.cc

        utils/shared_memory.cpp
        utils/auto_close_fd.cc
        utils/JniUtils.cc
        utils/TextUtils.cc
        utils/ProcessView.cpp
        utils/ElfView.cpp
        utils/FileMemMap.cpp

        ntkernel/NtRecallMsgHook.cc

        )

set_target_properties(qauxv PROPERTIES
        CXX_EXTENSIONS OFF
        POSITION_INDEPENDENT_CODE ON
        )

target_include_directories(qauxv PRIVATE ../../../../libs/linux-syscall-support)
target_include_directories(qauxv PRIVATE .)

target_compile_definitions(qauxv PRIVATE QAUXV_VERSION=\"${QAUXV_VERSION}\")
target_link_options(qauxv PRIVATE "-Wl,-e,__libqauxv_main")

target_link_libraries(qauxv dobby mmkv dex_kit_static silk c dl z ${ANDROID_LIBS})
