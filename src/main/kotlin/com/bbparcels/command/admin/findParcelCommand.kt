package com.bbparcels.command.admin

import com.bbparcels.BbParcels
import com.bbparcels.repository.ParcelRepository
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class findParcelCommand(plugin: BbParcels) : SubAdminCommand {
    
    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /parceladmin find <label>")
            return true
        }
        
        val parcelLabel = args[0]
        val parcel = parcelRepo.findByLabel(parcelLabel)
        if (parcel == null) {
            sender.sendMessage("§cParcelle non trouvée.")
            return true
        }
        
        val world = Bukkit.getWorld(parcel.world)
        val regionManager = WorldGuard.getInstance()
            .platform.regionContainer
            .get(BukkitAdapter.adapt(world))
        val region = regionManager?.getRegion(parcel.regionId) ?: return true
        
        val min = region.minimumPoint
        val max = region.maximumPoint

        val centerX = (min.x + max.x) / 2.0
        val frontZ = min.z - 2.0
        val y = (min.y + 1).toDouble()
        
        val coords = "§e(${"%.1f".format(centerX)}, ${"%.1f".format(y)}, ${"%.1f".format(frontZ)})"

        val message = TextComponent("§7Parcelle §a${parcel.label} §7→ $coords")
        message.clickEvent = ClickEvent(
            ClickEvent.Action.RUN_COMMAND,
            "/tp ${centerX.toInt()} ${y.toInt()} ${frontZ.toInt()}"
        )
        message.hoverEvent = HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            Text("§eCliquez pour vous téléporter devant la parcelle")
        )

        sender.spigot().sendMessage(message)
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
    if (args.size != 1) return emptyList()
    return parcelRepo.findAll()
        .map { it.label }
        .filter { it.startsWith(args[0], ignoreCase = true) }
    }
}