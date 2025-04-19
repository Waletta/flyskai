cmake_minimum_required(VERSION 3.22.1)
project(llama_flyskai)

# Set the C++ standard if needed.
set(CMAKE_CXX_STANDARD 17)

# Specify the directory where llama.cpp is located.
set(LLAMA_DIR "/Users/Walitta/AndroidStudioProjects/flyskai/extern/llama.cpp")

# Include the headers from llama.cpp
set(LLAMA_INCLUDE_DIR "${LLAMA_DIR}/include")

# Add the include directory so that the compiler can locate "llama.h".
include_directories(${LLAMA_INCLUDE_DIR})
include_directories(${LLAMA_DIR})
include_directories(${LLAMA_DIR}/ggml/include)


# Collect the llama.cpp source files.
file(GLOB LLAMA_SOURCES
        "${LLAMA_DIR}/llama.cpp"
        "${LLAMA_DIR}/llama-adapter.cpp"
        "${LLAMA_DIR}/llama-arch.cpp"
)

# List your JNI wrapper source file(s)
set(JNI_SRC
        native-flyskai.cpp
)

# Create the shared library that will be used by your Android app
add_library(llama SHARED ${LLAMA_SOURCES} ${JNI_SRC})

# Locate the log library required by Android
find_library(log-lib log)
target_link_libraries(llama ${log-lib})
