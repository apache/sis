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
package org.apache.sis.gui.dataset;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.nio.file.Path;
import java.io.File;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TreeCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;


/**
 * The "Copy file path" action. This class gets the file path of a resource and copies it in the clipboard.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class CopyAction implements EventHandler<ActionEvent> {
    /**
     * The cell for which to provide a copy action.
     */
    private final TreeCell<Resource> cell;

    /**
     * Creates a new instance.
     */
    CopyAction(final TreeCell<Resource> source) {
        this.cell = source;
    }

    /**
     * Invoked when the user selected "Copy file path" item in the contextual menu of a {@link ResourceTree} cell.
     * This method copies the path of the selected resource to the clipboard.
     */
    @Override
    public void handle(final ActionEvent event) {
        final Resource resource = cell.getItem();
        final Object path;
        try {
            path = URIDataStore.location(resource);
        } catch (DataStoreException e) {
            ExceptionReporter.show(cell, null, null, e);
            return;
        }
        /*
         * The file path can be given in two forms: as an URI or as a text (named "file" below).
         * The textual form is the one that will usually be pasted. We try to provide paths on
         * the local file system if possible, converting "file:///" URI if needed. Only if URI
         * can not be converted to a local file path, we keep the URI form.
         *
         * The `uri` is determined in a way opposite to `file`: we convert local path to URI.
         * That form is provided in case the path is pasted in applications expecting URI.
         */
        Object file = path;
        Object uri  = null;
        try {
            if (path instanceof URI) {
                uri  = path;                            // Must be first because next line may fail.
                file = new File((URI) path);
            } else if (path instanceof URL) {
                uri  = path;                            // Must be first because next line may fail.
                file = new File(((URL) path).toURI());
            } else if (path instanceof File) {
                uri  = ((File) path).toURI();
            } else if (path instanceof Path) {
                uri  = ((Path) path).toUri();           // Must be first because next line may fail.
                file = ((Path) path).toFile();
            } else if (path instanceof CharSequence) {
                uri  = new URI(path.toString());        // Stay null if this operation fail.
            } else {
                file = "";
            }
        } catch (URISyntaxException | IllegalArgumentException | UnsupportedOperationException e) {
            // Ignore. The `uri` or `text` field that we failed to assign keep its original value.
        }
        /*
         * Above code obtained a single path, considered the main one. But a resource may also be
         * associated with many files (some kinds of data are actually provided as a group of files).
         * We put in the clipboard all `java.io.File` instances that we can get from the resource.
         * This list of files will usually be ignored and only the `file` text will be pasted,
         * but it depends on the application where files will be pasted.
         */
        List<File> files = null;
        if (resource instanceof ResourceOnFileSystem) try {
            final Path[] components = ((ResourceOnFileSystem) resource).getComponentFiles();
            if (components != null) {
                files = new ArrayList<>(components.length);
                for (final Path p : components) try {
                    if (p != null) files.add(p.toFile());
                } catch (UnsupportedOperationException e) {
                    // Ignore and try to add other components.
                }
            }
        } catch (DataStoreException e) {
            ResourceTree.unexpectedException("copy", e);
        } else if (file instanceof File) {
            files = Collections.singletonList((File) file);
        }
        /*
         * Put in the clipboard all information that we could get.
         */
        final ClipboardContent content = new ClipboardContent();
        content.putString(file.toString());
        if (files != null) content.putFiles(files);
        if (uri   != null) content.putUrl(uri.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }
}
