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

import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.metadata.quality.EvaluationMethodType;
import org.apache.sis.metadata.iso.quality.DefaultDataQuality;
import org.apache.sis.metadata.iso.quality.DefaultDomainConsistency;
import org.apache.sis.metadata.iso.quality.DefaultConformanceResult;
import org.apache.sis.metadata.iso.quality.DefaultScope;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.util.resources.Errors;


/**
 * Provides validation methods to be shared by different implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
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
     * Adds a report for a constraint violation.
     *
     * @param type        Description of the property for which a constraint violation has been found.
     * @param explanation Explanation of the constraint violation.
     */
    void addViolationReport(final AbstractIdentifiedType type, final InternationalString explanation) {
        final DefaultDomainConsistency report = new DefaultDomainConsistency();
        final GenericName name = type.getName();
        report.setMeasureIdentification(name instanceof Identifier ? (Identifier) name : new NamedIdentifier(name));
        report.setMeasureDescription(type.getDescription());
        report.setEvaluationMethodType(EvaluationMethodType.DIRECT_INTERNAL);
        report.getResults().add(new DefaultConformanceResult(null, explanation, false));
        quality.getReports().add(report);
    }

    /**
     * Verifies if the given value is valid for the given attribute type.
     * This method delegates to one of the {@code validate(…)} methods depending of the value type.
     */
    void validateAny(final PropertyType type, final Object value) {
        if (type instanceof DefaultAttributeType<?>) {
            validate((DefaultAttributeType<?>) type, value);
        }
        if (type instanceof DefaultAssociationRole) {
            validate((DefaultAssociationRole) type, (DefaultFeature) value);
        }
    }

    /**
     * Verifies if the given value is valid for the given attribute type.
     */
    void validate(final DefaultAttributeType<?> type, final Object value) {
        if (value != null) {
            /*
             * In theory, the following check is unnecessary since the type was constrained by the Attribute.setValue(T)
             * method signature. However in practice the call to Attribute.setValue(…) is sometime done after type erasure,
             * so we are better to check.
             */
            if (!type.getValueClass().isInstance(value)) {
                addViolationReport(type, Errors.formatInternational(
                        Errors.Keys.IllegalPropertyClass_2, type.getName(), value.getClass()));
            }
        }
        verifyCardinality(type, type.getMinimumOccurs(), type.getMaximumOccurs(), value);
    }

    /**
     * Verifies if the given value is valid for the given association role.
     */
    void validate(final DefaultAssociationRole role, final DefaultFeature value) {
        if (value != null) {
            final DefaultFeatureType type = value.getType();
            if (!role.getValueType().isAssignableFrom(type)) {
                addViolationReport(role, Errors.formatInternational(
                        Errors.Keys.IllegalPropertyClass_2, role.getName(), type.getName()));
            }
        }
        verifyCardinality(role, role.getMinimumOccurs(), role.getMaximumOccurs(), value);
    }

    /**
     * Verifies if the given value mets the cardinality constraint.
     */
    private void verifyCardinality(final AbstractIdentifiedType type,
            final int minimumOccurs, final int maximumOccurs, final Object value)
    {
        if (value == null) {
            if (minimumOccurs != 0) {
                addViolationReport(type, Errors.formatInternational(
                        Errors.Keys.MissingValueForProperty_1, type.getName()));
            }
        } else {
            if (maximumOccurs == 0) {
                // TODO
            }
        }
    }
}
