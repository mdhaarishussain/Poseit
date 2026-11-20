package com.poseit.app.ui.settings

import com.poseit.app.pose.PoseSource
import com.poseit.app.pose.PoseTemplate

/** What an import will do: the poses to save (already renamed / cleaned) and how many were already present. */
data class ImportPlan(val toSave: List<PoseTemplate>, val alreadyPresent: Int)

/** Pure helpers for exporting / importing the user's poses as a JSON file. */
object PoseBackup {
    const val FILE_NAME = "poseit-poses.json"
    const val MIME_TYPE = "application/json"

    /** File types offered by the import picker (many file managers label .json files as text or binary). */
    val IMPORT_MIME_TYPES = arrayOf(MIME_TYPE, "text/json", "text/plain", "application/octet-stream")

    /** Refuse absurdly large files instead of running out of memory. */
    const val MAX_IMPORT_BYTES: Int = 16 * 1024 * 1024

    private val importedSuffix = Regex("-imported(-\\d+)?$")

    /**
     * Prepares [incoming] poses for saving next to [existingCustom]:
     *  - a pose identical to one already in the library is skipped (re-importing your own backup is harmless);
     *  - ids that clash with an existing custom pose or a built-in id ([reservedIds]) get a fresh "-imported" id;
     *  - reference / outline images that don't exist on this device ([fileExists] = false) are cleared;
     *  - poses claiming to be built-in become user poses, and a missing creation date becomes "now".
     */
    fun planImport(
        incoming: List<PoseTemplate>,
        existingCustom: List<PoseTemplate>,
        reservedIds: Set<String>,
        fileExists: (String) -> Boolean,
        now: Long,
    ): ImportPlan {
        val existingById = existingCustom.associateBy { it.id }
        val taken = HashSet<String>(reservedIds).apply { addAll(existingById.keys) }
        val toSave = ArrayList<PoseTemplate>(incoming.size)
        var alreadyPresent = 0
        incoming.forEachIndexed { index, raw ->
            val cleaned = raw.copy(
                id = raw.id.trim().ifEmpty { "imported-$now-$index" },
                referenceImage = raw.referenceImage?.takeIf { it.isNotBlank() && fileExists(it) },
                outlineImage = raw.outlineImage?.takeIf { it.isNotBlank() && fileExists(it) },
                source = if (raw.source == PoseSource.BUILT_IN) PoseSource.DRAWN else raw.source,
                // Keep the file's order when sorting newest-first.
                createdAt = if (raw.createdAt > 0L) raw.createdAt else now - index,
            )
            if (existingById[cleaned.id] == cleaned) {
                alreadyPresent++
                return@forEachIndexed
            }
            val id = if (cleaned.id in taken) uniqueId(cleaned.id, taken) else cleaned.id
            taken += id
            toSave += if (id == cleaned.id) cleaned else cleaned.copy(id = id)
        }
        return ImportPlan(toSave, alreadyPresent)
    }

    /** "pose" -> "pose-imported", then "pose-imported-2", "pose-imported-3", … (never stacks suffixes). */
    fun uniqueId(base: String, taken: Set<String>): String {
        val stem = base.replace(importedSuffix, "")
        var n = 1
        while (true) {
            val candidate = if (n == 1) "$stem-imported" else "$stem-imported-$n"
            if (candidate !in taken) return candidate
            n++
        }
    }

    fun importMessage(saved: Int, alreadyPresent: Int): String = when {
        saved == 0 && alreadyPresent > 0 ->
            if (alreadyPresent == 1) "That pose is already in your library" else "All $alreadyPresent poses are already in your library"
        saved == 0 -> "No poses found in that file"
        alreadyPresent > 0 -> "Imported ${poses(saved)} (${alreadyPresent} already in your library)"
        else -> "Imported ${poses(saved)}"
    }

    fun poses(count: Int): String = if (count == 1) "1 pose" else "$count poses"
}
