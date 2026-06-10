package com.bbparcels.command.agent
import com.bbparcels.BbParcels
import com.bbparcels.model.ParcelStatus
import com.bbparcels.model.Rental
import com.bbparcels.model.RentalMember
import com.bbparcels.model.RentalMemberRole
import com.bbparcels.repository.ParcelRepository
import com.bbparcels.repository.RentalRepository
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.LocalDateTime

class RentParcelCommand (private val plugin: BbParcels) : SubAgentCommand {

    private val parcelRepo = ParcelRepository(db = plugin.databaseManager)
    private val rentalRepo = RentalRepository(db = plugin.databaseManager)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /parcelagent rent <parcelLabel> <playerName> [description...]")
            return true
        }
        
        val parcelName = args[0]
        val playerName = args[1]
        val startDate = LocalDateTime.now()
        
        val player = Bukkit.getPlayer(playerName)
        if (player == null) {
            sender.sendMessage("§c Le joueur §e$playerName §cn'est pas connecté.")
            return true
        }
        
        val parcel = parcelRepo.findByLabel(parcelName)
        if (parcel == null) {
            sender.sendMessage("§c La parcelle §e$parcelName §cn'existe pas.")
            return true
        }
        
        if (parcel.status != ParcelStatus.AVAILABLE) {
            sender.sendMessage("§cLa parcelle §e$parcelName §cn'est pas disponible.")
            return true
        }
        
        
        val newIdRental = try {
            rentalRepo.rentWithMember(
                Rental(
                    parcelId   = parcel.id,
                    tenantUuid = player.uniqueId.toString(),
                    startDate  = startDate
                ),
                RentalMember(
                    rentalId   = 0,
                    memberUuid = player.uniqueId.toString(),
                    role       = RentalMemberRole.OWNER
                )
            )
        } catch (e: Exception) {
            plugin.logger.severe("Erreur lors de la création de la location : ${e.message}")
            sender.sendMessage("§cErreur lors de l'insertion en base.")
            return true
        }

        if (newIdRental > 0) {
            parcelRepo.updateParcelStatus(parcel.id, ParcelStatus.RENTED)

            val world = Bukkit.getWorld(parcel.world)
            if (world != null) {
                val regionManager = WorldGuard.getInstance()
                    .platform.regionContainer
                    .get(BukkitAdapter.adapt(world))

                val region = regionManager?.getRegion(parcel.regionId)
                if (region != null) {
                    region.members.addPlayer(player.uniqueId)
                    regionManager.save()
                } else {
                    plugin.logger.warning("Région WorldGuard '${parcel.regionId}' introuvable !")
                }
            }

            sender.sendMessage("§a✔ §e$playerName §aest maintenant locataire de la Parcelle §e$parcelName §a!")
        }
        else
            sender.sendMessage("§cErreur lors de la location.")
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> parcelRepo.findAll()
                    .filter { it.status == ParcelStatus.AVAILABLE }
                    .map { it.label }
                    .filter { it.startsWith(args[0], ignoreCase = true) }
                    
            2 -> Bukkit.getOnlinePlayers()
                    .map { it.name }
                    .filter { it.startsWith(args[1], ignoreCase = true) }

            else -> emptyList()
        }
    }
}