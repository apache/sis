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
    private int m_fileCode; // big

    /** File length. */
    private int m_fileLength; // big // The value for file length is the total length of the file in 16-bit words

    /** File version. */
    private int m_version; // little

    /** Shapefile type. */
    private ShapeTypeEnum m_shapeType; // little

    /** X Min. */
    private double m_xmin; // little

    /** Y Min. */
    private double m_ymin; // little

    /** X Max. */
    private double m_xmax; // little

    /** Y Max. */
    private double m_ymax; // little

    /** Z Min. */
    private double m_zmin; // little

    /** Z Max. */
    private double m_zmax; // little

    /** M Min. */
    private double m_mmin; // little

    /** M Max. */
    private double m_mmax; // little
    
    /**
     * Create a shapefile descriptor.
     * @param byteBuffer Source Bytebuffer.
     */
    public ShapefileDescriptor(MappedByteBuffer byteBuffer) {
        m_fileCode = byteBuffer.getInt();
        byteBuffer.getInt();
        byteBuffer.getInt();
        byteBuffer.getInt();
        byteBuffer.getInt();
        byteBuffer.getInt();
        m_fileLength = byteBuffer.getInt() * 2;

        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        m_version = byteBuffer.getInt();
        m_shapeType = ShapeTypeEnum.get(byteBuffer.getInt());
        m_xmin = byteBuffer.getDouble();
        m_ymin = byteBuffer.getDouble();
        m_xmax = byteBuffer.getDouble();
        m_ymax = byteBuffer.getDouble();
        m_zmin = byteBuffer.getDouble();
        m_zmax = byteBuffer.getDouble();
        m_mmin = byteBuffer.getDouble();
        m_mmax = byteBuffer.getDouble();
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

        s.append("FileCode: ").append(m_fileCode).append(lineSeparator);
        s.append("FileLength: ").append(m_fileLength).append(lineSeparator);
        s.append("Version: ").append(m_version).append(lineSeparator);
        s.append("ShapeType: ").append(m_shapeType).append(lineSeparator);
        s.append("xmin: ").append(m_xmin).append(lineSeparator);
        s.append("ymin: ").append(m_ymin).append(lineSeparator);
        s.append("xmax: ").append(m_xmax).append(lineSeparator);
        s.append("ymax: ").append(m_ymax).append(lineSeparator);
        s.append("zmin: ").append(m_zmin).append(lineSeparator);
        s.append("zmax: ").append(m_zmax).append(lineSeparator);
        s.append("mmin: ").append(m_mmin).append(lineSeparator);
        s.append("mmax: ").append(m_mmax).append(lineSeparator);
        s.append("------------------------").append(lineSeparator);

        return s.toString();
    }
}

