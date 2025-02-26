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
import org.apache.sis.storage.isobmff.FullBox;
import org.apache.sis.storage.isobmff.Reader;


/**
 * Container: File,Segment,MovieBox,TrackBox,MovieFragmentBox,TrackFragmentBox
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ItemInfo extends FullBox {

    public static final String FCC = "iinf";

    public ItemInfoEntry[] entries;

    @Override
    public void readProperties(Reader reader) throws IOException {
        final int entryCount = version == 0 ? reader.channel.readUnsignedShort() : reader.channel.readInt();

        entries = new ItemInfoEntry[entryCount];
        for (int i = 0; i < entryCount; i++) {
            entries[i] = (ItemInfoEntry) reader.readBox();
        }
    }

}
