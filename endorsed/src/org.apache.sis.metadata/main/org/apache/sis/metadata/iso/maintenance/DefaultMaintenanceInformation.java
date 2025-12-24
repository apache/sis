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
package org.apache.sis.metadata.iso.maintenance;

import java.util.Date;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.maintenance.MaintenanceFrequency;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.maintenance.ScopeDescription;
import org.opengis.util.InternationalString;
import org.apache.sis.temporal.TemporalDate;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.metadata.iso.legacy.LegacyPropertyAdapter;
import org.apache.sis.metadata.internal.Dependencies;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.util.collection.Containers;
import static org.apache.sis.metadata.internal.shared.ImplementationHelper.valueIfDefined;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.maintenance.Scope;

// Specific to the geoapi-4.0 branch:
import java.time.temporal.TemporalAmount;
import org.opengis.metadata.citation.Responsibility;


/**
 * Information about the scope and frequency of updating.
 * The following property is mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_MaintenanceInformation}
 * {@code   └─maintenanceAndUpdateFrequency……} Frequency with which changes and additions are made to the resource.</div>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.5
 * @since   0.3
 */
@XmlType(name = "MD_MaintenanceInformation_Type", propOrder = {
    "maintenanceAndUpdateFrequency",
    "maintenanceDate",                          // New in ISO 19115:2014
    "dateOfNextUpdate",                         // Legacy ISO 19115:2003
    "userDefinedMaintenanceFrequency",
    "maintenanceScope",                         // New in ISO 19115:2014 - contains information from the two below
    "updateScopes",                             // Legacy ISO 19115:2003
    "updateScopeDescriptions",                  // Legacy ISO 19115:2003
    "maintenanceNotes",
    "contacts"
})
@XmlRootElement(name = "MD_MaintenanceInformation")
public class DefaultMaintenanceInformation extends ISOMetadata implements MaintenanceInformation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -7934472150551882812L;

    /**
     * Frequency with which changes and additions are made to the resource after the
     * initial resource is completed.
     */
    private MaintenanceFrequency maintenanceAndUpdateFrequency;

    /**
     * Date information associated with maintenance of resource.
     */
    @SuppressWarnings("serial")
    private Collection<CitationDate> maintenanceDates;

    /**
     * Maintenance period other than those defined, in milliseconds.
     */
    @SuppressWarnings("serial")
    private TemporalAmount userDefinedMaintenanceFrequency;

    /**
     * Type of resource and / or extent to which the maintenance information applies.
     */
    @SuppressWarnings("serial")
    private Collection<Scope> maintenanceScopes;

    /**
     * Information regarding specific requirements for maintaining the resource.
     */
    @SuppressWarnings("serial")
    private Collection<InternationalString> maintenanceNotes;

    /**
     * Identification of, and means of communicating with, person(s) and organization(s)
     * with responsibility for maintaining the resource.
     */
    @SuppressWarnings("serial")
    private Collection<Responsibility> contacts;

    /**
     * Creates a an initially empty maintenance information.
     */
    public DefaultMaintenanceInformation() {
    }

    /**
     * Creates a maintenance information.
     *
     * @param maintenanceAndUpdateFrequency  the frequency with which changes and additions are
     *        made to the resource after the initial resource is completed, or {@code null} if none.
     */
    public DefaultMaintenanceInformation(final MaintenanceFrequency maintenanceAndUpdateFrequency) {
        this.maintenanceAndUpdateFrequency = maintenanceAndUpdateFrequency;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(MaintenanceInformation)
     */
    public DefaultMaintenanceInformation(final MaintenanceInformation object) {
        super(object);
        if (object != null) {
            maintenanceAndUpdateFrequency   = object.getMaintenanceAndUpdateFrequency();
            maintenanceDates                = copyCollection(object.getMaintenanceDates(), CitationDate.class);
            userDefinedMaintenanceFrequency = object.getUserDefinedMaintenanceFrequency();
            maintenanceScopes               = copyCollection(object.getMaintenanceScopes(), Scope.class);
            maintenanceNotes                = copyCollection(object.getMaintenanceNotes(), InternationalString.class);
            contacts                        = copyCollection(object.getContacts(), Responsibility.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultMaintenanceInformation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultMaintenanceInformation} instance is created using the
     *       {@linkplain #DefaultMaintenanceInformation(MaintenanceInformation) copy constructor}
     *       and returned. Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultMaintenanceInformation castOrCopy(final MaintenanceInformation object) {
        if (object == null || object instanceof DefaultMaintenanceInformation) {
            return (DefaultMaintenanceInformation) object;
        }
        return new DefaultMaintenanceInformation(object);
    }

    /**
     * Returns the frequency with which changes and additions are made to the resource
     * after the initial resource is completed.
     *
     * @return frequency with which changes and additions are made to the resource, or {@code null}.
     */
    @Override
    @XmlElement(name = "maintenanceAndUpdateFrequency")
    public MaintenanceFrequency getMaintenanceAndUpdateFrequency() {
        return maintenanceAndUpdateFrequency;
    }

    /**
     * Sets the frequency with which changes and additions are made to the resource
     * after the initial resource is completed.
     *
     * @param  newValue  the new maintenance frequency.
     */
    public void setMaintenanceAndUpdateFrequency(final MaintenanceFrequency newValue) {
        checkWritePermission(maintenanceAndUpdateFrequency);
        maintenanceAndUpdateFrequency = newValue;
    }

    /**
     * Return the date information associated with maintenance of resource.
     *
     * @return date information associated with maintenance of resource.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<CitationDate> getMaintenanceDates() {
        return maintenanceDates = nonNullCollection(maintenanceDates, CitationDate.class);
    }

    /**
     * Sets the date information associated with maintenance of resource.
     *
     * @param  newValues  the new date information associated with maintenance of resource.
     *
     * @since 0.5
     */
    public void setMaintenanceDates(final Collection<? extends CitationDate> newValues) {
        maintenanceDates = writeCollection(newValues, maintenanceDates, CitationDate.class);
    }

    /**
     * Returns the scheduled revision date for resource.
     * This method fetches the value from the {@linkplain #getMaintenanceDates() maintenance dates}.
     *
     * @return scheduled revision date, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMaintenanceDates()} in order to enable inclusion
     *             of a {@link DateType} to describe the type of the date. Note that {@link DateType#NEXT_UPDATE}
     *             was added to that code list.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getMaintenanceDates")
    @XmlElement(name = "dateOfNextUpdate", namespace = LegacyNamespaces.GMD)
    public Date getDateOfNextUpdate() {
        if (FilterByVersion.LEGACY_METADATA.accept()) {
            final Collection<CitationDate> dates = getMaintenanceDates();
            if (dates != null) {                                                    // May be null on XML marshalling.
                for (final CitationDate date : dates) {
                    if (date.getDateType() == DateType.NEXT_UPDATE) {
                        return date.getDate();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Sets the scheduled revision date for resource.
     * This method stores the value in the {@linkplain #getMaintenanceDates() maintenance dates}.
     *
     * @param  newValue  the new date of next update.
     */
    @Deprecated(since="1.0")
    public void setDateOfNextUpdate(final Date newValue) {
        checkWritePermission(valueIfDefined(maintenanceDates));
        Collection<CitationDate> dates = maintenanceDates;
        if (dates != null) {
            final Iterator<CitationDate> it = dates.iterator();
            while (it.hasNext()) {
                final CitationDate date = it.next();
                if (date.getDateType() == DateType.NEXT_UPDATE) {
                    if (newValue == null) {
                        it.remove();
                        return;
                    } else if (date instanceof DefaultCitationDate) {
                        ((DefaultCitationDate) date).setDate(newValue);
                        return;
                    }
                }
            }
        }
        if (newValue != null) {
            final var date = new DefaultCitationDate(TemporalDate.toTemporal(newValue), DateType.NEXT_UPDATE);
            if (dates != null) {
                dates.add(date);
            } else {
                dates = Collections.singleton(date);
            }
            setMaintenanceDates(dates);
        }
    }

    /**
     * Returns the maintenance period other than those defined.
     *
     * @return the maintenance period, or {@code null}.
     */
    @Override
    @XmlElement(name = "userDefinedMaintenanceFrequency")
    public TemporalAmount getUserDefinedMaintenanceFrequency() {
        return userDefinedMaintenanceFrequency;
    }

    /**
     * Sets the maintenance period other than those defined.
     *
     * @param  newValue  the new user defined maintenance frequency.
     */
    public void setUserDefinedMaintenanceFrequency(final TemporalAmount newValue) {
        checkWritePermission(userDefinedMaintenanceFrequency);
        userDefinedMaintenanceFrequency = newValue;
    }

    /**
     * Return the types of resource and / or extents to which the maintenance information applies.
     *
     * @return type of resource and / or extent to which the maintenance information applies.
     *
     * @since 0.5
     */
    @Override
    // @XmlElement at the end of this class.
    public Collection<Scope> getMaintenanceScopes() {
        return maintenanceScopes = nonNullCollection(maintenanceScopes, Scope.class);
    }

    /**
     * Sets the types of resource and / or extents to which the maintenance information applies.
     *
     * @param  newValues  the types of resource and / or extents to which the maintenance information applies.
     *
     * @since 0.5
     */
    public void setMaintenanceScopes(final Collection<? extends Scope> newValues) {
        maintenanceScopes = writeCollection(newValues, maintenanceScopes, Scope.class);
    }

    /**
     * Returns the scope of data to which maintenance is applied.
     * This method fetches the values from the {@linkplain #getMaintenanceScopes() maintenance scopes}.
     *
     * @return scope of data to which maintenance is applied.
     *
     * @deprecated As of ISO 19115:2014, {@code getUpdateScopes()} and {@link #getUpdateScopeDescriptions()}
     *             were combined into {@link #getMaintenanceScopes()} in order to allow specifying a scope
     *             that includes a spatial and temporal extent.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getMaintenanceScopes")
    @XmlElement(name = "updateScope", namespace = LegacyNamespaces.GMD)
    public final Collection<ScopeCode> getUpdateScopes() {
        if (!FilterByVersion.LEGACY_METADATA.accept()) return null;
        return new LegacyPropertyAdapter<ScopeCode,Scope>(getMaintenanceScopes()) {
            /** Stores a legacy value into the new kind of value. */
            @Override protected Scope wrap(final ScopeCode value) {
                return new DefaultScope(value);
            }

            /** Extracts the legacy value from the new kind of value. */
            @Override protected ScopeCode unwrap(final Scope container) {
                return container.getLevel();
            }

            /** Updates the legacy value in an existing new kind of value. */
            @Override protected boolean update(final Scope container, final ScopeCode value) {
                if (container instanceof DefaultScope) {
                    ((DefaultScope) container).setLevel(value);
                    return true;
                }
                return false;
            }
        }.validOrNull();
    }

    /**
     * Sets the scope of data to which maintenance is applied.
     * This method stores the values in the {@linkplain #getMaintenanceScopes() maintenance scopes}.
     *
     * @param  newValues  the new update scopes.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setMaintenanceScopes(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setUpdateScopes(final Collection<? extends ScopeCode> newValues) {
        checkWritePermission(valueIfDefined(maintenanceScopes));
        ((LegacyPropertyAdapter<ScopeCode,?>) getUpdateScopes()).setValues(newValues);
    }

    /**
     * Returns additional information about the range or extent of the resource.
     * This method fetches the values from the {@linkplain #getMaintenanceScopes() maintenance scopes}.
     *
     * @return additional information about the range or extent of the resource.
     *
     * @deprecated As of ISO 19115:2014, {@link #getUpdateScopes()} and {@code getUpdateScopeDescriptions()}
     *             were combined into {@link #getMaintenanceScopes()} in order to allow specifying a scope
     *             that includes a spatial and temporal extent.
     */
    @Override
    @Deprecated(since="1.0")
    @Dependencies("getMaintenanceScopes")
    @XmlElement(name = "updateScopeDescription", namespace = LegacyNamespaces.GMD)
    public final Collection<ScopeDescription> getUpdateScopeDescriptions() {
        if (!FilterByVersion.LEGACY_METADATA.accept()) return null;
        return new LegacyPropertyAdapter<ScopeDescription,Scope>(getMaintenanceScopes()) {
            /** Stores a legacy value into the new kind of value. */
            @Override protected Scope wrap(final ScopeDescription value) {
                final var container = new DefaultScope();
                container.setLevelDescription(Containers.singletonOrEmpty(value));
                return container;
            }

            /** Extracts the legacy value from the new kind of value. */
            @Override protected ScopeDescription unwrap(final Scope container) {
                return getSingleton(container.getLevelDescription(), ScopeDescription.class,
                        this, DefaultMaintenanceInformation.class, "getUpdateScopeDescriptions");
            }

            /** Updates the legacy value in an existing instance of the new kind of value. */
            @Override protected boolean update(final Scope container, final ScopeDescription value) {
                if (container instanceof DefaultScope) {
                    ((DefaultScope) container).setLevelDescription(Containers.singletonOrEmpty(value));
                    return true;
                }
                return false;
            }
        }.validOrNull();
    }

    /**
     * Sets additional information about the range or extent of the resource.
     * This method stores the values in the {@linkplain #getMaintenanceScopes() maintenance scopes}.
     *
     * @param  newValues  the new update scope descriptions.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #setMaintenanceScopes(Collection)}.
     */
    @Deprecated(since="1.0")
    public void setUpdateScopeDescriptions(final Collection<? extends ScopeDescription> newValues) {
        checkWritePermission(valueIfDefined(maintenanceScopes));
        ((LegacyPropertyAdapter<ScopeDescription,?>) getUpdateScopeDescriptions()).setValues(newValues);
    }

    /**
     * Returns information regarding specific requirements for maintaining the resource.
     *
     * @return information regarding specific requirements for maintaining the resource.
     */
    @Override
    @XmlElement(name = "maintenanceNote")
    public Collection<InternationalString> getMaintenanceNotes() {
        return maintenanceNotes = nonNullCollection(maintenanceNotes, InternationalString.class);
    }

    /**
     * Sets information regarding specific requirements for maintaining the resource.
     *
     * @param  newValues  the new maintenance notes.
     */
    public void setMaintenanceNotes(final Collection<? extends InternationalString> newValues) {
        maintenanceNotes = writeCollection(newValues, maintenanceNotes, InternationalString.class);
    }

    /**
     * Returns identification of, and means of communicating with,
     * person(s) and organization(s) with responsibility for maintaining the resource.
     *
     * @return means of communicating with person(s) and organization(s) with responsibility
     *         for maintaining the resource.
     */
    @Override
    @XmlElement(name = "contact")
    public Collection<Responsibility> getContacts() {
        return contacts = nonNullCollection(contacts, Responsibility.class);
    }

    /**
     * Sets identification of, and means of communicating with,
     * person(s) and organization(s) with responsibility for maintaining the resource.
     *
     * @param  newValues  the new identification of person(s) and organization(s)
     *                    with responsibility for maintaining the resource.
     */
    public void setContacts(final Collection<? extends Responsibility> newValues) {
        contacts = writeCollection(newValues, contacts, Responsibility.class);
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                  ┃
     ┃                               XML support with JAXB                              ┃
     ┃                                                                                  ┃
     ┃        The following methods are invoked by JAXB using reflection (even if       ┃
     ┃        they are private) or are helpers for other methods invoked by JAXB.       ┃
     ┃        Those methods can be safely removed if Geographic Markup Language         ┃
     ┃        (GML) support is not needed.                                              ┃
     ┃                                                                                  ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Invoked by JAXB at both marshalling and unmarshalling time.
     * This attribute has been added by ISO 19115:2014 standard.
     * If (and only if) marshalling an older standard version, we omit this attribute.
     */
    @XmlElement(name = "maintenanceDate")
    private Collection<CitationDate> getMaintenanceDate() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getMaintenanceDates() : null;
    }

    @XmlElement(name = "maintenanceScope")
    private Collection<Scope> getMaintenanceScope() {
        return FilterByVersion.CURRENT_METADATA.accept() ? getMaintenanceScopes() : null;
    }
}
