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
 * Units of measurement and miscellaneous utility methods required by Apache SIS.
 * It includes a few mathematical functions, collection classes, I/O utilities,
 * operations on common Java types like Arrays and character strings, logging, and more.
 *
 * <p>Some functionalities provided by this module may overlap some external libraries
 * like Common Logging and Commons collections.
 * Since this is not the purpose of this module to compete with dedicated libraries,
 * this module should be considered as mostly internal to the Apache SIS project.
 * For example any future SIS release may change the collection implementations
 * (how they perform synchronizations, how they handle exceptions, <i>etc.</i>)
 * in order to fit SIS needs.</p>
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.4
 * @since   0.3
 */
module org.apache.sis.util {
    requires java.management;
    requires transitive java.sql;
    requires transitive java.logging;
    requires transitive java.measure;
    requires transitive org.opengis.geoapi;

    provides javax.measure.spi.ServiceProvider
        with org.apache.sis.measure.UnitServices;

    provides org.apache.sis.util.ObjectConverter
        with org.apache.sis.converter.StringConverter.Number,
             org.apache.sis.converter.StringConverter.Byte,
             org.apache.sis.converter.StringConverter.Short,
             org.apache.sis.converter.StringConverter.Integer,
             org.apache.sis.converter.StringConverter.Long,
             org.apache.sis.converter.StringConverter.Float,
             org.apache.sis.converter.StringConverter.Double,
             org.apache.sis.converter.StringConverter.BigInteger,
             org.apache.sis.converter.StringConverter.BigDecimal,
             org.apache.sis.converter.StringConverter.Boolean,
             org.apache.sis.converter.StringConverter.Locale,
             org.apache.sis.converter.StringConverter.Charset,
             org.apache.sis.converter.StringConverter.InternationalString,
             org.apache.sis.converter.StringConverter.File,
             org.apache.sis.converter.StringConverter.Path,
             org.apache.sis.converter.StringConverter.URI,
             org.apache.sis.converter.StringConverter.URL,
             org.apache.sis.converter.StringConverter.Unit,
             org.apache.sis.converter.StringConverter.Angle,
             org.apache.sis.converter.AngleConverter,
             org.apache.sis.converter.AngleConverter.Inverse,
             org.apache.sis.converter.PathConverter.FilePath,
             org.apache.sis.converter.PathConverter.URLPath,
             org.apache.sis.converter.PathConverter.URIPath,
             org.apache.sis.converter.PathConverter.PathURI,
             org.apache.sis.converter.PathConverter.PathURL,
             org.apache.sis.converter.PathConverter.PathFile,
             org.apache.sis.converter.PathConverter.FileURI,
             org.apache.sis.converter.PathConverter.FileURL,
             org.apache.sis.converter.PathConverter.URLFile,
             org.apache.sis.converter.PathConverter.URIFile,
             org.apache.sis.converter.PathConverter.URL_URI,
             org.apache.sis.converter.PathConverter.URI_URL,
             org.apache.sis.converter.DateConverter.Long,
             org.apache.sis.converter.DateConverter.SQL,
             org.apache.sis.converter.DateConverter.Timestamp,
             org.apache.sis.converter.CollectionConverter.List,
             org.apache.sis.converter.CollectionConverter.Set,
             org.apache.sis.converter.FractionConverter,
             org.apache.sis.converter.FractionConverter.FromInteger;

    uses org.apache.sis.util.ObjectConverter;
    uses org.apache.sis.setup.InstallationResources;
    uses org.apache.sis.util.internal.MetadataServices;

    exports org.apache.sis.io;
    exports org.apache.sis.math;
    exports org.apache.sis.measure;
    exports org.apache.sis.setup;
    exports org.apache.sis.util;
    exports org.apache.sis.util.collection;
    exports org.apache.sis.util.logging;

    exports org.apache.sis.util.resources to
            org.apache.sis.metadata,
            org.apache.sis.referencing,
            org.apache.sis.referencing.gazetteer,
            org.apache.sis.feature,
            org.apache.sis.storage,
            org.apache.sis.storage.xml,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.geotiff,
            org.apache.sis.storage.earthobservation,
            org.apache.sis.portrayal,
            org.apache.sis.cloud.aws,
            org.apache.sis.console,
            org.apache.sis.openoffice,
            org.apache.sis.gui,                         // In the "optional" sub-project.
            org.apache.sis.referencing.epsg,
            org.apache.sis.referencing.database;

    exports org.apache.sis.util.internal to
            org.apache.sis.metadata,
            org.apache.sis.referencing,
            org.apache.sis.referencing.gazetteer,
            org.apache.sis.feature,
            org.apache.sis.storage,
            org.apache.sis.storage.xml,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.geotiff,
            org.apache.sis.storage.earthobservation,
            org.apache.sis.portrayal,
            org.apache.sis.cloud.aws,
            org.apache.sis.console,
            org.apache.sis.gui,                         // In the "optional" sub-project.
            org.apache.sis.referencing.epsg,
            org.apache.sis.referencing.database;

    exports org.apache.sis.pending.temporal to
            org.apache.sis.metadata;

    exports org.apache.sis.pending.geoapi.temporal to
            org.apache.sis.metadata,
            org.apache.sis.feature;

    exports org.apache.sis.converter to
            org.apache.sis.metadata,
            org.apache.sis.referencing,
            org.apache.sis.feature,
            org.apache.sis.storage;

    exports org.apache.sis.system to
            org.apache.sis.metadata,
            org.apache.sis.referencing,
            org.apache.sis.referencing.gazetteer,
            org.apache.sis.feature,
            org.apache.sis.storage,
            org.apache.sis.storage.xml,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.shapefile,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.geotiff,
            org.apache.sis.storage.earthobservation,
            org.apache.sis.portrayal,
            org.apache.sis.console,
            org.apache.sis.gui,                         // In the "optional" sub-project.
            org.apache.sis.referencing.database;

    exports org.apache.sis.pending.jdk to
            org.apache.sis.feature,
            org.apache.sis.storage,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.geotiff;
}
