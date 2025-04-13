package model
import android.content.Context
import android.util.Log
import ai.onnxruntime.*
import ai.djl.sentencepiece.SpTokenizer
import ai.onnxruntime.OrtSession.SessionOptions
import java.io.File

class PhiModel private constructor(private val context: Context) {

    private var session: OrtSession? = null
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val tokenizer: SpTokenizer

    companion object {
        private const val ONNX_FILE = "cpu_and_mobile/phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx"
        const val LLMS_DIR = "llms_data"
        const val DATA_FILE_NAME = "phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx.data"
        const val DRIVE_DOWNLOAD_URL = "https://drive.google.com/uc?export=download&id=1M3vsH0jsveNHqbFXdwGxjyWYuHPsGakI"
        const val EXACT_FILE_SIZE = 2_722_861_056L
        private const val TOKENIZER_FILE = "cpu_and_mobile/tokenizer.model"
        private const val EOS_TOKEN = "<|endoftext|>"

        // Thread-safe singleton
        @Volatile private var INSTANCE: PhiModel? = null
        fun getInstance(context: Context): PhiModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PhiModel(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        tokenizer = loadTokenizer()
        loadModel()
    }

    private fun loadTokenizer(): SpTokenizer {
        return try {
            val tokenizerBytes = context.assets.open(TOKENIZER_FILE).readBytes()
            SpTokenizer(tokenizerBytes)
        } catch (e: Exception) {
            Log.e("PhiModel", "❌ Failed to load tokenizer: ${e.message}", e)
            throw RuntimeException("Tokenizer failed to load")
        }
    }

    private fun loadModel() {
        try {
            // 1. Load ONNX from assets (included in APK)
            val onnxFile = extractAssetToFile(context, ONNX_FILE)

            // 2. Verify data file exists locally
            val dataFile = getDataFile()
            if (!dataFile.exists() || dataFile.length() != EXACT_FILE_SIZE) {
                throw IllegalStateException("AI Model file not available")
            }

            // 3. Initialize ONNX runtime
            session = env.createSession(onnxFile.absolutePath, SessionOptions())
            Log.d("PhiModel", "✅ Model initialized successfully")
        } catch (e: Exception) {
            Log.e("PhiModel", "❌ Model initialization failed", e)
        }
    }

    private fun extractAssetToFile(context: Context, assetPath: String): File {
        val input = context.assets.open(assetPath)
        val outFile = File(context.cacheDir, File(assetPath).name)
        input.use { inputStream ->
            outFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return outFile
    }

    private fun getDataFile(): File {
        val dir = File(context.filesDir, LLMS_DIR).apply { mkdirs() }
        return File(dir, DATA_FILE_NAME)
    }

    fun isReady(): Boolean = session != null

    suspend fun generateText(
        prompt: String,
        maxLength: Int = 100,
        temperature: Float = 0.7f
    ): String {
        if (!isReady()) return "⚠️ Model not loaded"

        // Tokenize input
        val inputIds = tokenizer.tokenize(prompt).map { it.toLong() }.toLongArray()
        var currentInput = inputIds
        val generatedTokens = mutableListOf<Long>()

        // Get EOS token ID
        val eosTokenId = tokenizer.tokenize(EOS_TOKEN).first().toLong()

        repeat(maxLength) {
            try {
                // Create input tensor (shape: [1, seq_len])
                val inputTensor = OnnxTensor.createTensor(env, arrayOf(currentInput))

                // Run inference
                val inputName = session!!.inputNames.first()
                val output = session!!.run(mapOf(inputName to inputTensor))
                inputTensor.close()
                val rawOutput = output[0].value

                // Get logits (shape: [1, seq_len, vocab_size])
                val logits = when {
                    rawOutput is Array<*> && rawOutput.isNotEmpty() -> {
                        val firstDim = rawOutput[0]
                        when {
                            firstDim is Array<*> && firstDim.isNotEmpty() -> {
                                val secondDim = firstDim[0]
                                if (secondDim is FloatArray) {
                                    @Suppress("UNCHECKED_CAST")
                                    rawOutput as Array<Array<FloatArray>>
                                } else {
                                    throw IllegalStateException("Unexpected output dimension type")
                                }
                            }
                            else -> throw IllegalStateException("Unexpected output shape")
                        }
                    }
                    else -> throw IllegalStateException("Invalid model output")
                }
                val lastTokenLogits = logits[0].last()

                // Apply temperature sampling
                val nextToken = sampleToken(lastTokenLogits, temperature)

                // Stop if EOS token is generated
                if (nextToken == eosTokenId) return@repeat

                // Append token and update input
                generatedTokens.add(nextToken)
                currentInput = currentInput + nextToken

            } catch (e: Exception) {
                Log.e("PhiModel", "❌ Generation failed: ${e.message}", e)
                return@repeat
            }
        }

        return decodeTokens(generatedTokens)
    }

    private fun sampleToken(logits: FloatArray, temperature: Float): Long {
        // Apply temperature scaling
        val scaledLogits = logits.map { it / temperature }.toFloatArray()

        // Softmax to get probabilities
        val expLogits = scaledLogits.map { kotlin.math.exp(it.toDouble()) }
        val sumExp = expLogits.sum()
        val probs = expLogits.map { it / sumExp }

        // Sample from distribution
        var randomer = Math.random().toFloat()
        for ((index, prob) in probs.withIndex()) {
            randomer -= prob.toFloat()
            if (randomer <= 0) return index.toLong()
        }
        return probs.indices.last().toLong() // fallback
    }

    private val buildSentenceMethod by lazy {
        tokenizer::class.java.methods.find { it.name == "buildSentence" }
    }
    private val decodeMethod by lazy {
        tokenizer::class.java.methods.find { it.name == "decode" }
    }

    private fun decodeTokens(tokens: List<Long>): String {
        return when {
            buildSentenceMethod != null -> {
                buildSentenceMethod!!.invoke(tokenizer, tokens.map { it.toString() }) as String
            }
            decodeMethod != null -> {
                decodeMethod!!.invoke(tokenizer, tokens.map { it.toInt() }) as String
            }
            else -> {
                tokens.joinToString(" ") { it.toString() }
            }
        }
    }
}