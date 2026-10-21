package com.example.laststick.data

import com.example.laststick.ai.LearningSnapshot
import java.io.File
import java.io.IOException

/** Persistence for the learning agent. Implementations must never throw on bad data. */
interface LearningStore {
    /** The saved snapshot, or `null` if there is none or it cannot be read/validated. */
    fun load(): LearningSnapshot?

    /** Returns true on success. */
    fun save(snapshot: LearningSnapshot): Boolean

    fun clear()
}

/** Stores the snapshot in a plain text file, written atomically (temp file + rename). */
class FileLearningStore(private val file: File) : LearningStore {

    override fun load(): LearningSnapshot? = try {
        if (file.isFile) LearningSnapshot.parse(file.readText()) else null
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    }

    override fun save(snapshot: LearningSnapshot): Boolean = try {
        file.absoluteFile.parentFile?.mkdirs()
        val tmp = File(file.absolutePath + ".tmp")
        tmp.writeText(snapshot.serialize())
        if (tmp.renameTo(file)) {
            true
        } else {
            file.delete()
            tmp.renameTo(file)
        }
    } catch (e: IOException) {
        false
    } catch (e: SecurityException) {
        false
    }

    override fun clear() {
        try {
            file.delete()
        } catch (e: SecurityException) {
            // Nothing sensible to do.
        }
    }
}
