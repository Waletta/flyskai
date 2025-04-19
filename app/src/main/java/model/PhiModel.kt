package model

import android.content.Context
import android.util.Log
import java.io.File
import java.lang.ref.WeakReference

class PhiModel private constructor(private val context: Context) {

    @Volatile private var modelPtr: Long = 0
    private var modelLoaded: Boolean = false

    external fun initModel(modelPath: String): Long
    external fun generateText(handle: Long, prompt: String): String
    external fun freeModel(handle: Long)

    companion object {
        init {
            System.loadLibrary("llama_flyskai")  // Must match CMake target name
        }

        private const val GGUF_FILE = "phi-3-mini-4k-instruct-q4.gguf"
        const val LLMS_DIR = "llms_data"
        const val DRIVE_DOWNLOAD_URL = "https://drive.google.com/uc?export=download&id=1VJ7stjdr0LWJEj1nLIK0sPtAW_ideIwI"
        const val EXACT_FILE_SIZE = 2_722_861_056L
        private const val EOS_TOKEN = "<|end|>"

        private var weakInstance: WeakReference<PhiModel>? = null

        fun getInstance(context: Context): PhiModel {
            weakInstance?.get()?.let { return it }

            val newInstance = PhiModel(context.applicationContext).apply {
                loadModel()
            }

            weakInstance = WeakReference(newInstance)
            return newInstance
        }

        fun getModelFile(context: Context): File {
            return File(context.filesDir, "$LLMS_DIR/$GGUF_FILE").also {
                it.parentFile?.mkdirs()
            }
        }

        fun isModelDownloaded(context: Context): Boolean {
            val file = getModelFile(context)
            return file.exists() && file.length() == EXACT_FILE_SIZE
        }
    }

    private fun loadModel() {
        if (modelLoaded) {
            Log.d("PhiModel", "ℹ️ Model already loaded, skipping reinitialization")
            return
        }

        try {
            val modelFile = getModelFile()
            if (!modelFile.exists()) {
                throw IllegalStateException("Model file not found at ${modelFile.absolutePath}")
            }

            if (modelFile.length() != EXACT_FILE_SIZE) {
                Log.w("PhiModel", "⚠️ Model file size mismatch. Expected: $EXACT_FILE_SIZE, Actual: ${modelFile.length()}")
            }

            modelPtr = initModel(modelFile.absolutePath)
            if (modelPtr == 0L) {
                throw IllegalStateException("AI model initialization failed")
            }

            modelLoaded = true
            Log.d("PhiModel", "✅ Phi-3-mini GGUF model loaded successfully")

        } catch (e: Exception) {
            Log.e("PhiModel", "❌ Model initialization failed", e)
            throw RuntimeException("Failed to load Phi-3 model", e)
        }
    }

    fun unload() {
        if (!modelLoaded || modelPtr == 0L) return

        freeModel(modelPtr)
        modelPtr = 0L
        modelLoaded = false
        Log.d("PhiModel", "🧹 Model unloaded")
    }

    fun isReady(): Boolean {
        val ready = modelLoaded && modelPtr != 0L
        if (!ready) Log.w("PhiModel", "⚠️ Attempted use while model is not loaded")
        return ready
    }

    fun chat(prompt: String): String {
        if (!isReady()) return "⚠️ Model not loaded"

        return try {
            val output = generateText(modelPtr, prompt)
            Log.d("PhiModel", "✅ Generation successful")
            output
        } catch (e: Exception) {
            Log.e("PhiModel", "❌ Generation failed", e)
            "Error: ${e.message}"
        }
    }

    fun getModelFile(): File {
        val dir = File(context.filesDir, LLMS_DIR)
        return File(dir, GGUF_FILE)
    }

}
