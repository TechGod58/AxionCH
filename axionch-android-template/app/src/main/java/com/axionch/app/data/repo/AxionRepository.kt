package com.axionch.app.data.repo

import com.axionch.app.data.api.ApiModule
import com.axionch.shared.api.AccountResponse
import com.axionch.shared.api.ConfigCheckResponse
import com.axionch.shared.api.ConfigSecurityResponse
import com.axionch.shared.api.ConfigStatusResponse
import com.axionch.shared.api.CreateApiKeyRequest
import com.axionch.shared.api.CreateAccountRequest
import com.axionch.shared.api.CreatePostRequest
import com.axionch.shared.api.DeadLetterListResponse
import com.axionch.shared.api.DryRunHistoryClearResponse
import com.axionch.shared.api.DryRunHistoryResponse
import com.axionch.shared.api.HealthResponse
import com.axionch.shared.api.PublishMetricsResponse
import com.axionch.shared.api.OAuthCallbackResponse
import com.axionch.shared.api.OAuthStartResponse
import com.axionch.shared.api.PublishJobStatusResponse
import com.axionch.shared.api.QueuedPostResponse
import com.axionch.shared.api.ApiKeyListResponse
import com.axionch.shared.api.ApiKeyResponse
import com.axionch.shared.api.ApiKeyRevokeResponse
import com.axionch.shared.api.RequeueDeadLetterResponse
import com.axionch.shared.api.PostResponse
import com.axionch.shared.api.VaultDeleteResponse
import com.axionch.shared.api.VaultEntryCreateRequest
import com.axionch.shared.api.VaultEntryResponse
import com.axionch.shared.api.VaultEntryUpdateRequest
import com.axionch.shared.api.VaultListResponse

class AxionRepository {
    suspend fun getHealth(): HealthResponse = ApiModule.api().health()
    suspend fun getConfigStatus(): ConfigStatusResponse = ApiModule.api().getConfigStatus()
    suspend fun runConfigCheck(): ConfigCheckResponse = ApiModule.api().runConfigCheck()
    suspend fun getConfigSecurity(): ConfigSecurityResponse = ApiModule.api().getConfigSecurity()
    suspend fun getDryRunHistory(
        limit: Int = 25,
        platform: String? = null,
        successOnly: Boolean? = null
    ): DryRunHistoryResponse = ApiModule.api().getDryRunHistory(
        limit = limit,
        platform = platform,
        successOnly = successOnly
    )
    suspend fun clearDryRunHistory(platform: String? = null): DryRunHistoryClearResponse =
        ApiModule.api().clearDryRunHistory(platform = platform)

    suspend fun queuePost(
        email: String,
        body: String,
        imageUrl: String?,
        accountIds: List<Int>
    ): QueuedPostResponse {
        return ApiModule.api().queuePost(
            CreatePostRequest(
                user_email = email,
                body = body,
                image_url = imageUrl,
                account_ids = accountIds
            )
        )
    }

    suspend fun getPublishJob(jobId: Int): PublishJobStatusResponse = ApiModule.api().getPublishJob(jobId)
    suspend fun getPublishMetrics(): PublishMetricsResponse = ApiModule.api().getPublishMetrics()
    suspend fun getDeadLetters(limit: Int = 50): DeadLetterListResponse = ApiModule.api().getDeadLetters(limit)
    suspend fun requeueDeadLetter(deadLetterId: Int): RequeueDeadLetterResponse =
        ApiModule.api().requeueDeadLetter(deadLetterId)

    suspend fun oauthStart(platform: String, email: String): OAuthStartResponse =
        ApiModule.api().oauthStart(platform = platform, userEmail = email)

    suspend fun oauthCallback(platform: String, code: String, state: String): OAuthCallbackResponse =
        ApiModule.api().oauthCallback(platform = platform, code = code, state = state)

    suspend fun createUserApiKey(userEmail: String, label: String): ApiKeyResponse =
        ApiModule.api().createUserApiKey(CreateApiKeyRequest(user_email = userEmail, label = label))

    suspend fun listUserApiKeys(userEmail: String): ApiKeyListResponse =
        ApiModule.api().listUserApiKeys(userEmail)

    suspend fun revokeUserApiKey(userEmail: String, keyId: String): ApiKeyRevokeResponse =
        ApiModule.api().revokeUserApiKey(userEmail, keyId)

    suspend fun getAccounts(): List<AccountResponse> = ApiModule.api().getAccounts()

    suspend fun createMockAccount(
        email: String,
        platform: String,
        handle: String
    ): AccountResponse {
        return ApiModule.api().createAccount(
            CreateAccountRequest(
                user_email = email,
                platform = platform,
                handle = handle,
                access_token = "mock-token-$platform"
            )
        )
    }

    suspend fun deleteAccount(id: Int) {
        ApiModule.api().deleteAccount(id)
    }

    suspend fun createPost(
        email: String,
        body: String,
        imageUrl: String?,
        accountIds: List<Int>
    ): PostResponse {
        return ApiModule.api().createPost(
            CreatePostRequest(
                user_email = email,
                body = body,
                image_url = imageUrl,
                account_ids = accountIds
            )
        )
    }

    suspend fun dryRunPost(
        email: String,
        body: String,
        imageUrl: String?,
        accountIds: List<Int>
    ): PostResponse {
        return ApiModule.api().dryRunPost(
            CreatePostRequest(
                user_email = email,
                body = body,
                image_url = imageUrl,
                account_ids = accountIds
            )
        )
    }

    suspend fun createVaultEntry(
        userEmail: String,
        serviceName: String,
        username: String,
        password: String,
        notes: String?
    ): VaultEntryResponse = ApiModule.api().createVaultEntry(
        VaultEntryCreateRequest(
            user_email = userEmail,
            service_name = serviceName,
            username = username,
            password = password,
            notes = notes
        )
    )

    suspend fun listVaultEntries(userEmail: String): VaultListResponse =
        ApiModule.api().listVaultEntries(userEmail)

    suspend fun getVaultEntry(entryId: Int, userEmail: String): VaultEntryResponse =
        ApiModule.api().getVaultEntry(entryId = entryId, userEmail = userEmail)

    suspend fun updateVaultEntry(
        entryId: Int,
        userEmail: String,
        serviceName: String? = null,
        username: String? = null,
        password: String? = null,
        notes: String? = null
    ): VaultEntryResponse = ApiModule.api().updateVaultEntry(
        entryId = entryId,
        userEmail = userEmail,
        requestBody = VaultEntryUpdateRequest(
            service_name = serviceName,
            username = username,
            password = password,
            notes = notes
        )
    )

    suspend fun deleteVaultEntry(entryId: Int, userEmail: String): VaultDeleteResponse =
        ApiModule.api().deleteVaultEntry(entryId = entryId, userEmail = userEmail)
}


