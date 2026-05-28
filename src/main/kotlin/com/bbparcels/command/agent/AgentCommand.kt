package com.bbparcels.command.agent

import com.bbparcels.BbParcels
import com.bbparcels.config.PluginConfig
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class AgentCommand(plugin: BbParcels, pluginConfig: PluginConfig) : CommandExecutor, TabCompleter {
    

    private val subCommands: Map<String, SubAgentCommand> = mapOf(
        "rent"    to RentParcelCommand(plugin),
            "list" to ListParcelCommand(plugin),
            "info" to InfoParcelCommand(plugin),
        "unrent" to UnrentParcelCommand(plugin),
    )

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("bbparcels.agentImmo")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.")
            return true
        }

        val sub = subCommands[args.getOrNull(0)?.lowercase()]
        if (sub == null) { sendHelp(sender); return true }

        return sub.execute(sender, args.drop(1))
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (!sender.hasPermission("bbparcels.agentImmo")) return emptyList()

        if (args.size == 1)
            return subCommands.keys.filter { it.startsWith(args[0].lowercase()) }

        val sub = subCommands[args[0].lowercase()] ?: return emptyList()
        return sub.tabComplete(sender, args.drop(1))
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== Parcels Agent ===")
        sender.sendMessage("§e/parcelagent rent <parcelName> <playerName> [description]")
        sender.sendMessage("§e/parcelagent list")
        sender.sendMessage("§e/parcelagent info <parcelLabel>")
        sender.sendMessage("§e/parcelagent unrent <parcelName>")
    }
}