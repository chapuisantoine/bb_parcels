package com.bbparcels.command.admin

import com.bbparcels.BbParcels
import com.bbparcels.model.Parcel
import com.bbparcels.repository.ParcelRepository
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AddParcelCommand(private val plugin: BbParcels) : SubAdminCommand {

    private val repo = ParcelRepository(plugin.databaseManager)

    // /parceladmin add <region_id> <label> [description...]
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /parceladmin add <region_id> <label> [description...]")
            return true
        }

        val regionId    = args[0]
        val label       = args[1]
        val description = if (args.size > 2) args.drop(2).joinToString(" ") else null
        val world       = sender.world

        val regionManager = WorldGuard.getInstance()
            .platform.regionContainer
            .get(BukkitAdapter.adapt(world))

        if (regionManager == null) {
            sender.sendMessage("§cImpossible de charger les régions du monde §e${world.name}§c.")
            return true
        }
        if (regionManager.getRegion(regionId) == null) {
            sender.sendMessage("§cLa région §e$regionId §cn'existe pas dans §e${world.name}§c.")
            return true
        }
        if (repo.existsByRegion(regionId, world.name)) {
            sender.sendMessage("§cCette région est déjà enregistrée en base.")
            return true
        }

        val newId = repo.addParcel(
            Parcel(regionId = regionId, world = world.name, label = label, description = description)
        )

        if (newId > 0)
            sender.sendMessage("§a✔ Parcelle §e$label §aajoutée ! §7(région: $regionId | id: $newId)")
        else
            sender.sendMessage("§cErreur lors de l'insertion en base.")

        return true
    }

    // Tab-complétion : propose les régions WorldGuard du monde du joueur
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (sender !is Player) return emptyList()
        val rm = WorldGuard.getInstance().platform.regionContainer
            .get(BukkitAdapter.adapt(sender.world))
        return rm?.regions?.keys
            ?.filter { it != "__global__" && it.startsWith(args.getOrElse(0) { "" }.lowercase()) }
            ?: emptyList()
    }
}