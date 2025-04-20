cmake_minimum_required(VERSION 3.22.1)
project(llama_flyskai)
# include(CheckIncludeFileCXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

set(LLAMA_DIR "/Users/Walitta/AndroidStudioProjects/flyskai/extern/llama.cpp")


set(BUILD_NUMBER 1)
set(BUILD_COMMIT "no-git-version")
set(BUILD_COMPILER "dummy-compiler")
set(BUILD_TARGET "dummy-target")

# Generate build-info.cpp from the template
configure_file(
        ${LLAMA_DIR}/common/build-info.cpp.in
        ${CMAKE_CURRENT_BINARY_DIR}/build-info.cpp
        @ONLY
)


include_directories(
        ${CMAKE_CURRENT_BINARY_DIR}
        ${LLAMA_DIR}/include
        ${LLAMA_DIR}/src
        ${LLAMA_DIR}/common
        ${LLAMA_DIR}/ggml/include
        ${LLAMA_DIR}/ggml/src
        ${CMAKE_CURRENT_BINARY_DIR}
)

set(GGML_SOURCES
        ${LLAMA_DIR}/ggml/src/ggml.c
        ${LLAMA_DIR}/ggml/src/ggml-alloc.c
        ${LLAMA_DIR}/ggml/src/ggml-backend.cpp
        ${LLAMA_DIR}/ggml/src/ggml-backend-impl.h
        ${LLAMA_DIR}/ggml/src/ggml-backend-reg.cpp
        ${LLAMA_DIR}/ggml/src/ggml-common.h
        ${LLAMA_DIR}/ggml/src/ggml-impl.h
        ${LLAMA_DIR}/ggml/src/ggml-opt.cpp
        ${LLAMA_DIR}/ggml/src/ggml-quants.c
        ${LLAMA_DIR}/ggml/src/ggml-quants.h
        ${LLAMA_DIR}/ggml/src/gguf.cpp
        ${LLAMA_DIR}/ggml/src/ggml-threading.cpp
        ${LLAMA_DIR}/ggml/src/ggml-threading.h
        ${LLAMA_DIR}/ggml/include/ggml.h
        ${LLAMA_DIR}/ggml/include/ggml-alloc.h
        ${LLAMA_DIR}/ggml/include/ggml-backend.h
        ${LLAMA_DIR}/ggml/include/ggml-cpp.h
        ${LLAMA_DIR}/ggml/include/ggml-cpu.h
        ${LLAMA_DIR}/ggml/include/gguf.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ggml-cpu.c
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ggml-cpu.cpp
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.cpp
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ggml-cpu-aarch64.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ggml-cpu-hbm.cpp
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ggml-cpu-hbm.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ggml-cpu-quants.c
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ggml-cpu-quants.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ggml-cpu-traits.cpp
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ggml-cpu-traits.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ggml-cpu-impl.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/common.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/binary-ops.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/binary-ops.cpp
        #${LLAMA_DIR}/ggml/src/ggml-cpu/unary-ops.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/unary-ops.cpp
        #${LLAMA_DIR}/ggml/src/ggml-cpu/simd-mappings.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/vec.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/vec.cpp
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ops.h
        #${LLAMA_DIR}/ggml/src/ggml-cpu/ops.cpp
)

set(LLAMA_SOURCES
        ${CMAKE_CURRENT_BINARY_DIR}/build-info.cpp  # <-- Add the generated file here
        ${LLAMA_DIR}/src/llama-adapter.cpp
        ${LLAMA_DIR}/src/llama-adapter.h
        ${LLAMA_DIR}/src/llama-arch.cpp
        ${LLAMA_DIR}/src/llama-arch.h
        ${LLAMA_DIR}/src/llama-batch.cpp
        ${LLAMA_DIR}/src/llama-batch.h
        ${LLAMA_DIR}/src/llama-chat.cpp
        ${LLAMA_DIR}/src/llama-chat.h
        ${LLAMA_DIR}/src/llama-context.cpp
        ${LLAMA_DIR}/src/llama-context.h
        ${LLAMA_DIR}/src/llama-cparams.cpp
        ${LLAMA_DIR}/src/llama-cparams.h
        ${LLAMA_DIR}/src/llama-grammar.cpp
        ${LLAMA_DIR}/src/llama-grammar.h
        ${LLAMA_DIR}/src/llama-graph.cpp
        ${LLAMA_DIR}/src/llama-graph.h
        ${LLAMA_DIR}/src/llama-hparams.cpp
        ${LLAMA_DIR}/src/llama-hparams.h
        ${LLAMA_DIR}/src/llama-impl.cpp
        ${LLAMA_DIR}/src/llama-impl.h
        ${LLAMA_DIR}/src/llama-io.cpp
        ${LLAMA_DIR}/src/llama-io.h
        ${LLAMA_DIR}/src/llama-kv-cache.cpp
        ${LLAMA_DIR}/src/llama-kv-cache.h
        ${LLAMA_DIR}/src/llama-memory.cpp
        ${LLAMA_DIR}/src/llama-memory.h
        ${LLAMA_DIR}/src/llama-mmap.cpp
        ${LLAMA_DIR}/src/llama-mmap.h
        ${LLAMA_DIR}/src/llama-model-loader.cpp
        ${LLAMA_DIR}/src/llama-model-loader.h
        ${LLAMA_DIR}/src/llama-model.cpp
        ${LLAMA_DIR}/src/llama-model.h
        ${LLAMA_DIR}/src/llama-quant.cpp
        ${LLAMA_DIR}/src/llama-quant.h
        ${LLAMA_DIR}/src/llama-sampling.cpp
        ${LLAMA_DIR}/src/llama-sampling.h
        ${LLAMA_DIR}/src/llama-vocab.cpp
        ${LLAMA_DIR}/src/llama-vocab.h
        ${LLAMA_DIR}/src/llama.cpp
        ${LLAMA_DIR}/src/unicode-data.cpp
        ${LLAMA_DIR}/src/unicode-data.h
        ${LLAMA_DIR}/src/unicode.cpp
        ${LLAMA_DIR}/src/unicode.h
        ${LLAMA_DIR}/common/arg.cpp
        ${LLAMA_DIR}/common/arg.h
        ${LLAMA_DIR}/common/build-info.cpp.in
        ${LLAMA_DIR}/common/chat.cpp
        ${LLAMA_DIR}/common/chat.h
        ${LLAMA_DIR}/common/common.cpp
        ${LLAMA_DIR}/common/common.h
        ${LLAMA_DIR}/common/console.cpp
        ${LLAMA_DIR}/common/console.h
        ${LLAMA_DIR}/common/json-schema-to-grammar.cpp
        ${LLAMA_DIR}/common/json-schema-to-grammar.h
        ${LLAMA_DIR}/common/llguidance.cpp
        ${LLAMA_DIR}/common/log.cpp
        ${LLAMA_DIR}/common/log.h
        ${LLAMA_DIR}/common/ngram-cache.cpp
        ${LLAMA_DIR}/common/ngram-cache.h
        ${LLAMA_DIR}/common/sampling.cpp
        ${LLAMA_DIR}/common/sampling.h
        ${LLAMA_DIR}/common/speculative.cpp
        ${LLAMA_DIR}/common/speculative.h
)

set(JNI_SRC
        native-flyskai.cpp
)

add_library(ggggml STATIC ${GGML_SOURCES})
target_compile_definitions(ggggml PRIVATE
        GGML_NO_ACCELERATE  # Disable Accelerate (macOS-specific)
        GGML_NO_OPENMP      # Disable OpenMP unless you have it configured
)

add_library(llama_core STATIC ${LLAMA_SOURCES})
target_link_libraries(llama_core PRIVATE ggggml)


find_library(log-lib log)
# find_library(android-lib android)

# target_link_libraries(llama PRIVATE ${android-lib})

add_library(llama_flyskai SHARED ${JNI_SRC})

target_link_libraries(llama_flyskai PRIVATE
        llama_core
        ggggml
        ${log-lib}
)

# Set architecture-specific compiler flags (from docs/android.md)
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8.7a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8.7a")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=x86-64")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=x86-64")
endif()

# Enable verbose output for debugging
set(CMAKE_VERBOSE_MAKEFILE ON)
