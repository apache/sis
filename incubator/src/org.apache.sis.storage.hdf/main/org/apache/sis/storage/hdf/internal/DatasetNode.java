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

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import static java.util.Collections.singletonMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.measure.Unit;
import javax.measure.format.MeasurementParseException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.math.Statistics;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.crs.DefaultEngineeringCRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.cs.DefaultLinearCS;
import org.apache.sis.referencing.cs.DefaultTimeCS;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.referencing.datum.DefaultTemporalDatum;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.Dataset;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.util.ArraysExt;
import static org.apache.sis.util.ArraysExt.swap;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.storage.hdf.base.DenseStorage;
import org.apache.sis.storage.hdf.base.IOStructure;
import org.apache.sis.storage.hdf.base.ObjectHeader;
import org.apache.sis.storage.hdf.heap.LocalHeap;
import org.apache.sis.storage.hdf.io.SegmentsSeekableByteChannel;
import org.apache.sis.storage.hdf.message.ExternalFileListMessage;
import static org.apache.sis.storage.hdf.internal.GroupNode.prettyPrint;
import org.apache.sis.storage.hdf.chunk.BTreeV1ChunkIndex;
import org.apache.sis.storage.hdf.chunk.BTreeV2ChunkIndex;
import org.apache.sis.storage.hdf.chunk.Chunk;
import org.apache.sis.storage.hdf.chunk.ChunkGrid;
import org.apache.sis.storage.hdf.chunk.ChunkIndex;
import org.apache.sis.storage.hdf.chunk.ExtensibleArrayChunkIndex;
import org.apache.sis.storage.hdf.chunk.FixedArrayChunkIndex;
import org.apache.sis.storage.hdf.chunk.ImplicitChunkIndex;
import org.apache.sis.storage.hdf.chunk.SingleChunkIndex;
import org.apache.sis.storage.hdf.datatype.DataType;
import org.apache.sis.storage.hdf.io.ChunkSeekableByteChannel;
import org.apache.sis.storage.hdf.io.ConstantSeekableByteChannel;
import org.apache.sis.storage.hdf.io.Connector;
import org.apache.sis.storage.hdf.io.HDF5ChannelDataInput;
import org.apache.sis.storage.hdf.io.HDF5DataInput;
import org.apache.sis.storage.hdf.message.AttributeInfoMessage;
import org.apache.sis.storage.hdf.message.AttributeMessage;
import org.apache.sis.storage.hdf.message.DataLayoutMessage;
import org.apache.sis.storage.hdf.message.DataspaceMessage;
import org.apache.sis.storage.hdf.message.DatatypeMessage;
import org.apache.sis.storage.hdf.message.FillValueMessage;
import org.apache.sis.storage.hdf.message.FilterPipelineMessage;
import org.apache.sis.storage.hdf.message.FilterPipelineMessage.FilterDescription;
import org.apache.sis.storage.hdf.message.Message;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.EngineeringDatum;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 * A dataset is an object header that contains messages that describe the
 * datatype, dataspace, layout, filters, external files, fill value, and other
 * elements with the layout message pointing to either a raw data chunk or to
 * a B-tree that points to raw data chunks.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DatasetNode extends AbstractResource implements Dataset {

    /** Engineering CRS with a unitless north axis, for datasets whose CF {@code standard_name} is {@code latitude}. */
    public static final CoordinateReferenceSystem LATITUDE;
    /** Engineering CRS with a unitless east axis, for datasets whose CF {@code standard_name} is {@code longitude}. */
    public static final CoordinateReferenceSystem LONGITUDE;

    static {
        {
            final EngineeringDatum datum = new DefaultEngineeringDatum(Collections.singletonMap("name", "Datum"));
            final CoordinateSystemAxis axis = new DefaultCoordinateSystemAxis(Collections.singletonMap("name", "Axis"), "unnamed", AxisDirection.NORTH, Units.UNITY);
            final CoordinateSystem cs = new DefaultLinearCS(Collections.singletonMap("name", "CS"), axis);
            LATITUDE = new DefaultEngineeringCRS(Collections.singletonMap("name", "LATITUDE"), datum, null, cs);
        }
        {
            final EngineeringDatum datum = new DefaultEngineeringDatum(Collections.singletonMap("name", "Datum"));
            final CoordinateSystemAxis axis = new DefaultCoordinateSystemAxis(Collections.singletonMap("name", "Axis"), "unnamed", AxisDirection.EAST, Units.UNITY);
            final CoordinateSystem cs = new DefaultLinearCS(Collections.singletonMap("name", "CS"), axis);
            LONGITUDE = new DefaultEngineeringCRS(Collections.singletonMap("name", "LONGITUDE"), datum, null, cs);
        }
    }

    private final GroupNode parent;
    private final Connector connector;

    private final String name;
    private final GenericName genericName;
    private final long address;

    //parsed values
    private final Map<String,Object> attributes = new HashMap<>();
    private DataType datatype;
    private DataspaceMessage dataspace;
    private FillValueMessage fillValue;
    private DataLayoutMessage.Layout layout;
    private FilterPipelineMessage filter;
    private ExternalFileListMessage externalFiles;
    private NumberRange valueRange; //computed only once

    //decoding informations
    private final int cellByteSize;
    private ChunkGrid chunkGrid;
    private ChunkIndex chunkIndex;

    /**
     * Chunks already read and de-filtered, by chunk address.
     * <p>
     * Without it every read decodes again the chunks it touches, so reading a
     * dataset tile by tile inflates the same chunks over and over. References are
     * soft rather than weak on purpose : a weakly reachable entry is cleared at
     * the next garbage collection, which for a cache means almost never hitting,
     * whereas a soft one survives until memory is actually needed. The virtual
     * machine clears them all before throwing an out of memory error.
     */
    private final Map<Long,Reference<byte[]>> decodedChunks = new ConcurrentHashMap<>();
    /**
     * Receives the cache references whose chunk has been reclaimed, so that the
     * map does not keep growing with empty entries.
     */
    private final ReferenceQueue<byte[]> reclaimedChunks = new ReferenceQueue<>();

    /** A cached chunk, remembering its key so a reclaimed entry can be removed. */
    private static final class ChunkReference extends SoftReference<byte[]> {
        private final Long address;

        ChunkReference(Long address, byte[] datas, ReferenceQueue<byte[]> queue) {
            super(datas, queue);
            this.address = address;
        }
    }

    /**
     * Create a dataset from the address of its object header.
     *
     * @param parent parent group, not {@code null}
     * @param connector file connector, not {@code null}
     * @param objectHeaderAddress address of this dataset object header
     * @param name name of this dataset within its parent
     * @throws IOException if an I/O error occurred, or the object header has no datatype or dataspace message.
     * @throws DataStoreException if the object header is invalid or unsupported.
     */
    public DatasetNode(GroupNode parent, Connector connector, long objectHeaderAddress, String name) throws IOException, DataStoreException {
        this(parent, connector, objectHeaderAddress, name, Nodes.readHeader(connector, objectHeaderAddress));
    }

    /**
     * Create a dataset from an already decoded object header, avoiding a second read.
     *
     * @param parent parent group, not {@code null}
     * @param connector file connector, not {@code null}
     * @param objectHeaderAddress address of this dataset object header
     * @param name name of this dataset within its parent
     * @param header decoded object header of this dataset
     * @throws IOException if an I/O error occurred, or the object header has no datatype or dataspace message.
     * @throws DataStoreException if the dense attributes are invalid or unsupported.
     * @see Nodes#create(GroupNode, Connector, long, String)
     */
    DatasetNode(GroupNode parent, Connector connector, long objectHeaderAddress, String name, ObjectHeader header) throws IOException, DataStoreException {
        super(null, false);
        this.parent = parent;
        this.connector = connector;
        this.name = name;
        this.address = objectHeaderAddress;

        //extract properties
        AttributeInfoMessage attributeInfo = null;
        for (Message message : header.getMessages()) {
            if (message instanceof AttributeMessage cdt) {
                attributes.put(cdt.getName(), cdt.getValue());
            } else if (message instanceof AttributeInfoMessage cdt) {
                attributeInfo = cdt;
            } else if (message instanceof DataspaceMessage cdt) {
                dataspace = cdt;
            } else if (message instanceof DatatypeMessage cdt) {
                datatype = cdt.getDataType();
            } else if (message instanceof FillValueMessage cdt) {
                fillValue = cdt;
            } else if (message instanceof FilterPipelineMessage cdt) {
                filter = cdt;
            } else if (message instanceof DataLayoutMessage cdt) {
                layout = cdt.getLayout();
            } else if (message instanceof ExternalFileListMessage cdt) {
                externalFiles = cdt;
            } else {
                /*
                Unknown or merely informative messages are ignored : refusing to
                open a file because of a single unrecognized optional message
                would be worse than reading what we do understand.
                */
                Constants.LOGGER.log(Level.FINE, () -> "Ignored message "
                        + message.getClass().getSimpleName() + " in dataset " + name);
            }
        }

        if (attributeInfo != null && connector.isDefinedOffset(attributeInfo.getFractalHeapAddress())) {
            /*
            Dense attribute storage : the Attribute messages live in a fractal heap
            indexed by a version 2 B-tree rather than in this object header.
            */
            try (final HDF5DataInput channel = connector.createChannel()) {
                DenseStorage.readAttributes(channel, attributeInfo.getFractalHeapAddress(),
                        attributeInfo.getAttributeNameV2btreeAddress(), attributes, name);
            }
        }

        if (datatype == null) {
            throw new IOException("Dataset " + name + " has no datatype message.");
        }
        if (dataspace == null) {
            throw new IOException("Dataset " + name + " has no dataspace message.");
        }

        cellByteSize = datatype.getByteSize();
        genericName = Nodes.createName(this);
    }

    private static long[] buildCellSizes(int cellByteSize, int[] dimensionSizes){
        ArraysExt.reverse(dimensionSizes);
        long[] dimensionByteSize = new long[dimensionSizes.length];
        if (dimensionSizes.length > 0){
            dimensionByteSize[0] = cellByteSize;
            for (int i = 1; i < dimensionSizes.length; i++) {
                dimensionByteSize[i] = dimensionByteSize[i-1] * dimensionSizes[i-1];
            }
            reverse(dimensionByteSize);
        }
        return dimensionByteSize;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public GroupNode getParent() {
        return parent;
    }

    @Override
    public long getAddress() {
        return address;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(genericName);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Extract attribute description concatenating CF {@code standard_name} and {@code long_name}.
     *
     * @param attributeName compound member name, or {@code null} if datatype is not compound.
     * @return the description, or {@code null} if none of these attributes is defined.
     */
    public String getAttributeDescription(String attributeName) {
        final Object standardName = attributes.get(attributeName == null ? "standard_name" : attributeName + ":standard_name");
        final Object longName = attributes.get(attributeName == null ? "long_name" : attributeName + ":long_name");
        final StringBuilder description = new StringBuilder();
        if (standardName != null) {
            description.append(standardName);
        }
        if (longName != null) {
            if (!description.isEmpty()) {
                description.append(" : ");
            }
            description.append(longName);
        }
        if (!description.isEmpty()) {
            return description.toString();
        }
        return null;
    }

    /**
     * Extract the unit of the values from the CF {@code units} attribute.
     * A unit of the form {@code "<unit> since <date>"} produces a temporal CRS,
     * and a {@code standard_name} of {@code latitude} or {@code longitude} produces
     * the {@link #LATITUDE} or {@link #LONGITUDE} CRS.
     *
     * @param attributeName compound member name, or {@code null} if datatype is not compound.
     * @return the unit and the CRS (possibly {@code null}), or {@code null} if no unit could be determined.
     * @throws FactoryException if the temporal CRS could not be created.
     */
    public Entry<Unit,CoordinateReferenceSystem> getAttributeUnit(String attributeName) throws FactoryException {
        final Object units = attributes.get(attributeName == null ? "units" : (attributeName + ":units"));
        final Object standardName = attributes.get(attributeName == null ? "standard_name" : (attributeName + ":standard_name"));
        final Object shortName = attributes.get(attributeName == null ? "short_name" : (attributeName + ":short_name"));
        final Object longName = attributes.get(attributeName == null ? "long_name" : (attributeName + ":long_name"));
        final String unitStr = units == null ? null : String.valueOf(units);
        Unit unit = null;
        CoordinateReferenceSystem crs = null;
        if (unitStr != null) {
            try {
                unit = Units.valueOf(unitStr);
            } catch (MeasurementParseException ex) {
                int idx = unitStr.indexOf("since");
                if (idx > 0) {
                    try {
                        final String baseUnitStr = unitStr.substring(0, idx).trim();
                        unit = Units.valueOf(baseUnitStr);
                        final String originStr = unitStr.substring(idx+5).trim();
                        final Instant epoch = LenientDateFormat.parseInstantUTC(originStr);

                        final Map<String,?> props = singletonMap(NAME_KEY, new NamedIdentifier(null, "attributeName"));
                        final TemporalDatum datum = new DefaultTemporalDatum(props, epoch);
                        final Map<String,Object> properties = new HashMap<>();
                        final String crsName = attributeName == null ? getName() : attributeName;
                        properties.put(TemporalCRS.NAME_KEY, new NamedIdentifier(Names.createLocalName(null, null, crsName)));
                        final Map<String,?> cs   = singletonMap(NAME_KEY, new NamedIdentifier(null, Vocabulary.formatInternational(Vocabulary.Keys.Temporal)));
                        final Map<String,?> axis = singletonMap(NAME_KEY, new NamedIdentifier(null, Vocabulary.formatInternational(Vocabulary.Keys.Time)));
                        final DefaultTimeCS timeCs = new DefaultTimeCS(cs, new DefaultCoordinateSystemAxis(axis, "t", AxisDirection.FUTURE, unit));
                        crs = new DefaultTemporalCRS(properties, datum, null, timeCs);

                    } catch (MeasurementParseException | DateTimeException e) {
                        Constants.LOGGER.log(Level.WARNING, "Failed to parse unit {0}", unitStr);
                    }
                } else {
                    Constants.LOGGER.log(Level.WARNING, "Failed to parse unit {0}", unitStr);
                }
            }
        }

        if (crs == null) {
            if (standardName != null) {
                final String stdName = String.valueOf(standardName);
                if ("latitude".equalsIgnoreCase(stdName)) {
                    crs = LATITUDE;
                    unit = crs.getCoordinateSystem().getAxis(0).getUnit();
                } else if ("longitude".equalsIgnoreCase(stdName)) {
                    crs = LONGITUDE;
                    unit = crs.getCoordinateSystem().getAxis(0).getUnit();
                }
            }
        }

        if (unit != null) {
            return new AbstractMap.SimpleImmutableEntry<>(unit, crs);
        }
        return null;
    }

    /**
     * Returns the datatype of the dataset elements, never {@code null}.
     *
     * @return the datatype of the dataset elements, never {@code null}
     */
    public DataType getDataType() {
        return datatype;
    }

    /**
     * Returns the Java type of the dataset elements.
     *
     * @return the Java type of the dataset elements.
     */
    @Override
    public Class<?> getValueClass() {
        return datatype.getValueClass();
    }

    /**
     * Returns the extent of this dataset in dataspace coordinates.
     *
     * @return the extent of this dataset in dataspace coordinates.
     */
    @Override
    public GridExtent getExtent() {
        return dataspace.getDimensionExtent();
    }

    /**
     * Returns the Dataspace message giving the dimension sizes, never {@code null}.
     *
     * @return the Dataspace message giving the dimension sizes, never {@code null}
     */
    public DataspaceMessage getDataspace() {
        return dataspace;
    }

    /**
     * Returns the Fill Value message for elements never written, or {@code null} if none.
     *
     * @return the Fill Value message for elements never written, or {@code null} if none
     */
    public FillValueMessage getFillValue() {
        return fillValue;
    }

    /**
     * Returns the raw data layout (compact, contiguous, chunked or virtual), or {@code null} if none.
     *
     * @return the raw data layout (compact, contiguous, chunked or virtual), or {@code null} if none
     */
    public DataLayoutMessage.Layout getLayout() {
        return layout;
    }

    /**
     * Returns the Filter Pipeline message, or {@code null} if the raw data is not filtered.
     * Currently, only datasets using chunked data storage use the filter
     * pipeline on their raw data.
     *
     * @return the Filter Pipeline message, or {@code null} if the raw data is not filtered
     */
    public FilterPipelineMessage getFilter() {
        return filter;
    }

    /**
     * Returns the range of the dataset values, inclusive.
     * Computed on first call by reading the whole dataset by blocks of 1000 elements
     * per dimension, ignoring the blocks which fail to be read.
     *
     * @return the range of the dataset values, inclusive
     */
    public synchronized NumberRange getValueRange() {
        if (valueRange != null) return valueRange;

        //limited to 1D dataset
        final int[] dimensionSizes = getDataspace().getDimensionSizes();
        final GridExtent area = getDataspace().getDimensionExtent();

        //read by blocks of 1000
        final int blockSize = 1000;
        final long[] sub = new long[dimensionSizes.length];
        Arrays.fill(sub, blockSize);
        final GridExtent points = area.subsample(sub);

        Stream<GridExtent> streamExtent = points.latticePointStream(false).map(new Function<long[],GridExtent>() {
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
        Stream<NumberRange> s = streamExtent.map(new Function<GridExtent,NumberRange>() {
            @Override
            public NumberRange apply(GridExtent t) {
                try {
                    return getValueRange(t);
                } catch (DataStoreException | IOException ex) {
                    //do nothing
                    return null;
                }
            }
        });

        Optional<NumberRange> reduce = s.filter(Objects::nonNull).reduce(new BinaryOperator<NumberRange>() {
            @Override
            public NumberRange apply(NumberRange t, NumberRange u) {
                return t.unionAny(u);
            }
        });
        valueRange = reduce.get();
        return valueRange;
    }

    private NumberRange getValueRange(GridExtent extent) throws DataStoreException, IOException {
        final Object rawData = readMembers(extent);

        final Statistics stats = new Statistics("v");

        for (int i = 0, n = Array.getLength(rawData); i < n; i++) {
            final Object valueI = Array.get(rawData, i);
            if (valueI instanceof Number v) {
                stats.accept(v.doubleValue());
            } else if (valueI.getClass().isArray()) {
                for (int j = 0, m = Array.getLength(valueI); j < m; j++) {
                    final Object valueJ = Array.get(valueI, j);
                    stats.accept(((Number)valueJ).doubleValue());
                }
            }
        }
        return NumberRange.create(stats.minimum(), true, stats.maximum(), true);
    }

    /**
     * Returns all the values in the given extent.
     *
     * @param extent region to read in dataspace coordinates, or {@code null} for the whole dataset.
     * @return the values in the given extent
     * @throws DataStoreException if the extent is not contained in the dataspace,
     *         if the data layout or storage is not supported, or if an I/O error occurred.
     */
    @Override
    public Object read(GridExtent extent) throws DataStoreException {
        try {
            return readMembers(extent);
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Returns the values in the given extent, optionally restricted to some members of a compound datatype.
     * The result is a multi-dimensional Java array of {@link DataType#getValueClass()} elements
     * with one level per dimension, or a single value for a scalar dataspace.
     *
     * @param extent region to read in dataspace coordinates, or {@code null} for the whole dataset.
     * @param compoundindexes indexes of the compound members to read, or none for all members.
     * @return the values in the given extent
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the extent is not contained in the dataspace,
     *         or the data layout or storage is not supported.
     */
    public Object readMembers(GridExtent extent, int ... compoundindexes) throws IOException, DataStoreException {

        //build read extent
        final int[] dimensionSizes = dataspace.getDimensionSizes();
        if (extent == null) {
            extent = dataspace.getDimensionExtent();
        } else if (!dataspace.getDimensionExtent().intersect(extent).equals(extent)){
            throw new DataStoreException("Requested extent " + extent + " is not contained in " + dataspace.getDimensionExtent());
        }

        //build channel
        try (HDF5DataInput channel = connector.createChannel()) {

            //build the fill value
            byte[] fill = new byte[1];
            if (fillValue != null && fillValue.getFillValue() != null && fillValue.getFillValue().length > 0) {
                fill = fillValue.getFillValue();
            }

            if (dimensionSizes.length == 0) {
                //scalar value
                if (layout instanceof DataLayoutMessage.Compact cdt) {
                    channel.seek(cdt.rawDataAddress);
                } else if (layout instanceof DataLayoutMessage.Contiguous cdt) {
                    if (connector.isUndefinedOffset(cdt.address)) {
                        throw new DataStoreException("Scalar dataset " + name + " has no allocated storage.");
                    }
                    channel.seek(cdt.address);
                } else {
                    /*
                    A chunked or virtual layout can not describe a scalar dataspace;
                    falling through would decode at whatever position the stream
                    happens to be at and silently return garbage.
                    */
                    throw new DataStoreException("Layout " + layout.getClass().getSimpleName()
                            + " is not supported for the scalar dataset " + name);
                }
                return datatype.readData(channel, compoundindexes);
            } else {
                final List<FilterDescription> filters = new ArrayList<>();
                if (filter != null) {
                    filters.addAll(filter.getFilters());
                }

                if (externalFiles != null) {
                    /*
                    The raw data lives outside this file, as the concatenation of
                    ranges of the files listed by the External Data Files message.
                    Filters do not apply to that storage.
                    */
                    final int[] dimensions = new int[extent.getDimension()];
                    for (int i = 0; i < dimensions.length; i++) {
                        dimensions[i] = (int) extent.getSize(i);
                    }
                    final Object results = java.lang.reflect.Array.newInstance(datatype.getValueClass(), dimensions);
                    final GridExtent queryExtent = extent;
                    appendChunkDatas(results, () -> openExternalFiles(channel), channel,
                            dataspace.getDimensionExtent(), queryExtent, compoundindexes);
                    return results;
                }

                final ChunkGrid grid = getChunkGrid();
                final ChunkIndex index = getChunkIndex();

                //read datas
                final int[] dimensions = new int[extent.getDimension()];
                for (int i = 0; i< dimensions.length; i++) {
                    dimensions[i] = (int) extent.getSize(i);
                }
                final Object results = java.lang.reflect.Array.newInstance(datatype.getValueClass(), dimensions);

                final GridExtent queryExtent = extent;
                final byte[] fillPattern = fill;
                final long chunkByteSize = grid.getChunkElementCount() * cellByteSize;

                for (long[] scaled : grid.intersecting(queryExtent)) {
                    final Chunk chunk = index.getChunk(channel, scaled);
                    if (chunk == null || connector.isUndefinedOffset(chunk.address)) {
                        /*
                        Storage was never allocated for this chunk, its elements
                        take the dataset fill value.
                        */
                        appendChunkDatas(results, () -> new HDF5ChannelDataInput(
                                new ChannelDataInput("", new ConstantSeekableByteChannel(chunkByteSize, fillPattern),
                                        ByteBuffer.allocate(4096), false)), channel,
                                grid.extentOf(scaled), queryExtent, compoundindexes);
                    } else if (!chunk.filtered || filters.isEmpty()) {
                        /*
                        Raw bytes, read straight from the file with no copy.
                        */
                        appendChunkDatas(results, () -> {
                            channel.seek(chunk.address);
                            return channel;
                        }, channel, chunk.offset != null ? chunk.offset : grid.extentOf(scaled),
                                queryExtent, compoundindexes);
                    } else {
                        /*
                        Filtered chunk, decoded once and kept for the next read
                        that touches it.
                        */
                        appendChunkDatas(results,
                                () -> channel.subChannel(getDecodedChunk(channel, chunk, filters)), channel,
                                chunk.offset != null ? chunk.offset : grid.extentOf(scaled),
                                queryExtent, compoundindexes);
                    }
                }
                return results;
            }
        }
    }

    /**
     * Returns the decoded bytes of a filtered chunk.
     * They are read and de-filtered only if not already cached.
     *
     * @param channel channel on the file, not null
     * @param chunk chunk to decode, its address being the cache key
     * @param filters filter pipeline of this dataset
     * @return the decoded bytes of a filtered chunk
     * @throws IOException if an error occurred while reading or decoding the chunk.
     */
    private byte[] getDecodedChunk(HDF5DataInput channel, Chunk chunk,
            List<FilterDescription> filters) throws IOException {
        //drop the entries whose chunk has been reclaimed since the last read
        Reference<? extends byte[]> reclaimed;
        while ((reclaimed = reclaimedChunks.poll()) != null) {
            decodedChunks.remove(((ChunkReference) reclaimed).address, reclaimed);
        }

        final Long key = chunk.address;
        final Reference<byte[]> reference = decodedChunks.get(key);
        if (reference != null) {
            final byte[] cached = reference.get();
            if (cached != null) {
                chunk.uncompressedSize = cached.length;
                return cached;
            }
            //reclaimed between the purge above and now
            decodedChunks.remove(key, reference);
        }

        final byte[] decoded = ChunkSeekableByteChannel.decode(chunk, channel, filters);
        decodedChunks.put(key, new ChunkReference(key, decoded, reclaimedChunks));
        return decoded;
    }

    /**
     * Forget every decoded chunk kept in memory. Reading them again decodes them
     * again, so this only frees memory.
     */
    public void clearChunkCache() {
        decodedChunks.clear();
    }

    /**
     * Returns a single channel over the external data files of this dataset.
     * <p>
     * File names live in a local heap of their own and are resolved relative to
     * the directory of this HDF5 file.
     *
     * @param channel channel on the file.
     * @return a single channel over the external data files of this dataset
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the local heap is invalid or a file location is not supported.
     */
    private HDF5DataInput openExternalFiles(HDF5DataInput channel) throws IOException, DataStoreException {
        final LocalHeap heap;
        channel.mark();
        try {
            channel.seek(externalFiles.getHeapAddress());
            heap = (LocalHeap) IOStructure.loadIdentifiedObject(channel);
        } finally {
            channel.reset();
        }

        final Path parent = connector.getPath().toAbsolutePath().getParent();
        final List<SegmentsSeekableByteChannel.Segment> segments = new ArrayList<>();
        for (ExternalFileListMessage.ExternalFileListSlot slot : externalFiles.getSlots()) {
            final String name = heap.getString(channel, slot.getNameOffset());
            segments.add(new SegmentsSeekableByteChannel.Segment(
                    resolveExternalFile(parent, name), slot.getOffsetInFile(), slot.getSizeInFile()));
        }
        return new HDF5ChannelDataInput(new ChannelDataInput("",
                new SegmentsSeekableByteChannel(segments), ByteBuffer.allocate(4096), false));
    }

    /**
     * Returns the path of an external data file.
     * External file names are URLs, of which only the file scheme and plain
     * relative paths make sense here.
     *
     * @param parent directory of this HDF5 file.
     * @param name external file name, as stored.
     * @return the path of an external data file
     * @throws DataStoreException if the name is a URL with an unsupported scheme.
     */
    private static Path resolveExternalFile(Path parent, String name) throws DataStoreException {
        String path = name;
        if (path.startsWith("file://")) {
            path = path.substring("file://".length());
            //an empty host means localhost
            if (path.startsWith("localhost/")) {
                path = path.substring("localhost".length());
            }
        } else if (path.startsWith("file:")) {
            path = path.substring("file:".length());
        } else if (path.contains("://")) {
            throw new DataStoreException("Unsupported external data file location " + name);
        }
        final Path resolved = Path.of(path);
        return resolved.isAbsolute() ? resolved : parent.resolve(resolved);
    }

    /**
     * Returns the chunk grid of this dataset, computed on first call.
     * Compact and contiguous layouts are described as a grid holding a single chunk
     * covering the whole dataset, so that all four layouts share one read path.
     *
     * @return the chunk grid of this dataset, computed on first call
     */
    private synchronized ChunkGrid getChunkGrid() {
        if (chunkGrid == null) {
            final int[] dimensionSizes = dataspace.getDimensionSizes();
            final int[] chunkDimensions;
            if (layout instanceof DataLayoutMessage.Chunked cdt) {
                chunkDimensions = cdt.dimensionSizes;
            } else {
                chunkDimensions = new int[dimensionSizes.length];
                for (int i = 0; i < chunkDimensions.length; i++) {
                    //a zero sized dimension would make the grid degenerate
                    chunkDimensions[i] = Math.max(1, dimensionSizes[i]);
                }
            }
            chunkGrid = new ChunkGrid(dimensionSizes, chunkDimensions);
        }
        return chunkGrid;
    }

    private synchronized ChunkIndex getChunkIndex() throws IOException, DataStoreException {
        if (chunkIndex == null) {
            final ChunkGrid grid = getChunkGrid();
            if (layout instanceof DataLayoutMessage.Compact cdt) {
                final Chunk chunk = new Chunk();
                chunk.address = cdt.rawDataAddress;
                chunk.size = cdt.size;
                chunk.uncompressedSize = cdt.size;
                chunk.offset = grid.extentOf(new long[grid.getDimension()]);
                chunkIndex = new SingleChunkIndex(chunk);
            } else if (layout instanceof DataLayoutMessage.Contiguous cdt) {
                if (connector.isUndefinedOffset(cdt.address)) {
                    //storage not allocated, everything is the fill value
                    chunkIndex = new SingleChunkIndex(null);
                } else {
                    final Chunk chunk = new Chunk();
                    chunk.address = cdt.address;
                    chunk.size = cdt.size;
                    chunk.uncompressedSize = cdt.size;
                    chunk.offset = grid.extentOf(new long[grid.getDimension()]);
                    chunkIndex = new SingleChunkIndex(chunk);
                }
            } else if (layout instanceof DataLayoutMessage.Chunked cdt) {
                chunkIndex = createChunkedIndex(cdt, grid);
            } else if (layout instanceof DataLayoutMessage.Virtual) {
                throw new IOException("Virtual datasets are not decoded yet for dataset " + name);
            } else {
                throw new IOException("Unexpected layout " + layout.getClass().getSimpleName());
            }
        }
        return chunkIndex;
    }

    private ChunkIndex createChunkedIndex(DataLayoutMessage.Chunked cdt, ChunkGrid grid) throws IOException, DataStoreException {
        if (connector.isUndefinedOffset(cdt.address)) {
            //storage not allocated, everything is the fill value
            return new SingleChunkIndex(null);
        }
        final long chunkByteSize = cdt.datasetElementSize > 0
                ? cdt.getChunkByteSize()
                : grid.getChunkElementCount() * cellByteSize;

        switch (cdt.chunkIndexingType) {
            case DataLayoutMessage.Chunked.INDEX_BTREE_V1: {
                /*
                A version 1 B-tree chunk key holds one offset per dataset dimension
                plus a trailing zero, so it must be given exactly the rank. Version
                1 and 2 layout messages declare one dimension more than the rank.
                */
                final int[] chunkDimensions = Arrays.copyOf(cdt.dimensionSizes, grid.getDimension());
                return new BTreeV1ChunkIndex(cdt.address, chunkDimensions, grid, filter != null);
            }
            case DataLayoutMessage.Chunked.INDEX_SINGLE_CHUNK: {
                final Chunk chunk = new Chunk();
                chunk.address = cdt.address;
                chunk.offset = grid.extentOf(new long[grid.getDimension()]);
                if ((cdt.flags & DataLayoutMessage.Chunked.FLAG_SINGLE_INDEX_WITH_FILTER) != 0) {
                    chunk.size = cdt.singleChunkSize;
                    chunk.filterMask = cdt.singleChunkFilterMask;
                    chunk.filtered = true;
                } else {
                    chunk.size = chunkByteSize;
                    chunk.uncompressedSize = chunkByteSize;
                    chunk.filtered = false;
                }
                return new SingleChunkIndex(chunk);
            }
            case DataLayoutMessage.Chunked.INDEX_IMPLICIT: {
                return new ImplicitChunkIndex(cdt.address, chunkByteSize, grid);
            }
            case DataLayoutMessage.Chunked.INDEX_FIXED_ARRAY: {
                return new FixedArrayChunkIndex(cdt.address, grid, chunkByteSize);
            }
            case DataLayoutMessage.Chunked.INDEX_EXTENSIBLE_ARRAY: {
                return new ExtensibleArrayChunkIndex(cdt.address, grid, chunkByteSize);
            }
            case DataLayoutMessage.Chunked.INDEX_BTREE_V2: {
                return new BTreeV2ChunkIndex(cdt.address, grid, chunkByteSize);
            }
            default:
                throw new IOException("Unexpected chunk indexing type " + cdt.chunkIndexingType);
        }
    }

    /**
     * Copies the part of a chunk intersecting the query extent in the result array.
     *
     * @param results array of the whole query extent.
     * @param chunkChannel supplier of a channel positioned on the chunk data.
     * @param shared channel reading this file, which the caller owns and which
     *        must not be closed here
     * @param chunkExtent chunk extent, in dataspace coordinates.
     * @param queryExtent queried extent, in dataspace coordinates.
     * @param compoundindexes indexes of the compound members to read.
     * @throws IOException if an I/O error occurred.
     */
    private void appendChunkDatas(Object results, Callable<HDF5DataInput> chunkChannel, HDF5DataInput shared,
            GridExtent chunkExtent, GridExtent queryExtent, int ... compoundindexes) throws IOException {


        final GridExtent intersection = safeIntersection(chunkExtent, queryExtent);
        if (intersection == null) {
            Constants.LOGGER.log(Level.FINER, () -> String.format("Chunk extent does not intersect queried one:%nChunk: %s%nQueried: %s%n", chunkExtent, queryExtent));
            return;
        }

        final long[] chunkTranslate = chunkExtent.getLow().getCoordinateValues();

        final long[] intersectionlow = intersection.getLow().getCoordinateValues();
        final int[] dimensions = new int[intersectionlow.length];
        final int[] fulldimensions = new int[intersectionlow.length];
        final long[] intersectionToQueryTranslate = new long[intersectionlow.length];
        for (int i = 0; i< dimensions.length; i++) {
            dimensions[i] = (int) intersection.getSize(i);
            if (dimensions[i] <= 0) return;
            fulldimensions[i] = (int) chunkExtent.getSize(i);
            intersectionToQueryTranslate[i] = intersectionlow[i] - queryExtent.getLow(i);
        }
        HDF5DataInput dfi = null;
        try {
            dfi = chunkChannel.call();
            Object chunkDatas = readChunkDatas(dfi, buildCellSizes(cellByteSize, fulldimensions), chunkTranslate, intersectionlow, dimensions, 0, compoundindexes);
            //copy datas in result array
            copy(chunkDatas, results, intersectionToQueryTranslate);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        } finally {
            /*
            Close the channel unless it is the shared one reading this file, which
            the caller owns. A dedicated channel may hold file handles of its own,
            as the external data files one does.
            */
            if (dfi != null && dfi != shared) {
                dfi.close();
            }
        }
    }

    private Object readChunkDatas(HDF5DataInput chunkChannel, final long[] dimensionByteSize, final long[] chunkLow, final long[] intersectionLow, final int[] intersectionSize, int dimIdx, int ... compoundindexes) throws IOException, DataStoreException {
        final long basePosition = chunkChannel.getStreamPosition();
        Object values;
        if (dimIdx == intersectionLow.length - 1) {
            //a strip
            chunkChannel.seek(basePosition + (cellByteSize * (intersectionLow[dimIdx] - chunkLow[dimIdx])));
            values = datatype.readData(chunkChannel, intersectionSize[dimIdx], compoundindexes);
        } else {
            //an array of strips
            final int[] sizes = new int[intersectionSize.length - dimIdx];
            sizes[0] = intersectionSize[dimIdx];
            values = java.lang.reflect.Array.newInstance(datatype.getValueClass(), sizes);
            for (int k = 0; k < intersectionSize[dimIdx]; k++) {
                chunkChannel.seek(basePosition + ((intersectionLow[dimIdx] - chunkLow[dimIdx]) + k) * dimensionByteSize[dimIdx]);
                final Object strip = readChunkDatas(chunkChannel, dimensionByteSize, chunkLow, intersectionLow, intersectionSize, dimIdx+1, compoundindexes);
                java.lang.reflect.Array.set(values, k, strip);
            }
        }
        return values;
    }

    private static void copy(Object source, Object target, long[] offset) {

        if (offset.length == 1) {
            int length = Array.getLength(source);
            if (length == 0) return;
            System.arraycopy(source, 0, target, (int) offset[0], length);
        } else {
            int nb = Array.getLength(source);
            final long[] subOffset = Arrays.copyOfRange(offset, 1, offset.length);
            for (int i = 0; i < nb; i++) {
                final Object subSource = Array.get(source, i);
                final Object subTarget = Array.get(target, (int) (i + offset[0]));
                copy(subSource, subTarget,subOffset);
            }
        }
    }

    @Override
    public String toString() {
        final String name = getName();
        final StringBuilder sb = new StringBuilder(name);
        sb.append("[HDF5-Dataset]");
        for (Map.Entry entry : getAttributes().entrySet()) {
            Object v = prettyPrint(this, entry.getValue());
            sb.append('\n').append("@").append(entry.getKey()).append(" = ").append(v);
        }
        sb.append("\n").append(".layout = ").append(layout.getClass().getSimpleName());
        sb.append("\n").append(".dataspace = ").append(Arrays.toString(dataspace.getDimensionSizes()));
        sb.append("\n").append(".datatype = ").append(datatype.getByteSize()).append("bytes, ").append(datatype);

        Object value = dataPreview(this, 16);
        sb.append("\nValues subset : ").append(value);

        return sb.toString();
    }

    /**
     * Returns a string representation of the first values, or the error message if they could not be read.
     *
     * @param dataset dataset to preview.
     * @param maxArraySize maximum number of values per dimension.
     * @return a string representation of the first values, or the error message if they could not be read
     */
    private static String dataPreview(DatasetNode dataset, int maxArraySize) {
        Object value;
        try {
            int[] dimensions = dataset.getDataspace().getDimensionSizes();
            for (int i : dimensions) {
                if (i == 0) {
                    return "<empty, dimension has a size of 0>";
                }
            }
            final long[] starts = new long[dimensions.length];
            final long[] ends = new long[dimensions.length];
            for (int i=0;i<dimensions.length;i++) {
                ends[i] = Math.min(dimensions[i], maxArraySize);
            }
            final GridExtent extent = new GridExtent(null, starts, ends, false);
            value = dataset.readMembers(extent);
        } catch (Exception e) {
            value = e.getMessage();
        }

        return toString(value, maxArraySize);
    }

    private static String toString(Object value, int maxArraySize) {
        if (value == null) {
            return "null";
        } else if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            length = Math.min(length, maxArraySize);
            Object[] arr = (Object[]) Array.newInstance(Object.class, length);
            for (int i=0;i<length;i++) {
                Object v = Array.get(value, i);
                Array.set(arr, i, toString(v, maxArraySize));
            }
            return Arrays.toString(arr);
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Reverses the order of elements in the given array.
     * This operation is performed in-place.
     * If the given array is {@code null}, then this method does nothing.
     *
     * @param  values  the array in which to reverse the order of elements, or {@code null} if none.
     */
    public static void reverse(final long[] values) {
        if (values != null) {
            int i = values.length >>> 1;
            int j = i + (values.length & 1);
            while (--i >= 0) {
                swap(values, i, j++);
            }
        }
    }

    /**
     * Returns the intersection of the given extents, or {@code null} if they do <em>not</em> intersect.
     *
     * @param first first extent.
     * @param second second extent, of same dimension.
     * @return the intersection of the given extents, or {@code null} if they do <em>not</em> intersect
     */
    private static GridExtent safeIntersection(GridExtent first, GridExtent second) {
        var dim = first.getDimension();
        assert dim == second.getDimension();
        final long[] newLow = new long[dim];
        final long[] newHigh = new long[dim];
        for (int i = 0; i < dim ; i++) {
            var low = Math.max(first.getLow(i), second.getLow(i));
            var high = Math.min(first.getHigh(i), second.getHigh(i));
            if (high < low) return null;
            newLow[i] = low;
            newHigh[i] = high;
        }
        return new GridExtent(null, newLow, newHigh, true);
    }
}
