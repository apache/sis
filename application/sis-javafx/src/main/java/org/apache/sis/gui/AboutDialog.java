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

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import org.apache.sis.gui.metadata.MetadataTree;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.Version;
import org.apache.sis.setup.About;


/**
 * Shows the "About" dialog.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class AboutDialog {
    /**
     * Do not allow instantiation of this class.
     */
    private AboutDialog() {
    }

    /**
     * Invoked when the user selects "Help" ▶ "About" menu.
     * It provides information about the system configuration.
     *
     * Apache SIS is a project of the Apache Software Foundation.
     */
    static void show() {
        final Resources  localized  = Resources.getInstance();
        final Vocabulary vocabulary = Vocabulary.getResources(localized.getLocale());
        final Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(localized.getString(Resources.Keys.About));
        alert.setHeaderText(vocabulary.getString(Vocabulary.Keys.Version_2,
                "Apache Spatial Information System", Version.SIS.toString()));

        final DialogPane pane = alert.getDialogPane();
        final Label content = new Label("Apache SIS is a project of the Apache Software Foundation.");
        content.setPadding(new Insets(18));
        pane.setContent(content);

        final MetadataTree tree = new MetadataTree();
        tree.contentProperty.set(About.configuration());
        tree.setPadding(Insets.EMPTY);
        pane.setExpandableContent(tree);
        pane.setPrefWidth(650);
        alert.show();
    }
}
