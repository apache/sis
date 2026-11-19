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
package org.apache.sis.storage.hdf.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Deflate filter (filter identifier 1) : chunk bytes are compressed with the
 * zlib format, see <a href="https://www.rfc-editor.org/rfc/rfc1950">RFC 1950</a>.
 *
 * @author Johann Sorel (Geomatys)
 */
public class Deflate implements Filter {

    /** Creates a new deflate filter, encoding with the default compression level. */
    public Deflate() {
    }

    @Override
    public byte[] decode(byte[] chunkdata) throws IOException {
        if (chunkdata.length == 0) {
            return chunkdata;
        }

        try (InputStream is = new InflaterInputStream(new ByteArrayInputStream(chunkdata, 0, chunkdata.length), new Inflater(false));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[4096];
            for(;;) {
                final int nb = is.read(buffer, 0, buffer.length);
                if (nb == -1) break;
                out.write(buffer, 0, nb);
            }
            return out.toByteArray();
        }
    }

    @Override
    public byte[] encode(byte[] chunkdata) throws IOException {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
             final DeflaterOutputStream os = new DeflaterOutputStream(out)) {
            os.write(chunkdata);
            os.finish();
            return out.toByteArray();
        }
    }

}
