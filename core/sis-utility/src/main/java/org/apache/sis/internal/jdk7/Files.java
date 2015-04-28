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
package org.apache.sis.internal.jdk7;

import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ByteChannel;

/**
 * Place holder for {@link java.nio.file.Files}.
 * This class exists only on the JDK6 branch of SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class Files {
    /**
     * Do not allow instantiation of this class.
     */
    private Files() {
    }

    /**
     * Simulates {@link java.nio.file.Files#newByteChannel(java.nio.file.Path, java.nio.file.OpenOption[])}.
     *
     * @param  file    The file to open.
     * @param  options The name of {@code OpenOption} values.
     * @return The channel.
     * @throws IOException If an error occurred while opening the channel.
     */
    public static ByteChannel newByteChannel(final File file, final Set<?> options) throws IOException {
        String mode = "r";
        if (options != null) {
            if (options.contains(StandardOpenOption.DSYNC)) {
                mode = "rwd";
            } else if (options.contains(StandardOpenOption.SYNC)) {
                mode = "rws";
            } else if (options.contains(StandardOpenOption.WRITE)) {
                mode = "rw";
            }
        }
        return new RandomAccessFile(file, mode).getChannel();
    }
}
