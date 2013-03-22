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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
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
    private static final long serialVersionUID = 2784101441023323052L;

    /**
     * Name of the series, or aggregate dataset, of which the dataset is a part.
     */
    private InternationalString name;

    /**
     * Information identifying the issue of the series.
     */
    private String issueIdentification;

    /**
     * Details on which pages of the publication the article was published.
     */
    private String page;

    /**
     * Constructs a default series.
     */
    public DefaultSeries() {
    }

    /**
     * Constructs a series with the specified name.
     *
     * @param name The name of the series of which the dataset is a part, or {@code null}.
     */
    public DefaultSeries(final CharSequence name) {
        this.name = Types.toInternationalString(name);
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultSeries castOrCopy(final Series object) {
        if (object == null || object instanceof DefaultSeries) {
            return (DefaultSeries) object;
        }
        final DefaultSeries copy = new DefaultSeries();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the name of the series, or aggregate dataset, of which the dataset is a part.
     */
    @Override
    @XmlElement(name = "name")
    public synchronized InternationalString getName() {
        return name;
    }

    /**
     * Sets the name of the series, or aggregate dataset, of which the dataset is a part.
     *
     * @param newValue The new name, or {@code null} if none.
     */
    public synchronized void setName(final InternationalString newValue) {
        checkWritePermission();
        name = newValue;
    }

    /**
     * Returns information identifying the issue of the series.
     */
    @Override
    @XmlElement(name = "issueIdentification")
    public synchronized String getIssueIdentification() {
        return issueIdentification;
    }

    /**
     * Sets information identifying the issue of the series.
     *
     * @param newValue The new issue identification, or {@code null} if none.
     */
    public synchronized void setIssueIdentification(final String newValue) {
        checkWritePermission();
        issueIdentification = newValue;
    }

    /**
     * Returns details on which pages of the publication the article was published.
     */
    @Override
    @XmlElement(name = "page")
    public synchronized String getPage() {
        return page;
    }

    /**
     * Sets details on which pages of the publication the article was published.
     *
     * @param newValue The new page, or {@code null} if none.
     */
    public synchronized void setPage(final String newValue) {
        checkWritePermission();
        page = newValue;
    }
}
