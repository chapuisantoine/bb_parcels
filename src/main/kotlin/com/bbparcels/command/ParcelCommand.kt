package com.bbparcels.command

import com.bbparcels.BbParcels
import com.bbparcels.cache.ActiveProfileCache
import com.bbparcels.cache.MemberCache
import com.bbparcels.cache.ParcelCache
import com.bbparcels.config.PluginConfig
import com.bbparcels.database.repository.ParcelStatus
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class AdminCommand(
    plugin: BbParcels,
    parcelCache: ParcelCache,
    memberCache: MemberCache,
    activeProfileCache: ActiveProfileCache
    ) : CommandExecutor, TabCompleter {

    private val subCommands: Map<String, SubCommand> = mapOf(
        "add"    to AddParcelCommand(plugin, parcelCache),
        "delete" to DeleteParcelCommand(plugin, parcelCache),
        "enable" to ChangeParcelStatusCommand(plugin, ParcelStatus.DISABLED, ParcelStatus.AVAILABLE, parcelCache),
        "disable" to ChangeParcelStatusCommand(plugin, ParcelStatus.AVAILABLE, ParcelStatus.DISABLED, parcelCache),
        "find" to FindParcelCommand(plugin, parcelCache),
        "givetablet" to GiveTabletCommand(),
        "givekey" to GiveParcelKeyCommand(plugin, parcelCache),
        "refreshcache" to RefreshCacheCommand(plugin, parcelCache, memberCache, activeProfileCache)
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
        sender.sendMessage("§e/parceladmin add <region_id> <label> <weeklyPrice> [description]")
        sender.sendMessage("§e/parceladmin find <label>")
        sender.sendMessage("§e/parceladmin enable <label>")
        sender.sendMessage("§e/parceladmin disable <label>")
        sender.sendMessage("§e/parceladmin delete <label>")
        sender.sendMessage("§e/parceladmin givetablet <playerName>")
        sender.sendMessage("§e/parceladmin givekey <label> <playerName>")
        sender.sendMessage("§e/parceladmin refreshcache")
    }
}

class AgentCommand(
    plugin: BbParcels,
    pluginConfig: PluginConfig,
    parcelCache: ParcelCache,
    memberCache: MemberCache,
    activeProfileCache: ActiveProfileCache
    ) : CommandExecutor, TabCompleter {
    

    private val subCommands: Map<String, SubCommand> = mapOf(
        "rent"    to RentParcelCommand(plugin, parcelCache, memberCache, activeProfileCache),
        "list" to ListParcelCommand(plugin),
        "info" to InfoParcelCommand(plugin, parcelCache),
        "unrent" to UnrentParcelCommand(plugin, parcelCache, memberCache),
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
        sender.sendMessage("§e/parcelagent rent <label> <playerName> [description]")
        sender.sendMessage("§e/parcelagent list")
        sender.sendMessage("§e/parcelagent info <label>")
        sender.sendMessage("§e/parcelagent unrent <label>")
    }
}

class OwnerCommand (
    plugin: BbParcels,
    private val parcelCache: ParcelCache,
    private val activeProfilCache: ActiveProfileCache,
    memberCache: MemberCache
    ): CommandExecutor, TabCompleter {
        
    private val subCommands: Map<String, SubCommand> = mapOf(
        "add"    to AddMemberCommand(plugin, parcelCache, memberCache),
        "list"   to ListMemberCommand(plugin),
        "remove" to RemoveMemberCommand(plugin, parcelCache, memberCache),
    )

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.size < 2) { sendHelp(sender); return true }

        val sub = subCommands[args[1].lowercase()]
        if (sub == null) { sendHelp(sender); return true }

        val subArgs = mutableListOf(args[0])
        subArgs.addAll(args.drop(2))
        return sub.execute(sender, subArgs)
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (sender !is Player) return emptyList()

        return when (args.size) {
            1 -> activeProfilCache.get(sender.uniqueId)
                ?.let { parcelCache.parcelsRentedBy(it.id, args[0]) }
                .orEmpty()
            2 -> subCommands.keys.filter { it.startsWith(args[1].lowercase()) }
            else -> {
                val sub = subCommands[args[1].lowercase()] ?: return emptyList()
                sub.tabComplete(sender, listOf(args[0]) + args.drop(2))
            }
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== Parcels Owner ===")
        sender.sendMessage("§e/myparcel <label> add <playerName>")
        sender.sendMessage("§e/myparcel <label> list")
        sender.sendMessage("§e/myparcel <label> remove <playerName>")
    }
}