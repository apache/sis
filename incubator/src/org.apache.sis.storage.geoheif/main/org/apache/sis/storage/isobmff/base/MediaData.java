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


/**
 * Container: File
 *
 * @author Johann Sorel (Geomatys)
 */
public final class MediaData extends Box {

    public static final String FCC = "mdat";

    public byte[] getData() throws IOException {
        synchronized (reader) {
            reader.channel.seek(payloadOffset);
            final long nb = (boxOffset + size) - payloadOffset;
            return reader.channel.readBytes(Math.toIntExact(nb));
        }
    }

    public byte[] getData(long offset, int count, byte[] target, int targetOffset) throws IOException {
        synchronized (reader) {
            reader.channel.seek(payloadOffset + offset);
            long nb = (boxOffset + size) - payloadOffset;
            if (count == -1) {
                nb = nb - offset;
            } else {
                if (nb < (offset + count)) {
                    throw new IOException("Trying to read more data then what is available");
                }
                nb = count;
            }
            if (target == null) {
                return reader.channel.readBytes(Math.toIntExact(nb));
            } else {
                reader.channel.readFully(target, targetOffset, Math.toIntExact(nb));
                return target;
            }
        }
    }

}
