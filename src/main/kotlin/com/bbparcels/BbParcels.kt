package com.bbparcels

import com.bbparcels.cache.ActiveProfileCache
import com.bbparcels.cache.MemberCache
import com.bbparcels.cache.ParcelCache
import com.bbparcels.command.AdminCommand
import com.bbparcels.command.AgentCommand
import com.bbparcels.command.OwnerCommand
import com.bbparcels.config.PluginConfig
import com.bbparcels.database.DatabaseManager
import com.bbparcels.database.repository.ParcelRepository
import com.bbparcels.database.repository.ProfileRepository
import com.bbparcels.database.repository.RentalMemberRepository
import com.bbparcels.gui.GuiListener
import com.bbparcels.gui.agent.AgentTabletListener
import com.bbparcels.gui.owner.OwnerGuiListener
import org.bukkit.plugin.java.JavaPlugin

class BbParcels : JavaPlugin() {

    lateinit var databaseManager: DatabaseManager
        private set
    
    lateinit var pluginConfig: PluginConfig
        private set
        
    lateinit var parcelCache: ParcelCache
        private set

    lateinit var memberCache: MemberCache
        private set
        
    lateinit var activeProfileCache: ActiveProfileCache
        private set

    companion object {
        lateinit var instance: BbParcels
            private set
    }

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        
        pluginConfig = PluginConfig(config)
        
        databaseManager = DatabaseManager(this)
        databaseManager.connect()
        
        
        parcelCache = ParcelCache(ParcelRepository(databaseManager))
        parcelCache.load()
        
        memberCache = MemberCache(RentalMemberRepository(databaseManager))
        memberCache.load()

        activeProfileCache = ActiveProfileCache(this, ProfileRepository(databaseManager))
        server.pluginManager.registerEvents(activeProfileCache, this)
        activeProfileCache.refreshAllOnline()
        
        server.pluginManager.registerEvents(AgentTabletListener(), this)
        server.pluginManager.registerEvents(OwnerGuiListener(), this)
        server.pluginManager.registerEvents(GuiListener(this), this)
        
        getCommand("parceladmin")?.let { cmd ->
            val handler = AdminCommand(this, parcelCache, memberCache, activeProfileCache)
            cmd.setExecutor(handler)
            cmd.tabCompleter = handler
        }
        
        
        getCommand("parcelagent")?.let { cmd ->
            val handler = AgentCommand(this, pluginConfig, parcelCache, memberCache,activeProfileCache)
            cmd.setExecutor(handler)
            cmd.tabCompleter = handler
        }
        
        getCommand("myparcel")?.let { cmd ->
            val handler = OwnerCommand(this, parcelCache, activeProfileCache, memberCache)
            cmd.setExecutor(handler)
            cmd.tabCompleter = handler
        }
        
        logger.info("BbParcels plugin enabled!")
    }

    override fun onDisable() {
        if (::databaseManager.isInitialized) {
            databaseManager.disconnect()
        }
        logger.info("BbParcels plugin disabled!")
    }
}