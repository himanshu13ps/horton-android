package com.himanshu13ps.horton.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val conversationId: Long = 0,
    val title: String,
    val startTimestamp: Long,
    val duration: Long,
    val processingStatus: String // "RECORDING", "PENDING_SYNTHESIS", "COMPLETED", "FAILED"
)

@Entity(tableName = "audio_segments")
data class AudioSegmentEntity(
    @PrimaryKey(autoGenerate = true) val segmentId: Long = 0,
    val conversationId: Long,
    val filePath: String,
    val sequenceIndex: Int
)

@Entity(tableName = "transcripts")
data class TranscriptEntity(
    @PrimaryKey(autoGenerate = true) val chunkId: Long = 0,
    val conversationId: Long,
    val segmentId: Long,
    val rawText: String,
    val timestampOffset: Long
)

@Entity(tableName = "extracted_notes")
data class ExtractedNoteEntity(
    @PrimaryKey(autoGenerate = true) val noteId: Long = 0,
    val conversationId: Long,
    val markdownContent: String,
    val generationTimestamp: Long
)

// Relations
data class ConversationWithDetails(
    @Embedded val conversation: ConversationEntity,
    
    @Relation(
        parentColumn = "conversationId",
        entityColumn = "conversationId"
    )
    val transcripts: List<TranscriptEntity>,

    @Relation(
        parentColumn = "conversationId",
        entityColumn = "conversationId"
    )
    val notes: ExtractedNoteEntity?
)

@Dao
interface NoteDao {
    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("UPDATE conversations SET duration = :duration, processingStatus = :status WHERE conversationId = :id")
    suspend fun updateConversationStatus(id: Long, duration: Long, status: String)

    @Insert
    suspend fun insertAudioSegment(segment: AudioSegmentEntity): Long

    @Insert
    suspend fun insertTranscript(transcript: TranscriptEntity): Long

    @Insert
    suspend fun insertExtractedNote(note: ExtractedNoteEntity): Long

    @Query("SELECT * FROM conversations ORDER BY startTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Transaction
    @Query("SELECT * FROM conversations WHERE conversationId = :conversationId")
    fun getConversationWithDetails(conversationId: Long): Flow<ConversationWithDetails>

    @Transaction
    @Query("SELECT * FROM conversations WHERE conversationId = :conversationId")
    suspend fun getConversationWithDetailsSync(conversationId: Long): ConversationWithDetails

    @Query("DELETE FROM audio_segments WHERE filePath = :filePath")
    suspend fun deleteAudioSegmentByPath(filePath: String)
}

@Database(
    entities = [
        ConversationEntity::class, 
        AudioSegmentEntity::class, 
        TranscriptEntity::class, 
        ExtractedNoteEntity::class
    ], 
    version = 1,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
