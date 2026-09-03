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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
 * The backend URL is NOT fixed here — the [provideOkHttpClient] interceptor
 * rewrites every request's scheme/host/port to the value persisted in
 * [SettingsRepository] (editable in Settings), so changing the backend takes
 * effect on the very next call without an app restart. The Retrofit baseUrl
 * below is only the path template plus the compile-time default.
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
    fun provideOkHttpClient(settings: SettingsRepository): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.VERBOSE_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        // Host of the compile-time default backend. ONLY requests aimed at
        // this host get redirected to the user's configured backend — the
        // same OkHttp client is also used for YouTube/Gemini key validation,
        // and rewriting those would send them to the Play2PDF backend
        // (which answers 404, making valid API keys look "Offline").
        val defaultBackendHost = BuildConfig.DEFAULT_BACKEND_URL.toHttpUrlOrNull()?.host

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            // Dynamic backend URL: read the user's configured backend and
            // rewrite ONLY our own API calls to point at it. Interceptors run
            // on OkHttp dispatcher threads (never main), so the blocking
            // DataStore read is safe here.
            .addInterceptor { chain ->
                val request = chain.request()
                val isOurBackend = defaultBackendHost != null &&
                    request.url.host == defaultBackendHost
                if (!isOurBackend) {
                    // Third-party host (googleapis.com, ...) — never touch it.
                    return@addInterceptor chain.proceed(request)
                }
                val configured = runBlocking {
                    settings.settings.first().backendUrl
                }.trim().removeSuffix("/")
                val target = configured.toHttpUrlOrNull()
                if (target != null && target.host != request.url.host) {
                    val rewritten = request.url.newBuilder()
                        .scheme(target.scheme)
                        .host(target.host)
                        .port(target.port)
                        .build()
                    chain.proceed(request.newBuilder().url(rewritten).build())
                } else {
                    // Unchanged (or the configured URL is unparseable — keep
                    // the default rather than failing the call).
                    chain.proceed(request)
                }
            }
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
            // Trailing "/" is required by Retrofit; paths come from Play2PdfApi.
            // The host is swapped at request time by the interceptor above.
            .baseUrl(BuildConfig.DEFAULT_BACKEND_URL + "/")
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
            // No destructive fallback. Schema is v1 and unchanged; any future
            // version bump without a real Migration now fails loudly at dev
            // time instead of silently wiping the user's history in prod.
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
