/*
 * Copyright (C) 2011-2014 lishid.  All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  version 3.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.lishid.orebfuscator;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.lishid.orebfuscator.cache.ObfuscatedDataCache;
import com.lishid.orebfuscator.commands.OrebfuscatorCommandExecutor;
import com.lishid.orebfuscator.hithack.BlockHitManager;
import com.lishid.orebfuscator.hook.ChunkProcessingThread;
import com.lishid.orebfuscator.hook.OrebfuscatorPlayerHook;
import com.lishid.orebfuscator.hook.ProtocolLibHook;
import com.lishid.orebfuscator.internal.InternalAccessor;
import com.lishid.orebfuscator.listeners.OrebfuscatorBlockListener;
import com.lishid.orebfuscator.listeners.OrebfuscatorEntityListener;
import com.lishid.orebfuscator.listeners.OrebfuscatorPlayerListener;
import com.lishid.orebfuscator.utils.UpdateManager;

/**
 * Orebfuscator Anti X-RAY
 * 
 * @author lishid
 */
public class Orebfuscator extends JavaPlugin {

    public static final Logger logger = Logger.getLogger("Minecraft.OFC");
    public static Orebfuscator instance;
    public static boolean usePL = false;
    public static boolean useSpigot = false;

    private UpdateManager updater = new UpdateManager();

    @Override
    public void onEnable() {
        // Get plugin manager
        PluginManager pm = getServer().getPluginManager();

        // Version check
        boolean success = InternalAccessor.Initialize(this.getServer());

        if (!success) {
            Orebfuscator.log("Your version of CraftBukkit is not supported.");
            Orebfuscator.log("Please look for an updated version of Orebfuscator.");
            pm.disablePlugin(this);
            return;
        }

        instance = this;
        // Load configurations
        OrebfuscatorConfig.load();

        updater.Initialize(this, getFile());

        // Orebfuscator events
        pm.registerEvents(new OrebfuscatorPlayerListener(), this);
        pm.registerEvents(new OrebfuscatorEntityListener(), this);
        pm.registerEvents(new OrebfuscatorBlockListener(), this);

        pm.registerEvents(new OrebfuscatorPlayerHook(), this);

        if (pm.getPlugin("ProtocolLib") != null) {
            Orebfuscator.log("ProtocolLib found! Hooking into ProtocolLib.");
            (new ProtocolLibHook()).register(this);
            usePL = true;
        }

        if (pm.getPlugin("NoLagg") != null && !usePL) {
            getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                @Override
                public void run() {
                    Orebfuscator.log("WARNING! NoLagg Absolutely NEED ProtocolLib to work with Orebfuscator!");
                }
            }, 0, 60 * 1000);// Warn every minute
        }

        // TODO: Disable spigot's built-in orebfuscator since it has limited functionality
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            useSpigot = true;
        }
        catch (Exception e) {
            // If error occurred, then ignore.
        }
    }

    @Override
    public void onDisable() {
        ObfuscatedDataCache.clearCache();
        BlockHitManager.clearAll();
        ChunkProcessingThread.KillAll();
        getServer().getScheduler().cancelAllTasks();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return OrebfuscatorCommandExecutor.onCommand(sender, command, label, args);
    }
    
    public void runTask(Runnable task) {
        if(this.isEnabled()) {
            getServer().getScheduler().runTask(this, task);
        }
    }

    /**
     * Log an information
     */
    public static void log(String text) {
        logger.info("[OFC] " + text);
    }

    /**
     * Log an error
     */
    public static void log(Throwable e) {
        logger.severe("[OFC] " + e.toString());
        e.printStackTrace();
    }

    /**
     * Send a message to a player
     */
    public static void message(CommandSender target, String message) {
        target.sendMessage(ChatColor.AQUA + "[OFC] " + message);
    }
}