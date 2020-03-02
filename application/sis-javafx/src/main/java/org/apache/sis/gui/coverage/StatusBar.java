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
import java.util.function.Consumer;
import javax.measure.Unit;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.event.EventHandler;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.CoordinateFormat;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Exceptions;


/**
 * A status bar showing coordinates of a grid cell.
 * The number of fraction digits is adjusted according pixel resolution for each coordinate to format.
 *
 * <p>Callers can register this object to {@link javafx.scene.Node#setOnMouseEntered(EventHandler)} and
 * {@link javafx.scene.Node#setOnMouseExited(EventHandler)} for showing or hiding the coordinate values
 * when the mouse enter or exit the region of interest.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class StatusBar extends HBox implements EventHandler<MouseEvent> {
    /**
     * A function which take cell indices in input and gives pixel coordinates in output.
     * The input and output coordinates are in the given array, which is updated in-place.
     *
     * @see GridView#toImageCoordinates(double[])
     */
    private final Consumer<double[]> toImageCoordinates;

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
     * The source cell indices before conversion to geospatial coordinates.
     * The number of dimensions should be 2.
     */
    private double[] sourceCoordinates;

    /**
     * Coordinates after conversion to the CRS. The number of dimensions depends on
     * the target CRS. This object is reused during each coordinate transformation.
     */
    private GeneralDirectPosition targetCoordinates;

    /**
     * The desired precisions for each dimension in the {@link #targetCoordinates} to format.
     * It may vary for each position if the {@link #gridToCRS} transform is non-linear.
     */
    private double[] precisions;

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
    StatusBar(final Consumer<double[]> toImageCoordinates) {
        this.toImageCoordinates = toImageCoordinates;
        format = new CoordinateFormat();
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
        precisions = null;
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
             * This will be used as a fallback if we can not compute the precision specific
             * to a coordinate, for example if we can not compute the derivative.
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
        /*
         * Prepare objects to be reused for each coordinate transformation.
         * Configure the CoordinateFormat with the CRS.
         */
        if (gridToCRS != null) {
            sourceCoordinates = new double[Math.max(gridToCRS.getSourceDimensions(), ImageLoader.BIDIMENSIONAL)];
            targetCoordinates = new GeneralDirectPosition(gridToCRS.getTargetDimensions());
        } else {
            targetCoordinates = new GeneralDirectPosition(ImageLoader.BIDIMENSIONAL);
            sourceCoordinates = targetCoordinates.coordinates;
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
        if (x != column || y != row) {
            sourceCoordinates[0] = column = x;
            sourceCoordinates[1] = row    = y;
            String text;
            try {
                toImageCoordinates.accept(sourceCoordinates);
                Matrix derivative;
                try {
                    derivative = MathTransforms.derivativeAndTransform(gridToCRS,
                            sourceCoordinates, 0, targetCoordinates.coordinates, 0);
                } catch (TransformException ignore) {
                    /*
                     * If above operation failed, it may be because the MathTransform does not support
                     * derivative calculation. Try again without derivative (the precision will be set
                     * to the default resolution computed in `setCoordinateConversion(…)`).
                     */
                    gridToCRS.transform(sourceCoordinates, 0, targetCoordinates.coordinates, 0, 1);
                    derivative = null;
                }
                if (derivative == null) {
                    precisions = null;
                } else {
                    if (precisions == null) {
                        precisions = new double[targetCoordinates.getDimension()];
                    }
                    /*
                     * Estimate the precision by looking at the maximal displacement in the CRS caused by
                     * a displacement of one cell (i.e. when moving by row or one column).  We search for
                     * maximal displacement instead than minimal because we expect the displacement to be
                     * zero along some axes (e.g. one row down does not change longitude value in a Plate
                     * Carrée projection).
                     */
                    for (int j=derivative.getNumRow(); --j >= 0;) {
                        double p = 0;
                        for (int i=derivative.getNumCol(); --i >= 0;) {
                            final double e = Math.abs(derivative.getElement(j, i));
                            if (e > p) p = e;
                        }
                        precisions[j] = p;
                    }
                }
                format.setPrecisions(precisions);
                text = format.format(targetCoordinates);
            } catch (TransformException | RuntimeException e) {
                /*
                 * If even the fallback without derivative failed, show the error message.
                 */
                Throwable cause = Exceptions.unwrap(e);
                text = cause.getLocalizedMessage();
                if (text == null) {
                    text = Classes.getShortClassName(cause);
                }
            }
            coordinates.setText(text);
        }
    }

    /**
     * Shows or hide the coordinates depending on whether the mouse entered or exited
     * the region for which this status bar is providing information.
     *
     * <p>For the convenience of {@link GridViewSkin}, a null value is equivalent to
     * a mouse exit event.</p>
     */
    @Override
    public final void handle(final MouseEvent event) {
        coordinates.setVisible(event != null && event.getEventType() != MouseEvent.MOUSE_EXITED);
    }
}
