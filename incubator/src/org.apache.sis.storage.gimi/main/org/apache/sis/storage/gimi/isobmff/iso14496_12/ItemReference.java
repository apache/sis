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
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.gimi.isobmff.Box;
import org.apache.sis.storage.gimi.isobmff.FullBox;
import org.apache.sis.storage.gimi.isobmff.ISOBMFFReader;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ItemReference extends FullBox {

    public static final String FCC = "iref";

    public List<Box> references;

    @Override
    public void readProperties(ChannelDataInput cdi) throws IOException {
        references = new ArrayList<>();

        while (cdi.getStreamPosition() < boxOffset+size) {
            final Box box = ISOBMFFReader.readBox(cdi);
            box.readPayload(cdi);
            cdi.seek(box.boxOffset + box.size);
            references.add(box);
        }

        if (version == 0) {

        } else if (version == 1) {

        }
    }

    @Override
    protected String propertiesToString() {
        final StringBuilder sb = new StringBuilder();
        if (references != null) {
            sb.append("references :");
            for (Box b : references) {
                sb.append(b);
            }
        }
        return sb.toString();
    }

}
