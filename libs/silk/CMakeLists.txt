cmake_minimum_required(VERSION 3.8.0)

enable_language(C)

# we only have asm files for arm, but the ldmgtia instruction makes AS unhappy
# SKP_Silk_allpass_int_arm.S:100:2: error: invalid instruction

# find source files silk-v3-decoder/silk/src/*.c
file(GLOB_RECURSE SILK_SRC "silk-v3-decoder/silk/src/*.c")

add_library(silk STATIC ${SILK_SRC})

target_include_directories(silk PUBLIC "silk-v3-decoder/silk/interface")

target_compile_definitions(silk PRIVATE "NO_ASM")

set_target_properties(silk PROPERTIES
        C_STANDARD 11
        POSITION_INDEPENDENT_CODE ON
        )
