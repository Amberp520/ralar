package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.*
import java.math.BigInteger

class RalarConverters {
  @TypeConverter
  fun fromBigInteger(value: BigInteger?): String? {
    return value?.toString()
  }

  @TypeConverter
  fun toBigInteger(value: String?): BigInteger? {
    return value?.let { BigInteger(it) }
  }

  @TypeConverter
  fun fromRalarType(value: RalarType?): String? = value?.name

  @TypeConverter
  fun toRalarType(value: String?): RalarType? = value?.let { RalarType.valueOf(it) }

  @TypeConverter
  fun fromRalarStatus(value: RalarStatus?): String? = value?.name

  @TypeConverter
  fun toRalarStatus(value: String?): RalarStatus? = value?.let { RalarStatus.valueOf(it) }

  @TypeConverter
  fun fromParticipationStatus(value: ParticipationStatus?): String? = value?.name

  @TypeConverter
  fun toParticipationStatus(value: String?): ParticipationStatus? = value?.let { ParticipationStatus.valueOf(it) }

  @TypeConverter
  fun fromPaymentKind(value: PaymentKind?): String? = value?.name

  @TypeConverter
  fun toPaymentKind(value: String?): PaymentKind? = value?.let { PaymentKind.valueOf(it) }

  @TypeConverter
  fun fromPaymentStatus(value: PaymentStatus?): String? = value?.name

  @TypeConverter
  fun toPaymentStatus(value: String?): PaymentStatus? = value?.let { PaymentStatus.valueOf(it) }

  @TypeConverter
  fun fromPayoutStatus(value: PayoutStatus?): String? = value?.name

  @TypeConverter
  fun toPayoutStatus(value: String?): PayoutStatus? = value?.let { PayoutStatus.valueOf(it) }
}
