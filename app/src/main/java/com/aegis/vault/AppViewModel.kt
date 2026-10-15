package com.aegis.vault

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.vault.crypto.BiometricKeys
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
import com.aegis.vault.ui.Biometric
import javax.crypto.Cipher
import kotlinx.coroutines.Dispatchers
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

    enum class Stage { FIRST_RUN, LOCKED, UNLOCKED }

    private val repo = VaultRepository(app)
    private val wordlist = Wordlist(app)
    private val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val _stage = MutableStateFlow(if (repo.vaultExists()) Stage.LOCKED else Stage.FIRST_RUN)
    val stage: StateFlow<Stage> = _stage.asStateFlow()

    private val _vault = MutableStateFlow<VaultData?>(null)
    val vault: StateFlow<VaultData?> = _vault.asStateFlow()

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
                lastInteraction = System.currentTimeMillis()
                _stage.value = Stage.UNLOCKED
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun lock() {
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
            BiometricKeys.delete()
            _vault.value = null
            _stage.value = Stage.FIRST_RUN
            toast("Vault erased from this device")
        }
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
        Biometric.authenticate(
            activity = activity,
            cipher = cipher,
            title = "Unlock Aegis",
            subtitle = "Confirm your identity to open your vault",
            onSuccess = { authed ->
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
                        lastInteraction = System.currentTimeMillis()
                        _stage.value = Stage.UNLOCKED
                        onResult(true)
                    } else {
                        onResult(false)
                    }
                }
            },
            onFailure = { onResult(false) },
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
        Biometric.authenticate(
            activity = activity,
            cipher = cipher,
            title = "Enable biometric unlock",
            subtitle = "Verify your identity to link this device",
            negativeText = "Cancel",
            onSuccess = { authed ->
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
            onFailure = { onDone(false) },
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
        if (item.lastUsedAt != null && now - item.lastUsedAt < 60_000L) return
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
        repo.persist(updated)
    }

    private fun persist(data: VaultData) {
        viewModelScope.launch { repo.persist(data) }
    }

    /* ── Master password ──────────────────────────────────────────────────── */

    fun changeMasterPassword(current: String, next: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _busyText.value = "Updating master password…"
            _busy.value = true
            val result = repo.changeMasterPassword(current.toCharArray(), next.toCharArray())
            _busy.value = false
            when (result) {
                VaultRepository.PasswordChangeResult.SUCCESS -> {
                    toast("Master password updated")
                    onResult(null)
                }
                VaultRepository.PasswordChangeResult.WRONG_PASSWORD ->
                    onResult("Current password is incorrect")
                VaultRepository.PasswordChangeResult.ERROR ->
                    onResult("Couldn't update the master password")
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
                _stage.value = Stage.UNLOCKED
                toast("Backup restored")
                onResult(null)
            }
        }
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
                try {
                    val current = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                    if (current == text) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                } catch (_: Exception) {
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

    fun onBackground() {
        backgroundedAt = System.currentTimeMillis()
        lastInteraction = System.currentTimeMillis()
    }

    fun onForeground() {
        val backgrounded = backgroundedAt ?: return
        backgroundedAt = null
        val minutes = _vault.value?.settings?.autoLockMinutes ?: 1
        val elapsed = System.currentTimeMillis() - backgrounded
        if (_stage.value == Stage.UNLOCKED && minutes >= 0 && elapsed > minutes * 60_000L) {
            lockWithMessage()
        } else {
            lastInteraction = System.currentTimeMillis()
        }
    }

    fun tick() {
        if (_stage.value != Stage.UNLOCKED) return
        val minutes = _vault.value?.settings?.autoLockMinutes ?: return
        if (minutes < 0) return
        if (System.currentTimeMillis() - lastInteraction > minutes * 60_000L) {
            lockWithMessage()
        }
    }
}
