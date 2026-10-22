package com.bbparcels.database.repository

import com.bbparcels.BbParcels
import com.bbparcels.database.DatabaseManager
import org.bukkit.Bukkit
import java.sql.SQLException
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime

data class Rental(
    val id: Int = 0,
    val parcelId: Int,
    val tenantUuid: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime? = null,
    val status: RentalStatus = RentalStatus.ACTIVE    
)

enum class RentalStatus(val value: String) {
    ACTIVE(value = "active"),
    EXPIRED(value = "expired"),
    TERMINATED(value= "terminated")
        
}

class RentalRepository(
    private val db: DatabaseManager,
    private val serverId: String = BbParcels.instance.pluginConfig.serverId
    ) {
    
    fun rentWithMember(rental: Rental, member: RentalMember): Int {
        db.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                val reserved = conn.prepareStatement("UPDATE bb_parcels SET status = ? WHERE id = ? AND status = ? AND server_id = ?")
                .use { ps ->
                    ps.setString(1, ParcelStatus.RENTED.value)
                    ps.setInt(2, rental.parcelId)
                    ps.setString(3, ParcelStatus.AVAILABLE.value)
                    ps.setString(4, serverId)
                    ps.executeUpdate() > 0
                }
                if (!reserved) {
                    conn.rollback()
                    return 0
                }
                
                val rentalId = conn.prepareStatement(
                    "INSERT INTO bb_rentals (parcel_id, tenant_uuid, start_date, status) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                ).use {ps ->
                    ps.setInt(1, rental.parcelId)
                    ps.setString(2, rental.tenantUuid)
                    ps.setTimestamp(3, Timestamp.valueOf(rental.startDate))
                    ps.setString(4, rental.status.value)
                    ps.executeUpdate()
                    ps.generatedKeys.use { rs ->
                        if (rs.next()) rs.getInt(1)
                        else throw SQLException("Impossible de récupérer l'ID de la location.")
                    }
                }

                conn.prepareStatement(
                    "INSERT INTO bb_rental_members (rental_id, member_uuid, role, added_at) VALUES (?, ?, ?, ?)"
                ).use { ps ->
                    ps.setInt(1, rentalId)
                    ps.setString(2, member.memberUuid)
                    ps.setString(3, member.role.value)
                    ps.setTimestamp(4, Timestamp.valueOf(member.addedAt))
                    ps.executeUpdate()
                }

                conn.commit()
                return rentalId
            } catch (e: Exception) {
                conn.rollback()
                Bukkit.getLogger().severe("[BbParcels] rentWithMember: ${e.message}")
                return -1
            } finally {
                conn.autoCommit = true
            }
        }
    }

    fun findActiveByParcelId(parcelId: Int): Rental? {
        val sql = "SELECT id, parcel_id, tenant_uuid, start_date, end_date, status FROM bb_rentals WHERE parcel_id = ? AND status = 'active'"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setInt(1, parcelId)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) return null
                        return Rental(
                            id         = rs.getInt("id"),
                            parcelId   = rs.getInt("parcel_id"),
                            tenantUuid = rs.getString("tenant_uuid"),
                            startDate  = rs.getTimestamp("start_date").toLocalDateTime(),
                            endDate    = rs.getTimestamp("end_date")?.toLocalDateTime(),
                            status     = RentalStatus.ACTIVE
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findActiveByParcelId: ${e.message}")
            return null
        }
    }

    fun terminateWithMembers(rentalId: Int, parcelId: Int): List<String>? {
    db.getConnection().use { conn ->
        conn.autoCommit = false
        try {
            // 1. Résilier la location, seulement si elle est encore active (protège contre deux unrent simultanés)
            val terminated = conn.prepareStatement(
                "UPDATE bb_rentals SET status = ?, end_date = NOW() WHERE id = ? AND status = ?"
            ).use { ps ->
                ps.setString(1, RentalStatus.TERMINATED.value)
                ps.setInt(2, rentalId)
                ps.setString(3, RentalStatus.ACTIVE.value)
                ps.executeUpdate() > 0
            }
            if (!terminated) {
                conn.rollback()
                return null
            }

            // 2. Récupérer les membres avant de les supprimer
            val members = conn.prepareStatement(
                "SELECT member_uuid FROM bb_rental_members WHERE rental_id = ?"
            ).use { ps ->
                ps.setInt(1, rentalId)
                ps.executeQuery().use { rs ->
                    buildList { while (rs.next()) add(rs.getString("member_uuid")) }
                }
            }

            // 3. Supprimer les membres
            conn.prepareStatement(
                "DELETE FROM bb_rental_members WHERE rental_id = ?"
            ).use { ps ->
                ps.setInt(1, rentalId)
                ps.executeUpdate()
            }

            // 4. Remettre la parcelle disponible
            conn.prepareStatement(
                "UPDATE bb_parcels SET status = ? WHERE id = ?"
            ).use { ps ->
                ps.setString(1, ParcelStatus.AVAILABLE.value)
                ps.setInt(2, parcelId)
                ps.executeUpdate()
            }

            conn.commit()
            return members
        } catch (e: SQLException) {
            conn.rollback()
            Bukkit.getLogger().severe("[BbParcels] terminateWithMembers: ${e.message}")
            return null
        } finally {
            conn.autoCommit = true
        }
    }
}
}
