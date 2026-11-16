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
    val memberProfileId: Int,
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
        val sql = "INSERT INTO bb_rental_members (rental_id, member_profile_id, role, added_at) VALUES (?, ?, ?, ?)"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                .use {ps ->
                    ps.setInt(1, rentalMember.rentalId)
                    ps.setInt(2, rentalMember.memberProfileId)
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
    
    fun getAllMembersParcels(serverId: String): List<Pair<String, ProfileInfo>> {
        val sql = """
            SELECT p.label, pr.id, pr.uuid AS account_uuid, pr.firstname, pr.lastname
            FROM bb_rental_members AS rm
            JOIN bb_rentals AS r ON rm.rental_id = r.id
            JOIN bb_parcels AS p ON r.parcel_id = p.id
            JOIN profiles pr ON pr.id = rm.member_profile_id
            WHERE p.server_id = ? AND r.status = 'active'
        """.trimIndent()
        try {
            db.getConnection().use {conn ->
                conn.prepareStatement(sql).use {ps ->
                    ps.setString(1, serverId)
                    ps.executeQuery().use {rs ->
                        val result = mutableListOf<Pair<String, ProfileInfo>>()
                        while (rs.next()) {
                            result += Pair(
                                rs.getString("label"),
                                ProfileInfo(
                                    id = rs.getInt("id"),
                                    accountUuid = rs.getString("account_uuid"),
                                    firstname = rs.getString("firstname"),
                                    lastname = rs.getString("lastname")
                                )
                            )
                        }
                        return result
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] getAllMembersParcels: ${e.message}")
            return emptyList()
        }
        
    }
    
    fun findMembers(parcelId: Int): List<ProfileInfo> {
        val sql = """
            SELECT pr.id AS tenant_id, pr.uuid AS tenant_uuid, pr.firstname, pr.lastname
            FROM bb_rental_members m
            INNER JOIN bb_rentals r ON r.id = m.rental_id
            INNER JOIN profiles pr  ON pr.id = m.member_profile_id
            WHERE r.parcel_id = ? AND r.status = 'active'
            """.trimIndent()
        try {
            db.getConnection().use {conn ->
                conn.prepareStatement(sql)
                .use {ps ->
                    ps.setInt(1, parcelId)
                    ps.executeQuery().use { rs ->
                        val members = mutableListOf<ProfileInfo>()
                        while (rs.next()) {
                            members += ProfileInfo(
                                id = rs.getInt("tenant_id"),
                                accountUuid = rs.getString("tenant_uuid"),
                                firstname = rs.getString("firstname"),
                                lastname = rs.getString("lastname")
                            )
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
    
    fun findMembersByRentalId(rentalId: Int): List<Int> {
        val sql = "SELECT member_profile_id FROM bb_rental_members WHERE rental_id = ?"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setInt(1, rentalId)
                    ps.executeQuery().use { rs ->
                        val members = mutableListOf<Int>()
                        while (rs.next()) members += rs.getInt("member_profile_id")
                        return members
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findMembersByRentalId: ${e.message}")
            return emptyList()
        }
    }
    
    fun deleteMember(label: String, memberProfileId: Int): Boolean {
        val sql = """
            DELETE FROM bb_rental_members
            WHERE member_profile_id = ?
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
                    ps.setInt(1, memberProfileId)
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