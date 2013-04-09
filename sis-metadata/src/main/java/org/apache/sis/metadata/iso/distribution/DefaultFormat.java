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
import org.opengis.util.InternationalString;
import org.opengis.metadata.distribution.Format;
import org.opengis.metadata.distribution.Distributor;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.iso.Types;


/**
 * Description of the computer language construct that specifies the representation
 * of data objects in a record, file, message, storage device or transmission channel.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_Format_Type", propOrder = {
    "name",
    "version",
    "amendmentNumber",
    "specification",
    "fileDecompressionTechnique",
    "formatDistributors"
})
@XmlRootElement(name = "MD_Format")
public class DefaultFormat extends ISOMetadata implements Format {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6713019619784302519L;

    /**
     * Name of the data transfer format(s).
     */
    private InternationalString name;

    /**
     * Version of the format (date, number, etc.).
     */
    private InternationalString version;

    /**
     * Amendment number of the format version.
     */
    private InternationalString amendmentNumber;

    /**
     * Name of a subset, profile, or product specification of the format.
     */
    private InternationalString specification;

    /**
     * Recommendations of algorithms or processes that can be applied to read or
     * expand resources to which compression techniques have been applied.
     */
    private InternationalString fileDecompressionTechnique;

    /**
     * Provides information about the distributor's format.
     */
    private Collection<Distributor> formatDistributors;

    /**
     * Constructs an initially empty format.
     */
    public DefaultFormat() {
    }

    /**
     * Creates a format initialized to the given name and version.
     *
     * @param name    The name of the data transfer format(s), or {@code null}.
     * @param version The version of the format (date, number, etc.), or {@code null}.
     */
    public DefaultFormat(final CharSequence name, final CharSequence version) {
        this.name    = Types.toInternationalString(name);
        this.version = Types.toInternationalString(version);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(Format)
     */
    public DefaultFormat(final Format object) {
        super(object);
        name                       = object.getName();
        version                    = object.getVersion();
        amendmentNumber            = object.getAmendmentNumber();
        specification              = object.getSpecification();
        fileDecompressionTechnique = object.getFileDecompressionTechnique();
        formatDistributors         = copyCollection(object.getFormatDistributors(), Distributor.class);
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultFormat}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultFormat} instance is created using the
     *       {@linkplain #DefaultFormat(Format) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultFormat castOrCopy(final Format object) {
        if (object == null || object instanceof DefaultFormat) {
            return (DefaultFormat) object;
        }
        return new DefaultFormat(object);
    }

    /**
     * Returns the name of the data transfer format(s).
     */
    @Override
    @XmlElement(name = "name", required = true)
    public synchronized InternationalString getName() {
        return name;
    }

    /**
     * Sets the name of the data transfer format(s).
     *
     * @param newValue The new name.
     */
    public synchronized void setName(final InternationalString newValue) {
         checkWritePermission();
         name = newValue;
     }

    /**
     * Returns the version of the format (date, number, etc.).
     */
    @Override
    @XmlElement(name = "version", required = true)
    public synchronized InternationalString getVersion() {
        return version;
    }

    /**
     * Sets the version of the format (date, number, etc.).
     *
     * @param newValue The new version.
     */
    public synchronized void setVersion(final InternationalString newValue) {
        checkWritePermission();
        version = newValue;
    }

    /**
     * Returns the amendment number of the format version.
     */
    @Override
    @XmlElement(name = "amendmentNumber")
    public synchronized InternationalString getAmendmentNumber() {
        return amendmentNumber;
    }

    /**
     * Sets the amendment number of the format version.
     *
     * @param newValue The new amendment number.
     */
    public synchronized void setAmendmentNumber(final InternationalString newValue) {
        checkWritePermission();
        amendmentNumber = newValue;
    }

    /**
     * Returns the name of a subset, profile, or product specification of the format.
     */
    @Override
    @XmlElement(name = "specification")
    public synchronized InternationalString getSpecification() {
        return specification;
    }

    /**
     * Sets the name of a subset, profile, or product specification of the format.
     *
     * @param newValue The new specification.
     */
    public synchronized void setSpecification(final InternationalString newValue) {
        checkWritePermission();
        specification = newValue;
    }

    /**
     * Returns recommendations of algorithms or processes that can be applied to read or
     * expand resources to which compression techniques have been applied.
     */
    @Override
    @XmlElement(name = "fileDecompressionTechnique")
    public synchronized InternationalString getFileDecompressionTechnique() {
        return fileDecompressionTechnique;
    }

    /**
     * Sets recommendations of algorithms or processes that can be applied to read or
     * expand resources to which compression techniques have been applied.
     *
     * @param newValue The new file decompression technique.
     */
    public synchronized void setFileDecompressionTechnique(final InternationalString newValue) {
        checkWritePermission();
        fileDecompressionTechnique = newValue;
    }

    /**
     * Provides information about the distributor's format.
     */
    @Override
    @XmlElement(name = "formatDistributor")
    public synchronized Collection<Distributor> getFormatDistributors() {
        return formatDistributors = nonNullCollection(formatDistributors, Distributor.class);
    }

    /**
     * Sets information about the distributor's format.
     *
     * @param newValues The new format distributors.
     */
    public synchronized void setFormatDistributors(final Collection<? extends Distributor> newValues) {
        formatDistributors = writeCollection(newValues, formatDistributors, Distributor.class);
    }
}
