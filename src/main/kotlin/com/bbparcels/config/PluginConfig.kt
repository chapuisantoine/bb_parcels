package com.bbparcels.config

import org.bukkit.configuration.file.FileConfiguration

class PluginConfig(private val config: FileConfiguration) {
    
    val serverId: String = config.getString("server-id")
        ?.takeIf { it.isNotBlank() }
        ?: error("'server-id' manquant dans config.yml")
    
    fun getDefaultRenewalDays(): Long = config.getLong("rentals.default-renewal-days", 30)
}