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
package org.apache.sis.storage.gimi.isobmff.iso14496_12;

import java.io.IOException;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.FullBox;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ItemLocation extends FullBox {

    public static final String FCC = "iloc";

    public int offsetSize;
    public int lengthSize;
    public int baseOffsetSize;
    public int indexSize;
    public int itemCount;
    public Item[] items;

    public static class Item {
        public int itemID;
        public int constructionMethod;
        public int dataReferenceIndex;
        public long baseOffset;
        public int extentCount;
        public int[] itemReferenceIndex;
        public int[] extentOffset;
        public int[] extentLength;

        @Override
        public String toString() {
            return Box.beanToString(this);
        }
    }

    @Override
    public void readProperties(ChannelDataInput cdi) throws IOException {
        offsetSize = (int) cdi.readBits(4);
        lengthSize = (int) cdi.readBits(4);
        baseOffsetSize = (int) cdi.readBits(4);
        if (version == 1 || version == 2) {
            indexSize = (int) cdi.readBits(4);
        } else {
            cdi.readBits(4);
        }
        if (version < 2) {
            itemCount = cdi.readUnsignedShort();
        } else if (version == 2) {
            itemCount = cdi.readInt();
        }
        items = new Item[itemCount];
        for (int i = 0; i < itemCount; i++) {
            items[i] = new Item();
            if (version < 2) {
                items[i].itemID = cdi.readUnsignedShort();
            } else if (version == 2) {
                items[i].itemID = cdi.readInt();
            }
            if (version == 1 || version == 2) {
                cdi.readBits(12);
                items[i].constructionMethod = (int) cdi.readBits(4);
            }
            items[i].dataReferenceIndex = cdi.readUnsignedShort();
            items[i].baseOffset = cdi.readBits(baseOffsetSize*8);
            items[i].extentCount = cdi.readUnsignedShort();
            items[i].itemReferenceIndex = new int[items[i].extentCount];
            items[i].extentOffset = new int[items[i].extentCount];
            items[i].extentLength = new int[items[i].extentCount];
            for (int k = 0; k < items[i].extentCount; k++) {
                if ((version == 1 || version == 2) && indexSize >0) {
                    items[i].itemReferenceIndex[k] = (int) cdi.readBits(indexSize*8);
                }
                items[i].extentOffset[k] = (int) cdi.readBits(offsetSize*8);
                items[i].extentLength[k] = (int) cdi.readBits(lengthSize*8);
            }
        }
    }

}
