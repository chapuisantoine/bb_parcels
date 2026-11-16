package com.bbparcels.command

import com.bbparcels.BbParcels
import com.bbparcels.cache.ActiveProfileCache
import com.bbparcels.cache.MemberCache
import com.bbparcels.cache.ParcelCache
import com.bbparcels.database.repository.*
import com.bbparcels.gui.owner.OwnerGuiListener
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.LocalDateTime

class InfoParcelCommand(
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache
    ) : SubCommand {
    
    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val profileRepo = ProfileRepository(plugin.databaseManager)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /parcelagent info <label>")
            return true
        }
        
        val label = args[0]


        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val parcel = parcelRepo.findByLabelWithOwner(label)
            
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val message = when {
                    parcel == null -> "§cCette parcelle n'existe pas ou n'a pas été trouvé."
                    else -> "§aParcel : §e$label §a| regionId : §e${parcel.regionId} §a| Monde : §e${parcel.world}" + 
                            "§a| Status : §e${parcel.status} §a| Owner §e${parcel.tenantProfile?.displayName ?: "aucun"}"
                }
                sender.sendMessage(message)
            })
        })
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (args.size == 1) parcelCache.parcelLabels(args[0]) else emptyList()
    }
}



class ListParcelCommand(private val plugin: BbParcels) : SubCommand {
    
    private val repo = ParcelRepository(plugin.databaseManager)
    private val profileRepo = ProfileRepository(plugin.databaseManager)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.isNotEmpty()) {
            sender.sendMessage("§cUsage : /parcelagent list")
            return true
        }
        
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val parcels = repo.findAllWithOwners()
            
            val lines = when {
                parcels.isEmpty() -> listOf("§cAucune parcel existante.")
                else -> listOf("§aListe des parcelles :") +
                    parcels.map {parcel -> 
                        "§aParcel : §e${parcel.label} §a| Status : §e${parcel.status} §a| Owner : §e${parcel.tenantProfile?.displayName ?: "aucun"}"
                    }
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                lines.forEach{sender.sendMessage(it)}
            })
        })
                    
        return true
    } 
}

class RentParcelCommand (
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache,
    private val memberCache: MemberCache,
    private val activeProfileCache: ActiveProfileCache
    ) : SubCommand {

    private val parcelRepo = ParcelRepository(db = plugin.databaseManager)
    private val rentalRepo = RentalRepository(db = plugin.databaseManager)
    private val profileRepo = ProfileRepository(db = plugin.databaseManager)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /parcelagent rent <label> <profileId>")
            return true
        }
        
        val label = args[0]
        val profileId = args[1].toIntOrNull() ?: run {
            sender.sendMessage("§cUsage : /parcelagent rent <label> <profileId>")
            return true
        }
        val startDate = LocalDateTime.now()
        
        val onlineAccounts = Bukkit.getOnlinePlayers().map { it.uniqueId.toString() }.toSet()
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val profile = profileRepo.findById(profileId)
            val isPlayed = profile != null && profile.accountUuid in onlineAccounts &&
                profileRepo.findActiveProfile(profile.accountUuid)?.id == profile.id
            val parcel = parcelRepo.findByLabelWithOwner(label)
            
            val rentalId = if (profile != null && isPlayed && parcel != null && parcel.status == ParcelStatus.AVAILABLE) {
                rentalRepo.rentWithMember(
                    Rental(parcelId = parcel.id, tenantProfileId = profile.id, startDate = startDate),
                    RentalMember(rentalId = 0, memberProfileId = profile.id, role = RentalMemberRole.OWNER)
                )
            } else 0
                       
            Bukkit.getScheduler().runTask(plugin, Runnable {           
                val message = when {
                    profile == null -> "§cCette personne n'existe pas."
                    !isPlayed -> "§c Le joueur §e${profile.displayName} §cn'est pas connecté."
                    parcel == null -> "§cLa parcelle §e$label §cn'existe pas."
                    rentalId == 0 -> "§cLa parcelle §e$label §cn'est pas disponible."
                    rentalId < 0 -> "§cErreur lors de la location."
                    else -> {
                        parcelCache.onParcelRented(label, profile.id)
                        memberCache.onMemberAdded(label, profile)
                        
                        val regionOk = Bukkit.getWorld(parcel.world)?.let {world ->
                            WorldGuard.getInstance().platform.regionContainer
                            .get(BukkitAdapter.adapt(world))
                            ?.getRegion(parcel.regionId)
                            ?.members?.addPlayer(profile.accountId)
                        } != null
                        
                        Bukkit.getPlayer(profile.accountId)
                            ?.sendMessage("§aVous êtes maintenant locataire de la parcelle §e$label §a!")
                        
                        sender.inventory.addItem(OwnerGuiListener.createParcelKey(parcel))
                            .values.forEach { sender.world.dropItemNaturally(sender.location, it) }
                        if (regionOk) {
                            "§a✔ §e${profile.displayName} §aest maintenant locataire de la parcelle §e$label §a!"
                        }
                        else {
                            "§6⚠ Location enregistrée, mais la région WorldGuard §e${parcel.regionId} §6est introuvable."
                        }
                    }
                }
                sender.sendMessage(message)
            })
        })
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> parcelCache.parcelsLabelsOfStatus(ParcelStatus.AVAILABLE, args[0])                    
            2 -> Bukkit.getOnlinePlayers()
                    .mapNotNull { activeProfileCache.get(it.uniqueId) }
                    .map { it.id.toString() }
                    .filter { it.startsWith(args[1]) }
            else -> emptyList()
        }
    }
}

class UnrentParcelCommand(
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache,
    private val memberCache: MemberCache
    ) : SubCommand {

    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val rentalRepo = RentalRepository(plugin.databaseManager)

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /parcelagent unrent <label>")
            return true
        }

        val label = args[0]
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val parcel = parcelRepo.findByLabel(label)
            val rental = parcel?.takeIf { it.status == ParcelStatus.RENTED}
                ?.let { rentalRepo.findActiveByParcelId(it.id)}
            val removedMembers = if (parcel != null && rental != null)
                rentalRepo.terminateWithMembers(rental.id, parcel.id)
            else null
            
            val message = when {
                parcel == null -> "§cLa parcelle §e$label §cn'existe pas."
                parcel.status != ParcelStatus.RENTED -> "§cLa parcelle §e$label §cn'est pas en cours de location."
                rental == null -> "§cAucune location active trouvée pour §e$label§c."
                removedMembers == null -> "§cErreur lors de la résiliation (déjà résiliée ?)."
                else -> "§a✔ La location de §e$label §aa été résiliée."
            }
            
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (parcel != null && removedMembers != null) {
                    memberCache.onRentalEnded(label)
                    parcelCache.onParcelUnrented(label)
                    Bukkit.getWorld(parcel.world)?.let {world ->
                        val region = WorldGuard.getInstance()
                            .platform.regionContainer
                            .get(BukkitAdapter.adapt(world))
                            ?.getRegion(parcel.regionId)
                            if (region == null)
                                plugin.logger.warning("Région WorldGuard '${parcel.regionId}' introuvable lors du unrent !")
                            else
                                removedMembers.forEach { region.members.removePlayer(it.accountId) }
                        }
                        removedMembers.forEach {
                            Bukkit.getPlayer(it.accountId)?.sendMessage("§cLa location de la parcelle §e$label §ca été résiliée.")
                        }
                    }
                    sender.sendMessage(message)
                })
            })
            return true
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (args.size == 1) parcelCache.parcelsLabelsOfStatus(ParcelStatus.RENTED, args[0]) else emptyList()
    }
}
