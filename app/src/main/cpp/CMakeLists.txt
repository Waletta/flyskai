cmake_minimum_required(VERSION 3.22.1)
project(llama_flyskai C CXX)
include(CheckIncludeFileCXX)

# Set the C++ standard if needed.
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)


# Specify the directory where llama.cpp is located.
set(LLAMA_DIR "/Users/Walitta/AndroidStudioProjects/flyskai/extern/llama.cpp")
include_directories(
        ${LLAMA_DIR}/include
        ${LLAMA_DIR}/src
        ${LLAMA_DIR}/common
        ${LLAMA_DIR}/ggml/include
        ${LLAMA_DIR}/ggml/src
)

set(GGML_SOURCES
        ${LLAMA_DIR}/ggml/src/ggml.c
        ${LLAMA_DIR}/ggml/src/ggml-alloc.c
        ${LLAMA_DIR}/ggml/src/ggml-backend-impl.h
        ${LLAMA_DIR}/ggml/src/ggml-backend.cpp
        ${LLAMA_DIR}/ggml/src/ggml-backend-reg.cpp
        ${LLAMA_DIR}/ggml/src/ggml-quants.c
        ${LLAMA_DIR}/ggml/src/ggml-quants.h
        ${LLAMA_DIR}/ggml/src/gguf.cpp
        ${LLAMA_DIR}/ggml/src/ggml-threading.cpp
        ${LLAMA_DIR}/ggml/src/ggml-threading.h
)

# Collect llama.cpp source files
set(LLAMA_SOURCES
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
        ${LLAMA_DIR}/common/common.h
        ${LLAMA_DIR}/common/common.cpp
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

add_library(llama SHARED ${JNI_SRC})
target_link_libraries(llama PRIVATE llama_core ggggml)

# Locate the log library required by Android
find_library(log-lib log)
target_link_libraries(llama PRIVATE ${log-lib})

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
