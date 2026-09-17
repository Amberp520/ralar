package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.*
import com.example.data.model.*

@Database(
  entities = [
    UserEntity::class,
    RalarEntity::class,
    ParticipationEntity::class,
    PaymentEntity::class,
    PayoutEntity::class,
    AuditLogEntity::class
  ],
  version = 1,
  exportSchema = false
)
@TypeConverters(RalarConverters::class)
abstract class RalarDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun ralarDao(): RalarDao
  abstract fun participationDao(): ParticipationDao
  abstract fun paymentDao(): PaymentDao
  abstract fun payoutDao(): PayoutDao
  abstract fun auditLogDao(): AuditLogDao

  companion object {
    @Volatile
    private var INSTANCE: RalarDatabase? = null

    fun getDatabase(context: Context): RalarDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          RalarDatabase::class.java,
          "ralar_ledger.db"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
