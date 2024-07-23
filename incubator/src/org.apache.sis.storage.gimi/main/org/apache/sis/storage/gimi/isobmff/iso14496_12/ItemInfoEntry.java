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
import org.apache.sis.storage.gimi.isobmff.ISOBMFFReader;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ItemInfoEntry extends FullBox {

    public static final String FCC = "infe";

    public int itemId;
    public int itemProtectionIndex;
    public String itemType;
    public String itemName;
    public String contentType;
    public String contentEncoding;
    public int extensionType;
    public Box extension;
    public String itemUriType;

    @Override
    public void readProperties(ChannelDataInput cdi) throws IOException {
        if (version == 0 || version == 1) {
            itemId = cdi.readUnsignedShort();
            itemProtectionIndex = cdi.readUnsignedShort();
            itemName = ISOBMFFReader.readUtf8String(cdi);
            contentType = ISOBMFFReader.readUtf8String(cdi);
            contentEncoding = ISOBMFFReader.readUtf8String(cdi);
        }
        if (version == 1) {
            extensionType = cdi.readInt();
            extension = ISOBMFFReader.readBox(cdi);
        }
        if (version >= 2) {
            if (version == 2) {
                itemId = cdi.readUnsignedShort();
            } else if (version == 3) {
                itemId = cdi.readInt();
            }
            itemProtectionIndex = cdi.readUnsignedShort();
            itemType = intToFourCC(cdi.readInt());
            itemName = ISOBMFFReader.readUtf8String(cdi);
            if ("mime".equals(itemType)) {
                contentType = ISOBMFFReader.readUtf8String(cdi);
                contentEncoding = ISOBMFFReader.readUtf8String(cdi);
            } else if ("uri ".equals(itemType)) {
                itemUriType = ISOBMFFReader.readUtf8String(cdi);
            }
        }
        cdi.seek(boxOffset + size);
    }

}
