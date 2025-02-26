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
    public void readProperties(Reader reader) throws IOException {
        if (version == 0 || version == 1) {
            itemId = reader.channel.readUnsignedShort();
            itemProtectionIndex = reader.channel.readUnsignedShort();
            itemName = reader.readUtf8String();
            contentType = reader.readUtf8String();
            contentEncoding = reader.readUtf8String();
        }
        if (version == 1) {
            extensionType = reader.channel.readInt();
            extension = reader.readBox();
        }
        if (version >= 2) {
            if (version == 2) {
                itemId = reader.channel.readUnsignedShort();
            } else if (version == 3) {
                itemId = reader.channel.readInt();
            }
            itemProtectionIndex = reader.channel.readUnsignedShort();
            itemType = intToFourCC(reader.channel.readInt());
            itemName = reader.readUtf8String();
            if ("mime".equals(itemType)) {
                contentType = reader.readUtf8String();
                contentEncoding = reader.readUtf8String();
            } else if ("uri ".equals(itemType)) {
                itemUriType = reader.readUtf8String();
            }
        }
        reader.channel.seek(boxOffset + size);
    }

}
