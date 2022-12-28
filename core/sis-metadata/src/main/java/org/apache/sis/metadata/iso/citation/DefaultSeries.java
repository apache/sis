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
package org.apache.sis.metadata.iso.citation;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.citation.Series;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.iso.Types;


/**
 * Information about the series, or aggregate dataset, to which a dataset belongs.
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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @version 1.0
 * @since   0.3
 */
@XmlType(name = "CI_Series_Type", propOrder = {
    "name",
    "issueIdentification",
    "page"
})
@XmlRootElement(name = "CI_Series")
public class DefaultSeries extends ISOMetadata implements Series {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7061644572814855051L;

    /**
     * Name of the series, or aggregate dataset, of which the dataset is a part.
     */
    @SuppressWarnings("serial")
    private InternationalString name;

    /**
     * Information identifying the issue of the series.
     */
    @SuppressWarnings("serial")
    private String issueIdentification;

    /**
     * Details on which pages of the publication the article was published.
     */
    @SuppressWarnings("serial")
    private String page;

    /**
     * Constructs a default series.
     */
    public DefaultSeries() {
    }

    /**
     * Constructs a series with the specified name.
     *
     * @param  name  the name of the series of which the dataset is a part, or {@code null}.
     */
    public DefaultSeries(final CharSequence name) {
        this.name = Types.toInternationalString(name);
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <em>shallow</em> copy constructor, because the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param  object  the metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(Series)
     */
    public DefaultSeries(final Series object) {
        super(object);
        if (object != null) {
            name                = object.getName();
            issueIdentification = object.getIssueIdentification();
            page                = object.getPage();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultSeries}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultSeries} instance is created using the
     *       {@linkplain #DefaultSeries(Series) copy constructor} and returned.
     *       Note that this is a <em>shallow</em> copy operation, because the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultSeries castOrCopy(final Series object) {
        if (object == null || object instanceof DefaultSeries) {
            return (DefaultSeries) object;
        }
        return new DefaultSeries(object);
    }

    /**
     * Returns the name of the series, or aggregate dataset, of which the dataset is a part.
     *
     * @return the name of the series or aggregate dataset, or {@code null}.
     */
    @Override
    @XmlElement(name = "name")
    public InternationalString getName() {
        return name;
    }

    /**
     * Sets the name of the series, or aggregate dataset, of which the dataset is a part.
     *
     * @param  newValue  the new name, or {@code null} if none.
     */
    public void setName(final InternationalString newValue) {
        checkWritePermission(name);
        name = newValue;
    }

    /**
     * Returns information identifying the issue of the series.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code String} is replaced by the {@link InternationalString} interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @return information identifying the issue of the series, or {@code null}.
     */
    @Override
    @XmlElement(name = "issueIdentification")
    public String getIssueIdentification() {
        return issueIdentification;
    }

    /**
     * Sets information identifying the issue of the series.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code String} is replaced by the {@link InternationalString} interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @param  newValue  the new issue identification, or {@code null} if none.
     */
    public void setIssueIdentification(final String newValue) {
        checkWritePermission(issueIdentification);
        issueIdentification = newValue;
    }

    /**
     * Returns details on which pages of the publication the article was published.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code String} is replaced by the {@link InternationalString} interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @return details on which pages of the publication the article was published, or {@code null}.
     */
    @Override
    @XmlElement(name = "page")
    public String getPage() {
        return page;
    }

    /**
     * Sets details on which pages of the publication the article was published.
     *
     * <div class="warning"><b>Upcoming API change — generalization</b><br>
     * As of ISO 19115:2014, {@code String} is replaced by the {@link InternationalString} interface.
     * This change will be tentatively applied in GeoAPI 4.0.
     * </div>
     *
     * @param  newValue  the new page, or {@code null} if none.
     */
    public void setPage(final String newValue) {
        checkWritePermission(page);
        page = newValue;
    }
}
