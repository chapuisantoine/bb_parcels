package com.bbparcels.command.admin

import com.bbparcels.gui.agent.AgentGuiListener
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GiveTabletCommand : SubAdminCommand {

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /parceladmin givetablet <playerName>")
            return true
        }
        
        val playerName = args[0]
        val target = Bukkit.getPlayer(playerName)
        if (target == null) {
            sender.sendMessage("§cJoueur introuvable.")
            return true
        } else {
            target.inventory.addItem(AgentGuiListener.createTablet())
            sender.sendMessage("§aTablette donnée à §e${target.name}§a.")
            target.sendMessage("§aVous avez reçu la §6Tablette Agent Immo§a.")
            return true
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size == 1)
            return Bukkit.getOnlinePlayers().map {it.name}.filter {it.startsWith(args[0], ignoreCase = true)}
        return emptyList()
    }
}