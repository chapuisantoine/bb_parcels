package com.bbparcels.gui.owner

import com.bbparcels.BbParcels
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

object OwnerMenuGui {
    
    private val cachedParcel = mutableMapOf<UUID, ParcelWithOwner>()
    
    private val rentalRepo = RentalRepository(BbParcels.instance.databaseManager)
    private val rentalMemberRepo = RentalMemberRepository(BbParcels.instance.databaseManager)
    
    private val BORDER_SLOTS = (0..8).toList() + (45..53).toList() + listOf(9, 18, 27, 36, 17, 26, 35, 44)
    
    private const val SLOT_INFO_PARCEL = 21
    private const val SLOT_INFO_MEMBERS = 23
    
    private const val SLOT_ADD_MEMBER = 39
    private const val SLOT_MANAGE_MEMBERS = 41
    
    fun open(player: Player, parcel: ParcelWithOwner) {
        cachedParcel[player.uniqueId] = parcel
        
        val inv = Bukkit.createInventory(null, 54, "[BB] Ma parcelle")
        
        fillBorders(inv)
        
        inv.setItem(SLOT_INFO_PARCEL, buildInfoParcelItem(parcel))
        inv.setItem(SLOT_INFO_MEMBERS, buildListMembersItem(parcel))
        inv.setItem(SLOT_ADD_MEMBER, buildNavItem(Material.GREEN_CONCRETE, "§eAjouter un membre"))
        inv.setItem(SLOT_MANAGE_MEMBERS, buildNavItem(Material.ORANGE_CONCRETE, "§eGérer les membres"))
        player.openInventory(inv)
    }
    
    fun handleClick(player: Player, slot: Int) {
        val parcel = cachedParcel[player.uniqueId] ?: return
        when (slot) {
            SLOT_ADD_MEMBER -> {
                val radius = 20.0
                val nearby = player.getNearbyEntities(radius, radius, radius)
                    .filterIsInstance<Player>()
                ListAddMemberGui.open(player, nearby, parcel)
            }
            SLOT_MANAGE_MEMBERS -> {
                val membersUuids = rentalMemberRepo.findMembers(parcel.label)
                val members = membersUuids.mapNotNull {Bukkit.getPlayer(UUID.fromString(it))}
                ListManageMemberGui.open(player, members, parcel)
            }
        }
    }
    
    private fun buildInfoParcelItem(parcel: ParcelWithOwner): ItemStack {
        val material = Material.PAPER
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        
        meta.setDisplayName("§fParcelle §a${parcel.label}§f :")
        
        meta.lore = buildList {
            val ownerName = Bukkit.getOfflinePlayer(UUID.fromString(parcel.ownerUuid)).name ?: parcel.ownerUuid
            add("§7Locataire : §e${ownerName}")
                       
            val world = Bukkit.getWorld(parcel.world)
            if (world != null) {
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
                val listMembers = rentalMemberRepo.findMembersByRentalId(rental.id)
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
    
    private fun buildNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        item.itemMeta = meta
        return item
    }
    
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
    
    private fun statusLabel(status: ParcelStatus) = when (status) {
        ParcelStatus.AVAILABLE -> "§aDisponible"
        ParcelStatus.RENTED    -> "§cLouée"
        ParcelStatus.DISABLED  -> "§7Désactivée"
    }
    
}