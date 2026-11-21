package com.poseit.app.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.poseit.app.pose.PoseTemplate
import com.poseit.app.pose.io.PoseJson
import com.poseit.app.pose.library.BuiltInPoses
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private val Context.libraryStore by preferencesDataStore(name = "library")

/**
 * Single source of truth for poses: the built-in library plus the user's own poses
 * (stored as JSON in app-private storage), favourites and recently used poses.
 */
class PoseRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file = File(context.filesDir, "custom_poses.json")
    private val fileMutex = Mutex()

    private object Keys {
        val favorites = stringSetPreferencesKey("favorites")
        /** Comma-separated, most recent first. */
        val recent = stringPreferencesKey("recent")
    }

    val builtIn: List<PoseTemplate> get() = BuiltInPoses.all

    private val _custom = MutableStateFlow<List<PoseTemplate>>(emptyList())
    val customPoses: StateFlow<List<PoseTemplate>> = _custom.asStateFlow()

    /** Custom poses first (newest first), then the built-in library. */
    val allPoses: Flow<List<PoseTemplate>> = _custom.map { custom -> custom.sortedByDescending { it.createdAt } + builtIn }

    val favorites: Flow<Set<String>> = context.libraryStore.data.map { it[Keys.favorites] ?: emptySet() }

    val recent: Flow<List<String>> = context.libraryStore.data.map { prefs ->
        prefs[Keys.recent]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
    }

    val favoritePoses: Flow<List<PoseTemplate>> = combine(allPoses, favorites) { all, fav -> all.filter { it.id in fav } }

    init {
        scope.launch { _custom.value = loadCustom() }
    }

    /** Synchronous lookup among built-ins and already-loaded custom poses. */
    fun pose(id: String): PoseTemplate? = _custom.value.firstOrNull { it.id == id } ?: BuiltInPoses.byId(id)

    fun poseFlow(id: String): Flow<PoseTemplate?> = _custom.map { custom -> custom.firstOrNull { it.id == id } ?: BuiltInPoses.byId(id) }

    suspend fun toggleFavorite(id: String) {
        context.libraryStore.edit { prefs ->
            val cur = prefs[Keys.favorites] ?: emptySet()
            prefs[Keys.favorites] = if (id in cur) cur - id else cur + id
        }
    }

    suspend fun markUsed(id: String) {
        context.libraryStore.edit { prefs ->
            val cur = prefs[Keys.recent]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
            prefs[Keys.recent] = (listOf(id) + cur.filter { it != id }).take(20).joinToString(",")
        }
    }

    suspend fun saveCustom(template: PoseTemplate) = fileMutex.withLock {
        val updated = _custom.value.filter { it.id != template.id } + template
        writeCustom(updated)
        _custom.value = updated
    }

    suspend fun deleteCustom(id: String) = fileMutex.withLock {
        val victim = _custom.value.firstOrNull { it.id == id } ?: return@withLock
        val updated = _custom.value - victim
        writeCustom(updated)
        _custom.value = updated
        // Remove the reference photo and outline we copied into app storage (files/refs), unless another pose still
        // uses them (an imported backup copy of an edited pose keeps the same image paths).
        val stillUsed = updated.flatMap { listOfNotNull(it.referenceImage, it.outlineImage) }.toSet()
        withContext(Dispatchers.IO) {
            val refsDir = runCatching { File(context.filesDir, REFS_DIR).canonicalPath + File.separator }.getOrNull()
            if (refsDir != null) {
                listOfNotNull(victim.referenceImage, victim.outlineImage)
                    .distinct()
                    .filter { it !in stillUsed && runCatching { File(it).canonicalPath.startsWith(refsDir) }.getOrDefault(false) }
                    .forEach { runCatching { File(it).delete() } }
            }
        }
        context.libraryStore.edit { prefs ->
            prefs[Keys.favorites]?.let { prefs[Keys.favorites] = it - id }
            prefs[Keys.recent]?.let { recent ->
                prefs[Keys.recent] = recent.split(',').filter { it.isNotBlank() && it != id }.joinToString(",")
            }
        }
    }

    private suspend fun loadCustom(): List<PoseTemplate> = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            if (!file.exists()) return@withLock emptyList()
            runCatching { PoseJson.decode(file.readText()) }
                .onFailure { Log.e(TAG, "Could not read custom poses", it) }
                .getOrDefault(emptyList())
        }
    }

    private suspend fun writeCustom(list: List<PoseTemplate>) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(PoseJson.encode(list))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private companion object {
        const val TAG = "PoseRepository"

        /** Folder under filesDir holding the reference photos and outlines of photo-based poses. */
        const val REFS_DIR = "refs"
    }
}
