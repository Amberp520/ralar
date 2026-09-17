package com.example.data.model

enum class RalarType {
  EVENT,
  HACKATHON,
  CAUSE
}

enum class RalarStatus {
  DRAFT,
  PUBLISHED,
  CLOSED
}

enum class ParticipationStatus {
  REGISTERED,
  CONFIRMED,
  CANCELLED
}

enum class PaymentKind {
  TICKET,
  REGISTRATION_FEE,
  DONATION
}

enum class PaymentStatus {
  PENDING,
  PROCESSING,
  PAID,
  FAILED
}

enum class PayoutStatus {
  AUTHORIZED,
  PROCESSING,
  PAID,
  FAILED
}
