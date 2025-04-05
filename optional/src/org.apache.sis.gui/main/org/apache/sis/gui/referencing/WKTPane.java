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

import java.util.EnumMap;
import java.util.Locale;
import java.time.ZoneId;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Small panel to display an object as WKT in various conventions.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class WKTPane extends StringConverter<Convention> implements ChangeListener<Convention> {
    /**
     * The object to use for formatting and CRS.
     */
    private final WKTFormat format;

    /**
     * A choice box for choosing the WKT conventions.
     */
    final ChoiceBox<Convention> convention;

    /**
     * Localized string representations of {@link #convention}.
     */
    private final EnumMap<Convention,String> conventionTexts;

    /**
     * The pane where to show the Well Known Text.
     */
    final TextArea text = new TextArea();

    /**
     * The object to format.
     */
    private CoordinateReferenceSystem crs;

    /**
     * Creates a new pane for showing CRS Well Known Text.
     */
    WKTPane(final Locale locale) {
        final Convention[] sc = {           // Selected conventions in the order we want them to appear.
            Convention.WKT2_SIMPLIFIED,
            Convention.WKT2,
            Convention.WKT1,
            Convention.WKT1_COMMON_UNITS
        };
        conventionTexts = new EnumMap<>(Convention.class);
        final Vocabulary vocabulary = Vocabulary.forLocale(locale);
        for (final Convention c : sc) {
            conventionTexts.put(c, toString(c, vocabulary));
        }
        format = new WKTFormat(locale, (ZoneId) null);
        format.setConvention(Convention.WKT2_SIMPLIFIED);
        convention = new ChoiceBox<>(FXCollections.observableArrayList(sc));
        convention.setConverter(this);
        convention.getSelectionModel().select(format.getConvention());
        convention.valueProperty().addListener(this);
        convention.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(convention, Priority.ALWAYS);
    }

    /**
     * Returns the text to write in {@link #convention} choice box for the given convention.
     */
    @SuppressWarnings("fallthrough")
    private String toString(final Convention c, final Vocabulary vocabulary) {
        final Object version;
        boolean simplified = false;
        switch (c) {
            case WKT2_SIMPLIFIED:   simplified = true;         // Fall through.
            case WKT2:              version = 2; break;
            case WKT1:              version = 1; break;
            case WKT1_COMMON_UNITS: version = "GDAL 1-2"; break;
            default: return c.name();
        }
        String text = vocabulary.getString(Vocabulary.Keys.Version_2, "WKT (Well Known Text)", version);
        if (simplified) {
            text += " — " + vocabulary.getString(Vocabulary.Keys.Simplified);
        }
        return text;
    }

    /**
     * Returns the text to write in {@link #convention} choice box for the given convention.
     */
    @Override
    public String toString(final Convention c) {
        return conventionTexts.get(c);
    }

    /**
     * Returns the convention from the given string.
     * This is the reverse of {@link #toString(Convention)}.
     */
    @Override
    public Convention fromString(final String text) {
        for (final EnumMap.Entry<Convention,String> e : conventionTexts.entrySet()) {
            if (e.getValue().equals(text)) return e.getKey();
        }
        return null;
    }

    /**
     * Invoked when the user select a new format. This method is public as an implementation side-effect;
     * it should not be invoked explicitly.
     */
    @Override
    public void changed(ObservableValue<? extends Convention> observable, Convention oldValue, Convention newValue) {
        format.setConvention(newValue);
        if (crs != null) {
            text.setText(format.format(crs));
        }
    }

    /**
     * Sets the CRS to show in this pane. The CRS is constructed in a background thread.
     * The execution will usually be very quick because {@link CRSChooser} already started
     * a background thread for fetching the selected CRS, and WKT formatting is fast.
     */
    final void setContent(final AuthorityCodes source, final String code) {
        text.setDisable(true);
        BackgroundThreads.execute(new Task<CoordinateReferenceSystem>() {
            /** The WKT text formatted in background thread. */
            private String wkt;

            /** Invoked in background thread for fetching the CRS from an authority code. */
            @Override protected CoordinateReferenceSystem call() throws FactoryException {
                final CoordinateReferenceSystem crs = source.getFactory().createCoordinateReferenceSystem(code);
                if (crs != null) {
                    wkt = format.format(crs);
                }
                return crs;
            }

            /** Invoked in JavaFX thread on success. */
            @Override protected void succeeded() {
                setContent(getValue(), wkt);
            }

            /** Invoked in JavaFX thread on cancellation. */
            @Override protected void cancelled() {
                text.setText(null);
            }

            /** Invoked in JavaFX thread on failure. */
            @Override protected void failed() {
                text.setDisable(false);
                text.setEditable(false);
                text.setText(Exceptions.getLocalizedMessage(getException(), source.locale));
            }
        });
    }

    /**
     * Sets the content to the given coordinate reference system.
     */
    private void setContent(final CoordinateReferenceSystem newCRS, final String wkt) {
        text.setEditable(false);     // TODO: make editable if we allow WKT parsing in a future version.
        text.setDisable(false);
        if (newCRS != crs) {
            crs = newCRS;
            text.setText(wkt);
        }
    }
}
