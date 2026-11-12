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
package org.apache.sis.storage.hdf.convention;

/**
 * Dimension Scale API is used to reference columns of a dataset to another dataset
 * which in turn describes the dimension.
 *
 * @see <a href="https://support.hdfgroup.org/HDF5/doc/HL/H5DS_Spec.pdf">HDF5 Dimension Scale Specification (PDF)</a>
 * @see <a href="https://confluence.hdfgroup.org/display/HDF5/Dimension+Scales">Dimension Scales</a>
 * @see <a href="https://docs.hdfgroup.org/hdf5/develop/group___h5_d_s.html">Dimension Scales (H5DS) API reference</a>
 * @author Johann Sorel (Geomatys)
 */
public final class HDF5DimensionScaleAPI {

    /** Dataset attribute holding, for each dimension, references to the dimension scales attached to it. */
    public static final String ATT_DIMENSION_LIST = "DIMENSION_LIST";
    /** Dimension scale attribute listing the datasets and dimension indexes the scale is attached to. */
    public static final String ATT_REFERENCE_LIST = "REFERENCE_LIST";
    /** Attribute identifying the kind of object, {@link #VALUE_DIMENSION_SCALE} for a dimension scale. */
    public static final String ATT_CLASS = "CLASS";

    /** Value of the {@link #ATT_CLASS} attribute identifying a dimension scale. */
    public static final String VALUE_DIMENSION_SCALE = "DIMENSION_SCALE";

    private HDF5DimensionScaleAPI(){}

}
