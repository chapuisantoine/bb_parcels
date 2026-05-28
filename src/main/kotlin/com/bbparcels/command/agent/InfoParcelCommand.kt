package com.bbparcels.command.agent

import com.bbparcels.BbParcels
import com.bbparcels.repository.ParcelRepository
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class InfoParcelCommand(plugin: BbParcels) : SubAgentCommand {
    
    private val repo = ParcelRepository(plugin.databaseManager)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /parcelagent info <parcelName>")
            return true
        }
        
        val parcelName = args[0]
        
        val parcel = repo.findByLabelWithOwner(parcelName)
        
        if (parcel != null) {
            val ownerId = parcel.ownerUuid
            val playerName = if (ownerId != null)
                Bukkit.getPlayer(UUID.fromString(ownerId))?.name?: ownerId
            else
                null
            sender.sendMessage("§aParcel : §e${parcel.label} §a| regionId : §e${parcel.regionId} §a| world : §e${parcel.world} §a| Status : §e${parcel.status} §a| Owner §e${playerName}")
        }
        else
            sender.sendMessage("§cCette parcelle n'existe pas ou n'a pas été trouvé.")
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
    if (args.size != 1) return emptyList()
    return repo.findAll()
        .map { it.label }
        .filter { it.startsWith(args[0], ignoreCase = true) }
    }
}