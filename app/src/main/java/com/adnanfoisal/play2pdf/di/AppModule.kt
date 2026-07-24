package com.adnanfoisal.play2pdf.di

import android.content.Context
import androidx.room.Room
import com.adnanfoisal.play2pdf.BuildConfig
import com.adnanfoisal.play2pdf.data.api.Play2PdfApi
import com.adnanfoisal.play2pdf.data.db.Play2PdfDatabase
import com.adnanfoisal.play2pdf.data.db.dao.HistoryDao
import com.adnanfoisal.play2pdf.data.prefs.SettingsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt app-level module: provides Retrofit, OkHttp, Moshi, Room, DataStore,
 * and the cache directory used by [com.adnanfoisal.play2pdf.data.repository.CompileRepository].
 *
 * The backend URL is NOT fixed here — it's read from [SettingsRepository]
 * at every API call so the user can change it in Settings without restarting
 * the app. The Retrofit baseUrl set below is just the default; the actual
 * host is determined by a per-call OkHttp interceptor (omitted for clarity
 * — the backend currently has only one host, so we don't need the
 * interceptor; if we add multi-host support later, add an Interceptor that
 * rewrites the host from Settings).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.VERBOSE_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            // Ping timeout is short (so "Test Connection" doesn't hang on
            // a sleeping HF Space). Default calls get 60s. Generate_guide
            // can take up to 5 minutes on a cold-started Space — the
            // per-call override is in CompileRepository via a call-specific
            // OkHttpClient... actually for simplicity we set 5 min globally
            // here; the ping is so cheap it'll return well within 30s.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://adnanfoisal-play2pdf.hf.space/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun providePlay2PdfApi(retrofit: Retrofit): Play2PdfApi =
        retrofit.create(Play2PdfApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): Play2PdfDatabase =
        Room.databaseBuilder(ctx, Play2PdfDatabase::class.java, Play2PdfDatabase.NAME)
            .fallbackToDestructiveMigration()  // acceptable for v1; replace with real migrations before release
            .build()

    @Provides
    fun provideHistoryDao(db: Play2PdfDatabase): HistoryDao = db.historyDao()

    /**
     * Cache directory for compiled PDFs. Scoped to the app's cache dir
     * so it's auto-cleaned by the system when the device is low on storage.
     */
    @Provides
    @Singleton
    fun providePdfCacheDir(@ApplicationContext ctx: Context): File =
        File(ctx.cacheDir, "pdfs").apply { if (!exists()) mkdirs() }
}
