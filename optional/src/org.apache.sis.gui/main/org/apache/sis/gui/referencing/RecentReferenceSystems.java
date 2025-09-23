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
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.DerivedCRS;
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
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.gui.internal.BackgroundThreads;
import org.apache.sis.gui.internal.ExceptionReporter;
import org.apache.sis.gui.internal.GUIUtilities;
import org.apache.sis.gui.internal.NonNullObjectProperty;
import org.apache.sis.gui.internal.RecentChoices;
import org.apache.sis.gui.internal.io.OptionalDataDownloader;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;
import static org.apache.sis.gui.internal.LogHandler.LOGGER;


/**
 * A short list (~10 items) of most recently used {@link ReferenceSystem}s.
 * The list can be shown in a {@link ChoiceBox} or in a list of {@link MenuItem} controls.
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
 * @version 1.4
 * @since   1.1
 */
public class RecentReferenceSystems {
    /**
     * Number of reference systems to always show before all other reference systems.
     * They are the native of preferred reference system for the visualized data.
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
    private volatile CRSAuthorityFactory factory;

    /**
     * The area of interest, or {@code null} if none. This is used for filtering the reference systems added by
     * {@code addAlternatives(…)} and for providing some guidance to user when {@link CRSChooser} is shown.
     *
     * <h4>API note</h4>
     * We do not provide getter/setter for this property; use {@link ObjectProperty#set(Object)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
     */
    public final ObjectProperty<Envelope> areaOfInterest;

    /**
     * Area of interest converted to geographic coordinates, or {@code null} if none.
     */
    private ImmutableEnvelope geographicAOI;

    /**
     * The comparison criterion for considering two reference systems as a duplication.
     * The default value is {@link ComparisonMode#ALLOW_VARIANT}, i.e. axis orders are ignored.
     *
     * <h4>API note</h4>
     * We do not provide getter/setter for this property; use {@link ObjectProperty#set(Object)}
     * directly instead. We omit the "Property" suffix for making this operation more natural.
     */
    public final ObjectProperty<ComparisonMode> duplicationCriterion;

    /**
     * Values of controls created by this {@code RecentReferenceSystems} instance. We retain those properties
     * because modifying the {@link #referenceSystems} list sometimes causes controls to clear their selection
     * if we removed the selected item from the list. We use {@code controlValues} for saving currently selected
     * values before to modify the item list, and restore selections after we finished to modify the list.
     */
    private final List<WritableValue<ReferenceSystem>> controlValues;

    /**
     * The preferred locale for displaying object name, or {@code null} for the default locale.
     */
    final Locale locale;

    /**
     * Wrapper for a {@link ReferenceSystem} which has not yet been compared with authoritative definitions.
     * Those wrappers are created when {@link ReferenceSystem} instances have been specified to {@code setPreferred(…)}
     * or {@code addAlternatives(…)} methods with {@code replaceByAuthoritativeDefinition} argument set to {@code true}.
     *
     * @see #setPreferred(boolean, ReferenceSystem)
     * @see #addAlternatives(boolean, ReferenceSystem...)
     */
    private static final class Unverified {
        /** The reference system to verify. */
        private final ReferenceSystem system;

        /** Flags the given reference system as unverified. */
        Unverified(final ReferenceSystem system) {
            this.system = system;
        }

        /** Returns the verified (if possible) reference system. */
        ReferenceSystem find(final IdentifiedObjectFinder finder) throws FactoryException {
            if (finder != null) {
                final IdentifiedObject replacement = finder.findSingleton(system);
                if (replacement instanceof ReferenceSystem) {
                    return (ReferenceSystem) replacement;
                }
            }
            return system;
        }
    }

    /**
     * The reference systems either as {@link ReferenceSystem} instances, {@link Unverified} wrappers or
     * {@link String} codes. All {@code String} elements should be authority codes that {@link #factory}
     * can recognize. The first item in this list should be the native or preferred reference system.
     * The {@link #OTHER} reference system is <em>not</em> included in this list.
     *
     * <p>The list content is specified by calls to {@code setPreferred(…)} and {@code addAlternatives(…)} methods,
     * then is filtered by {@link #filterReferenceSystems(ImmutableEnvelope, ComparisonMode)} for resolving authority
     * codes and removing duplicated elements.</p>
     *
     * <p>All accesses to this field and to {@link #isModified} field shall be done in a block synchronized
     * on {@code systemsOrCodes}.</p>
     */
    private final List<Object> systemsOrCodes;

    /**
     * The {@link #systemsOrCodes} elements with all codes or wrappers replaced by {@link ReferenceSystem}
     * instances and duplicated values removed. This is the list given to JavaFX controls that we build.
     * This list includes {@link #OTHER} as its last item.
     *
     * <p>This list is initially null and created only when first needed. After the list has been created,
     * this reference is never modified. As long as the reference is null, we can skip the synchronization
     * of this list content with the {@link #systemsOrCodes} content when the latter changed. Because that
     * synchronization may involve accesses to the EPSG database, it is potentially costly.</p>
     *
     * @see #getReferenceSystems(boolean)
     */
    private ObservableList<ReferenceSystem> referenceSystems;

    /**
     * A view of {@link #referenceSystems} with only items that are instances of {@link CoordinateReferenceSystem}.
     * This list includes also {@link #OTHER} as its last item. This list is used for menus shown in contexts where
     * identifiers cannot be used, for example for selecting the CRS to use for displaying a map.
     *
     * <p>This list is lazily created when first needed,
     * because it depends on {@link #referenceSystems} which is itself lazily created.</p>
     *
     * @see #getReferenceSystems(boolean)
     */
    private ObservableList<ReferenceSystem> coordinateReferenceSystems;

    /**
     * A filtered view of {@link #referenceSystems} without the {@link #OTHER} item.
     * This is the list returned to users by public API, but otherwise it is not used by this class.
     *
     * <h4>Design notes</h4>
     * The {@link #OTHER} item needs to exist in the list used internally by this class because those lists
     * are used directly by controls like {@code ChoiceBox<ReferenceSystem>}, with the {@link #OTHER} value
     * handled in a special way by {@link ObjectStringConverter} for making the "Other…" item present in the
     * list of choices. But since {@link #OTHER} is not a real CRS, we want to hide that trick to users.
     *
     * <p>This list is lazily created when first needed,
     * because it depends on {@link #referenceSystems} which is itself lazily created.</p>
     *
     * @see #getItems()
     */
    private ObservableList<ReferenceSystem> publicItemList;

    /**
     * Coordinate reference systems used for computing cell indices of grid coverages.
     * Those reference systems are offered in a sub-menu and are not included in {@link #publicItemList}.
     * The content of this list depends on the grid coverages shown in the widget.
     *
     * @see #setGridReferencing(boolean, Map)
     */
    private final List<DerivedCRS> cellIndiceSystems;

    /**
     * {@code true} if the {@link #referenceSystems} list needs to be rebuilt from {@link #systemsOrCodes} content.
     * This field shall be read and modified in a block synchronized on {@link #systemsOrCodes}.
     *
     * @see #listModified()
     */
    private boolean isModified;

    /**
     * {@code true} if {@code RecentReferenceSystems} is in the process of modifying {@link #referenceSystems} list.
     * In such case we want to temporarily disable the {@link SelectionListener}.
     * This field is read and updated in JavaFX thread.
     */
    private boolean isAdjusting;

    /**
     * Creates a builder which will use a default authority factory.
     * The factory will be capable to handle at least some EPSG codes.
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
        systemsOrCodes       = new ArrayList<>();
        cellIndiceSystems    = new ArrayList<>();
        areaOfInterest       = new SimpleObjectProperty<>(this, "areaOfInterest");
        duplicationCriterion = new NonNullObjectProperty<>(this, "duplicationCriterion", ComparisonMode.ALLOW_VARIANT);
        controlValues        = new ArrayList<>();
        duplicationCriterion.addListener((e) -> listModified());
        areaOfInterest.addListener((e,o,n) -> {
            geographicAOI = Utils.toGeographic(RecentReferenceSystems.class, "areaOfInterest", n);
            listModified();
        });
    }

    /**
     * Sets the reference systems, area of interest and "referencing by grid indices" systems.
     * This method performs all the following work:
     *
     * <ul>
     *   <li>Invokes {@link #setPreferred(boolean, ReferenceSystem)} with the first CRS in iteration order.</li>
     *   <li>Invokes {@link #addAlternatives(boolean, ReferenceSystem...)} for all other CRS (single call).</li>
     *   <li>Sets the {@link #areaOfInterest} to the union of all envelopes.</li>
     *   <li>Sets the content of "Referencing by cell indices" sub-menu.</li>
     * </ul>
     *
     * @param  replaceByAuthoritativeDefinition  whether the reference systems should be replaced by authoritative definition.
     * @param  geometries  grid coverage names together with their grid geometry. May be empty.
     *
     * @since 1.3
     */
    public void setGridReferencing(final boolean replaceByAuthoritativeDefinition,
            final Map<String,GridGeometry> geometries)
    {
        /*
         * Fetch or compute information needed, but without modifying the state of this object yet.
         * All assignments to `this` should be done inside the `try … finally` block.
         */
        int countEnv = 0;
        int countCRS = 0;
        int countCIR = 0;
        final Envelope[] envelopes = new Envelope[geometries.size()];
        final DerivedCRS[] derived = new DerivedCRS[geometries.size()];
        final CoordinateReferenceSystem[] alt = new CoordinateReferenceSystem[Math.max(derived.length - 1, 0)];
        CoordinateReferenceSystem preferred = null;
        for (final Map.Entry<String,GridGeometry> entry : geometries.entrySet()) {
            final GridGeometry gg = entry.getValue();
            if (gg.isDefined(GridGeometry.ENVELOPE)) {
                envelopes[countEnv++] = gg.getEnvelope();
            }
            if (gg.isDefined(GridGeometry.CRS)) {
                final CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem();
                if (preferred == null) {
                    preferred = crs;
                } else {
                    alt[countCRS++] = crs;
                }
                if (gg.isDefined(GridGeometry.GRID_TO_CRS | GridGeometry.EXTENT)) {
                    derived[countCIR++] = gg.createImageCRS(entry.getKey(), PixelInCell.CELL_CENTER);
                }
            }
        }
        Envelope aoi = null;
        try {
            aoi = Envelopes.union(envelopes);       // No need to trim null elements.
        } catch (TransformException e) {
            errorOccurred("setGridReferencing", e);
        }
        /*
         * Modify now the state of `this` object but with `listModified()` made almost no-op.
         * The intent is to have only one effective call to `listModified()` at the end,
         * in order to have only one call to `filterReferenceSystems(…)`.
         */
        final ObservableList<ReferenceSystem> savedReferenceSystemList = referenceSystems;
        try {
            referenceSystems = null;
            if (preferred != null) {
                setPreferred(replaceByAuthoritativeDefinition, preferred);
                addAlternatives(replaceByAuthoritativeDefinition, alt);         // No need to trim null elements.
                cellIndiceSystems.clear();
                cellIndiceSystems.addAll(UnmodifiableArrayList.wrap(derived, 0, countCIR));
            }
            areaOfInterest.set(aoi);
        } finally {
            referenceSystems = savedReferenceSystemList;
        }
        listModified();
    }

    /**
     * Sets the native or preferred reference system. This is the system to always show as the first
     * choice and should typically be the native {@link CoordinateReferenceSystem} of visualized data.
     * If a previous preferred system existed, the previous system will be moved to alternative choices.
     *
     * <p>The {@code replaceByAuthoritativeDefinition} argument specifies whether the given reference system should
     * be replaced by authoritative definition. If {@code true} then for example a <q>WGS 84</q> geographic
     * CRS with (<var>longitude</var>, <var>latitude</var>) axis order may be replaced by "EPSG::4326" definition with
     * (<var>latitude</var>, <var>longitude</var>) axis order.</p>
     *
     * @param  replaceByAuthoritativeDefinition  whether the given system should be replaced by authoritative definition.
     * @param  system  the native or preferred reference system to show as the first choice.
     */
    public final void setPreferred(final boolean replaceByAuthoritativeDefinition, final ReferenceSystem system) {
        // Final because `setGridReferencing(…)` needs to be sure that `referenceSystems` is not rebuilt.
        ArgumentChecks.ensureNonNull("system", system);
        synchronized (systemsOrCodes) {
            systemsOrCodes.add(0, replaceByAuthoritativeDefinition ? new Unverified(system) : system);
            listModified();
        }
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
    public void setPreferred(final String code) {
        ArgumentChecks.ensureNonEmpty("code", code);
        synchronized (systemsOrCodes) {
            systemsOrCodes.add(0, code);
            listModified();
        }
    }

    /**
     * Invoked when a new CRS is selected and that CRS has not been found in the list.
     * The new CRS is added after the CRS and menu items will be added in background thread.
     */
    final void addSelected(final ReferenceSystem system) {
        if (isAccepted(system)) {
            synchronized (systemsOrCodes) {
                systemsOrCodes.add(Math.min(systemsOrCodes.size(), NUM_CORE_ITEMS), system);
                listModified();
            }
        }
    }

    /**
     * Adds the given reference systems to the list of alternative choices.
     * If there is duplicated values in the given list or with previously added systems,
     * then only the first occurrence of duplicated values is retained.
     * If an {@linkplain #areaOfInterest area of interest} (AOI) is specified,
     * then reference systems that do not intersect the AOI will be hidden.
     *
     * <p>The {@code replaceByAuthoritativeDefinition} argument specifies whether the given reference systems should
     * be replaced by authoritative definitions. If {@code true} then for example a <q>WGS 84</q> geographic
     * CRS with (<var>longitude</var>, <var>latitude</var>) axis order may be replaced by "EPSG::4326" definition with
     * (<var>latitude</var>, <var>longitude</var>) axis order.</p>
     *
     * @param  replaceByAuthoritativeDefinition  whether the given systems should be replaced by authoritative definitions.
     * @param  systems  the reference systems to add as alternative choices. Null elements are ignored.
     */
    public final void addAlternatives(final boolean replaceByAuthoritativeDefinition, final ReferenceSystem... systems) {
        // Final because `setGridReferencing(…)` needs to be sure that `referenceSystems` is not rebuilt.
        ArgumentChecks.ensureNonNull("systems", systems);
        synchronized (systemsOrCodes) {
            for (final ReferenceSystem system : systems) {
                if (system != null) {
                    systemsOrCodes.add(replaceByAuthoritativeDefinition ? new Unverified(system) : system);
                }
            }
            listModified();
        }
        // Check for duplication will be done in `filterReferenceSystems()` method.
    }

    /**
     * Adds the coordinate reference system identified by the given authority codes.
     * If there is duplicated values in the given list or with previously added systems,
     * then only the first occurrence of duplicated values is retained.
     * If an {@linkplain #areaOfInterest area of interest} (AOI) is specified,
     * then reference systems that do not intersect the AOI will be hidden.
     *
     * <p>If a code is not recognized, then the error will be notified at some later time by a call to
     * {@link #errorOccurred(FactoryException)} in a background thread and the code will be silently ignored.
     * This behavior allows the use of codes that depend on whether an optional dependency is present or not,
     * in particular the <a href="https://sis.apache.org/epsg.html">EPSG dataset</a>.</p>
     *
     * @param  codes  authority codes of the coordinate reference systems to add as alternative choices.
     *                Null or empty elements are ignored.
     */
    public void addAlternatives(final String... codes) {
        ArgumentChecks.ensureNonNull("codes", codes);
        synchronized (systemsOrCodes) {
            for (String code : codes) {
                code = Strings.trimOrNull(code);
                if (code != null) {
                    systemsOrCodes.add(code);
                }
            }
            listModified();
        }
        // Parsing will be done in `filterReferenceSystems()` method.
    }

    /**
     * Adds the coordinate reference systems saved in user preferences. The user preferences are determined
     * from the reference systems observed during current execution or previous executions of JavaFX application.
     * If an {@linkplain #areaOfInterest area of interest} (AOI) is specified,
     * then reference systems that do not intersect the AOI will be ignored.
     */
    public void addUserPreferences() {
        addAlternatives(RecentChoices.getReferenceSystems());
    }

    /**
     * Returns whether the given object is accepted for inclusion in the list of CRS choices.
     * In current implementation we accept a CRS if it has an authority code (typically an EPSG code).
     */
    private static boolean isAccepted(final IdentifiedObject object) {
        return IdentifiedObjects.getIdentifier(object, null) != null;
    }

    /**
     * Filters the {@link #systemsOrCodes} list by making sure that it contains only {@link ReferenceSystem} instances.
     * Authority codes are resolved if possible or removed if they cannot be resolved. Unverified CRSs are compared
     * with authoritative definitions and replaced when a match is found. Duplications are removed.
     * Finally reference systems with a domain of validity outside the {@link #geographicAOI} are omitted
     * from the returned list (but not removed from the original {@link #systemsOrCodes} list).
     *
     * <p>This method can be invoked from any thread. In practice, it is invoked from a background thread.</p>
     *
     * @param  domain  the {@link #areaOfInterest} value read from JavaFX thread, or {@code null} if none.
     * @param  mode    the {@link #duplicationCriterion} value read from JavaFX thread.
     * @return the filtered reference systems, or {@code null} if already filtered.
     */
    private List<ReferenceSystem> filterReferenceSystems(final ImmutableEnvelope domain, final ComparisonMode mode) {
        final List<ReferenceSystem> systems;
        final GazetteerFactory gf = new GazetteerFactory();     // Cheap to construct.
        synchronized (systemsOrCodes) {
            CRSAuthorityFactory factory = this.factory;         // Hide volatile field by local field.
            if (!isModified) {
                return null;                                    // Another thread already did the work.
            }
            boolean noFactoryFound = false;
            boolean searchedFinder = false;
            IdentifiedObjectFinder finder = null;
            for (int i=systemsOrCodes.size(); --i >= 0;) {
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
                            if (factory instanceof GeodeticAuthorityFactory) {
                                finder = ((GeodeticAuthorityFactory) factory).newIdentifiedObjectFinder();
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
            for (int i=0,j; i < (j=systemsOrCodes.size()); i++) {
                if (i >= RecentChoices.MAXIMUM_REFERENCE_SYSTEMS) {
                    systemsOrCodes.subList(i, j).clear();
                    break;
                }
                Object item = systemsOrCodes.get(i);
                while (--j > i) {
                    if (Utilities.deepEquals(item, systemsOrCodes.get(j), mode)) {
                        final Object removed = systemsOrCodes.remove(j);
                        if (isAccepted((IdentifiedObject) removed) && !isAccepted((IdentifiedObject) item)) {
                            /*
                             * Keep the instance which has an identifier. The instance without identifier
                             * is typically a CRS with non-standard axis order. It happens when it is the
                             * CRS associated to an image that has just been read.
                             */
                            systemsOrCodes.set(i, item = removed);
                        }
                    }
                }
            }
            /*
             * Finished to filter the `systemsOrCodes` list: all elements are now guaranteed to be
             * `ReferenceSystem` instances with no duplicated values. Copy those reference systems
             * in a separated list as a protection against changes in `systemsOrCodes` list that
             * could happen after this method returned, and also for retaining only the reference
             * systems that are valid in the area of interest. We do not remove "invalid" CRS
             * because they may become valid later if the area of interest changes.
             */
            final int n = systemsOrCodes.size();
            systems = new ArrayList<>(Math.min(NUM_SHOWN_ITEMS, n) + NUM_OTHER_ITEMS);
            for (int i=0; i<n; i++) {
                final ReferenceSystem system = (ReferenceSystem) systemsOrCodes.get(i);
                if (i >= NUM_CORE_ITEMS && !Utils.intersects(domain, system)) {
                    continue;
                }
                if (Utils.isIgnoreable(system)) {       // Ignore "Computer display" CRS.
                    continue;
                }
                systems.add(system);
                if (systems.size() >= NUM_SHOWN_ITEMS) break;
            }
            systems.add(OTHER);
            isModified   = false;
            this.factory = factory;         // Save in volatile field.
        }
        return systems;
    }

    /**
     * Invoked when {@link #systemsOrCodes} has been modified. If the modification happens after
     * some controls have been created ({@link ChoiceBox} or {@link MenuItem}s), then this method
     * updates their list of items. The update may happen at some time after this method returned.
     */
    private void listModified() {
        synchronized (systemsOrCodes) {
            isModified = true;
            if (referenceSystems != null) {
                // ChoiceBox or Menu already created. They will observe the changes in item list.
                getReferenceSystems(false);
            }
        }
    }

    /**
     * Updates {@link #referenceSystems} with the reference systems added to {@link #systemsOrCodes} list.
     * The new items may not be added immediately; instead the CRS will be processed in background thread
     * and copied to the {@link #referenceSystems} list when ready.
     *
     * @param  filtered  whether to filter the list for retaining only {@link CoordinateReferenceSystem} instances.
     * @return the list of items. May be empty on return and filled later.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private ObservableList<ReferenceSystem> getReferenceSystems(final boolean filtered) {
        if (referenceSystems == null) {
            referenceSystems = FXCollections.observableArrayList();
        }
        synchronized (systemsOrCodes) {
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
            if (isModified) {
                final int insertAt = Math.min(systemsOrCodes.size(), NUM_CORE_ITEMS);
                final List<ReferenceSystem> selected = getSelectedItems();
                systemsOrCodes.addAll(insertAt, selected);
                systemsOrCodes.addAll(insertAt + selected.size(), referenceSystems);
                final ImmutableEnvelope domain = geographicAOI;
                final ComparisonMode mode = duplicationCriterion.get();
                BackgroundThreads.execute(new Task<List<ReferenceSystem>>() {
                    /** Filters the {@link ReferenceSystem}s in a background thread. */
                    @Override protected List<ReferenceSystem> call() {
                        return filterReferenceSystems(domain, mode);
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
        }
        if (filtered) {
            if (coordinateReferenceSystems == null) {
                coordinateReferenceSystems = new FilteredList<>(referenceSystems, RecentReferenceSystems::isCRS);
            }
            return coordinateReferenceSystems;
        }
        return referenceSystems;
    }

    /**
     * Returns {@code true} if the given reference system can be included
     * in the {@link #coordinateReferenceSystems} list.
     */
    private static boolean isCRS(final ReferenceSystem system) {
        return (system == OTHER) || (system instanceof CoordinateReferenceSystem);
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
             * Save the values before to modify the list. Note that if `referenceSystems` was empty before
             * the copy, `controlValues` should be null before the copy but may become non-null after the
             * copy because listeners will have initialized them to the first `ReferenceSystem` available.
             * Those non-null values will not be reflected in the `values` array, so we should ignore them.
             */
            final ReferenceSystem[] values = controlValues.stream().map(WritableValue::getValue).toArray(ReferenceSystem[]::new);
            if (GUIUtilities.copyAsDiff(systems, referenceSystems)) {
                notifyChanges();
            }
            /*
             * Restore the previous selections. This code also serves another purpose: the previous selection
             * may not be an item in the list.  If the value was set by a call to `ChoiceBox.setValue(…)` and
             * is a `GeographicCRS` with (λ,φ) axis order, it may have been replaced in the list by a CRS with
             * (φ,λ) axis order. We need to replace the previous value by the instance in the list, otherwise
             * `ChoiceBox` will not show the CRS as selected.
             */
            final int n = referenceSystems.size();
            for (int j=0; j<values.length; j++) {
                ReferenceSystem system = values[j];
                if (system != null) {                   // See comment about empty `referenceSystems` list.
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
     * Invoked when user selects a reference system. If the choice is "Other…", then {@link CRSChooser} popups
     * and the selected reference system is added to the list of choices. If the selected CRS is different than
     * the previous one, then {@link RecentChoices} is notified and the user-specified listener is notified.
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
                final CRSChooser chooser = new CRSChooser(factory, geographicAOI, locale);
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
                            for (int i=count; --i >= NUM_CORE_ITEMS;) {
                                if (!selected.contains(items.get(i))) {         // Do not remove selected items.
                                    items.remove(i);                            // Remove an item before `OTHER`.
                                    if (--count < NUM_SHOWN_ITEMS) break;
                                }
                            }
                        }
                        items.add(Math.min(items.size(), NUM_CORE_ITEMS), newValue);
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
                items.add(Math.min(items.size(), NUM_CORE_ITEMS), newValue);
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
     * instead of relying on {@code ListChangeListener} in order to process only one event after we have done
     * a bunch of changes instead of an event after each individual add or remove operation.
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
        if (publicItemList == null) {
            publicItemList = new FilteredList<>(getReferenceSystems(false), Objects::nonNull);
        }
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
        final ReferenceSystem[] selected = new ReferenceSystem[controlValues.size()];
        for (final WritableValue<ReferenceSystem> value : controlValues) {
            final ReferenceSystem system = value.getValue();
            if (system != null) selected[count++] = system;
        }
        /*
         * Now filter the `referenceSystems` list, retaining only elements that are present in `selected`.
         * We do that way for having selected elements in the same order as they appear in JavaFX controls.
         */
        final List<ReferenceSystem> ordered = new ArrayList<>(count);
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
                for (int j=ordered.size(); --j >= 0;) {
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
     * The returned control may be initially empty, in which case its content will be automatically set at
     * a later time (after a background thread finished to process the {@link CoordinateReferenceSystem}s).
     *
     * <p>If the {@code filtered} argument is {@code true}, then the choice box will contain only reference systems
     * that can be used for rendering purposes. That filtered list can contain {@link CoordinateReferenceSystem}
     * instances but not reference systems by identifiers such as {@linkplain MilitaryGridReferenceSystem MGRS}.
     * The latter are usable only for the purposes of formatting coordinate values as texts.</p>
     *
     * <h4>Limitations</h4>
     * There is currently no mechanism for disposing the returned control. For garbage collecting the
     * returned {@code ChoiceBox}, this {@code RecentReferenceSystems} must be garbage-collected as well.
     *
     * @param  filtered  whether the choice box should contain only {@link CoordinateReferenceSystem} instances.
     * @param  action    the action to execute when a reference system is selected.
     * @return a choice box with reference systems specified by {@code setPreferred(…)}
     *         and {@code addAlternatives(…)} methods.
     *
     * @since 1.3
     */
    public ChoiceBox<ReferenceSystem> createChoiceBox(final boolean filtered, final ChangeListener<ReferenceSystem> action) {
        ArgumentChecks.ensureNonNull("action", action);
        final ChoiceBox<ReferenceSystem> choices = new ChoiceBox<>(getReferenceSystems(filtered));
        choices.setConverter(new ObjectStringConverter<>(choices.getItems(), locale));
        choices.valueProperty().addListener(new SelectionListener(action));
        controlValues.add(choices.valueProperty());
        return choices;
    }

    /**
     * Creates menu items offering choices among the reference systems specified to this {@code RecentReferenceSystems}.
     * The items will be inserted in the {@linkplain Menu#getItems() menu list}. The content of that list will
     * change at any time after this method returned: items will be added or removed as a result of user actions.
     *
     * <p>If the {@code filtered} argument is {@code true}, then the menu items will contain only reference systems
     * that can be used for rendering purposes. That filtered list can contain {@link CoordinateReferenceSystem}
     * instances but not reference systems by identifiers such as {@linkplain MilitaryGridReferenceSystem MGRS}.
     * The latter are usable only for the purposes of formatting coordinate values as texts.</p>
     *
     * <h4>Limitations</h4>
     * There is currently no mechanism for disposing the returned control. For garbage collecting the
     * returned {@code Menu}, this {@code RecentReferenceSystems} must be garbage-collected as well.
     *
     * @param  filtered  whether the menu should contain only {@link CoordinateReferenceSystem} instances.
     * @param  action    the action to execute when a reference system is selected.
     * @return the menu containing items for reference systems.
     *
     * @since 1.3
     */
    public Menu createMenuItems(final boolean filtered, final ChangeListener<ReferenceSystem> action) {
        ArgumentChecks.ensureNonNull("action", action);
        final List<ReferenceSystem> main = getReferenceSystems(filtered);
        final List<DerivedCRS> derived = (filtered) ? null : cellIndiceSystems;
        final Menu menu = new Menu(Vocabulary.forLocale(locale).getString(Vocabulary.Keys.ReferenceSystem));
        final MenuSync property = new MenuSync(main, !filtered, derived, menu, new SelectionListener(action));
        menu.getProperties().put(SELECTED_ITEM_KEY, property);
        controlValues.add(property);
        return menu;
    }

    /**
     * Returns the property for the selected value in a menu created by {@link #createMenuItems(boolean, ChangeListener)}.
     *
     * @param  menu  the menu, or {@code null} if none.
     * @return the property for the selected value, or {@code null} if none.
     */
    public static ObjectProperty<ReferenceSystem> getSelectedProperty(final Menu menu) {
        if (menu != null) {
            final Object property = menu.getProperties().get(SELECTED_ITEM_KEY);
            if (property instanceof MenuSync) {
                return (MenuSync) property;
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
