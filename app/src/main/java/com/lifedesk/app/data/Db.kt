package com.lifedesk.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromDate(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun toDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
}

@Dao
interface LifeDao {
    @Query("SELECT * FROM items WHERE archived = 0 ORDER BY dueDate IS NULL, dueDate, title")
    fun observeActive(): Flow<List<LifeItem>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeItem(id: Long): Flow<LifeItem?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun item(id: Long): LifeItem?

    @Query("SELECT * FROM items WHERE archived = 0")
    suspend fun activeItems(): List<LifeItem>

    @Query("SELECT * FROM items")
    suspend fun allItems(): List<LifeItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(item: LifeItem): Long

    @Delete
    suspend fun delete(item: LifeItem)

    @Query("DELETE FROM items")
    suspend fun deleteAllItems()

    @Insert
    suspend fun insertPrice(record: PriceRecord): Long

    @Query("SELECT * FROM prices WHERE `key` = :key ORDER BY date DESC, id DESC")
    suspend fun pricesForKey(key: String): List<PriceRecord>

    @Query("SELECT * FROM prices WHERE itemId = :itemId ORDER BY date DESC, id DESC")
    suspend fun pricesForItem(itemId: Long): List<PriceRecord>

    @Query("SELECT * FROM prices ORDER BY date DESC, id DESC")
    fun observePrices(): Flow<List<PriceRecord>>

    @Query("SELECT * FROM prices")
    suspend fun allPrices(): List<PriceRecord>

    @Query("DELETE FROM prices WHERE itemId = :itemId")
    suspend fun deletePricesFor(itemId: Long)

    @Query("DELETE FROM prices")
    suspend fun deleteAllPrices()
}

@Database(entities = [LifeItem::class, PriceRecord::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class LifeDatabase : RoomDatabase() {
    abstract fun dao(): LifeDao

    companion object {
        fun create(context: Context): LifeDatabase =
            Room.databaseBuilder(context, LifeDatabase::class.java, "lifedesk.db").build()
    }
}
