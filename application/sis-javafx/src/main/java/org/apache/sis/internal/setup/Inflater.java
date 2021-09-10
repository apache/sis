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
package org.apache.sis.internal.setup;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JProgressBar;


/**
 * Decompress the ZIP file for JavaFX in a background thread.
 *
 * <p><b>Design note:</b> we do not use {@link javax.swing.SwingWorker} because that classes
 * is more expansive than what we need. For example it creates a pool of 10 threads while we
 * need only one.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class Inflater implements Runnable {
    /**
     * The wizard to notify about completion of failure.
     */
    private final Wizard wizard;

    /**
     * The zip file to decompress.
     */
    private final File source;

    /**
     * The directory where the ZIP file is decompressed.
     */
    private File destination;

    /**
     * The {@linkplain #destination} directory, plus the first subdirectory in
     * the ZIP file that starts with {@value FXFinder#JAVAFX_DIRECTORY_PREFIX}.
     */
    private File subdir;

    /**
     * If decompression failed, the cause. Otherwise {@code null}.
     */
    private Exception failure;

    /**
     * Whether this task is cancelled.
     */
    private volatile boolean cancelled;

    /**
     * Creates a new inflater for the specified ZIP file.
     */
    Inflater(final Wizard wizard, final File source) {
        this.wizard = wizard;
        this.source = source;
    }

    /**
     * The task to be executed in a background {@link Thread}.
     * This method dispatches the work to {@link #doInBackground()} and {@link #done()} methods.
     */
    @Override
    public synchronized void run() {
        try {
            doInBackground();
        } catch (Exception e) {
            failure = e;
            delete(destination);
        }
        EventQueue.invokeLater(this::done);
    }

    /**
     * Decompresses the JavaFX ZIP file.
     */
    private void doInBackground() throws Exception {
        destination = wizard.javafxFinder.getDestinationDirectory();
        final JProgressBar progressBar = wizard.inflateProgress;
        final byte[] buffer = new byte[65536];
        try (ZipFile zip = new ZipFile(source)) {
            final int size = zip.size();
            EventQueue.invokeAndWait(() -> progressBar.setMaximum(size));
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            int progressValue = 0;
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                final File file = new File(destination, entry.getName());
                if (entry.isDirectory()) {
                    if (!file.isDirectory() && !file.mkdir()) {
                        throw new IOException("Directory can not be created: " + file);
                    }
                    if (subdir == null && entry.getName().startsWith(FXFinder.JAVAFX_DIRECTORY_PREFIX)) {
                        subdir = file;
                    }
                } else {
                    try (InputStream  in  = zip.getInputStream(entry);
                         OutputStream out = new FileOutputStream(file))     // No need for buffered streams here.
                    {
                        int n;
                        while ((n = in.read(buffer)) >= 0) {
                            if (cancelled) return;
                            out.write(buffer, 0, n);
                        }
                    }
                }
                final int p = progressValue++;
                EventQueue.invokeLater(() -> progressBar.setValue(p));
            }
        }
    }

    /**
     * Invoked in Swing thread after the decompression is done, either successfully or on failure.
     * Note that {@link #cancelled} may be {@code true} only if {@link #cancel()} has been invoked,
     * in which case this method does nothing because the files will be deleted by {@code cancel()}
     * and the system will exit.
     */
    private void done() {
        if (!cancelled) {
            if (subdir == null) subdir = destination;
            wizard.decompressionFinished(subdir, failure);
        }
    }

    /**
     * Stops the thread if it is running, then delete the files.
     * This method is invoked by {@link FXFinder} just before {@link System#exit(int)}.
     */
    final void cancel() {
        cancelled = true;
        synchronized (this) {               // Wait for background thread to finish.
            delete(destination);
        }
    }

    /**
     * Deletes a directory and all its content recursively.
     */
    private static void delete(final File directory) {
        if (directory != null) {
            final File[] content = directory.listFiles();
            if (content != null) {
                for (final File file : content) {
                    delete(file);
                }
            }
            directory.delete();
        }
    }
}
