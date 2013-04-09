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


/**
 * Information about the application schema used to build the dataset.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
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
public class DefaultApplicationSchemaInformation extends ISOMetadata
        implements ApplicationSchemaInformation
{
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
     * @param name               The name of the application schema used.
     * @param schemaLanguage     The the identification of the schema language used.
     * @param constraintLanguage The formal language used in application schema.
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
     * @param object The metadata to copy values from.
     *
     * @see #castOrCopy(ApplicationSchemaInformation)
     */
    public DefaultApplicationSchemaInformation(final ApplicationSchemaInformation object) {
        super(object);
        name                          = object.getName();
        schemaLanguage                = object.getSchemaLanguage();
        constraintLanguage            = object.getConstraintLanguage();
        schemaAscii                   = object.getSchemaAscii();
        graphicsFile                  = object.getGraphicsFile();
        softwareDevelopmentFile       = object.getSoftwareDevelopmentFile();
        softwareDevelopmentFileFormat = object.getSoftwareDevelopmentFileFormat();
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
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
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
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
     */
    @Override
    @XmlElement(name = "name", required = true)
    public synchronized Citation getName() {
        return name;
    }

    /**
     * Sets the name of the application schema used.
     *
     * @param newValue The new name.
     */
    public synchronized void setName(final Citation newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Identification of the schema language used.
     */
    @Override
    @XmlElement(name = "schemaLanguage", required = true)
    public synchronized String getSchemaLanguage() {
        return schemaLanguage;
    }

    /**
     * Sets the identification of the schema language used.
     *
     * @param newValue The new schema language.
     */
    public synchronized void setSchemaLanguage(final String newValue) {
        checkWritePermission();
        schemaLanguage = newValue;
    }

    /**
     * Formal language used in Application Schema.
     */
    @Override
    @XmlElement(name = "constraintLanguage", required = true)
    public synchronized String getConstraintLanguage()  {
        return constraintLanguage;
    }

    /**
     * Sets the formal language used in application schema.
     *
     * @param newValue The new constraint language.
     */
    public synchronized void setConstraintLanguage(final String newValue) {
        checkWritePermission();
        constraintLanguage = newValue;
    }

    /**
     * Full application schema given as an ASCII file.
     */
    @Override
    @XmlElement(name = "schemaAscii")
    public synchronized URI getSchemaAscii()  {
        return schemaAscii;
    }

    /**
     * Sets the full application schema given as an ASCII file.
     *
     * @param newValue The new ASCII file.
     */
    public synchronized void setSchemaAscii(final URI newValue) {
        checkWritePermission();
        schemaAscii = newValue;
    }

    /**
     * Full application schema given as a graphics file.
     */
    @Override
    @XmlElement(name = "graphicsFile")
    public synchronized URI getGraphicsFile()  {
        return graphicsFile;
    }

    /**
     * Sets the full application schema given as a graphics file.
     *
     * @param newValue The new graphics file.
     */
    public synchronized void setGraphicsFile(final URI newValue) {
        checkWritePermission();
        graphicsFile = newValue;
    }

    /**
     * Full application schema given as a software development file.
     */
    @Override
    @XmlElement(name = "softwareDevelopmentFile")
    public synchronized URI getSoftwareDevelopmentFile()  {
        return softwareDevelopmentFile;
    }

    /**
     * Sets the full application schema given as a software development file.
     *
     * @param newValue The new software development file.
     */
    public synchronized void setSoftwareDevelopmentFile(final URI newValue) {
        checkWritePermission();
        softwareDevelopmentFile = newValue;
    }

    /**
     * Software dependent format used for the application schema software dependent file.
     */
    @Override
    @XmlElement(name = "softwareDevelopmentFileFormat")
    public synchronized String getSoftwareDevelopmentFileFormat()  {
        return softwareDevelopmentFileFormat;
    }

    /**
     * Sets the software dependent format used for the application schema software dependent file.
     *
     * @param newValue The new software development file format.
     */
    public synchronized void setSoftwareDevelopmentFileFormat(final String newValue) {
        checkWritePermission();
        softwareDevelopmentFileFormat = newValue;
    }
}
