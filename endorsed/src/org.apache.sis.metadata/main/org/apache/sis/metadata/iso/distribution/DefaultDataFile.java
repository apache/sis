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

import java.net.URI;
import java.util.Collection;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.distribution.DataFile;
import org.opengis.util.InternationalString;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.xml.bind.gcx.MimeFileTypeAdapter;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.util.LocalName;


/**
 * Description of a transfer data file.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MX_DataFile}
 * {@code   ├─fileName……………………………………………………………} Name or path of the file.
 * {@code   ├─fileDescription…………………………………………} Text description of the data.
 * {@code   └─fileType……………………………………………………………} Format in which the data is encoded.</div>
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
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
@XmlType(name = "MX_DataFile_Type", namespace = Namespaces.MDT, propOrder = {
    "fileName",
    "fileDescription",
    "fileType",
    "featureTypes",
    "fileFormat"
})
@XmlRootElement(name = "MX_DataFile", namespace = Namespaces.MDT)
public class DefaultDataFile extends ISOMetadata implements DataFile {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4556006719009557349L;

    /**
     * Name or path of the file.
     */
    private URI fileName;

    /**
     * Text description of the file.
     */
    @SuppressWarnings("serial")
    private InternationalString fileDescription;

    /**
     * Format in which the file is encoded.
     */
    private String fileType;

    /**
     * Provides the list of feature types concerned by the transfer data file. Depending on
     * the transfer choices, a data file may contain data related to one or many feature types.
     * This attribute may be omitted when the dataset is composed of a single file and/or the
     * data does not relate to a feature catalogue.
     */
    @SuppressWarnings("serial")
    private Collection<LocalName> featureTypes;

    /**
     * Defines the format of the transfer data file.
     *
     * @deprecated Removed in latest XSD schemas.
     */
    @Deprecated(since="1.0")
    @SuppressWarnings("serial")
    private Format fileFormat;

    /**
     * Constructs an initially empty data file.
     */
    public DefaultDataFile() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(DataFile)
     */
    @SuppressWarnings("deprecation")
    public DefaultDataFile(final DataFile object) {
        super(object);
        if (object != null) {
            fileName        = object.getFileName();
            fileDescription = object.getFileDescription();
            fileType        = object.getFileType();
            featureTypes    = copyCollection(object.getFeatureTypes(), LocalName.class);
            fileFormat      = object.getFileFormat();
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
     *       {@linkplain #DefaultDataFile(DataFile) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultDataFile castOrCopy(final DataFile object) {
        if (object == null || object instanceof DefaultDataFile) {
            return (DefaultDataFile) object;
        }
        return new DefaultDataFile(object);
    }

    /**
     * Returns the name or path of the file.
     *
     * @return file name, or {@code null}.
     *
     * @see org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic#getFileName()
     * @since 1.0
     */
    @Override
    @XmlElement(name = "fileName", required = true)
    public URI getFileName() {
        return fileName;
    }

    /**
     * Sets the name or path of the file.
     *
     * @param  newValue  the new filename or path.
     *
     * @since 1.0
     */
    public void setFileName(final URI newValue) {
        checkWritePermission(fileName);
        fileName = newValue;
    }

    /**
     * Returns the text description of the file.
     *
     * @return text description of the file, or {@code null}.
     *
     * @see org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic#getFileDescription()
     * @since 1.0
     */
    @Override
    @XmlElement(name = "fileDescription", required = true)
    public InternationalString getFileDescription() {
        return fileDescription;
    }

    /**
     * Sets the text description of the file.
     *
     * @param  newValue  the new file description.
     *
     * @since 1.0
     */
    public void setFileDescription(final InternationalString newValue)  {
        checkWritePermission(fileDescription);
        fileDescription = newValue;
    }

    /**
     * Format in which the file is encoded.
     *
     * @return format in which the file is encoded, or {@code null}.
     *
     * @see org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic#getFileType()
     * @since 1.0
     */
    @Override
    @XmlElement(name = "fileType", required = true)
    @XmlJavaTypeAdapter(MimeFileTypeAdapter.class)
    public String getFileType() {
        return fileType;
    }

    /**
     * Sets the format in which the illustration is encoded.
     * Raster formats are encouraged to use one of the names returned by
     * {@link javax.imageio.ImageIO#getReaderFormatNames()}.
     *
     * @param  newValue  the new file type.
     */
    public void setFileType(final String newValue)  {
        checkWritePermission(fileType);
        fileType = newValue;
    }

    /**
     * Returns the list of feature types concerned by the transfer data file. Depending on
     * the transfer choices, a data file may contain data related to one or many feature types.
     * This attribute may be omitted when the dataset is composed of a single file and/or the
     * data does not relate to a feature catalogue.
     *
     * @return list of features types concerned by the transfer data file.
     */
    @Override
    @XmlElement(name = "featureTypes")
    public Collection<LocalName> getFeatureTypes() {
        return featureTypes = nonNullCollection(featureTypes, LocalName.class);
    }

    /**
     * Sets the list of feature types concerned by the transfer data file.
     *
     * @param newValues  the new feature type values.
     */
    public void setFeatureTypes(final Collection<? extends LocalName> newValues) {
        featureTypes = writeCollection(newValues, featureTypes, LocalName.class);
    }

    /**
     * Returns the format of the transfer data file.
     *
     * @return format of the transfer data file, or {@code null}.
     *
     * @deprecated Removed in latest XSD schemas.
     */
    @Override
    @Deprecated(since="1.0")
    @XmlElement(name = "fileFormat", namespace = LegacyNamespaces.GMX)
    public Format getFileFormat() {
        return FilterByVersion.LEGACY_METADATA.accept() ? fileFormat : null;
    }

    /**
     * Sets the format of the transfer data file.
     *
     * @param newValue  the new file format value.
     *
     * @deprecated Removed in latest XSD schemas.
     */
    @Deprecated(since="1.0")
    public void setFileFormat(final Format newValue) {
        checkWritePermission(fileFormat);
        fileFormat = newValue;
    }
}
