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
import java.util.TimeZone;
import javax.measure.Unit;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.geometry.CoordinateFormat;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Classes;


/**
 * A status bar showing coordinates of a grid cell.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class StatusBar extends HBox {
    /**
     * The view for which we are showing geographic or projected coordinates of selected cell.
     */
    private final GridView view;

    /**
     * Zero-based cell coordinates currently formatted in the {@link #coordinates} field.
     * This is used for detecting if coordinate values changed since last formatting.
     */
    private int column, row;

    /**
     * Conversion from ({@linkplain #column},{@linkplain #row}) cell coordinates
     * to geographic or projected coordinates.
     */
    private MathTransform gridToCRS;

    /**
     * Coordinates after conversion to the CRS. The number of dimensions depends on
     * the target CRS. This object is reused during each coordinate transformation.
     */
    private DirectPosition position;

    /**
     * The object to use for formatting coordinate values.
     */
    private final CoordinateFormat format;

    /**
     * The labels where to format the coordinates.
     */
    private final Label coordinates;

    /**
     * Creates a new status bar.
     */
    StatusBar(final GridView view) {
        this.view = view;
        format = new CoordinateFormat(Locale.getDefault(Locale.Category.FORMAT), TimeZone.getDefault());
        coordinates = new Label();
        setAlignment(Pos.CENTER_RIGHT);
        getChildren().setAll(coordinates);
        setPadding(new Insets(5, GridViewSkin.SCROLLBAR_WIDTH, 6, 0));
        setCoordinateConversion(null, null);
    }

    /**
     * Sets the conversion from (column, row) cell indices to geographic or projected coordinates.
     * The conversion is computed from the given grid geometry.
     *
     * @param  geometry  geometry of the grid coverage shown in {@link GridView}, or {@code null}.
     * @param  request   sub-region of the coverage which is shown, or {@code null} for the full coverage.
     */
    final void setCoordinateConversion(final GridGeometry geometry, GridExtent request) {
        gridToCRS = MathTransforms.identity(2);
        CoordinateReferenceSystem crs = null;
        double resolution = 1;
        Unit<?> unit = Units.PIXEL;
        if (geometry != null) {
            if (geometry.isDefined(GridGeometry.GRID_TO_CRS)) {
                gridToCRS = geometry.getGridToCRS(PixelInCell.CELL_CENTER);
                if (geometry.isDefined(GridGeometry.CRS)) {
                    crs = geometry.getCoordinateReferenceSystem();
                }
            }
            if (request == null && geometry.isDefined(GridGeometry.EXTENT)) {
                request = geometry.getExtent();
            }
            /*
             * Computes the precision of coordinates to format. We use the finest resolution,
             * looking only at axes having the same units of measurement than the first axis.
             */
            if (geometry.isDefined(GridGeometry.RESOLUTION)) {
                double[] resolutions = geometry.getResolution(true);
                if (crs != null && resolutions.length != 0) {
                    final CoordinateSystem cs = crs.getCoordinateSystem();
                    unit = cs.getAxis(0).getUnit();
                    for (int i=0; i<resolutions.length; i++) {
                        if (unit.equals(cs.getAxis(i).getUnit())) {
                            final double r = resolutions[i];
                            if (r < resolution) resolution = r;
                        }
                    }
                }
            }
        }
        /*
         * By `GridCoverage.render(GridExtent)` contract, the `RenderedImage` pixel coordinates are relative
         * to the requested `GridExtent`. Consequently we need to translate the image coordinates so that it
         * become the coordinates of the original `GridGeometry` before to apply `gridToCRS`.
         */
        if (request != null) {
            final double[] origin = new double[request.getDimension()];
            for (int i=0; i<origin.length; i++) {
                origin[i] = request.getLow(i);
            }
            gridToCRS = MathTransforms.concatenate(MathTransforms.translation(origin), gridToCRS);
        }
        format.setDefaultCRS(crs);
        format.setPrecision(resolution, unit);
        Tooltip tp = null;
        if (crs != null) {
            tp = new Tooltip(IdentifiedObjects.getDisplayName(crs, format.getLocale(Locale.Category.DISPLAY)));
        }
        coordinates.setTooltip(tp);
    }

    /**
     * Sets the pixel coordinates to show. Those pixel coordinates will be automatically
     * transformed to geographic coordinates if a "grid to CRS" conversion is available.
     */
    final void setCoordinates(final int x, final int y) {
        if (x != this.column || y != this.row) {
            this.column = x;
            this.row = y;
            String text;
            try {
                position = gridToCRS.transform(view.toImageCoordinates(x, y), position);
                text = format.format(position);
            } catch (TransformException e) {
                text = e.getLocalizedMessage();
                if (text == null) {
                    text = Classes.getShortClassName(e);
                }
            }
            coordinates.setText(text);
        }
    }
}
