package com.example.library

import android.content.Context
import android.util.Log
import com.example.model.LibraryItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class LibraryStore private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val storageFile = File(context.filesDir, "cosmo_library_metadata.json")

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, LibraryItem::class.java)
    private val adapter = moshi.adapter<List<LibraryItem>>(listType)

    private val _items = MutableStateFlow<List<LibraryItem>>(emptyList())
    val items: StateFlow<List<LibraryItem>> = _items.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        scope.launch {
            mutex.withLock {
                try {
                    if (storageFile.exists()) {
                        val json = storageFile.readText()
                        val loaded = adapter.fromJson(json) ?: emptyList()
                        _items.value = loaded.sortedByDescending { it.downloadedAt }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load library metadata from disk", e)
                }
            }
        }
    }

    private suspend fun saveToDisk(list: List<LibraryItem>) = withContext(Dispatchers.IO) {
        try {
            val json = adapter.toJson(list)
            storageFile.writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save library metadata to disk", e)
        }
    }

    fun addItem(item: LibraryItem) {
        scope.launch {
            mutex.withLock {
                val current = _items.value.toMutableList()
                val existingIndex = current.indexOfFirst {
                    it.id == item.id || (it.localFilePath == item.localFilePath)
                }

                if (existingIndex >= 0) {
                    current[existingIndex] = item
                } else {
                    current.add(0, item)
                }

                _items.value = current
                saveToDisk(current)
            }
        }
    }

    fun removeItem(id: String) {
        scope.launch {
            mutex.withLock {
                val current = _items.value.filter { it.id != id }
                _items.value = current
                saveToDisk(current)
            }
        }
    }

    fun deleteItemAndFile(id: String) {
        scope.launch {
            mutex.withLock {
                val target = _items.value.find { it.id == id }
                if (target != null) {
                    try {
                        val file = File(target.localFilePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not delete physical file for $id", e)
                    }
                }

                val current = _items.value.filter { it.id != id }
                _items.value = current
                saveToDisk(current)
            }
        }
    }

    fun refreshState() {
        scope.launch {
            mutex.withLock {
                // Trigger recomposition by re-emitting items list
                _items.value = _items.value.map { it.copy() }
            }
        }
    }

    companion object {
        private const val TAG = "LibraryStore"

        @Volatile
        private var INSTANCE: LibraryStore? = null

        fun getInstance(context: Context): LibraryStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LibraryStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
