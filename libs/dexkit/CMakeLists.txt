cmake_minimum_required(VERSION 3.4.1...3.27)
project(dexkit)

find_program(CCACHE ccache)

set(DEXKIT_CORE_DIR_BASE DexKit/Core)

if (CCACHE)
    set(CMAKE_CXX_COMPILER_LAUNCHER ${CCACHE})
    set(CMAKE_C_COMPILER_LAUNCHER ${CCACHE})
endif ()

set(CMAKE_CXX_STANDARD 20)
option(DEXKIT_ENABLE_INTERNAL_METRICS "Compile internal metrics into core" OFF)

# file(GLOB_RECURSE SLICER_SRC_FILES CONFIGURE_DEPENDS third_party/slicer/*.cc)
# dex ir slicer resues LSPlant's dex_builder to avoid diamond dependency problem,
# which is verified to cause native crash
file(GLOB_RECURSE DEXKIT_SRC_FILES CONFIGURE_DEPENDS ${DEXKIT_CORE_DIR_BASE}/dexkit/*.cpp)

# set(SLICER_SOURCES ${SLICER_SRC_FILES})

set(COMPILE_FLAGS "-Wno-empty-body -Wno-unused-private-field \
    -Wno-unused-value -Wno-unused-variable -Wno-unused-function")
set(CMAKE_C_FLAGS "${CMAKE_CXX_FLAGS} ${COMPILE_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${COMPILE_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pthread")

add_library(${PROJECT_NAME}_static
        STATIC
        ${DEXKIT_SRC_FILES}
)
# removed "${SLICER_SOURCES}"

# workaround: include overlay: fix tools/dexter/slicer/export/slicer/dex_instruction_list.h problem
target_include_directories(${PROJECT_NAME}_static
        PRIVATE
        include_overlay
        PUBLIC
        ${DEXKIT_CORE_DIR_BASE}/dexkit/include
        ${DEXKIT_CORE_DIR_BASE}/third_party/thread_helper
        ${DEXKIT_CORE_DIR_BASE}/third_party/aho_corasick_trie
        ${DEXKIT_CORE_DIR_BASE}/third_party/flatbuffers/include
        ../LSPlant/lsplant/src/main/jni/external/dex_builder/include
        ../LSPlant/lsplant/src/main/jni/external/dex_builder/include/slicer
)
# removed "third_party/slicer/export", "third_party/parallel_hashmap"

set_target_properties(${PROJECT_NAME}_static PROPERTIES POSITION_INDEPENDENT_CODE ON)
if (DEXKIT_ENABLE_INTERNAL_METRICS)
    target_compile_definitions(${PROJECT_NAME}_static PUBLIC DEXKIT_ENABLE_INTERNAL_METRICS=1)
else ()
    target_compile_definitions(${PROJECT_NAME}_static PUBLIC DEXKIT_ENABLE_INTERNAL_METRICS=0)
endif ()

target_link_libraries(${PROJECT_NAME}_static
        PUBLIC
        z
        phmap
        dex_builder_static
)

# workaround: for access private dex::Reader::ExtractAnnotationSet
foreach (f ${DEXKIT_SRC_FILES})
    if (f MATCHES "dex_item\\.cpp$")
        set_source_files_properties(
                ${f}
                PROPERTIES
                COMPILE_DEFINITIONS "private=public"
        )
    endif ()
endforeach ()
