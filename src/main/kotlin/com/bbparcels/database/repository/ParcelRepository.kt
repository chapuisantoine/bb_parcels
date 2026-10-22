package com.bbparcels.database.repository

import com.bbparcels.BbParcels
import com.bbparcels.database.DatabaseManager
import org.bukkit.Bukkit
import java.sql.SQLException
import java.sql.Statement

data class Parcel(
    val id: Int = 0,
    val regionId: String,
    val world: String,
    val label: String,
    val description: String? = null,
    val status: ParcelStatus = ParcelStatus.AVAILABLE
)

data class ParcelWithOwner(
    val id: Int,
    val regionId: String,
    val world: String,
    val label: String,
    val status: ParcelStatus = ParcelStatus.AVAILABLE,
    val ownerUuid: String?
)

enum class ParcelStatus(val value: String) {
    AVAILABLE("available"),
    RENTED("rented"),
    DISABLED("disabled");
    
    companion object {
        fun from(value: String): ParcelStatus = entries.first { it.value == value }
    }
}

class ParcelRepository(
    private val db: DatabaseManager,
    private val serverId: String = BbParcels.instance.pluginConfig.serverId,
    ) {

    fun existsByRegion(regionId: String, world: String): Boolean {
        val sql = "SELECT COUNT(*) FROM bb_parcels WHERE region_id = ? AND world = ? AND server_id = ?"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, regionId)
                    ps.setString(2, world)
                    ps.setString(3, serverId)
                    ps.executeQuery().use { rs ->
                        return rs.next() && rs.getInt(1) > 0
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] existsByRegion: ${e.message}")
            return false
        }
    }
    
    fun existsByLabel(label: String): Boolean {
        val sql = "SELECT COUNT(*) FROM bb_parcels WHERE label = ? AND server_id = ?"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, label)
                    ps.setString(2, serverId)
                    ps.executeQuery().use { rs ->
                        return rs.next() && rs.getInt(1) > 0
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] existsByLabel: ${e.message}")
            return false
        }
    }

    fun addParcel(parcel: Parcel): Int {
        val sql = "INSERT INTO bb_parcels (server_id, region_id, world, label, description, status) VALUES (?, ?, ?, ?, ?, ?)"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                .use { ps ->
                    ps.setString(1, serverId)
                    ps.setString(2, parcel.regionId)
                    ps.setString(3, parcel.world)
                    ps.setString(4, parcel.label)
                    ps.setString(5, parcel.description)
                    ps.setString(6, parcel.status.value)
                    ps.executeUpdate()
                    ps.generatedKeys.use { rs ->
                        return if (rs.next()) rs.getInt(1) else -1
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] addParcel: ${e.message}")
            return -1
        }
    }
    
    fun findByLabel(label: String): Parcel? {
        val sql = "SELECT id, region_id, world, label, description, status FROM bb_parcels WHERE label = ? AND server_id = ?"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, label)
                    ps.setString(2, serverId)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) return null
                        return Parcel(
                            id          = rs.getInt("id"),
                            regionId    = rs.getString("region_id"),
                            world       = rs.getString("world"),
                            label       = rs.getString("label"),
                            description = rs.getString("description"),
                            status      = ParcelStatus.entries.first { it.value == rs.getString("status") },
                        )
                    }
                }
            }   
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findByLabel: ${e.message}")
            return null
        }
    }
    
    fun findByLabelWithOwner(label: String): ParcelWithOwner? {
        val sql = """
            SELECT bb_parcels.id, bb_parcels.label, bb_parcels.region_id, bb_parcels.world,
            bb_parcels.status, bb_rentals.tenant_uuid AS owner
            FROM bb_parcels
            LEFT JOIN bb_rentals
                ON bb_rentals.parcel_id = bb_parcels.id
                AND bb_rentals.status = 'active'
            WHERE bb_parcels.server_id = ? AND bb_parcels.label = ?
            """.trimIndent()
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, serverId)
                    ps.setString(2, label)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) return null
                        return ParcelWithOwner(
                            id          = rs.getInt("id"),
                            label       = rs.getString("label"),
                            regionId    = rs.getString("region_id"),
                            world       = rs.getString("world"),
                            status      = ParcelStatus.entries.first { it.value == rs.getString("status") },
                            ownerUuid   = rs.getString("owner")   // null si pas de location active
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findByLabelWithOwner: ${e.message}")
            return null
        }
    }

    fun findAll(): List<Parcel> {
        val sql = "SELECT id, region_id, world, label, description, status FROM bb_parcels WHERE server_id = ?"
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, serverId)
                    ps.executeQuery().use { rs ->
                        val parcels = mutableListOf<Parcel>()
                        while (rs.next()) {
                            parcels += Parcel(
                                id          = rs.getInt("id"),
                                regionId    = rs.getString("region_id"),
                                world       = rs.getString("world"),
                                label       = rs.getString("label"),
                                description = rs.getString("description"),
                                status      = ParcelStatus.entries.first { it.value == rs.getString("status") },
                            )
                        }
                        return parcels
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findAll: ${e.message}")
            return emptyList()
        }
    }
    
    fun findAllWithOwners(): List<ParcelWithOwner> {
        val sql = """
            SELECT bb_parcels.id, bb_parcels.label, bb_parcels.region_id, bb_parcels.world,
            bb_parcels.status, bb_rentals.tenant_uuid AS owner
            FROM bb_parcels

            LEFT JOIN bb_rentals
                ON bb_rentals.parcel_id = bb_parcels.id
                AND bb_rentals.status = 'active'
            WHERE bb_parcels.server_id = ?
            """.trimIndent()
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, serverId)
                    ps.executeQuery().use { rs ->
                        val result = mutableListOf<ParcelWithOwner>()
                        while (rs.next()) {
                            result += ParcelWithOwner(
                                id          = rs.getInt("id"),
                                label       = rs.getString("label"),
                                regionId    = rs.getString("region_id"),
                                world       = rs.getString("world"),
                                status      = ParcelStatus.entries.first { it.value == rs.getString("status") },
                                ownerUuid = rs.getString("owner")   // null si pas de location active
                            )
                        }
                        return result
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findAllWithOwners: ${e.message}")
            return emptyList()
        }
    }
    
    fun findAllOwnerParcels(playerUuid: String): List<Parcel> {
        val sql = """SELECT p.id, p.label, p.region_id, p.world, p.status
                    FROM bb_parcels p
                    INNER JOIN bb_rentals r ON r.parcel_id = p.id AND r.status = 'active'
                    WHERE r.tenant_uuid = ? AND p.server_id = ?
                    
                """.trimIndent() 
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, playerUuid)
                    ps.setString(2, serverId)
                    ps.executeQuery().use { rs ->
                        val result = mutableListOf<Parcel>()
                        while (rs.next()) {
                            result += Parcel(
                                id = rs.getInt("id"),
                                label = rs.getString("label"),
                                regionId = rs.getString("region_id"),
                                world = rs.getString("world"),
                                status = ParcelStatus.entries.first { it.value == rs.getString("status") }
                            )
                        }
                        return result
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findAllOwnerParcels: ${e.message}")
            return emptyList()
        }
    }
    
    fun changeStatus(parcelId: Int, from: ParcelStatus, to: ParcelStatus): Boolean {
        val sql = "UPDATE bb_parcels SET status = ? WHERE id = ? AND server_id = ? AND status = ?"
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, to.value)
                    ps.setInt(2, parcelId)
                    ps.setString(3, serverId)
                    ps.setString(4, from.value)
                    return ps.executeUpdate() > 0
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] changeStatus: ${e.message}")
            return false
        }
    }
    
    fun deleteById(parcelId: Int): Boolean {
       db.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                // 1. Verrouiller la parcelle et vérifier qu'elle n'est pas louée
                val status = conn.prepareStatement(
                    "SELECT status FROM bb_parcels WHERE server_id = ? AND id = ? FOR UPDATE"
                ).use { ps ->
                    ps.setString(1, serverId)
                    ps.setInt(2, parcelId)
                    ps.executeQuery().use { rs -> if (rs.next()) ParcelStatus.from(rs.getString("status")) else null }
                }
                if (status == null || status == ParcelStatus.RENTED) {
                    conn.rollback()
                    return false
                }

                // 2. Membres de toutes les locations de la parcelle
                conn.prepareStatement(
                    """
                    DELETE m FROM bb_rental_members m
                    INNER JOIN bb_rentals r ON r.id = m.rental_id
                    WHERE r.parcel_id = ?
                    """.trimIndent()
                ).use { ps ->
                    ps.setInt(1, parcelId)
                    ps.executeUpdate()
                }

                // 3. Locations
                conn.prepareStatement("DELETE FROM bb_rentals WHERE parcel_id = ?").use { ps ->
                    ps.setInt(1, parcelId)
                    ps.executeUpdate()
                }

                // 4. Parcelle
                conn.prepareStatement("DELETE FROM bb_parcels WHERE id = ?").use { ps ->
                    ps.setInt(1, parcelId)
                    ps.executeUpdate()
                }

                conn.commit()
                return true
            } catch (e: SQLException) {
                conn.rollback()
                Bukkit.getLogger().severe("[BbParcels] deleteWithHistory: ${e.message}")
                return false
            } finally {
                conn.autoCommit = true
            }
        }
    }
}