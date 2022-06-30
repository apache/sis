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
import javafx.scene.layout.Region;
import javafx.application.Application;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.opengis.metadata.spatial.DimensionNameType;


/**
 * Shows selectors built by {@link GridSliceSelector} with arbitrary data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public final strictfp class GridSliceSelectorApp extends Application {
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
        final Scene scene = new Scene(createWidget());
        window.setTitle("GridSliceSelector Test");
        window.setScene(scene);
        window.setWidth (400);
        window.setHeight(300);
        window.show();
    }

    /**
     * Creates a view with arbitrary sliders to show.
     */
    private static Region createWidget() {
        final DimensionNameType[] types = {
            DimensionNameType.COLUMN,
            DimensionNameType.ROW,
            DimensionNameType.SAMPLE,
            DimensionNameType.VERTICAL,
            DimensionNameType.TIME
        };
        final GridExtent extent = new GridExtent(types,
                new long[] {-100, -100, 20, 40, 1000},
                new long[] { 500,  800, 20, 90, 1200}, true);
        final GridSliceSelector selector = new GridSliceSelector(Locale.getDefault());
        selector.gridGeometry.set(new GridGeometry(extent, null, null));
        return selector.getView();
    }
}
