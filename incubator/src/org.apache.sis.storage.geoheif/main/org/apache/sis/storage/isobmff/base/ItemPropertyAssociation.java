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
package org.apache.sis.storage.isobmff.base;

import java.io.IOException;
import org.apache.sis.storage.isobmff.Box;
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ItemPropertyAssociation extends FullBox {

    public static final String FCC = "ipma";

    public static class Entry {
        public int itemId;
        public boolean[] essential;
        public int[] propertyIndex;

        @Override
        public String toString() {
            return Box.beanToString(this);
        }
    }

    public Entry[] entries;

    @Override
    public void readProperties(Reader reader) throws IOException {
        entries = new Entry[reader.channel.readInt()];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new Entry();
            if (version < 1) {
                entries[i].itemId = reader.channel.readUnsignedShort();
            } else {
                entries[i].itemId = reader.channel.readInt();
            }
            entries[i].essential = new boolean[reader.channel.readUnsignedByte()];
            entries[i].propertyIndex = new int[entries[i].essential.length];
            for (int k = 0; k < entries[i].essential.length; k++) {
                entries[i].essential[k] = reader.channel.readBit() == 1;
                if ((flags & 1) != 0) {
                    entries[i].propertyIndex[k] = (int) reader.channel.readBits(15);
                } else {
                    entries[i].propertyIndex[k] = (int) reader.channel.readBits(7);
                }
            }
        }
    }

}
