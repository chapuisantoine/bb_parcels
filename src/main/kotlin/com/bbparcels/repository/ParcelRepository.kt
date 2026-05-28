package com.bbparcels.repository

import com.bbparcels.database.DatabaseManager
import com.bbparcels.model.Parcel
import com.bbparcels.model.ParcelStatus
import com.bbparcels.model.ParcelWithOwner
import org.bukkit.Bukkit
import java.sql.SQLException
import java.sql.Statement

class ParcelRepository(private val db: DatabaseManager) {

    fun existsByRegion(regionId: String, world: String): Boolean {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT COUNT(*) FROM bb_parcels WHERE region_id = ? AND world = ?"
                ).use { ps ->
                    ps.setString(1, regionId)
                    ps.setString(2, world)
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

    fun addParcel(parcel: Parcel): Int {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    """INSERT INTO bb_parcels (region_id, world, label, description, status, active)
                    VALUES (?, ?, ?, ?, ?, ?)""",
                    Statement.RETURN_GENERATED_KEYS
                ).use { ps ->
                    ps.setString(1, parcel.regionId)
                    ps.setString(2, parcel.world)
                    ps.setString(3, parcel.label)
                    ps.setString(4, parcel.description)
                    ps.setString(5, parcel.status.value)
                    ps.setBoolean(6, parcel.active)
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
    
    fun findById(id: Int): Parcel? {
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT id, region_id, world, label, description, status, active FROM bb_parcels WHERE id = ?"
                ).use { ps ->
                    ps.setInt(1, id)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) return null
                        return Parcel(
                            id          = rs.getInt("id"),
                            regionId    = rs.getString("region_id"),
                            world       = rs.getString("world"),
                            label       = rs.getString("label"),
                            description = rs.getString("description"),
                            status      = ParcelStatus.entries.first { it.value == rs.getString("status") },
                            active      = rs.getBoolean("active")
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findById: ${e.message}")
            return null
        }
    }
    
    fun findByLabel(label: String): Parcel? {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT id, region_id, world, label, description, status, active FROM bb_parcels WHERE label = ?"
                ).use { ps ->
                    ps.setString(1, label)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) return null
                        return Parcel(
                            id          = rs.getInt("id"),
                            regionId    = rs.getString("region_id"),
                            world       = rs.getString("world"),
                            label       = rs.getString("label"),
                            description = rs.getString("description"),
                            status      = ParcelStatus.entries.first { it.value == rs.getString("status") },
                            active      = rs.getBoolean("active")
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
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement("""
                    SELECT bb_parcels.label, bb_parcels.region_id, bb_parcels.world,
                    bb_parcels.status, bb_rentals.tenant_uuid AS owner
                    FROM bb_parcels
                    LEFT JOIN bb_rentals
                        ON bb_rentals.parcel_id = bb_parcels.id
                        AND bb_rentals.status = 'active'
                    WHERE bb_parcels.active = true AND bb_parcels.label = ?
                """.trimIndent()
                ).use { ps ->
                    ps.setString(1, label)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) return null
                        return ParcelWithOwner(
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
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT id, region_id, world, label, description, status, active FROM bb_parcels WHERE active = true"
                ).use { ps ->
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
                                active      = rs.getBoolean("active")
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
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement("""
                    SELECT bb_parcels.label, bb_parcels.region_id, bb_parcels.world,
                    bb_parcels.status, bb_rentals.tenant_uuid AS owner
                    FROM bb_parcels
                    LEFT JOIN bb_rentals
                        ON bb_rentals.parcel_id = bb_parcels.id
                        AND bb_rentals.status = 'active'
                    WHERE bb_parcels.active = true
                """.trimIndent()
                ).use { ps ->
                    ps.executeQuery().use { rs ->
                        val result = mutableListOf<ParcelWithOwner>()
                        while (rs.next()) {
                            result += ParcelWithOwner(
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
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(
                "SELECT bb_parcels.id, bb_parcels.label, bb_parcels.region_id, bb_parcels.world, bb_parcels.status FROM bb_parcels LEFT JOIN bb_rentals ON bb_rentals.parcel_id = bb_parcels.id WHERE bb_rentals.tenant_uuid = ?"   
                ).use { ps ->
                    ps.setString(1, playerUuid)
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

    fun updateParcelStatus(parcelId: Int, newStatus: ParcelStatus ): Boolean {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    """UPDATE bb_parcels SET status = ? WHERE id = ?"""
                    ).use { ps ->
                        ps.setString(1, newStatus.value)
                        ps.setInt(2, parcelId)
                        return ps.executeUpdate() > 0
                    }
            }   
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] updateParcelStatus: ${e.message}")
            return false
        }
    }
    
    
    fun deleteById(id: Int): Boolean {
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    "UPDATE bb_parcels SET active = false WHERE id = ?"
                ).use { ps ->
                    ps.setInt(1, id)
                    return ps.executeUpdate() > 0
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] deleteById: ${e.message}")
            return false
        }
    }
}