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

// Specific to the main and geoapi-3.1 branches:
import java.net.URI;
import java.net.URISyntaxException;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.opengis.metadata.citation.OnlineResource;
import org.apache.sis.xml.bind.metadata.CI_OnlineResource;
import org.apache.sis.metadata.iso.citation.DefaultOnlineResource;


/**
 * Converts an URI to a {@code <cit:OnlineResource>} element for ISO 19115-3:2016 compliance.
 * We need this additional adapter because some property type changed from {@code URI} to
 * {@code OnlineResource} in the upgrade from ISO 19115:2003 to ISO 19115-1:2014.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class OnlineResourceAdapter extends XmlAdapter<CI_OnlineResource, URI> {
    /**
     * The adapter performing the actual work.
     */
    private static final CI_OnlineResource ADAPTER = new CI_OnlineResource.Since2014();

    /**
     * Wraps the given URI in a {@code <cit:OnlineResource>} element.
     */
    @Override
    public CI_OnlineResource marshal(final URI value) {
        if (value != null) {
            return ADAPTER.marshal(new DefaultOnlineResource(value));
        }
        return null;
    }

    /**
     * Returns a URI from the given {@code <cit:OnlineResource>} element.
     */
    @Override
    public URI unmarshal(final CI_OnlineResource value) throws URISyntaxException {
        if (value != null) {
            final OnlineResource res = ADAPTER.unmarshal(value);
            if (res != null) {
                return res.getLinkage();
            }
        }
        return null;
    }
}
