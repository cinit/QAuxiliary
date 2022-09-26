cmake_minimum_required(VERSION 3.4.1)
project(dexkit)

add_subdirectory(DexKit/Core Core)

add_library(${PROJECT_NAME} STATIC
        native-bridge.cpp)

find_library(log-lib log)

target_link_libraries(${PROJECT_NAME}
        dex_kit_static
        z
        ${log-lib})
