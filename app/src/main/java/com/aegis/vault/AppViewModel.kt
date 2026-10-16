package com.aegis.vault

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.vault.crypto.BiometricKeys
import com.aegis.vault.crypto.CloudKeys
import com.aegis.vault.crypto.Crypto
import com.aegis.vault.crypto.ShareCrypto
import com.aegis.vault.data.CardItem
import com.aegis.vault.data.IdentityItem
import com.aegis.vault.data.LoginItem
import com.aegis.vault.data.NoteItem
import com.aegis.vault.data.PasswordTools
import com.aegis.vault.data.VaultData
import com.aegis.vault.data.VaultItem
import com.aegis.vault.data.VaultRepository
import com.aegis.vault.data.VaultSettings
import com.aegis.vault.data.Wordlist
import com.aegis.vault.sync.CloudClient
import com.aegis.vault.sync.CloudSyncStore
import com.aegis.vault.sync.SharePayload
import com.aegis.vault.ui.Biometric
import javax.crypto.Cipher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {

    enum class Stage { FIRST_RUN, LOCKED, UNLOCKED, DECOY }

    private val repo = VaultRepository(app)
    private val wordlist = Wordlist(app)
    private val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val _stage = MutableStateFlow(if (repo.vaultExists()) Stage.LOCKED else Stage.FIRST_RUN)
    val stage: StateFlow<Stage> = _stage.asStateFlow()

    private val _vault = MutableStateFlow<VaultData?>(null)
    val vault: StateFlow<VaultData?> = _vault.asStateFlow()

    /** On-disk size, refreshed after every write so the UI never shows stale bytes. */
    private val _vaultBytes = MutableStateFlow(repo.vaultSizeBytes())
    val vaultBytes: StateFlow<Long> = _vaultBytes.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _busyText = MutableStateFlow("Working…")
    val busyText: StateFlow<String> = _busyText.asStateFlow()

    val audit: StateFlow<PasswordTools.Audit?> = _vault
        .map { data -> data?.let { PasswordTools.audit(it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var lastInteraction = System.currentTimeMillis()
    private var backgroundedAt: Long? = null

    /** True while a system biometric prompt is up — auto-lock pauses then. */
    private var systemPromptActive = false

    private fun refreshVaultBytes() {
        _vaultBytes.value = repo.vaultSizeBytes()
    }

    /* ── Queries ──────────────────────────────────────────────────────────── */

    fun vaultExists(): Boolean = repo.vaultExists()

    fun biometricWrapExists(): Boolean = repo.biometricWrapExists()

    fun biometricReady(): Boolean = repo.biometricWrapExists() && BiometricKeys.exists()

    fun itemCountHint(): Int = repo.itemCountHint()

    fun vaultSizeBytes(): Long = repo.vaultSizeBytes()

    fun lastSavedHint(): Long? = repo.lastSavedHint()

    fun canUseBiometrics(): Boolean = Biometric.canAuthenticate(getApplication())

    fun wordlistWords(): List<String> = wordlist.words()

    fun generateRandom(
        length: Int,
        upper: Boolean = true,
        lower: Boolean = true,
        digits: Boolean = true,
        symbols: Boolean = true,
        avoidLookalikes: Boolean = false,
    ): String = PasswordTools.generateRandom(length, upper, lower, digits, symbols, avoidLookalikes)

    fun generateMemorable(count: Int = 4, separator: String = "-", capitalize: Boolean = true): String =
        PasswordTools.generateMemorable(wordlist.words(), count, separator, capitalize)

    /* ── Stage flow ───────────────────────────────────────────────────────── */

    fun createVault(
        password: String,
        enableBiometricAfter: Boolean,
        activity: FragmentActivity,
        onCreated: () -> Unit,
    ) {
        viewModelScope.launch {
            _busyText.value = "Creating your vault…"
            _busy.value = true
            val data = repo.create(password.toCharArray())
            _vault.value = data
            cacheSettings(data)
            refreshVaultBytes()
            lastInteraction = System.currentTimeMillis()
            _stage.value = Stage.UNLOCKED
            _busy.value = false
            onCreated()
            if (enableBiometricAfter) enableBiometric(activity) { }
        }
    }

    fun unlock(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _busyText.value = "Unlocking…"
            _busy.value = true
            val data = repo.unlock(password.toCharArray())
            _busy.value = false
            if (data != null) {
                _vault.value = data
                cacheSettings(data)
                refreshVaultBytes()
                lastInteraction = System.currentTimeMillis()
                _stage.value = Stage.UNLOCKED
                onResult(true)
            } else {
                onResult(false)
                // Wrong password: open the decoy vault instead of an error.
                enterDecoy()
            }
        }
    }

    fun lock() {
        // Flush pending changes to the cloud while the vault key is still in
        // memory-friendly territory; the file on disk is already written.
        if (cloud.enabled && cloudGeneration != cloudSyncedGeneration) {
            viewModelScope.launch { cloudBackupQuiet() }
        }
        cacheSettings(_vault.value)
        repo.lock()
        _vault.value = null
        _stage.value = if (repo.vaultExists()) Stage.LOCKED else Stage.FIRST_RUN
    }

    fun lockWithMessage() {
        lock()
        toast("Vault locked")
    }

    fun showUnlock() {
        _stage.value = Stage.LOCKED
    }

    fun wipeVault() {
        viewModelScope.launch {
            repo.wipeEverything()
            refreshVaultBytes()
            BiometricKeys.delete()
            val hadCloud = cloud.enabled
            if (hadCloud) {
                // The local device forgets the account; the encrypted copy
                // stays on the server so the user can still restore it.
                cloud.disable()
                cloudSyncedGeneration = cloudGeneration
                refreshCloudState()
            }
            _vault.value = null
            _stage.value = Stage.FIRST_RUN
            toast(if (hadCloud) "Vault erased — cloud backup turned off" else "Vault erased from this device")
        }
    }

    /* ── Decoy vault ──────────────────────────────────────────────────────── */

    /** Settings shown by the decoy vault — in-memory only, never persisted. */
    private val _decoySettings = MutableStateFlow(VaultSettings())
    val decoySettings: StateFlow<VaultSettings> = _decoySettings.asStateFlow()

    /** Last settings seen on the real vault, so the decoy opens looking identical. */
    private var lastKnownSettings: VaultSettings? = null

    private fun cacheSettings(data: VaultData?) {
        data?.settings?.let { lastKnownSettings = it }
    }

    fun updateDecoySettings(transform: (VaultSettings) -> VaultSettings) {
        _decoySettings.value = transform(_decoySettings.value)
    }

    /**
     * Opens the decoy vault after a failed unlock attempt. The decoy renders
     * demo content only (ui/screens/fake) and never reads or writes the
     * on-disk vault, which stays locked underneath — entering the correct
     * master password afterwards still opens the real vault.
     */
    private fun enterDecoy() {
        _decoySettings.value = lastKnownSettings ?: VaultSettings()
        lastInteraction = System.currentTimeMillis()
        backgroundedAt = null
        _stage.value = Stage.DECOY
    }

    /* ── Biometric ────────────────────────────────────────────────────────── */

    fun beginBiometricUnlock(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        val wrap = repo.readBiometricWrap()
        if (wrap == null) {
            onResult(false)
            return
        }
        val (nonce, ciphertext) = wrap
        val cipher = try {
            BiometricKeys.cipherForDecrypt(nonce)
        } catch (e: Exception) {
            // Keystore key invalidated (biometrics re-enrolled, lock screen changed…)
            BiometricKeys.delete()
            repo.deleteBiometricWrap()
            updateSettings { it.copy(biometricEnabled = false) }
            onResult(false)
            return
        }
        beginSystemPrompt()
        Biometric.authenticate(
            activity = activity,
            cipher = cipher,
            title = "Unlock Aegis",
            subtitle = "Confirm your identity to open your vault",
            onSuccess = { authed ->
                endSystemPrompt()
                val key = try {
                    authed.doFinal(ciphertext)
                } catch (e: Exception) {
                    null
                }
                if (key == null) {
                    onResult(false)
                    return@authenticate
                }
                viewModelScope.launch {
                    val data = repo.unlockWithKey(key)
                    if (data != null) {
                        _vault.value = data
                        cacheSettings(data)
                        refreshVaultBytes()
                        lastInteraction = System.currentTimeMillis()
                        _stage.value = Stage.UNLOCKED
                        onResult(true)
                    } else {
                        onResult(false)
                    }
                }
            },
            onFailure = {
                endSystemPrompt()
                onResult(false)
            },
        )
    }

    fun enableBiometric(activity: FragmentActivity, onDone: (Boolean) -> Unit) {
        if (!canUseBiometrics()) {
            toast("No biometrics enrolled on this device")
            onDone(false)
            return
        }
        val key = repo.sessionKey()
        if (key == null) {
            onDone(false)
            return
        }
        try {
            BiometricKeys.delete()
            BiometricKeys.createKey()
        } catch (e: Exception) {
            toast("Biometric unlock isn't available on this device")
            onDone(false)
            return
        }
        val cipher = try {
            BiometricKeys.cipherForEncrypt()
        } catch (e: Exception) {
            BiometricKeys.delete()
            onDone(false)
            return
        }
        beginSystemPrompt()
        Biometric.authenticate(
            activity = activity,
            cipher = cipher,
            title = "Enable biometric unlock",
            subtitle = "Verify your identity to link this device",
            negativeText = "Cancel",
            onSuccess = { authed ->
                endSystemPrompt()
                viewModelScope.launch {
                    val ok = try {
                        val ciphertext = authed.doFinal(key)
                        repo.storeBiometricWrap(authed.iv, ciphertext)
                        applySettings { it.copy(biometricEnabled = true) }
                        true
                    } catch (e: Exception) {
                        false
                    }
                    toast(if (ok) "Biometric unlock enabled" else "Couldn't enable biometric unlock")
                    onDone(ok)
                }
            },
            onFailure = {
                endSystemPrompt()
                onDone(false)
            },
        )
    }

    fun disableBiometric() {
        BiometricKeys.delete()
        repo.deleteBiometricWrap()
        updateSettings { it.copy(biometricEnabled = false) }
        toast("Biometric unlock disabled")
    }

    /* ── Items ────────────────────────────────────────────────────────────── */

    fun upsert(item: VaultItem) {
        val current = _vault.value ?: return
        val exists = current.items.any { it.id == item.id }
        val items = if (exists) {
            current.items.map { if (it.id == item.id) item else it }
        } else {
            current.items + item
        }
        val updated = current.copy(items = items)
        _vault.value = updated
        persist(updated)
    }

    fun markUsed(id: String) {
        val current = _vault.value ?: return
        val item = current.items.firstOrNull { it.id == id } ?: return
        val now = System.currentTimeMillis()
        val last = item.lastUsedAt
        if (last != null && now - last < 60_000L) return
        val touched = when (item) {
            is LoginItem -> item.copy(lastUsedAt = now)
            is CardItem -> item.copy(lastUsedAt = now)
            is NoteItem -> item.copy(lastUsedAt = now)
            is IdentityItem -> item.copy(lastUsedAt = now)
        }
        val updated = current.copy(items = current.items.map { if (it.id == id) touched else it })
        _vault.value = updated
        persist(updated)
    }

    fun moveToTrash(id: String) {
        val current = _vault.value ?: return
        val now = System.currentTimeMillis()
        val updated = current.copy(
            items = current.items.map { item ->
                if (item.id == id) {
                    when (item) {
                        is LoginItem -> item.copy(trashedAt = now)
                        is CardItem -> item.copy(trashedAt = now)
                        is NoteItem -> item.copy(trashedAt = now)
                        is IdentityItem -> item.copy(trashedAt = now)
                    }
                } else {
                    item
                }
            }
        )
        _vault.value = updated
        persist(updated)
        toast("Moved to trash")
    }

    fun restore(id: String) {
        val current = _vault.value ?: return
        val updated = current.copy(
            items = current.items.map { item ->
                if (item.id == id) {
                    when (item) {
                        is LoginItem -> item.copy(trashedAt = null)
                        is CardItem -> item.copy(trashedAt = null)
                        is NoteItem -> item.copy(trashedAt = null)
                        is IdentityItem -> item.copy(trashedAt = null)
                    }
                } else {
                    item
                }
            }
        )
        _vault.value = updated
        persist(updated)
        toast("Restored")
    }

    fun deleteForever(id: String) {
        val current = _vault.value ?: return
        val updated = current.copy(items = current.items.filterNot { it.id == id })
        _vault.value = updated
        persist(updated)
        toast("Deleted")
    }

    fun emptyTrash() {
        val current = _vault.value ?: return
        val updated = current.copy(items = current.items.filterNot { it.isTrashed })
        _vault.value = updated
        persist(updated)
        toast("Trash emptied")
    }

    /* ── Settings ─────────────────────────────────────────────────────────── */

    fun updateSettings(transform: (VaultSettings) -> VaultSettings) {
        viewModelScope.launch { applySettings(transform) }
    }

    private suspend fun applySettings(transform: (VaultSettings) -> VaultSettings) {
        val current = _vault.value ?: return
        val updated = current.copy(settings = transform(current.settings))
        _vault.value = updated
        lastKnownSettings = updated.settings
        repo.persist(updated)
        refreshVaultBytes()
        noteCloudChange()
    }

    private fun persist(data: VaultData) {
        viewModelScope.launch {
            repo.persist(data)
            refreshVaultBytes()
            noteCloudChange()
        }
    }

    /* ── Master password ──────────────────────────────────────────────────── */

    fun changeMasterPassword(current: String, next: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _busyText.value = "Updating master password…"
            _busy.value = true
            val result = repo.changeMasterPassword(current.toCharArray(), next.toCharArray())
            refreshVaultBytes()
            when (result) {
                VaultRepository.PasswordChangeResult.SUCCESS -> {
                    var message: String? = null
                    if (cloud.enabled && cloud.hasCredentials()) {
                        _busyText.value = "Re-linking cloud backup…"
                        message = performCloudRelink(next)
                    }
                    _busy.value = false
                    noteCloudChange()
                    toast(message ?: "Master password updated")
                    onResult(null)
                }
                VaultRepository.PasswordChangeResult.WRONG_PASSWORD -> {
                    _busy.value = false
                    onResult("Current password is incorrect")
                }
                VaultRepository.PasswordChangeResult.ERROR -> {
                    _busy.value = false
                    onResult("Couldn't update the master password")
                }
            }
        }
    }

    /* ── Backup ───────────────────────────────────────────────────────────── */

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val bytes = repo.exportBytes()
            if (bytes == null) {
                toast("Nothing to export")
                return@launch
            }
            val ok = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    true
                } catch (e: Exception) {
                    false
                }
            }
            if (ok) {
                applySettings { it.copy(lastBackupAt = System.currentTimeMillis()) }
                toast("Encrypted backup saved")
            } else {
                toast("Export failed")
            }
        }
    }

    fun importFrom(uri: Uri, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _busyText.value = "Restoring backup…"
            _busy.value = true
            val bytes = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } catch (e: Exception) {
                    null
                }
            }
            if (bytes == null) {
                _busy.value = false
                onResult("Couldn't read the file")
                return@launch
            }
            val data = repo.importFrom(bytes, password.toCharArray())
            _busy.value = false
            if (data == null) {
                onResult("Wrong password for this backup — or the file is damaged")
            } else {
                _vault.value = data
                cacheSettings(data)
                refreshVaultBytes()
                _stage.value = Stage.UNLOCKED
                toast("Backup restored")
                onResult(null)
            }
        }
    }

    /* ── Cloud backup ─────────────────────────────────────────────────────── */

    /** UI-facing cloud state for the Settings sheet. */
    data class CloudUiState(
        val enabled: Boolean = false,
        val busy: Boolean = false,
        val lastBackupAt: Long? = null,
        val serverUrl: String = CloudSyncStore.DEFAULT_SERVER,
        val error: String? = null,
    )

    private val cloud = CloudSyncStore(app)

    private val _cloudState = MutableStateFlow(
        CloudUiState(
            enabled = cloud.enabled,
            lastBackupAt = cloud.lastBackupAt.takeIf { it > 0 },
            serverUrl = cloud.serverUrl,
        )
    )
    val cloudState: StateFlow<CloudUiState> = _cloudState.asStateFlow()

    /** True while the enable flow waits for the "replace existing backup?" answer. */
    private val _cloudEnableConfirm = MutableStateFlow(false)
    val cloudEnableConfirm: StateFlow<Boolean> = _cloudEnableConfirm.asStateFlow()

    /** True while a manual backup waits for the "overwrite the newer copy?" answer. */
    private val _cloudReplaceConfirm = MutableStateFlow(false)
    val cloudReplaceConfirm: StateFlow<Boolean> = _cloudReplaceConfirm.asStateFlow()

    private var cloudEnableDeferred: CompletableDeferred<Boolean>? = null
    private var cloudReplaceDeferred: CompletableDeferred<Boolean>? = null

    /** Every local change bumps the generation; the last uploaded one is tracked. */
    private var cloudGeneration = 0L
    private var cloudSyncedGeneration = 0L
    private var cloudBackupInFlight = false
    private var cloudDebounceJob: Job? = null

    private var pendingShareCode: String? = null
    private var pendingSharePcPub: ByteArray? = null

    fun cloudServerDefault(): String = cloud.serverUrl

    fun cloudConfigured(): Boolean = cloud.enabled && cloud.hasCredentials()

    private fun refreshCloudState() {
        val current = _cloudState.value
        _cloudState.value = current.copy(
            enabled = cloud.enabled,
            lastBackupAt = cloud.lastBackupAt.takeIf { it > 0 },
            serverUrl = cloud.serverUrl,
        )
    }

    private sealed interface BackupOutcome {
        data object Success : BackupOutcome
        data object Conflict : BackupOutcome
        data class Failure(val message: String) : BackupOutcome
    }

    /**
     * Upload the current vault file. Without [force] a newer remote copy is
     * never overwritten (409 → Conflict). Forced writes happen only from the
     * manual flow after the user confirms; the server keeps the old revision
     * in its history.
     */
    private suspend fun performBackup(force: Boolean): BackupOutcome {
        val token = cloud.ensureToken()
            ?: return BackupOutcome.Failure("Cloud sign-in failed — turn cloud backup off and on again")
        val bytes = repo.exportBytes() ?: return BackupOutcome.Failure("Nothing to back up yet")
        val client = cloud.client()
        val blob = String(bytes, Charsets.UTF_8)

        val put = client.putVault(token, blob, cloud.revision)
            ?: return BackupOutcome.Failure("Couldn't reach the server")
        when (put.status) {
            200 -> {
                cloud.markBackedUp(put.revision ?: cloud.revision + 1)
                refreshCloudState()
                return BackupOutcome.Success
            }
            401 -> return BackupOutcome.Failure("Cloud sign-in expired — reconnect in Settings")
            409 -> {
                if (!force) return BackupOutcome.Conflict
                val serverRevision = put.revision ?: 0
                val forced = client.putVault(token, blob, serverRevision)
                    ?: return BackupOutcome.Failure("Couldn't reach the server")
                if (forced.status == 200) {
                    cloud.markBackedUp(forced.revision ?: serverRevision + 1)
                    refreshCloudState()
                    return BackupOutcome.Success
                }
                return BackupOutcome.Failure("Backup failed (${forced.status})")
            }
            else -> return BackupOutcome.Failure("Backup failed (${put.status})")
        }
    }

    /** Manual "Back up now" from the cloud sheet — with visible progress and dialogs. */
    fun cloudBackupNow() {
        if (!cloud.enabled) {
            toast("Cloud backup is off")
            return
        }
        viewModelScope.launch {
            _cloudState.value = _cloudState.value.copy(busy = true, error = null)
            var outcome: BackupOutcome? = performBackup(force = false)
            if (outcome is BackupOutcome.Conflict) {
                // Drop the busy overlay so the confirm dialog isn't covered.
                _cloudState.value = _cloudState.value.copy(busy = false)
                cloudReplaceDeferred = CompletableDeferred()
                _cloudReplaceConfirm.value = true
                val proceed = cloudReplaceDeferred?.await() ?: false
                _cloudReplaceConfirm.value = false
                cloudReplaceDeferred = null
                outcome = if (proceed) {
                    _cloudState.value = _cloudState.value.copy(busy = true)
                    performBackup(force = true)
                } else {
                    null
                }
            }
            when (outcome) {
                is BackupOutcome.Success -> {
                    _cloudState.value = _cloudState.value.copy(error = null)
                    toast("Vault backed up")
                }
                is BackupOutcome.Failure -> {
                    _cloudState.value = _cloudState.value.copy(error = outcome.message)
                    toast(outcome.message)
                }
                is BackupOutcome.Conflict -> Unit // handled above
                null -> Unit // user cancelled
            }
            _cloudState.value = _cloudState.value.copy(busy = false)
        }
    }

    fun answerCloudEnable(proceed: Boolean) {
        cloudEnableDeferred?.takeIf { !it.isCompleted }?.complete(proceed)
    }

    fun answerCloudReplace(proceed: Boolean) {
        cloudReplaceDeferred?.takeIf { !it.isCompleted }?.complete(proceed)
    }

    /**
     * Turns cloud backup on: derives the cloud credentials from the master
     * password, registers/logs in, and uploads the encrypted vault. When the
     * account already has a vault, asks before replacing it.
     */
    fun cloudEnable(password: String, server: String) {
        if (password.isBlank()) return
        viewModelScope.launch {
            _busyText.value = "Preparing encrypted backup…"
            _busy.value = true
            try {
                val normalizedServer = CloudSyncStore.normalizeServer(server)
                val creds = withContext(Dispatchers.Default) {
                    CloudKeys.derive(password.toCharArray())
                }
                val client = CloudClient(normalizedServer)

                _busyText.value = "Creating your cloud account…"
                val register = client.register(creds.accountId, creds.authKey)
                when {
                    register == null -> {
                        toast("Couldn't reach the server — check the URL and your connection")
                        return@launch
                    }
                    register.status == 200 || register.status == 409 -> Unit
                    register.status == 429 -> {
                        toast("Too many attempts — try again later")
                        return@launch
                    }
                    else -> {
                        toast("Setup failed (${register.status})")
                        return@launch
                    }
                }

                _busyText.value = "Signing in…"
                val login = client.login(creds.accountId, creds.authKey)
                if (login == null) {
                    toast("Couldn't reach the server")
                    return@launch
                }
                if (login.status != 200) {
                    toast("Setup failed (${login.status})")
                    return@launch
                }
                val token = login.token
                if (token == null) {
                    toast("Setup failed (unexpected response)")
                    return@launch
                }

                _busyText.value = "Checking the cloud copy…"
                val current = client.getVault(token)
                if (current == null) {
                    toast("Couldn't reach the server")
                    return@launch
                }
                val remoteRevision = if (current.status == 200) (current.revision ?: 0) else 0
                if (remoteRevision > 0) {
                    _busy.value = false
                    cloudEnableDeferred = CompletableDeferred()
                    _cloudEnableConfirm.value = true
                    val proceed = cloudEnableDeferred?.await() ?: false
                    _cloudEnableConfirm.value = false
                    cloudEnableDeferred = null
                    if (!proceed) return@launch
                    _busyText.value = "Uploading your vault…"
                    _busy.value = true
                }

                val bytes = repo.exportBytes()
                if (bytes == null) {
                    toast("Nothing to back up yet")
                    return@launch
                }
                val put = client.putVault(token, String(bytes, Charsets.UTF_8), remoteRevision)
                if (put == null) {
                    toast("Couldn't reach the server")
                    return@launch
                }
                when (put.status) {
                    200 -> Unit
                    409 -> {
                        toast("The cloud copy changed — try again")
                        return@launch
                    }
                    else -> {
                        toast("Upload failed (${put.status})")
                        return@launch
                    }
                }

                cloud.enabled = true
                cloud.serverUrl = normalizedServer
                cloud.accountId = creds.accountId
                cloud.authKey = creds.authKey
                cloud.saveSession(token, login.expiresAt ?: 0L)
                cloud.markBackedUp(put.revision ?: remoteRevision + 1)
                cloudSyncedGeneration = cloudGeneration
                refreshCloudState()
                _cloudState.value = _cloudState.value.copy(error = null)
                toast("Cloud backup is on — vault uploaded")
            } finally {
                _busy.value = false
            }
        }
    }

    /**
     * Restores the encrypted vault from the cloud (unlock screen). Runs two
     * key derivations on old phones — the cloud-seed PBKDF2 here, and the
     * envelope PBKDF2 inside [VaultRepository.importFrom].
     */
    fun restoreFromCloud(password: String, server: String, onResult: (String?) -> Unit) {
        if (password.isBlank()) {
            onResult("Enter your master password")
            return
        }
        viewModelScope.launch {
            _busyText.value = "Deriving keys — this can take a minute…"
            _busy.value = true
            try {
                val normalizedServer = CloudSyncStore.normalizeServer(server)
                val creds = withContext(Dispatchers.Default) {
                    CloudKeys.derive(password.toCharArray())
                }
                val client = CloudClient(normalizedServer)

                val login = client.login(creds.accountId, creds.authKey)
                when {
                    login == null -> {
                        onResult("Couldn't reach the server — check your connection")
                        return@launch
                    }
                    login.status == 401 -> {
                        onResult("No cloud backup found for this master password")
                        return@launch
                    }
                    login.status == 429 -> {
                        onResult("Too many attempts — wait a few minutes")
                        return@launch
                    }
                    login.status != 200 -> {
                        onResult("Restore failed (${login.status})")
                        return@launch
                    }
                }
                val token = login.token
                if (token == null) {
                    onResult("Restore failed (unexpected response)")
                    return@launch
                }

                _busyText.value = "Downloading the encrypted backup…"
                val remote = client.getVault(token)
                when {
                    remote == null -> {
                        onResult("Couldn't reach the server")
                        return@launch
                    }
                    remote.status == 404 -> {
                        onResult("No cloud backup found for this master password")
                        return@launch
                    }
                    remote.status != 200 -> {
                        onResult("Restore failed (${remote.status})")
                        return@launch
                    }
                }
                val blob = remote.blob
                if (blob.isNullOrBlank()) {
                    onResult("The cloud backup is empty")
                    return@launch
                }

                _busyText.value = "Decrypting your vault…"
                val data = repo.importFrom(blob.toByteArray(Charsets.UTF_8), password.toCharArray())
                if (data == null) {
                    onResult("The backup couldn't be decrypted with this password")
                    return@launch
                }

                cloud.enabled = true
                cloud.serverUrl = normalizedServer
                cloud.accountId = creds.accountId
                cloud.authKey = creds.authKey
                cloud.saveSession(token, login.expiresAt ?: 0L)
                cloud.revision = remote.revision ?: 0
                cloud.lastBackupAt = remote.updatedAt ?: System.currentTimeMillis()
                cloudSyncedGeneration = cloudGeneration
                refreshCloudState()
                _cloudState.value = _cloudState.value.copy(error = null)
                _vault.value = data
                cacheSettings(data)
                refreshVaultBytes()
                lastInteraction = System.currentTimeMillis()
                _stage.value = Stage.UNLOCKED
                toast("Vault restored from cloud")
                onResult(null)
            } finally {
                _busy.value = false
            }
        }
    }

    /**
     * Turns cloud backup off. With [deleteRemote] the encrypted copy is
     * deleted from the server first — and if that fails, nothing is changed
     * locally, so the user can retry instead of silently keeping an orphan.
     */
    fun cloudDisable(deleteRemote: Boolean) {
        viewModelScope.launch {
            if (!deleteRemote) {
                cloud.disable()
                cloudSyncedGeneration = cloudGeneration
                refreshCloudState()
                _cloudState.value = _cloudState.value.copy(error = null)
                toast("Cloud backup turned off")
                return@launch
            }
            _cloudState.value = _cloudState.value.copy(busy = true)
            try {
                val token = cloud.ensureToken()
                if (token == null) {
                    toast("Couldn't sign in — nothing was deleted")
                    return@launch
                }
                val response = cloud.client().deleteAccount(token)
                if (response == null) {
                    toast("Couldn't reach the server — nothing was deleted")
                    return@launch
                }
                if (response.status != 200 && response.status != 404) {
                    toast("Delete failed (${response.status}) — nothing was deleted")
                    return@launch
                }
                cloud.disable()
                cloudSyncedGeneration = cloudGeneration
                refreshCloudState()
                _cloudState.value = _cloudState.value.copy(error = null)
                toast("Cloud copy deleted — cloud backup is off")
            } finally {
                _cloudState.value = _cloudState.value.copy(busy = false)
            }
        }
    }

    /** Queue a debounced background backup after a vault change. */
    private fun noteCloudChange() {
        if (!cloud.enabled || !cloud.hasCredentials()) return
        cloudGeneration++
        cloudDebounceJob?.cancel()
        cloudDebounceJob = viewModelScope.launch {
            delay(8_000)
            cloudBackupQuiet()
        }
    }

    /** Debounced backup — silent: problems only land in [CloudUiState.error]. */
    private suspend fun cloudBackupQuiet() {
        if (!cloud.enabled || !cloud.hasCredentials()) return
        if (cloudBackupInFlight) {
            // A backup is already running; make sure the newest change still
            // gets a chance after it finishes.
            cloudDebounceJob?.cancel()
            cloudDebounceJob = viewModelScope.launch {
                delay(8_000)
                cloudBackupQuiet()
            }
            return
        }
        if (cloudGeneration == cloudSyncedGeneration) return
        val target = cloudGeneration
        cloudBackupInFlight = true
        try {
            when (val outcome = performBackup(force = false)) {
                is BackupOutcome.Success -> {
                    if (target == cloudGeneration) cloudSyncedGeneration = target
                    _cloudState.value = _cloudState.value.copy(error = null)
                    if (cloudGeneration != cloudSyncedGeneration) {
                        cloudDebounceJob?.cancel()
                        cloudDebounceJob = viewModelScope.launch {
                            delay(2_000)
                            cloudBackupQuiet()
                        }
                    }
                }
                is BackupOutcome.Conflict ->
                    _cloudState.value = _cloudState.value.copy(error = "A newer backup exists on another device")
                is BackupOutcome.Failure ->
                    _cloudState.value = _cloudState.value.copy(error = outcome.message)
            }
        } finally {
            cloudBackupInFlight = false
        }
    }

    /**
     * Moves the cloud account to credentials derived from the new master
     * password. Authenticates with the stored (old) credentials, so the old
     * password never needs re-deriving. On failure cloud backup is turned off
     * locally — the vault itself is untouched.
     */
    private suspend fun performCloudRelink(nextPassword: String): String? {
        val oldAccountId = cloud.accountId
        val oldAuthKey = cloud.authKey
        if (oldAccountId == null || oldAuthKey == null) return null

        val newCreds = withContext(Dispatchers.Default) {
            CloudKeys.derive(nextPassword.toCharArray())
        }
        val client = cloud.client()
        val relink = client.relink(oldAccountId, oldAuthKey, newCreds.accountId, newCreds.authKey)
        if (relink == null || relink.status != 200) {
            cloud.disable()
            cloudSyncedGeneration = cloudGeneration
            refreshCloudState()
            return "Password updated — cloud backup was turned off. Set it up again in Settings"
        }
        val login = client.login(newCreds.accountId, newCreds.authKey)
        val newToken = if (login != null && login.status == 200) login.token else null
        if (newToken == null) {
            cloud.disable()
            cloudSyncedGeneration = cloudGeneration
            refreshCloudState()
            return "Password updated — cloud backup was turned off. Set it up again in Settings"
        }
        cloud.accountId = newCreds.accountId
        cloud.authKey = newCreds.authKey
        cloud.saveSession(newToken, login?.expiresAt ?: 0L)
        refreshCloudState()
        return null
    }

    /* ── Send to PC (one-time share) ──────────────────────────────────────── */

    /**
     * Step 1: look the PC's one-time code up and return its fingerprint for
     * the visual comparison on both screens.
     */
    fun shareToPcLookup(codeInput: String, onResult: (fingerprint: String?, error: String?) -> Unit) {
        val code = ShareCrypto.normalizeCode(codeInput)
        if (code == null) {
            onResult(null, "Enter the 6-character code from your PC")
            return
        }
        viewModelScope.launch {
            val response = withContext(Dispatchers.IO) {
                CloudClient(cloud.serverUrl).relayLookup(code)
            }
            when {
                response == null -> onResult(null, "Couldn't reach the server — check your connection")
                response.status == 404 -> onResult(null, "Code not found or expired — check the PC screen")
                response.status == 429 -> onResult(null, "Too many attempts — wait a minute and try again")
                response.status != 200 -> onResult(null, "Lookup failed (${response.status})")
                else -> {
                    val pcPub = response.pcPub?.let { Crypto.unb64Url(it) }
                    if (pcPub == null || pcPub.size != 65) {
                        onResult(null, "The PC sent an unexpected key")
                        return@launch
                    }
                    pendingShareCode = code
                    pendingSharePcPub = pcPub
                    onResult(ShareCrypto.fingerprint(pcPub), null)
                }
            }
        }
    }

    /**
     * Step 2: encrypt just the selected item to the PC's key and upload it.
     * The relay stores it until the PC fetches it once (or it expires).
     */
    fun shareToPcSend(item: VaultItem, onResult: (String?) -> Unit) {
        val code = pendingShareCode
        val pcPub = pendingSharePcPub
        if (code == null || pcPub == null) {
            onResult("Start over — the code may have expired")
            return
        }
        viewModelScope.launch {
            val payload = SharePayload.forItem(item)
            val sealed = withContext(Dispatchers.Default) {
                ShareCrypto.sealToPc(pcPub, payload.toByteArray(Charsets.UTF_8))
            }
            if (sealed == null) {
                onResult("Couldn't encrypt the item")
                return@launch
            }
            val senderPub = Crypto.b64Url(sealed.senderPublicKey)
            val nonce = Crypto.b64Url(sealed.nonce)
            val ciphertext = Crypto.b64Url(sealed.ciphertext)
            if (ciphertext.length > 6000) {
                onResult("This item is too large to send")
                return@launch
            }
            val response = withContext(Dispatchers.IO) {
                CloudClient(cloud.serverUrl).relayUpload(
                    code = code,
                    senderPub = senderPub,
                    nonce = nonce,
                    ciphertext = ciphertext,
                )
            }
            when {
                response == null -> onResult("Couldn't reach the server — check your connection")
                response.status == 404 -> onResult("The code expired — start again")
                response.status == 409 -> onResult("That code was already used")
                response.status == 429 -> onResult("Too many attempts — wait a minute")
                response.status == 200 -> {
                    pendingShareCode = null
                    pendingSharePcPub = null
                    onResult(null)
                }
                else -> onResult("Sending failed (${response.status})")
            }
        }
    }

    fun shareCancel() {
        pendingShareCode = null
        pendingSharePcPub = null
    }

    /* ── Clipboard & toasts ───────────────────────────────────────────────── */

    fun copyToClipboard(text: String, sensitive: Boolean = false, label: String = "Aegis") {
        if (text.isBlank()) {
            toast("Nothing to copy")
            return
        }
        try {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        } catch (e: Exception) {
            toast("Couldn't copy")
            return
        }
        if (sensitive) {
            toast("Copied — clears in 60 seconds")
            viewModelScope.launch {
                delay(60_000)
                // Android 10+ denies clipboard reads to background apps, so a null
                // or failed read means either "nothing copied since" or "can't
                // check" — clear either way. clearPrimaryClip() only succeeds while
                // we still own the clip, so a newer user copy is never clobbered.
                val current = try {
                    clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                } catch (_: Exception) {
                    null
                }
                if (current == null || current == text) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            clipboard.clearPrimaryClip()
                        } else {
                            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        } else {
            toast("Copied")
        }
    }

    fun toast(msg: String) {
        _message.value = msg
    }

    fun clearMessage() {
        _message.value = null
    }

    /* ── Auto-lock ────────────────────────────────────────────────────────── */

    fun noteInteraction() {
        lastInteraction = System.currentTimeMillis()
    }

    /** Called around system biometric prompts so auto-lock doesn't fire mid-prompt. */
    fun beginSystemPrompt() {
        systemPromptActive = true
        noteInteraction()
    }

    fun endSystemPrompt() {
        systemPromptActive = false
        noteInteraction()
    }

    fun onBackground() {
        backgroundedAt = System.currentTimeMillis()
        lastInteraction = System.currentTimeMillis()
    }

    fun onForeground() {
        val backgrounded = backgroundedAt ?: return
        backgroundedAt = null
        val stage = _stage.value
        val minutes = if (stage == Stage.DECOY) {
            _decoySettings.value.autoLockMinutes
        } else {
            _vault.value?.settings?.autoLockMinutes ?: 1
        }
        val elapsed = System.currentTimeMillis() - backgrounded
        if ((stage == Stage.UNLOCKED || stage == Stage.DECOY) && minutes >= 0 && elapsed > minutes * 60_000L) {
            lockWithMessage()
        } else {
            lastInteraction = System.currentTimeMillis()
        }
    }

    fun tick() {
        val stage = _stage.value
        if (stage != Stage.UNLOCKED && stage != Stage.DECOY) return
        if (systemPromptActive) return
        val minutes = if (stage == Stage.DECOY) {
            _decoySettings.value.autoLockMinutes
        } else {
            _vault.value?.settings?.autoLockMinutes ?: return
        }
        if (minutes < 0) return
        if (System.currentTimeMillis() - lastInteraction > minutes * 60_000L) {
            lockWithMessage()
        }
    }
}
