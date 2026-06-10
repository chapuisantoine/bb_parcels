package com.bbparcels.gui.agent

import com.bbparcels.BbParcels
import com.bbparcels.gui.owner.OwnerGuiListener
import com.bbparcels.model.ParcelStatus
import com.bbparcels.model.ParcelWithOwner
import com.bbparcels.repository.RentalMemberRepository
import com.bbparcels.repository.RentalRepository
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

object ParcelDetailGui {
    
    private val rentalRepo = RentalRepository(BbParcels.instance.databaseManager)
    private val rentalMembersRepo = RentalMemberRepository(BbParcels.instance.databaseManager)
    
    private val cachedParcel = mutableMapOf<UUID, ParcelWithOwner>()
    
    private val BORDER_SLOTS = (0..8).toList() + (45..53).toList() + listOf(9, 18, 27, 36, 17, 26, 35, 44)
    private const val SLOT_INFO_PARCEL = 21
    private const val SLOT_INFO_MEMBERS = 23
    
    private const val SLOT_RENT_UNRENT = 40
    private const val SLOT_GENERATE_KEY = 38
    private const val SLOT_RETURN = 43
    
    
    fun open(player: Player, parcel: ParcelWithOwner) {      
        cachedParcel[player.uniqueId]  = parcel
        
        val inv = Bukkit.createInventory(null, 54, "[BB] Détails de la parcelle")

        fillBorders(inv)
        
        inv.setItem(SLOT_INFO_PARCEL, buildInfoParcelItem(parcel))
        inv.setItem(SLOT_INFO_MEMBERS, buildListMembersItem(parcel))
        
        if (parcel.status == ParcelStatus.AVAILABLE)
            inv.setItem(SLOT_RENT_UNRENT, buildNavItem(Material.GREEN_CONCRETE, "§eChoisir à qui louer"))
        if (parcel.status == ParcelStatus.RENTED) {
            inv.setItem(SLOT_RENT_UNRENT, buildNavItem(Material.ORANGE_CONCRETE, "§eRésilier la location"))
            inv.setItem(SLOT_GENERATE_KEY, buildNavItem(Material.TRIPWIRE_HOOK, "§eCréer une clé"))
        }
        if (parcel.status == ParcelStatus.DISABLED)
            inv.setItem(SLOT_RENT_UNRENT, buildNavItem(Material.GRAY_CONCRETE, "§eCette parcelle ne peut pas être louée"))
        
        inv.setItem(SLOT_RETURN, buildNavItem(Material.BARRIER, "§7Retour"))
        player.openInventory(inv)
    }
    
    fun hanbleClick(player: Player, slot: Int, item: ItemStack) {
        val parcel = cachedParcel[player.uniqueId] ?: return
        when (slot) {
            SLOT_RETURN -> AgentMenuGui.open(player)
            SLOT_GENERATE_KEY -> player.inventory.addItem(OwnerGuiListener.createParcelKey(parcel))
            SLOT_RENT_UNRENT -> {
                if (item.type == Material.GREEN_CONCRETE) {
                    val radius = 20.0
                    val nearby = player.getNearbyEntities(radius, radius, radius)
                        .filterIsInstance<Player>()
                    ListMembersGui.open(player, nearby, parcel)
                }
                if (item.type == Material.ORANGE_CONCRETE) {
                    ConfirmUnrentGui.open(player, parcel)
                }
            }
        }
    }
    
    private fun buildInfoParcelItem(parcel: ParcelWithOwner): ItemStack {
        val material = Material.PAPER
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        
        meta.setDisplayName("§fParcelle §a${parcel.label}§f :")
        
        meta.lore = buildList {
            add("§7Statut de la parcelle : §e${statusLabel(parcel.status)}") 
            
            if (parcel.ownerUuid != null) {
                val ownerName = Bukkit.getOfflinePlayer(UUID.fromString(parcel.ownerUuid)).name ?: parcel.ownerUuid
                add("§7Locataire : §e${ownerName}")
            } else {
                add("§7Locataire : §epas de locataire")
            }
            
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
            
            add("§7Prix de la location : XXXX") //TODO a faire quand il y aura la gestion des prix    
        }
        
        item.itemMeta = meta
        return item
    }
    
    private fun buildListMembersItem(parcel: ParcelWithOwner): ItemStack {
        val material = Material.PAPER
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        
        meta.setDisplayName("§fMembres :")
        meta.lore = buildList {
            val rental = rentalRepo.findActiveByParcelId(parcel.id)
            if (rental != null) {
                val listMembers = rentalMembersRepo.findMembersByRentalId(rental.id)
                if (listMembers.isNotEmpty()){
                    listMembers.forEach { memberUuid ->
                    add("§7     ${Bukkit.getOfflinePlayer(UUID.fromString(memberUuid)).name}")
                    }
                }
            }
        }
        
        item.itemMeta = meta
        return item
    }
    
    // définition de l'item de pagination
    private fun buildNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        item.itemMeta = meta
        return item
    }
    
    
    //contour de l'inventaire en Verre Bleu et Cyan
    private fun fillBorders(inv: Inventory) {    
        for (slot in BORDER_SLOTS) {
            val material = if (slot % 2 == 0 ) Material.BLUE_STAINED_GLASS_PANE else Material.CYAN_STAINED_GLASS_PANE
            val item = ItemStack(material)
            val meta = item.itemMeta!!
            meta.setDisplayName("§r")
            item.itemMeta = meta
            inv.setItem(slot, item)
        }
    }
    
    //traduction des status
    private fun statusLabel(status: ParcelStatus) = when (status) {
        ParcelStatus.AVAILABLE -> "§aDisponible"
        ParcelStatus.RENTED    -> "§cLouée"
        ParcelStatus.DISABLED  -> "§7Désactivée"
    }
    
}