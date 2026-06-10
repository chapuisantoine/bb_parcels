package com.bbparcels.command.owner

import com.bbparcels.BbParcels
import com.bbparcels.repository.ParcelRepository
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class OwnerCommand (plugin: BbParcels): CommandExecutor, TabCompleter {
    
    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    
    private val subCommands: Map<String, SubOwnerCommand> = mapOf(
        "add"    to AddMemberCommand(plugin),
            "list"   to ListMemberCommand(plugin),
        "remove" to DeleteMemberCommand(plugin),
    )
    
    // /myparcel <parcelLabel> <add|delete> <playerName>
    //  args[0]=parcelLabel   args[1]=sub   args[2]=playerName
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.size < 2) { sendHelp(sender); return true }

        val sub = subCommands[args[1].lowercase()]
        if (sub == null) { sendHelp(sender); return true }

        // passe [parcelLabel, playerName] à la sous-commande
        val subArgs = mutableListOf(args[0])
        subArgs.addAll(args.drop(2))
        return sub.execute(sender, subArgs)
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (sender !is Player) return emptyList()

        return when (args.size) {
            // arg 1 — labels des parcelles du joueur
            1 -> parcelRepo.findAllOwnerParcels(sender.uniqueId.toString())
                    .map { it.label }
                    .filter { it.startsWith(args[0], ignoreCase = true) }

            // arg 2 — sous-commandes
            2 -> subCommands.keys
                    .filter { it.startsWith(args[1].lowercase()) }

            // arg 3+ — délègue avec [parcelLabel, playerPrefix]
            else -> {
                val sub = subCommands[args[1].lowercase()] ?: return emptyList()
                val subArgs = mutableListOf(args[0])
                subArgs.addAll(args.drop(2))
                return sub.tabComplete(sender, subArgs)
            }
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== Parcels Owner ===")
        sender.sendMessage("§e/myparcel <parcelLabel> add <playerName>")
        sender.sendMessage("§e/myparcel <parcelLabel> list")
        sender.sendMessage("§e/myparcel <parcelLabel> remove <playerName>")
    }
}