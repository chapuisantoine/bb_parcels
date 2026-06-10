package com.bbparcels

import com.bbparcels.command.admin.AdminCommand
import com.bbparcels.command.agent.AgentCommand
import com.bbparcels.command.owner.OwnerCommand
import com.bbparcels.config.PluginConfig
import com.bbparcels.database.DatabaseManager
import com.bbparcels.gui.agent.AgentGuiListener
import com.bbparcels.gui.owner.OwnerGuiListener
import org.bukkit.plugin.java.JavaPlugin

class BbParcels : JavaPlugin() {

    lateinit var databaseManager: DatabaseManager
        private set

    companion object {
        lateinit var instance: BbParcels
            private set
    }

    override fun onEnable() {
        instance = this
        saveDefaultConfig()

        databaseManager = DatabaseManager(this)
        databaseManager.connect()
        
        val pluginConfig = PluginConfig(config)
        
        getCommand("parceladmin")?.let { cmd ->
            val handler = AdminCommand(this)
            cmd.setExecutor(handler)
            cmd.tabCompleter = handler
        }
        
        
        getCommand("parcelagent")?.let { cmd ->
            val handler = AgentCommand(this, pluginConfig)
            cmd.setExecutor(handler)
            cmd.tabCompleter = handler
        }
        
        getCommand("myparcel")?.let { cmd ->
            val handler = OwnerCommand(this)
            cmd.setExecutor(handler)
            cmd.tabCompleter = handler
        }
        
        server.pluginManager.registerEvents(AgentGuiListener(), this)
        server.pluginManager.registerEvents(OwnerGuiListener(), this)

        logger.info("BbParcels plugin enabled!")
    }

    override fun onDisable() {
        if (::databaseManager.isInitialized) {
            databaseManager.disconnect()
        }
        logger.info("BbParcels plugin disabled!")
    }
}
