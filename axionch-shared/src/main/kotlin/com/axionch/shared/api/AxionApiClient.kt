package com.axionch.shared.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class AxionApiClient(
    private val baseUrl: String = "http://127.0.0.1:8010/",
    private val apiKey: String? = null,
    private val userEmail: String? = null,
    enableHttpLogging: Boolean = false
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .apply {
            if (enableHttpLogging) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
            }
        }
        .build()

    suspend fun health(): HealthResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}health")
            .get()
            .build()
        execute(request, HealthResponse::class.java)
    }

    suspend fun getAccounts(): List<AccountResponse> = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}accounts")
            .get()
            .build()
        val body = executeRaw(request)
        val listType = object : TypeToken<List<AccountResponse>>() {}.type
        gson.fromJson(body, listType)
    }

    suspend fun createAccount(requestBody: CreateAccountRequest): AccountResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}accounts")
            .post(gson.toJson(requestBody).toRequestBody(JSON))
            .build()
        execute(request, AccountResponse::class.java)
    }

    suspend fun deleteAccount(id: Int): Unit = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}accounts/$id")
            .delete()
            .build()
        executeRaw(request)
    }

    suspend fun createPost(requestBody: CreatePostRequest): PostResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}posts")
            .post(gson.toJson(requestBody).toRequestBody(JSON))
            .build()
        execute(request, PostResponse::class.java)
    }

    suspend fun dryRunPost(requestBody: CreatePostRequest): PostResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}posts/dry-run")
            .post(gson.toJson(requestBody).toRequestBody(JSON))
            .build()
        execute(request, PostResponse::class.java)
    }

    suspend fun getConfigStatus(): ConfigStatusResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}config/status")
            .get()
            .build()
        execute(request, ConfigStatusResponse::class.java)
    }

    suspend fun runConfigCheck(): ConfigCheckResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}config/check")
            .post("{}".toRequestBody(JSON))
            .build()
        execute(request, ConfigCheckResponse::class.java)
    }

    suspend fun getConfigSecurity(): ConfigSecurityResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}config/security")
            .get()
            .build()
        execute(request, ConfigSecurityResponse::class.java)
    }

    suspend fun getVideoFilters(): VideoFilterListResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}media/filters")
            .get()
            .build()
        execute(request, VideoFilterListResponse::class.java)
    }

    suspend fun applyVideoFilter(requestBody: VideoFilterApplyRequest): VideoFilterApplyResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}media/filters/apply")
            .post(gson.toJson(requestBody).toRequestBody(JSON))
            .build()
        execute(request, VideoFilterApplyResponse::class.java)
    }

    suspend fun getImageFilters(): ImageFilterListResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}media/image-filters")
            .get()
            .build()
        execute(request, ImageFilterListResponse::class.java)
    }

    suspend fun applyImageFilter(requestBody: ImageFilterApplyRequest): ImageFilterApplyResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}media/image-filters/apply")
            .post(gson.toJson(requestBody).toRequestBody(JSON))
            .build()
        execute(request, ImageFilterApplyResponse::class.java)
    }

    suspend fun getDryRunHistory(
        limit: Int = 25,
        platform: String? = null,
        successOnly: Boolean? = null
    ): DryRunHistoryResponse = withContext(Dispatchers.IO) {
        val base = "${normalizedBaseUrl()}posts/dry-run-history".toHttpUrlOrNull()
            ?: throw IOException("Invalid URL")
        val urlBuilder = base.newBuilder()
            .addQueryParameter("limit", limit.toString())
        if (!platform.isNullOrBlank()) {
            urlBuilder.addQueryParameter("platform", platform)
        }
        if (successOnly != null) {
            urlBuilder.addQueryParameter("success_only", successOnly.toString())
        }
        val request = withAuth(Request.Builder())
            .url(urlBuilder.build())
            .get()
            .build()
        execute(request, DryRunHistoryResponse::class.java)
    }

    suspend fun clearDryRunHistory(platform: String? = null): DryRunHistoryClearResponse = withContext(Dispatchers.IO) {
        val base = "${normalizedBaseUrl()}posts/dry-run-history".toHttpUrlOrNull()
            ?: throw IOException("Invalid URL")
        val urlBuilder = base.newBuilder()
        if (!platform.isNullOrBlank()) {
            urlBuilder.addQueryParameter("platform", platform)
        }
        val request = withAuth(Request.Builder())
            .url(urlBuilder.build())
            .delete()
            .build()
        execute(request, DryRunHistoryClearResponse::class.java)
    }

    suspend fun queuePost(requestBody: CreatePostRequest): QueuedPostResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}posts/queue")
            .post(gson.toJson(requestBody).toRequestBody(JSON))
            .build()
        execute(request, QueuedPostResponse::class.java)
    }

    suspend fun getPublishJob(jobId: Int): PublishJobStatusResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}posts/jobs/$jobId")
            .get()
            .build()
        execute(request, PublishJobStatusResponse::class.java)
    }

    suspend fun getPublishMetrics(): PublishMetricsResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}posts/metrics")
            .get()
            .build()
        execute(request, PublishMetricsResponse::class.java)
    }

    suspend fun getDeadLetters(limit: Int = 50): DeadLetterListResponse = withContext(Dispatchers.IO) {
        val base = "${normalizedBaseUrl()}posts/dead-letters".toHttpUrlOrNull()
            ?: throw IOException("Invalid URL")
        val request = withAuth(Request.Builder())
            .url(base.newBuilder().addQueryParameter("limit", limit.toString()).build())
            .get()
            .build()
        execute(request, DeadLetterListResponse::class.java)
    }

    suspend fun requeueDeadLetter(deadLetterId: Int): RequeueDeadLetterResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}posts/dead-letters/$deadLetterId/requeue")
            .post("{}".toRequestBody(JSON))
            .build()
        execute(request, RequeueDeadLetterResponse::class.java)
    }

    suspend fun oauthStart(platform: String, userEmail: String): OAuthStartResponse = withContext(Dispatchers.IO) {
        val base = "${normalizedBaseUrl()}oauth/$platform/start".toHttpUrlOrNull()
            ?: throw IOException("Invalid URL")
        val request = withAuth(Request.Builder())
            .url(base.newBuilder().addQueryParameter("user_email", userEmail).build())
            .get()
            .build()
        execute(request, OAuthStartResponse::class.java)
    }

    suspend fun oauthCallback(platform: String, code: String, state: String): OAuthCallbackResponse = withContext(Dispatchers.IO) {
        val base = "${normalizedBaseUrl()}oauth/$platform/callback".toHttpUrlOrNull()
            ?: throw IOException("Invalid URL")
        val request = withAuth(Request.Builder())
            .url(
                base.newBuilder()
                    .addQueryParameter("code", code)
                    .addQueryParameter("state", state)
                    .build()
            )
            .get()
            .build()
        execute(request, OAuthCallbackResponse::class.java)
    }

    suspend fun createUserApiKey(requestBody: CreateApiKeyRequest): ApiKeyResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}auth/keys")
            .post(gson.toJson(requestBody).toRequestBody(JSON))
            .build()
        execute(request, ApiKeyResponse::class.java)
    }

    suspend fun listUserApiKeys(userEmail: String): ApiKeyListResponse = withContext(Dispatchers.IO) {
        val base = "${normalizedBaseUrl()}auth/keys".toHttpUrlOrNull()
            ?: throw IOException("Invalid URL")
        val request = withAuth(Request.Builder())
            .url(base.newBuilder().addQueryParameter("user_email", userEmail).build())
            .get()
            .build()
        execute(request, ApiKeyListResponse::class.java)
    }

    suspend fun revokeUserApiKey(userEmail: String, keyId: String): ApiKeyRevokeResponse = withContext(Dispatchers.IO) {
        val base = "${normalizedBaseUrl()}auth/keys/$keyId".toHttpUrlOrNull()
            ?: throw IOException("Invalid URL")
        val request = withAuth(Request.Builder())
            .url(base.newBuilder().addQueryParameter("user_email", userEmail).build())
            .delete()
            .build()
        execute(request, ApiKeyRevokeResponse::class.java)
    }

    suspend fun createVaultEntry(requestBody: VaultEntryCreateRequest): VaultEntryResponse = withContext(Dispatchers.IO) {
        val request = withAuth(Request.Builder())
            .url("${normalizedBaseUrl()}vault")
            .post(gson.toJson(requestBody).toRequestBody(JSON))
            .build()
        execute(request, VaultEntryResponse::class.java)
    }

    suspend fun listVaultEntries(userEmail: String): VaultListResponse = withContext(Dispatchers.IO) {
        val base = "${normalizedBaseUrl()}vault".toHttpUrlOrNull()
            ?: throw IOException("Invalid URL")
        val request = withAuth(Request.Builder())
            .url(base.newBuilder().addQueryParameter("user_email", userEmail).build())
            .get()
            .build()
        execute(request, VaultListResponse::class.java)
    }

    suspend fun getVaultEntry(entryId: Int, userEmail: String): VaultEntryResponse = withContext(Dispatchers.IO) {
        val base = "${normalizedBaseUrl()}vault/$entryId".toHttpUrlOrNull()
            ?: throw IOException("Invalid URL")
        val request = withAuth(Request.Builder())
            .url(base.newBuilder().addQueryParameter("user_email", userEmail).build())
            .get()
            .build()
        execute(request, VaultEntryResponse::class.java)
    }

    suspend fun updateVaultEntry(
        entryId: Int,
        userEmail: String,
        requestBody: VaultEntryUpdateRequest
    ): VaultEntryResponse = withContext(Dispatchers.IO) {
        val base = "${normalizedBaseUrl()}vault/$entryId".toHttpUrlOrNull()
            ?: throw IOException("Invalid URL")
        val request = withAuth(Request.Builder())
            .url(base.newBuilder().addQueryParameter("user_email", userEmail).build())
            .patch(gson.toJson(requestBody).toRequestBody(JSON))
            .build()
        execute(request, VaultEntryResponse::class.java)
    }

    suspend fun deleteVaultEntry(entryId: Int, userEmail: String): VaultDeleteResponse = withContext(Dispatchers.IO) {
        val base = "${normalizedBaseUrl()}vault/$entryId".toHttpUrlOrNull()
            ?: throw IOException("Invalid URL")
        val request = withAuth(Request.Builder())
            .url(base.newBuilder().addQueryParameter("user_email", userEmail).build())
            .delete()
            .build()
        execute(request, VaultDeleteResponse::class.java)
    }

    private fun normalizedBaseUrl(): String {
        return if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    }

    private fun withAuth(builder: Request.Builder): Request.Builder {
        if (!apiKey.isNullOrBlank()) {
            builder.header("X-Axionch-Api-Key", apiKey)
        }
        if (!userEmail.isNullOrBlank()) {
            builder.header("X-Axionch-User-Email", userEmail)
        }
        return builder
    }

    private fun <T> execute(request: Request, clazz: Class<T>): T {
        val body = executeRaw(request)
        return gson.fromJson(body, clazz)
    }

    private fun executeRaw(request: Request): String {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $body")
            }
            return body
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun fromEnvironment(defaultBaseUrl: String = "http://127.0.0.1:8010/"): AxionApiClient {
            val env = System.getenv("AXIONCH_API_BASE_URL").orEmpty().trim()
            val base = if (env.isBlank()) defaultBaseUrl else env
            val apiKey = System.getenv("AXIONCH_API_KEY").orEmpty().trim().ifBlank { null }
            val userEmail = System.getenv("AXIONCH_USER_EMAIL").orEmpty().trim().ifBlank { null }
            return AxionApiClient(baseUrl = base, apiKey = apiKey, userEmail = userEmail)
        }
    }
}
