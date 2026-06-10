package com.bbparcels.command.admin

import com.bbparcels.BbParcels
import com.bbparcels.gui.owner.OwnerGuiListener
import com.bbparcels.model.ParcelStatus
import com.bbparcels.repository.ParcelRepository
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GiveParcelKeyCommand(plugin: BbParcels) : SubAdminCommand {
    
    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /parceladmin givekey <parcelLabel> <playerName>")
            return true
        }
        
        val parcelLabel = args[0]
        val playerName = args[1]
        val parcel = parcelRepo.findByLabelWithOwner(parcelLabel)
        
        if (parcel == null) {
            sender.sendMessage("§cPas de parcelle à ce nom.")
            return true
        }
        
        val target = Bukkit.getPlayer(playerName)
        if (target == null) {
            sender.sendMessage("§cJoueur introuvable.")
            return true
        } else {
            target.inventory.addItem(OwnerGuiListener.createParcelKey(parcel))
            sender.sendMessage("§aClé de la parcelle §e${parcel.label} §adonnée à §e${target.name}§a.")
            target.sendMessage("§aVous avez reçu la §6Clé - ${parcel.label}§a.")
            return true
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> parcelRepo.findAll()
                    .filter { it.status == ParcelStatus.RENTED }
                    .map { it.label }
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> Bukkit.getOnlinePlayers()
                    .map { it.name }
                    .filter { it.startsWith(args[1], ignoreCase = true) }

            else -> emptyList()
        }
    }
}