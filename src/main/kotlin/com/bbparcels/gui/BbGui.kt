package com.bbparcels.gui

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

abstract class BbGui(title: String, size: Int = 54): InventoryHolder {
    
    private val inv: Inventory = Bukkit.createInventory(this, size, title)
    
    override fun getInventory(): Inventory = inv
    
    open val permission: String? = null
    
    
    fun open(player: Player) {
        player.openInventory(inv)
    }
    
    abstract fun onClick(player: Player, slot: Int, item: ItemStack)
}