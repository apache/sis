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

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.measure.Unit;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.BufferedGridCoverage;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.AbstractGridCoverageResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.math.NumberType;
import org.apache.sis.storage.hdf.HDF5Store;
import org.apache.sis.storage.hdf.internal.DatasetNode;
import org.apache.sis.storage.hdf.internal.GroupNode;
import org.apache.sis.storage.hdf.Node;
import org.apache.sis.storage.hdf.ObjectReference;
import org.apache.sis.storage.hdf.message.DataspaceMessage;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 * Exposes an HDF-5 group following the CF (Climate and Forecast) conventions as a grid coverage.
 * The multi-dimensional datasets of the group whose dimensions are identified by a
 * {@code coordinates} or {@code DIMENSION_LIST} attribute become the coverage sample dimensions,
 * and the one dimensional datasets of those dimensions define the grid geometry.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CFCoverageResource extends AbstractGridCoverageResource implements StoreResource {

    private final HDF5Store store;
    private final GroupNode group;
    private final List<DatasetNode> variables = new ArrayList<>();
    private final List<SampleDimension> sampleDimensions = new ArrayList<>();
    private final GridGeometry innerGridGeometry;
    private final GridGeometry outerGridGeometry;

    /**
     * Creates a coverage resource for the given CF group.
     *
     * @param store store owning the group.
     * @param group group containing the variables and their dimension datasets.
     * @throws DataStoreException if the group has no usable variable, the variables have
     *         different sizes, or their dimensions can not be read or converted to a CRS.
     * @throws FactoryException if a coordinate reference system could not be created.
     */
    public CFCoverageResource(HDF5Store store, GroupNode group) throws DataStoreException, FactoryException {
        super(null, false);
        this.store = store;
        this.group = group;

        //find usable variables
        int[] dimension = null;
        for (Node n : group.components()) {
            if (n instanceof DatasetNode ds) {
                if (isCoverage(ds, group)) {
                    DataspaceMessage dataspace = ds.getDataspace();
                    if (dimension == null) {
                        dimension = dataspace.getDimensionSizes();
                        variables.add(ds);
                    } else if (!Arrays.equals(dimension, dataspace.getDimensionSizes())) {
                        throw new DataStoreException("Dataset " + ds.getName() + " has a size different from other datasets in this group");
                    } else {
                        variables.add(ds);
                    }
                }
            }
        }

        if (variables.isEmpty()) {
            throw new DataStoreException("No dataset usable as coverage");
        }

        //build sample dimensions
        for (DatasetNode ds : variables) {
            final Map.Entry<Unit, CoordinateReferenceSystem> unit = ds.getAttributeUnit(null);
            final String description = ds.getAttributeDescription(null);
            final SampleDimension.Builder sdb = new SampleDimension.Builder();
            sdb.setName(description == null ? ds.getName() : description);
            if (unit != null) {
                sdb.addQuantitative("data", -Double.MAX_VALUE, Double.MAX_VALUE, unit.getKey());
            }
            sampleDimensions.add(sdb.build());
        }

        // TODO: verify that all variables use the same dimensions, or create one resource per variable (it might be a better choice).
        final DatasetNode firstVariable = variables.get(0);
        List<DatasetNode> dimensionDatasets = searchForDimensions(firstVariable, group);

        if (dimensionDatasets.isEmpty()) throw new DataStoreException("No dimension found for variable " + firstVariable.getName());
        else if (dimensionDatasets.size() != dimension.length) throw new DataStoreException(String.format(
                "Expect %d dimensions, but %d were found",
                dimension.length, dimensionDatasets.size()
        ));

        for (int i = 0; i < dimension.length; i++) {
            final DatasetNode dim = dimensionDatasets.get(i);
            final int[] dimSizes = dim.getDataspace().getDimensionSizes();
            if (dimSizes.length != 1) throw new DataStoreException(String.format(
                    "Dimension %d (%s) is not a 1D variable, but a %dD variable",
                    i, dim.getName(), dimSizes.length
            ));
            if (dimension[i] != dimSizes[0]) throw new DataStoreException(String.format(
                    "Mismatch size for dimension %d. Expected %d, but was %d.",
                    i, dimension[i], dimSizes[0]
            ));
        }

        final List<CoordinateReferenceSystem> axecrs = new ArrayList<>();
        final MathTransform[] axetrs = new MathTransform[dimensionDatasets.size()];
        for (int k = 0; k < axetrs.length; k++) {
            final DatasetNode axeDataset = dimensionDatasets.get(k);
            //build axe transform
            try {
                Object array = axeDataset.readMembers(null);
                final double[] values = new double[Array.getLength(array)];
                for (int i = 0; i < values.length; i++) {
                    values[i] = ((Number)Array.get(array, i)).doubleValue();
                }
                axetrs[k] = MathTransforms.interpolate(null, values);
            } catch (IOException ex) {
                throw new DataStoreException("Failed to read axe values.\n" + ex.getMessage(), ex);
            }
            //build axe coordinate system
            final Map.Entry<Unit, CoordinateReferenceSystem> entry = axeDataset.getAttributeUnit(null);
            if (entry == null || entry.getValue() == null) {
                throw new DataStoreException("Axe dataset for name '" + axeDataset.getName() + "' could not be converted to CRS");
            }
            final CoordinateReferenceSystem crs = entry.getValue();
            if (k > 0) {
                if (crs == DatasetNode.LATITUDE && axecrs.get(k-1) == DatasetNode.LONGITUDE) {
                    axecrs.remove(k-1);
                    axecrs.add(CommonCRS.WGS84.geographic()); //should be the reverse order but crs are going to be reversed after
                } else if (crs == DatasetNode.LONGITUDE && axecrs.get(k-1) == DatasetNode.LATITUDE) {
                    axecrs.remove(k-1);
                    axecrs.add(CommonCRS.WGS84.normalizedGeographic()); //should be the reverse order but crs are going to be reversed after
                } else {
                    axecrs.add(crs);
                }
            } else {
                axecrs.add(crs);
            }
        }

        {//buld grid geometry

            // reverse axes order : time+lat+lon becomes lon+lat+time
            Collections.reverse(axecrs);
            final CoordinateReferenceSystem crs = CRS.compound(axecrs.toArray(CoordinateReferenceSystem[]::new));

            {
                final MathTransform gridToStraightCrs = MathTransforms.compound(axetrs);
                final MatrixSIS matrix = Matrices.createZero(axetrs.length+1, axetrs.length+1);
                for (int i = 0; i < axetrs.length; i++) {
                    matrix.setElement(i, axetrs.length - i - 1, 1);
                }
                matrix.setElement(axetrs.length, axetrs.length, 1);

                final MathTransform reverseTrs = MathTransforms.linear(matrix);
                final MathTransform gridToCrs = MathTransforms.concatenate(gridToStraightCrs, reverseTrs);
                final GridExtent extent = new GridExtent(null, null, ArraysExt.copyAsLongs(dimension), false);
                this.innerGridGeometry = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCrs, crs);
            }

            {
                Collections.reverse(Arrays.asList(axetrs));
                final MathTransform gridToCrs = MathTransforms.compound(axetrs);
                ArraysExt.reverse(dimension);
                final GridExtent extent = new GridExtent(null, null, ArraysExt.copyAsLongs(dimension), false);
                this.outerGridGeometry = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCrs, crs);
            }
        }
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return group.getIdentifier();
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return outerGridGeometry;
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return Collections.unmodifiableList(sampleDimensions);
    }

    @Override
    public GridCoverage read(GridGeometry innerReadGridGeom, int... ints) throws DataStoreException {
        final GridGeometry outerReadGridGeometry;
        if (innerReadGridGeom == null) {
            innerReadGridGeom = this.innerGridGeometry;
            outerReadGridGeometry = this.outerGridGeometry;
        } else {
            innerReadGridGeom = this.innerGridGeometry.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(innerReadGridGeom).build();
            outerReadGridGeometry = this.outerGridGeometry.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(innerReadGridGeom).build();
        }

        if (ints == null || ints.length == 0) {
            ints = new int[sampleDimensions.size()];
            for (int i = 0; i < ints.length; i++) ints[i] = i;
        }

        if (ints.length != 1) {
            throw new DataStoreException("Multiple band coverage not supported yet");
        }

        final GridExtent innerExtent = innerReadGridGeom.getExtent();

        final DatasetNode ds = variables.get(ints[0]);
        try {
            Object datas = ds.readMembers(innerExtent);
            return new BufferedGridCoverage(outerReadGridGeometry, sampleDimensions, toDataBuffer(datas, true));
        } catch (IOException ex) {
            throw new DataStoreException("Cannot extract values from dataset " + ds.getName(), ex);
        }

    }

    private static DataBuffer toDataBuffer(Object array, boolean reverse) throws DataStoreException {
        final int[] dimensions = getDimension(array);
        final int[] stepSizes = new int[dimensions.length];
        long sizeLong = 1;
        if (reverse) {
            stepSizes[dimensions.length-1] = 1;
            for (int i = dimensions.length-2; i >= 0; i--) {
                stepSizes[i] = Math.multiplyExact(stepSizes[i+1], dimensions[i+1]);
            }
            sizeLong = Math.multiplyExact(dimensions[0],stepSizes[0]);
        } else {
            stepSizes[0] = 1;
            for (int i = 0; i < dimensions.length; i++) {
                sizeLong = Math.multiplyExact(sizeLong, dimensions[i]);
                if (i > 0) {
                    stepSizes[i] = Math.multiplyExact(stepSizes[i-1], dimensions[i-1]);
                }
            }
        }

        final int size = Math.toIntExact(sizeLong);

        final DataBuffer db;

        final Class<?> componentType = NumberType.primitiveToWrapper(getComponentType(array));
        if (Double.class.equals(componentType)) {
            db = new DataBufferDouble(size);
        } else if (Float.class.equals(componentType)) {
            db = new DataBufferFloat(size);
        } else if (Integer.class.equals(componentType)) {
            db = new DataBufferInt(size);
        } else if (Short.class.equals(componentType)) {
            db = new DataBufferShort(size);
        } else {
            throw new DataStoreException("Unsupported array type " + componentType.getSimpleName());
        }

        final int[] coordinate = new int[dimensions.length];
        fill(array, dimensions, stepSizes, coordinate, 0, db);
        return db;
    }

    private static void fill(Object array, int[] dimensions, int[] stepSizes, int[] coordinate, int index, DataBuffer target) {
        if (index == dimensions.length -1) {
            for (int i = 0; i < dimensions[index]; i++) {
                coordinate[index] = i;
                int offset = offset(coordinate, stepSizes);
                Number num = (Number) Array.get(array, i);
                target.setElemDouble(offset, num.doubleValue());
            }
        } else {
            for (int i = 0; i < dimensions[index]; i++) {
                coordinate[index] = i;
                fill(Array.get(array, i), dimensions, stepSizes, coordinate, index+1, target);
            }
        }
    }

    private static int offset(int[] coordinate, int[] stepSize) {
        int offset = 0;
        for (int i = 0; i< stepSize.length; i++) {
            offset += coordinate[i] * stepSize[i];
        }
        return offset;
    }

    private static int[] getDimension(Object array) {
        final List<Integer> lst = new ArrayList<>();
        Object cdt = array;
        while (cdt.getClass().isArray()) {
            lst.add(Array.getLength(cdt));
            cdt = Array.get(cdt, 0);
        }

        final int[] dimension = new int[lst.size()];
        for (int i = 0; i < dimension.length; i++) {
            dimension[i] = lst.get(i);
        }
        return dimension;
    }

    private static Class<?> getComponentType(Object array) {
        Class<?> type = array.getClass();
        while (type.getComponentType() != null) type = type.getComponentType();
        return type;
    }

    /**
     * Try to identify input dataset dimensions by analyzing its "coordinates" or "DIMENSION_LIST" attribute.
     *
     * @param target The dataset to find dimensions for.
     * @param parent The group where dimension datasets are searched.
     * @return An empty list if neither "coordinates" nor "DIMENSION_LIST" attribute is found, or they're empty.
     *         Otherwise, return the list of datasets referenced by dimension list.
     * @throws DataStoreException If one of the attribute contains unsupported value or type.
     */
    private static List<DatasetNode> searchForDimensions(DatasetNode target, GroupNode parent) throws DataStoreException {
        final Map<String, Object> attributes = target.getAttributes();
        Object coordinates = attributes.getOrDefault("coordinates", attributes.get(HDF5DimensionScaleAPI.ATT_DIMENSION_LIST));
        if (coordinates == null) return Collections.emptyList();

        if (coordinates instanceof String s) coordinates = s.split("\\s+");
        if (coordinates instanceof Object[] objs) coordinates = Arrays.asList(objs);
        if (coordinates instanceof List<?> objs) {
            List<DatasetNode> dimensions = new ArrayList<>(objs.size());
            for (Object obj : objs) {
                if (obj instanceof String name && parent.getComponent(name) instanceof DatasetNode dim) dimensions.add(dim);
                else if (obj instanceof ObjectReference ref && parent.findNode(ref.getAddress()) instanceof DatasetNode ds) dimensions.add(ds);
                else throw new DataStoreException("Cannot find dimension Dataset for coordinate "+obj);
            }
            return dimensions;
        }

        throw new DataStoreException("Unsupported coordinates/dimension list. Expect a list of names or dataset references.");
    }

    /**
     * Returns whether the given dataset is a CF coverage variable.
     * This is the case if it has more than one dimension and its dimension attributes,
     * if any, reference existing datasets.
     *
     * @param dataset dataset to test.
     * @param parent group where dimension datasets are searched.
     * @return whether the given dataset is a CF coverage variable
     */
    private static boolean isCoverage(DatasetNode dataset, GroupNode parent) {
        if (dataset.getDataspace().getDimensionSizes().length > 1) {
            try {
                final List<DatasetNode> dimensions = searchForDimensions(dataset, parent);
            } catch (DataStoreException ex) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public DataStore getOriginator() {
        return store;
    }

    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        return store.getFileSet();
    }
}
