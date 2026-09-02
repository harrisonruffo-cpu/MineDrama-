package com.example

import android.app.Application
import android.util.Log

class LitoralNovelasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LitoralApp", "Exceção tratada na thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            Appwrite.init(this)
            Log.d("LitoralApp", "Aplicação Litoral Novelas inicializada com sucesso")
        } catch (e: Throwable) {
            Log.e("LitoralApp", "Erro na inicialização do Appwrite: ${e.message}", e)
        }
    }
}
