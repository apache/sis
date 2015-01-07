/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.shapefile;

import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

import org.apache.sis.storage.shapefile.ShapeTypeEnum;

/**
 * Shapefile Descriptor.
 * @author  Marc Le Bihan
 * @version 0.5
 * @since   0.5
 * @module
 */
public class ShapefileDescriptor {
    /** File code. */
    private int fileCode; // big

    /** File length. */
    private int fileLength; // big // The value for file length is the total length of the file in 16-bit words

    /** File version. */
    private int version; // little

    /** Shapefile type. */
    private ShapeTypeEnum shapeType; // little

    /** X Min. */
    private double xmin; // little

    /** Y Min. */
    private double ymin; // little

    /** X Max. */
    private double xmax; // little

    /** Y Max. */
    private double ymax; // little

    /** Z Min. */
    private double zmin; // little

    /** Z Max. */
    private double zmax; // little

    /** M Min. */
    private double mmin; // little

    /** M Max. */
    private double mmax; // little
    
    /**
     * Create a shapefile descriptor.
     * @param byteBuffer Source Bytebuffer.
     */
    public ShapefileDescriptor(MappedByteBuffer byteBuffer) {
        fileCode = byteBuffer.getInt();
        byteBuffer.getInt();
        byteBuffer.getInt();
        byteBuffer.getInt();
        byteBuffer.getInt();
        byteBuffer.getInt();
        fileLength = byteBuffer.getInt() * 2;

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        version = byteBuffer.getInt();
        shapeType = ShapeTypeEnum.get(byteBuffer.getInt());
        xmin = byteBuffer.getDouble();
        ymin = byteBuffer.getDouble();
        xmax = byteBuffer.getDouble();
        ymax = byteBuffer.getDouble();
        zmin = byteBuffer.getDouble();
        zmax = byteBuffer.getDouble();
        mmin = byteBuffer.getDouble();
        mmax = byteBuffer.getDouble();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //dbf.byteBuffer.get(); // should be 0d for field terminator
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator", "\n");

        s.append("FileCode: ").append(fileCode).append(lineSeparator);
        s.append("FileLength: ").append(fileLength).append(lineSeparator);
        s.append("Version: ").append(version).append(lineSeparator);
        s.append("ShapeType: ").append(shapeType).append(lineSeparator);
        s.append("xmin: ").append(xmin).append(lineSeparator);
        s.append("ymin: ").append(ymin).append(lineSeparator);
        s.append("xmax: ").append(xmax).append(lineSeparator);
        s.append("ymax: ").append(ymax).append(lineSeparator);
        s.append("zmin: ").append(zmin).append(lineSeparator);
        s.append("zmax: ").append(zmax).append(lineSeparator);
        s.append("mmin: ").append(mmin).append(lineSeparator);
        s.append("mmax: ").append(mmax).append(lineSeparator);
        s.append("------------------------").append(lineSeparator);

        return s.toString();
    }
}

