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

import org.opengis.metadata.quality.DataQuality;
import org.apache.sis.metadata.iso.quality.DefaultDataQuality;
import org.apache.sis.metadata.iso.quality.DefaultDomainConsistency;
import org.apache.sis.metadata.iso.quality.DefaultQuantitativeResult;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.referencing.NamedIdentifier;

import static java.util.Collections.singleton;


/**
 * For testing {@link AbstractAttribute} customization.
 * This implementation adds its own criterion to the attribute quality evaluation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@SuppressWarnings("serial")
final strictfp class CustomAttribute<V> extends AbstractAttribute<V> {
    /**
     * A quality information that this attribute will report in addition to the default ones.
     */
    static final String ADDITIONAL_QUALITY_INFO = "Some statistical quality measurement.";

    /**
     * The singleton value.
     */
    private V value;

    /**
     * Creates a new attribute.
     */
    public CustomAttribute(final DefaultAttributeType<V> type) {
        super(type);
        value = type.getDefaultValue();
    }

    /**
     * Returns the singleton value.
     */
    @Override
    public V getValue() {
        return value;
    }

    /**
     * Sets the singleton value.
     */
    @Override
    public void setValue(final V value) {
        this.value = value;
    }

    /**
     * Evaluates the quality of this attribute with a custom rule.
     */
    @Override
    public DataQuality quality() {
        final DefaultDataQuality        quality = (DefaultDataQuality) super.quality();
        final DefaultDomainConsistency  report  = new DefaultDomainConsistency();
        final DefaultQuantitativeResult result  = new DefaultQuantitativeResult();
        result.setErrorStatistic(new SimpleInternationalString(ADDITIONAL_QUALITY_INFO));
        report.setMeasureIdentification(new NamedIdentifier(getName()));
        report .setResults(singleton(result));
        quality.setReports(singleton(report));
        return quality;
    }
}
