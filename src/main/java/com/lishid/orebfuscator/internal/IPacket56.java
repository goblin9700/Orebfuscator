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

package com.lishid.orebfuscator.internal;

import java.util.zip.Deflater;

public interface IPacket56 {
    public void setPacket(Object packet);

    public int getPacketChunkNumber();

    public int[] getX();

    public int[] getZ();

    public int[] getChunkMask();

    public int[] getExtraMask();

    public Object getFieldData(String field);

    public void setFieldData(String field, Object data);

    public String getInflatedBuffers();

    public String getBuildBuffer();

    public String getOutputBuffer();

    public void compress(Deflater deflater);

}
