cmake_minimum_required(VERSION 3.4.1)
project(dex_kit_trampoline)

find_program(CCACHE ccache)

if (CCACHE)
    set(CMAKE_CXX_COMPILER_LAUNCHER ${CCACHE})
    set(CMAKE_C_COMPILER_LAUNCHER ${CCACHE})
endif ()

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

set(DEX_BUILDER_SRC_ROOT "../dex_builder")

set(DB_SOURCES ../dex_kit/dex_kit.cc)

set(CFLAGS -flto -fvisibility=default)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -O3 -fPIC")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -O3 -fPIC")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pthread")

add_library(dex_kit_trampoline STATIC ${DB_SOURCES})
target_include_directories(dex_kit_trampoline PUBLIC ${DEX_BUILDER_SRC_ROOT}/include)
target_include_directories(dex_kit_trampoline PUBLIC ../dex_kit/include)
target_link_libraries(dex_kit_trampoline PUBLIC z)
