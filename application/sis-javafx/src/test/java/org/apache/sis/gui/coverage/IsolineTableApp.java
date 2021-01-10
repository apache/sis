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
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.application.Application;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Shows isoline table built by {@link IsolineTable} with arbitrary data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class IsolineTableApp extends Application {
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
        pane.setCenter(createIsolineTable());
        pane.setBottom(new Button("Focus here"));
        window.setTitle("IsolineTable Test");
        window.setScene(new Scene(pane));
        window.setWidth (400);
        window.setHeight(300);
        window.show();
    }

    /**
     * Creates a table with arbitrary isolines to show.
     */
    private static TableView<IsolineLevel> createIsolineTable() {
        final IsolineTable handler = new IsolineTable();
        final TableView<IsolineLevel> table = handler.createIsolineTable(Vocabulary.getResources((Locale) null));
        table.getItems().setAll(
                new IsolineLevel( 10, Color.BLUE),
                new IsolineLevel( 25, Color.GREEN),
                new IsolineLevel( 50, Color.ORANGE),
                new IsolineLevel(100, Color.RED),
                new IsolineLevel());                    // Empty row for inserting new values.
        return table;
    }
}
