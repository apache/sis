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
import java.util.Arrays;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.storage.gimi.isobmff.Box;

/**
 * Container: MetaBox
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ItemData extends Box {

    public static final String FCC = "idat";

    public byte[] data;

    @Override
    public void readProperties(ChannelDataInput cdi) throws IOException {
        final long nb = (boxOffset + size) - payloadOffset;
        data = cdi.readBytes(Math.toIntExact(nb));
    }

    @Override
    protected String propertiesToString() {
        byte[] data = this.data.length < 20 ? this.data : Arrays.copyOf(this.data, 20);
        return "rawData : " + Arrays.toString(data) +" ... \nasString : " + new String(data) ;
    }

}
