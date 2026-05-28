package com.bbparcels.command.owner

import com.bbparcels.BbParcels
import com.bbparcels.repository.ParcelRepository
import com.bbparcels.repository.RentalMemberRepository
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class ListMemberCommand(private val plugin: BbParcels) : SubOwnerCommand {

    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(plugin.databaseManager)

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /myparcel <parcelLabel> list")
            return true
        }

        val parcelLabel = args[0]
        val ownerParcels = parcelRepo.findAllOwnerParcels(sender.uniqueId.toString())
        val parcel = ownerParcels.find { it.label == parcelLabel }

        if (parcel == null) {
            sender.sendMessage("§cVous n'avez pas cette parcelle.")
            return true
        }

        val membersUuid = rentalMemberRepo.findMembers(parcelLabel)

        if (membersUuid.isEmpty()) {
            sender.sendMessage("§7Aucun membre sur la parcelle §e$parcelLabel§7.")
            return true
        }

        sender.sendMessage("§6§lMembres de §e$parcelLabel §6§l(${membersUuid.size}) :")
        membersUuid.forEach { uuid ->
            val name = Bukkit.getPlayer(UUID.fromString(uuid))?.name ?: uuid
            sender.sendMessage("$§7- §e$name")
        }

        return true
    }

    // pas d'arg supplémentaire après "list"
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}