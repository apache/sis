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
package org.apache.sis.gui.dataset;

import java.util.EventObject;
import javafx.scene.layout.Region;
import javafx.scene.control.MenuItem;
import org.apache.sis.gui.coverage.CoverageExplorer;
import org.apache.sis.gui.coverage.ImageRequest;
import org.apache.sis.internal.gui.Resources;


/**
 * A description of currently selected data.
 * The selected data may be one of the following resources:
 *
 * <ul>
 *   <li>{@link org.apache.sis.storage.FeatureSet}</li>
 *   <li>{@link org.apache.sis.storage.GridCoverageResource}</li>
 * </ul>
 *
 * {@code SelectedData} does not contain those resources directly, but rather contains the view or
 * other kind of object wrapping the selected resource. The kind of wrappers used for each type of
 * resource may change in any future version of this class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class SelectedData {
    /**
     * Key of a property for storing {@link CoverageExplorer.View} value
     * specifying the initial view of new windows.
     */
    private static final String COVERAGE_VIEW_KEY = "org.apache.sis.gui.CoverageView";

    /**
     * A title to use for windows and menu items.
     */
    final String title;

    /**
     * The control that contains the currently selected data if those data are features.
     * Only one of {@link #features} and {@link #coverage} shall be non-null.
     */
    private final FeatureTable features;

    /**
     * The request for coverage data, or {@code null} if the selected data are not coverage.
     * Only one of {@link #features} and {@link #coverage} shall be non-null.
     */
    private final ImageRequest coverage;

    /**
     * Localized resources, for convenience only.
     */
    final Resources localized;

    /**
     * Creates a snapshot of selected data.
     */
    SelectedData(final String title, final FeatureTable features, final ImageRequest coverage, final Resources localized) {
        this.title     = title;
        this.features  = features;
        this.coverage  = coverage;
        this.localized = localized;
    }

    /**
     * Specifies that the given menu item should create a window initialized to tabular data
     * instead of the image.
     */
    static MenuItem setTabularView(final MenuItem item) {
        item.getProperties().put(COVERAGE_VIEW_KEY, CoverageExplorer.View.TABLE);
        return item;
    }

    /**
     * Creates the view for selected data.
     *
     * @param  event  the event (e.g. mouse action) requesting a new window, or {@code null} if unknown.
     */
    final Region createView(final EventObject event) {
        if (features != null) {
            return new FeatureTable(features);
        } else {
            CoverageExplorer.View view = CoverageExplorer.View.IMAGE;
            if (event != null) {
                final Object source = event.getSource();
                if (source instanceof MenuItem) {
                    final Object value = ((MenuItem) source).getProperties().get(COVERAGE_VIEW_KEY);
                    if (value instanceof CoverageExplorer.View) {
                        view = (CoverageExplorer.View) value;
                    }
                }
            }
            final CoverageExplorer ce = new CoverageExplorer(view);
            ce.setCoverage(coverage);
            return ce.getView();
        }
    }
}
