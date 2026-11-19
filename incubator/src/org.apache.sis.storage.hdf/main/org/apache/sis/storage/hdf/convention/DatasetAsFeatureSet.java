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

import org.apache.sis.storage.hdf.internal.Constants;
import java.io.IOException;
import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.storage.hdf.HDF5Store;
import org.apache.sis.storage.hdf.internal.DatasetNode;
import org.apache.sis.storage.hdf.datatype.Compound;
import org.apache.sis.storage.hdf.datatype.DataType;
import org.apache.sis.storage.hdf.message.DataspaceMessage;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;

/**
 * View multiple HDF Datasets as a FeatureSet.
 * Each element of the dataset becomes a feature, identified by its index in the dataspace.
 * Each dataset becomes a feature attribute.
 * Compound datatype members become feature attributes, and datasets listed in the CF
 * {@code coordinates} attribute (limited to longitude and latitude) are combined into a point geometry.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class DatasetAsFeatureSet extends AbstractFeatureSet implements StoreResource{

    private static final GeometryFactory GF = new GeometryFactory();

    private final HDF5Store store;
    private final DatasetNode[] datasets;
    private final GenericName name;
    private final Map<String,DatasetNode> index = new LinkedHashMap<>();

    //featureset
    private FeatureType featureType;
    private boolean is1D;
    private boolean isEmpty;

    //geometry type info
    private CoordinateReferenceSystem crs;
    private DatasetNode[] coordinates;

    /**
     * Creates a feature set view of the given dataset.
     *
     * @param store store owning the dataset.
     * @param datasets dataset to expose as features.
     */
    public DatasetAsFeatureSet(HDF5Store store, GenericName name, DatasetNode ... datasets) {
        super(null, false);
        this.store = store;
        this.name = name;
        this.datasets = datasets;

        for (DatasetNode ds : datasets) {
            index.put(ds.getName(), ds);
        }

    }

    /**
     * Returns the datasets viewed as a feature set.
     *
     * @return the dataset viewed as a feature set
     */
    public DatasetNode[] getDatasets() {
        return datasets.clone();
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        getType();
        if (crs != null) {
            final GeneralEnvelope env = new GeneralEnvelope(crs);
            for (int i = 0; i < coordinates.length; i++) {
                final NumberRange range = coordinates[i].getValueRange();
                final double scale = getScale(coordinates[i]);
                env.setRange(i, range.getMinDouble(true) * scale, range.getMaxDouble(true) * scale);
            }
            return Optional.of(env);
        }
        return Optional.empty();
    }

    @Override
    public synchronized FeatureType getType() throws DataStoreException {
        if (featureType != null) return featureType;

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName(name);

        {
            //check if we have CF related coordinates
            final DatasetNode dataset = datasets[0];
            final String coordinates = (String) dataset.getAttributes().getOrDefault("coordinates", null);
            if (coordinates != null) {
                final String[] parts = coordinates.split(" ");


                this.coordinates = new DatasetNode[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    this.coordinates[i] = (DatasetNode) dataset.getParent().resolve(parts[i].trim());
                }

                if (parts.length == 2) {
                    if ("longitude".equals(this.coordinates[0].getName())) {
                        crs = CommonCRS.WGS84.normalizedGeographic();
                    } else {
                        crs = CommonCRS.WGS84.geographic();
                    }
                    crs = AbstractCRS.castOrCopy(crs).forConvention(AxesConvention.POSITIVE_RANGE);

                    ftb.addAttribute(Point.class).setName("coordinates").setCRS(crs).addRole(AttributeRole.DEFAULT_GEOMETRY);
                } else {
                    Constants.LOGGER.log(Level.WARNING, "Dataset " + dataset.getName() +" has " + parts.length + "dimensions, it is not supported yet");
                    this.coordinates = null;
                }
            }
        }

        for (DatasetNode dataset : datasets) {
            final DataType datatype = dataset.getDataType();
            final DataspaceMessage dataspace = dataset.getDataspace();

            Class valueClass = datatype.getValueClass();

            //check if we have a CF scale factor
            double scaleFactor = getScale(dataset);
            if (scaleFactor != 1.0) {
                valueClass = double.class;
            }

            final int[] dimensionSizes = dataspace.getDimensionSizes();
            if (dimensionSizes == null || dimensionSizes.length < 1) {
                isEmpty = true;
                is1D = true;
            } else {
                for (int i = 0; i < dimensionSizes.length; i++) {
                    if (dimensionSizes[i] == 0) {
                        isEmpty = true;
                        break;
                    }
                }
                is1D = dimensionSizes.length == 1;
            }

            if (datatype instanceof Compound cmp) {
                throw new IllegalStateException("Compound type not supported in aggregated datasets");
            } else {
                ftb.addAttribute(valueClass).setName(dataset.getName());
            }
        }
        featureType = ftb.build();

        return featureType;
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        if (isEmpty) return Stream.empty();

        //limited to 1D dataset
        final int[] dimensionSizes = datasets[0].getDataspace().getDimensionSizes();
        final GridExtent area = datasets[0].getDataspace().getDimensionExtent();

        //read by blocks of 1000
        final int blockSize = 1000;
        final long[] sub = new long[dimensionSizes.length];
        Arrays.fill(sub, blockSize);
        final GridExtent points = area.subsample(sub);

        Stream<GridExtent> streamExtent = points.latticePointStream(parallel).map(new Function<long[],GridExtent>() {
            @Override
            public GridExtent apply(long[] value) {
                final long[] low = new long[dimensionSizes.length];
                final long[] high = new long[dimensionSizes.length];
                for (int i=0;i<low.length;i++) {
                    low[i] = value[i] * blockSize;
                    high[i] = (value[i]+1) * blockSize -1; //inclusive
                }
                //ensure we do not got out of data range
                return area.intersect(new GridExtent(null, low, high, true));
            }
        });

        return streamExtent.flatMap(new Function<GridExtent, Stream<Feature>>() {
            @Override
            public Stream<Feature> apply(GridExtent t) {
                try {
                    return toFeatures(getType(), t);
                } catch (IOException | DataStoreException | TransformException ex) {
                    throw new BackingStoreException(ex.getMessage(), ex);
                }
            }
        });
    }

    private Stream<Feature> toFeatures(FeatureType type, GridExtent extent) throws DataStoreException, IOException, TransformException {

        //read coordinates
        Object coord0 = null;
        Object coord1 = null;
        if (crs != null) {
            coord0 = readScaled(coordinates[0],extent);
            coord1 = readScaled(coordinates[1],extent);
        }

        final Map<String,Object> rawDatas = new LinkedHashMap<>();
        for (PropertyType pt : type.getProperties(true)) {
            if (pt instanceof AttributeType at) {
                final String dsName = at.getName().toString();
                final DatasetNode ds = index.get(dsName);
                if (ds != null) {
                    rawDatas.put(dsName, readScaled(ds, extent));
                }
            }
        }

        final long[] low = extent.getLow().getCoordinateValues();
        final List<Feature> features = new ArrayList<>();

        final Iterator<long[]> iterator = extent.latticePointStream(false).iterator();
        while (iterator.hasNext()) {
            final long[] location = iterator.next();

            final Feature feature = type.newInstance();
            //rebuild geometry
            if (crs != null) {
                Number x = (Number) Array.get(coord0, Math.toIntExact(location[0] - low[0]));
                Number y = (Number) Array.get(coord1, Math.toIntExact(location[0] - low[0]));
                final Point pt = GF.createPoint(new CoordinateXY(x.doubleValue(), y.doubleValue()));
                pt.setUserData(crs);
                feature.setPropertyValue("coordinates", pt);
            }
            //read properties
            for (Entry<String,Object> entry : rawDatas.entrySet()) {
                Object rawRecord = Array.get(entry.getValue(), Math.toIntExact(location[0] - low[0]));
                if (!is1D) {
                    for (int i = 1; i < low.length; i++) {
                        rawRecord = Array.get(rawRecord, Math.toIntExact(location[i] - low[i]));
                    }
                }
                feature.setPropertyValue(entry.getKey(), rawRecord);
            }
            features.add(feature);
        }

        return features.stream();
    }

    @Override
    public DataStore getOriginator() {
        return store;
    }

    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        return store.getFileSet();
    }

    private static Object readScaled(DatasetNode dataset, GridExtent extent, int ... compoundindexes) throws IOException, DataStoreException {
        Object data = dataset.readMembers(extent, compoundindexes);
        final double scaleFactor = getScale(dataset);
        if (scaleFactor != 1.0) {
            double[] array = new double[Array.getLength(data)];
            for (int i = 0; i < array.length; i++) {
                array[i] = ((Number)Array.get(data, i)).doubleValue() * scaleFactor;
            }
            data = array;
        }

        return data;
    }

    private static double getScale(DatasetNode dataset) {
        //check if we have a CF scale factor
        double scaleFactor = 1.0;
        final Object cdt = dataset.getAttributes().getOrDefault("scale_factor", null);
        if (cdt instanceof Number n) {
            scaleFactor = n.doubleValue();
        } else if (cdt != null && cdt.getClass().isArray()) {
            final Object av = Array.get(cdt, 0);
            if (av instanceof Number n) {
                scaleFactor = n.doubleValue();
            }
        }
        return scaleFactor;
    }
}
