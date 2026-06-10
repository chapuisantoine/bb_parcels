package com.bbparcels.command.admin

import com.bbparcels.BbParcels
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class AdminCommand(plugin: BbParcels) : CommandExecutor, TabCompleter {

    private val subCommands: Map<String, SubAdminCommand> = mapOf(
        "add"    to AddParcelCommand(plugin),
        "delete" to DeleteParcelCommand(plugin),
        "find" to findParcelCommand(plugin),
        "givetablet" to GiveTabletCommand(),
        "givekey" to GiveParcelKeyCommand(plugin)
    )

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("bbparcels.admin")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.")
            return true
        }

        val sub = subCommands[args.getOrNull(0)?.lowercase()]
        if (sub == null) { sendHelp(sender); return true }

        return sub.execute(sender, args.drop(1))
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (!sender.hasPermission("bbparcels.admin")) return emptyList()

        if (args.size == 1)
            return subCommands.keys.filter { it.startsWith(args[0].lowercase()) }

        val sub = subCommands[args[0].lowercase()] ?: return emptyList()
        return sub.tabComplete(sender, args.drop(1))
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== Parcels Admin ===")
        sender.sendMessage("§e/parceladmin add <region_id> <parcelLabel> [description]")
        sender.sendMessage("§e/parceladmin find <parcelLabel>")
        sender.sendMessage("§e/parceladmin delete <id>")
        sender.sendMessage("§e/parceladmin givetablet <playerName>")
        sender.sendMessage("§e/parceladmin givekey <parcelLabel> <playerName>")
    }
}