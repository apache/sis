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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.maintenance.MaintenanceFrequency;
import org.opengis.metadata.maintenance.MaintenanceInformation;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.maintenance.ScopeDescription;
import org.opengis.metadata.quality.Scope;
import org.opengis.temporal.PeriodDuration;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.iso.quality.DefaultScope;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.internal.metadata.LegacyProperties;
import org.apache.sis.internal.metadata.MetadataUtilities;
import org.apache.sis.util.resources.Messages;


/**
 * Information about the scope and frequency of updating.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_MaintenanceInformation_Type", propOrder = {
    "maintenanceAndUpdateFrequency",
    "dateOfNextUpdate",
    "userDefinedMaintenanceFrequency",
    "updateScopes",
    "updateScopeDescriptions",
    "maintenanceNotes",
    "contacts"
})
@XmlRootElement(name = "MD_MaintenanceInformation")
public class DefaultMaintenanceInformation extends ISOMetadata implements MaintenanceInformation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -8736825706141936429L;

    /**
     * Frequency with which changes and additions are made to the resource after the
     * initial resource is completed.
     */
    private MaintenanceFrequency maintenanceAndUpdateFrequency;

    /**
     * Date information associated with maintenance of resource.
     */
    private Collection<CitationDate> maintenanceDates;

    /**
     * Maintenance period other than those defined, in milliseconds.
     */
    private PeriodDuration userDefinedMaintenanceFrequency;

    /**
     * Type of resource and / or extent to which the maintenance information applies.
     */
    private Collection<Scope> maintenanceScopes;

    /**
     * Information regarding specific requirements for maintaining the resource.
     */
    private Collection<InternationalString> maintenanceNotes;

    /**
     * Identification of, and means of communicating with, person(s) and organization(s)
     * with responsibility for maintaining the resource.
     */
    private Collection<ResponsibleParty> contacts;

    /**
     * Creates a an initially empty maintenance information.
     */
    public DefaultMaintenanceInformation() {
    }

    /**
     * Creates a maintenance information.
     *
     * @param maintenanceAndUpdateFrequency The frequency with which changes and additions are
     *        made to the resource after the initial resource is completed, or {@code null} if none.
     */
    public DefaultMaintenanceInformation(final MaintenanceFrequency maintenanceAndUpdateFrequency) {
        this.maintenanceAndUpdateFrequency = maintenanceAndUpdateFrequency;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(MaintenanceInformation)
     */
    public DefaultMaintenanceInformation(final MaintenanceInformation object) {
        super(object);
        if (object != null) {
            setDateOfNextUpdate        (object.getDateOfNextUpdate());
            setDateOfNextUpdate        (object.getDateOfNextUpdate());
            setUpdateScopes            (object.getUpdateScopes());
            setUpdateScopeDescriptions (object.getUpdateScopeDescriptions());

            maintenanceAndUpdateFrequency   = object.getMaintenanceAndUpdateFrequency();
///         maintenanceDates                = copyCollection(object.getMaintenanceDate(), CitationDate.class);
            userDefinedMaintenanceFrequency = object.getUserDefinedMaintenanceFrequency();
///         maintenanceScope                = copyCollection(object.getMaintenanceScope(), Scope.class);
            maintenanceNotes                = copyCollection(object.getMaintenanceNotes(), InternationalString.class);
            contacts                        = copyCollection(object.getContacts(), ResponsibleParty.class);
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
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     * @return Frequency with which changes and additions are made to the resource, or {@code null}.
     */
    @Override
    @XmlElement(name = "maintenanceAndUpdateFrequency", required = true)
    public MaintenanceFrequency getMaintenanceAndUpdateFrequency() {
        return maintenanceAndUpdateFrequency;
    }

    /**
     * Sets the frequency with which changes and additions are made to the resource
     * after the initial resource is completed.
     *
     * @param newValue The new maintenance frequency.
     */
    public void setMaintenanceAndUpdateFrequency(final MaintenanceFrequency newValue) {
        checkWritePermission();
        maintenanceAndUpdateFrequency = newValue;
    }

    /**
     * Return the date information associated with maintenance of resource.
     *
     * @return date information associated with maintenance of resource.
     *
     * @since 0.5
     */
/// @Override
/// @XmlElement(name = "maintenanceDate", required = true)
    public Collection<CitationDate> getMaintenanceDates() {
        return maintenanceDates = nonNullCollection(maintenanceDates, CitationDate.class);
    }

    /**
     * Sets the date information associated with maintenance of resource.
     *
     * @param newValues The new date information associated with maintenance of resource.
     *
     * @since 0.5
     */
    public void setMaintenanceDates(final Collection<? extends CitationDate> newValues) {
        maintenanceDates = writeCollection(newValues, maintenanceDates, CitationDate.class);
    }

    /**
     * Returns the scheduled revision date for resource.
     *
     * @return Scheduled revision date, or {@code null}.
     *
     * @deprecated As of ISO 19115:2014, replaced by {@link #getMaintenanceDates()} in order to enable inclusion
     *             of a {@link DateType} to describe the type of the date. Note that {@link DateType#NEXT_UPDATE}
     *             was added to that code list.
     */
    @Override
    @Deprecated
    @XmlElement(name = "dateOfNextUpdate")
    public Date getDateOfNextUpdate() {
        if (maintenanceDates != null) {
            for (final CitationDate date : maintenanceDates) {
                if (DateType.NEXT_UPDATE.equals(date.getDateType())) {
                    return date.getDate();
                }
            }
        }
        return null;
    }

    /**
     * Sets the scheduled revision date for resource.
     *
     * @param newValue The new date of next update.
     */
    @Deprecated
    public void setDateOfNextUpdate(final Date newValue) {
        if (newValue != null) {
            if (maintenanceDates != null) {
                for (final CitationDate date : maintenanceDates) {
                    if (date instanceof DefaultCitationDate && DateType.NEXT_UPDATE.equals(date.getDateType())) {
                        ((DefaultCitationDate) date).setDate(newValue);
                        return;
                    }
                }
            }
            getMaintenanceDates().add(new DefaultCitationDate(newValue, DateType.NEXT_UPDATE));
        }
    }

    /**
     * Returns the maintenance period other than those defined.
     *
     * @return The maintenance period, or {@code null}.
     */
    @Override
    @XmlElement(name = "userDefinedMaintenanceFrequency")
    public PeriodDuration getUserDefinedMaintenanceFrequency() {
        return userDefinedMaintenanceFrequency;
    }

    /**
     * Sets the maintenance period other than those defined.
     *
     * @param newValue The new user defined maintenance frequency.
     */
    public void setUserDefinedMaintenanceFrequency(final PeriodDuration newValue) {
        checkWritePermission();
        userDefinedMaintenanceFrequency = newValue;
    }

    /**
     * Return the types of resource and / or extents to which the maintenance information applies.
     *
     * @return type of resource and / or extent to which the maintenance information applies.
     *
     * @since 0.5
     */
/// @Override
/// @XmlElement(name = "maintenanceScope")
    public Collection<Scope> getMaintenanceScopes() {
        return maintenanceScopes = nonNullCollection(maintenanceScopes, Scope.class);
    }

    /**
     * Sets the types of resource and / or extents to which the maintenance information applies.
     *
     * @param newValues The types of resource and / or extents to which the maintenance information applies.
     *
     * @since 0.5
     */
    public void setMaintenanceScopes(final Collection<? extends Scope> newValues) {
        maintenanceScopes = writeCollection(newValues, maintenanceScopes, Scope.class);
    }

    /**
     * Returns the scope of data to which maintenance is applied.
     *
     * @return Scope of data to which maintenance is applied.
     *
     * @deprecated As of ISO 19115:2014, {@code getUpdateScopes()} and {@link #getUpdateScopeDescriptions()}
     *             were combined into {@link #getMaintenanceScopes()} in order to allow specifying a scope
     *             that includes a spatial and temporal extent.
     */
    @Override
    @Deprecated
    @XmlElement(name = "updateScope")
    public Collection<ScopeCode> getUpdateScopes() {
        final Collection<Scope> scopes = getMaintenanceScopes();
        if (scopes == null) {
            return null; // May happen at marshalling time.
        }
        return new LegacyProperties<ScopeCode,Scope>(scopes) {
            @Override protected Scope wrap(final ScopeCode code) {
                return new DefaultScope(code);
            }

            @Override protected ScopeCode unwrap(final Scope scope) {
                return scope.getLevel();
            }
        };
    }

    /**
     * Sets the scope of data to which maintenance is applied.
     *
     * @param newValues The new update scopes.
     */
    @Deprecated
    public void setUpdateScopes(Collection<? extends ScopeCode> newValues) {
        if (newValues == null) {
            newValues = Collections.emptySet();
        }
        final Iterator<? extends ScopeCode> it = newValues.iterator();
        final Collection<Scope> scopes = getMaintenanceScopes();
        final Iterator<Scope> im = scopes.iterator();
        while (im.hasNext()) {
            final Scope scope = im.next();
            if (scope instanceof DefaultScope) {
                final DefaultScope df = (DefaultScope) scope;
                df.setLevel(it.hasNext() ? it.next() : null);
                if (df.isEmpty()) {
                    im.remove();
                }
            }
        }
        while (it.hasNext()) {
            scopes.add(new DefaultScope(it.next()));
        }
    }

    /**
     * Returns additional information about the range or extent of the resource.
     *
     * @return Additional information about the range or extent of the resource.
     *
     * @deprecated As of ISO 19115:2014, {@link #getUpdateScopes()} and {@code getUpdateScopeDescriptions()}
     *             were combined into {@link #getMaintenanceScopes()} in order to allow specifying a scope
     *             that includes a spatial and temporal extent.
     */
    @Override
    @Deprecated
    @XmlElement(name = "updateScopeDescription")
    public Collection<ScopeDescription> getUpdateScopeDescriptions() {
        final Collection<Scope> scopes = getMaintenanceScopes();
        if (scopes == null) {
            return null; // May happen at marshalling time.
        }
        return new LegacyProperties<ScopeDescription,Scope>(scopes) {
            private boolean warningOccurred;

            @Override protected Scope wrap(final ScopeDescription code) {
                final DefaultScope scope = new DefaultScope();
                scope.setLevelDescription(Collections.singleton(code));
                return scope;
            }

            @Override protected ScopeDescription unwrap(final Scope scope) {
                final Iterator<? extends ScopeDescription> it = scope.getLevelDescription().iterator();
                if (!it.hasNext()) {
                    return null;
                }
                final ScopeDescription description = it.next();
                if (!warningOccurred && it.hasNext()) {
                    warningOccurred = true;
                    MetadataUtilities.warning(DefaultMaintenanceInformation.class, "getUpdateScopeDescriptions",
                            Messages.Keys.IgnoredPropertiesAfterFirst_1, ScopeDescription.class);
                }
                return description;
            }
        };
    }

    /**
     * Sets additional information about the range or extent of the resource.
     *
     * @param newValues The new update scope descriptions.
     */
    @Deprecated
    public void setUpdateScopeDescriptions(Collection<? extends ScopeDescription> newValues) {
        if (newValues == null) {
            newValues = Collections.emptySet();
        }
        final Iterator<? extends ScopeDescription> it = newValues.iterator();
        final Collection<Scope> scopes = getMaintenanceScopes();
        final Iterator<Scope> im = scopes.iterator();
        while (im.hasNext()) {
            final Scope scope = im.next();
            if (scope instanceof DefaultScope) {
                final DefaultScope df = (DefaultScope) scope;
                df.setLevelDescription(it.hasNext() ? Collections.singleton(it.next()) : null);
                if (df.isEmpty()) {
                    im.remove();
                }
            }
        }
        while (it.hasNext()) {
            final DefaultScope scope = new DefaultScope();
            scope.setLevelDescription(Collections.singleton(it.next()));
            scopes.add(scope);
        }
    }

    /**
     * Returns information regarding specific requirements for maintaining the resource.
     *
     * @return Information regarding specific requirements for maintaining the resource.
     */
    @Override
    @XmlElement(name = "maintenanceNote")
    public Collection<InternationalString> getMaintenanceNotes() {
        return maintenanceNotes = nonNullCollection(maintenanceNotes, InternationalString.class);
    }

    /**
     * Sets information regarding specific requirements for maintaining the resource.
     *
     * @param newValues The new maintenance notes.
     */
    public void setMaintenanceNotes(final Collection<? extends InternationalString> newValues) {
        maintenanceNotes = writeCollection(newValues, maintenanceNotes, InternationalString.class);
    }

    /**
     * Returns identification of, and means of communicating with,
     * person(s) and organization(s) with responsibility for maintaining the resource.
     *
     * @return Means of communicating with person(s) and organization(s) with responsibility
     *         for maintaining the resource.
     */
    @Override
    @XmlElement(name = "contact")
    public Collection<ResponsibleParty> getContacts() {
        return contacts = nonNullCollection(contacts, ResponsibleParty.class);
    }

    /**
     * Sets identification of, and means of communicating with,
     * person(s) and organization(s) with responsibility for maintaining the resource.
     *
     * @param newValues The new identification of person(s) and organization(s)
     *                  with responsibility for maintaining the resource.
     */
    public void setContacts(final Collection<? extends ResponsibleParty> newValues) {
        contacts = writeCollection(newValues, contacts, ResponsibleParty.class);
    }
}
