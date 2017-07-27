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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.content.CoverageDescription;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.quality.CoverageResult;
import org.opengis.metadata.distribution.DataFile;
import org.opengis.metadata.spatial.SpatialRepresentation;
import org.opengis.metadata.spatial.SpatialRepresentationType;
import org.apache.sis.xml.Namespaces;


/**
 * Result of a data quality measure organising the measured values as a coverage.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code QE_CoverageResult}
 * {@code   ├─spatialRepresentationType……………………} Method used to spatially represent the coverage result.
 * {@code   ├─resultSpatialRepresentation………………} Digital representation of data quality measures composing the coverage result.
 * {@code   ├─resultContentDescription………………………} Description of the content of the result coverage, i.e. semantic definition of the data quality measures.
 * {@code   │   └─attributeDescription………………………} Description of the attribute described by the measurement value.
 * {@code   ├─resultFormat………………………………………………………} Information about the format of the result coverage data.
 * {@code   │   └─formatSpecificationCitation……} Citation/URL of the specification format.
 * {@code   │       ├─title……………………………………………………} Name by which the cited resource is known.
 * {@code   │       └─date………………………………………………………} Reference date for the cited resource.
 * {@code   └─resultFile……………………………………………………………} Information about the data file containing the result coverage data.
 * {@code       └─fileFormat…………………………………………………} Defines the format of the transfer data file.</div>
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 * @since   0.3
 * @module
 */
@SuppressWarnings("CloneableClassWithoutClone")                 // ModifiableMetadata needs shallow clones.
@XmlType(name = "QE_CoverageResult_Type", propOrder = {
    "spatialRepresentationType",
    "resultSpatialRepresentation",
    "resultContentDescription",
    "resultFormat",
    "resultFile"
})
@XmlRootElement(name = "QE_CoverageResult", namespace = Namespaces.GMI)
public class DefaultCoverageResult extends AbstractResult implements CoverageResult {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 5860811052940576277L;

    /**
     * Method used to spatially represent the coverage result.
     */
    private SpatialRepresentationType spatialRepresentationType;

    /**
     * Provides the digital representation of data quality measures composing the coverage result.
     */
    private SpatialRepresentation resultSpatialRepresentation;

    /**
     * Provides the description of the content of the result coverage, i.e. semantic definition
     * of the data quality measures.
     */
    private CoverageDescription resultContentDescription;

    /**
     * Provides information about the format of the result coverage data.
     */
    private Format resultFormat;

    /**
     * Provides information about the data file containing the result coverage data.
     */
    private DataFile resultFile;

    /**
     * Constructs an initially empty coverage result.
     */
    public DefaultCoverageResult() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(CoverageResult)
     */
    public DefaultCoverageResult(final CoverageResult object) {
        super(object);
        if (object != null) {
            spatialRepresentationType   = object.getSpatialRepresentationType();
            resultSpatialRepresentation = object.getResultSpatialRepresentation();
            resultContentDescription    = object.getResultContentDescription();
            resultFormat                = object.getResultFormat();
            resultFile                  = object.getResultFile();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultCoverageResult}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultCoverageResult} instance is created using the
     *       {@linkplain #DefaultCoverageResult(CoverageResult) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultCoverageResult castOrCopy(final CoverageResult object) {
        if (object == null || object instanceof DefaultCoverageResult) {
            return (DefaultCoverageResult) object;
        }
        return new DefaultCoverageResult(object);
    }

    /**
     * Returns the method used to spatially represent the coverage result.
     *
     * @return spatial representation of the coverage result, or {@code null}.
     */
    @Override
    @XmlElement(name = "spatialRepresentationType", namespace = Namespaces.GMI, required = true)
    public SpatialRepresentationType getSpatialRepresentationType() {
        return spatialRepresentationType;
    }

    /**
     * Sets the method used to spatially represent the coverage result.
     *
     * @param  newValue  the new spatial representation type value.
     */
    public void setSpatialRepresentationType(final SpatialRepresentationType newValue) {
        checkWritePermission();
        spatialRepresentationType = newValue;
    }

    /**
     * Returns the digital representation of data quality measures composing the coverage result.
     *
     * @return digital representation of data quality measures composing the coverage result, or {@code null}.
     */
    @Override
    @XmlElement(name = "resultSpatialRepresentation", namespace = Namespaces.GMI, required = true)
    public SpatialRepresentation getResultSpatialRepresentation() {
        return resultSpatialRepresentation;
    }

    /**
     * Sets the digital representation of data quality measures composing the coverage result.
     *
     * @param  newValue  the new spatial representation value.
     */
    public void setResultSpatialRepresentation(final SpatialRepresentation newValue) {
        checkWritePermission();
        resultSpatialRepresentation = newValue;
    }

    /**
     * Returns the description of the content of the result coverage, i.e. semantic definition
     * of the data quality measures.
     *
     * @return description of the content of the result coverage, or {@code null}.
     */
    @Override
    @XmlElement(name = "resultContentDescription", namespace = Namespaces.GMI, required = true)
    public CoverageDescription getResultContentDescription() {
        return resultContentDescription;
    }

    /**
     * Sets the description of the content of the result coverage, i.e. semantic definition
     * of the data quality measures.
     *
     * @param  newValue  the new content description value.
     */
    public void setResultContentDescription(final CoverageDescription newValue) {
        checkWritePermission();
        resultContentDescription = newValue;
    }

    /**
     * Returns the information about the format of the result coverage data.
     *
     * @return format of the result coverage data, or {@code null}.
     */
    @Override
    @XmlElement(name = "resultFormat", namespace = Namespaces.GMI, required = true)
    public Format getResultFormat() {
        return resultFormat;
    }

    /**
     * Sets the information about the format of the result coverage data.
     *
     * @param  newValue  the new result format value.
     */
    public void setResultFormat(final Format newValue) {
        checkWritePermission();
        resultFormat = newValue;
    }

    /**
     * Returns the information about the data file containing the result coverage data.
     *
     * @return data file containing the result coverage data, or {@code null}.
     */
    @Override
    @XmlElement(name = "resultFile", namespace = Namespaces.GMX, required = true)
    public DataFile getResultFile() {
        return resultFile;
    }

    /**
     * Sets the information about the data file containing the result coverage data.
     *
     * @param  newValue  the new result file value.
     */
    public void setResultFile(final DataFile newValue) {
        checkWritePermission();
        resultFile = newValue;
    }
}
