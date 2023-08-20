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
 * Japanese profile of netCDF store.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.0
 */
module org.apache.sis.profile.japan {
    requires transitive org.apache.sis.storage.netcdf;
    requires cdm.core;

    provides org.apache.sis.internal.netcdf.Convention
        with org.apache.sis.profile.japan.netcdf.GCOM_C,
             org.apache.sis.profile.japan.netcdf.GCOM_W;

    provides ucar.nc2.dataset.spi.CoordSystemBuilderFactory
        with org.apache.sis.profile.japan.netcdf.FactoryForUCAR;

    exports org.apache.sis.profile.japan;
}
