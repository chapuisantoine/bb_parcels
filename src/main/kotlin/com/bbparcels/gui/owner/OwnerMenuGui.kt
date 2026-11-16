package com.bbparcels.gui.owner

import com.bbparcels.BbParcels
import com.bbparcels.database.repository.ParcelWithOwner
import com.bbparcels.database.repository.ProfileInfo
import com.bbparcels.database.repository.RentalMemberRepository
import com.bbparcels.gui.BbGui
import com.bbparcels.gui.utils.Utils
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class OwnerMenuGui private constructor(
    private val parcel: ParcelWithOwner,
    private val members: List<ProfileInfo>
) : BbGui("[BB] Ma parcelle") {
    
    
    init {
        Utils.fillBorders(inventory)
        inventory.setItem(SLOT_INFO_PARCEL, buildInfoParcelItem(parcel))
        inventory.setItem(SLOT_INFO_MEMBERS, Utils.buildListMembersItem(members))
        inventory.setItem(SLOT_ADD_MEMBER, Utils.buildNavItem(Material.GREEN_CONCRETE, "§eAjouter un membre"))
        inventory.setItem(SLOT_MANAGE_MEMBERS, Utils.buildNavItem(Material.ORANGE_CONCRETE, "§eGérer les membres"))
        
    }

    override fun onClick(player: Player, slot: Int, item: ItemStack) {
        when (slot) {
            SLOT_ADD_MEMBER -> ListAddMemberGui.open(player, parcel)
            SLOT_MANAGE_MEMBERS -> ListManageMemberGui(parcel, members, 0).open(player)
        }
    }

    
    private fun buildInfoParcelItem(parcel: ParcelWithOwner): ItemStack {
        val material = Material.PAPER
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        
        meta.setDisplayName("§fParcelle §a${parcel.label}§f :")
        
        meta.lore = buildList {
            if (parcel.tenantProfile != null)
                add("§7Locataire : §e${parcel.tenantProfile.displayName}")
            else
                add("§7Locataire : §ePas de locataire")
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
            add("§7Prix de la location : ${parcel.weeklyPrice}")   
        }
        
        item.itemMeta = meta
        return item
    }
    
    companion object {
        
        private const val SLOT_INFO_PARCEL = 21
        private const val SLOT_INFO_MEMBERS = 23
        private const val SLOT_ADD_MEMBER = 39
        private const val SLOT_MANAGE_MEMBERS = 41
        
        private val rentalMemberRepo by lazy {RentalMemberRepository(BbParcels.instance.databaseManager)} 
        
        fun open(player: Player, parcel: ParcelWithOwner) {
            val plugin = BbParcels.instance
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val members = rentalMemberRepo.findMembers(parcel.id)
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    if (!player.isOnline) return@Runnable
                    OwnerMenuGui(parcel, members).open(player)
                })
            })
        }
    }
}