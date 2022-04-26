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
package org.apache.sis.internal.storage.esri;

import java.awt.image.BandedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;


/**
 * Kind of pixel layout in a raw raster file.
 * They indirectly determine the sample model.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
enum RawRasterLayout {
    /**
     * Band interleaved by line. There is no direct equivalent in Java2D sample models.
     * This is the default value.
     */
    BIL,

    /**
     * Band interleaved by pixel. This is equivalent to {@link PixelInterleavedSampleModel}.
     */
    BIP,

    /**
     * Band sequential. This is equivalent to {@link BandedSampleModel}.
     */
    BSQ;
}
