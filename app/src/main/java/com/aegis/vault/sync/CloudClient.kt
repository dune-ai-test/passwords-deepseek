package com.aegis.vault.sync

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Zero-dependency HTTP client for the Aegis cloud API
 * (see `cloud/README.md` for the server contract).
 *
 * Every method returns `null` for a network-level failure (offline, DNS,
 * timeout) and an [ApiResponse] for anything the server answered, so callers
 * can distinguish "couldn't reach the server" from a real status code.
 * Responses are parsed defensively — a bad error body never crashes a flow.
 */
class CloudClient(private val baseUrl: String) {

    data class ApiResponse(val status: Int, val json: JsonObject?) {
        private fun string(name: String): String? = (json?.get(name) as? JsonPrimitive)?.contentOrNull

        private fun int(name: String): Int? = (json?.get(name) as? JsonPrimitive)?.intOrNull

        /** Error slug from the server, e.g. "auth", "conflict", "rate-limited". */
        val error: String? get() = string("error")

        val revision: Int? get() = int("revision")

        val blob: String? get() = string("blob")

        val token: String? get() = string("token")

        val expiresAt: Long? get() = string("expiresAt")?.toLongOrNull()

        val updatedAt: Long? get() = string("updatedAt")?.toLongOrNull()

        val pcPub: String? get() = string("pcPub")

        val statusField: String? get() = string("status")
    }

    private val json = Json { ignoreUnknownKeys = true }

    /* ── Sync API ─────────────────────────────────────────────────────────── */

    suspend fun register(accountId: String, authKey: String): ApiResponse? =
        call("POST", "/v1/register", credsBody(accountId, authKey), null)

    suspend fun login(accountId: String, authKey: String): ApiResponse? =
        call("POST", "/v1/login", credsBody(accountId, authKey), null)

    suspend fun getVault(token: String): ApiResponse? =
        call("GET", "/v1/vault", null, token)

    suspend fun putVault(token: String, blob: String, baseRevision: Int): ApiResponse? =
        call(
            "PUT",
            "/v1/vault",
            buildJsonObject {
                put("blob", blob)
                put("baseRevision", baseRevision)
            },
            token,
        )

    suspend fun relink(
        oldAccountId: String,
        oldAuthKey: String,
        newAccountId: String,
        newAuthKey: String,
    ): ApiResponse? = call(
        "POST",
        "/v1/relink",
        buildJsonObject {
            put("oldAccountId", oldAccountId)
            put("oldAuthKey", oldAuthKey)
            put("newAccountId", newAccountId)
            put("newAuthKey", newAuthKey)
        },
        null,
    )

    suspend fun deleteAccount(token: String): ApiResponse? =
        call("POST", "/v1/delete", buildJsonObject { }, token)

    /* ── Share relay ──────────────────────────────────────────────────────── */

    suspend fun relayLookup(code: String): ApiResponse? =
        call("POST", "/api/relay/lookup", buildJsonObject { put("code", code) }, null)

    suspend fun relayUpload(
        code: String,
        senderPub: String,
        nonce: String,
        ciphertext: String,
    ): ApiResponse? = call(
        "POST",
        "/api/relay/upload",
        buildJsonObject {
            put("code", code)
            put("senderPub", senderPub)
            put("nonce", nonce)
            put("ciphertext", ciphertext)
        },
        null,
    )

    /* ── Plumbing ─────────────────────────────────────────────────────────── */

    private fun credsBody(accountId: String, authKey: String): JsonObject = buildJsonObject {
        put("accountId", accountId)
        put("authKey", authKey)
    }

    private suspend fun call(
        method: String,
        path: String,
        body: JsonObject?,
        token: String?,
    ): ApiResponse? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(baseUrl.trimEnd('/') + path)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 30_000
                doOutput = body != null
                setRequestProperty("Accept", "application/json")
                if (body != null) setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (token != null) setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val parsed = if (text.isBlank()) {
                null
            } else {
                try {
                    json.parseToJsonElement(text).jsonObject
                } catch (e: Exception) {
                    null
                }
            }
            ApiResponse(status, parsed)
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            null
        } finally {
            try {
                connection?.disconnect()
            } catch (e: Exception) {
                // nothing to clean up
            }
        }
    }
}
