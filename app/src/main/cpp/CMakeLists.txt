cmake_minimum_required(VERSION 3.28)
# because cmake 3.28 support C++ modules
project(qauxv)

find_library(ANDROID_LIBS log)

macro(SET_OPTION option value)
    set(${option} ${value} CACHE INTERNAL "" FORCE)
endmacro()

enable_language(CXX C)

# keep this synchronized with the ones in build.gradle.kts
set(CMAKE_CXX_STANDARD 20)
# C standard should be 18 by default in build.gradle.kts, but we cmake 3.22.1 doesn't support it
set(CMAKE_C_STANDARD 11)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Werror=format-invalid-specifier -Werror=return-type -Wno-invalid-offsetof")
if ("${CMAKE_CXX_COMPILER_ID}" MATCHES "Clang")
    SET(CLANG_CXX_EXTRA_OPT "-Werror=unknown-warning-option -Werror=format-invalid-specifier -Werror=call-to-pure-virtual-from-ctor-dtor")
    SET(CLANG_C_EXTRA_OPT "-Werror=format-invalid-specifier")
else ()
    SET(CLANG_CXX_EXTRA_OPT "")
    SET(CLANG_C_EXTRA_OPT "")
endif ()

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${CLANG_CXX_EXTRA_OPT} -fPIC -Werror=return-type -Wno-invalid-offsetof")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${CLANG_C_EXTRA_OPT} -fPIC -Werror=return-type")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384 -Wl,--no-allow-shlib-undefined,--no-undefined -Wl,-z,defs,-z,now,-z,relro")

SET_OPTION(DOBBY_GENERATE_SHARED OFF)
# Do not use Dobby symbol resolver. You link it once and it crashes everywhere.
SET_OPTION(Plugin.SymbolResolver OFF)
add_subdirectory(../../../../libs/Dobby dobby)

# set -fomit-frame-pointer for Dobby if ANDROID_ABI is armeabi-v7a
if ("${ANDROID_ABI}" STREQUAL "armeabi-v7a")
    target_compile_options(dobby PRIVATE -fomit-frame-pointer)
endif ()

# add -fno-omit-frame-pointer -Werror=non-virtual-dtor -Werror=delete-non-virtual-dtor
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-omit-frame-pointer -Werror=non-virtual-dtor -Werror=delete-non-virtual-dtor")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fno-omit-frame-pointer")

add_subdirectory(../../../../libs/mmkv mmkv)
add_subdirectory(../../../../libs/dexkit/DexKit/Core dexkit_static)
add_subdirectory(../../../../libs/silk silk)
add_subdirectory(../../../../libs/libunwindstack unwindstack)
add_subdirectory(../../../../libs/fmt fmt)

SET_OPTION(DEX_BUILDER_BUILD_SHARED OFF)
SET_OPTION(LSPLANT_BUILD_SHARED OFF)
add_subdirectory(../../../../libs/LSPlant/lsplant/src/main/jni lsplant_static)
target_compile_options(dex_builder_static PRIVATE "-Wno-c++2b-extensions")

target_compile_definitions(fmt-header-only INTERFACE FMT_STATIC_THOUSANDS_SEPARATOR=1)

add_library(qauxv-core0 SHARED
        misc/version.c
        misc/v2sign.cc
        misc/md5.cpp

        ../../../../libs/dexkit/DexKit/dexkit/src/main/cpp/native-bridge.cpp

        qauxv_core/Natives.cpp
        qauxv_core/SilkCodec.cc
        qauxv_core/HostInfo.cc
        qauxv_core/NativeCoreBridge.cc
        qauxv_core/linker_utils.cc
        qauxv_core/LsplantBridge.cc
        qauxv_core/jni_method_registry.cc
        qauxv_core/native_loader.cc

        utils/shared_memory.cpp
        utils/auto_close_fd.cc
        utils/JniUtils.cc
        utils/TextUtils.cc
        utils/ProcessView.cpp
        utils/ElfView.cpp
        utils/FileMemMap.cpp
        utils/ThreadUtils.cc
        utils/MemoryUtils.cc
        utils/ConfigManager.cc
        utils/ElfScan.cc
        utils/AobScanUtils.cc
        utils/arch_utils.cc
        utils/MemoryDexLoader.cc
        utils/debug_utils.cc
        utils/Log.cc
        utils/art_symbol_resolver.cc
        utils/xz_decoder.cc
        utils/byte_array_output_stream.cc

        ntkernel/NtRecallMsgHook.cc
        ntkernel/card_msg_sender.cc

        jni/dexkit_native_bridge.cc
)

set_target_properties(qauxv-core0 PROPERTIES
        CXX_EXTENSIONS OFF
        POSITION_INDEPENDENT_CODE ON
)

target_include_directories(qauxv-core0 PRIVATE ../../../../libs/linux-syscall-support)
target_include_directories(qauxv-core0 PRIVATE ../../../../libs/LSPlant/lsplant/src/main/jni)
target_include_directories(qauxv-core0 PRIVATE ../../../../libs/mmkv/MMKV/Core)
target_include_directories(qauxv-core0 PRIVATE .)

target_compile_definitions(qauxv-core0 PRIVATE QAUXV_VERSION=\"${QAUXV_VERSION}\")
target_link_options(qauxv-core0 PRIVATE "-Wl,-e,__libqauxv_main")

target_link_libraries(qauxv-core0 dobby mmkv dexkit_static unwindstack base silk
        c dl fmt-header-only z lzma lsplant_static ${ANDROID_LIBS}
)

add_library(qauxv-dummy SHARED dummy/dummy.cc)
add_library(qauxv-preload SHARED preload/preload.cc)
target_link_libraries(qauxv-dummy unwindstack base c ${ANDROID_LIBS})
target_link_libraries(qauxv-preload unwindstack base c dl ${ANDROID_LIBS})
