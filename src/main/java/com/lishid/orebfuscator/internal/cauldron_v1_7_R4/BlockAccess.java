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

package com.lishid.orebfuscator.internal.cauldron_v1_7_R4;

import com.lishid.orebfuscator.internal.IBlockAccess;
import net.minecraft.server.v1_7_R4.Block;
import net.minecraft.server.v1_7_R4.Packet;
import net.minecraft.server.v1_7_R4.TileEntity;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

//Volatile

public class BlockAccess implements IBlockAccess {
    @Override
    public boolean isBlockTransparent(int id) {
        return !Block.getById(id).r();
    }

    @Override
    public void updateBlockTileEntity(org.bukkit.block.Block block, Player player) {
        CraftWorld world = (CraftWorld) block.getWorld();
        TileEntity tileEntity = world.getTileEntityAt(block.getX(), block.getY(), block.getZ());
        if (tileEntity == null) {
            return;
        }
        Packet packet = tileEntity.getUpdatePacket();
        if (packet != null) {
            CraftPlayer player2 = (CraftPlayer) player;
            player2.getHandle().playerConnection.sendPacket(packet);
        }
    }
}