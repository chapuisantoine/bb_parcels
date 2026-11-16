package com.bbparcels.gui.agent

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.ParcelStatus
import com.bbparcels.database.repository.ParcelWithOwner
import com.bbparcels.database.repository.ProfileInfo
import com.bbparcels.database.repository.RentalMemberRepository
import com.bbparcels.gui.BbGui
import com.bbparcels.gui.owner.OwnerGuiListener
import com.bbparcels.gui.utils.Utils
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ParcelDetailGui private constructor(
    private val parcel: ParcelWithOwner,
    members: List<ProfileInfo>
) : BbGui("[BB] Détails de la parcelle"){
    
    override val permission = "bbparcels.agentImmo"
        
    init {
        Utils.fillBorders(inventory)
        inventory.setItem(SLOT_INFO_PARCEL, buildInfoParcelItem(parcel))
        inventory.setItem(SLOT_INFO_MEMBERS, Utils.buildListMembersItem(members))
        
        when(parcel.status) {
            ParcelStatus.AVAILABLE -> 
                inventory.setItem(SLOT_RENT_UNRENT, Utils.buildNavItem(Material.GREEN_CONCRETE, "§eChoisir à qui louer"))
            ParcelStatus.RENTED -> {
                inventory.setItem(SLOT_RENT_UNRENT, Utils.buildNavItem(Material.ORANGE_CONCRETE, "§eRésilier la location"))
                inventory.setItem(SLOT_GENERATE_KEY, Utils.buildNavItem(Material.TRIPWIRE_HOOK, "§eCréer une clé"))
            }
            ParcelStatus.DISABLED ->
                inventory.setItem(SLOT_RENT_UNRENT, Utils.buildNavItem(Material.GRAY_CONCRETE, "§eCette parcelle ne peut pas être louée"))
        }
        inventory.setItem(SLOT_RETURN, Utils.buildNavItem(Material.BARRIER, "§7Retour"))
    }
    
    override fun onClick(player: Player, slot: Int, item: ItemStack) {
        when (slot) {
            SLOT_RETURN -> AgentMenuGui.open(player)
            SLOT_GENERATE_KEY -> if (parcel.status == ParcelStatus.RENTED) {
                player.inventory.addItem(OwnerGuiListener.createParcelKey(parcel))
                    .values.forEach { player.world.dropItemNaturally(player.location, it) }
                player.sendMessage("§aVous avez reçu une clé de la parcelle §e${parcel.label}§a.")
            }
            SLOT_RENT_UNRENT -> when (parcel.status) {
                ParcelStatus.AVAILABLE -> ChooseTenantGui.open(player, parcel)
                ParcelStatus.RENTED -> ConfirmUnrentGui(parcel).open(player)
                ParcelStatus.DISABLED -> {}
            }
        }
    }
    
    private fun buildInfoParcelItem(parcel: ParcelWithOwner): ItemStack {
        val material = Material.PAPER
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        
        meta.setDisplayName("§fParcelle §a${parcel.label}§f :")
        
        meta.lore = buildList {
            add("§7Statut de la parcelle : §e${Utils.statusLabel(parcel.status)}") 
            
            if (parcel.tenantProfile != null) 
                add("§7Locataire : §e${parcel.tenantProfile.displayName}")
             else 
                add("§7Locataire : §ePas de locataire")
            
            val world = Bukkit.getWorld(parcel.world)
            if (world != null){
                add("§7Monde : §e${world.name}")
                val regionManager = WorldGuard.getInstance()
                    .platform.regionContainer
                    .get(BukkitAdapter.adapt(world))
                val region = regionManager?.getRegion(parcel.regionId)
                if (region != null)
                    add("§7Région : §e${region.id}")
            }
            
            add("§7Prix de la location : §e${parcel.weeklyPrice}")
        }
        
        item.itemMeta = meta
        return item
    }
    
    companion object {
        private const val SLOT_INFO_PARCEL = 21
        private const val SLOT_INFO_MEMBERS = 23
        
        private const val SLOT_RENT_UNRENT = 40
        private const val SLOT_GENERATE_KEY = 38
        private const val SLOT_RETURN = 43
        
        private val plugin get() = BbParcels.instance
        private val rentalMemberRepo by lazy {RentalMemberRepository(plugin.databaseManager)}
        
        fun open(player: Player, parcel: ParcelWithOwner) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val members = if (parcel.status == ParcelStatus.RENTED)
                    rentalMemberRepo.findMembers(parcel.id)
                else emptyList()
                
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!player.isOnline) return@Runnable
                    ParcelDetailGui(parcel, members).open(player)
                })
            })
        }
    } 
}