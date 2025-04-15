#include <jni.h>
#include <string>
#include <vector>
#include "llama.h"
#include <android/log.h>

#define LOG_TAG "FlyskAINative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)


struct ModelContext {
    llama_model *model;
    llama_context *ctx;
};

extern "C" JNIEXPORT jlong JNICALL
Java_model_PhiModel_initModel(JNIEnv *env, jobject, jstring model_path) {

    if (model_path == nullptr) {
        LOGE("Model path is null");
        return 0;
    }

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (!path) {
        LOGE("Failed to retrieve the model path string");
        return 0;
    }

    // Load model with default parameters.
    llama_model_params model_params = llama_model_default_params();
    llama_model *model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (!model) {
        LOGE("Model load failed");
        llama_backend_free();
        return 0;
    }

    // Configure context parameters.
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 4096;        // Match Phi-3's 4k context
    ctx_params.n_threads = 4;       // Optimal for mobile devices
    ctx_params.n_threads_batch = 4;

    llama_context *ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Context creation failed");
        llama_model_free(model);
        return 0;
    }

    // Create and return the native handle.
    auto* handle = new ModelContext{model, ctx};
    LOGE("Model initialized");
    return reinterpret_cast<jlong>(handle);
}


extern "C" JNIEXPORT void JNICALL
Java_model_PhiModel_freeModel(
        JNIEnv*,
        jobject,
        jlong handle_ptr
) {
    if (handle_ptr == 0) return;

    auto* ctx = reinterpret_cast<ModelContext*>(handle_ptr);
    llama_free(ctx->ctx);
    llama_model_free(ctx->model);  // Updated to modern API
    delete ctx;
    LOGI("Model resources freed");
}


extern "C" JNIEXPORT jstring JNICALL
Java_model_PhiModel_generateText(JNIEnv *env, jobject,  jlong modelHandle, jstring prompt) {

    auto *ctx = reinterpret_cast<ModelContext*>(modelHandle);
    if (!ctx || !ctx->model || !ctx->ctx) return env->NewStringUTF("");

    const char *input = env->GetStringUTFChars(prompt, nullptr);
    std::string output;

    // 1. Get the vocabulary from the model
    const llama_vocab* vocab = llama_model_get_vocab(ctx->model);
    if (!vocab) {
        env->ReleaseStringUTFChars(prompt, input);
        return env->NewStringUTF("Error: Failed to get vocabulary");
    }


    // Tokenize prompt

    const size_t input_len = strlen(input);
    if (input_len > std::numeric_limits<int>::max()) {
        env->ReleaseStringUTFChars(prompt, input);
        return env->NewStringUTF("Error: Input too long");
    }
    const int input_len_int = static_cast<int>(input_len);

    // First pass - get token count
    const int n_tokens_max = llama_tokenize(
            vocab,
            input,
            input_len_int,
            nullptr,
            0,
            true,   // add_special (add BOS token)
            true    // parse_special (handle special tokens)
    );

    if (n_tokens_max <= 0) {
        env->ReleaseStringUTFChars(prompt, input);
        return env->NewStringUTF("Error: Tokenization failed");
    }

    // Second pass - actual tokenization
    std::vector<llama_token> tokens(n_tokens_max);
    const int n_tokens = llama_tokenize(
            vocab,
            input,
            input_len_int,
            tokens.data(),
            static_cast<int>(tokens.size()),
            true,   // add_special
            true    // parse_special
    );

    env->ReleaseStringUTFChars(prompt, input);
    if (n_tokens <= 0) {
        return env->NewStringUTF("Error: Tokenization failed");
    }

    return env->NewStringUTF(output.c_str());

}