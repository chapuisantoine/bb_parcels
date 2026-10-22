package com.bbparcels.command

import com.bbparcels.BbParcels
import com.bbparcels.cache.MemberCache
import com.bbparcels.cache.ParcelCache
import com.bbparcels.database.repository.*
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class AddMemberCommand(
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache,
    private val memberCache: MemberCache
    ): SubCommand {
    
    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val rentalRepo = RentalRepository(plugin.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(plugin.databaseManager)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /myparcel <label> add <playerName>")
            return true
        }
        
        val label = args[0]
        val toAddPlayerName = args[1]
        val toAddPlayer = Bukkit.getPlayer(toAddPlayerName)
        
        if (toAddPlayer == null){
            sender.sendMessage("§cCe joueur n'existe pas.")
            return true
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val ownerParcels = parcelRepo.findAllOwnerParcels(sender.uniqueId.toString())
            val parcel = ownerParcels.find {it.label == label}
            val rental = parcel?.let {rentalRepo.findActiveByParcelId(parcel.id)}
            val membersUuid = parcel?.let {rentalMemberRepo.findMembers(it.id)}
            val isIn =  membersUuid?.contains(toAddPlayer.uniqueId.toString()) == true
            val added = when {
                parcel == null || rental == null || isIn -> false
                else -> {
                    rentalMemberRepo.addMember(
                         RentalMember(
                            rentalId = rental.id,
                            memberUuid = toAddPlayer.uniqueId.toString(),
                            role = RentalMemberRole.MEMBER
                        )
                    )
                }
            }
            
            val message = when {
                parcel == null -> "§cVous n'avez pas cette parcelle."
                rental == null -> "§cCette parcelle n'a pas de location active."
                isIn -> "§cCe joueur fait déjà partie de la parcelle."
                !added -> "§cErreur lors de l'ajout du joueur."
                else -> "§a✔ Le joueur §e${toAddPlayer.name} §aa été ajouté à la parcelle §e$label §a!"
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {

                if (added && parcel != null) {
                    val playerUuid = toAddPlayer.uniqueId
                    Bukkit.getWorld(parcel.world)?.let {world ->
                        WorldGuard.getInstance().platform.regionContainer
                        .get(BukkitAdapter.adapt(world))
                        ?.getRegion(parcel.regionId)
                        ?.members?.addPlayer(playerUuid)
                    }
                    memberCache.onMemberAdded(label, playerUuid.toString())
                    Bukkit.getPlayer(playerUuid)?.sendMessage("§aVous avez été ajouté à la parcelle §e${parcel.label}§a.")
                }
                sender.sendMessage(message)
            })
        })
        
        return true
    }
    

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (sender is Player) when (args.size) {
            1 -> parcelCache.parcelsByOwner(sender.uniqueId.toString(), args[0])
            2 -> listOf("add").filter {it.startsWith(args[1], ignoreCase = true)}
            3 -> memberCache.membersNotOf(args[0], sender, args[2])
            else -> emptyList()
        } else emptyList()
    }
}


class RemoveMemberCommand(
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache,
    private val memberCache: MemberCache
    ) : SubCommand {

    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(plugin.databaseManager)

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /myparcel <label> remove <playerName>")
            return true
        }

        val label    = args[0]
        val playerName     = args[1]
        val senderUuid = sender.uniqueId.toString()
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val parcel = parcelRepo.findAllOwnerParcels(senderUuid)
                .find { it.label.equals(label, ignoreCase = true) }
            val targetUuid = parcel?.let {p ->
                rentalMemberRepo.findMembers(p.id).firstOrNull {uuid ->
                    Bukkit.getOfflinePlayer(UUID.fromString(uuid)).name.equals(playerName, ignoreCase = true)
                }
            }
            val deleted = when {
                parcel == null || targetUuid == null || targetUuid == senderUuid -> false
                else -> rentalMemberRepo.deleteMember(parcel.label, targetUuid)
            }
            val message = when {
                parcel == null -> "§cVous n'avez pas cette parcelle."
                targetUuid == null -> "§c§e$playerName §cn'est pas membre de cette parcelle."
                targetUuid == senderUuid -> "§cVous ne pouvez pas vous retirer de votre propre parcelle."
                !deleted -> "§cErreur lors de la suppression."
                else -> "§a✔ §e$playerName §aa été retiré de la parcelle §e$label§a."
            }
            
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (deleted && parcel != null) {
                    val playerUuid = UUID.fromString(targetUuid)
                    Bukkit.getWorld(parcel.world)?.let {world ->
                        WorldGuard.getInstance().platform.regionContainer
                            .get(BukkitAdapter.adapt(world))
                            ?.getRegion(parcel.regionId)
                            ?.members?.removePlayer(playerUuid)
                    }
                    memberCache.onMemberRemoved(label, playerUuid.toString())
                    Bukkit.getPlayer(playerUuid)?.sendMessage("§cVous avez été retiré de la parcelle §e${parcel.label}§c.")
                }
                sender.sendMessage(message)
            })
        })
        return true
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (sender is Player) when (args.size) {
            1 -> parcelCache.parcelsByOwner(sender.uniqueId.toString(), args[0])
            2 -> listOf("remove").filter {it.startsWith(args[1], ignoreCase = true)}
            3 -> memberCache.membersOf(args[0], sender, args[2])
            else -> emptyList()
        } else emptyList()
    }
}

class ListMemberCommand(private val plugin: BbParcels) : SubCommand {

    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(plugin.databaseManager)

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /myparcel <label> list")
            return true
        }

        val label = args[0]
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val ownerParcels = parcelRepo.findAllOwnerParcels(sender.uniqueId.toString())
            val parcel = ownerParcels.find { it.label == label }
            val membersUuid = parcel?.let {rentalMemberRepo.findMembers(it.id)}
            val lines = when {
                parcel == null -> listOf("§cVous n'avez pas cette parcelle.")
                membersUuid.isNullOrEmpty() -> listOf("§7Aucun membre sur la parcelle §e$label§7.")
                else -> { listOf("§6§lMembres de §e$label §6§l(${membersUuid.size}) :") +
                    membersUuid.map {uuid ->
                        val name = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).name
                        "§7- §e$name"
                    }
                }
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                lines.forEach{sender.sendMessage(it)}
            })
        })

        return true
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}