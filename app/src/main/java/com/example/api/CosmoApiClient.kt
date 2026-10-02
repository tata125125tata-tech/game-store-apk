package com.example.api

import com.example.model.StoreApp
import com.example.model.StoreCategory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface CosmoApiService {
    @GET("api/apps")
    suspend fun getApps(): List<StoreApp>

    @GET("api/categories")
    suspend fun getCategories(): List<StoreCategory>

    @GET("api/apps/{id}")
    suspend fun getAppDetails(@Path("id") appId: Long): StoreApp

    @POST("api/apps/{id}/download")
    suspend fun recordDownload(@Path("id") appId: Long): Map<String, Any>
}

object CosmoApiClient {
    private const val BASE_URL = "https://holy-firefly-9726.play125store.workers.dev/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val apiService: CosmoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CosmoApiService::class.java)
    }
}
