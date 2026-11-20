package com.poseit.app.ui.settings

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poseit.app.data.AppSettings
import com.poseit.app.data.PoseRepository
import com.poseit.app.data.SettingsRepository
import com.poseit.app.pose.io.PoseJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val poseRepository: PoseRepository,
    private val contentResolver: ContentResolver,
) : ViewModel() {

    /** null until DataStore has been read, so switches don't flicker from defaults to the real values. */
    val settings: StateFlow<AppSettings?> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val customPoseCount: StateFlow<Int> =
        poseRepository.customPoses.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _busy = MutableStateFlow(false)
    /** True while an import or export is running. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    /** One-off snackbar messages. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            try {
                settingsRepository.update(transform)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Could not save settings", e)
                _messages.send("Couldn't save that setting")
            }
        }
    }

    fun showMessage(text: String) {
        _messages.trySend(text)
    }

    /** Writes every custom pose to the document the user created. */
    fun exportTo(uri: Uri) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            val poses = poseRepository.customPoses.value
            val message = try {
                withContext(Dispatchers.IO) {
                    val bytes = PoseJson.encode(poses).toByteArray(Charsets.UTF_8)
                    openForWriting(uri).use { it.write(bytes) }
                }
                "Exported ${PoseBackup.poses(poses.size)}"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.w(TAG, "Export failed", e)
                "Couldn't save the file"
            } finally {
                _busy.value = false
            }
            _messages.send(message)
        }
    }

    /** Reads a Poseit JSON file and adds its poses to "My poses". */
    fun importFrom(uri: Uri) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            val message = try {
                val plan = withContext(Dispatchers.IO) {
                    val text = readText(uri)
                    val incoming = PoseJson.decode(text)
                    PoseBackup.planImport(
                        incoming = incoming,
                        existingCustom = poseRepository.customPoses.value,
                        reservedIds = poseRepository.builtIn.mapTo(HashSet()) { it.id },
                        fileExists = { path -> runCatching { File(path).isFile }.getOrDefault(false) },
                        now = System.currentTimeMillis(),
                    )
                }
                plan.toSave.forEach { poseRepository.saveCustom(it) }
                PoseBackup.importMessage(plan.toSave.size, plan.alreadyPresent)
            } catch (e: CancellationException) {
                throw e
            } catch (e: FileTooLargeException) {
                "That file is too big to be a Poseit backup"
            } catch (e: Throwable) {
                Log.w(TAG, "Import failed", e)
                "Couldn't read that file"
            } finally {
                _busy.value = false
            }
            _messages.send(message)
        }
    }

    private fun openForWriting(uri: Uri) =
        // "wt" truncates an existing file; some providers only support plain "w".
        (runCatching { contentResolver.openOutputStream(uri, "wt") }.getOrNull() ?: contentResolver.openOutputStream(uri, "w"))
            ?: throw FileNotFoundException("No output stream for $uri")

    private fun readText(uri: Uri): String {
        val input = contentResolver.openInputStream(uri) ?: throw FileNotFoundException("No input stream for $uri")
        return input.use { readBounded(it, PoseBackup.MAX_IMPORT_BYTES) }.toString(Charsets.UTF_8)
    }

    private class FileTooLargeException : IOException()

    private fun readBounded(input: InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > limit) throw FileTooLargeException()
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
