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

import com.lishid.orebfuscator.internal.IPacket51;
import com.lishid.orebfuscator.internal.InternalAccessor;
import com.lishid.orebfuscator.utils.ReflectionHelper;
import net.minecraft.server.v1_7_R4.PacketPlayOutMapChunk;

import java.util.zip.Deflater;

//Volatile

public class Packet51 implements IPacket51 {
    private static Class<? extends Object> packetClass = PacketPlayOutMapChunk.class;

    PacketPlayOutMapChunk packet;
    byte[] buffer;
    byte[] inflatedBuffer;

    @Override
    public void setPacket(Object packet) {
        if (packet instanceof PacketPlayOutMapChunk) {
            this.packet = (PacketPlayOutMapChunk) packet;

            buffer = (byte[]) ReflectionHelper.getPrivateField(packetClass, packet, "field_149281_e");
            inflatedBuffer = (byte[]) ReflectionHelper.getPrivateField(packetClass, packet, "field_149278_f");
        }
        else {
            InternalAccessor.Instance.PrintError();
        }
    }

    @Override
    public int getX() {
        return (Integer) ReflectionHelper.getPrivateField(packetClass, packet, "field_149284_a");
    }

    @Override
    public int getZ() {
        return (Integer) ReflectionHelper.getPrivateField(packetClass, packet, "field_149282_b");
    }

    @Override
    public int getChunkMask() {
        return (Integer) ReflectionHelper.getPrivateField(packetClass, packet, "field_149283_c");
    }

    @Override
    public int getExtraMask() {
        return (Integer) ReflectionHelper.getPrivateField(packetClass, packet, "field_149280_d");
    }

    @Override
    public byte[] getBuffer() {
        return inflatedBuffer;
    }

    private byte[] getOutputBuffer() {
        return buffer;
    }

    @Override
    public void compress(Deflater deflater) {
        byte[] chunkInflatedBuffer = getBuffer();
        byte[] chunkBuffer = getOutputBuffer();

        deflater.reset();
        deflater.setInput(chunkInflatedBuffer, 0, chunkInflatedBuffer.length);
        deflater.finish();
        ReflectionHelper.setPrivateField(packetClass, packet, "field_149285_h", deflater.deflate(chunkBuffer));
    }
}
