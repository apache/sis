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
package org.apache.sis.gui;

import java.util.Locale;
import java.util.function.UnaryOperator;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.stage.WindowEvent;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.apache.sis.gui.dataset.LogViewer;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.DataStoreOpener;
import org.apache.sis.internal.gui.io.FileAccessView;
import org.apache.sis.internal.storage.io.ChannelFactory;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Shows the "System monitor" window.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class SystemMonitor implements EventHandler<WindowEvent> {
    /**
     * The provider of wrappers around channels used for reading data.
     * Those wrappers are used for listening to file accesses.
     *
     * @see DataStoreOpener#setFactoryWrapper(UnaryOperator)
     */
    private final UnaryOperator<ChannelFactory> listener;

    /**
     * Creates new event handler.
     */
    private SystemMonitor(final UnaryOperator<ChannelFactory> listener) {
        this.listener = listener;
    }

    /**
     * Invoked when the system monitor window is shown or hidden.
     * This method starts or stops listening to read events on channels.
     */
    @Override
    public void handle(final WindowEvent event) {
        final EventType<WindowEvent> type = event.getEventType();
        UnaryOperator<ChannelFactory> wrapper;
        if (WindowEvent.WINDOW_SHOWN.equals(type)) {
            wrapper = listener;
        } else if (WindowEvent.WINDOW_HIDDEN.equals(type)) {
            wrapper = null;
        } else {
            return;
        }
        DataStoreOpener.setFactoryWrapper(wrapper);
    }

    /**
     * Creates the system monitor window.
     *
     * @param  parent  the parent window.
     * @param  locale  the locale, or {@code null} for default.
     */
    static Stage create(final Stage parent, final Locale locale) {
        final Resources  resources  = Resources.forLocale(locale);
        final Vocabulary vocabulary = Vocabulary.getResources(locale);
        final FileAccessView files = new FileAccessView(resources, vocabulary);
        final LogViewer logging = new LogViewer();
        logging.systemLogs.set(true);
        /*
         * Creates the tab pane.
         */
        final Tab fileTab = new Tab(resources .getString(Resources.Keys.FileAccesses), files.getView());
        final Tab logTab  = new Tab(vocabulary.getString(Vocabulary.Keys.Logs), logging.getView());
        fileTab.setClosable(false);
        logTab .setClosable(false);
        final TabPane panes = new TabPane(fileTab, logTab);
        /*
         * Create the window.
         */
        final Stage w = new Stage();
        w.setTitle(resources.getString(Resources.Keys.SystemMonitor) + " — Apache SIS");
        w.getIcons().setAll(parent.getIcons());
        w.setScene(new Scene(panes));
        w.setMinWidth (400);
        w.setMinHeight(500);        // For preventing logging details to hide logging message table.
        w.setWidth (800);
        w.setHeight(600);
        /*
         * Install listeners.
         */
        final SystemMonitor handler = new SystemMonitor(files);
        w.setOnShown (handler);
        w.setOnHidden(handler);
        parent.setOnHidden((e) -> w.hide());
        return w;
    }
}
