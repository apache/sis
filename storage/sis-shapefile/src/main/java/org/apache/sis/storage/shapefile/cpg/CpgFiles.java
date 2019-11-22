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
package org.apache.sis.storage.shapefile.cpg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CPG files utilities.
 * *.cpg files contains a single word for the name of the dbf character encoding.
 *
 * @author Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public final class CpgFiles {

    private CpgFiles(){}

    /**
     * Read charset from given stream.
     *
     * @param in input channel
     * @return CharSet
     * @throws IOException
     */
    public static Charset read(ReadableByteChannel in) throws IOException {
        final String str = toString(Channels.newInputStream(in),Charset.forName("UTF-8")).trim();
        return Charset.forName(str);
    }

    /**
     * Write charset to given file.
     *
     * @param cs charset to write.
     * @param file output file.
     * @throws IOException
     */
    public static void write(Charset cs, Path file) throws IOException {
        try (BufferedWriter cpgWriter = Files.newBufferedWriter(file, Charset.defaultCharset())) {
            cpgWriter.write(cs.name());
        }
    }

    /**
     * Read the contents of a stream into String with specified encoding and close the Stream.
     *
     * @param stream input steam
     * @return The file contents as string
     * @throws IOException if the file does not exist or cannot be read.
     */
    private static String toString(final InputStream stream, final Charset encoding) throws IOException {
        final StringBuilder sb  = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, encoding))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            stream.close();
        }
        return sb.toString();
    }
}
