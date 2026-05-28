package com.bbparcels.command.admin

import com.bbparcels.BbParcels
import com.bbparcels.repository.ParcelRepository
import org.bukkit.command.CommandSender

class DeleteParcelCommand(plugin: BbParcels) : SubAdminCommand {

    private val repo = ParcelRepository(plugin.databaseManager)

    // /parceladmin delete <id>
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        val id = args.getOrNull(0)?.toIntOrNull()
        if (id == null) {
            sender.sendMessage("§cUsage : /parceladmin delete <id>")
            return true
        }

        val parcel = repo.findById(id)
        if (parcel == null) {
            sender.sendMessage("§cAucune parcelle trouvée avec l'id §e$id§c.")
            return true
        }

        if (parcel.status.value == "rented") {
            sender.sendMessage("§cImpossible de supprimer §e${parcel.label}§c : une location est en cours.")
            return true
        }

        val deleted = repo.deleteById(id)
        if (deleted)
            sender.sendMessage("§a✔ Parcelle §e${parcel.label} §a(id: $id) supprimée.")
        else
            sender.sendMessage("§cErreur lors de la suppression.")

        return true
    }

    // Tab-complétion : propose les IDs des parcelles disponibles/désactivées
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val prefix = args.getOrElse(0) { "" }
        return repo.findAll()
            .filter { it.status.value != "rented" }
            .map { "${it.id}" }
            .filter { it.startsWith(prefix) }
    }
}