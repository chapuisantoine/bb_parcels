package com.bbparcels.repository

import com.bbparcels.database.DatabaseManager
import com.bbparcels.model.Rental
import com.bbparcels.model.RentalMember
import com.bbparcels.model.RentalStatus
import org.bukkit.Bukkit
import java.sql.SQLException
import java.sql.Statement
import java.sql.Timestamp

class RentalRepository(private val db: DatabaseManager) {

    fun addRental(rental: Rental): Int {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    """INSERT INTO bb_rentals (parcel_id, tenant_uuid, start_date, status)
                        VALUES (?, ?, ?, ?)""",
                    Statement.RETURN_GENERATED_KEYS
                ).use { ps ->
                    ps.setInt(1, rental.parcelId)
                    ps.setString(2, rental.tenantUuid)
                    ps.setTimestamp(3, Timestamp.valueOf(rental.startDate))
                    ps.setString(5, rental.status.value)
                    ps.executeUpdate()
                    ps.generatedKeys.use { rs ->
                        return if (rs.next()) rs.getInt(1) else -1
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] addRental: ${e.message}")
            return -1
        } 
    }

    
    fun rentWithMember(rental: Rental, member: RentalMember): Int {
        db.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                val rentalId = conn.prepareStatement(
                    """INSERT INTO bb_rentals (parcel_id, tenant_uuid, start_date, status)
                       VALUES (?, ?, ?, ?)""",
                    Statement.RETURN_GENERATED_KEYS
                ).use { ps ->
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
                    """INSERT INTO bb_rental_members (rental_id, member_uuid, role, added_at)
                       VALUES (?, ?, ?, ?)"""
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
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    fun findActiveByParcelId(parcelId: Int): Rental? {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    "SELECT id, parcel_id, tenant_uuid, start_date, end_date, status FROM bb_rentals WHERE parcel_id = ? AND status = 'active'"
                ).use { ps ->
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

    fun findExpiredRentals(): List<Rental> {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    """SELECT id, parcel_id, tenant_uuid, start_date, end_date, status
                    FROM bb_rentals WHERE status = 'active' AND end_date < NOW()"""
                ).use { ps ->
                    ps.executeQuery().use { rs ->
                        val rentals = mutableListOf<Rental>()
                        while (rs.next()) {
                            rentals += Rental(
                                id         = rs.getInt("id"),
                                parcelId   = rs.getInt("parcel_id"),
                                tenantUuid = rs.getString("tenant_uuid"),
                                startDate  = rs.getTimestamp("start_date").toLocalDateTime(),
                                endDate    = rs.getTimestamp("end_date")?.toLocalDateTime(),
                                status     = RentalStatus.ACTIVE
                            )
                        }
                        return rentals
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] findExpiredRentals: ${e.message}")
            return emptyList()
        }
    }

    fun terminateRental(id: Int): Boolean {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    "UPDATE bb_rentals SET status = 'terminated', end_date = NOW() WHERE id = ?"
                ).use { ps ->
                    ps.setInt(1, id)
                    return ps.executeUpdate() > 0
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] terminateRental: ${e.message}")
            return false
        }
    }

    fun expireRental(id: Int): Boolean {
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(
                    "UPDATE bb_rentals SET status = 'expired' WHERE id = ?"
                ).use { ps ->
                    ps.setInt(1, id)
                    return ps.executeUpdate() > 0
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbParcels] expireRental: ${e.message}")
            return false
        }
    }
}
