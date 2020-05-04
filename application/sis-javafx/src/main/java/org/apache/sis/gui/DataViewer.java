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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.sis.gui.dataset.ResourceExplorer;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.RecentChoices;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Entry point for Apache SIS application.
 * Current implementation shows a {@link ResourceExplorer} on which user can drop the files to open.
 * The content shown by this {@code Main} class may change in any future Apache SIS version.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public class DataViewer extends Application {
    /**
     * Starts the Apache SIS application.
     *
     * @param args  ignored.
     */
    public static void main(final String[] args) {
        launch(args);
    }

    /**
     * The primary stage onto which the application scene is set.
     */
    private Stage window;

    /**
     * The main content of this application. For now this is the metadata viewer.
     * In a future version it may be another component.
     */
    private ResourceExplorer content;

    /**
     * The file filters to use in the dialog box shown by the "File" ▶ "Open" menu.
     * This array is created together with {@link #saveFilters} when first needed.
     *
     * @see #createFileFilters()
     */
    private FileChooser.ExtensionFilter[] openFilters;

    /**
     * The file filters to use in the dialog box shown by the "File" ▶ "Save" menu.
     * This array is created together with {@link #openFilters} when first needed.
     *
     * @see #createFileFilters()
     */
    private FileChooser.ExtensionFilter[] saveFilters;

    /**
     * The last filter used by the {@link #open(ActionEvent)} action.
     */
    private FileChooser.ExtensionFilter lastFilter;

    /**
     * Creates a new Apache SIS application.
     */
    public DataViewer() {
    }

    /**
     * Invoked by JavaFX for starting the application.
     * This method is called on the JavaFX Application Thread.
     *
     * @param window  the primary stage onto which the application scene will be be set.
     */
    @Override
    public void start(final Stage window) {
        this.window = window;
        content = new ResourceExplorer();
        final Resources  localized  = Resources.getInstance();
        final Vocabulary vocabulary = Vocabulary.getResources(localized.getLocale());
        /*
         * Configure the menu bar. For all menu items except simple ones, the action is
         * to invoke a method of the same name in this application class (e.g. open(…)).
         */
        final MenuBar menus = new MenuBar();
        final Menu file = new Menu(vocabulary.getString(Vocabulary.Keys.File));
        {   // For keeping variables locale.
            final MenuItem open;
            file.getItems().addAll(
                    open = localized.menu(Resources.Keys.Open, this::open),
                    new SeparatorMenuItem(),
                    localized.menu(Resources.Keys.Exit, (event) -> Platform.exit()));

            open.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
        }
        final Menu windows = new Menu(localized.getString(Resources.Keys.Windows));
        windows.getItems().add(content.createNewWindowMenu());
        content.setWindowsItems(windows.getItems());
        menus.getMenus().addAll(file, windows);
        /*
         * Set the main content and show.
         */
        final BorderPane pane = new BorderPane();
        pane.setTop(menus);
        pane.setCenter(content.getView());
        final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        window.setTitle("Apache Spatial Information System");
        window.getIcons().addAll(new Image(DataViewer.class.getResourceAsStream("SIS_64px.png")),
                                 new Image(DataViewer.class.getResourceAsStream("SIS_128px.png")));
        window.setScene(new Scene(pane));
        window.setWidth (0.75 * bounds.getWidth());
        window.setHeight(0.75 * bounds.getHeight());
        window.show();
    }

    /**
     * Creates the file filters for the dialog box to shown in "File" ▶ "Open" and "File" ▶ "Save" menus.
     *
     * @todo Iterate only over the classes in JDK9, without initializing the providers.
     */
    private void createFileFilters() {
        final Resources res = Resources.getInstance();
        final Set<String> allSuffixes = new LinkedHashSet<>();
        final List<FileChooser.ExtensionFilter> open = new ArrayList<>();
        final List<FileChooser.ExtensionFilter> save = new ArrayList<>();
        /*
         * Add an "All files (*.*)" filter only for the Open action.
         * The Save action will need to specify a specific filter.
         */
        open.add(new FileChooser.ExtensionFilter(res.getString(Resources.Keys.AllFiles), "*.*"));
        for (DataStoreProvider provider : DataStores.providers()) {
            final StoreMetadata md = provider.getClass().getAnnotation(StoreMetadata.class);
            if (md != null) {
                final String[] suffixes = md.fileSuffixes();
                if (suffixes.length != 0) {
                    final boolean canOpen = ArraysExt.contains(md.capabilities(), Capability.READ);
                    final boolean canSave = ArraysExt.contains(md.capabilities(), Capability.WRITE);
                    for (int i=0; i < suffixes.length; i++) {
                        final String fs = "*.".concat(suffixes[i]);
                        suffixes[i] = fs;
                        if (canOpen) {
                            allSuffixes.add(fs);
                        }
                    }
                    final FileChooser.ExtensionFilter f = new FileChooser.ExtensionFilter(
                                    md.formatName() + " (" + suffixes[0] + ')', suffixes);
                    if (canOpen) open.add(f);
                    if (canSave) save.add(f);
                }
            }
        }
        /*
         * Add a filter for all geospatial files in second position, after "All files" and before
         * the filters for specific formats. This will be the default filter for the "Open" action.
         */
        open.add(1, new FileChooser.ExtensionFilter(res.getString(Resources.Keys.GeospatialFiles),
                            allSuffixes.toArray(new String[allSuffixes.size()])));
        this.openFilters = open.toArray(new FileChooser.ExtensionFilter[open.size()]);
        this.saveFilters = save.toArray(new FileChooser.ExtensionFilter[save.size()]);
    }

    /**
     * Invoked when the user selects "File" ▶ "Open" menu.
     * Users can select an arbitrary amount of files or directories.
     * The effect is the same as dragging the files in the "resources tree" window.
     *
     * @param  event  ignored (can be {@code null}).
     */
    private void open(final ActionEvent event) {
        if (openFilters == null) {
            createFileFilters();
            lastFilter = openFilters[1];
        }
        final FileChooser chooser = new FileChooser();
        chooser.setTitle(Resources.format(Resources.Keys.OpenDataFile));
        chooser.getExtensionFilters().addAll(openFilters);
        chooser.setSelectedExtensionFilter(lastFilter);
        chooser.setInitialDirectory(RecentChoices.getOpenDirectory());
        final List<File> files = chooser.showOpenMultipleDialog(window);
        if (files != null) {
            RecentChoices.setOpenDirectory(files);
            lastFilter = chooser.getSelectedExtensionFilter();
            content.loadResources(files);
        }
    }

    /**
     * Invoked when the application should stop. No SIS application can be used after
     * this method has been invoked (i.e. the application can not be restarted).
     *
     * @throws Exception if an error occurred, for example while closing a data store.
     */
    @Override
    public void stop() throws Exception {
        BackgroundThreads.stop();
        RecentChoices.saveReferenceSystems();
        super.stop();
    }
}
