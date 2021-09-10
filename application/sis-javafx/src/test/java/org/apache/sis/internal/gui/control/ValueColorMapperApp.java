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
package org.apache.sis.internal.gui.control;

import java.util.Locale;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.application.Application;
import org.apache.sis.internal.gui.Styles;
import org.apache.sis.internal.gui.Resources;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Shows isoline table built by {@link ValueColorMapper} with arbitrary data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class ValueColorMapperApp extends Application {
    /**
     * Starts the test application.
     *
     * @param  args  ignored.
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
        final Scene scene = new Scene(pane);
        scene.getStylesheets().add(Styles.STYLESHEET);
        window.setTitle("ValueColorMapper Test");
        window.setScene(scene);
        window.setWidth (400);
        window.setHeight(300);
        window.show();
    }

    /**
     * Creates a table with arbitrary isolines to show.
     */
    private static Region createIsolineTable() {
        final ValueColorMapper handler = new ValueColorMapper(
                Resources.forLocale(null),
                Vocabulary.getResources((Locale) null));
        handler.getSteps().setAll(
                new ValueColorMapper.Step( 10, Color.BLUE),
                new ValueColorMapper.Step( 25, Color.GREEN),
                new ValueColorMapper.Step( 50, Color.ORANGE),
                new ValueColorMapper.Step(100, Color.RED),
                new ValueColorMapper.Step());                    // Empty row for inserting new values.
        return handler.getView();
    }
}
