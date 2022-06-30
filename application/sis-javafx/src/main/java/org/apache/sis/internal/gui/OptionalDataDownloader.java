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
package org.apache.sis.internal.gui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.web.WebView;
import org.opengis.util.FactoryException;
import org.apache.sis.gui.DataViewer;
import org.apache.sis.internal.system.Fallback;
import org.apache.sis.setup.OptionalInstallations;


/**
 * A provider for data licensed under different terms of use than the Apache license.
 * This class is automatically instantiated and executed when EPSG data are missing.
 * It popups a dialog box proposing to the user to download the data, if (s)he accepts
 * the EPSG terms of use.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
@Fallback
public final class OptionalDataDownloader extends OptionalInstallations {
    /**
     * Whether user accepted to download and install the resources.
     * This is used for deciding if error should be reported to the user or only logged.
     *
     * @see #reportIfInstalling(FactoryException)
     */
    private static volatile boolean accepted;

    /**
     * Creates a new installation scripts provider.
     */
    public OptionalDataDownloader() {
        super("text/html");
    }

    /**
     * Asks to the user if (s)he agree to download and install the resource for the given authority.
     * This method may be invoked twice for the same {@code authority} argument:
     * first with a null {@code license} argument for asking if the user agrees to download the data,
     * then with a non-null {@code license} argument for asking if the user agrees with the license terms.
     */
    @Override
    protected boolean askUserAgreement(final String authority, final String license) {
        if (!Platform.isFxApplicationThread()) {
            return BackgroundThreads.runAndWaitDialog(() -> {
                return askUserAgreement(authority, license);
            });
        }
        final Resources resources = Resources.forLocale(getLocale());
        final Alert dialog;
        dialog = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.NO, ButtonType.YES);
        dialog.initOwner(DataViewer.getCurrentStage());
        dialog.setTitle(resources.getString(Resources.Keys.GeodeticDataset_1, authority));
        dialog.setResizable(true);
        if (license == null) {
            dialog.getDialogPane().setPrefWidth(600);
            dialog.setHeaderText(resources.getString(Resources.Keys.DownloadAndInstall_1, authority));
            dialog.setContentText(resources.getString(Resources.Keys.DownloadDetails_3,
                    authority, getSpaceRequirement(authority), destinationDirectory));
        } else {
            final WebView content = new WebView();
            content.getEngine().loadContent(license);
            final DialogPane pane = dialog.getDialogPane();
            pane.setContent(content);
            pane.setPrefWidth(800);
            pane.setPrefHeight(600);
            dialog.setHeaderText(resources.getString(Resources.Keys.LicenseAgreement));
        }
        return accepted = dialog.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    /**
     * Reports the given error if it happened during download and installation of a resource.
     * An explicit action for reporting errors to the user is needed because callers such as
     * {@link org.apache.sis.gui.referencing.RecentReferenceSystems} will be default catch
     * exceptions and simply log that the CRS as unavailable.
     */
    public static void reportIfInstalling(final FactoryException exception) {
        final boolean s;
        synchronized (OptionalInstallations.class) {
            s = accepted;
            accepted = false;       // For avoidig to report twice.
        }
        if (s) {
            ExceptionReporter.show(DataViewer.getCurrentStage(), null,
                    Resources.format(Resources.Keys.CanNotInstallResource), exception);
        }
    }
}
