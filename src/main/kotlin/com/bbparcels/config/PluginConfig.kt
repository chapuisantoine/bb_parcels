package com.bbparcels.config

import org.bukkit.configuration.file.FileConfiguration

class PluginConfig(private val config: FileConfiguration) {
    
    fun getDefaultRenewalDays(): Long = config.getLong("rentals.default-renewal-days", 30)
    
}