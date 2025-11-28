package com.nexomc.nexo_bolt_addon

import com.nexomc.nexo.mechanics.furniture.BlockLocation
import com.nexomc.nexo.mechanics.furniture.IFurniturePacketManager
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.popcraft.bolt.BoltAPI
import org.popcraft.bolt.BoltPlugin
import org.popcraft.bolt.event.LockBlockEvent

val boltPlugin by lazy { Bukkit.getPluginManager().getPlugin("Bolt") as BoltPlugin }
val bolt by lazy { Bukkit.getServer().servicesManager.load(BoltAPI::class.java)!! }

class NexoBoltAddon : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(NexoFurnitureListener(), this)
        bolt.registerListener(LockBlockEvent::class.java) { event ->
            if (!IFurniturePacketManager.blockIsHitbox(event.block)) return@registerListener
            IFurniturePacketManager.baseEntityFromHitbox(event.block.location) ?: return@registerListener
            event.isCancelled = true
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
