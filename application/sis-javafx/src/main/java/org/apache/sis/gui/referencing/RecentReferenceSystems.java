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

import java.util.List;
import java.util.ArrayList;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuItem;
import javafx.concurrent.Task;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.IdentifiedObjectFinder;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.gui.BackgroundThreads;
import org.apache.sis.internal.gui.ExceptionReporter;
import org.apache.sis.internal.gui.GUIUtilities;
import org.apache.sis.internal.gui.NonNullObjectProperty;
import org.apache.sis.internal.gui.RecentChoices;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.Strings;


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
 * @version 1.1
 * @since   1.1
 * @module
 */
public class RecentReferenceSystems {
    /**
     * The authority of the {@link #factory} (for example "EPSG"),
     * or {@code null} for all authorities known to SIS.
     */
    private static final String AUTHORITY = null;

    /**
     * Number of reference systems to always show before all other reference systems.
     * They are the native of preferred reference system for the visualized data.
     */
    private static final int NUM_CORE_SYSTEMS = 1;

    /**
     * Number of reference systems to shown in {@link ChoiceBox} or {@link MenuItem}s.
     * The {@value #NUM_CORE_SYSTEMS} core systems are included but not {@link #OTHER}.
     */
    private static final int NUM_SHOWN_SYSTEMS = 9;

    /**
     * Number of reference systems to keep at the end of the list.
     */
    private static final int NUM_OTHER_SYSTEMS = 1;

    /**
     * A pseudo-reference system for the "Other…" choice. We use a null value because {@link ChoiceBox}
     * seems to insist for inserting a null value in the items list when we remove the selected item.
     */
    static final ReferenceSystem OTHER = null;

    /**
     * The factory to use for creating a Coordinate Reference System from an authority code.
     * If {@code null}, then the {@linkplain CRS#getAuthorityFactory(String) default factory}
     * will be fetched when first needed.
     */
    private volatile CRSAuthorityFactory factory;

    /**
     * The area of interest, or {@code null} if none. This is used for filtering the reference systems added by
     * {@code addAlternatives(…)} and for providing some guidance to user when {@link CRSChooser} is shown.
     */
    public final ObjectProperty<Envelope> areaOfInterest;

    /**
     * The comparison criterion for considering two reference systems as a duplication.
     * The default value is {@link ComparisonMode#ALLOW_VARIANT}, i.e. axis orders are ignored.
     */
    public final ObjectProperty<ComparisonMode> duplicationCriterion;

    /**
     * Values of controls created by this {@code RecentReferenceSystems} instance. We retain those properties
     * because modifying the {@link #referenceSystems} list sometime causes controls to clear their selection
     * if we removed the selected item from the list. We use {@code controlValues} for saving currently selected
     * values before to modify the item list, and restore selections after we finished to modify the list.
     */
    private final List<ObjectProperty<ReferenceSystem>> controlValues;

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
        final ReferenceSystem system;

        /** Flags the given reference system as unverified. */
        Unverified(final ReferenceSystem system) {
            this.system = system;
        }
    }

    /**
     * The reference systems either as {@link ReferenceSystem} instances, {@link Unverified} wrappers or
     * {@link String} codes. All {@code String} elements should be authority codes that {@link #factory}
     * can recognize. The first item in this list should be the native or preferred reference system.
     * The {@link #OTHER} reference system is <em>not</em> included in this list.
     *
     * <p>The list content is specified by calls to {@code setPreferred(…)} and {@code addAlternatives(…)} methods,
     * then is filtered by {@link #filterSystems(ImmutableEnvelope, ComparisonMode)} for resolving authority codes
     * and removing duplicated elements.</p>
     *
     * <p>All accesses to this field and to {@link #isModified} field shall be done in a block synchronized
     * on {@code systemsOrCodes}.</p>
     */
    private final List<Object> systemsOrCodes;

    /**
     * The {@link #systemsOrCodes} elements with all codes or wrappers replaced by {@link ReferenceSystem}
     * instances and duplicated values removed. This is the list given to JavaFX controls that we build.
     * This list includes {@link #OTHER} as its last item.
     */
    private ObservableList<ReferenceSystem> referenceSystems;

    /**
     * {@code true} if the {@link #referenceSystems} list needs to be rebuilt from {@link #systemsOrCodes} content.
     * This field shall be read and modified in a block synchronized on {@link #systemsOrCodes}.
     *
     * @see #modified()
     */
    private boolean isModified;

    /**
     * {@code true} if {@code RecentReferenceSystems} is in the process of modifying {@link #referenceSystems} list.
     * In such we want to temporarily disable the {@link Listener}. This field is read and updated in JavaFX thread.
     */
    private boolean isAdjusting;

    /**
     * Creates a builder which will use the {@linkplain CRS#getAuthorityFactory(String) default authority factory}.
     */
    public RecentReferenceSystems() {
        systemsOrCodes       = new ArrayList<>();
        areaOfInterest       = new SimpleObjectProperty<>(this, "areaOfInterest");
        duplicationCriterion = new NonNullObjectProperty<>(this, "duplicationCriterion", ComparisonMode.ALLOW_VARIANT);
        controlValues        = new ArrayList<>();
        final InvalidationListener pl = (e) -> modified();
        areaOfInterest.addListener(pl);
        duplicationCriterion.addListener(pl);
    }

    /**
     * Creates a builder which will use the specified authority factory.
     *
     * @param  factory  the factory to use for building CRS from authority codes.
     *
     * @see CRS#getAuthorityFactory(String)
     */
    public RecentReferenceSystems(final CRSAuthorityFactory factory) {
        this();
        ArgumentChecks.ensureNonNull("factory", factory);
        this.factory = factory;
    }

    /**
     * Sets the native or preferred reference system. This is the system to always show as the first
     * choice and should typically be the native {@link CoordinateReferenceSystem} of visualized data.
     * If a previous preferred system existed, the previous system will be moved to alternative choices.
     *
     * <p>The {@code replaceByAuthoritativeDefinition} argument specifies whether the given reference system should
     * be replaced by authoritative definition. If {@code true} then for example a <cite>"WGS 84"</cite> geographic
     * CRS with (<var>longitude</var>, <var>latitude</var>) axis order may be replaced by "EPSG::4326" definition with
     * (<var>latitude</var>, <var>longitude</var>) axis order.</p>
     *
     * @param  replaceByAuthoritativeDefinition  whether the given system should be replaced by authoritative definition.
     * @param  system  the native or preferred reference system to show as the first choice.
     */
    public void setPreferred(final boolean replaceByAuthoritativeDefinition, final ReferenceSystem system) {
        ArgumentChecks.ensureNonNull("system", system);
        synchronized (systemsOrCodes) {
            systemsOrCodes.add(0, replaceByAuthoritativeDefinition ? new Unverified(system) : system);
            modified();
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
            modified();
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
     * be replaced by authoritative definitions. If {@code true} then for example a <cite>"WGS 84"</cite> geographic
     * CRS with (<var>longitude</var>, <var>latitude</var>) axis order may be replaced by "EPSG::4326" definition with
     * (<var>latitude</var>, <var>longitude</var>) axis order.</p>
     *
     * @param  replaceByAuthoritativeDefinition  whether the given systems should be replaced by authoritative definitions.
     * @param  systems  the reference systems to add as alternative choices. Null elements are ignored.
     */
    public void addAlternatives(final boolean replaceByAuthoritativeDefinition, final ReferenceSystem... systems) {
        ArgumentChecks.ensureNonNull("systems", systems);
        synchronized (systemsOrCodes) {
            for (final ReferenceSystem system : systems) {
                if (system != null) {
                    systemsOrCodes.add(replaceByAuthoritativeDefinition ? new Unverified(system) : system);
                }
            }
            modified();
        }
        // Check for duplication will be done in `filterSystems()` method.
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
            modified();
        }
        // Parsing will be done in `filterSystems()` method.
    }

    /**
     * Adds the coordinate reference systems saved in user preferences. The user preferences are determined
     * from the reference systems observed during current execution or previous execution of JavaFX application.
     * If an {@linkplain #areaOfInterest area of interest} (AOI) is specified,
     * then reference systems that do not intersect the AOI will be ignored.
     */
    public void addUserPreferences() {
        addAlternatives(RecentChoices.getReferenceSystems());
    }

    /**
     * Filters the {@link #systemsOrCodes} list by making sure that it contains only {@link ReferenceSystem} instances.
     * Authority codes are resolved if possible or removed if they can not be resolved. Unverified CRSs are compared
     * with authoritative definitions and replaced when a match is found. Duplications are removed.
     *
     * <p>This method can be invoked from any thread.</p>
     *
     * @param  domain  the {@link #areaOfInterest} value read from JavaFX thread, or {@code null} if none.
     * @param  mode    the {@link #duplicationCriterion} value read from JavaFX thread.
     * @return the filtered reference systems, or {@code null} if already filtered.
     */
    private List<ReferenceSystem> filterSystems(final ImmutableEnvelope domain, final ComparisonMode mode) {
        final List<ReferenceSystem> systems;
        synchronized (systemsOrCodes) {
            CRSAuthorityFactory factory = this.factory;         // Hide volatile field by local field.
            if (!isModified) {
                return null;                                    // Another thread already did the work.
            }
            boolean noFactoryFound = false;
            boolean searchedFinder = false;
            IdentifiedObjectFinder finder = null;
            for (int i=systemsOrCodes.size(); --i >= 0;) try {
                final Object item = systemsOrCodes.get(i);
                if (item == OTHER) {
                    systemsOrCodes.remove(i);
                } else if (item instanceof String) {
                    /*
                     * The current list element is an authority code such as "EPSG::4326".
                     * Replace that code by the full `CoordinateReferenceSystem` instance.
                     * Note that authority factories are optional, so it is okay if we can
                     * not resolve the code. In such case the item will be removed.
                     */
                    if (!noFactoryFound) {
                        if (factory == null) {
                            factory = CRS.getAuthorityFactory(AUTHORITY);
                        }
                        systemsOrCodes.set(i, factory.createCoordinateReferenceSystem((String) item));
                    } else {
                        systemsOrCodes.remove(i);
                    }
                } else if (item instanceof Unverified) {
                    /*
                     * The current list element is a `ReferenceSystem` instance but maybe not
                     * conform to authoritative definition, for example regarding axis order.
                     * If we can find an authoritative definition, do the replacement.
                     * If this operation can not be done, accept the reference system as-is.
                     */
                    if (!searchedFinder) {
                        searchedFinder = true;                              // Set now in case an exception is thrown.
                        if (factory instanceof GeodeticAuthorityFactory) {
                            finder = ((GeodeticAuthorityFactory) factory).newIdentifiedObjectFinder();
                        } else {
                            finder = IdentifiedObjects.newFinder(AUTHORITY);
                        }
                        finder.setIgnoringAxes(true);
                    }
                    ReferenceSystem system = ((Unverified) item).system;
                    if (finder != null) {
                        final IdentifiedObject replacement = finder.findSingleton(system);
                        if (replacement instanceof ReferenceSystem) {
                            system = (ReferenceSystem) replacement;
                        }
                    }
                    systemsOrCodes.set(i, system);
                }
            } catch (FactoryException e) {
                errorOccurred(e);
                systemsOrCodes.remove(i);
                noFactoryFound = (factory == null);
            }
            /*
             * Search for duplicated values after we finished filtering. This block is inefficient
             * (execution time of O(N²)) but it should not be an issue if this list is short (e.g.
             * 20 elements). We cut the list if we reach the maximal amount of systems to keep.
             */
            for (int i=0,j; i < (j=systemsOrCodes.size()); i++) {
                if (i >= RecentChoices.MAXIMUM_REFERENCE_SYSTEMS) {
                    systemsOrCodes.subList(i, j).clear();
                    break;
                }
                final Object item = systemsOrCodes.get(i);
                while (--j > i) {
                    if (Utilities.deepEquals(item, systemsOrCodes.get(j), mode)) {
                        systemsOrCodes.remove(j);
                    }
                }
            }
            /*
             * Finished to filter the `systemsOrCodes` list: all elements are now guaranteed to be
             * `ReferenceSystem` instances with no duplicated values. Copy those reference systems
             * in a separated list as a protection against changes in `systemsOrCodes` list that
             * could happen after this method returned, and also for retaining only the reference
             * systems that are valid in the area of interest. We do not remove "invalid" CRS
             * because they would become valid later if the area of interest changes.
             */
            final int n = systemsOrCodes.size();
            systems = new ArrayList<>(Math.min(NUM_SHOWN_SYSTEMS, n) + NUM_OTHER_SYSTEMS);
            for (int i=0; i<n; i++) {
                final ReferenceSystem system = (ReferenceSystem) systemsOrCodes.get(i);
                if (i >= NUM_CORE_SYSTEMS && domain != null) {
                    final GeographicBoundingBox bbox = Extents.getGeographicBoundingBox(system.getDomainOfValidity());
                    if (bbox != null && !domain.intersects(new ImmutableEnvelope(bbox))) {
                        continue;
                    }
                }
                systems.add(system);
                if (systems.size() >= NUM_SHOWN_SYSTEMS) break;
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
    private void modified() {
        synchronized (systemsOrCodes) {
            isModified = true;
            if (referenceSystems != null) {
                updateItems();
            }
        }
    }

    /**
     * Updates {@link #referenceSystems} with the reference systems added to {@link #systemsOrCodes} list.
     * The new items may not be added immediately; instead the CRS will be processed in background thread
     * and copied to the {@link #referenceSystems} list when ready.
     *
     * @return the list of items. May be empty on return and filled later.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private ObservableList<ReferenceSystem> updateItems() {
        if (referenceSystems == null) {
            referenceSystems = FXCollections.observableArrayList();
        }
        synchronized (systemsOrCodes) {
            systemsOrCodes.addAll(Math.min(systemsOrCodes.size(), NUM_CORE_SYSTEMS), referenceSystems);
            // Duplicated values will be filtered by the background task below.
            isModified = true;
            final ImmutableEnvelope domain = ImmutableEnvelope.castOrCopy(areaOfInterest.get());
            final ComparisonMode mode = duplicationCriterion.get();
            BackgroundThreads.execute(new Task<List<ReferenceSystem>>() {
                /** Filters the {@link ReferenceSystem}s in a background thread. */
                @Override protected List<ReferenceSystem> call() {
                    return filterSystems(domain, mode);
                }

                /** Should never happen. */
                @Override protected void failed() {
                    ExceptionReporter.show(this);
                }

                /** Sets the {@link ChoiceBox} content to the list computed in background thread. */
                @Override protected void succeeded() {
                    setReferenceSystems(getValue(), mode);
                }
            });
        }
        return referenceSystems;
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
        if (systems != null) {
            /*
             * The call to `copyAsDiff(…)` may cause `ChoiceBox` values to be lost if the corresponding item
             * in the `referenceSystems` list is temporarily removed (before to be inserted elsewhere).
             * Save the values before to modify the list.
             */
            final ReferenceSystem[] values = controlValues.stream().map(ObjectProperty::get).toArray(ReferenceSystem[]::new);
            try {
                isAdjusting = true;
                GUIUtilities.copyAsDiff(systems, referenceSystems);
            } finally {
                isAdjusting = false;
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
                for (int i=0; i<n; i++) {
                    final ReferenceSystem candidate = referenceSystems.get(i);
                    if (Utilities.deepEquals(candidate, system, mode)) {
                        system = candidate;
                        break;
                    }
                }
                controlValues.get(j).set(system);
            }
        }
    }

    /**
     * Invoked when user selects a reference system. If the choice is "Other…", then {@link CRSChooser} popups
     * and the selected reference system is added to the list of choices. If the selected CRS is different than
     * the previous one, then {@link RecentChoices} is notified and the user-specified listener is notified.
     */
    private final class Listener implements ChangeListener<ReferenceSystem> {
        /** The user-specified action to execute when a reference system is selected. */
        private final ChangeListener<ReferenceSystem> action;

        /** Creates a new listener of reference system selection. */
        Listener(final ChangeListener<ReferenceSystem> action) {
            this.action = action;
        }

        /** Invoked when the user selects a reference system or the "Other…" item. */
        @SuppressWarnings("unchecked")
        @Override public void changed(final ObservableValue<? extends ReferenceSystem> property,
                                      final ReferenceSystem oldValue, ReferenceSystem newValue)
        {
            if (isAdjusting) {
                return;
            }
            if (newValue == OTHER) {
                final CRSChooser chooser = new CRSChooser(factory, areaOfInterest.get());
                newValue = chooser.showDialog(GUIUtilities.getWindow(property)).orElse(null);
                if (newValue == null) {
                    newValue = oldValue;
                } else {
                    final ObservableList<ReferenceSystem> items = referenceSystems;
                    final ComparisonMode mode = duplicationCriterion.get();
                    final int count = items.size() - NUM_OTHER_SYSTEMS;
                    boolean found = false;
                    for (int i=0; i<count; i++) {
                        if (Utilities.deepEquals(newValue, items.get(i), mode)) {
                            if (i >= NUM_CORE_SYSTEMS) {
                                items.set(i, newValue);
                            }
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        if (count >= NUM_SHOWN_SYSTEMS) {
                            items.remove(count - 1);        // Remove the last item before `OTHER`.
                        }
                        items.add(Math.min(count, NUM_CORE_SYSTEMS), newValue);
                    }
                }
                /*
                 * Following cast is safe because this listener is registered only on ObjectProperty
                 * instances, and the ObjectProperty class implements WritableValue.
                 */
                ((WritableValue<ReferenceSystem>) property).setValue(newValue);
            }
            if (oldValue != newValue) {
                /*
                 * Notify the user-specified listener first. It will typically starts a background process.
                 * If an exception occurs in that user code, the list of CRS choices will be left unchanged.
                 */
                action.changed(property, oldValue, newValue);
                RecentChoices.useReferenceSystem(IdentifiedObjects.toString(IdentifiedObjects.getIdentifier(newValue, null)));
                /*
                 * Move the selected reference system as the first choice after the core systems.
                 * We need to remove the old value before to add the new one, otherwise it seems
                 * to confuse the list.
                 */
                final ObservableList<ReferenceSystem> items = referenceSystems;
                final int count = items.size() - NUM_OTHER_SYSTEMS;
                for (int i=Math.min(count, NUM_CORE_SYSTEMS + 1); --i >= 0;) {
                    if (items.get(i) == newValue) {
                        return;
                    }
                }
                for (int i=count; --i >= NUM_CORE_SYSTEMS;) {
                    if (items.get(i) == newValue) {
                        items.remove(i);
                        break;
                    }
                }
                items.add(Math.max(0, Math.min(count, NUM_CORE_SYSTEMS)), newValue);
            }
        }
    }

    /**
     * Creates a box offering choices among the reference systems specified to this {@code ShortChoiceList}.
     * The returned control may be initially empty, in which case its content will be automatically set at
     * a later time (after a background thread finished to process the {@link CoordinateReferenceSystem}s).
     *
     * @param  action  the action to execute when a reference system is selected.
     * @return a choice box with reference systems specified by {@code setPreferred(…)}
     *         and {@code addAlternatives(…)} methods.
     */
    public ChoiceBox<ReferenceSystem> createChoiceBox(final ChangeListener<ReferenceSystem> action) {
        ArgumentChecks.ensureNonNull("action", action);
        final ChoiceBox<ReferenceSystem> choices = new ChoiceBox<>(updateItems());
        choices.setConverter(new ObjectStringConverter<>(choices.getItems(), null));
        choices.valueProperty().addListener(new Listener(action));
        controlValues.add(choices.valueProperty());
        return choices;
    }

    public MenuItem[] createMenuItems() {
        return null;
    }

    /**
     * Invoked when an error occurred while filtering a {@link ReferenceSystem} instance.
     * The error may be a failure to convert an EPSG code to a {@link CoordinateReferenceSystem} instance,
     * or an error during a CRS verification. Some errors may be normal, for example because EPSG dataset
     * is not expected to be present in every runtime environments. The consequence of this error is "only"
     * that the CRS will not be listed among the reference systems that the user can choose.
     *
     * <p>The default implementation log the error at {@link java.util.logging.Level#FINE}.
     * No other processing is done; user is not notified unless (s)he paid attention to loggings.</p>
     *
     * @param  e  the error that occurred.
     */
    protected void errorOccurred(final FactoryException e) {
        Logging.recoverableException(Logging.getLogger(Modules.APPLICATION), RecentReferenceSystems.class, "updateItems", e);
    }
}
