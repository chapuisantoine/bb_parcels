package com.bbparcels.repository

import com.bbparcels.database.DatabaseManager
import com.bbparcels.model.RentalMember
import org.bukkit.Bukkit
import java.sql.SQLException
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime

class RentalMemberRepository(private val db: DatabaseManager) {
    
    fun addMember(rentalMember: RentalMember): Int {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    """INSERT INTO bb_rental_members (rental_id, member_uuid, role, added_at) 
                            VALUES (?, ?, ?, ?)""",
                    Statement.RETURN_GENERATED_KEYS
                ).use {ps ->
                    ps.setInt(1, rentalMember.rentalId)
                    ps.setString(2, rentalMember.memberUuid)
                    ps.setString(3, rentalMember.role.value)
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()))
                    ps.executeUpdate()
                    ps.generatedKeys.use {rs ->
                        return if (rs.next()) rs.getInt(1) else -1
                    }            
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] addMember: ${e.message}")
            return -1
        }
    }
    
    fun findMembers(parcelLabel: String): List<String> {
        try {
            db.getConnection().use {conn ->
                conn.prepareStatement(
                    """
                        SELECT member_uuid FROM bb_rental_members 
                        LEFT JOIN bb_rentals 
                            ON bb_rental_members.rental_id = bb_rentals.id 
                            AND bb_rentals.status = 'active'
                        LEFT JOIN bb_parcels 
                            ON bb_rentals.parcel_id = bb_parcels.id 
                        WHERE bb_parcels.label = ?
                    """.trimIndent()
                ).use {ps ->
                    ps.setString(1, parcelLabel)
                    ps.executeQuery().use { rs ->
                        val members = mutableListOf<String>()
                        while (rs.next()) {
                            members += rs.getString("member_uuid")
                        }
                        return members
                    }
                }
            }  
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findMembers: ${e.message}")
            return emptyList()
        }
    }
    
    fun findMembersByRentalId(rentalId: Int): List<String> {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT member_uuid FROM bb_rental_members WHERE rental_id = ?"
                ).use { ps ->
                    ps.setInt(1, rentalId)
                    ps.executeQuery().use { rs ->
                        val members = mutableListOf<String>()
                        while (rs.next()) members += rs.getString("member_uuid")
                        return members
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findMembersByRentalId: ${e.message}")
            return emptyList()
        }
    }
    
    fun deleteMember(parcelLabel: String, memberUuid: String): Boolean {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement("""
                    DELETE FROM bb_rental_members
                    WHERE member_uuid = ?
                    AND rental_id IN (
                        SELECT bb_rentals.id FROM bb_rentals
                        INNER JOIN bb_parcels ON bb_rentals.parcel_id = bb_parcels.id
                        WHERE bb_parcels.label = ? AND bb_rentals.status = 'active'
                    )
                """.trimIndent()).use { ps ->
                    ps.setString(1, memberUuid)
                    ps.setString(2, parcelLabel)
                    return ps.executeUpdate() > 0
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] deleteMember: ${e.message}")
            return false
        }
    }
    
    
    fun deleteAllByRentalId(rentalId: Int): Boolean {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    "DELETE FROM bb_rental_members WHERE rental_id = ?"
                ).use { ps ->
                    ps.setInt(1, rentalId)
                    return ps.executeUpdate() > 0
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] deleteAllByRentalId: ${e.message}")
            return false
        }
    }
}