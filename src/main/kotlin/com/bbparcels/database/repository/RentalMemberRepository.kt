package com.bbparcels.database.repository

import com.bbparcels.BbParcels
import com.bbparcels.database.DatabaseManager
import org.bukkit.Bukkit
import java.sql.SQLException
import java.sql.Statement
import java.sql.Timestamp
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

class RentalMemberRepository(
    private val db: DatabaseManager,
    private val serverId: String = BbParcels.instance.pluginConfig.serverId
    ) {
    
    fun addMember(rentalMember: RentalMember): Boolean {
        val sql = "INSERT INTO bb_rental_members (rental_id, member_uuid, role, added_at) VALUES (?, ?, ?, ?)"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                .use {ps ->
                    ps.setInt(1, rentalMember.rentalId)
                    ps.setString(2, rentalMember.memberUuid)
                    ps.setString(3, rentalMember.role.value)
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()))
                    ps.executeUpdate()
                    ps.generatedKeys.use {rs ->
                        return rs.next()
                    }            
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] addMember: ${e.message}")
            return false
        }
    }
    
    fun getAllMembersParcels(serverId: String): List<Pair<String, String>> {
        val sql = """
            SELECT p.label, rm.member_uuid
            FROM bb_rental_members AS rm
            JOIN bb_rentals AS r ON rm.rental_id = r.id
            JOIN bb_parcels AS p ON r.parcel_id = p.id
            WHERE p.server_id = ? AND r.status = 'active'
        """.trimIndent()
        try {
            db.getConnection().use {conn ->
                conn.prepareStatement(sql).use {ps ->
                    ps.setString(1, serverId)
                    ps.executeQuery().use {rs ->
                        val result = mutableListOf<Pair<String, String>>()
                        while (rs.next()) {
                            result += Pair(
                                rs.getString("label"),
                                rs.getString("member_uuid")
                            )
                        }
                        return result
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbEnterprise] getAllMembersParcels: ${e.message}")
            return emptyList()
        }
        
    }
    
    fun findMembers(parcelId: Int): List<String> {
        val sql = """
            SELECT m.member_uuid
            FROM bb_rental_members m
            INNER JOIN bb_rentals r ON r.id = m.rental_id
            WHERE r.parcel_id = ? AND r.status = 'active'
            """.trimIndent()
        try {
            db.getConnection().use {conn ->
                conn.prepareStatement(sql)
                .use {ps ->
                    ps.setInt(1, parcelId)
                    ps.executeQuery().use { rs ->
                        val members = mutableListOf<String>()
                        while (rs.next()) members += rs.getString("member_uuid")
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
        val sql = "SELECT member_uuid FROM bb_rental_members WHERE rental_id = ?"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
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
    
    fun deleteMember(label: String, memberUuid: String): Boolean {
        val sql = """
            DELETE FROM bb_rental_members
            WHERE member_uuid = ?
            AND rental_id IN (
                SELECT bb_rentals.id FROM bb_rentals
                INNER JOIN bb_parcels ON bb_rentals.parcel_id = bb_parcels.id
                WHERE bb_parcels.label = ? AND bb_rentals.status = 'active'  AND bb_parcels.server_id = ?
            )
            """.trimIndent()
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, memberUuid)
                    ps.setString(2, label)
                    ps.setString(3, serverId)
                    return ps.executeUpdate() > 0
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] deleteMember: ${e.message}")
            return false
        }
    }
}