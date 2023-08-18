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

/**
 * Implementations of metadata derived from ISO 19115.
 * This module provides both an implementation of the metadata interfaces defined in GeoAPI,
 * and a framework for handling those metadata through Java reflection.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.4
 * @since   0.3
 */
module org.apache.sis.metadata {
    /*
     * Dependencies. Only the ones that appear in public API should be transitive.
     */
    requires java.sql;
    requires java.naming;
    requires transitive jakarta.xml.bind;
    requires transitive org.apache.sis.util;

    /*
     * Services needed or implemented by this module.
     */
    provides org.opengis.util.NameFactory
        with org.apache.sis.util.iso.DefaultNameFactory;

    provides org.apache.sis.internal.jaxb.TypeRegistration
        with org.apache.sis.internal.metadata.MetadataTypes;

    provides org.apache.sis.internal.util.MetadataServices
        with org.apache.sis.internal.metadata.ServicesForUtility;

    uses org.apache.sis.internal.metadata.ReferencingServices;
    uses org.apache.sis.internal.metadata.sql.Initializer;
    uses org.apache.sis.internal.jaxb.AdapterReplacement;
    uses org.apache.sis.internal.jaxb.TypeRegistration;
    uses org.apache.sis.internal.geoapi.temporal.TemporalFactory;

    /*
     * Public API open to everyone.
     */
    exports org.apache.sis.metadata;
    exports org.apache.sis.metadata.iso;
    exports org.apache.sis.metadata.iso.acquisition;
    exports org.apache.sis.metadata.iso.citation;
    exports org.apache.sis.metadata.iso.constraint;
    exports org.apache.sis.metadata.iso.content;
    exports org.apache.sis.metadata.iso.distribution;
    exports org.apache.sis.metadata.iso.extent;
    exports org.apache.sis.metadata.iso.identification;
    exports org.apache.sis.metadata.iso.lineage;
    exports org.apache.sis.metadata.iso.maintenance;
    exports org.apache.sis.metadata.iso.quality;
    exports org.apache.sis.metadata.iso.spatial;
    exports org.apache.sis.metadata.sql;
    exports org.apache.sis.util.iso;
    exports org.apache.sis.xml;

    /*
     * Internal API open only to other Apache SIS modules.
     */
    exports org.apache.sis.internal.metadata to
            org.apache.sis.referencing,
            org.apache.sis.feature,
            org.apache.sis.storage,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.geotiff;

    exports org.apache.sis.internal.jaxb to
            org.apache.sis.referencing,
            org.apache.sis.storage.xml,
            org.apache.sis.portrayal,
            org.apache.sis.profile.france;

    exports org.apache.sis.internal.jaxb.gco to
            org.apache.sis.referencing,
            org.apache.sis.portrayal;

    exports org.apache.sis.internal.jaxb.gcx to
            org.glassfish.jaxb.runtime,         // TODO: need to export to Jakarta only.
            org.glassfish.jaxb.core,            // TODO: need to export to Jakarta only.
            jakarta.xml.bind;                   // Seems ignored.

    exports org.apache.sis.internal.jaxb.lan to
            org.glassfish.jaxb.runtime,         // TODO: need to export to Jakarta only.
            org.glassfish.jaxb.core,            // TODO: need to export to Jakarta only.
            jakarta.xml.bind;                   // Seems ignored.

    exports org.apache.sis.internal.jaxb.cat to
            org.glassfish.jaxb.runtime,         // TODO: need to export to Jakarta only.
            org.glassfish.jaxb.core,            // TODO: need to export to Jakarta only.
            jakarta.xml.bind;                   // Seems ignored.

    exports org.apache.sis.internal.jaxb.gml to
            org.apache.sis.referencing,
            org.apache.sis.storage.xml,
            org.glassfish.jaxb.runtime,         // TODO: need to export to Jakarta only.
            org.glassfish.jaxb.core,            // TODO: need to export to Jakarta only.
            jakarta.xml.bind;                   // Seems ignored.

    exports org.apache.sis.internal.jaxb.gmi to
            org.glassfish.jaxb.core,            // TODO: need to export to Jakarta only.
            jakarta.xml.bind;                   // Seems ignored.

    exports org.apache.sis.internal.jaxb.gts to
            org.glassfish.jaxb.core,            // TODO: need to export to Jakarta only.
            jakarta.xml.bind;                   // Seems ignored.

    exports org.apache.sis.internal.jaxb.code to
            org.glassfish.jaxb.runtime,         // TODO: need to export to Jakarta only.
            org.glassfish.jaxb.core,            // TODO: need to export to Jakarta only.
            jakarta.xml.bind;                   // Seems ignored.

    exports org.apache.sis.internal.jaxb.metadata to
            org.apache.sis.referencing,
            org.apache.sis.storage,
            org.apache.sis.profile.france,
            org.glassfish.jaxb.runtime,         // TODO: need to export to Jakarta only.
            org.glassfish.jaxb.core,            // TODO: need to export to Jakarta only.
            jakarta.xml.bind;                   // Seems ignored.

    exports org.apache.sis.internal.jaxb.geometry to
            org.glassfish.jaxb.core,            // TODO: need to export to Jakarta only.
            jakarta.xml.bind;                   // Seems ignored.

    exports org.apache.sis.internal.jaxb.metadata.replace to
            org.apache.sis.referencing,
            org.apache.sis.profile.france,
            org.glassfish.jaxb.runtime,         // For access to beforeUnmarshal(…).
            org.glassfish.jaxb.core,            // For access to various classes.
            jakarta.xml.bind;                   // Seems ignored.

    exports org.apache.sis.internal.metadata.sql to
            org.apache.sis.referencing,
            org.apache.sis.storage.sql;

    exports org.apache.sis.internal.simple to
            org.apache.sis.referencing,
            org.apache.sis.feature,
            org.apache.sis.storage,
            org.apache.sis.storage.xml;

    exports org.apache.sis.internal.xml to
            org.apache.sis.referencing,
            org.apache.sis.storage,
            org.apache.sis.storage.xml,
            org.apache.sis.gui;                 // In the "optional" sub-project.

    exports org.apache.sis.internal.geoapi.evolution to
            org.apache.sis.referencing,
            org.opengis.geoapi;

    // For instantiation of new CodeList values by reflection.
    opens org.apache.sis.internal.geoapi.evolution to org.opengis.geoapi;

    /*
     * Allow JAXB to use reflection for marshalling and
     * unmarshalling Apache SIS objects in XML documents.
     *
     * Module names of some implementations:
     *   - com.sun.xml.bind
     *   - org.glassfish.jaxb.runtime
     *
     * Opening to implementation is a temporary hack, until
     * we find why opening to Jakarta only is not sufficient.
     */
    opens org.apache.sis.util.iso                    to jakarta.xml.bind, org.glassfish.jaxb.core, org.glassfish.jaxb.runtime;
    opens org.apache.sis.metadata.iso                to jakarta.xml.bind;
    opens org.apache.sis.metadata.iso.citation       to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens org.apache.sis.metadata.iso.constraint     to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens org.apache.sis.metadata.iso.identification to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens org.apache.sis.metadata.iso.quality        to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens org.apache.sis.metadata.iso.lineage        to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens org.apache.sis.metadata.iso.content        to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens org.apache.sis.metadata.iso.distribution   to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens org.apache.sis.metadata.iso.maintenance    to jakarta.xml.bind, org.glassfish.jaxb.runtime;

    opens org.apache.sis.internal.jaxb.gco to jakarta.xml.bind;
    opens org.apache.sis.internal.jaxb.gml to jakarta.xml.bind;
}
