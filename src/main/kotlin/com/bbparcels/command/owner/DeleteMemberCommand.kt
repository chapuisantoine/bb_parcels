package com.bbparcels.command.owner

import com.bbparcels.BbParcels
import com.bbparcels.repository.ParcelRepository
import com.bbparcels.repository.RentalMemberRepository
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class DeleteMemberCommand(private val plugin: BbParcels) : SubOwnerCommand {

    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(plugin.databaseManager)

    // reçoit args = [parcelLabel, playerName]
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /myparcel <parcelLabel> remove <playerName>")
            return true
        }

        val parcelLabel    = args[0]
        val playerName     = args[1]

        // Vérifier que la parcelle appartient au sender
        val ownerParcels = parcelRepo.findAllOwnerParcels(sender.uniqueId.toString())
        val parcel = ownerParcels.find { it.label == parcelLabel }
        
        if (parcel == null) {
            sender.sendMessage("§cVous n'avez pas cette parcelle.")
            return true
        }

        val membersUuid = rentalMemberRepo.findMembers(parcelLabel)
        val targetUuid = membersUuid.firstOrNull { uuid ->
            Bukkit.getPlayer(UUID.fromString(uuid))?.name
                ?.equals(playerName, ignoreCase = true) == true
        }
        if (targetUuid == null) {
            sender.sendMessage("§c§e$playerName §cn'est pas membre de cette parcelle.")
            return true
        }

        val deleted = rentalMemberRepo.deleteMember(parcelLabel, targetUuid)
        val world = Bukkit.getWorld(parcel.world)
        if (world != null) {
            val regionManager = WorldGuard.getInstance()
                .platform.regionContainer
                .get(BukkitAdapter.adapt(world))
            val region = regionManager?.getRegion(parcel.regionId)
            if (region != null) {
                region.members.removePlayer(UUID.fromString(targetUuid))
                regionManager.save()
            }
        }
        
        if (deleted)
            sender.sendMessage("§a✔ §e$playerName §aa été retiré de la parcelle §e$parcelLabel§a.")
        else
            sender.sendMessage("§cErreur lors de la suppression.")

        return true
    }

    // tabComplete : arg1=parcelLabel (connu), arg2=membres actuels
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (sender !is Player) return emptyList()
        if (args.size != 2) return emptyList()

        val parcelLabel = args[0]
        val prefix      = args[1]

        return rentalMemberRepo.findMembers(parcelLabel)
            .mapNotNull { uuid ->
                Bukkit.getPlayer(UUID.fromString(uuid))?.name
            }
            .filter { it.startsWith(prefix, ignoreCase = true) }
    }
}