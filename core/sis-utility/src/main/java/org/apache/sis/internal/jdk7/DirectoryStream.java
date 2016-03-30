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

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;


/**
 * Place holder for {@link java.nio.file.DirectoryStream}.
 * This class exists only on the JDK6 branch of SIS.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final class DirectoryStream implements Closeable, Iterable<Path>, FileFilter {
    /**
     * The directory of files to be returned by the iterator.
     */
    private final File directory;

    /**
     * The pattern to match.
     */
    private final Pattern pattern;

    /**
     * Creates a new stream for the given directory.
     */
    DirectoryStream(final File directory, String glob) {
        this.directory = directory;
        glob = glob.replace(".", "\\.");
        glob = glob.replace("?", ".");
        glob = glob.replace("*", ".*");
        pattern = Pattern.compile(glob);
    }

    /**
     * Returns an iterator over the files in the directory.
     */
    @Override
    public Iterator<Path> iterator() {
        final File[] files = directory.listFiles(this);
        final Path[] paths = new Path[files.length];
        for (int i=0; i<files.length; i++) {
            paths[i] = Path.castOrCopy(files[i]);
        }
        return Arrays.asList(paths).iterator();
    }

    /**
     * Public as a side effect.
     */
    @Override
    public boolean accept(final File pathname) {
        return pattern.matcher(pathname.getName()).matches();
    }

    /**
     * No-op.
     */
    @Override
    public void close() throws IOException {
    }
}
