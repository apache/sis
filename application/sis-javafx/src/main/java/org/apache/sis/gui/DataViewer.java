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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.awt.SplashScreen;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.sis.gui.dataset.ResourceExplorer;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.LogHandler;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.internal.gui.RecentChoices;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStore;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Entry point for Apache SIS application.
 * Current implementation shows a {@link ResourceExplorer} on which user can drop the files to open.
 * The content shown by this {@code Main} class may change in any future Apache SIS version.
 *
 * @author  Smaniotto Enzo (GSoC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.1
 * @module
 */
public class DataViewer extends Application {
    /**
     * The application being executed, or {@code null} if none.
     */
    private static volatile DataViewer running;

    /**
     * Starts the Apache SIS application.
     *
     * @param args  ignored.
     */
    public static void main(final String[] args) {
        LogHandler.register(true);
        /*
         * Following line seems necessary for enabling input method framework
         * (tested on Java 14 and JavaFX 14).
         */
        java.awt.im.InputContext.getInstance();
        launch(DataViewer.class, args);
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
     * The last filter used by the {@link #showOpenFileDialog()} action.
     */
    private FileChooser.ExtensionFilter lastFilter;

    /**
     * The window showing system logs. Created when first requested.
     *
     * @see #showSystemMonitorWindow()
     */
    private Stage systemLogsWindow;

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
            final MenuItem open, oUrl, close;
            final Menu recentFiles = RecentFiles.create(content, localized);
            file.getItems().addAll(
                    open  = localized.menu(Resources.Keys.Open,    (e) -> showOpenFileDialog()),
                    oUrl  = localized.menu(Resources.Keys.OpenURL, (e) -> showOpenURLDialog()), recentFiles,
                    close = localized.menu(Resources.Keys.Close,   (e) -> closeSelectedFile()),
                    new SeparatorMenuItem(),
                    localized.menu(Resources.Keys.Exit, (e) -> Platform.exit()));

            open.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
            oUrl.setAccelerator(KeyCombination.keyCombination("Shortcut+U"));
            close.setDisable(true);
            content.selectedResourceProperty().addListener((e,o,n) -> {
                close.setDisable(!(n instanceof DataStore));
            });
        }
        final Menu windows = new Menu(localized.getString(Resources.Keys.Windows));
        {
            final ObservableList<MenuItem> items = windows.getItems();
            final MenuItem monitor = new MenuItem(localized.getString(Resources.Keys.SystemMonitor));
            monitor.setOnAction((e) -> showSystemMonitorWindow());
            items.addAll(monitor);
        }
        final Menu help = new Menu(localized.getString(Resources.Keys.Help));
        {   // For keeping variables locale.
            help.getItems().addAll(
                    localized.menu(Resources.Keys.WebSite, (e) -> getHostServices().showDocument("https://sis.apache.org/javafx.html")),
                    localized.menu(Resources.Keys.About, (e) -> AboutDialog.show()));
        }
        menus.getMenus().addAll(file, windows, help);
        /*
         * Set the main content and show.
         */
        final BorderPane pane = new BorderPane();
        pane.setTop(menus);
        pane.setCenter(content.getView());
        final Scene scene = new Scene(pane);
        scene.getStylesheets().add(Styles.STYLESHEET);
        final Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        window.setTitle("Apache Spatial Information System");
        window.getIcons().addAll(new Image(DataViewer.class.getResourceAsStream("SIS_64px.png")),
                                 new Image(DataViewer.class.getResourceAsStream("SIS_128px.png")));
        window.setScene(scene);
        window.setWidth (0.75 * bounds.getWidth());
        window.setHeight(0.75 * bounds.getHeight());
        window.show();
        /*
         * Hide splash screen when the application it ready to show. Despite above call to `window.show()`,
         * the window will become really visible only after this method returned. Consequently the splash
         * screen will be hidden before the main window become visible.
         */
        final SplashScreen sp = SplashScreen.getSplashScreen();
        if (sp != null) {
            sp.close();
        }
        running = this;
    }

    /**
     * Creates the file filters for the dialog box to show in "File" ▶ "Open" and "File" ▶ "Save" menus.
     *
     * @todo Iterate only over the classes in JDK9, without initializing the providers.
     */
    private void createFileFilters() {
        final Resources res = Resources.getInstance();
        final Set<String>    suffixes = new LinkedHashSet<>();
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
                boolean read  = true;
                boolean write = true;
                switch (md.formatName()) {
                    /*
                     * GPX and XML have the same file suffix. Keep XML are reading time because
                     * we can auto-detect the format. Keep GPX at writing time because we need
                     * to be specific about the format to write.
                     */
                    case org.apache.sis.internal.storage.wkt.StoreProvider.NAME: continue;
                    case org.apache.sis.internal.storage.xml.StoreProvider.NAME: write = false; break;
                    case org.apache.sis.internal.storage.gpx.StoreProvider.NAME: read  = false; break;
                }
                String label = null;
                for (final String suffix : md.fileSuffixes()) {
                    final String fs = "*.".concat(suffix);
                    suffixes.add(fs);
                    suffixes.add(fs.toUpperCase());       // Locale-dependent conversion is okay.
                    if (label == null) label = fs;
                }
                if (label != null) {
                    final FileChooser.ExtensionFilter f = new FileChooser.ExtensionFilter(
                            md.formatName() + " (" + label + ')', suffixes.toArray(String[]::new));
                    /*
                     * We use two lists depending on whether the `DataStore` can read, write or
                     * do both. The "All formats" choice is relevant only for read operations.
                     */
                    final Capability[] capabilities = md.capabilities();
                    if (read && ArraysExt.contains(capabilities, Capability.READ)) {
                        allSuffixes.addAll(suffixes);
                        open.add(f);
                    }
                    if (write && ArraysExt.contains(capabilities, Capability.WRITE)) {
                        save.add(f);
                    }
                    suffixes.clear();
                }
            }
        }
        /*
         * Add a filter for all geospatial files in second position, after "All files" and before
         * the filters for specific formats. This will be the default filter for the "Open" action.
         */
        open.add(1, new FileChooser.ExtensionFilter(res.getString(Resources.Keys.GeospatialFiles),
                                                    allSuffixes.toArray(String[]::new)));
        openFilters = open.toArray(FileChooser.ExtensionFilter[]::new);
        saveFilters = save.toArray(FileChooser.ExtensionFilter[]::new);
    }

    /**
     * Invoked when the user selects "File" ▶ "Open" menu.
     * Users can select an arbitrary amount of files or directories.
     * The effect is the same as dragging the files in the "resources tree" window.
     */
    private void showOpenFileDialog() {
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
     * Invoked when the user selects "File" ▶ "Open URL" menu.
     */
    private void showOpenURLDialog() {
        final TextInputDialog  chooser = new TextInputDialog();
        final ListView<String> recents = new ListView<>();
        RecentChoices.getURLs(recents.getItems());
        recents.setPrefWidth (500);
        recents.setPrefHeight(200);
        recents.getSelectionModel().selectedItemProperty().addListener((p,o,n) -> chooser.getEditor().setText(n));
        final DialogPane pane = chooser.getDialogPane();
        pane.setHeaderText(Resources.format(Resources.Keys.EnterURL));
        pane.setExpandableContent(recents);
        pane.setExpanded(true);
        chooser.setTitle(Resources.format(Resources.Keys.OpenDataFile));
        chooser.initOwner(window);
        chooser.showAndWait().ifPresent((choice) -> {
            try {
                final URI url = new URI(choice);
                final Set<String> save = new LinkedHashSet<>(16);
                save.add(url.toString());
                for (final String old : recents.getItems()) {
                    save.add(old);
                    if (save.size() >= RecentFiles.MAX_COUNT) break;
                }
                RecentChoices.setURLs(save);
                content.loadResources(Collections.singleton(url));
            } catch (URISyntaxException e) {
                ExceptionReporter.canNotReadFile(content.getView(), choice, e);
            }
        });
    }

    /**
     * Shows system logs in a separated window.
     */
    private void showSystemMonitorWindow() {
        if (systemLogsWindow == null) {
            systemLogsWindow = SystemMonitor.create(window, null);
        }
        systemLogsWindow.show();
        systemLogsWindow.toFront();
    }

    /**
     * Closes the currently selected file.
     */
    private void closeSelectedFile() {
        content.removeAndClose(content.getSelectedResource());
    }

    /**
     * Invoked when the application should stop. No SIS application can be used after
     * this method has been invoked (i.e. the application can not be restarted).
     *
     * @throws Exception if an error occurred, for example while closing a data store.
     */
    @Override
    public void stop() throws Exception {
        running = null;
        LogHandler.register(false);
        BackgroundThreads.stop();
        RecentChoices.saveReferenceSystems();
        super.stop();
    }

    /**
     * Returns the window in which the application is running, or {@code null} if the application is not running.
     *
     * @return the window in which the application is running, or {@code null}.
     */
    public static Stage getCurrentStage() {
        final DataViewer r = running;
        return (r != null) ? r.window : null;
    }
}
