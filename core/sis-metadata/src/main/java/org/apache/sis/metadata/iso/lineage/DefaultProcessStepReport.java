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
package org.apache.sis.metadata.iso.lineage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.lineage.ProcessStepReport;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.metadata.TitleProperty;
import org.apache.sis.xml.Namespaces;


/**
 * Report of what occurred during the process step.
 * The following property is mandatory in a well-formed metadata according ISO 19115:
 *
 * <div class="preformat">{@code LE_ProcessStepReport}
 * {@code   └─name………………………………} Name of the processing report.</div>
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
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 * @since   0.3
 * @module
 */
@SuppressWarnings("CloneableClassWithoutClone")                 // ModifiableMetadata needs shallow clones.
@TitleProperty(name = "name")
@XmlType(name = "LE_ProcessStepReport_Type", propOrder = {
    "name",
    "description",
    "fileType"
})
@XmlRootElement(name = "LE_ProcessStepReport", namespace = Namespaces.GMI)
public class DefaultProcessStepReport extends ISOMetadata implements ProcessStepReport {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -6413716753156038081L;

    /**
     * Name of the processing report.
     */
    private InternationalString name;

    /**
     * Textual description of what occurred during the process step.
     */
    private InternationalString description;

    /**
     * Type of file that contains the processing report.
     */
    private InternationalString fileType;

    /**
     * Constructs an initially empty process step report.
     */
    public DefaultProcessStepReport() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(ProcessStepReport)
     */
    public DefaultProcessStepReport(final ProcessStepReport object) {
        super(object);
        if (object != null) {
            name        = object.getName();
            description = object.getDescription();
            fileType    = object.getFileType();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultProcessStepReport}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultProcessStepReport} instance is created using the
     *       {@linkplain #DefaultProcessStepReport(ProcessStepReport) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultProcessStepReport castOrCopy(final ProcessStepReport object) {
        if (object == null || object instanceof DefaultProcessStepReport) {
            return (DefaultProcessStepReport) object;
        }
        return new DefaultProcessStepReport(object);
    }

    /**
     * Returns the name of the processing report.
     *
     * @return name of the processing report, or {@code null}.
     */
    @Override
    @XmlElement(name = "name", namespace = Namespaces.GMI, required = true)
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the name of the processing report.
     *
     * @param  newValue  the new name value.
     */
    public void setName(final InternationalString newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Returns the textual description of what occurred during the process step.
     *
     * @return what occurred during the process step, or {@code null}.
     */
    @Override
    @XmlElement(name = "description", namespace = Namespaces.GMI)
    public InternationalString getDescription() {
        return description;
    }

    /**
     * Sets the textual description of what occurred during the process step.
     *
     * @param  newValue  the new description value.
     */
    public void setDescription(final InternationalString newValue) {
        checkWritePermission();
        description = newValue;
    }

    /**
     * Returns the type of file that contains the processing report.
     *
     * @return type of file that contains the processing report, or {@code null}.
     */
    @Override
    @XmlElement(name = "fileType", namespace = Namespaces.GMI)
    public InternationalString getFileType() {
        return fileType;
    }

    /**
     * Sets the type of file that contains the processing report.
     *
     * @param  newValue  the new file type value.
     */
    public void setFileType(final InternationalString newValue) {
        checkWritePermission();
        fileType = newValue;
    }
}
