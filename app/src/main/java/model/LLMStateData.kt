package model

const val DEFAULT_LLM_STORAGE_DIR = "/storage/emulated/0/LLM_Models/"

data class LLMStateData(
    val id: String,                         // Unique identifier for the LLM instance
    val configuration: LLMConfiguration,    // Metadata about the LLM (e.g., model type, version)
    val temperature: Float = 1.0f,          // Sampling temperature for generation
    val maxTokens: Int = 512,               // Max tokens for output
    val contextWindow: Int = 4096,          // Context window size
)

data class LLMConfiguration(
    val modelId: String,                    // Unique model identifier (e.g., "Grok-3")
    val modelName: String,                  // Human-readable name (e.g., "Grok 3")
    val version: String,                    // Model version (e.g., "v1.2")
    val architecture: String,               // Architecture (e.g., "Transformer")
    val parameterCount: Long,               // Number of parameters (e.g., 70_000_000_000)
    val storagePath: String = DEFAULT_LLM_STORAGE_DIR, // Path to model files on mobile storage
    val deploymentEnvironment: String,      // Environment (e.g., "xAI Cloud", "Mobile Local")
)


val knownLLMConfigurations = listOf(
    LLMConfiguration(
        modelId = "GROK2",
        modelName = "Grok 2",
        version = "v2.1",
        architecture = "Transformer",
        parameterCount = 3_000_000_000,
        storagePath = "$DEFAULT_LLM_STORAGE_DIR/grok2",
        deploymentEnvironment = "Mobile Local"
    ),
    LLMConfiguration(
        modelId = "PHI3",
        modelName = "Phi-3 Mini",
        version = "mini-128k",
        architecture = "Transformer",
        parameterCount = 3_821_079_552,
        storagePath = "$DEFAULT_LLM_STORAGE_DIR/Phi-3-mini-4k-instruct-q4.gguf",
        deploymentEnvironment = "Mobile Local"
    ),
)

data class LLMStateId(
    val modelId: String,                    // Matches LLMConfiguration.modelId
    val instanceId: String,                 // Unique instance identifier
    val latencyMs: Double?,                 // Current latency
    val throughputTokensPerSec: Double?,    // Current throughput
)