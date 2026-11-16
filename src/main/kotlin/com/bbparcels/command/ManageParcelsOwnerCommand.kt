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

class AddMemberCommand(
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache,
    private val memberCache: MemberCache
    ): SubCommand {
    
    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val rentalRepo = RentalRepository(plugin.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(plugin.databaseManager)
    private val profileRepo = ProfileRepository(db = plugin.databaseManager)

    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /myparcel <label> add <profileId>")
            return true
        }
        
        val label = args[0]
        val profileId = args[1].toIntOrNull() ?: run {
            sender.sendMessage("§cUsage : /myparcel <label> add <profileId>")
            return true
        }
        
        val onlineAccounts = Bukkit.getOnlinePlayers().map { it.uniqueId.toString() }.toSet()
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val senderProfile = profileRepo.findActiveProfile(sender.uniqueId.toString())
            val toAddProfile  = profileRepo.findById(profileId)
            val isPlayed = toAddProfile != null && toAddProfile.accountUuid in onlineAccounts &&
                profileRepo.findActiveProfile(toAddProfile.accountUuid)?.id == toAddProfile.id
            val parcel = senderProfile?.let {
                parcelRepo.findAllOwnerParcels(senderProfile.id)
                .find { it.label == label }
            }
            val rental = parcel?.let {rentalRepo.findActiveByParcelId(parcel.id)}
            val memberIds = parcel?.let {rentalMemberRepo.findMembers(it.id)}
            val isIn =  memberIds?.contains(toAddProfile) == true
            val added = when {
                parcel == null || toAddProfile == null || !isPlayed || rental == null || isIn -> false
                else -> {
                    rentalMemberRepo.addMember(
                         RentalMember(
                            rentalId = rental.id,
                            memberProfileId = toAddProfile.id,
                            role = RentalMemberRole.MEMBER
                        )
                    )
                }
            }
            
            val message = when {
                toAddProfile == null -> "§cCe joueur n'existe pas."
                parcel == null -> "§cVous n'avez pas cette parcelle."
                rental == null -> "§cCette parcelle n'a pas de location active."
                !isPlayed -> "§c Le joueur §e${toAddProfile.displayName} §cn'est pas connecté."
                isIn -> "§cCe joueur fait déjà partie de la parcelle."
                !added -> "§cErreur lors de l'ajout du joueur."
                else -> "§a✔ Le joueur §e${toAddProfile.displayName} §aa été ajouté à la parcelle §e$label §a!"
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {  
                if (added && toAddProfile != null && parcel != null) {
                    Bukkit.getWorld(parcel.world)?.let {world ->
                        WorldGuard.getInstance().platform.regionContainer
                        .get(BukkitAdapter.adapt(world))
                        ?.getRegion(parcel.regionId)
                        ?.members?.addPlayer(toAddProfile.accountId)
                    }
                    memberCache.onMemberAdded(label, toAddProfile)
                    Bukkit.getPlayer(toAddProfile.accountId)?.sendMessage("§aVous avez été ajouté à la parcelle §e${parcel.label}§a.")
                }
                sender.sendMessage(message)
            })
        })
        
        return true
    }

    // override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
    //     return if (sender is Player) when (args.size) {
    //         1 -> parcelCache.parcelsByOwner(sender.uniqueId.toString(), args[0])
    //         2 -> listOf("add").filter {it.startsWith(args[1], ignoreCase = true)}
    //         3 -> memberCache.membersNotOf(args[0], sender, args[2])
    //         else -> emptyList()
    //     } else emptyList()
    // }
}


class RemoveMemberCommand(
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache,
    private val memberCache: MemberCache
    ) : SubCommand {

    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(plugin.databaseManager)
    private val profileRepo = ProfileRepository(plugin.databaseManager)

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /myparcel <label> remove <profileId>")
            return true
        }

        val label = args[0]
        val profileId = args[1].toIntOrNull() ?: run {
            sender.sendMessage("§cUsage : /myparcel <label> remove <profileId>")
            return true
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val senderProfile = profileRepo.findActiveProfile(sender.uniqueId.toString())
            val profileToRemove = profileRepo.findById(profileId)
            val parcel = senderProfile?.let {
                parcelRepo.findAllOwnerParcels(senderProfile.id)
                .find {it.label == label}
            }
            val memberIds = parcel?.let {rentalMemberRepo.findMembers(it.id)}
            val isIn =  memberIds?.contains(profileToRemove) == true
            val deleted = when {
                parcel == null || profileToRemove == null || profileToRemove == senderProfile -> false
                else -> rentalMemberRepo.deleteMember(parcel.label, profileToRemove.id)
            }
            val message = when {
                parcel == null -> "§cVous n'avez pas cette parcelle."
                profileToRemove == null -> "§cJoueur introuvable."
                profileToRemove == senderProfile -> "§cVous ne pouvez pas vous retirer de votre propre parcelle."
                !isIn -> "§cCe joueur ne fait pas partie de la parcelle."
                !deleted -> "§cErreur lors de la suppression."
                else -> "§a✔ §e${profileToRemove.displayName} §aa été retiré de la parcelle §e$label§a."
            }
            
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (deleted && profileToRemove != null && parcel != null) {
                    Bukkit.getWorld(parcel.world)?.let {world ->
                        WorldGuard.getInstance().platform.regionContainer
                            .get(BukkitAdapter.adapt(world))
                            ?.getRegion(parcel.regionId)
                            ?.members?.removePlayer(profileToRemove.accountId)
                    }
                    memberCache.onMemberRemoved(label, profileToRemove.id)
                    Bukkit.getPlayer(profileToRemove.accountId)?.sendMessage("§cVous avez été retiré de la parcelle §e${parcel.label}§c.")
                }
                sender.sendMessage(message)
            })
        })
        return true
    }

    // override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
    //     return if (sender is Player) when (args.size) {
        //     1 -> parcelCache.parcelsByOwner(sender.uniqueId.toString(), args[0])
        //     2 -> listOf("remove").filter {it.startsWith(args[1], ignoreCase = true)}
        //     3 -> memberCache.membersOf(args[0], sender, args[2])
        //     else -> emptyList()
        // } else emptyList()
    // }
}

class ListMemberCommand(private val plugin: BbParcels) : SubCommand {

    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(plugin.databaseManager)
    private val profileRepo = ProfileRepository(plugin.databaseManager)

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
            val senderProfile = profileRepo.findActiveProfile(sender.uniqueId.toString())
            val parcel = senderProfile?.let {
                parcelRepo.findAllOwnerParcels(senderProfile.id)
                .find { it.label == label }
            }
            val membersIds = parcel?.let {rentalMemberRepo.findMembers(it.id)}
            val lines = when {
                parcel == null -> listOf("§cVous n'avez pas cette parcelle.")
                membersIds.isNullOrEmpty() -> listOf("§7Aucun membre sur la parcelle §e$label§7.")
                else -> { listOf("§6§lMembres de §e$label §6§l(${membersIds.size}) :") +
                    membersIds.map {profil ->
                        "§7- §e${profil.displayName}"
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