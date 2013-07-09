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
package org.apache.sis.metadata.iso.identification;

import java.net.URI;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.identification.BrowseGraphic;
import org.apache.sis.metadata.iso.ISOMetadata;


/**
 * Graphic that provides an illustration of the dataset (should include a legend for the graphic).
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "MD_BrowseGraphic_Type", propOrder = {
    "fileName",
    "fileDescription",
    "fileType"
})
@XmlRootElement(name = "MD_BrowseGraphic")
public class DefaultBrowseGraphic extends ISOMetadata implements BrowseGraphic {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 1769063690091153678L;

    /**
     * Name of the file that contains a graphic that provides an illustration of the dataset.
     */
    private URI fileName;

    /**
     * Text description of the illustration.
     */
    private InternationalString fileDescription;

    /**
     * Format in which the illustration is encoded.
     * Examples: CGM, EPS, GIF, JPEG, PBM, PS, TIFF, XWD.
     */
    private String fileType;

    /**
     * Constructs an initially empty browse graphic.
     */
    public DefaultBrowseGraphic() {
    }

    /**
     * Creates a browse graphics initialized to the specified URI.
     *
     * @param fileName The name of the file that contains a graphic.
     */
    public DefaultBrowseGraphic(final URI fileName) {
        this.fileName = fileName;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(BrowseGraphic)
     */
    public DefaultBrowseGraphic(final BrowseGraphic object) {
        super(object);
        if (object != null) {
            fileName        = object.getFileName();
            fileDescription = object.getFileDescription();
            fileType        = object.getFileType();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable actions in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultBrowseGraphic}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultBrowseGraphic} instance is created using the
     *       {@linkplain #DefaultBrowseGraphic(BrowseGraphic) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultBrowseGraphic castOrCopy(final BrowseGraphic object) {
        if (object == null || object instanceof DefaultBrowseGraphic) {
            return (DefaultBrowseGraphic) object;
        }
        return new DefaultBrowseGraphic(object);
    }

    /**
     * Returns the name of the file that contains a graphic that provides an illustration of the dataset.
     *
     * @return File that contains a graphic that provides an illustration of the dataset, or {@code null}.
     */
    @Override
    @XmlElement(name = "fileName", required = true)
    public URI getFileName() {
        return fileName;
    }

    /**
     * Sets the name of the file that contains a graphic that provides an illustration of the dataset.
     *
     * @param newValue The new filename.
     */
    public void setFileName(final URI newValue) {
        checkWritePermission();
        fileName = newValue;
    }

    /**
     * Returns the text description of the illustration.
     *
     * @return Text description of the illustration, or {@code null}.
     */
    @Override
    @XmlElement(name = "fileDescription")
    public InternationalString getFileDescription() {
        return fileDescription;
    }

    /**
     * Sets the text description of the illustration.
     *
     * @param newValue The new file description.
     */
    public void setFileDescription(final InternationalString newValue)  {
        checkWritePermission();
        fileDescription = newValue;
    }

    /**
     * Format in which the illustration is encoded.
     * Examples: CGM, EPS, GIF, JPEG, PBM, PS, TIFF, XWD.
     *
     * @return Format in which the illustration is encoded, or {@code null}.
     */
    @Override
    @XmlElement(name = "fileType")
    public String getFileType() {
        return fileType;
    }

    /**
     * Sets the format in which the illustration is encoded.
     *
     * @param newValue The new file type.
     */
    public void setFileType(final String newValue)  {
        checkWritePermission();
        fileType = newValue;
    }
}
