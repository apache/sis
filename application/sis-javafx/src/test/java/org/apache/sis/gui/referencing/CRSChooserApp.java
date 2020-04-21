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
package org.apache.sis.gui.referencing;

import java.util.Optional;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.application.Application;
import javafx.geometry.Insets;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.referencing.CommonCRS;


/**
 * Shows {@link CRSChooser}. The area of interest is set to Canada
 * for allowing to test the CRS domain of validity checks.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class CRSChooserApp extends Application {
    /**
     * Starts the test application.
     *
     * @param args  ignored.
     */
    public static void main(final String[] args) {
        launch(args);
    }

    /**
     * Sets a dummy scene for the given window.
     * The main purpose is to provide a scene for testing dialog boxes
     * that user can close after the test is finished.
     */
    private static void setDummyScene(final Stage window) {
        Label note = new Label("Close this window for stopping the application.");
        note.setPadding(new Insets(15));
        Scene scene = new Scene(note);
        window.setTitle("CRSChooserApp");
        window.setScene(scene);
    }

    /**
     * Creates and starts the test application.
     *
     * @param  window  where to show the application.
     */
    @Override
    public void start(final Stage window) {
        setDummyScene(window);
        window.show();

        final GeneralEnvelope bbox = new GeneralEnvelope(CommonCRS.defaultGeographic());
        bbox.setRange(0, -140.99778, -52.6480987209);
        bbox.setRange(1, 41.6751050889, 83.23324);              // Canada
        final CRSChooser chooser = new CRSChooser(null, bbox, null);
        final Optional<CoordinateReferenceSystem> crs = chooser.showDialog(window);

        System.out.println("The selected CRS is: " + crs);
    }

    /**
     * Stops the test application.
     *
     * @throws Exception if an error occurred while stopping the application.
     */
    @Override
    public void stop() throws Exception {
        BackgroundThreads.stop();
        super.stop();
    }
}
