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
package org.apache.sis.storage.hdf.internal;

import java.util.logging.Logger;


/**
 * Constants shared by the classes of the <abbr>HDF5</abbr> reader.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Constants {
    /**
     * Name of the <abbr>HDF5</abbr> format, used as the name of the root node of native metadata.
     */
    public static final String FORMAT_NAME = "HDF5";

    /**
     * The logger for the <abbr>HDF5</abbr> reader.
     */
    public static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.hdf");

    /**
     * Do not allow instantiation of this class.
     */
    private Constants() {
    }
}
