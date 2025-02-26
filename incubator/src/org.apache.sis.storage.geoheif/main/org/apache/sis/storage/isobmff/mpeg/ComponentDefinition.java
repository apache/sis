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
 * distributed under the License is distributed on an "AS IS" BASIS,z
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.isobmff.mpeg;

import java.io.IOException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.Reader;


/**
 * From ISO 23001-17
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ComponentDefinition extends Box{

    public static final String FCC = "cmpd";

    public static final int CT_MONOCHROME = 0;
    public static final int CT_LUMA_Y = 1;
    public static final int CT_CHROMA_CB_U = 2;
    public static final int CT_CHROMA_CR_V = 3;
    public static final int CT_RED = 4;
    public static final int CT_GREEN = 5;
    public static final int CT_BLUE = 6;
    public static final int CT_ALPHA = 7;
    public static final int CT_DEPTH = 8;
    public static final int CT_DISPARITY = 9;
    public static final int CT_PALETTE = 10;
    public static final int CT_FILTER = 11;
    public static final int CT_PADDED = 12;
    public static final int CT_CYAN = 13;
    public static final int CT_MAGENTA = 14;
    public static final int CT_YELLOW = 15;
    public static final int CT_KEY = 16;

    public int[] componentType;
    public String[] componentTypeURI;

    @Override
    public void readProperties(Reader reader) throws IOException {
        final int componentCount = reader.channel.readInt();
        componentType = new int[componentCount];
        componentTypeURI = new String[componentCount];

        for (int i = 0; i < componentCount; i++) {
            componentType[i] = reader.channel.readUnsignedShort();
            if (componentType[i] >= 0x8000) {
                componentTypeURI[i] = reader.readUtf8String();
            }
        }
    }

}
