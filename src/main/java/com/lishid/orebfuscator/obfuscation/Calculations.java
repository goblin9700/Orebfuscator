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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.zip.Deflater;

import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.OrebfuscatorConfig;
import com.lishid.orebfuscator.cache.ObfuscatedCachedChunk;
import com.lishid.orebfuscator.internal.IPacket51;
import com.lishid.orebfuscator.internal.IPacket56;
import com.lishid.orebfuscator.internal.InternalAccessor;

public class Calculations {
    public static final ThreadLocal<byte[]> buffer = new ThreadLocal<byte[]>() {
        protected byte[] initialValue() {
            return new byte[65536];
        }
    };

    public static final ThreadLocal<Deflater> localDeflater = new ThreadLocal<Deflater>() {
        @Override
        protected Deflater initialValue() {
            // Not used from orebfuscator thread, best speed instead
            return new Deflater(Deflater.BEST_SPEED);
        }
    };

    private static WeakHashMap<Player, Map<ChunkAddress, Set<MinecraftBlock>>> signsMap = new WeakHashMap<Player, Map<ChunkAddress, Set<MinecraftBlock>>>();

    private static Map<ChunkAddress, Set<MinecraftBlock>> getPlayerSignsMap(Player player) {
        Map<ChunkAddress, Set<MinecraftBlock>> map = signsMap.get(player);
        if (map == null) {
            map = new HashMap<ChunkAddress, Set<MinecraftBlock>>();
            signsMap.put(player, map);
        }
        return map;
    }

    private static void putSignsList(Player player, int chunkX, int chunkZ, Set<MinecraftBlock> blocks) {
        Map<ChunkAddress, Set<MinecraftBlock>> map = getPlayerSignsMap(player);
        ChunkAddress address = new ChunkAddress(chunkX, chunkZ);
        map.put(address, blocks);
    }

    public static Set<MinecraftBlock> getSignsList(Player player, int chunkX, int chunkZ) {
        Map<ChunkAddress, Set<MinecraftBlock>> map = getPlayerSignsMap(player);
        ChunkAddress address = new ChunkAddress(chunkX, chunkZ);
        return map.get(address);
    }

    public static void putSignsList(Player player, int chunkX, int chunkZ, List<Block> proximityBlocks) {
        Set<MinecraftBlock> signs = new HashSet<MinecraftBlock>();
        for (Block b : proximityBlocks) {
            if (b.getState() instanceof Sign) {
                signs.add(new MinecraftBlock(b));
            }
        }
        putSignsList(player, chunkX, chunkZ, signs);
    }

    public static void Obfuscate(Object packet, Player player) {
        // Assuming that NoLagg will pass a Packet51
        IPacket51 packet51 = InternalAccessor.Instance.newPacket51();
        packet51.setPacket(packet);
        Calculations.Obfuscate(packet51, player);
    }

    public static void Obfuscate(IPacket56 packet, Player player) {
        if (packet.getFieldData(packet.getOutputBuffer()) != null) {
            return;
        }

        ChunkInfo[] infos = getInfo(packet, player);

        for (int chunkNum = 0; chunkNum < infos.length; chunkNum++) {
            // Create an info objects
            ChunkInfo info = infos[chunkNum];
            info.buffer = buffer.get();
            ComputeChunkInfoAndObfuscate(info, (byte[]) packet.getFieldData(packet.getBuildBuffer()));
        }
    }

    public static void Obfuscate(IPacket51 packet, Player player) {
        Obfuscate(packet, player, true);
    }

    public static void Obfuscate(IPacket51 packet, Player player, boolean needCompression) {
        ChunkInfo info = getInfo(packet, player);
        info.buffer = buffer.get();

        if (info.chunkMask == 0 && info.extraMask == 0) {
            return;
        }

        if (info.buffer == null || info.buffer.length == 0) {
            return;
        }

        ComputeChunkInfoAndObfuscate(info, packet.getBuffer());

        if (needCompression) {
            Deflater deflater = localDeflater.get();
            packet.compress(deflater);
        }
    }

    public static ChunkInfo[] getInfo(IPacket56 packet, Player player) {
        ChunkInfo[] infos = new ChunkInfo[packet.getPacketChunkNumber()];

        int dataStartIndex = 0;

        int[] x = packet.getX();
        int[] z = packet.getZ();

        byte[][] inflatedBuffers = (byte[][]) packet.getFieldData(packet.getInflatedBuffers());

        int[] chunkMask = packet.getChunkMask();
        int[] extraMask = packet.getExtraMask();

        byte[] buildBuffer = (byte[]) packet.getFieldData(packet.getBuildBuffer());

        // Check for spigot and fix accordingly
        if (buildBuffer.length == 0) {
            int finalBufferSize = 0;
            for (int i = 0; i < inflatedBuffers.length; i++) {
                finalBufferSize += inflatedBuffers[i].length;
            }

            buildBuffer = new byte[finalBufferSize];
            int bufferLocation = 0;
            for (int i = 0; i < inflatedBuffers.length; i++) {
                System.arraycopy(inflatedBuffers[i], 0, buildBuffer, bufferLocation, inflatedBuffers[i].length);
                bufferLocation += inflatedBuffers[i].length;
            }

            packet.setFieldData(packet.getBuildBuffer(), buildBuffer);
        }

        for (int chunkNum = 0; chunkNum < packet.getPacketChunkNumber(); chunkNum++) {
            // Create an info objects
            ChunkInfo info = new ChunkInfo();
            infos[chunkNum] = info;
            info.world = player.getWorld();
            info.player = player;
            info.chunkX = x[chunkNum];
            info.chunkZ = z[chunkNum];
            info.chunkMask = chunkMask[chunkNum];
            info.extraMask = extraMask[chunkNum];
            info.data = buildBuffer;
            info.startIndex = dataStartIndex;
            info.size = inflatedBuffers[chunkNum].length;

            dataStartIndex += info.size;
        }

        return infos;
    }

    public static ChunkInfo getInfo(IPacket51 packet, Player player) {
        // Create an info objects
        ChunkInfo info = new ChunkInfo();
        info.world = player.getWorld();
        info.player = player;
        info.chunkX = packet.getX();
        info.chunkZ = packet.getZ();
        info.chunkMask = packet.getChunkMask();
        info.extraMask = packet.getExtraMask();
        info.data = packet.getBuffer();
        info.startIndex = 0;
        return info;
    }

    public static void ComputeChunkInfoAndObfuscate(ChunkInfo info, byte[] original) {
        // Compute chunk number
        for (int i = 0; i < 16; i++) {
            if ((info.chunkMask & 1 << i) > 0) {
                info.chunkSectionToIndexMap[i] = info.chunkSectionNumber;
                info.chunkSectionNumber++;
            }
            else {
                info.chunkSectionToIndexMap[i] = -1;
            }
            if ((info.extraMask & 1 << i) > 0) {
                info.extraSectionToIndexMap[i] = info.extraSectionNumber;
                info.extraSectionNumber++;
            }
        }

        info.size = 2048 * (5 * info.chunkSectionNumber + info.extraSectionNumber) + 256;
        info.blockSize = 4096 * info.chunkSectionNumber;

        if (info.startIndex + info.blockSize > info.data.length) {
            return;
        }

        // Obfuscate
        if (!OrebfuscatorConfig.isWorldDisabled(info.world.getName()) && // World not enabled
                OrebfuscatorConfig.obfuscateForPlayer(info.player) && // Should the player have obfuscation?
                OrebfuscatorConfig.Enabled) // Plugin enabled
        {
            byte[] obfuscated = Obfuscate(info, original);
            // Copy the data out of the buffer
            System.arraycopy(obfuscated, 0, original, info.startIndex, info.blockSize);
        }
    }

    public static byte[] Obfuscate(ChunkInfo info, byte[] original) {
        boolean isNether = info.world.getEnvironment() == Environment.NETHER;
        // Used for caching
        ObfuscatedCachedChunk cache = null;
        // Hash used to check cache consistency
        long hash = 0L;
        // Blocks kept track for ProximityHider
        ArrayList<Block> proximityBlocks = new ArrayList<Block>();
        // Start with caching false
        info.useCache = false;

        int initialRadius = OrebfuscatorConfig.InitialRadius;

        // Expand buffer if not enough space
        if (info.blockSize > info.buffer.length) {
            info.buffer = new byte[info.blockSize];
            buffer.set(info.buffer);
        }

        // Copy data into buffer
        System.arraycopy(info.data, info.startIndex, info.buffer, 0, info.blockSize);

        // Caching
        if (OrebfuscatorConfig.UseCache) {
            // Sanitize buffer for caching
            PrepareBufferForCaching(info.buffer, info.blockSize);

            // Get cache folder
            File cacheFolder = new File(OrebfuscatorConfig.getCacheFolder(), info.world.getName());
            // Create cache objects
            cache = new ObfuscatedCachedChunk(cacheFolder, info.chunkX, info.chunkZ);
            info.useCache = true;
            // Hash the chunk
            hash = CalculationsUtil.Hash(info.buffer, info.blockSize);

            // Check if hash is consistent
            cache.Read();

            long storedHash = cache.getHash();
            int[] proximityList = cache.proximityList;

            if (storedHash == hash && cache.data != null) {
                // Decrypt chest list
                if (proximityList != null) {
                    for (int i = 0; i < proximityList.length; i += 3) {
                        Block b = CalculationsUtil.getBlockAt(info.player.getWorld(), proximityList[i], proximityList[i + 1], proximityList[i + 2]);
                        proximityBlocks.add(b);
                    }
                }

                // Caching done, de-sanitize buffer
                RepaintChunkToBuffer(cache.data, info);

                // ProximityHider add blocks
                putSignsList(info.player, info.chunkX, info.chunkZ, proximityBlocks);
                ProximityHider.AddProximityBlocks(info.player, proximityBlocks);

                // Hash match, use the cached data instead and skip calculations
                return cache.data;
            }
        }

        // Track of pseudo-randomly assigned randomBlock
        int randomIncrement = 0;
        int randomIncrement2 = 0;
        int ramdomCave = 0;
        // Track of whether a block should be obfuscated or not
        boolean obfuscate = false;
        boolean specialObfuscate = false;

        int engineMode = OrebfuscatorConfig.EngineMode;
        int maxChance = OrebfuscatorConfig.AirGeneratorMaxChance;
        int incrementMax = maxChance;

        int randomBlocksLength = OrebfuscatorConfig.getRandomBlocks(false, isNether).length;
        boolean randomAlternate = false;

        // Loop over 16x16x16 chunks in the 16x256x16 column
        int dataIndexModifier = 0;
        int extraFirstIndex = (int)(info.blockSize*2.5);
        int extraIncrement = 0;
        boolean extraFirstPart = true;
        int startX = info.chunkX << 4;
        int startZ = info.chunkZ << 4;
        for (int i = 0; i < 16; i++) {
            // If the bitmask indicates this chunk is sent...
            if ((info.chunkMask & 1 << i) != 0) {
                int indexDataStart = dataIndexModifier * 4096;
                boolean useExtraData = (info.extraMask & (1 << i)) > 0;

                int tempIndex = 0;
                OrebfuscatorConfig.shuffleRandomBlocks();
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        incrementMax = (maxChance + OrebfuscatorConfig.random(maxChance)) / 2;
                        for (int x = 0; x < 16; x++) {

                            int index = indexDataStart + tempIndex;
                            byte data = info.data[info.startIndex + index];
                            int blockY = (i << 4) + y;

                            int extra = 0;
                            if(useExtraData)
                            {
                                if(extraFirstPart)
                                    extra = info.data[info.startIndex + extraFirstIndex + extraIncrement] & 0x0F;
                                else
                                    extra = info.data[info.startIndex + extraFirstIndex + extraIncrement++] >> 4;
                                extraFirstPart = !extraFirstPart;
                            }

                            int blockId = (extra << 8) + (data & 0xFF);
                            /*Block b = info.player.getWorld().getBlockAt(startX + x, blockY, startZ + z);
                            if(b.getTypeId() != blockId)
                            {
                                System.out.println("BlockID mismatch at "+b.getLocation()+" read "+blockId+" expected "+b.getTypeId());
                            }*/

                            // Initialize data
                            obfuscate = false;
                            specialObfuscate = false;

                            // Check if the block should be obfuscated for the default engine modes
                            if (OrebfuscatorConfig.isObfuscated(blockId, isNether)) {
                                if (initialRadius == 0) {
                                    // Do not interfere with PH
                                    if (OrebfuscatorConfig.UseProximityHider && OrebfuscatorConfig.isProximityObfuscated(blockY, blockId)) {
                                        if (!areAdjacentBlocksTransparent(info, blockId, startX + x, blockY, startZ + z, 1)) {
                                            obfuscate = true;
                                        }
                                    }
                                    else {
                                        // Obfuscate all blocks
                                        obfuscate = true;
                                    }
                                }
                                else {
                                    // Check if any nearby blocks are transparent
                                    if (!areAdjacentBlocksTransparent(info, blockId, startX + x, blockY, startZ + z, initialRadius)) {
                                        obfuscate = true;
                                    }
                                }
                            }

                            // Check if the block should be obfuscated because of proximity check
                            if (!obfuscate && OrebfuscatorConfig.UseProximityHider && OrebfuscatorConfig.isProximityObfuscated(blockY, blockId)) {
                                if (OrebfuscatorConfig.isProximityHiderOn(blockY, blockId)) {
                                    Block block = CalculationsUtil.getBlockAt(info.player.getWorld(), startX + x, blockY, startZ + z);
                                    if (block != null) {
                                        proximityBlocks.add(block);
                                    }
                                    obfuscate = true;
                                    if (OrebfuscatorConfig.UseSpecialBlockForProximityHider)
                                        specialObfuscate = true;
                                }
                            }

                            // Check if the block is obfuscated
                            if (obfuscate) {
                                if (specialObfuscate) {
                                    // Proximity hider
                                    //TODO 4096 BlockID replacement support
                                    info.buffer[index] = (byte) OrebfuscatorConfig.ProximityHiderID;
                                }
                                else {
                                    randomIncrement2 = OrebfuscatorConfig.random(incrementMax);// CalculationsUtil.increment(randomIncrement2, incrementMax);

                                    if (engineMode == 1) {
                                        // Engine mode 1, replace with stone
                                        //TODO 4096 BlockID replacement support
                                        info.buffer[index] = (byte) (isNether ? 87 : 1);
                                    }
                                    else if (engineMode == 2) {
                                        // Ending mode 2, replace with random block
                                        if (randomBlocksLength > 1)
                                            randomIncrement = CalculationsUtil.increment(randomIncrement, randomBlocksLength);
                                        //TODO 4096 BlockID replacement support
                                        info.buffer[index] = OrebfuscatorConfig.getRandomBlock(randomIncrement, randomAlternate, isNether);
                                        randomAlternate = !randomAlternate;
                                    }
                                    // Anti texturepack and freecam
                                    if (OrebfuscatorConfig.AntiTexturePackAndFreecam) {
                                        // Add random air blocks
                                        if (randomIncrement2 == 0) {
                                            ramdomCave = 1 + OrebfuscatorConfig.random(3);
                                        }

                                        if (ramdomCave > 0) {
                                            //TODO 4096 BlockID replacement support
                                            info.buffer[index] = 0;
                                            ramdomCave--;
                                        }
                                    }
                                }
                            }

                            // Check if the block should be obfuscated because of the darkness
                            if (!obfuscate && OrebfuscatorConfig.DarknessHideBlocks && OrebfuscatorConfig.isDarknessObfuscated(blockId)) {
                                if (!areAdjacentBlocksBright(info, startX + x, (i << 4) + y, startZ + z, 1)) {
                                    // Hide block, setting it to air
                                    //TODO 4096 BlockID replacement support
                                    info.buffer[index] = 0;
                                }
                            }

                            tempIndex++;
                        }
                    }
                }

                dataIndexModifier++;
                if(useExtraData && !extraFirstPart)
                    extraFirstIndex++;
            }
        }

        putSignsList(info.player, info.chunkX, info.chunkZ, proximityBlocks);
        ProximityHider.AddProximityBlocks(info.player, proximityBlocks);

        // If cache is still allowed
        if (info.useCache) {
            // Save cache
            int[] proximityList = new int[proximityBlocks.size() * 3];
            for (int i = 0; i < proximityBlocks.size(); i++) {
                Block b = proximityBlocks.get(i);
                if (b != null) {
                    proximityList[i * 3] = b.getX();
                    proximityList[i * 3 + 1] = b.getY();
                    proximityList[i * 3 + 2] = b.getZ();
                }
            }
            cache.Write(hash, info.buffer, proximityList);
        }

        // Free memory taken by cache quickly
        if (cache != null) {
            cache.free();
        }

        // Caching done, de-sanitize buffer
        if (OrebfuscatorConfig.UseCache) {
            RepaintChunkToBuffer(info.buffer, info);
        }

        return info.buffer;
    }

    private static byte[] cacheMap = new byte[256];
    static {
        buildCacheMap();
    }

    public static void buildCacheMap() {
        for (int i = 0; i < 256; i++) {
            cacheMap[i] = (byte) i;
            if (OrebfuscatorConfig.isBlockTransparent((short) i) && !isBlockSpecialObfuscated(64, (byte) i)) {
                cacheMap[i] = 0;
            }
        }
    }

    private static void PrepareBufferForCaching(byte[] data, int length) {
        for (int i = 0; i < length; i++) {
            data[i] = cacheMap[((int) data[i] + 256) % 256];
        }
    }

    private static boolean isBlockSpecialObfuscated(int y, byte id) {
        if (OrebfuscatorConfig.DarknessHideBlocks && OrebfuscatorConfig.isDarknessObfuscated(id)) {
            return true;
        }
        if (OrebfuscatorConfig.UseProximityHider && OrebfuscatorConfig.isProximityObfuscated(y, id)) {
            return true;
        }
        return false;
    }

    private static void RepaintChunkToBuffer(byte[] data, ChunkInfo info) {
        if (OrebfuscatorConfig.useYLocation()) {
            RepaintChunkToBuffer2(data, info);
        }
        else {
            RepaintChunkToBuffer1(data, info);
        }
    }

    private static void RepaintChunkToBuffer1(byte[] data, ChunkInfo info) {
        byte[] original = info.data;
        int start = info.startIndex;
        int length = info.blockSize;

        for (int i = 0; i < length; i++) {
            if (data[i] == 0 && original[start + i] != 0) {
                if (OrebfuscatorConfig.isBlockTransparent(original[start + i])) {
                    if (!isBlockSpecialObfuscated(0, original[start + i])) {
                        data[i] = original[start + i];
                    }
                }
            }
        }
    }

    private static void RepaintChunkToBuffer2(byte[] data, ChunkInfo info) {
        byte[] original = info.data;
        int start = info.startIndex;
        int dataIndexModifier = 0;

        for (int i = 0; i < 16; i++) {
            if ((info.chunkMask & 1 << i) != 0) {
                int indexDataStart = dataIndexModifier * 4096;
                int tempIndex = 0;
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            int index = indexDataStart + tempIndex;
                            int blockY = (i << 4) + y;
                            if (data[index] == 0 && original[start + index] != 0) {
                                if (OrebfuscatorConfig.isBlockTransparent(original[start + index])) {
                                    if (!isBlockSpecialObfuscated(blockY, original[start + index])) {
                                        data[index] = original[start + index];
                                    }
                                }
                            }
                            tempIndex++;
                        }
                    }
                }

                dataIndexModifier++;
            }
        }
    }

    @Deprecated
    public static boolean areAjacentBlocksTransparent(ChunkInfo info, byte currentBlockID, int x, int y, int z, int countdown) {
        return areAdjacentBlocksTransparent(info, (int) currentBlockID, x, y, z, countdown);
    }

    public static boolean areAdjacentBlocksTransparent(ChunkInfo info, int currentBlockID, int x, int y, int z, int countdown) {
        Integer id = info.getBlockId(x, y, z);

        if (id == null) {
            if (CalculationsUtil.isChunkLoaded(info.world, x >> 4, z >> 4)) {
                id = info.world.getBlockTypeIdAt(x, y, z);
            }
            else {
                id = 1;
                info.useCache = false;
            }
        }

        if (id != currentBlockID && OrebfuscatorConfig.isBlockTransparent(id)) {
            return true;
        }

        if (countdown == 0)
            return false;

        if (areAdjacentBlocksTransparent(info, currentBlockID, x, y + 1, z, countdown - 1))
            return true;
        if (areAdjacentBlocksTransparent(info, currentBlockID, x, y - 1, z, countdown - 1))
            return true;
        if (areAdjacentBlocksTransparent(info, currentBlockID, x + 1, y, z, countdown - 1))
            return true;
        if (areAdjacentBlocksTransparent(info, currentBlockID, x - 1, y, z, countdown - 1))
            return true;
        if (areAdjacentBlocksTransparent(info, currentBlockID, x, y, z + 1, countdown - 1))
            return true;
        if (areAdjacentBlocksTransparent(info, currentBlockID, x, y, z - 1, countdown - 1))
            return true;

        return false;
    }

    /**
     * @deprecated Typo in method name.
     * @see com.lishid.orebfuscator.obfuscation.Calculations#areAdjacentBlocksBright(ChunkInfo, int, int, int, int)
     */
    @Deprecated
    public static boolean areAjacentBlocksBright(ChunkInfo info, int x, int y, int z, int countdown) {
        return areAdjacentBlocksBright(info, x, y, z, countdown);
    }

    public static boolean areAdjacentBlocksBright(ChunkInfo info, int x, int y, int z, int countdown) {
        if (CalculationsUtil.isChunkLoaded(info.world, x >> 4, z >> 4)) {
            if (info.world.getBlockAt(x, y, z).getLightLevel() > 0) {
                return true;
            }
        }
        else {
            return true;
        }

        if (countdown == 0)
            return false;

        if (areAdjacentBlocksBright(info, x, y + 1, z, countdown - 1))
            return true;
        if (areAdjacentBlocksBright(info, x, y - 1, z, countdown - 1))
            return true;
        if (areAdjacentBlocksBright(info, x + 1, y, z, countdown - 1))
            return true;
        if (areAdjacentBlocksBright(info, x - 1, y, z, countdown - 1))
            return true;
        if (areAdjacentBlocksBright(info, x, y, z + 1, countdown - 1))
            return true;
        if (areAdjacentBlocksBright(info, x, y, z - 1, countdown - 1))
            return true;

        return false;
    }
}