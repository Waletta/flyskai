package model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class ModelDownloader(private val context: Context) {

    companion object {
        private const val DRIVE_DOWNLOAD_URL = "https://drive.google.com/uc?export=download&id=1VJ7stjdr0LWJEj1nLIK0sPtAW_ideIwI"
        private const val BUFFER_SIZE = 8 * 1024 // 8KB buffer
    }

    suspend fun downloadIfNeeded(
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val destination = PhiModel.getModelFile(context)

        try {
            // Skip if file exists with correct size
            if (destination.exists() && destination.length() == PhiModel.EXACT_FILE_SIZE) {
                return@withContext Result.success(destination)
            }

            destination.parentFile?.mkdirs()

            // Start download
            val client = OkHttpClient()
            val request = Request.Builder().url(DRIVE_DOWNLOAD_URL).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to download: ${response.code}"))
                }

                response.body?.let { body ->
                    val totalBytes = body.contentLength()
                    var bytesDownloaded = 0L

                    FileOutputStream(destination).use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead
                                onProgress(bytesDownloaded, totalBytes)
                            }
                        }
                    }

                    // Verify download
                    return@withContext if (destination.length() == PhiModel.EXACT_FILE_SIZE) {
                        Result.success(destination)
                    } else {
                        destination.delete()
                        Result.failure(Exception("File size mismatch"))
                    }
                } ?: return@withContext Result.failure(Exception("Empty response body"))
            }
        } catch (e: Exception) {
            destination.delete() // Clean up partial download
            return@withContext Result.failure(e)
        }
    }
}