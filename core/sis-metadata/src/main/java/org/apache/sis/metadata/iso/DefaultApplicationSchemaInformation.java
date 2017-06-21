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
package org.apache.sis.metadata.iso;

import java.net.URI;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.ApplicationSchemaInformation;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnlineResource;


/**
 * Information about the application schema used to build the dataset.
 * The following properties are mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code MD_ApplicationSchemaInformation}
 * {@code   ├─name……………………………………………………………} Name of the application schema used.
 * {@code   │   ├─title………………………………………………} Name by which the cited resource is known.
 * {@code   │   └─date…………………………………………………} Reference date for the cited resource.
 * {@code   ├─schemaLanguage…………………………………} Identification of the schema language used.
 * {@code   └─constraintLanguage………………………} Formal language used in Application Schema.</div>
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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @version 0.5
 * @since   0.3
 * @module
 */
@SuppressWarnings("CloneableClassWithoutClone")                 // ModifiableMetadata needs shallow clones.
@XmlType(name = "MD_ApplicationSchemaInformation_Type", propOrder = {
    "name",
    "schemaLanguage",
    "constraintLanguage",
    "schemaAscii",
    "graphicsFile",
    "softwareDevelopmentFile",
    "softwareDevelopmentFileFormat"
})
@XmlRootElement(name = "MD_ApplicationSchemaInformation")
public class DefaultApplicationSchemaInformation extends ISOMetadata implements ApplicationSchemaInformation {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -884081423040392985L;

    /**
     * Name of the application schema used.
     */
    private Citation name;

    /**
     * Identification of the schema language used.
     */
    private String schemaLanguage;

    /**
     * Formal language used in Application Schema.
     */
    private String constraintLanguage;

    /**
     * Full application schema given as an ASCII file.
     */
    private URI schemaAscii;

    /**
     * Full application schema given as a graphics file.
     */
    private URI graphicsFile;

    /**
     * Full application schema given as a software development file.
     */
    private URI softwareDevelopmentFile;

    /**
     * Software dependent format used for the application schema software dependent file.
     */
    private String softwareDevelopmentFileFormat;

    /**
     * Construct an initially empty application schema information.
     */
    public DefaultApplicationSchemaInformation() {
    }

    /**
     * Creates a application schema information initialized to the specified values.
     *
     * @param name                the name of the application schema used.
     * @param schemaLanguage      the the identification of the schema language used.
     * @param constraintLanguage  the formal language used in application schema.
     */
    public DefaultApplicationSchemaInformation(final Citation name,
                                               final String schemaLanguage,
                                               final String constraintLanguage)
    {
        this.name = name;
        this.schemaLanguage = schemaLanguage;
        this.constraintLanguage = constraintLanguage;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(ApplicationSchemaInformation)
     */
    public DefaultApplicationSchemaInformation(final ApplicationSchemaInformation object) {
        super(object);
        if (object != null) {
            name                          = object.getName();
            schemaLanguage                = object.getSchemaLanguage();
            constraintLanguage            = object.getConstraintLanguage();
            schemaAscii                   = object.getSchemaAscii();
            graphicsFile                  = object.getGraphicsFile();
            softwareDevelopmentFile       = object.getSoftwareDevelopmentFile();
            softwareDevelopmentFileFormat = object.getSoftwareDevelopmentFileFormat();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultApplicationSchemaInformation}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultApplicationSchemaInformation} instance is created using the
     *       {@linkplain #DefaultApplicationSchemaInformation(ApplicationSchemaInformation) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultApplicationSchemaInformation castOrCopy(final ApplicationSchemaInformation object) {
        if (object == null || object instanceof DefaultApplicationSchemaInformation) {
            return (DefaultApplicationSchemaInformation) object;
        }
        return new DefaultApplicationSchemaInformation(object);
    }

    /**
     * Name of the application schema used.
     *
     * @return name of the application schema, or {@code null}.
     */
    @Override
    @XmlElement(name = "name", required = true)
    public Citation getName() {
        return name;
    }

    /**
     * Sets the name of the application schema used.
     *
     * @param  newValue  the new name.
     */
    public void setName(final Citation newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Identification of the schema language used.
     *
     * @return the schema language used, or {@code null}.
     */
    @Override
    @XmlElement(name = "schemaLanguage", required = true)
    public String getSchemaLanguage() {
        return schemaLanguage;
    }

    /**
     * Sets the identification of the schema language used.
     *
     * @param  newValue  the new schema language.
     */
    public void setSchemaLanguage(final String newValue) {
        checkWritePermission();
        schemaLanguage = newValue;
    }

    /**
     * Formal language used in Application Schema.
     *
     * @return formal language used in Application Schema, or {@code null}.
     */
    @Override
    @XmlElement(name = "constraintLanguage", required = true)
    public String getConstraintLanguage()  {
        return constraintLanguage;
    }

    /**
     * Sets the formal language used in application schema.
     *
     * @param  newValue  the new constraint language.
     */
    public void setConstraintLanguage(final String newValue) {
        checkWritePermission();
        constraintLanguage = newValue;
    }

    /**
     * Full application schema given as an ASCII file.
     *
     * <div class="warning"><b>Upcoming API change</b><br>
     * {@code URI} may be replaced by {@link CharSequence} in GeoAPI 4.0.
     * </div>
     *
     * @return application schema as an ASCII file, or {@code null}.
     */
    @Override
    @XmlElement(name = "schemaAscii")
    public URI getSchemaAscii()  {
        return schemaAscii;
    }

    /**
     * Sets the full application schema given as an ASCII file.
     *
     * <div class="warning"><b>Upcoming API change</b><br>
     * {@code URI} may be replaced by {@link CharSequence} in GeoAPI 4.0.
     * </div>
     *
     * @param  newValue  the new ASCII file.
     */
    public void setSchemaAscii(final URI newValue) {
        checkWritePermission();
        schemaAscii = newValue;
    }

    /**
     * Full application schema given as a graphics file.
     *
     * <div class="warning"><b>Upcoming API change</b><br>
     * As of ISO 19115:2014, {@code URI} is replaced by {@link OnlineResource}.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @return application schema as a graphics file, or {@code null}.
     */
    @Override
    @XmlElement(name = "graphicsFile")
    public URI getGraphicsFile()  {
        return graphicsFile;
    }

    /**
     * Sets the full application schema given as a graphics file.
     *
     * <div class="warning"><b>Upcoming API change</b><br>
     * As of ISO 19115:2014, {@code URI} is replaced by {@link OnlineResource}.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @param  newValue  the new graphics file.
     */
    public void setGraphicsFile(final URI newValue) {
        checkWritePermission();
        graphicsFile = newValue;
    }

    /**
     * Full application schema given as a software development file.
     *
     * <div class="warning"><b>Upcoming API change</b><br>
     * As of ISO 19115:2014, {@code URI} is replaced by {@link OnlineResource}.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @return application schema as a software development file, or {@code null}.
     */
    @Override
    @XmlElement(name = "softwareDevelopmentFile")
    public URI getSoftwareDevelopmentFile()  {
        return softwareDevelopmentFile;
    }

    /**
     * Sets the full application schema given as a software development file.
     *
     * <div class="warning"><b>Upcoming API change</b><br>
     * As of ISO 19115:2014, {@code URI} is replaced by {@link OnlineResource}.
     * This change may be applied in GeoAPI 4.0.
     * </div>
     *
     * @param  newValue  the new software development file.
     */
    public void setSoftwareDevelopmentFile(final URI newValue) {
        checkWritePermission();
        softwareDevelopmentFile = newValue;
    }

    /**
     * Software dependent format used for the application schema software dependent file.
     *
     * @return format used for the application schema software file, or {@code null}.
     */
    @Override
    @XmlElement(name = "softwareDevelopmentFileFormat")
    public String getSoftwareDevelopmentFileFormat()  {
        return softwareDevelopmentFileFormat;
    }

    /**
     * Sets the software dependent format used for the application schema software dependent file.
     *
     * @param  newValue  the new software development file format.
     */
    public void setSoftwareDevelopmentFileFormat(final String newValue) {
        checkWritePermission();
        softwareDevelopmentFileFormat = newValue;
    }
}
