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
package org.apache.sis.metadata.iso.quality;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.quality.Result;
import org.opengis.metadata.quality.CoverageResult;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.metadata.quality.QuantitativeResult;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Type of test applied to the data specified by a data quality scope.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "AbstractDQ_Result_Type")
@XmlRootElement(name = "DQ_Result")
@XmlSeeAlso({
    DefaultConformanceResult.class,
    DefaultQuantitativeResult.class,
    DefaultCoverageResult.class
})
public class AbstractResult extends ISOMetadata implements Result {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 2961355780515174732L;

    /**
     * Constructs an initially empty result.
     */
    public AbstractResult() {
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * <p>This method checks for the {@link CoverageResult}, {@link QuantitativeResult}, and
     * {@link ConformanceResult} sub-interfaces. If one of those interfaces is found, then
     * this method delegates to the corresponding {@code castOrCopy} static method. If the given
     * object implements more than one of the above-cited interfaces, then the {@code castOrCopy}
     * method to be used is unspecified.</p>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static AbstractResult castOrCopy(final Result object) {
        if (object instanceof CoverageResult) {
            return DefaultCoverageResult.castOrCopy((CoverageResult) object);
        }
        if (object instanceof QuantitativeResult) {
            return DefaultQuantitativeResult.castOrCopy((QuantitativeResult) object);
        }
        if (object instanceof ConformanceResult) {
            return DefaultConformanceResult.castOrCopy((ConformanceResult) object);
        }
        if (object == null || object instanceof AbstractResult) {
            return (AbstractResult) object;
        }
        final AbstractResult copy = new AbstractResult();
        copy.shallowCopy(object);
        return copy;
    }
}
