package com.bbparcels.command.agent

import com.bbparcels.BbParcels
import com.bbparcels.model.ParcelStatus
import com.bbparcels.repository.ParcelRepository
import com.bbparcels.repository.RentalMemberRepository
import com.bbparcels.repository.RentalRepository
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.util.*

class UnrentParcelCommand(plugin: BbParcels) : SubAgentCommand {

    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val rentalRepo = RentalRepository(plugin.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(plugin.databaseManager)

    // /parcelagent unrent <parcelLabel>
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /parcelagent unrent <parcelLabel>")
            return true
        }

        val parcelName = args[0]
        val parcel = parcelRepo.findByLabel(parcelName)
        if (parcel == null) {
            sender.sendMessage("§cLa parcelle §e$parcelName §cn'existe pas.")
            return true
        }
                
        if (parcel.status != ParcelStatus.RENTED) {
            sender.sendMessage("§cLa parcelle §e$parcelName §cn'est pas en cours de location.")
            return true
        }

        val rental = rentalRepo.findActiveByParcelId(parcel.id)
        if (rental == null) {
            sender.sendMessage("§cAucune location active trouvée pour §e$parcelName§c.")
            return true
        }

        rentalRepo.terminateRental(rental.id)
        parcelRepo.updateParcelStatus(parcel.id, ParcelStatus.AVAILABLE)
        
        val allMemberUuids = rentalMemberRepo.findMembersByRentalId(rental.id)

        val world = Bukkit.getWorld(parcel.world)
        if (world != null) {
            val regionManager = WorldGuard.getInstance()
                .platform.regionContainer
                .get(BukkitAdapter.adapt(world))
            val region = regionManager?.getRegion(parcel.regionId)
            if (region != null) {
                allMemberUuids.forEach { uuid ->
                    region.members.removePlayer(UUID.fromString(uuid))
                }
                regionManager.save()
            }
        }
        
        rentalMemberRepo.deleteAllByRentalId(rental.id)

        sender.sendMessage("§a✔ La location de §e$parcelName §aa été résiliée.")
        return true
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size != 1) return emptyList()
        return parcelRepo.findAll()
            .filter { it.status == ParcelStatus.RENTED }
            .map { it.label }
            .filter { it.startsWith(args[0], ignoreCase = true) }
    }
}