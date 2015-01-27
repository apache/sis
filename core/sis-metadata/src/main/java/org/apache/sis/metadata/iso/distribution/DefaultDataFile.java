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
package org.apache.sis.metadata.iso.distribution;

import java.util.Collection;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.distribution.DataFile;
import org.opengis.util.LocalName;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Description of a transfer data file.
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
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "MX_DataFile_Type", propOrder = {
    "featureTypes",
    "fileFormat"
})
@XmlRootElement(name = "MX_DataFile", namespace = Namespaces.GMX)
public class DefaultDataFile extends ISOMetadata implements DataFile {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4556006719009557349L;

    /**
     * Provides the list of feature types concerned by the transfer data file. Depending on
     * the transfer choices, a data file may contain data related to one or many feature types.
     * This attribute may be omitted when the dataset is composed of a single file and/or the
     * data does not relate to a feature catalogue.
     */
    private Collection<LocalName> featureTypes;

    /**
     * Defines the format of the transfer data file.
     */
    private Format fileFormat;

    /**
     * Constructs an initially empty data file.
     */
    public DefaultDataFile() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(DataFile)
     */
    public DefaultDataFile(final DataFile object) {
        super(object);
        if (object != null) {
            featureTypes = copyCollection(object.getFeatureTypes(), LocalName.class);
            fileFormat   = object.getFileFormat();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultDataFile}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultDataFile} instance is created using the
     *       {@linkplain #DefaultDataFile(DataFile) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDataFile castOrCopy(final DataFile object) {
        if (object == null || object instanceof DefaultDataFile) {
            return (DefaultDataFile) object;
        }
        return new DefaultDataFile(object);
    }

    /**
     * Returns the list of feature types concerned by the transfer data file. Depending on
     * the transfer choices, a data file may contain data related to one or many feature types.
     * This attribute may be omitted when the dataset is composed of a single file and/or the
     * data does not relate to a feature catalogue.
     *
     * @return List of features types concerned by the transfer data file.
     */
    @Override
    @XmlElement(name = "featureType", namespace = Namespaces.GMX)
    public Collection<LocalName> getFeatureTypes() {
        return featureTypes = nonNullCollection(featureTypes, LocalName.class);
    }

    /**
     * Sets the list of feature types concerned by the transfer data file.
     *
     * @param newValues The new feature type values.
     */
    public void setFeatureTypes(final Collection<? extends LocalName> newValues) {
        featureTypes = writeCollection(newValues, featureTypes, LocalName.class);
    }

    /**
     * Returns the format of the transfer data file.
     *
     * @return Format of the transfer data file, or {@code null}.
     */
    @Override
    @XmlElement(name = "fileFormat", namespace = Namespaces.GMX, required = true)
    public Format getFileFormat() {
        return fileFormat;
    }

    /**
     * Sets the format of the transfer data file.
     *
     * @param newValue The new file format value.
     */
    public void setFileFormat(final Format newValue) {
        checkWritePermission();
        fileFormat = newValue;
    }
}
