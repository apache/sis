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

import java.time.Duration;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.geometry.VPos;
import javafx.geometry.Insets;
import javafx.util.StringConverter;
import javafx.collections.ObservableList;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javax.measure.Unit;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.gui.Widget;
import org.apache.sis.gui.map.StatusBar;
import org.apache.sis.measure.UnitFormat;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A control for selecting a two-dimensional slice in a grid extent having more than 2 dimensions.
 * For example if a <var>n</var>-dimensional data cube contains a time axis, this widget provides
 * a slider for selecting a slice on the time axis.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
public class GridSliceSelector extends Widget {
    /**
     * Constants used for identifying the code assuming a two-dimensional space.
     *
     * @see #getXYDimensions()
     */
    private static final int BIDIMENSIONAL = 2;

    /**
     * Some spaces to add around the selectors.
     */
    private static final Insets PADDING = new Insets(3, 9, 3, 9);

    /**
     * Approximate number of pixels per character. Used for computing the estimated size of labels.
     * This is okay if larger than reality, as it will just put more space between labels.
     */
    private static final int PIXELS_PER_CHAR = 10;

    /**
     * Maximum number of minor ticks to place between two major ticks, including one major tick.
     * The actual number will be adjusted so that there is no tick smaller than grid cells.
     */
    private static final int MAX_MINOR_TICK_COUNT = 10;

    /**
     * The grid geometry for which to provide sliders.
     */
    public final ObjectProperty<GridGeometry> gridGeometry;

    /**
     * The currently selected grid extent.
     *
     * @see #selectedExtentProperty()
     */
    private final ReadOnlyObjectWrapper<GridExtent> selectedExtent;

    /**
     * Indices of {@link GridExtent} dimensions which are assigned to <var>x</var> and <var>y</var> coordinates.
     * They are usually 0 for <var>x</var> and 1 for <var>y</var>.
     *
     * @see #BIDIMENSIONAL
     */
    private int xDimension, yDimension;

    /**
     * The locale to use for axis labels, or {@code null} for a default locale.
     */
    private final Locale locale;

    /**
     * The pane which contains all sliders. The grid has two columns and a number of rows
     * equals to the number of {@link #sliders} to be shown. Children at even indices are
     * labels and children at odd indices are sliders.
     */
    private final GridPane view;

    /**
     * The object to use for formatting numbers, created when first needed.
     *
     * @see #getNumberFormat()
     */
    private NumberFormat numberFormat;

    /**
     * The object to use for formatting dates without time, created when first needed.
     *
     * @see #getDateFormat(boolean, boolean)
     */
    private DateFormat dateFormat;

    /**
     * The object to use for formatting dates with time, created when first needed.
     *
     * @see #getDateFormat(boolean, boolean)
     */
    private DateFormat dateAndTimeFormat;

    /**
     * The object to use for formatting long date and time in the status bar, created when first needed.
     *
     * @see #getDateFormat(boolean, boolean)
     */
    private DateFormat longDateFormat;

    /**
     * The status bar where to report the selected position during adjustment, or {@code null} if none.
     *
     * @todo Should be replaced by a {@link ReadOnlyProperty} which computes the value only if there is
     *       at least one listener.
     */
    StatusBar status;

    /**
     * Creates a new widget.
     *
     * @param  locale  the locale to use for axis labels, or {@code null} for a default locale.
     */
    public GridSliceSelector(final Locale locale) {
        this.locale = locale;
        view = new GridPane();
        view.setVgap(9);
        view.setHgap(12);
        view.setPadding(PADDING);
        selectedExtent = new ReadOnlyObjectWrapper<>(this, "selectedExtent");
        gridGeometry = new SimpleObjectProperty<>(this, "gridGeometry");
        gridGeometry.addListener((p,o,n) -> setGridGeometry(n));
        yDimension = 1;
    }

    /**
     * Invoked when the grid extent changed. This method adds or removes sliders as needed
     * and configure the slider ranges for the new grid.
     *
     * @param  gg  the new grid geometry (potentially null).
     */
    private void setGridGeometry(final GridGeometry gg) {
        final ObservableList<Node> children = view.getChildren();
        if (gg == null || gg.getDimension() <= BIDIMENSIONAL || !gg.isDefined(GridGeometry.EXTENT)) {
            children.clear();
            return;
        }
        TransformSeparator gridToCRS  = null;       // Created when first needed.
        Envelope           envelope   = null;       // Fetched when first needed.
        double[]           resolution = null;       // Fetched when first needed.
        Vocabulary         vocabulary = null;       // Fetched when first needed.

        int childrenCount = 0;
        int row = -BIDIMENSIONAL - 1;
        final GridExtent extent = gg.getExtent();
        GridExtent selected = extent;
        final int dimension = extent.getDimension();
        for (int dim=0; dim < dimension; dim++) {
            final long min = extent.getLow (dim);
            final long max = extent.getHigh(dim);
            if (min < max) {
                switch (++row) {
                    case -2: xDimension = dim; continue;
                    case -1: yDimension = dim; continue;
                }
                /*
                 * A new slider needs to be shown. Recycle existing slider and label if any,
                 * or create new controls if we already used all existing controls.
                 */
                final Label     label;
                final Slider    slider;
                final Converter converter;
                if (childrenCount < children.size()) {
                    label     = (Label)  children.get(childrenCount++);
                    slider    = (Slider) children.get(childrenCount++);
                    converter = (Converter) slider.getLabelFormatter();
                } else {
                    childrenCount += 2;
                    view.add(label  = new Label(),  0, row);
                    view.add(slider = new Slider(), 1, row);
                    slider.setShowTickLabels(true);
                    slider.setShowTickMarks(true);
                    slider.setBlockIncrement(1);
                    label.setLabelFor(slider);
                    GridPane.setHgrow(label,  Priority.NEVER);
                    GridPane.setHgrow(slider, Priority.ALWAYS);
                    GridPane.setValignment(label, VPos.TOP);
                    converter = new Converter();
                    slider.setLabelFormatter(converter);
                    slider.widthProperty().addListener((p,o,n) -> converter.setTickSpacing(slider, n.doubleValue()));
                    slider.valueProperty().addListener(converter);
                    slider.valueChangingProperty().addListener((p,o,n) -> converter.setPosition(n, Math.round(slider.getValue())));
                }
                /*
                 * Configure the slider for the current grid axis.
                 */
                if (row == 0) {
                    vocabulary = Vocabulary.getResources(locale);
                    if (gg.isDefined(GridGeometry.GRID_TO_CRS)) {
                        gridToCRS = new TransformSeparator(gg.getGridToCRS(PixelInCell.CELL_CENTER));
                    }
                    if (gg.isDefined(GridGeometry.ENVELOPE)) {
                        envelope = gg.getEnvelope();
                    }
                    if (gg.isDefined(GridGeometry.RESOLUTION)) {
                        resolution = gg.getResolution(false);
                    }
                }
                slider.setMin(min);
                slider.setMax(max);
                slider.setValue(min);
                selected = selected.withRange(dim, min, min);       // Initially selected slice.
                converter.configure(gg, gridToCRS, dim, min, max, envelope, resolution);
                converter.setTickSpacing(slider, slider.getWidth());
                label.setText(vocabulary.toLabel(converter.getAxisLabel(extent, vocabulary)));
            }
        }
        children.remove(childrenCount, children.size());
        selectedExtent.set(selected);
    }




    /**
     * Handle conversion of grid indices to "real world" coordinates or dates.
     * This is also a listener notified when the position of a slider changed.
     * We take the change only after the user finished to drag the slider
     * in order to avoid causing to many load requests.
     */
    private final class Converter extends StringConverter<Double> implements ChangeListener<Number> {
        /**
         * Index of the grid axis where the position changed.
         */
        private int dimension;

        /**
         * Conversion from grid indices to "real world" coordinates, or {@code null} if none.
         */
        private MathTransform1D gridToCRS;

        /**
         * The coordinate system axis, or {@code null} if none.
         */
        private CoordinateSystemAxis axis;

        /**
         * If the axis is a temporal axis, a converter of axis values to dates.
         * Otherwise {@code null}.
         */
        private DefaultTemporalCRS timeCRS;

        /**
         * The minimal temporal resolution value for allowing the formatter
         * to omit the time field after the date field.
         */
        private double timeResolutionThreshold;

        /**
         * The resolution in CRS axis unit, or 1 if unknown.
         */
        private double resolution;

        /**
         * A conversion factor for computing the space between major tick, or 0 or {@link Double#NaN} if unknown.
         * This is the estimated maximal label length (in pixels) multiplied by the span in units of the CRS axis.
         * This value shall be divided by the slider width (in pixels) for getting the space between major ticks
         * in units of CRS axis.
         */
        private double spacingNumerator;

        /**
         * The unit name to append after the value in verbose display, or {@code null} if none.
         */
        private String unitName;

        /**
         * Creates a new converter.
         */
        Converter() {
        }

        /**
         * Creates a new converter.
         *
         * @param gg   the grid geometry for which to create a converter. Can not be null.
         * @param ts   a transform separator initialized to the coverage "grid to CRS" transform, or {@code null}.
         * @param dim  the source dimension (grid axis) to extract in the {@code ts} separator.
         * @param min  minimal grid coordinate (before conversion to CRS coordinate).
         * @param max  maximal grid coordinate (before conversion to CRS coordinate).
         * @param env  the envelope in CRS units, or {@code null} if unknown.
         * @param res  the exact resolution (no estimation) for each CRS axis, or {@code null} if unknown.
         */
        final void configure(final GridGeometry gg, final TransformSeparator ts, final int dim,
                    final double min, final double max, final Envelope env, final double[] res)
        {
            dimension   = dim;
            gridToCRS   = null;
            axis        = null;
            unitName    = null;
            timeCRS     = null;
            resolution  = 1;
            timeResolutionThreshold = Double.NaN;
            double span = max - min;
            if (ts != null) try {
                ts.clear();
                ts.addSourceDimensions(dim);
                gridToCRS = (MathTransform1D) ts.separate();
                final int targetDim = ts.getTargetDimensions()[0];      // Must be after call to `separate(…)`.
                if (gg.isDefined(GridGeometry.CRS)) {
                    final CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem();
                    final CoordinateReferenceSystem c = CRS.getComponentAt(crs, targetDim, targetDim+1);
                    timeCRS = (c instanceof TemporalCRS) ? DefaultTemporalCRS.castOrCopy((TemporalCRS) c) : null;
                    axis    = crs.getCoordinateSystem().getAxis(targetDim);
                    if (timeCRS == null) {
                        final Unit<?> unit = axis.getUnit();
                        if (unit != null) {
                            final Locale locale = GridSliceSelector.this.locale;
                            final UnitFormat f = new UnitFormat(locale != null ? locale : Locale.getDefault());
                            f.setStyle(UnitFormat.Style.NAME);
                            unitName = ' ' + f.format(unit);
                        }
                    }
                }
                /*
                 * `span` and `resolution` must be updated together, assuming linear conversion.
                 * If the conversion is non-linear, then tick spacing computations will use grid
                 * coordinates instead of "real world" coordinates.
                 */
                if (env != null && res != null && res[targetDim] > 0) {
                    resolution = res[targetDim];
                    span = env.getSpan(targetDim);
                    if (timeCRS != null) {
                        timeResolutionThreshold = timeCRS.toValue(Duration.ofDays(1));
                    }
                }
            } catch (FactoryException | ClassCastException e) {
                Logging.ignorableException(Logger.getLogger(Modules.APPLICATION), GridSliceSelector.class,
                        "setGridGeometry", e);              // "gridGeometry.set(…)" is the public API.
            }
            /*
             * After the converter is fully configured (except for `spacingNumerator`),
             * format two arbitrary values in order to estimate the size of labels.
             * Note that the min/max values need to be grid indices, not envelope.
             */
            final int length = Math.max(toString(min).length(), toString(max).length());
            spacingNumerator = length * PIXELS_PER_CHAR * span;
        }

        /**
         * Sets the spacing between marks after the configuration changed or the slider width changed.
         *
         * @param  slider  the slider to configure.
         * @param  width   the new slider width.
         */
        final void setTickSpacing(final Slider slider, final double width) {
            double spacing = spacingNumerator / width;                              // In units of the CRS axis.
            spacing = MathFunctions.pow10(Math.ceil(Math.log10(spacing)));          // Round to 0.1, 1, 10, 100…
            spacing = Math.max(Math.rint(spacing / resolution), 1);                 // Convert to grid units.
            if (spacing > 0 && spacing < Double.POSITIVE_INFINITY) {
                final double minor = Math.max(Math.rint(spacing / MAX_MINOR_TICK_COUNT), 1);
                final long   count = Math.max(Math.min(Math.round(spacing / minor), MAX_MINOR_TICK_COUNT), 1) - 1;
                slider.setMinorTickCount((int) count);
                slider.setMajorTickUnit(spacing);
                slider.setSnapToTicks(minor == 1);
                if (timeCRS != null) {
                    timeResolutionThreshold = timeCRS.toValue(Duration.ofDays(1)) / spacing;
                }
            }
        }

        /**
         * Returns a label for the slider, or {@code null} if unknown.
         * The label is derived from the CRS axis name. If none, fallbacks on grid axis name.
         *
         * @param  extent      the grid geometry extent, used as a fallback if no CRS axis label is found.
         * @param  vocabulary  localized resources for latitude, longitude, height and time words.
         */
        final String getAxisLabel(final GridExtent extent, final Vocabulary vocabulary) {
            if (axis != null) {
                String label = CoordinateSystems.getShortName(axis, locale);
                if (label != null) {
                    if (timeCRS == null) {
                        final Unit<?> unit = axis.getUnit();
                        if (unit != null) {
                            String symbol = unit.toString();
                            if (!symbol.isEmpty()) {
                                label = label + " (" + symbol + ')';
                            }
                        }
                    }
                    return label;
                }
            }
            /*
             * Fallback on grid axis name if no CRS axis name is found.
             */
            final DimensionNameType axis = extent.getAxisType(dimension).orElse(null);
            if (axis != null) {
                return Types.getCodeTitle(axis).toString(locale);
            } else {
                return vocabulary.getString(Vocabulary.Keys.Axis_1, dimension);
            }
        }

        /**
         * Invoked when the user begins of finished to adjust the slider position.
         * The grid extent is updated only after the user finished to adjust.
         */
        final void setPosition(final boolean adjusting, final long position) {
            final StatusBar bar = status;
            if (bar != null) {
                bar.setInfoMessage(adjusting ? toString(position, true) : null);
            }
            if (!adjusting) {
                final GridExtent extent = selectedExtent.get();
                if (extent != null && position != extent.getLow(dimension)) {
                    selectedExtent.set(extent.withRange(dimension, position, position));
                }
            }
        }

        /**
         * Invoked when the slider position changed. If the user is adjusting the position,
         * we only update the message on the status bar. Otherwise the slice extent is updated.
         */
        @Override
        public void changed(final ObservableValue<? extends Number> property, final Number oldValue, final Number newValue) {
            final Slider slider = (Slider) ((ReadOnlyProperty<?>) property).getBean();
            setPosition(slider.isValueChanging(), Math.round(newValue.doubleValue()));
        }

        /**
         * Converts a grid index to a string representation.
         *
         * @param  value    the grid index. Should be an integer value.
         * @param  verbose  whether to use a verbose format.
         */
        private String toString(double value, final boolean verbose) {
            double derivative;
            int    numDigits;
            if (gridToCRS != null) try {
                value      = gridToCRS.transform(value);
                derivative = gridToCRS.derivative(value);
                numDigits  = DecimalFunctions.fractionDigitsForDelta(derivative, false);
            } catch (TransformException e) {
                return "N/A";
            } else {
                derivative = 0;
                numDigits  = 0;
            }
            if (timeCRS != null) {
                final DateFormat f = getDateFormat(verbose, derivative < timeResolutionThreshold);
                return f.format(timeCRS.toDate(value));
            } else {
                final NumberFormat f = getNumberFormat();
                f.setMinimumFractionDigits(numDigits);
                f.setMaximumFractionDigits(numDigits);
                String text = f.format(value);
                if (verbose && unitName != null) {
                    text = text.concat(unitName);
                }
                return text;
            }
        }

        /**
         * Converts a grid index to a string representation.
         * The given value is rounded to nearest integer for making sure that it describes a cell position.
         */
        @Override
        public String toString(final Double index) {
            return toString(Math.rint(index), false);
        }

        /**
         * Converts a string representation to a grid index.
         * This method is defined as a matter of principle but should not be invoked.
         */
        @Override
        public Double fromString(final String text) {
            double value;
            try {
                if (timeCRS != null) {
                    value = timeCRS.toValue(getDateFormat(false, true).parse(text));
                } else {
                    value = getNumberFormat().parse(text).doubleValue();
                }
                if (gridToCRS != null) {
                    value = gridToCRS.inverse().transform(value);
                }
            } catch (ParseException | TransformException e) {
                value = Double.NaN;
            }
            return value;
        }
    }

    /**
     * Returns the object to use for formatting numbers.
     */
    private NumberFormat getNumberFormat() {
        if (numberFormat == null) {
            numberFormat = (locale != null)
                    ? NumberFormat.getNumberInstance(locale)
                    : NumberFormat.getNumberInstance();
        }
        return numberFormat;
    }

    /**
     * Returns the object to use for formatting dates.
     *
     * @param  verbose   whether to use a verbose format for display on as a message (as opposed to label).
     * @param  withTime  {@code false} for dates only, or {@code true} for dates with times.
     */
    private DateFormat getDateFormat(final boolean verbose, final boolean withTime) {
        if (verbose) {
            if (longDateFormat == null) {
                longDateFormat = (locale != null)
                        ? DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale)
                        : DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
            }
            return longDateFormat;
        } else if (withTime) {
            if (dateAndTimeFormat == null) {
                dateAndTimeFormat = (locale != null)
                        ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
                        : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            }
            return dateAndTimeFormat;
        } else {
            if (dateFormat == null) {
                dateFormat = (locale != null)
                        ? DateFormat.getDateInstance(DateFormat.SHORT, locale)
                        : DateFormat.getDateInstance(DateFormat.SHORT);
            }
            return dateFormat;
        }
    }




    /**
     * Returns the property for the currently selected grid extent.
     * This value varies after the user finished to drag the slider.
     *
     * @return the currently selected grid extent as a read-only property.
     */
    public final ReadOnlyProperty<GridExtent> selectedExtentProperty() {
        return selectedExtent.getReadOnlyProperty();
    }

    /**
     * Returns the grid dimensions of <var>x</var> and <var>y</var> axes rendered in a two-dimensional image or table.
     * This value is inferred from the {@linkplain #gridGeometry grid geometry} property value.
     * It is almost always {0,1}, i.e. the 2 first dimensions in a coordinate tuple.
     *
     * @return indices of <var>x</var> and <var>y</var> coordinate values in a grid coordinate tuple.
     */
    public final int[] getXYDimensions() {
        return new int[] {xDimension, yDimension};
    }

    /**
     * Returns the encapsulated JavaFX component to add in a scene graph for making the selectors visible.
     * The {@code Region} subclass is implementation dependent and may change in any future SIS version.
     *
     * @return the JavaFX component to insert in a scene graph.
     */
    @Override
    public final Region getView() {
        return view;
    }

    /**
     * Returns {@code true} if this slice selector has no component to shown.
     * Slice selectors are always empty with two-dimensional data.
     *
     * @return {@code true} if this slice selector has no component to shown.
     */
    public boolean isEmpty() {
        return view.getChildren().isEmpty();
    }
}
