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
package org.apache.sis.gui.internal.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.UnaryOperator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.application.Platform;
import org.apache.sis.gui.Widget;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.gui.internal.FixedHeaderColumnSize;
import org.apache.sis.gui.internal.ImmutableObjectProperty;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.io.stream.ChannelFactory;
import org.apache.sis.io.stream.FileCacheByteChannel;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreListeners;


/**
 * A table of filenames associated with bars showing which parts of the files have been read or written.
 * New rows are added when files are opened and removed when files are closed.
 *
 * <p>The {@link org.apache.sis.gui.SystemMonitor} class will track only channels opened while the
 * "System monitor" window was visible. This policy is for avoiding the cost of tracking operations
 * in the vast majority of cases when user has no interest in those information.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class FileAccessView extends Widget implements UnaryOperator<ChannelFactory> {
    /**
     * The table where opened files are listed.
     */
    private final TableView<FileAccessItem> table;

    /**
     * Creates a new widget.
     *
     * @param  resources   localized resources, provided because already known by caller.
     * @param  vocabulary  localized resources, provided because already known by caller.
     */
    @SuppressWarnings("unchecked")      // Generic array construction.
    public FileAccessView(final Resources resources, final Vocabulary vocabulary) {
        final TableColumn<FileAccessItem, String> filenameColumn;
        final TableColumn<FileAccessItem,Pane> accessColumn;
        filenameColumn = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.File));
        accessColumn   = new TableColumn<>(resources.getString(Resources.Keys.AccessedRegions));
        accessColumn  .setSortable(false);
        accessColumn  .setMinWidth (120);
        filenameColumn.setMinWidth ( 80);
        filenameColumn.setPrefWidth(200);
        filenameColumn.setCellValueFactory((cell) -> new ImmutableObjectProperty<>(cell.getValue().filename));
        accessColumn  .setCellValueFactory((cell) -> new ImmutableObjectProperty<>(cell.getValue().accessView));
        table = new TableView<>();
        table.setColumnResizePolicy(FixedHeaderColumnSize.INSTANCE);
        table.getColumns().setAll(filenameColumn, accessColumn);
    }

    /**
     * Returns the node to show in a window.
     *
     * @return the node to show.
     */
    @Override
    public Region getView() {
        return table;
    }

    /**
     * Invoked when a new {@link ReadableByteChannel} or {@link WritableByteChannel} is about to be created.
     * The caller will replace the given factory by the returned factory. It allows us to wrap the channel
     * in an object which will collect information about blocks read.
     *
     * @param  factory  the factory for creating channels.
     * @return the factory to use instead of the factory given in argument.
     */
    @Override
    public ChannelFactory apply(final ChannelFactory factory) {
        return new ChannelFactory(factory.suggestDirectBuffer) {
            /**
             * Returns whether using the streams or channels will affect the original {@code storage} object.
             */
            @Override
            public boolean isCoupled() {
                return factory.isCoupled();
            }

            /**
             * Returns {@code true} if this factory is capable to create another readable byte channel.
             */
            @Override
            public boolean canReopen() {
                return factory.canReopen();
            }

            /**
             * Returns {@code true} if opening the channel will create a new, initially empty, file.
             */
            @Override
            public boolean isCreateNew() throws IOException {
                return factory.isCreateNew();
            }

            /**
             * Returns a new visual item and adds it as a row in the table of opened files.
             * This method can be invoked from any thread (usually not the JavaFX one).
             *
             * @param  filename  data store name.
             * @return the view of the row added in the table.
             */
            private FileAccessItem newItem(final String filename) {
                final FileAccessItem item = new FileAccessItem(table.getItems(), filename);
                Platform.runLater(() -> item.owner.add(item));
                return item;
            }

            /**
             * Returns the readable channel as an input stream.
             *
             * @param  filename  data store name.
             * @param  listeners set of registered {@code StoreListener}s for the data store, or {@code null} if none.
             * @return the input stream.
             * @throws DataStoreException if the channel is read-once.
             * @throws IOException if the input stream or its underlying byte channel cannot be created.
             */
            @Override
            public InputStream inputStream(final String filename, final StoreListeners listeners)
                    throws DataStoreException, IOException
            {
                final InputStream input = factory.inputStream(filename, listeners);
                return newItem(filename).new InputObserver(input);
            }

            /**
             * Creates a readable channel and listens (if possible) read operations.
             * Current implementation listens only to {@link SeekableByteChannel}
             * because otherwise we do not know the file size.
             *
             * @param  filename  data store name.
             * @param  listeners set of registered {@code StoreListener}s for the data store, or {@code null} if none.
             * @return the channel for the given input.
             * @throws DataStoreException if the channel is read-once.
             * @throws IOException if an error occurred while opening the channel.
             */
            @Override
            public ReadableByteChannel readable(final String filename, final StoreListeners listeners)
                    throws DataStoreException, IOException
            {
                final ReadableByteChannel channel = factory.readable(filename, listeners);
                if (channel instanceof SeekableByteChannel) {
                    final FileAccessItem item = newItem(filename);
                    if (channel instanceof FileCacheByteChannel) {
                        ((FileCacheByteChannel) channel).setFilter((input, start, end) ->
                                item.new InputObserver(input, start));
                    }
                    return item.new Observer((SeekableByteChannel) channel);
                }
                return channel;
            }

            /**
             * Forwards to the factory (listeners not yet implemented).
             */
            @Override
            public WritableByteChannel writable(final String filename, final StoreListeners listeners)
                    throws DataStoreException, IOException
            {
                return factory.writable(filename, listeners);
            }
        };
    }
}
