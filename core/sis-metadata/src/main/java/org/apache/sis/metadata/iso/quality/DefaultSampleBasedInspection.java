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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.quality.SampleBasedInspection;


/**
 * Sample based inspection.
 * The following properties are mandatory in a well-formed metadata according ISO 19157:
 *
 * <div class="preformat">{@code DQ_SampleBasedInspection}
 * {@code   ├─samplingScheme……………} Type of sampling scheme and description of the sampling procedure.
 * {@code   ├─lotDescription……………} How lots are defined.
 * {@code   └─samplingRatio………………} How many samples on average are extracted for inspection from each lot of population.</div>
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
 * @author  Alexis Gaillard (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
@XmlType(name = "DQ_SampleBasedInspection_Type", propOrder = {
    "samplingScheme",
    "lotDescription",
    "samplingRatio"
})
@XmlRootElement(name = "DQ_SampleBasedInspection")
public class DefaultSampleBasedInspection extends AbstractDataEvaluation implements SampleBasedInspection {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -811881513591264926L;

    /**
     * Information of the type of sampling scheme and description of the sampling procedure.
     */
    @SuppressWarnings("serial")
    private InternationalString samplingScheme;

    /**
     * Information of how lots are defined.
     */
    @SuppressWarnings("serial")
    private InternationalString lotDescription;

    /**
     * Information on how many samples on average are extracted for inspection from each lot of population.
     */
    @SuppressWarnings("serial")
    private InternationalString samplingRatio;

    /**
     * Constructs an initially empty sample based description.
     */
    public DefaultSampleBasedInspection() {
    }

   /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <dfn>shallow</dfn> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(SampleBasedInspection)
     */
    public DefaultSampleBasedInspection(final SampleBasedInspection object) {
        super(object);
        if (object != null) {
            samplingScheme = object.getSamplingScheme();
            lotDescription = object.getLotDescription();
            samplingRatio  = object.getSamplingRatio();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultSampleBasedInspection}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultSampleBasedInspection} instance is created using the
     *       {@linkplain #DefaultSampleBasedInspection(SampleBasedInspection) copy constructor}
     *       and returned. Note that this is a <dfn>shallow</dfn> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultSampleBasedInspection castOrCopy(final SampleBasedInspection object) {
        if (object == null || object instanceof DefaultSampleBasedInspection) {
            return (DefaultSampleBasedInspection) object;
        }
        return new DefaultSampleBasedInspection(object);
    }

     /**
     * Returns the information of the type of sampling scheme and description of the sampling procedure.
     *
     * @return sampling scheme and sampling procedure.
     */
    @Override
    @XmlElement(name = "samplingScheme", required = true)
    public InternationalString getSamplingScheme() {
        return samplingScheme;
    }

    /**
     * Sets the information of the type of sampling scheme and description of the sampling procedure.
     *
     * @param  newValue  the new sampling scheme.
     */
    public void setSamplingScheme(final InternationalString newValue) {
        checkWritePermission(samplingScheme);
        samplingScheme = newValue;
    }

     /**
     * Returns the information of how lots are defined.
     *
     * @return information on lots.
     */
    @Override
    @XmlElement(name = "lotDescription", required = true)
    public InternationalString getLotDescription() {
        return lotDescription;
    }

    /**
     * Sets the information of how lots are defined.
     *
     * @param  newValue  the new information.
     */
    public void setLotDescription(final InternationalString newValue) {
        checkWritePermission(lotDescription);
        lotDescription = newValue;
    }

     /**
     * Returns the information on how many samples on average are extracted for inspection from each lot of population.
     *
     * @return average number of samples extracted for inspection.
     */
    @Override
    @XmlElement(name = "samplingRatio", required = true)
    public InternationalString getSamplingRatio() {
        return samplingRatio;
    }

    /**
     * Sets the information on how many samples on average are extracted for inspection from each lot of population.
     *
     * @param  newValue  the new information.
     */
    public void setSamplingRatio(final InternationalString newValue) {
        checkWritePermission(samplingRatio);
        samplingRatio = newValue;
    }
}
