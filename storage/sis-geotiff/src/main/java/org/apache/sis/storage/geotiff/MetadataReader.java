/*
 * Copyright 2016 haonguyen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.geotiff;

import static com.sun.corba.se.impl.ior.iiop.JavaSerializationComponent.singleton;
import static java.util.Collections.singleton;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.identification.Identification;

/**
 *
 * @author haonguyen
 */
public class MetadataReader {

    private DefaultDataIdentification identication;
     DefaultCitation citation ;
    DefaultMetadata metadata;

    private Citation getTitle() {
        //final DefaultCitation citation = new DefaultCitation();

        citation.setTitle(new DefaultInternationalString("dfdfs"));
        return citation;
    }

    private Metadata read() {

        // final DefaultCitation citation = citation.getTitle();
        metadata.setMetadataProfiles(singleton(getTitle()));
        return metadata;
    }

    public static void main(String[] args) {
        MetadataReader a = new MetadataReader();

        System.out.println(a.read());
    }
}
