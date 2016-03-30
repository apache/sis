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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;


/**
 * Place holder for {@link java.nio.file.Files}.
 * This class exists only on the JDK6 branch of SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
public final class Files {
    /**
     * Do not allow instantiation of this class.
     */
    private Files() {
    }

    /**
     * Creates the directory at the given path.
     * This method does not create parent directories.
     *
     * @param dir The directory to create.
     * @return The given path.
     * @throws IOException if the directory can not be created.
     *
     * @since 0.7
     */
    public static Path createDirectory(final Path dir) throws IOException {
        if (!dir.mkdir()) {
            throw new IOException("Can not create directory: " + dir);
        }
        return dir;
    }

    /**
     * Returns {@code true} if the given path is a directory.
     *
     * @param path The path to test.
     * @return {@code true} if the path is a directory.
     *
     * @since 0.7
     */
    public static boolean isDirectory(final Path path) {
        return path.isDirectory();
    }

    /**
     * Returns {@code true} if the given path is an ordinary file.
     *
     * @param path The path to test.
     * @return {@code true} if the path is a file.
     *
     * @since 0.7
     */
    public static boolean isRegularFile(final Path path) {
        return path.isFile();
    }

    /**
     * Returns {@code true} if the file at the given path can be read.
     *
     * @param path The path to test.
     * @return {@code true} if file at the given path can be read.
     *
     * @since 0.7
     */
    public static boolean isReadable(final Path path) {
        return path.canRead();
    }

    /**
     * Returns {@code true} if the file at the given path can be written.
     *
     * @param path The path to test.
     * @return {@code true} if file at the given path can be written.
     *
     * @since 0.7
     */
    public static boolean isWritable(final Path path) {
        return path.canWrite();
    }

    /**
     * Returns {@code true} if the given file or directory exits.
     *
     * @param path The path to test.
     * @return {@code true} if file or directory exists.
     *
     * @since 0.7
     */
    public static boolean exists(final Path path) {
        return path.exists();
    }

    /**
     * Returns an iterable over the content of the given directory.
     *
     * @param  dir   The directory.
     * @param  glob  The pattern to match.
     * @return Iterable over the given directory.
     * @throws IOException if an error occurred while creating the iterable.
     */
    public static DirectoryStream newDirectoryStream(Path dir, String glob) throws IOException {
        return new DirectoryStream(dir, glob);
    }

    /**
     * Creates a new input stream. The input stream is intentionally not buffered;
     * it is caller's responsibility to provide buffering.
     *
     * @param path The path of the file to read.
     * @return The input stream.
     * @throws IOException if an error occurred while creating the input stream.
     *
     * @since 0.7
     */
    public static InputStream newInputStream(final Path path) throws IOException {
        return new FileInputStream(path);
    }

    /**
     * Creates a new buffered reader for the given character encoding.
     *
     * @param path The path of the file to read.
     * @param cs The character encoding to use.
     * @return The reader.
     * @throws IOException if an error occurred while creating the reader.
     *
     * @since 0.7
     */
    public static BufferedReader newBufferedReader(final Path path, final Charset cs) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(path), cs.newDecoder()));
    }

    /**
     * Creates a new buffered writer for the given character encoding.
     *
     * @param path The path of the file to write.
     * @param cs The character encoding to use.
     * @return The writer.
     * @throws IOException if an error occurred while creating the writer.
     *
     * @since 0.7
     */
    public static BufferedWriter newBufferedWriter(final Path path, final Charset cs) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), cs.newEncoder()));
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

    /**
     * Creates a new byte channel.
     *
     * @param  path    The file to open.
     * @return The channel.
     * @throws IOException If an error occurred while opening the channel.
     */
    public static ByteChannel newByteChannel(final Path path) throws IOException {
        return newByteChannel(path, null);
    }
}
