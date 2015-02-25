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

package com.lishid.orebfuscator.obfuscation;

import com.lishid.orebfuscator.Orebfuscator;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ChunkInfo {
    public boolean useCache;
    public int chunkX;
    public int chunkZ;
    public int chunkMask;
    public int extraMask;
    public int chunkSectionNumber;
    public int extraSectionNumber;
    public boolean canUseCache;
    public int[] chunkSectionToIndexMap = new int[16];
    public int[] extraSectionToIndexMap = new int[16];
    public World world;
    public byte[] data;
    public byte[] buffer;
    public Player player;
    public int startIndex;
    public int size;
    public int blockSize;

    public Integer getBlockId(int x, int y, int z)
    {
        int section = chunkSectionToIndexMap[y >> 4];

        Integer id = null;

        if ((chunkMask & (1 << (y >> 4))) > 0 && x >> 4 == chunkX && z >> 4 == chunkZ)
        {
            int cX = ((x % 16) < 0) ? (x % 16 + 16) : (x % 16);
            int cZ = ((z % 16) < 0) ? (z % 16 + 16) : (z % 16);

            int index = section * 4096 + (y % 16 << 8) + (cZ << 4) + cX;
            try {
                id = data[startIndex + index] & 0xFF;
            }
            catch (Exception e) {
                Orebfuscator.log(e);
                return null;
            }

            if((extraMask & (1 << (y >> 4))) > 0)
            {
                int extra = 0;
                int pos = (y % 16 <<8) + (cZ << 4) + cX;
                int extraSection = extraSectionToIndexMap[y >> 4];
                int extraIndex = startIndex + ((int)(blockSize*2.5)) + (extraSection * 2048) + (pos >> 1);
                if(pos % 2 == 0)
                    extra = data[extraIndex] & 0x0F;
                else
                    extra = data[extraIndex] >> 4;

                id += extra << 8;
                /*int expected = world.getBlockTypeIdAt(x, y, z);
                if(expected != id)
                {
                    System.out.println("Block mismatch at x"+x+" y:"+y+" z:"+z+" expected:"+expected+" read:"+id);
                }*/
            }
        }

        return id;
    }
}
