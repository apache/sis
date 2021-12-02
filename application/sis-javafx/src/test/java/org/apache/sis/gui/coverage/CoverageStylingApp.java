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
package org.apache.sis.gui.coverage;

import java.util.Locale;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.measure.Units;


/**
 * Shows category table built by {@link CoverageStyling} with arbitrary data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public final strictfp class CoverageStylingApp extends Application {
    /**
     * Starts the test application.
     *
     * @param args  ignored.
     */
    public static void main(final String[] args) {
        launch(args);
    }

    /**
     * Creates and starts the test application.
     *
     * @param  window  where to show the application.
     */
    @Override
    public void start(final Stage window) {
        final BorderPane pane = new BorderPane();
        pane.setCenter(createCategoryTable());
        pane.setBottom(new Button("Focus here"));
        window.setTitle("CategoryTable Test");
        window.setScene(new Scene(pane));
        window.setWidth (400);
        window.setHeight(300);
        window.show();
    }

    /**
     * Creates a table with arbitrary categories to show.
     */
    private static TableView<Category> createCategoryTable() {
        final SampleDimension band = new SampleDimension.Builder()
                .addQualitative("Background", 0)
                .addQualitative("Cloud",      1)
                .addQualitative("Land",       2)
                .addQuantitative("Temperature", 5, 255, 0.15, -5, Units.CELSIUS)
                .setName("Sea Surface Temperature")
                .build();

        final CoverageStyling styling = new CoverageStyling(null);
        styling.setARGB(band.getCategories().get(1), new int[] {0xFF607080});
        final TableView<Category> table = styling.createCategoryTable(
                Resources.forLocale(null), Vocabulary.getResources((Locale) null));
        table.getItems().setAll(band.getCategories());
        return table;
    }
}
