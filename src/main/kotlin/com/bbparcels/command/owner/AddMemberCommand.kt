package com.bbparcels.command.owner

import com.bbparcels.BbParcels
import com.bbparcels.model.RentalMember
import com.bbparcels.model.RentalMemberRole
import com.bbparcels.repository.ParcelRepository
import com.bbparcels.repository.RentalMemberRepository
import com.bbparcels.repository.RentalRepository
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AddMemberCommand(private val plugin: BbParcels): SubOwnerCommand {
    
    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val rentalRepo = RentalRepository(plugin.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(plugin.databaseManager)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /myparcel <parcelLabel> add <playerName>")
            return true
        }
        
        val parcelLabel = args[0]
        val toAddPlayerName = args[1]
        val toAddPlayer = Bukkit.getPlayer(toAddPlayerName)
        
        if (toAddPlayer == null){
            sender.sendMessage("§cCe joueur n'existe pas.")
            return true
        }
        
        val senderId = sender.uniqueId.toString()
        
        val ownerParcels = parcelRepo.findAllOwnerParcels(senderId)
        val parcel = ownerParcels.find {it.label == parcelLabel}
        
        if (parcel == null) {
            sender.sendMessage("§cVous n'avez pas cette parcelle.")
            return true
        }
        
        val rental = rentalRepo.findActiveByParcelId(parcel.id)
        
        if (rental == null) {
            sender.sendMessage("§cCette parcelle n'a pas de location active.")
            return true
        }
        
        val membersUuid = rentalMemberRepo.findMembers(parcel.label)
        
        if (toAddPlayer.uniqueId.toString() in membersUuid) {
            sender.sendMessage("§cCe joueur fait déjà partie de la parcelle.")
            return true
        }
        
        
        val newId = rentalMemberRepo.addMember(
            RentalMember(
                rentalId = rental.id,
                memberUuid = toAddPlayer.uniqueId.toString(),
                role = RentalMemberRole.MEMBER
            )
        )
        
        if (newId > 0) {
            val world = Bukkit.getWorld(parcel.world)
            if (world != null) {
                val regionManager = WorldGuard.getInstance()
                    .platform.regionContainer
                    .get(BukkitAdapter.adapt(world))

                val region = regionManager?.getRegion(parcel.regionId)
                if (region != null) {
                    region.members.addPlayer(toAddPlayer.uniqueId)
                    regionManager.save()
                } else {
                    plugin.logger.warning("Région WorldGuard '${parcel.regionId}' introuvable !")
                }
            }
            sender.sendMessage("§a✔ Le joueur §e$toAddPlayerName §aa été ajouté à la parcelle §e$parcelLabel §a!")
        }
        else
            sender.sendMessage("§cErreur lors de l'ajout du joueur.")
        return true
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
    if (sender !is Player) return emptyList()
    if (args.size != 2) return emptyList()

    val parcelLabel = args[0]   // déjà connu
    val prefix      = args[1]   // ce que le joueur est en train de taper

    val membersUuid = rentalMemberRepo.findMembers(parcelLabel)

    return Bukkit.getOnlinePlayers()
        .filter { it.uniqueId.toString() !in membersUuid }
        .filter { it.uniqueId != sender.uniqueId }
        .map { it.name }
        .filter { it.startsWith(prefix, ignoreCase = true) }
}
}