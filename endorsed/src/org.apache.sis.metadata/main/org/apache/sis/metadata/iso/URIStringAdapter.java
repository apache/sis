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
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.gco.CharSequenceAdapter;
import org.apache.sis.xml.bind.gco.GO_CharacterString;


/**
 * Converts an URI to a {@code <gco:CharacterSequence>} element for ISO 19115-3:2016 compliance.
 * We need this additional adapter because some property type changed from {@code URI}
 * to {@code CharacterSequence} in the upgrade from ISO 19115:2003 to ISO 19115-1:2014.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class URIStringAdapter extends XmlAdapter<GO_CharacterString, URI> {
    /**
     * The adapter performing the actual work.
     */
    private static final CharSequenceAdapter ADAPTER = new CharSequenceAdapter.Since2014();

    /**
     * Wraps the given URI in a {@code <cit:OnlineResource>} element.
     */
    @Override
    public GO_CharacterString marshal(final URI value) {
        if (value != null) {
            return ADAPTER.marshal(value.toString());
        }
        return null;
    }

    /**
     * Returns a URI from the given {@code <cit:OnlineResource>} element.
     */
    @Override
    public URI unmarshal(final GO_CharacterString value) throws URISyntaxException {
        if (value != null) {
            final CharSequence uri = ADAPTER.unmarshal(value);
            if (uri != null) {
                final Context context = Context.current();
                return Context.converter(context).toURI(context, uri.toString());
            }
        }
        return null;
    }
}
