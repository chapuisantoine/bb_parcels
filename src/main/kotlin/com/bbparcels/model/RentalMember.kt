package com.bbparcels.model

import java.time.LocalDateTime

data class RentalMember(
    val id: Int = 0,
    val rentalId: Int,
    val memberUuid: String,
    val role: RentalMemberRole = RentalMemberRole.MEMBER,
    val addedAt: LocalDateTime = LocalDateTime.now()
)

enum class RentalMemberRole(val value: String) {
    OWNER("owner"),
    MEMBER("member")
}