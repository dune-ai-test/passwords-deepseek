package com.aegis.vault.sync

import android.content.Context
import android.content.SharedPreferences

/**
 * Cloud-backup state stored in app-private SharedPreferences.
 *
 * What lives here: the server URL, the derived PII-free account id, the auth
 * key, the current bearer token, the last synced revision and timestamp.
 *
 * This is exactly the same exposure class as the vault file itself (both sit
 * in app-private storage), and no plaintext master password is ever stored —
 * only the derived credentials the server already knows.
 */
class CloudSyncStore(context: Context) {

    companion object {
        const val DEFAULT_SERVER = "https://aegis-cloud.pages.dev"

        private const val KEY_ENABLED = "enabled"
        private const val KEY_SERVER = "server"
        private const val KEY_ACCOUNT = "accountId"
        private const val KEY_AUTH = "authKey"
        private const val KEY_TOKEN = "token"
        private const val KEY_TOKEN_EXPIRES = "tokenExpiresAt"
        private const val KEY_REVISION = "revision"
        private const val KEY_LAST_BACKUP = "lastBackupAt"

        /** Accepts "host", "https://host" or "https://host/" and normalizes. */
        fun normalizeServer(input: String): String {
            var server = input.trim().trimEnd('/')
            if (server.isNotEmpty() && !server.startsWith("http://") && !server.startsWith("https://")) {
                server = "https://$server"
            }
            return server
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aegis_cloud", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER, DEFAULT_SERVER) ?: DEFAULT_SERVER
        set(value) = prefs.edit().putString(KEY_SERVER, normalizeServer(value)).apply()

    var accountId: String?
        get() = prefs.getString(KEY_ACCOUNT, null)
        set(value) = prefs.edit().putString(KEY_ACCOUNT, value).apply()

    var authKey: String?
        get() = prefs.getString(KEY_AUTH, null)
        set(value) = prefs.edit().putString(KEY_AUTH, value).apply()

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var tokenExpiresAt: Long
        get() = prefs.getLong(KEY_TOKEN_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_TOKEN_EXPIRES, value).apply()

    /** Server revision this device last synced (0 = never uploaded). */
    var revision: Int
        get() = prefs.getInt(KEY_REVISION, 0)
        set(value) = prefs.edit().putInt(KEY_REVISION, value).apply()

    /** When this device last pushed a backup (0 = never). */
    var lastBackupAt: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP, value).apply()

    fun hasCredentials(): Boolean = !accountId.isNullOrBlank() && !authKey.isNullOrBlank()

    fun client(): CloudClient = CloudClient(serverUrl)

    /** Current token when it still has >1 minute of life. */
    private fun liveToken(): String? =
        token?.takeIf { it.isNotBlank() && System.currentTimeMillis() < tokenExpiresAt - 60_000L }

    /**
     * A usable bearer token — re-logs in silently with the stored auth key
     * when the cached token is missing or stale. Returns null when offline,
     * out of credentials, or the server rejected the re-login.
     */
    suspend fun ensureToken(): String? {
        liveToken()?.let { return it }
        val accountId = accountId ?: return null
        val authKey = authKey ?: return null
        val response = client().login(accountId, authKey) ?: return null
        if (response.status != 200) return null
        val fresh = response.token ?: return null
        token = fresh
        tokenExpiresAt = response.expiresAt ?: 0L
        return fresh
    }

    /** Store a fresh session after enable / restore / relink. */
    fun saveSession(newToken: String, expiresAt: Long) {
        token = newToken
        tokenExpiresAt = expiresAt
    }

    /** Record a successful backup at `rev`. */
    fun markBackedUp(rev: Int) {
        revision = rev
        lastBackupAt = System.currentTimeMillis()
    }

    /** Turn cloud backup off locally (optionally forgetting the server too). */
    fun disable() {
        prefs.edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_ACCOUNT)
            .remove(KEY_AUTH)
            .remove(KEY_TOKEN)
            .remove(KEY_TOKEN_EXPIRES)
            .remove(KEY_REVISION)
            .remove(KEY_LAST_BACKUP)
            .apply()
    }
}
