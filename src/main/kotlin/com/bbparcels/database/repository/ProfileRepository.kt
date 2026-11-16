package com.bbparcels.database.repository

import com.bbparcels.database.DatabaseManager
import org.bukkit.Bukkit
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

data class ProfileInfo(
    val id: Int,
    val accountUuid: String,
    val firstname: String,
    val lastname: String
) {
    
    val accountId: UUID get() = UUID.fromString(accountUuid)
    
    val displayName: String get() = "$firstname $lastname"
    
    val commandName: String get() = "${firstname}_$lastname"

    companion object {
        fun parseCommandName(arg: String): Pair<String, String>? {
            val parts = arg.split("_")
            if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) return null
            return parts[0] to parts[1]
        }
    }
}

internal fun ResultSet.toProfileInfo() = ProfileInfo(
    id          = getInt("id"),
    accountUuid = getString("uuid"),
    firstname   = getString("firstname"),
    lastname    = getString("lastname")
)

class ProfileRepository(private val db: DatabaseManager) {

    fun findActiveProfile(accountUuid: String): ProfileInfo? {
        val sql = """
            SELECT p.id, p.uuid, p.firstname, p.lastname
            FROM mc_accounts a
            INNER JOIN profiles p ON p.id = a.active_profile
            WHERE a.uuid = ? AND p.active = TRUE AND p.life_status = 'alive'
            """.trimIndent()
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, accountUuid)
                    ps.executeQuery().use { rs ->
                        return if (rs.next()) rs.toProfileInfo() else null
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findActiveProfile: ${e.message}")
            return null
        }
    }
    
    fun findActiveProfiles(accountUuids: List<String>): List<ProfileInfo> {
        if (accountUuids.isEmpty()) return emptyList()
        val placeholders = accountUuids.joinToString(", ") { "?" }
        val sql = """
            SELECT p.id, p.uuid, p.firstname, p.lastname
            FROM mc_accounts a
            INNER JOIN profiles p ON p.id = a.active_profile
            WHERE a.uuid IN ($placeholders) AND p.active = TRUE AND p.life_status = 'alive'
            ORDER BY p.lastname, p.firstname
            """.trimIndent()
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    accountUuids.forEachIndexed { i, uuid -> ps.setString(i + 1, uuid) }
                    ps.executeQuery().use { rs ->
                        return buildList { while (rs.next()) add(rs.toProfileInfo()) }
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findActiveProfiles: ${e.message}")
            return emptyList()
        }
    }

    fun findById(profileId: Int): ProfileInfo? {
        val sql = "SELECT id, uuid, firstname, lastname FROM profiles WHERE id = ?"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.setInt(1, profileId)
                    ps.executeQuery().use { rs ->
                        return if (rs.next()) rs.toProfileInfo() else null
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findById: ${e.message}")
            return null
        }
    }
    
    fun findActiveProfilesByName(firstname: String, lastname: String): List<ProfileInfo> {
        val sql = """
            SELECT p.id, p.uuid, p.firstname, p.lastname
            FROM mc_accounts a
            INNER JOIN profiles p ON p.id = a.active_profile
            WHERE p.firstname = ? AND p.lastname = ? AND p.active = TRUE AND p.life_status = 'alive'
            """.trimIndent()
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, firstname)
                    ps.setString(2, lastname)
                    ps.executeQuery().use { rs ->
                        return buildList { while (rs.next()) add(rs.toProfileInfo()) }
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findActiveProfilesByName: ${e.message}")
            return emptyList()
        }
    }
}