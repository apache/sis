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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.concurrent.Task;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.referencing.gazetteer.MilitaryGridReferenceSystem;
import org.apache.sis.referencing.gazetteer.GazetteerException;
import org.apache.sis.referencing.gazetteer.GazetteerFactory;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.gui.internal.ExceptionReporter;
import org.apache.sis.gui.internal.GUIUtilities;
import org.apache.sis.gui.internal.NonNullObjectProperty;
import org.apache.sis.gui.internal.RecentChoices;
import org.apache.sis.gui.internal.io.OptionalDataDownloader;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;


/**
 * A short list (~10 items) of most recently used {@link ReferenceSystem}s.
 * The reference systems can be listed in {@link ChoiceBox} or i{@link Menu} controls.
 * The last choice is an "Other…" item which, when selected, popups the {@link CRSChooser}.
 *
 * <p>The choices are listed in following order:</p>
 * <ul>
 *   <li>The first choice is the native or preferred reference system of visualized data.
 *       That choice stay always in the first position.</li>
 *   <li>The last choice is "Other…" and stay always in the last position.</li>
 *   <li>All other choices between first and last are ordered with most recently used first.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.7
 * @since   1.1
 */
public class RecentReferenceSystems {
    /**
     * The {@value} value, for identifying code that assume two-dimensional objects.
     */
    static final int BIDIMENSIONAL = 2;

    /**
     * Number of reference systems to always show before all other reference systems.
     * They are the native of preferred reference system for the visualized data.
     *
     * @see #numCoreItems(List)
     */
    private static final int NUM_CORE_ITEMS = 1;

    /**
     * Number of reference systems to show in {@link ChoiceBox} or {@link MenuItem}s.
     * The {@value #NUM_CORE_ITEMS} core systems are included but not {@link #OTHER}.
     */
    private static final int NUM_SHOWN_ITEMS = 9;

    /**
     * Number of reference systems to keep at the end of the list.
     */
    private static final int NUM_OTHER_ITEMS = 1;

    /**
     * Key for use with the {@linkplain Menu#getProperties() property map} for storing the selected item.
     * Used for providing the functionality of {@link javafx.scene.control.CheckBox#selectedProperty()}
     * on controls that do not have an explicit selected property.
     */
    private static final String SELECTED_ITEM_KEY = "SelectedItem";

    /**
     * A pseudo-reference system for the "Other…" choice. We use a null value because {@link ChoiceBox}
     * seems to insist for inserting a null value in the items list when we remove the selected item.
     *
     * <h4>Maintenance note</h4>
     * If this field is changed to a non-null value,
     * search also for usages of {@code Object::nonNull} predicate.
     */
    static final ReferenceSystem OTHER = null;

    /**
     * The factory to use for creating a Coordinate Reference System from an authority code.
     * If {@code null}, then a default factory will be fetched when first needed.
     */
    private CRSAuthorityFactory factory;

    /**
     * Filter for reference systems to shown in the menus of combo boxes.
     * Used for showing only the reference systems that are compatible with the data.
     * A {@code null} value means that no filtering is applied.
     */
    private FilterByDatum filterByDatum;

    /**
     * The area of interest, or {@code null} if none. This is used for filtering the reference systems added by
     * {@code addAlternatives(…)} and for providing some guidance to user when {@link CRSChooser} is shown.
     *
     * <h4>API note</h4>
     * We do not provide getter/setter for this property; use {@link ObjectProperty#set(Object)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
     *
     * @see #addAlternatives(String...)
     * @see #addAlternatives(boolean, ReferenceSystem...)
     */
    public final ObjectProperty<Envelope> areaOfInterest;

    /**
     * Area of interest converted to geographic coordinates, or {@code null} if none.
     * This is recomputed automatically when {@link #areaOfInterest} is modified.
     */
    private ImmutableEnvelope geographicAOI;

    /**
     * The comparison criterion for considering two reference systems as a duplication.
     * The default value is {@link ComparisonMode#ALLOW_VARIANT}, i.e. axis orders are ignored.
     *
     * <h4>API note</h4>
     * We do not provide getter/setter for this property, use {@link ObjectProperty#set(Object)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
     */
    public final ObjectProperty<ComparisonMode> duplicationCriterion;

    /**
     * Values of controls created by this {@code RecentReferenceSystems} instance. We memorize those properties
     * because we sometime move items in the {@link #referenceSystems} list in a way that looks like we removed
     * items (before to insert them elsewhere), which can cause menus or combo boxes to clear their selection.
     * For preventing that, we use {@code controlValues} for saving the currently selected values before to
     * modify the item list, then restore the selections after we finished to modify the list.
     */
    private final List<WritableValue<ReferenceSystem>> controlValues;

    /**
     * The preferred locale for displaying object name, or {@code null} for the default locale.
     */
    final Locale locale;

    /**
     * The reference systems either as {@link ReferenceSystem} instances, {@link Unverified} wrappers or
     * {@link String} codes. All {@code String} elements should be authority codes that {@link #factory}
     * can recognize. The first item in this list should be the native or preferred reference system.
     * The {@link #OTHER} reference system is <em>not</em> included in this list.
     *
     * <p>The list content is specified by calls to {@code setPreferred(…)} and {@code addAlternatives(…)} methods,
     * then is filtered by {@link #filterReferenceSystems filterReferenceSystems(…)} for resolving authority codes
     * and removing duplicated elements. A view is returned by {@code filterReferenceSystems(…)} for excluding the
     * elements that are not applicable for the current <abbr>AOI</abbr> and datum types.</p>
     *
     * <p>All accesses to this field and all accesses to the {@link #isModified}
     * field shall be done in a block synchronized on {@code this}.</p>
     */
    private final List<Object> systemsOrCodes;

    /**
     * The {@link #systemsOrCodes} elements with all codes or wrappers replaced by {@link ReferenceSystem}
     * instances and duplicated values removed. This is the list given to JavaFX controls that we build.
     * This list includes {@link #OTHER} as its last item.
     *
     * @see #getReferenceSystems(boolean)
     */
    private final ObservableList<ReferenceSystem> referenceSystems;

    /**
     * A view of {@link #referenceSystems} with only items that are instances of {@link CoordinateReferenceSystem}.
     * This list includes also {@link #OTHER} as its last item. This list is used for menus shown in contexts where
     * identifiers cannot be used, for example for selecting the CRS to use for displaying a map.
     *
     * @see #getReferenceSystems(boolean)
     */
    private final ObservableList<ReferenceSystem> coordinateReferenceSystems;

    /**
     * A filtered view of {@link #referenceSystems} without the {@link #OTHER} item.
     * This is the list returned to users by public <abbr>API</abbr>.
     *
     * <h4>Design notes</h4>
     * The {@link #OTHER} item needs to exist in the list used internally by this class because those lists
     * are used directly by controls like {@code ChoiceBox<ReferenceSystem>}, with the {@link #OTHER} value
     * handled in a special way by {@link ObjectStringConverter} for making the "Other…" item present in the
     * list of choices. But since {@link #OTHER} is not a real CRS, we want to hide that trick to users.
     *
     * @see #getItems()
     */
    private final ObservableList<ReferenceSystem> publicItemList;

    /**
     * Coordinate reference systems used for computing cell indices of grid coverages.
     * Those reference systems are offered in a sub-menu and are not included in {@link #publicItemList}.
     * The content of this list depends on the grid coverages shown in the widget.
     *
     * @see #setGridReferencing(boolean, Map)
     */
    private final List<CoordinateReferenceSystem> cellIndiceSystems;

    /**
     * {@code true} if the {@link #referenceSystems} list needs to be rebuilt from {@link #systemsOrCodes} content.
     * This field shall be read and modified in a block synchronized on {@code this}.
     *
     * @see #refreshObservedReferenceSystemList()
     */
    private boolean isModified;

    /**
     * {@code true} if {@code RecentReferenceSystems} is in the process of modifying {@link #referenceSystems} list.
     * In such case we want to temporarily disable the {@link SelectionListener}.
     * This field is read and updated in JavaFX thread.
     */
    private boolean isAdjusting;

    /**
     * For detecting when the {@code RecentReferenceSystems} state has been modified concurrently.
     * Used only in contexts where the state is computed by a background thread.
     */
    private final AtomicInteger modificationCount;

    /**
     * Creates a builder which will use a default authority factory.
     * The factory will be capable to handle at least some <abbr>EPSG</abbr> codes.
     */
    public RecentReferenceSystems() {
        this(null, null);
    }

    /**
     * Creates a builder which will use the specified authority factory.
     *
     * @param  factory  the factory to use for building CRS from authority codes, or {@code null} for the default.
     * @param  locale   the preferred locale for displaying object name, or {@code null} for the default locale.
     *
     * @see org.apache.sis.referencing.CRS#getAuthorityFactory(String)
     */
    @SuppressWarnings("this-escape")    // `this` appears in a cyclic graph.
    public RecentReferenceSystems(final CRSAuthorityFactory factory, final Locale locale) {
        this.factory         = factory;
        this.locale          = locale;
        controlValues        = new ArrayList<>();
        systemsOrCodes       = new ArrayList<>();
        cellIndiceSystems    = new ArrayList<>();
        modificationCount    = new AtomicInteger();
        areaOfInterest       = new SimpleObjectProperty<>(this, "areaOfInterest");
        duplicationCriterion = new NonNullObjectProperty<>(this, "duplicationCriterion", ComparisonMode.ALLOW_VARIANT);
        duplicationCriterion.addListener((e) -> refreshObservedReferenceSystemList());
        areaOfInterest.addListener((e,o,n) -> {
            geographicAOI = Utils.toGeographic(RecentReferenceSystems.class, "areaOfInterest", n);
            refreshObservedReferenceSystemList();
        });
        referenceSystems = FXCollections.observableArrayList();
        publicItemList = new FilteredList<>(referenceSystems, Objects::nonNull);
        coordinateReferenceSystems = new FilteredList<>(referenceSystems, (ReferenceSystem system) -> {
            return (system == OTHER) || (system instanceof CoordinateReferenceSystem);
        });
    }

    /**
     * Sets the reference systems, area of interest and "referencing by grid indices" systems.
     * Contrarily to other methods in this class, this method can be invoked from any thread.
     * This method performs the following tasks, where the methods cited below are invoked from the JavaFX thread:
     *
     * <ul>
     *   <li>Invokes {@link #setPreferred(boolean, ReferenceSystem)} with the first <abbr>CRS</abbr> in iteration order.</li>
     *   <li>Invokes {@link #addAlternatives(boolean, ReferenceSystem...)} once for all other <abbr>CRS</abbr>s.</li>
     *   <li>Sets the {@link #areaOfInterest} to the union of all envelopes.</li>
     *   <li>Sets the content of "Referencing by cell indices" sub-menu.</li>
     * </ul>
     *
     * Above information are derived from the values of the {@code geometries} map.
     * For each entry, the map key should be the {@link org.apache.sis.storage.Resource#getIdentifier() identifier} of
     * the resource that provided the {@link GridGeometry} value, or other text allowing the user to identify the resource.
     * Those keys are used for naming the <abbr>CRS</abbr>s of cell coordinates, which are different for each grid coverage.
     * See the {@linkplain GridGeometry#createGridCRS grid <abbr>grid CRS</abbr> documentation} for more details.
     *
     * <p>The {@code replaceByAuthoritativeDefinition} argument specifies whether the coordinate reference systems
     * should be replaced by authoritative definitions when such definitions are found. If {@code true} then,
     * for example, a <q>WGS 84</q> geographic <abbr>CRS</abbr> with (λ, ϕ) axis order may be replaced by the
     * "EPSG::4326" definition with (ϕ, λ) axis order.</p>
     *
     * @param  replaceByAuthoritativeDefinition  whether the reference systems should be replaced by authoritative definition.
     * @param  geometries  grid coverage names together with their grid geometry. May be empty.
     *
     * @since 1.3
     */
    public void setGridReferencing(final boolean replaceByAuthoritativeDefinition,
                                   final Map<Identifier, GridGeometry> geometries)
    {
        /*
         * Fetch or compute information needed, but without modifying the state of this object yet.
         * All assignments to `this` should be done inside the `try … finally` block.
         */
        int countCRS = 0;
        int countCIR = 0;
        final var refsys    = new CoordinateReferenceSystem[geometries.size()];
        final var derived   = new CoordinateReferenceSystem[refsys.length];
        final var envelopes = new Envelope[refsys.length];
        for (final Map.Entry<Identifier, GridGeometry> entry : geometries.entrySet()) {
            GridGeometry gg = entry.getValue();
            if (gg.isDefined(GridGeometry.CRS)) {
                if (gg.isDefined(GridGeometry.ENVELOPE)) {
                    envelopes[countCRS] = gg.getEnvelope();
                }
                refsys[countCRS++] = gg.getCoordinateReferenceSystem();
            }
            final GridExtent extent = gg.getExtent();
            gg = gg.selectDimensions(extent.getSubspaceDimensions(Math.max(extent.getDegreesOfFreedom(), BIDIMENSIONAL)));
            try {
                derived[countCIR] = gg.createGridCRS(entry.getKey(), PixelInCell.CELL_CENTER);
                countCIR++;     // Increment only if above line was successful.
            } catch (FactoryException e) {
                errorOccurred(e);
            }
        }
        if (countCRS == 0 && countCIR != 0) {
            refsys[0] = derived[0];
        }
        Envelope union;
        try {
            union = Envelopes.union(envelopes);       // Null elements are ignored.
        } catch (TransformException e) {
            errorOccurred("setGridReferencing", e);
            union = null;
        }
        final Envelope aoi = union;     // Because lambda functions want a final variable.
        final FilterByDatum filter = FilterByDatum.create(refsys);
        final List<CoordinateReferenceSystem> cellCRS = Containers.viewAsUnmodifiableList(derived, 0, countCIR);
        final int stamp = modificationCount.incrementAndGet();
        Platform.runLater(() -> {
            if (modificationCount.get() == stamp) {
                final boolean old = isAdjusting;
                try {
                    isAdjusting = true;
                    if (refsys.length != 0) {
                        setPreferred(replaceByAuthoritativeDefinition, refsys[0]);
                        refsys[0] = null;
                        addAlternatives(replaceByAuthoritativeDefinition, refsys);  // Null elements are ignored.
                    }
                    cellIndiceSystems.clear();
                    cellIndiceSystems.addAll(cellCRS);
                    filterByDatum = filter;
                    areaOfInterest.set(aoi);
                } finally {
                    isAdjusting = old;
                }
                refreshObservedReferenceSystemList();
            }
        });
    }

    /**
     * Sets the native or preferred reference system. This is the system to always show as the first
     * choice and should typically be the native {@link CoordinateReferenceSystem} of visualized data.
     * If a previous preferred system existed, the previous system will be moved to alternative choices.
     *
     * <p>The {@code replaceByAuthoritativeDefinition} argument specifies whether the given reference system
     * should be replaced by an authoritative definition when such definition is found. If {@code true} then,
     * for example, a <q>WGS 84</q> geographic <abbr>CRS</abbr> with (λ, ϕ) axis order may be replaced by the
     * "EPSG::4326" definition with (ϕ, λ) axis order.</p>
     *
     * @param  replaceByAuthoritativeDefinition  whether the given system should be replaced by authoritative definition.
     * @param  system  the native or preferred reference system to show as the first choice.
     */
    public synchronized void setPreferred(final boolean replaceByAuthoritativeDefinition, final ReferenceSystem system) {
        ArgumentChecks.ensureNonNull("system", system);
        systemsOrCodes.add(0, replaceByAuthoritativeDefinition ? new Unverified(system) : system);
        refreshObservedReferenceSystemList();
    }

    /**
     * Sets the native or preferred reference system as an authority code. This is the system to always show as
     * the first choice and should typically be the native {@link CoordinateReferenceSystem} of visualized data.
     * If a previous preferred system existed, the previous system will be moved to alternative choices.
     *
     * <p>If the given code is not recognized, then the error will be notified at some later time by a call to
     * {@link #errorOccurred(FactoryException)} in a background thread and the given code will be silently ignored.
     * This behavior allows the use of codes that depend on whether an optional dependency is present or not,
     * in particular the <a href="https://sis.apache.org/epsg.html">EPSG dataset</a>.</p>
     *
     * @param  code  authority code of the native of preferred reference system to show as the first choice.
     */
    public synchronized void setPreferred(final String code) {
        ArgumentChecks.ensureNonEmpty("code", code);
        systemsOrCodes.add(0, code);
        refreshObservedReferenceSystemList();
    }

    /**
     * Returns the number of reference systems to always show before all other reference systems.
     * This is {@link #NUM_CORE_ITEMS}, but taking in account the case when the list is empty.
     */
    private static int numCoreItems(final List<?> items) {
        int n = Math.min(items.size(), NUM_CORE_ITEMS);
        if (n != 0 && items.get(n-1) == OTHER) n--;
        return n;
    }

    /**
     * Invoked when a new reference system is selected and has not been found in the current list of systems.
     * It may be, for example, a system selected among the content of a registry shown by {@link CRSChooser}.
     * The new system is added at the beginning of the list but after the preferred (native) reference system.
     * Because the added system is considered as one alternative among others, it may be ignored.
     * Menu items will be updated in background thread.
     */
    final synchronized void addSelectedItem(final ReferenceSystem system) {
        if (isAuthoritative(system)) {
            systemsOrCodes.add(numCoreItems(systemsOrCodes), system);
            refreshObservedReferenceSystemList();
        }
    }

    /**
     * Adds the given reference systems to the list of alternative choices.
     * If there are duplicated values in the given list or with previously added systems,
     * then only the first occurrence of duplicated values is retained.
     * If an {@linkplain #areaOfInterest area of interest} (AOI) is specified,
     * then reference systems that do not intersect the AOI will be hidden.
     *
     * <p>The {@code replaceByAuthoritativeDefinition} argument specifies whether the given reference systems
     * should be replaced by authoritative definitions when such definitions are found. If {@code true} then,
     * for example, a <q>WGS 84</q> geographic <abbr>CRS</abbr> with (λ, ϕ) axis order may be replaced by the
     * "EPSG::4326" definition with (ϕ, λ) axis order.</p>
     *
     * @param  replaceByAuthoritativeDefinition  whether the given systems should be replaced by authoritative definitions.
     * @param  systems  the reference systems to add as alternative choices. Null elements are ignored.
     */
    public synchronized void addAlternatives(final boolean replaceByAuthoritativeDefinition, final ReferenceSystem... systems) {
        ArgumentChecks.ensureNonNull("systems", systems);
        for (final ReferenceSystem system : systems) {
            if (system != null) {
                systemsOrCodes.add(replaceByAuthoritativeDefinition ? new Unverified(system) : system);
            }
        }
        refreshObservedReferenceSystemList();
        // Check for duplication will be done in `filterReferenceSystems(…)` method.
    }

    /**
     * Adds the coordinate reference system identified by the given authority codes.
     * If there are duplicated values in the given list or with previously added systems,
     * then only the first occurrence of duplicated values is retained.
     * If an {@linkplain #areaOfInterest area of interest} (<abbr>AOI</abbr>) is specified,
     * then reference systems that do not intersect the <abbr>AOI</abbr> will be hidden.
     *
     * <p>If a code is not recognized, then the error will be notified at some later time by a call to
     * {@link #errorOccurred(FactoryException)} in a background thread and the code will be silently ignored.
     * This behavior allows the use of codes that depend on whether an optional dependency is present or not,
     * in particular the <a href="https://sis.apache.org/epsg.html">EPSG dataset</a>.</p>
     *
     * @param  codes  authority codes of the coordinate reference systems to add as alternative choices.
     *                Null or empty elements are ignored.
     */
    public synchronized void addAlternatives(final String... codes) {
        ArgumentChecks.ensureNonNull("codes", codes);
        for (String code : codes) {
            code = Strings.trimOrNull(code);
            if (code != null) {
                systemsOrCodes.add(code);
            }
        }
        refreshObservedReferenceSystemList();
        // Parsing will be done in `filterReferenceSystems(…)` method.
    }

    /**
     * Adds the coordinate reference systems saved in user preferences. The user preferences are determined
     * from the reference systems observed during current execution or previous executions of JavaFX application.
     * If an {@linkplain #areaOfInterest area of interest} (<abbr>AOI</abbr>) is specified,
     * then reference systems that do not intersect the <abbr>AOI</abbr> will be ignored.
     */
    public void addUserPreferences() {
        addAlternatives(RecentChoices.getReferenceSystems());
    }

    /**
     * Returns whether the given object seems authoritative. For example, if two objects are considered
     * equivalent according the {@linkplain #duplicationCriterion duplication criterion}, then the one
     * with an identifier (typically an <abbr>EPSG</abbr> code) is preferred. Objects without identifier
     * are typically a <abbr>CRS</abbbr> with non-standard axis order, for example for an image which has
     * just been read.
     */
    private static boolean isAuthoritative(final ReferenceSystem object) {
        return IdentifiedObjects.getIdentifier(object, null) != null;
    }

    /**
     * Filters the {@link #systemsOrCodes} list by resolving and verifying the reference systems.
     * This is invoked by {@link #refreshObservedReferenceSystemList()} in a background thread.
     * First, this method modifies {@link #systemsOrCodes} as below:
     *
     * <ul>
     *   <li>Authority codes are resolved if possible or removed if they cannot be resolved.</li>
     *   <li>Unverified <abbr>CRS</abbr>s are replaced by authoritative definitions when a match is found.</li>
     *   <li>Duplications are removed.</li>
     * </ul>
     *
     * Then, this method returns a list which hides the following elements,
     * but without removing them from the {@link #systemsOrCodes} list:
     *
     * <ul>
     *   <li>Reference systems with incompatible datum types.</li>
     *   <li>Reference systems with a domain of validity outside the {@link #geographicAOI}.</li>
     * </ul>
     *
     * The caller will copy the returned list into {@link #referenceSystems} in the JavaFX thread.
     *
     * @param  filter  the {@link #filterByDatum} value read from JavaFX thread, or {@code null} if none.
     * @param  domain  the {@link #areaOfInterest} value read from JavaFX thread, or {@code null} if none.
     * @param  mode    the {@link #duplicationCriterion} value read from JavaFX thread.
     * @return the filtered reference systems, or {@code null} if already filtered.
     */
    private synchronized List<ReferenceSystem> filterReferenceSystems(
            final FilterByDatum     filter,
            final ImmutableEnvelope domain,
            final ComparisonMode    mode)
    {
        if (!isModified) {
            return null;    // Another thread already did the work.
        }
        boolean noFactoryFound = false;
        boolean searchedFinder = false;
        IdentifiedObjectFinder finder = null;
        final var gf = new GazetteerFactory();
        for (int i = systemsOrCodes.size(); --i >= 0;) {
            final Object item = systemsOrCodes.get(i);
            if (item instanceof ReferenceSystem) {
                continue;
            }
            ReferenceSystem system = null;
            if (item != OTHER) try {
                if (item instanceof String) {
                    /*
                     * The current list element is an authority code such as "EPSG::4326".
                     * Replace that code by the full `CoordinateReferenceSystem` instance.
                     * Note that authority factories are optional, so it is okay if we can
                     * not resolve the code. In such case the item will be removed.
                     */
                    system = gf.forNameIfKnown((String) item).orElse(null);
                    if (system == null && !noFactoryFound) {
                        if (factory == null) {
                            factory = Utils.getDefaultFactory();
                        }
                        system = factory.createCoordinateReferenceSystem((String) item);
                    }
                } else if (item instanceof Unverified) {
                    /*
                     * The current list element is a `ReferenceSystem` instance but maybe not
                     * conform to authoritative definition, for example regarding axis order.
                     * If we can find an authoritative definition, do the replacement.
                     * If this operation cannot be done, accept the reference system as-is.
                     */
                    if (!searchedFinder) {
                        searchedFinder = true;          // Set now in case an exception is thrown.
                        if (factory instanceof GeodeticAuthorityFactory f) {
                            finder = f.newIdentifiedObjectFinder();
                        } else {
                            finder = IdentifiedObjects.newFinder(null);
                        }
                        finder.setIgnoringAxes(true);
                    }
                    system = ((Unverified) item).find(finder);
                }
            } catch (FactoryException e) {
                errorOccurred(e);
                noFactoryFound = (factory == null);
            } catch (GazetteerException e) {
                errorOccurred("getReferenceSystems", e);
                // Note: `getReferenceSystems(…)` is indirectly the caller of this method.
            }
            if (system != null) {
                systemsOrCodes.set(i, system);
            } else {
                systemsOrCodes.remove(i);
            }
        }
        /*
         * Search for duplicated values after we finished filtering. This block is inefficient
         * (execution time of O(N²)) but it should not be an issue if this list is short (e.g.
         * 20 elements). We cut the list if we reach the maximal number of systems to keep.
         */
        for (int i=0, j; i < (j = systemsOrCodes.size()); i++) {
            if (i >= RecentChoices.MAXIMUM_REFERENCE_SYSTEMS) {
                systemsOrCodes.subList(i, j).clear();
                break;
            }
            var item = (ReferenceSystem) systemsOrCodes.get(i);
            while (--j > i) {
                if (Utilities.deepEquals(item, systemsOrCodes.get(j), mode)) {
                    final var removed = (ReferenceSystem) systemsOrCodes.remove(j);
                    if (isAuthoritative(removed) && !isAuthoritative(item)) {
                        // Keep the instance which has an authority code.
                        systemsOrCodes.set(i, item = removed);
                    }
                }
            }
        }
        /*
         * Finished to filter the `systemsOrCodes` list: all elements are now guaranteed to be
         * `ReferenceSystem` instances with no duplicated values. Copy those reference systems
         * in a separated list as a protection against concurrent changes in `systemsOrCodes`,
         * and for retaining only the reference systems that are valid in the area of interest.
         * We do not remove hidden CRS because they may become valid later if the AOI changes.
         */
        final int n = systemsOrCodes.size();
        final var systems = new ArrayList<ReferenceSystem>(Math.min(NUM_SHOWN_ITEMS, n) + NUM_OTHER_ITEMS);
        for (int i=0; i<n; i++) {
            final var system = (ReferenceSystem) systemsOrCodes.get(i);
            if (system != OTHER && (filter == null || filter.test(system))) {
                if (i < NUM_CORE_ITEMS || Utils.intersects(domain, system)) {
                    if (Utils.isIgnoreable(system)) continue;   // Ignore "Computer display" CRS.
                    systems.add(system);
                    if (systems.size() >= NUM_SHOWN_ITEMS) break;
                }
            }
        }
        systems.add(OTHER);
        isModified = false;
        return systems;
    }

    /**
     * Updates the {@link #referenceSystems} list with the elements added into the {@link #systemsOrCodes} list.
     * This method must be invoked from the JavaFX thread. The <abbr>CRS</abbr> will be processed in background
     * thread and transferred later (in the JavaFX thread) to the {@link #referenceSystems} list when ready.
     * This copy will cause an update of the {@link ChoiceBox} and {@link MenuItem} controls created by this class.
     */
    private synchronized void refreshObservedReferenceSystemList() {
        isModified = true;  // Will be reset to `false` after `filterReferenceSystems(…)` finished.
        if (isAdjusting) {
            return;         // `refreshObservedReferenceSystemList()` will be invoked again later.
        }
        /*
         * Prepare a temporary list as the concatenation of all items that are currently visible in JavaFX
         * controls with all items that were specified by `setPreferred(…)` or `addAlternatives(…)` methods.
         * This concatenation creates a lot of duplicated values, but those duplications will be filtered by
         * `filterReferenceSystems(…)` method. The intent is to preserve following order:
         *
         *   - NUM_CORE_ITEMS preferred reference systems first.
         *   - All reference systems that are currently selected by JavaFX controls.
         *   - All reference systems offered as choice in JavaFX controls.
         *   - All reference systems specified by `addAlternatives(…)`.
         *   - NUM_OTHER_ITEMS systems (will be handled in a special way by `filterReferenceSystems(…)`).
         *
         * The list will be truncated to NUM_SHOWN_ITEMS after duplications are removed and before OTHER
         * is added. The first occurrence of duplicated values is kept, which will result in above-cited
         * order as the priority order where to insert the CRS.
         */
        final int insertAt = numCoreItems(systemsOrCodes);
        final List<ReferenceSystem> selected = getSelectedItems();
        systemsOrCodes.addAll(insertAt, selected);
        systemsOrCodes.addAll(insertAt + selected.size(), referenceSystems);
        final FilterByDatum filter = filterByDatum;
        final ImmutableEnvelope domain = geographicAOI;
        final ComparisonMode mode = duplicationCriterion.get();
        BackgroundThreads.execute(new Task<List<ReferenceSystem>>() {
            /** Filters the {@link ReferenceSystem}s in a background thread. */
            @Override protected List<ReferenceSystem> call() {
                return filterReferenceSystems(filter, domain, mode);
            }

            /** Should never happen. */
            @Override protected void failed() {
                ExceptionReporter.show(null, this);
            }

            /** Sets the {@link ChoiceBox} content to the list computed in background thread. */
            @Override protected void succeeded() {
                setReferenceSystems(getValue(), mode);
            }
        });
    }

    /**
     * Sets the reference systems to the given content. The given list is often similar to current content,
     * for example with only a reference system that moved to a different index. This method compares the
     * given list with current one and tries to fire as few change events as possible.
     *
     * @param  systems  the new reference systems, or {@code null} for no changes.
     * @param  mode     the value of {@link #duplicationCriterion} at the time the
     *                  {@code systems} list has been computed.
     */
    private void setReferenceSystems(final List<ReferenceSystem> systems, final ComparisonMode mode) {
        // Nested calls to this method should never happen, but check !isAdjusting anyway as safety.
        if (systems != null && !isAdjusting) try {
            isAdjusting = true;
            /*
             * The call to `copyAsDiff(…)` may cause some ChoiceBox values to be lost if the corresponding
             * item in the `referenceSystems` list is temporarily removed before to be inserted elsewhere.
             * Save the values before to modify the list.
             */
            final ReferenceSystem[] values = controlValues.stream().map(WritableValue::getValue).toArray(ReferenceSystem[]::new);
            if (GUIUtilities.copyAsDiff(systems, referenceSystems)) {
                notifyChanges();
            }
            /*
             * Restore the previous selections, or a variant of these selections. For example, if the value
             * is a `GeographicCRS` with (λ,φ) axis order which was set by a call to `ChoiceBox.setValue(…)`,
             * that value may have been replaced in the list by a CRS with (φ,λ) axis order. We must replace
             * the selected value by an instance from the list, otherwise it will not appear as selected.
             */
            final int n = referenceSystems.size();
            for (int j=0; j<values.length; j++) {
                ReferenceSystem system = values[j];
                if (system != null) {
                    for (int i=0; i<n; i++) {
                        final ReferenceSystem candidate = referenceSystems.get(i);
                        if (Utilities.deepEquals(candidate, system, mode)) {
                            system = candidate;
                            break;
                        }
                    }
                    controlValues.get(j).setValue(system);
                }
            }
        } finally {
            isAdjusting = false;
        }
    }

    /**
     * Returns the list of reference systems to show in a {@link Menu} or {@link ChoiceBox} control.
     * The returned list may be temporarily empty or outdated, and updated later after a background
     * thread finished to update the list.
     *
     * @param  renderable  whether the list should be restricted to items that can be used for rendering maps.
     * @return the list of items. May be empty on return and populated later.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private synchronized ObservableList<ReferenceSystem> getReferenceSystems(final boolean renderable) {
        if (isModified) {
            refreshObservedReferenceSystemList();
        }
        return renderable ? coordinateReferenceSystems : referenceSystems;
    }

    /**
     * Invoked when the user selects a reference system in a choice box or a menu.
     * This listener intercepts the case where the selected item is "Other…", in which case {@code SelectionListener}
     * popups a {@link CRSChooser}, then {@linkplain #addSelectedItem adds the user's selection} to the system list.
     */
    final class SelectionListener implements ChangeListener<ReferenceSystem> {
        /** The user-specified action to execute when a reference system is selected. */
        final ChangeListener<ReferenceSystem> action;

        /** Creates a new listener of reference system selection. */
        private SelectionListener(final ChangeListener<ReferenceSystem> action) {
            this.action = action;
        }

        /** The manager of reference systems to synchronize with. */
        final RecentReferenceSystems owner() {
            return RecentReferenceSystems.this;
        }

        /** Invoked when the user selects a reference system or the "Other…" item. */
        @SuppressWarnings("unchecked")
        @Override public void changed(final ObservableValue<? extends ReferenceSystem> property,
                                      final ReferenceSystem oldValue, ReferenceSystem newValue)
        {
            if (isAdjusting) {
                action.changed(property, oldValue, newValue);
                return;
            }
            final ComparisonMode mode = duplicationCriterion.get();
            if (newValue == OTHER) {
                final var chooser = new CRSChooser(factory, filterByDatum, geographicAOI, locale);
                newValue = chooser.showDialog(GUIUtilities.getWindow(property)).orElse(null);
                if (newValue == null) {
                    newValue = oldValue;
                } else {
                    /*
                     * If user selected a CRS in the CRSChooser list, verify if her/his selection is a CRS
                     * already presents in the `referenceSystems` list. We ignore axis order (by default)
                     * because the previous CRS in the list may be a CRS given by `setPreferred(…)` method,
                     * which typically come from a DataStore. That previous CRS will be replaced by the CRS
                     * given by CRSChooser, which is more conform to authoritative definition.
                     */
                    final ObservableList<ReferenceSystem> items = referenceSystems;
                    int count = items.size() - NUM_OTHER_ITEMS;
                    boolean found = false;
                    for (int i=0; i<count; i++) {
                        if (Utilities.deepEquals(newValue, items.get(i), mode)) {
                            if (i >= NUM_CORE_ITEMS) {
                                items.set(i, newValue);
                            }
                            found = true;
                            break;
                        }
                    }
                    /*
                     * If the selected CRS was not present in the list, we may need to remove the last item
                     * for making room for the new one. New item must be added before `property.setValue(…)`
                     * is invoked, otherwise ChoiceBox may add a new item by itself.
                     */
                    if (!found) {
                        if (count >= NUM_SHOWN_ITEMS) {
                            final List<ReferenceSystem> selected = getSelectedItems();
                            for (int i = count; --i >= NUM_CORE_ITEMS;) {
                                if (!selected.contains(items.get(i))) {         // Do not remove selected items.
                                    items.remove(i);                            // Remove an item before `OTHER`.
                                    if (--count < NUM_SHOWN_ITEMS) break;
                                }
                            }
                        }
                        items.add(numCoreItems(items), newValue);
                        notifyChanges();
                    }
                }
                /*
                 * Following cast is safe because this listener is registered only on ObjectProperty
                 * instances, and the ObjectProperty class implements WritableValue.
                 * The effect of this method call is to set the selected value.
                 */
                ((WritableValue<ReferenceSystem>) property).setValue(newValue);
            }
            if (oldValue != newValue) {
                /*
                 * If the selected CRS is already at the beginning of the list, do nothing. The beginning is
                 * either one of the core items (specified by `setPreferred(…)`) or the first item after the
                 * core items.
                 */
                final ObservableList<ReferenceSystem> items = referenceSystems;
                final int count = items.size() - NUM_OTHER_ITEMS;
                for (int i = Math.min(count, NUM_CORE_ITEMS + 1); --i >= 0;) {
                    final ReferenceSystem current = items.get(i);
                    if (Utilities.deepEquals(current, newValue, mode)) {
                        action.changed(property, oldValue, current);
                        return;
                    }
                }
                /*
                 * Move the selected reference system as the first choice after the core systems.
                 * We need to remove the old value before to add the new one, otherwise it seems
                 * to confuse the list.
                 */
                for (int i = count; --i >= NUM_CORE_ITEMS;) {
                    if (Utilities.deepEquals(items.get(i), newValue, mode)) {
                        newValue = items.remove(i);
                        break;
                    }
                }
                items.add(numCoreItems(items), newValue);
                /*
                 * Notify the user-specified listeners. It will typically starts a background process.
                 * We test (oldValue != newValue) again because `newValue` may have been replaced.
                 */
                notifyChanges();
                if (oldValue != newValue) {
                    action.changed(property, oldValue, newValue);       // Typically starts a background process.
                }
                RecentChoices.useReferenceSystem(IdentifiedObjects.toString(IdentifiedObjects.getIdentifier(newValue, null)));
            }
        }
    }

    /**
     * Notifies all {@link MenuSync} that the list of reference systems changed. We send a notification manually
     * instead of relying on {@code ListChangeListener} because we want to process only one event after we have
     * done a bunch of changes instead of an event after each individual add or remove operation.
     */
    private void notifyChanges() {
        for (final WritableValue<ReferenceSystem> value : controlValues) {
            if (value instanceof MenuSync) {
                ((MenuSync) value).notifyChanges();
            }
        }
    }

    /**
     * Returns all reference systems in the order they appear in JavaFX controls. The first element
     * is the {@link #setPreferred(boolean, ReferenceSystem) preferred} (or native) reference system.
     * All other elements are {@linkplain #addAlternatives(boolean, ReferenceSystem...) alternatives}.
     *
     * @return all reference systems in the order they appear in JavaFX controls.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public ObservableList<ReferenceSystem> getItems() {
        return publicItemList;
    }

    /**
     * Returns all currently selected reference systems in the order they appear in JavaFX controls.
     * This method collects selected values of all controls created by a {@code createXXX(…)} method.
     * The returned list does not contain duplicated values.
     *
     * @return currently selected values of all controls, without duplicated values and in the order
     *         they appear in choice lists.
     */
    public List<ReferenceSystem> getSelectedItems() {
        /*
         * Build an array of selected reference systems. This array may contain duplicated elements if two
         * or more JavaFX controls have the same selected value. Those duplications will be resolved later.
         * Conceptually we will use this array as a java.util.Set, except that its length is so small
         * (usually no more than 3 elements) that it is not worth to use HashSet.
         */
        int count = 0;
        final var selected = new ReferenceSystem[controlValues.size()];
        for (final WritableValue<ReferenceSystem> value : controlValues) {
            final ReferenceSystem system = value.getValue();
            if (system != null) selected[count++] = system;
        }
        /*
         * Now filter the `referenceSystems` list, retaining only elements that are present in `selected`.
         * We do that way for having selected elements in the same order as they appear in JavaFX controls.
         */
        final var ordered = new ArrayList<ReferenceSystem>(count);
        if (count != 0) {
            // (count > 0) implies (referenceSystems != null).
            for (final ReferenceSystem system : referenceSystems) {
                if (system != OTHER) {
                    for (int i=0; i<count; i++) {
                        if (selected[i] == system) {
                            ordered.add(system);
                            if (--count == 0) return ordered;
                            System.arraycopy(selected, i+1, selected, i, count - i);
                            break;
                        }
                    }
                }
            }
            /*
             * If some selected elements were not found in the `referenceSystems` list, add them last.
             * It should not happen, unless those remaining elements are duplicated values (i.e. two
             * or more controls having the same selection).
             */
next:       for (int i=0; i<count; i++) {
                final ReferenceSystem system = selected[i];
                for (int j = ordered.size(); --j >= 0;) {
                    if (ordered.get(j) == system) {
                        continue next;                  // Skip duplicated value.
                    }
                }
                ordered.add(system);
            }
        }
        return ordered;
    }

    /**
     * Creates a box offering choices among the reference systems specified to this {@code RecentReferenceSystems}.
     * The returned control may be initially empty and be automatically populated at a later time,
     * after a background thread finished to process the list of reference systems.
     *
     * <p>If the {@code renderable} argument is {@code true}, then the choice box will contain only reference systems
     * that can be used for rendering purposes. The list includes {@link CoordinateReferenceSystem} instances
     * but not reference systems by identifiers such as {@linkplain MilitaryGridReferenceSystem MGRS}.</p>
     *
     * <h4>Limitations</h4>
     * There is currently no mechanism for disposing the returned control. For garbage collecting the
     * returned {@code ChoiceBox}, this {@code RecentReferenceSystems} must be garbage-collected as well.
     *
     * @param  renderable  whether the choice box should be restricted to items that can be used for rendering maps.
     * @param  action      the action to execute when a reference system is selected.
     * @return a choice box with reference systems specified by {@code setPreferred(…)} and {@code addAlternatives(…)} methods.
     *
     * @since 1.3
     */
    public ChoiceBox<ReferenceSystem> createChoiceBox(final boolean renderable, final ChangeListener<ReferenceSystem> action) {
        ArgumentChecks.ensureNonNull("action", action);
        final var choices = new ChoiceBox<ReferenceSystem>(getReferenceSystems(renderable));
        choices.setConverter(new ObjectStringConverter<>(choices.getItems(), locale));
        choices.valueProperty().addListener(new SelectionListener(action));
        controlValues.add(choices.valueProperty());
        return choices;
    }

    /**
     * Creates a menu offering choices among the reference systems specified to this {@code RecentReferenceSystems}.
     * The returned control may be initially empty and be automatically populated at a later time,
     * after a background thread finished to process the list of reference systems.
     *
     * <p>If the {@code renderable} argument is {@code true}, then the menu will contain only reference systems
     * that can be used for rendering purposes. The list includes {@link CoordinateReferenceSystem} instances
     * but not reference systems by identifiers such as {@linkplain MilitaryGridReferenceSystem MGRS}.</p>
     *
     * <h4>Limitations</h4>
     * There is currently no mechanism for disposing the returned control. For garbage collecting the
     * returned {@code Menu}, this {@code RecentReferenceSystems} must be garbage-collected as well.
     *
     * @param  renderable  whether the menu should be restricted to items that can be used for rendering maps.
     * @param  action      the action to execute when a reference system is selected.
     * @return a menu with reference systems specified by {@code setPreferred(…)} and {@code addAlternatives(…)} methods.
     *
     * @since 1.3
     */
    public Menu createMenuItems(final boolean renderable, final ChangeListener<ReferenceSystem> action) {
        ArgumentChecks.ensureNonNull("action", action);
        final List<ReferenceSystem> main = getReferenceSystems(renderable);
        final List<CoordinateReferenceSystem> derived = (renderable) ? null : cellIndiceSystems;
        final var menu = new Menu(Vocabulary.forLocale(locale).getString(Vocabulary.Keys.ReferenceSystem));
        final var property = new MenuSync(main, !renderable, derived, menu, new SelectionListener(action));
        menu.getProperties().put(SELECTED_ITEM_KEY, property);
        controlValues.add(property);
        return menu;
    }

    /**
     * Returns the property for the selected value in a menu created by {@code createMenuItems(…)}.
     *
     * @param  menu  the menu, or {@code null} if none.
     * @return the property for the selected value, or {@code null} if none.
     */
    public static ObjectProperty<ReferenceSystem> getSelectedProperty(final Menu menu) {
        if (menu != null) {
            if (menu.getProperties().get(SELECTED_ITEM_KEY) instanceof MenuSync property) {
                return property;
            }
        }
        return null;
    }

    /**
     * Invoked when an error occurred while filtering a {@link ReferenceSystem} instance.
     * The error may be a failure to convert an EPSG code to a {@link CoordinateReferenceSystem} instance,
     * or an error during a CRS verification. Some errors may be normal, for example because EPSG dataset
     * is not expected to be present in every runtime environments. The consequence of this error is "only"
     * that the CRS will not be listed among the reference systems that the user can choose.
     *
     * <p>The default implementation popups an alert dialog only if the error occurred after the user
     * accepted to {@linkplain org.apache.sis.setup.OptionalInstallations download optional dependencies},
     * because the error may be caused by a problem related to the download operation.
     * Otherwise this method only logs the error at {@link java.util.logging.Level#FINE}.
     * No other processing is done; user is not notified unless (s)he paid attention to loggings.</p>
     *
     * @param  e  the error that occurred.
     */
    protected void errorOccurred(final FactoryException e) {
        OptionalDataDownloader.reportIfInstalling(e);
        Logging.recoverableException(LOGGER, RecentReferenceSystems.class, "getReferenceSystems", e);
    }

    /**
     * Invoked when an error other than {@link FactoryException} occurred.
     * The error shall be recoverable, e.g. by ignoring a menu item.
     *
     * @param  caller  the method to report as the source the in log record.
     * @param  e  the error that occurred.
     */
    static void errorOccurred(final String caller, final Exception e) {
        Logging.recoverableException(LOGGER, RecentReferenceSystems.class, caller, e);
    }
}
