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
package org.apache.sis.feature;

import java.util.Collection;
import java.util.Collections;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.DataQuality;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.apache.sis.metadata.iso.quality.AbstractElement;
import org.apache.sis.metadata.iso.quality.DefaultDataQuality;
import org.apache.sis.metadata.iso.quality.DefaultDomainConsistency;
import org.apache.sis.metadata.iso.quality.DefaultConformanceResult;
import org.apache.sis.metadata.iso.maintenance.DefaultScope;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.util.resources.Errors;


/**
 * Provides validation methods to be shared by different implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 */
final class Validator {
    /**
     * The data quality report.
     */
    final DefaultDataQuality quality;

    /**
     * Creates a new validator.
     *
     * @param feature {@code FEATURE} if the object to validate is a feature,
     *        or {@code ATTRIBUTE} for an attribute, or {@code null} otherwise.
     */
    Validator(final ScopeCode scope) {
        quality = new DefaultDataQuality();
        if (scope != null) {
            quality.setScope(new DefaultScope(scope));
        }
    }

    /**
     * Adds a report for a constraint violation. If the given {@code report} is {@code null}, then this method creates
     * a new {@link DefaultDomainConsistency} instance with the measure identification set to the property name.
     *
     * <div class="note"><b>Note:</b>
     * setting {@code measureIdentification} to the property name may look like a departure from ISO intend,
     * since the former should be an identification of the <em>quality measurement</em> rather then the measure itself.
     * (setting {@code measureDescription} to {@code type.getDescription()} would probably be wrong for that reason).
     * However {@code measureIdentification} is only an identifier, not a full description of the quality measurement
     * We are not strictly forbidden to use the same identifier for both the quality measurement than the measurement
     * itself. However strictly speaking, maybe we should use a different scope.</div>
     *
     * @param  report      Where to add the result, or {@code null} if not yet created.
     * @param  type        Description of the property for which a constraint violation has been found.
     * @param  explanation Explanation of the constraint violation.
     * @return The {@code report}, or a new report if {@code report} was null.
     */
    private AbstractElement addViolationReport(AbstractElement report,
            final AbstractIdentifiedType type, final InternationalString explanation)
    {
        if (report == null) {
            final GenericName name = type.getName();
            report = new DefaultDomainConsistency();
            // Do not invoke report.setMeasureDescription(type.getDescription()) - see above javadoc.
            report.setMeasureIdentification(name instanceof Identifier ? (Identifier) name : new NamedIdentifier(name));
            report.setEvaluationMethodType(EvaluationMethodType.DIRECT_INTERNAL);
            quality.getReports().add(report);
        }
        report.getResults().add(new DefaultConformanceResult(null, explanation, false));
        return report;
    }

    /**
     * Wraps singleton value in a collection for processing by {@code validate(…)} methods.
     */
    private static Collection<?> asList(final Object value, final int maximumOccurrences) {
        if (maximumOccurrences <= 1) {
            return (value != null) ? Collections.singletonList(value) : Collections.emptyList();
        } else {
            return (Collection<?>) value;
        }
    }

    /**
     * Implementation of {@link AbstractFeature#quality()}, also shared by {@link Features} static method.
     *
     * @param type     the type of the {@code feature} argument, provided explicitely for protecting from user overriding.
     * @param feature  the feature to validate.
     */
    void validate(final FeatureType type, final AbstractFeature feature) {
        for (final AbstractIdentifiedType pt : type.getProperties(true)) {
            final Object property = feature.getProperty(pt.getName().toString());
            final DataQuality pq;
            if (property instanceof AbstractAttribute<?>) {
                pq = ((AbstractAttribute<?>) property).quality();
            } else if (property instanceof AbstractAssociation) {
                pq = ((AbstractAssociation) property).quality();
            } else if (property instanceof AbstractAttribute<?>) {
                validateAny(((AbstractAttribute<?>) property).getType(), ((AbstractAttribute<?>) property).getValues());
                continue;
            } else if (property instanceof AbstractAssociation) {
                validateAny(((AbstractAssociation) property).getRole(), ((AbstractAssociation) property).getValues());
                continue;
            } else {
                continue;
            }
            if (pq != null) {                                          // Should not be null, but let be safe.
                quality.getReports().addAll(pq.getReports());
            }
        }
    }

    /**
     * Verifies if the given value is valid for the given attribute type.
     * This method delegates to one of the {@code validate(…)} methods depending of the value type.
     */
    void validateAny(final AbstractIdentifiedType type, final Object value) {
        if (type instanceof DefaultAttributeType<?>) {
            validate((DefaultAttributeType<?>) type, asList(value,
                    ((DefaultAttributeType<?>) type).getMaximumOccurs()));
        }
        if (type instanceof DefaultAssociationRole) {
            validate((DefaultAssociationRole) type, asList(value,
                    ((DefaultAssociationRole) type).getMaximumOccurs()));
        }
    }

    /**
     * Verifies if the given values are valid for the given attribute type.
     */
    void validate(final DefaultAttributeType<?> type, final Collection<?> values) {
        AbstractElement report = null;
        for (final Object value : values) {
            /*
             * In theory, the following check is unnecessary since the type was constrained by the Attribute.setValue(V)
             * method signature. However in practice the call to Attribute.setValue(…) is sometime done after type erasure,
             * so we are better to check.
             */
            final Class<?> valueClass = type.getValueClass();
            if (!valueClass.isInstance(value)) {
                report = addViolationReport(report, type, Errors.formatInternational(
                        Errors.Keys.IllegalPropertyValueClass_3, type.getName(), valueClass, value.getClass()));

                // Report only the first violation for now.
                break;
            }
        }
        verifyCardinality(report, type, type.getMinimumOccurs(), type.getMaximumOccurs(), values.size());
    }

    /**
     * Verifies if the given value is valid for the given association role.
     */
    void validate(final DefaultAssociationRole role, final Collection<?> values) {
        AbstractElement report = null;
        for (final Object value : values) {
            final DefaultFeatureType type = ((AbstractFeature) value).getType();
            final FeatureType valueType = role.getValueType();
            if (!valueType.isAssignableFrom(type)) {
                report = addViolationReport(report, role, Errors.formatInternational(
                        Errors.Keys.IllegalPropertyValueClass_3, role.getName(), valueType.getName(), type.getName()));

                // Report only the first violation for now.
                break;
            }
        }
        verifyCardinality(report, role, role.getMinimumOccurs(), role.getMaximumOccurs(), values.size());
    }

    /**
     * Verifies if the given value mets the cardinality constraint.
     *
     * @param report Where to add the result, or {@code null} if not yet created.
     */
    private void verifyCardinality(final AbstractElement report, final AbstractIdentifiedType type,
            final int minimumOccurs, final int maximumOccurs, final int count)
    {
        if (count < minimumOccurs) {
            final InternationalString message;
            if (count == 0) {
                message = Errors.formatInternational(Errors.Keys.MissingValueForProperty_1, type.getName());
            } else {
                message = Errors.formatInternational(Errors.Keys.TooFewOccurrences_2, minimumOccurs, type.getName());
            }
            addViolationReport(report, type, message);
        } else if (count > maximumOccurs) {
            final InternationalString message;
            if (maximumOccurs == 0) {
                message = Errors.formatInternational(Errors.Keys.ForbiddenProperty_1, type.getName());
            } else {
                message = Errors.formatInternational(Errors.Keys.TooManyOccurrences_2, maximumOccurs, type.getName());
            }
            addViolationReport(report, type, message);
        }
    }
}
