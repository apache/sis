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

import java.util.Locale;
import java.util.Optional;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Window;
import javafx.util.Duration;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.OperationMethod;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.gui.internal.ExceptionReporter;
import org.apache.sis.gui.internal.IdentityValueFactory;
import org.apache.sis.gui.internal.Resources;
import org.apache.sis.gui.internal.Styles;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.resources.Vocabulary;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.referencing.crs.GeneralDerivedCRS;


/**
 * A list of Coordinate Reference Systems (CRS) from which the user can select.
 * The CRS choices is built in a background thread from a specified {@link CRSAuthorityFactory}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.1
 */
public class CRSChooser extends Dialog<CoordinateReferenceSystem> {
    /**
     * The pane where the controls for this CRS chooser will be put.
     * The top part contains the tools bar. The center part contains
     * the table or the WKT and change depending on user actions.
     */
    private final BorderPane content;

    /**
     * The tools bar for this pane. Children are in this order:
     * <ul>
     *   <li>A {@link Label} for the second child.</li>
     *   <li>A text field, combo box or other control.</li>
     *   <li>An arbitrary number of buttons.</li>
     * </ul>
     */
    private final HBox tools;

    /**
     * The text field where user can enter a fragment of the name of the CRS (s)he is looking for.
     */
    private final TextField searchField;

    /**
     * The table showing CRS codes together with their names. Table items are provided by a background thread.
     * Items are initially authority codes as {@link Code} instances without {@link Code#name} value.
     * Names are completed later when needed.
     */
    private final TableView<Code> table;

    /**
     * A panel showing the type and domain of validity of selected CRS.
     */
    private final GridPane summary;

    /**
     * The label where to write the CRS type and domain of validity.
     */
    private final Label type, domain;

    /**
     * The area of interest, or {@code null} if none.
     * Axis order is (<var>longitude</var>, <var>latitude</var>).
     */
    private final ImmutableEnvelope areaOfInterest;

    /**
     * The pane showing the CRS in Well Known Text format.
     * Created when first needed.
     */
    private WKTPane wktPane;

    /**
     * Creates a chooser proposing all coordinate reference systems from the default factory.
     */
    public CRSChooser() {
        this(null, null, null);
    }

    /**
     * Creates a chooser proposing all coordinate reference systems from the given factory.
     * If the given factory is {@code null}, then a
     * {@linkplain org.apache.sis.referencing.CRS#getAuthorityFactory(String) default factory}
     * capable to handle at least some EPSG codes will be used.
     *
     * @param  factory         the factory to use for creating coordinate reference systems, or {@code null} for default.
     * @param  areaOfInterest  geographic area for which to choose a CRS, or {@code null} if no restriction.
     * @param  locale          the preferred locale for displaying object name, or {@code null} for the default locale.
     */
    @SuppressWarnings({"unchecked", "this-escape"})
    public CRSChooser(final CRSAuthorityFactory factory, final Envelope areaOfInterest, Locale locale) {
        this.areaOfInterest = Utils.toGeographic(CRSChooser.class, "<init>", areaOfInterest);
        if (locale == null)  locale     = Locale.getDefault();
        final Resources      i18n       = Resources.forLocale(locale);
        final Vocabulary     vocabulary = Vocabulary.forLocale(locale);
        final AuthorityCodes codeList   = new AuthorityCodes(factory, locale);
        table = new TableView<>(codeList);
        codeList.owner = table;
        /*
         * Columns to show in CRS table. First column is typically EPSG codes and second
         * column is the CRS descriptions. The content is loaded in a background thread.
         */
        final TableColumn<Code,Code>   codes = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Code));
        final TableColumn<Code,String> names = new TableColumn<>(vocabulary.getString(Vocabulary.Keys.Name));
        names.setCellValueFactory(codeList);
        codes.setCellValueFactory(IdentityValueFactory.instance());
        codes.setCellFactory(Code.Cell::new);
        codes.setMinWidth ( 60);            // Will be the initial size of this column.
        codes.setMaxWidth (120);            // Seems to be required for preventing `codes` to be as large as `names`.
        table.setPrefWidth(500);
        table.getColumns().setAll(codes, names);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        final Label clock = new Label("\u23F3");      // Unicode U+23F3: Hourglass With Flowing Sand.
        clock.setFont(Font.font(30));
        table.setPlaceholder(clock);
        /*
         * Controls on the top of CRS list. This is either a filter or a combox box
         * giving WKT format choices, depending on what is currently shown.
         */
        {// block for keeping variable locales.
            /*
             * Text field for filtering the list of CRS codes using keywords.
             * The filtering is applied when the "Enter" key is pressed in that field.
             */
            searchField = new TextField();
            searchField.setOnAction((ActionEvent event) -> {
                CodeFilter.apply(table, searchField.getText());
            });
            HBox.setHgrow(searchField, Priority.ALWAYS);
            final Label label = new Label(vocabulary.getString(Vocabulary.Keys.Filter));
            label.setLabelFor(searchField);
            /*
             * Button for showing the CRS description in Well Known Text (WKT) format.
             * The button is enabled only if a row in the table is selected.
             */
            final ToggleButton infoButton = new ToggleButton("\uD83D\uDDB9");   // Unicode U+1F5B9: Document With Text.
            table.getSelectionModel().selectedItemProperty().addListener((e,o,n) -> {
                infoButton.setDisable(n == null);
                updateSummary(n);
            });
            infoButton.setOnAction((ActionEvent event) -> {
                setTools(infoButton.isSelected());
            });
            infoButton.setDisable(true);
            /*
             * Creates the tools bar to show above the table of codes.
             * The tools bar contains the search field and the button for showing the WKT.
             */
            tools = new HBox(label, searchField, infoButton);
            tools.setSpacing(9);
            tools.setAlignment(Pos.BASELINE_LEFT);
            BorderPane.setMargin(tools, new Insets(0, 0, 9, 0));
        }
        /*
         * Details about the selected items. This is a form with the following lines:
         *   - Type (e.g. "Projected — Transverse Mercator").
         *   - Domain of validity.
         */
        {// block for keeping variable locales.
            final Label lt = new Label(vocabulary.getLabel(Vocabulary.Keys.Type));
            final Label ld = new Label(vocabulary.getLabel(Vocabulary.Keys.Domain));
            lt.setLabelFor(type   = new Label());
            ld.setLabelFor(domain = new Label());
            summary = Styles.createControlGrid(0, lt, ld);
            final Tooltip tp = new Tooltip();
            tp.setShowDelay(Duration.seconds(0.5));
            tp.setShowDuration(Duration.minutes(1));
            tp.maxWidthProperty().bind(summary.widthProperty());
            tp.setWrapText(true);
            domain.setTooltip(tp);
        }
        /*
         * Layout table and tools bar inside the dialog content.
         * Configure the dialog buttons.
         */
        final DialogPane pane = getDialogPane();
        content = new BorderPane();
        content.setCenter(table);
        content.setTop(tools);
        content.setBottom(summary);
        pane.setContent(content);
        pane.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        setTitle(i18n.getString(Resources.Keys.SelectCRS));
        setResultConverter(this::getSelectedCRS);
        setResizable(true);
    }

    /**
     * Sets the tools bar and its content to controls for the given mode.
     * If {@code wkt} is {@code true}, then this method set the controls for showing the WKT.
     * If {@code wkt} is {@code false} (the default), then this method set the controls to the table of CRS codes.
     */
    private void setTools(final boolean wkt) {
        final Locale locale = getAuthorityCodes().locale;
        final short labelText;
        final Control  control;
        final Control  main;
        final GridPane info;
        if (wkt) {
            if (wktPane == null) {
                wktPane = new WKTPane(locale);
            }
            wktPane.setContent(getAuthorityCodes(), table.getSelectionModel().getSelectedItem().code);
            labelText = Vocabulary.Keys.Format;
            control   = wktPane.convention;
            main      = wktPane.text;
            info      = null;
        } else {
            labelText = Vocabulary.Keys.Filter;
            control   = searchField;
            main      = table;
            info      = summary;
        }
        final ObservableList<Node> children = tools.getChildren();
        final Label label = (Label) children.get(0);
        final Vocabulary vocabulary = Vocabulary.forLocale(locale);
        label.setText(vocabulary.getLabel(labelText));
        label.setLabelFor(control);
        children.set(1, control);
        content.setCenter(main);
        content.setBottom(info);
    }

    /**
     * Returns the list of all authority codes. The list may not be complete at the
     * time this method returns because codes are loaded in a background thread.
     */
    private AuthorityCodes getAuthorityCodes() {
        ObservableList<?> items = table.getItems();
        if (items instanceof FilteredList<?>) {
            items = ((FilteredList<?>) items).getSource();
        }
        return (AuthorityCodes) items;
    }

    /**
     * Invoked when a new CRS is selected in the table. This method updates
     * the {@link #type} and {@link #domain} fields with CRS information.
     */
    private void updateSummary(final Code selected) {
        if (selected == null) {
            clearSummary();
            return;
        }
        final AuthorityCodes source = getAuthorityCodes();
        final String code = selected.code;
        BackgroundThreads.execute(new Task<CoordinateReferenceSystem>() {
            /** Invoked in background thread for fetching the CRS from an authority code. */
            @Override protected CoordinateReferenceSystem call() throws FactoryException {
                return source.getFactory().createCoordinateReferenceSystem(code);
            }

            /** Invoked in JavaFX thread on success. */
            @Override protected void succeeded() {
                final CoordinateReferenceSystem crs = getValue();
                type.setTextFill(Styles.NORMAL_TEXT);
                type.setText(typeOf(crs, source.locale));
                setDomainOfValidity(crs, source.locale);
            }

            /** Invoked in JavaFX thread on cancellation. */
            @Override protected void cancelled() {
                clearSummary();
            }

            /** Invoked in JavaFX thread on failure. */
            @Override protected void failed() {
                cancelled();
                type.setTextFill(Styles.ERROR_TEXT);
                type.setText(Exceptions.getLocalizedMessage(getException(), source.locale));
            }
        });
    }

    /**
     * Clears the {@link #type} and {@link #domain} fields.
     */
    private void clearSummary() {
        type.setText(null);
        domain.setText(null);
    }

    /**
     * Sets the text that describes the domain of validity.
     */
    private void setDomainOfValidity(final CoordinateReferenceSystem crs, final Locale locale) {
        String extent = null;
        if (crs != null) {
            extent = Extents.getDescription(crs.getDomainOfValidity(), locale);
        }
        String tip   = extent;
        Color  color = Styles.NORMAL_TEXT;
        if (!Utils.intersects(areaOfInterest, crs)) {
            tip    = Resources.forLocale(locale).getString(Resources.Keys.DoesNotCoverAOI);
            extent = Styles.WARNING_ICON + " " + (extent != null ? extent : tip);
            color  = Styles.ERROR_TEXT;
        }
        domain.setTextFill(color);
        domain.setText(extent);
        domain.getTooltip().setText(tip);
    }

    /**
     * Returns the text to show of right of the "type" label.
     */
    private static String typeOf(CoordinateReferenceSystem crs, final Locale locale) {
        while (crs instanceof CompoundCRS) {
            crs = ((CompoundCRS) crs).getComponents().get(0);
        }
        final short key;
        final int expected;
             if (crs instanceof GeographicCRS)  {key = Vocabulary.Keys.Geographic;  expected = 2;}
        else if (crs instanceof GeodeticCRS)    {key = Vocabulary.Keys.Geocentric;  expected = 2;}
        else if (crs instanceof VerticalCRS)    {key = Vocabulary.Keys.Vertical;    expected = 1;}
        else if (crs instanceof TemporalCRS)    {key = Vocabulary.Keys.Temporal;    expected = 1;}
        else if (crs instanceof ProjectedCRS)   {key = Vocabulary.Keys.Projected;   expected = 2;}
        else if (crs instanceof EngineeringCRS) {key = Vocabulary.Keys.Engineering; expected = 0;}
        else {
            key = Vocabulary.Keys.NotKnown;
            expected = 0;
        }
        String text = Vocabulary.forLocale(locale).getString(key);
        final int     dimension = CRS.getDimensionOrZero(crs);
        final boolean addDimension = (dimension != expected && expected != 0);
        final boolean isProjection = (crs instanceof GeneralDerivedCRS);
        if (addDimension | isProjection) {
            final StringBuilder buffer = new StringBuilder(text);
            if (addDimension) {
                buffer.append(" (").append(dimension).append("D)");
            }
            if (isProjection) {
                final Conversion conversion = ((GeneralDerivedCRS) crs).getConversionFromBase();
                if (conversion != null) {
                    final OperationMethod method = conversion.getMethod();
                    if (method != null) {
                        final String name = IdentifiedObjects.getDisplayName(method, locale);
                        if (name != null) {
                            buffer.append(" — ").append(name);
                        }
                    }
                }
            }
            text = buffer.toString();
        }
        return text;
    }

    /**
     * Returns the currently selected CRS, or {@code null} if none.
     *
     * @return the currently selected CRS, or {@code null}.
     */
    private CoordinateReferenceSystem getSelectedCRS(final ButtonType button) {
        if (ButtonType.OK.equals(button)) {
            final Code code = table.getSelectionModel().getSelectedItem();
            if (code != null) try {
                return getAuthorityCodes().getFactory().createCoordinateReferenceSystem(code.code);
            } catch (FactoryException e) {
                ExceptionReporter.canNotCreateCRS(getOwner(), code.code, e);
            }
        }
        return null;
    }

    /**
     * Shows a dialog to select a {@link CoordinateReferenceSystem}.
     *
     * @param  parent  parent frame of dialog, or {@code null} for an unowned dialog.
     * @return the selected {@link CoordinateReferenceSystem}, or empty if none.
     */
    public Optional<CoordinateReferenceSystem> showDialog(final Window parent) {
        initOwner(parent);
        return showAndWait();
    }
}
