package com.bbparcels.command

import com.bbparcels.BbParcels
import com.bbparcels.cache.ActiveProfileCache
import com.bbparcels.cache.MemberCache
import com.bbparcels.cache.ParcelCache
import com.bbparcels.database.repository.Parcel
import com.bbparcels.database.repository.ParcelRepository
import com.bbparcels.database.repository.ParcelStatus
import com.bbparcels.gui.agent.AgentTabletListener
import com.bbparcels.gui.owner.OwnerGuiListener
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AddParcelCommand(
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache
    ) : SubCommand {

    private val parcelRepo = ParcelRepository(plugin.databaseManager)

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.size < 3) {
            sender.sendMessage("§cUsage : /parceladmin add <region_id> <label> <weeklyPrice> [description...]")
            return true
        }

        val regionId = args[0].lowercase()
        val label = args[1]
        val weeklyPrice = args[2].toBigDecimalOrNull()
        if (weeklyPrice == null || weeklyPrice <= 0.toBigDecimal() ) {
            sender.sendMessage("§cEntrez un prix correct.")
            return true
        }
        
        val description = if (args.size > 3) args.drop(3).joinToString(" ") else null
        val world = sender.world

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val regionManager = WorldGuard.getInstance()
                .platform.regionContainer
                .get(BukkitAdapter.adapt(world))
            val region = regionManager?.getRegion(regionId)
            val alreadyRegionExists = parcelRepo.existsByRegion(regionId, world.name)
            val alreadyParcelExists = parcelRepo.existsByLabel(label)
            
            val added = when {
                regionManager == null || region == null || alreadyRegionExists || alreadyParcelExists -> -1
                else -> {
                    parcelRepo.addParcel(
                        Parcel(
                            regionId = regionId,
                            world = world.name,
                            label = label,
                            weeklyPrice = weeklyPrice,
                            description = description
                        )
                    ) 
                }
            }
            
            val message = when {
                regionManager == null -> "§cImpossible de charger les régions du monde §e${world.name}§c."
                region == null -> "§cLa région §e$regionId §cn'existe pas dans §e${world.name}§c."
                alreadyRegionExists -> "§cCette région est déjà enregistrée en base."
                alreadyParcelExists -> "§cCe nom de parcelle est déjà utilisé dans ce serveur."
                added < 0 -> "§cErreur lors de l'ajout."
                else -> "§a✔ Parcelle §e$label §aajoutée ! §7(région: $regionId | id: $added)"   
            }
            
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (added > 0)
                    parcelCache.onParcelAdded(label)
                sender.sendMessage(message)
            })
            
        })

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


class FindParcelCommand(
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache
    ) : SubCommand {
    
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
        
        val label = args[0]
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val parcel = parcelRepo.findByLabel(label)
            
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!sender.isOnline) return@Runnable
                
                val world = parcel?.let {Bukkit.getWorld(parcel.world)}
                val region = if (parcel != null && world != null) {
                    WorldGuard.getInstance()
                        .platform.regionContainer
                        .get(BukkitAdapter.adapt(world))?.getRegion((parcel.regionId))
                } else null
                
                
                val message: BaseComponent = when {
                    parcel == null -> TextComponent("§cParcelle non trouvée.")
                    world == null -> TextComponent("§cMonde de la parcelle ${parcel.world} non trouvée.")
                    region == null -> TextComponent("§cRégion non trouvée.")
                    else -> {
                        val min = region.minimumPoint
                        val max = region.maximumPoint
                        val x = (min.x + max.x) / 2
                        val z = min.z - 2
                        val y = world.getHighestBlockYAt(x, z) + 1
                        val coords = "§e($x, $y, $z)"
                        TextComponent("§7Parcelle §a${parcel.label} §7→ $coords").apply {
                            clickEvent = ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/execute in ${world.key} run tp @s ${x + 0.5} $y ${z + 0.5} 0 0"
                            )
                            hoverEvent = HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text("§eCliquez pour vous téléporter devant la parcelle")
                            )
                        }
                    }
                }
                sender.spigot().sendMessage(message)
            })
        })
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (args.size == 1) parcelCache.parcelLabels(args[0]) else emptyList()
    }
}

class ChangeParcelStatusCommand(
    private val plugin: BbParcels,
    private val from: ParcelStatus,
    private val to: ParcelStatus,
    private val parcelCache: ParcelCache
    ): SubCommand {
    
    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    private val subCommand = if (to == ParcelStatus.DISABLED) "disable" else "enable"
    private val doneLabel  = if (to == ParcelStatus.DISABLED) "désactivée" else "réactivée"
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /parceladmin $subCommand <label>")
            return true
        }

        val label = args[0]
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val parcel = parcelRepo.findByLabel(label)            
            val changed = parcel?.let {parcelRepo.changeStatus(parcel.id, from, to) } ?: false
            val message = when {
                parcel == null -> "§cPas de parcelle à ce nom."
                parcel.status == to -> "§cCette parcelle a déjà ce status."
                parcel.status == ParcelStatus.RENTED -> "§cLa parcelle §e${parcel.label} §cest louée : résiliez la location d'abord."
                !changed -> "§cLe status de la parcelle n'a pas pu être changé."
                else -> "§a✔ Parcelle §e${parcel.label} §a$doneLabel."
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (changed)
                    parcelCache.onParcelStatusChanged(label, to)
                sender.sendMessage(message)
            })
        })
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (args.size == 1) parcelCache.parcelsLabelsOfStatus(from, args[0]) else emptyList()
    }
}


class DeleteParcelCommand(
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache
    ) : SubCommand {

    private val parcelRepo = ParcelRepository(plugin.databaseManager)

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /parceladmin delete <label>")
            return true
        }

        val label = args[0]
        if (!args.getOrNull(1).equals("confirm", ignoreCase = true)) {
            sender.sendMessage("§6⚠ La parcelle §e$label §6et tout son historique de locations seront supprimés définitivement.")
            sender.sendMessage("§6Pour confirmer : §e/parceladmin delete $label confirm")
            sender.sendMessage("§7(Pour la retirer temporairement : §e/parceladmin disable $label§7)")
            return true
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val parcel = parcelRepo.findByLabel(label)
            val deleted = when {
                parcel == null || parcel.status == ParcelStatus.RENTED -> false
                else -> parcelRepo.deleteById(parcel.id)
            }            
            val message = when {
                parcel == null -> "§cAucune parcelle trouvée avec l'id §e$label§c."
                parcel.status == ParcelStatus.RENTED -> "§cImpossible de supprimer §e${parcel.label}§c : une location est en cours."
                !deleted -> "§cErreur lors de la suppression."
                else -> "§a✔ Parcelle §e${parcel.label} supprimée."
            }
            
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (deleted)
                    parcelCache.onParcelRemoved(label)
                sender.sendMessage(message)
            })
        })

        return true
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> parcelCache.parcelsLabelsNotOfStatus(ParcelStatus.RENTED, args[0])
            2 -> listOf("confirm").filter { it.startsWith(args[1], ignoreCase = true) }
            else -> emptyList()
        }
    }
}

class GiveTabletCommand : SubCommand {

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /parceladmin givetablet <playerName>")
            return true
        }
        
        val playerName = args[0]
        val target = Bukkit.getPlayer(playerName)
        if (target == null) {
            sender.sendMessage("§cJoueur introuvable.")
            return true
        } else {
            target.inventory.addItem(AgentTabletListener.createTablet())
                .values.forEach { target.world.dropItemNaturally(target.location, it) }
            sender.sendMessage("§aTablette donnée à §e${target.name}§a.")
            target.sendMessage("§aVous avez reçu la §6Tablette Agent Immo§a.")
            return true
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size == 1)
            return Bukkit.getOnlinePlayers().map {it.name}.filter {it.startsWith(args[0], ignoreCase = true)}
        return emptyList()
    }
}

class GiveParcelKeyCommand(
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache
    ) : SubCommand {
    
    private val parcelRepo = ParcelRepository(plugin.databaseManager)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /parceladmin givekey <label> <playerName>")
            return true
        }
        
        val label = args[0]
        val playerName = args[1]
        val target = Bukkit.getPlayer(playerName)
        if (target == null) {
            sender.sendMessage("§cJoueur introuvable.")
            return true
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val parcel = parcelRepo.findByLabelWithOwner(label)
            
            Bukkit.getScheduler().runTask(plugin, Runnable {
                val message = when {
                    parcel == null -> "§cPas de parcelle à ce nom."
                    parcel.status != ParcelStatus.RENTED -> "§cCette parcelle n'est pas louée ou pas disponible."
                    else -> {
                        target.inventory.addItem(OwnerGuiListener.createParcelKey(parcel))
                            .values.forEach { target.world.dropItemNaturally(target.location, it) }
                        target.sendMessage("§aVous avez reçu la §6Clé - ${parcel.label}§a.")
                        "§aClé de la parcelle §e${parcel.label} §adonnée à §e${target.name}§a."
                    } 
                }
                sender.sendMessage(message)
            })
            
        })
        return true
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> parcelCache.parcelsLabelsOfStatus(ParcelStatus.RENTED, args[0])
            2 -> Bukkit.getOnlinePlayers()
                    .map { it.name }
                    .filter { it.startsWith(args[1], ignoreCase = true) }
            else -> emptyList()
        }
    }
}

class RefreshCacheCommand(
    private val plugin: BbParcels,
    private val parcelCache: ParcelCache,
    private val memberCache: MemberCache,
    private val activeProfileCache: ActiveProfileCache
    ) : SubCommand {

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.isNotEmpty()) {
            sender.sendMessage("§cUsage : /parceladmin refreshcache")
            return true
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            parcelCache.load()
            memberCache.load()
            activeProfileCache.refreshAllOnline()
            
            
            Bukkit.getScheduler().runTask(plugin, Runnable {
                sender.sendMessage("§a✔ Cache bien rechargé depuis la base.")
            })
        })
        return true
    }
}