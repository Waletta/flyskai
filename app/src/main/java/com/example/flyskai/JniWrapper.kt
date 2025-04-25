package com.example.flyskai

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class JniWrapper {

    private val tag: String? = this::class.simpleName
//    private val threadLocalState: ThreadLocal<State> = ThreadLocal.withInitial { State.Idle }

    private val runLoop: CoroutineDispatcher = Executors.newSingleThreadExecutor {
        thread(start = false, name = "Llm-RunLoop") {
            Log.d(tag, "Dedicated thread for native code: ${Thread.currentThread().name}")

            // No-op if called more than once.
            System.loadLibrary("llama-android")

            // Set llama log handler to Android
            log_to_android()
            backend_init(false)

            Log.d(tag, system_info())

            it.run()
        }.apply {
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception: Throwable ->
                Log.e(tag, "Unhandled exception", exception)
            }
        }
    }.asCoroutineDispatcher()


    private external fun log_to_android()
    private external fun load_model(filename: String): Long
    private external fun free_model(model: Long)
    private external fun new_context(model: Long): Long
    private external fun free_context(context: Long)
    private external fun backend_init(numa: Boolean)
    private external fun backend_free()
    private external fun new_batch(nTokens: Int, embd: Int, nSeqMax: Int): Long
    private external fun free_batch(batch: Long)
    private external fun new_sampler(): Long
    private external fun free_sampler(sampler: Long)
    private external fun bench_model(
        context: Long,
        model: Long,
        batch: Long,
        pp: Int,
        tg: Int,
        pl: Int,
        nr: Int
    ): String

    private external fun system_info(): String
    private val _instance: JniWrapper = JniWrapper()
    fun instance(): JniWrapper = _instance


//    suspend fun load(pathToModel: String) {
//        withContext(runLoop) {
//            when (threadLocalState.get()) {
//                is State.Idle -> {
//                    val model = load_model(pathToModel)
//                    if (model == 0L)  throw IllegalStateException("load_model() failed")
//
//                    val context = new_context(model)
//                    if (context == 0L) throw IllegalStateException("new_context() failed")
//
//                    val batch = new_batch(512, 0, 1)
//                    if (batch == 0L) throw IllegalStateException("new_batch() failed")
//
//                    val sampler = new_sampler()
//                    if (sampler == 0L) throw IllegalStateException("new_sampler() failed")
//
//                    Log.i(tag, "Loaded model $pathToModel")
//                    threadLocalState.set(State.Loaded(model, context, batch, sampler))
//                }
//                else -> throw IllegalStateException("Model already loaded")
//            }
//        }
//    }

}