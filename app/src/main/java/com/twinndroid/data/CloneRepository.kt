package com.twinndroid.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Clone definitions and the folders that group them, persisted as small JSON files
 * (clones.json, folders.json) in app-private storage.
 */
class CloneRepository private constructor(context: Context) {

    private val file = File(context.filesDir, "clones.json")
    private val foldersFile = File(context.filesDir, "folders.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val _clones = MutableStateFlow(load())
    val clones: StateFlow<List<CloneConfig>> = _clones.asStateFlow()
    private val _folders = MutableStateFlow(loadFolders())
    val folders: StateFlow<List<CloneFolder>> = _folders.asStateFlow()

    private fun load(): List<CloneConfig> = runCatching {
        if (file.exists()) json.decodeFromString<List<CloneConfig>>(file.readText()) else emptyList()
    }.getOrDefault(emptyList())

    private fun loadFolders(): List<CloneFolder> = runCatching {
        if (foldersFile.exists()) json.decodeFromString<List<CloneFolder>>(foldersFile.readText()) else emptyList()
    }.getOrDefault(emptyList())

    /** Re-reads clones.json and folders.json from disk (after a backup restore). */
    @Synchronized
    fun reload() {
        _folders.value = loadFolders()
        _clones.value = load()
    }

    @Synchronized
    fun upsert(config: CloneConfig) {
        val list = _clones.value.toMutableList()
        val i = list.indexOfFirst { it.id == config.id }
        if (i >= 0) list[i] = config else list.add(config)
        persist(list)
    }

    @Synchronized
    fun remove(id: String) = persist(_clones.value.filterNot { it.id == id })

    fun get(id: String): CloneConfig? = _clones.value.firstOrNull { it.id == id }

    // ---- folders -------------------------------------------------------------------------------

    @Synchronized
    fun upsertFolder(folder: CloneFolder) {
        val list = _folders.value.toMutableList()
        val i = list.indexOfFirst { it.id == folder.id }
        if (i >= 0) list[i] = folder else list.add(folder)
        persistFolders(list)
    }

    /** Deletes a folder only; the clones inside move back to the top level. */
    @Synchronized
    fun removeFolder(id: String) {
        if (_clones.value.any { it.folderId == id }) {
            persist(_clones.value.map { if (it.folderId == id) it.copy(folderId = null) else it })
        }
        persistFolders(_folders.value.filterNot { it.id == id })
    }

    /** Files a clone under [folderId], or back at the top level when it is null. */
    @Synchronized
    fun setFolder(cloneId: String, folderId: String?) {
        val current = _clones.value.firstOrNull { it.id == cloneId } ?: return
        if (current.folderId == folderId) return
        upsert(current.copy(folderId = folderId))
    }

    // ---- persistence ---------------------------------------------------------------------------

    private fun persist(list: List<CloneConfig>) {
        writeAtomically(file, json.encodeToString(list))
        _clones.value = list
    }

    private fun persistFolders(list: List<CloneFolder>) {
        writeAtomically(foldersFile, json.encodeToString(list))
        _folders.value = list
    }

    private fun writeAtomically(target: File, text: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(text)
        tmp.renameTo(target)
    }

    companion object {
        @Volatile private var instance: CloneRepository? = null
        fun get(context: Context): CloneRepository =
            instance ?: synchronized(this) {
                instance ?: CloneRepository(context.applicationContext).also { instance = it }
            }
    }
}
