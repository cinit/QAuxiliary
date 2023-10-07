cmake_minimum_required(VERSION 3.10...3.27)
project(qauxv)

find_library(ANDROID_LIBS log)

macro(SET_OPTION option value)
    set(${option} ${value} CACHE INTERNAL "" FORCE)
endmacro()

enable_language(CXX C)
set(CMAKE_CXX_STANDARD 20)
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

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--no-allow-shlib-undefined,--no-undefined -Wl,-z,defs,-z,now,-z,relro")

SET_OPTION(DOBBY_GENERATE_SHARED OFF)
SET_OPTION(Plugin.SymbolResolver ON)
add_subdirectory(../../../../libs/Dobby dobby)

# set -fno-omit-frame-pointer for Dobby if ANDROID_ABI is armeabi-v7a
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

target_compile_definitions(fmt-header-only INTERFACE FMT_STATIC_THOUSANDS_SEPARATOR=1 FMT_USE_FLOAT=0 FMT_USE_DOUBLE=0 FMT_USE_LONG_DOUBLE=0)

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
        utils/ThreadUtils.cc
        utils/MemoryUtils.cc
        utils/ConfigManager.cc
        utils/ElfScan.cc
        utils/AobScanUtils.cc
        utils/arch_utils.cc
        utils/MemoryDexLoader.cc

        ntkernel/NtRecallMsgHook.cc

        )

set_target_properties(qauxv PROPERTIES
        CXX_EXTENSIONS OFF
        POSITION_INDEPENDENT_CODE ON
        )

target_include_directories(qauxv PRIVATE ../../../../libs/linux-syscall-support)
target_include_directories(qauxv PRIVATE ../../../../libs/LSPlant/lsplant/src/main/jni)
target_include_directories(qauxv PRIVATE .)

target_compile_definitions(qauxv PRIVATE QAUXV_VERSION=\"${QAUXV_VERSION}\")
target_link_options(qauxv PRIVATE "-Wl,-e,__libqauxv_main")

target_link_libraries(qauxv dobby mmkv dexkit_static unwindstack base silk
        c dl fmt-header-only z lsplant_static absl::flat_hash_map
        ${ANDROID_LIBS})
