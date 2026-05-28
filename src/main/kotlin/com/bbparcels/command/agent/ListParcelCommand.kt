package com.bbparcels.command.agent

import com.bbparcels.BbParcels
import com.bbparcels.repository.ParcelRepository
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class ListParcelCommand(private val plugin: BbParcels) : SubAgentCommand {
    
    private val repo = ParcelRepository(plugin.databaseManager)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (!args.isEmpty()) {
            sender.sendMessage("§cUsage : /parcelagent list")
            return true
        }
        
        val parcels = repo.findAllWithOwners()
        
        for (parcel in parcels) {
            val ownerId = parcel.ownerUuid
            val playerName = if (ownerId != null)
                Bukkit.getPlayer(UUID.fromString(ownerId))?.name?: ownerId
            else
                null
            sender.sendMessage("§aParcel : §e${parcel.label} §a| Status : §e${parcel.status} §a| Owner : §e$playerName")
        }
            
        return true
    } 
}