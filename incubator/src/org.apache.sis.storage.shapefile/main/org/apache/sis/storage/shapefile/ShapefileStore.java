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
package org.apache.sis.storage.shapefile;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.privy.AttributeConvention;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.filter.Optimization;
import org.apache.sis.filter.privy.FunctionNames;
import org.apache.sis.filter.privy.ListingPropertyVisitor;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.storage.WritableFeatureSet;
import org.apache.sis.storage.shapefile.cpg.CpgFiles;
import org.apache.sis.storage.shapefile.dbf.DBFField;
import org.apache.sis.storage.shapefile.dbf.DBFHeader;
import org.apache.sis.storage.shapefile.dbf.DBFReader;
import org.apache.sis.storage.shapefile.dbf.DBFWriter;
import org.apache.sis.storage.shapefile.shp.ShapeGeometryEncoder;
import org.apache.sis.storage.shapefile.shp.ShapeHeader;
import org.apache.sis.storage.shapefile.shp.ShapeReader;
import org.apache.sis.storage.shapefile.shp.ShapeRecord;
import org.apache.sis.storage.shapefile.shp.ShapeType;
import org.apache.sis.storage.shapefile.shp.ShapeWriter;
import org.apache.sis.storage.shapefile.shx.IndexWriter;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.BackingStoreException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.util.CodeList;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.Literal;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.ValueReference;
import org.apache.sis.geometry.wrapper.*;


/**
 * Shapefile datastore.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ShapefileStore extends DataStore implements WritableFeatureSet {

    private static final String GEOMETRY_NAME = "geometry";
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.shapefile");

    private final Path shpPath;
    private final ShpFiles files;
    private final Charset userDefinedCharSet;
    private final ZoneId timezone;

    /**
     * Internal class to inherit AbstractFeatureSet.
     */
    private final AsFeatureSet featureSetView = new AsFeatureSet(null, true, null);

    /**
     * Lock to control read and write operations.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Construct store from given path.
     *
     * @param path path to .shp file
     */
    public ShapefileStore(Path path) {
        this.shpPath = path;
        this.userDefinedCharSet = null;
        this.timezone = null;
        this.files = new ShpFiles(shpPath);
    }

    /**
     * Construct store from given connector.
     *
     * @param cnx not null
     * @throws IllegalArgumentException if connector could not provide a valid Path instance
     * @throws DataStoreException if connector could not provide a valid Path instance
     */
    public ShapefileStore(StorageConnector cnx) throws IllegalArgumentException, DataStoreException {
        this.shpPath = cnx.getStorageAs(Path.class);
        this.userDefinedCharSet = cnx.getOption(OptionKey.ENCODING);
        this.timezone = cnx.getOption(OptionKey.TIMEZONE);
        this.files = new ShpFiles(shpPath);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final Parameters parameters = Parameters.castOrWrap(ShapefileProvider.PARAMETERS_DESCRIPTOR.createValue());
        parameters.parameter(ShapefileProvider.LOCATION).setValue(shpPath.toUri());
        return Optional.of(parameters);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void close() throws DataStoreException {
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return featureSetView.getIdentifier();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Metadata getMetadata() throws DataStoreException {
        return featureSetView.getMetadata();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureType getType() throws DataStoreException {
        return featureSetView.getType();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public FeatureSet subset(Query query) throws UnsupportedQueryException, DataStoreException {
        return featureSetView.subset(query);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        return featureSetView.features(parallel);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return featureSetView.getEnvelope();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void updateType(FeatureType featureType) throws DataStoreException {
        featureSetView.updateType(featureType);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void add(Iterator<? extends Feature> iterator) throws DataStoreException {
        featureSetView.add(iterator);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeIf(Predicate<? super Feature> predicate) throws DataStoreException {
        featureSetView.removeIf(predicate);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void replaceIf(Predicate<? super Feature> predicate, UnaryOperator<Feature> unaryOperator) throws DataStoreException {
        featureSetView.replaceIf(predicate, unaryOperator);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        return featureSetView.getFileSet();
    }

    private class AsFeatureSet extends AbstractFeatureSet implements WritableFeatureSet {

        private final Rectangle2D.Double filter;
        private final Set<String> dbfProperties;
        private final boolean readShp;
        private Charset charset;

        /**
         * Extracted informations
         */
        private int[] dbfPropertiesIndex;
        private ShapeHeader shpHeader;
        private DBFHeader dbfHeader;
        /**
         * Name of the field used as identifier, may be null.
         */
        private String idField;
        private CoordinateReferenceSystem crs;
        private FeatureType type;

        /**
         * @param filter optional shape filter, must be in data CRS
         * @param properties dbf properties to read, null for all properties
         */
        private AsFeatureSet(Rectangle2D.Double filter, boolean readShp, Set<String> properties) {
            super(null);
            this.readShp = readShp;
            this.filter = filter;
            this.dbfProperties = properties;
        }

        /**
         * @return true if this view reads all data without any filter.
         */
        private boolean isDefaultView() {
            return filter == null && dbfProperties == null && readShp;
        }

        @Override
        public synchronized FeatureType getType() throws DataStoreException {
            if (type == null) {
                if (!Files.isRegularFile(shpPath)) {
                    throw new DataStoreException("Shape files do not exist. Update FeatureType first to initialize this empty datastore");
                }

                final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
                ftb.setName(files.baseName);

                if (readShp) {
                    //read shp header to obtain geometry type
                    final Class geometryClass;
                    try (final ShapeReader reader = new ShapeReader(ShpFiles.openReadChannel(shpPath), filter)) {
                        final ShapeHeader header = reader.getHeader();
                        this.shpHeader = header;
                        geometryClass = ShapeGeometryEncoder.getEncoder(header.shapeType).getValueClass();
                    } catch (IOException ex) {
                        throw new DataStoreException("Failed to parse shape file header.", ex);
                    }

                    //read prj file for projection
                    final Path prjFile = files.getPrj(false);
                    if (prjFile != null) {
                        try {
                            crs = CRS.fromWKT(Files.readString(prjFile, StandardCharsets.UTF_8));
                        } catch (IOException | FactoryException ex) {
                            throw new DataStoreException("Failed to parse prj file.", ex);
                        }
                    } else {
                        //shapefile often do not have a .prj, mostly those are in CRS:84.
                        //we do not raise an error otherwise we would not be able to read a lot of data.
                        crs = CommonCRS.WGS84.normalizedGeographic();
                    }

                    ftb.addAttribute(geometryClass).setName(GEOMETRY_NAME).setCRS(crs).addRole(AttributeRole.DEFAULT_GEOMETRY);
                }

                //read cpg for dbf file charset
                final Path cpgFile = files.getCpg(false);
                if (cpgFile != null) {
                    try (final SeekableByteChannel channel = Files.newByteChannel(cpgFile, StandardOpenOption.READ)) {
                        charset = CpgFiles.read(channel);
                    } catch (IOException ex) {
                        throw new DataStoreException("Failed to parse cpg file.", ex);
                    }
                } else {
                    charset = StandardCharsets.UTF_8;
                }

                //read dbf for attributes
                final Path dbfFile = files.getDbf(false);
                if (dbfFile != null) {
                    try (DBFReader reader = new DBFReader(ShpFiles.openReadChannel(dbfFile), charset, timezone, null)) {
                        final DBFHeader header = reader.getHeader();
                        this.dbfHeader = header;
                        boolean hasId = false;

                        if (dbfProperties == null) {
                            dbfPropertiesIndex = new int[header.fields.length];
                        } else {
                            dbfPropertiesIndex = new int[dbfProperties.size()];
                        }

                        int idx=0;
                        for (int i = 0; i < header.fields.length; i++) {
                            final DBFField field = header.fields[i];
                            if (dbfProperties != null && !dbfProperties.contains(field.fieldName)) {
                                //skip unwanted fields
                                continue;
                            }
                            dbfPropertiesIndex[idx] = i;
                            idx++;

                            final AttributeTypeBuilder atb = ftb.addAttribute(field.valueClass).setName(field.fieldName);
                            //no official but 'id' field is common
                            if (!hasId && "id".equalsIgnoreCase(field.fieldName) || "identifier".equalsIgnoreCase(field.fieldName)) {
                                idField = field.fieldName;
                                atb.addRole(AttributeRole.IDENTIFIER_COMPONENT);
                                hasId = true;
                            }
                        }
                        //the properties collection may have contain other names, for links or geometry, trim those
                        dbfPropertiesIndex = Arrays.copyOf(dbfPropertiesIndex, idx);

                    } catch (IOException ex) {
                        throw new DataStoreException("Failed to parse dbf file header.", ex);
                    }
                } else {
                    throw new DataStoreException("DBF file is missing.");
                }

                type = ftb.build();
            }
            return type;
        }

        @Override
        public Optional<Envelope> getEnvelope() throws DataStoreException {
            getType();//force loading headers
            if (shpHeader != null && filter == null) {
                final GeneralEnvelope env = new GeneralEnvelope(crs);
                env.setRange(0, shpHeader.bbox.getMinimum(0), shpHeader.bbox.getMaximum(0));
                env.setRange(1, shpHeader.bbox.getMinimum(1), shpHeader.bbox.getMaximum(1));
                return Optional.of(env);
            }
            return super.getEnvelope();
        }

        @Override
        public OptionalLong getFeatureCount() {
            try {
                getType();//force loading headers
                if (dbfHeader != null && filter == null) {
                    return OptionalLong.of(dbfHeader.nbRecord);
                }
            } catch (DataStoreException ex) {
                //do nothing
            }
            return super.getFeatureCount();
        }

        @Override
        public Stream<Feature> features(boolean parallel) throws DataStoreException {
            final FeatureType type = getType();
            final ShapeReader shpreader;
            final DBFReader dbfreader;
            try {
                shpreader = readShp ? new ShapeReader(ShpFiles.openReadChannel(files.shpFile), filter) : null;
                dbfreader = (dbfPropertiesIndex.length > 0) ? new DBFReader(ShpFiles.openReadChannel(files.getDbf(false)), charset, timezone, dbfPropertiesIndex) : null;
            } catch (IOException ex) {
                throw new DataStoreException("Faild to open shp and dbf files.", ex);
            }

            int srid = 0;
            final Identifier id = IdentifiedObjects.getIdentifier(crs, Citations.EPSG);
            if (id != null) try {
                srid = Integer.parseInt(id.getCode());
            } catch (NumberFormatException e) {
                // Ignore. Note: this is also the exception if id.getCode() is null.
            }
            final int geomSrid = srid;

            final Spliterator spliterator;
            if (readShp && dbfPropertiesIndex.length > 0) {
                //read both shp and dbf
                final DBFHeader header = dbfreader.getHeader();

                spliterator = new Spliterators.AbstractSpliterator(Long.MAX_VALUE, Spliterator.ORDERED) {
                    @Override
                    public boolean tryAdvance(Consumer action) {
                        try {
                            final ShapeRecord shpRecord = shpreader.next();
                            if (shpRecord == null) return false;
                            //move dbf to record offset, some shp record might have been skipped because of filter
                            long offset = (long)header.headerSize + ((long)(shpRecord.recordNumber-1)) * ((long)header.recordSize);
                            dbfreader.moveToOffset(offset);
                            final Object[] dbfRecord = dbfreader.next();
                            final Feature next = type.newInstance();
                            if (shpRecord.geometry != null) {
                                shpRecord.geometry.setUserData(crs);
                                shpRecord.geometry.setSRID(geomSrid);
                            }
                            next.setPropertyValue(GEOMETRY_NAME, shpRecord.geometry);
                            for (int i = 0; i < dbfPropertiesIndex.length; i++) {
                                next.setPropertyValue(header.fields[dbfPropertiesIndex[i]].fieldName, dbfRecord[i]);
                            }
                            action.accept(next);
                            return true;
                        } catch (IOException ex) {
                            throw new BackingStoreException(ex.getMessage(), ex);
                        }
                    }
                };
            } else if (readShp) {
                //read only the shp
                spliterator = new Spliterators.AbstractSpliterator(Long.MAX_VALUE, Spliterator.ORDERED) {
                    @Override
                    public boolean tryAdvance(Consumer action) {
                        try {
                            final ShapeRecord shpRecord = shpreader.next();
                            if (shpRecord == null) return false;
                            final Feature next = type.newInstance();
                            if (shpRecord.geometry != null) {
                                shpRecord.geometry.setUserData(crs);
                                shpRecord.geometry.setSRID(geomSrid);
                            }
                            next.setPropertyValue(GEOMETRY_NAME, shpRecord.geometry);
                            action.accept(next);
                            return true;
                        } catch (IOException ex) {
                            throw new BackingStoreException(ex.getMessage(), ex);
                        }
                    }
                };
            } else {
                //read only dbf
                final DBFHeader header = dbfreader.getHeader();
                spliterator = new Spliterators.AbstractSpliterator(Long.MAX_VALUE, Spliterator.ORDERED) {
                    @Override
                    public boolean tryAdvance(Consumer action) {
                        try {
                            final Object[] dbfRecord = dbfreader.next();
                            if (dbfRecord == null) return false;
                            final Feature next = type.newInstance();
                            for (int i = 0; i < dbfPropertiesIndex.length; i++) {
                                next.setPropertyValue(header.fields[dbfPropertiesIndex[i]].fieldName, dbfRecord[i]);
                            }
                            action.accept(next);
                            return true;
                        } catch (IOException ex) {
                            throw new BackingStoreException(ex.getMessage(), ex);
                        }
                    }
                };
            }

            final Stream<Feature> stream = StreamSupport.stream(spliterator, false);
            return stream.onClose(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (shpreader != null) shpreader.close();
                        if (dbfreader != null) dbfreader.close();
                    } catch (IOException ex) {
                        throw new BackingStoreException(ex.getMessage(), ex);
                    }
                }
            });

        }

        @Override
        public FeatureSet subset(Query query) throws UnsupportedQueryException, DataStoreException {
            //try to optimise the query for common cases
            opti:
            if (query instanceof FeatureQuery) {
                final FeatureQuery fq = (FeatureQuery) query;
                FeatureQuery.NamedExpression[] projection = fq.getProjection();
                Filter<? super Feature> selection = fq.getSelection();

                if (selection == null && projection == null) {
                    //no optimisation
                    break opti;
                }

                //force loading
                final FeatureType type = getType();

                //extract bbox
                Envelope bbox = null;
                if (selection != null) {
                    //run optimizations
                    final Optimization optimization = new Optimization();
                    optimization.setFeatureType(type);
                    selection = optimization.apply(selection);
                    final Entry<Envelope, Filter> split = extractBbox(selection);
                    bbox = split.getKey();
                    selection = split.getValue();
                }

                //extract field names
                boolean simpleSelection = true; //true if there are no alias and all expressions are ValueReference
                Set<String> properties = null;
                if (projection != null) {
                    properties = ListingPropertyVisitor.xpaths(selection, properties);
                    for (FeatureQuery.NamedExpression ne : projection) {
                        properties = ListingPropertyVisitor.xpaths(ne.expression(), properties);
                        simpleSelection &= (ne.alias() == null);
                        simpleSelection &= (ne.expression().getFunctionName().tip().toString().equals(FunctionNames.ValueReference));
                    }

                    //if link fields are referenced, add target fields
                    if (properties.contains(AttributeConvention.IDENTIFIER)) simpleSelection &= !properties.add(idField);
                    if (properties.contains(AttributeConvention.GEOMETRY)) simpleSelection &= !properties.add(GEOMETRY_NAME);
                    if (properties.contains(AttributeConvention.ENVELOPE)) simpleSelection &= !properties.add(GEOMETRY_NAME);
                }

                final boolean readShp = projection == null || properties.contains(GEOMETRY_NAME);
                Rectangle2D.Double area = null;
                if (bbox != null) {
                    try {
                        bbox = Envelopes.transform(bbox, crs);
                    } catch (TransformException ex) {
                        throw new DataStoreException("Failed to transform bbox filter", ex);
                    }
                    area = new Rectangle2D.Double(bbox.getMinimum(0), bbox.getMinimum(1), bbox.getSpan(0), bbox.getSpan(1));
                }

                if (area == null) {
                    //use current subset one
                    area = filter;
                } else {
                    //combine this area with the one we already have since this is a subset
                    if (filter != null) {
                        area = (Rectangle2D.Double) area.createIntersection(filter);
                    }
                }

                final AsFeatureSet fs = new AsFeatureSet(area, readShp, properties);
                //see if there are elements we could not handle
                final FeatureQuery subQuery = new FeatureQuery();
                boolean needSubProcessing = false;
                if (fq.getLimit().isPresent()){
                    needSubProcessing = true;
                    subQuery.setLimit(fq.getLimit().getAsLong());
                }
                if (fq.getLinearResolution() != null) {
                    needSubProcessing = true;
                    subQuery.setLinearResolution(fq.getLinearResolution());
                }
                if (fq.getOffset() != 0) {
                    needSubProcessing = true;
                    subQuery.setOffset(fq.getOffset());
                }
                if (fq.getSortBy() != null) {
                    needSubProcessing = true;
                    subQuery.setSortBy(fq.getSortBy());
                }
                if (selection != null) {
                    needSubProcessing = true;
                    subQuery.setSelection(selection);
                }
                if (!simpleSelection) {
                    needSubProcessing = true;
                    subQuery.setProjection(projection);
                }

                return needSubProcessing ? fs.parentSubSet(subQuery) : fs;
            }

            return super.subset(query);
        }

        private FeatureSet parentSubSet(Query query) throws DataStoreException {
            return super.subset(query);
        }

        @Override
        public synchronized void updateType(FeatureType newType) throws DataStoreException {

            if (!isDefaultView()) throw new DataStoreException("Resource not writable in current filter state");
            if (Files.exists(shpPath)) {
                throw new DataStoreException("Update type is possible only when files do not exist. It can be used to create a new shapefile but not to update one.");
            }

            final Class<?>[] supportedDateTypes;    // All types other than the one at index 0 will lost information.
            if (timezone == null) {
                supportedDateTypes = new Class<?>[] {
                    LocalDate.class, LocalDateTime.class
                };
            } else {
                supportedDateTypes = new Class<?>[] {
                    LocalDate.class, LocalDateTime.class, OffsetDateTime.class, ZonedDateTime.class, Instant.class
                };
            }

            lock.writeLock().lock();
            try {
                final ShapeHeader shpHeader = new ShapeHeader();
                shpHeader.bbox = new ImmutableEnvelope(new GeneralEnvelope(4));
                final DBFHeader dbfHeader = new DBFHeader();
                dbfHeader.lastUpdate = LocalDate.now();
                dbfHeader.fields = new DBFField[0];
                final Charset charset = userDefinedCharSet == null ? StandardCharsets.UTF_8 : userDefinedCharSet;
                CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();

                for (PropertyType pt : newType.getProperties(true)) {
                    if (pt instanceof AttributeType) {
                        final AttributeType at = (AttributeType) pt;
                        final Class valueClass = at.getValueClass();
                        final String attName = at.getName().tip().toString();

                        Integer length = AttributeConvention.getMaximalLengthCharacteristic(newType, pt);
                        if (length == null || length == 0) length = 255;

                        if (Geometry.class.isAssignableFrom(valueClass)) {
                            if (shpHeader.shapeType != null) {
                                throw new DataStoreException("Shapefile format can only contain one geometry");
                            }
                            if (Point.class.isAssignableFrom(valueClass)) shpHeader.shapeType = ShapeType.POINT;
                            else if (MultiPoint.class.isAssignableFrom(valueClass))
                                shpHeader.shapeType = ShapeType.MULTIPOINT;
                            else if (LineString.class.isAssignableFrom(valueClass) || MultiLineString.class.isAssignableFrom(valueClass))
                                shpHeader.shapeType = ShapeType.POLYLINE;
                            else if (Polygon.class.isAssignableFrom(valueClass) || MultiPolygon.class.isAssignableFrom(valueClass))
                                shpHeader.shapeType = ShapeType.POLYGON;
                            else throw new DataStoreException("Unsupported geometry type " + valueClass);

                            Object cdt = at.characteristics().get(AttributeConvention.CRS);
                            if (cdt instanceof AttributeType) {
                                Object defaultValue = ((AttributeType) cdt).getDefaultValue();
                                if (defaultValue instanceof CoordinateReferenceSystem) {
                                    crs = (CoordinateReferenceSystem) defaultValue;
                                }
                            }

                        } else if (String.class.isAssignableFrom(valueClass)) {
                            dbfHeader.fields = ArraysExt.append(dbfHeader.fields, new DBFField(attName, DBFField.TYPE_CHAR, 0, length, 0, charset, null));
                        } else if (Byte.class.isAssignableFrom(valueClass)) {
                            dbfHeader.fields = ArraysExt.append(dbfHeader.fields, new DBFField(attName, DBFField.TYPE_NUMBER, 0, 4, 0, null, null));
                        } else if (Short.class.isAssignableFrom(valueClass)) {
                            dbfHeader.fields = ArraysExt.append(dbfHeader.fields, new DBFField(attName, DBFField.TYPE_NUMBER, 0, 6, 0, null, null));
                        } else if (Integer.class.isAssignableFrom(valueClass)) {
                            dbfHeader.fields = ArraysExt.append(dbfHeader.fields, new DBFField(attName, DBFField.TYPE_NUMBER, 0, 9, 0, null, null));
                        } else if (Long.class.isAssignableFrom(valueClass)) {
                            dbfHeader.fields = ArraysExt.append(dbfHeader.fields, new DBFField(attName, DBFField.TYPE_NUMBER, 0, 19, 0, null, null));
                        } else if (Float.class.isAssignableFrom(valueClass)) {
                            dbfHeader.fields = ArraysExt.append(dbfHeader.fields, new DBFField(attName, DBFField.TYPE_NUMBER, 0, 11, 6, null, null));
                        } else if (Double.class.isAssignableFrom(valueClass)) {
                            dbfHeader.fields = ArraysExt.append(dbfHeader.fields, new DBFField(attName, DBFField.TYPE_NUMBER, 0, 33, 18, null, null));
                        } else if (Classes.isAssignableToAny(valueClass, supportedDateTypes)) {
                            dbfHeader.fields = ArraysExt.append(dbfHeader.fields, new DBFField(attName, DBFField.TYPE_DATE, 0, 20, 0, null, timezone));
                            if (!(LocalDate.class.isAssignableFrom(valueClass))) {  // TODO: use `index != 0` instead.
                                LOGGER.log(Level.WARNING, "Shapefile writing, field {0} will lost the time component of the date", pt.getName());
                            }
                        } else {
                            LOGGER.log(Level.WARNING, "Shapefile writing, field {0} is not supported", pt.getName());
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "Shapefile writing, field {0} is not supported", pt.getName());
                    }
                }

                //write shapefile
                try (ShapeWriter writer = new ShapeWriter(ShpFiles.openWriteChannel(files.shpFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    writer.writeHeader(shpHeader);
                } catch (IOException ex) {
                    throw new DataStoreException("Failed to create shapefile (shp).", ex);
                }

                //write shx
                try (IndexWriter writer = new IndexWriter(ShpFiles.openWriteChannel(files.getShx(true), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    writer.writeHeader(shpHeader);
                } catch (IOException ex) {
                    throw new DataStoreException("Failed to create shapefile (shx).", ex);
                }

                //write dbf
                try (DBFWriter writer = new DBFWriter(ShpFiles.openWriteChannel(files.getDbf(true), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                    writer.writeHeader(dbfHeader);
                } catch (IOException ex) {
                    throw new DataStoreException("Failed to create shapefile (dbf).", ex);
                }

                //write cpg
                try {
                    CpgFiles.write(charset, files.getCpg(true));
                } catch (IOException ex) {
                    throw new DataStoreException("Failed to create shapefile (cpg).", ex);
                }

                //write prj
                try {
                    final String wkt;

                    if (Utilities.equalsApproximately(crs, CommonCRS.WGS84.normalizedGeographic())) {
                        /*
                         * TODO until we manage to understand the expected ESRI writing for CRS:84
                         * we replace it by hand.
                         * There is an odd recursive information that shapefiles are longitude first whatever the CRS.
                         * But the ESRI specification do not say anything about it.
                         * Generate WKT by tools like ogr,qgis do not declare the axes so we are clueless.
                         */
                        wkt = "GEOGCS[\"GCS_WGS_84_CRS84\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]]";
                    } else {
                        final WKTFormat format = new WKTFormat();
                        format.setConvention(Convention.WKT1_COMMON_UNITS);
                        format.setNameAuthority(Citations.ESRI);
                        format.setIndentation(WKTFormat.SINGLE_LINE);
                        wkt = format.format(crs);
                    }
                    Files.writeString(files.getPrj(true), wkt, StandardCharsets.ISO_8859_1, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

                } catch (IOException ex) {
                    throw new DataStoreException("Failed to create shapefile (prj).", ex);
                }

                //update file list
                files.scan();
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public void add(Iterator<? extends Feature> features) throws DataStoreException {
            if (!isDefaultView()) throw new DataStoreException("Resource not writable in current filter state");
            if (!Files.exists(shpPath)) throw new DataStoreException("FeatureType do not exist, use updateType before modifying features.");

            final Writer writer = new Writer(charset);
            try {
                //write existing features
                try (Stream<Feature> stream = features(false)) {
                    Iterator<Feature> iterator = stream.iterator();
                    while (iterator.hasNext()) {
                        writer.write(iterator.next());
                    }
                }

                //write new features
                while (features.hasNext()) {
                    writer.write(features.next());
                }

                writer.finish(true);
            } catch (IOException ex) {
                try {
                    writer.finish(false);
                } catch (IOException e) {
                    ex.addSuppressed(e);
                }
                throw  new DataStoreException("Writing failed", ex);
            }
        }

        @Override
        public void removeIf(Predicate<? super Feature> filter) throws DataStoreException {
            if (!isDefaultView()) throw new DataStoreException("Resource not writable in current filter state");
            if (!Files.exists(shpPath)) throw new DataStoreException("FeatureType do not exist, use updateType before modifying features.");

            final Writer writer = new Writer(charset);
            try {
                //write existing features not matching filter
                try (Stream<Feature> stream = features(false)) {
                    Iterator<Feature> iterator = stream.filter(filter.negate()).iterator();
                    while (iterator.hasNext()) {
                        writer.write(iterator.next());
                    }
                }
                writer.finish(true);
            } catch (IOException ex) {
                try {
                    writer.finish(false);
                } catch (IOException e) {
                    ex.addSuppressed(e);
                }
                throw  new DataStoreException("Writing failed", ex);
            }
        }

        @Override
        public void replaceIf(Predicate<? super Feature> filter, UnaryOperator<Feature> updater) throws DataStoreException {
            if (!isDefaultView()) throw new DataStoreException("Resource not writable in current filter state");
            if (!Files.exists(shpPath)) throw new DataStoreException("FeatureType do not exist, use updateType before modifying features.");

            final Writer writer = new Writer(charset);
            try {
                //write existing features applying modifications
                try (Stream<Feature> stream = features(false)) {
                    Iterator<Feature> iterator = stream.iterator();
                    while (iterator.hasNext()) {
                        Feature feature = iterator.next();
                        if (filter.test(feature)) {
                            feature = updater.apply(feature);
                        }
                        if (feature != null) writer.write(feature);
                    }
                }
                writer.finish(true);
            } catch (IOException ex) {
                try {
                    writer.finish(false);
                } catch (IOException e) {
                    ex.addSuppressed(e);
                }
                throw  new DataStoreException("Writing failed", ex);
            }
        }

        @Override
        public Optional<FileSet> getFileSet() throws DataStoreException {
            final var paths = new ArrayList<Path>(5);
            final Path shp = files.shpFile;
            final Path shx = files.getShx(false);
            final Path dbf = files.getDbf(false);
            final Path prj = files.getPrj(false);
            final Path cpg = files.getCpg(false);
            if (               Files.exists(shp)) paths.add(shp);
            if (shx != null && Files.exists(shx)) paths.add(shx);
            if (dbf != null && Files.exists(dbf)) paths.add(dbf);
            if (prj != null && Files.exists(prj)) paths.add(prj);
            if (cpg != null && Files.exists(cpg)) paths.add(cpg);
            return Optional.of(new FileSet(paths));
        }
    }

    /**
     * Manipulate the different shape files.
     */
    private static class ShpFiles {

        private final String baseName;
        private final boolean baseUpper;
        private final Path shpFile;
        private Path shxFile;
        private Path dbfFile;
        private Path prjFile;
        private Path cpgFile;

        public ShpFiles(Path shpFile) {
            this.shpFile = shpFile;
            final String fileName = shpFile.getFileName().toString();
            baseUpper = Character.isUpperCase(fileName.codePointAt(fileName.length()-1));
            this.baseName = IOUtilities.filenameWithoutExtension(fileName);
            scan();
        }

        /**
         * Search related files.
         * Should be called after data have been modified.
         */
        private void scan() {
            shxFile = findSibling("shx");
            dbfFile = findSibling("dbf");
            prjFile = findSibling("prj");
            cpgFile = findSibling("cpg");
        }

        /**
         * @param create true to create the path even if file do not exist.
         * @return file if it exist or create is true, null otherwise
         */
        public Path getShx(boolean create) {
            if (create && shxFile == null) {
                return shpFile.resolveSibling(baseName + '.' + (baseUpper ? "SHX" : "shx"));
            }
            return shxFile;
        }

        /**
         * @param create true to create the path even if file do not exist.
         * @return file if it exist or create is true, null otherwise
         */
        public Path getDbf(boolean create) {
            if (create && dbfFile == null) {
                return shpFile.resolveSibling(baseName + '.' + (baseUpper ? "DBF" : "dbf"));
            }
            return dbfFile;
        }

        /**
         * @param create true to create the path even if file do not exist.
         * @return file if it exist or create is true, null otherwise
         */
        public Path getPrj(boolean create) {
            if (create && prjFile == null) {
                return shpFile.resolveSibling(baseName + '.' + (baseUpper ? "PRJ" : "prj"));
            }
            return prjFile;
        }

        /**
         * @param create true to create the path even if file do not exist.
         * @return file if it exist or create is true, null otherwise
         */
        public Path getCpg(boolean create) {
            if (create && cpgFile == null) {
                return shpFile.resolveSibling(baseName + '.' + (baseUpper ? "CPG" : "cpg"));
            }
            return cpgFile;
        }

        /**
         * Create a set of temporary files for edition.
         */
        private ShpFiles createTempFiles() throws IOException{
            final Path tmp = Files.createTempFile(shpFile.getParent(), ".write-session-" + baseName + "-", ".shp");
            Files.delete(tmp);
            return new ShpFiles(tmp);
        }

        /**
         * Delete files permanently.
         */
        private void deleteFiles() throws IOException{
            Files.deleteIfExists(shpFile);
            if (shxFile != null) Files.deleteIfExists(shxFile);
            if (dbfFile != null) Files.deleteIfExists(dbfFile);
            if (cpgFile != null) Files.deleteIfExists(cpgFile);
            if (prjFile != null) Files.deleteIfExists(prjFile);
        }

        /**
         * Override target files by current ones.
         */
        private void replace(ShpFiles toReplace) throws IOException{
            replace(shpFile, toReplace.shpFile);
            replace(shxFile, toReplace.getShx(true));
            replace(dbfFile, toReplace.getDbf(true));
            replace(cpgFile, toReplace.getCpg(true));
            replace(prjFile, toReplace.getPrj(true));
        }

        private static void replace(Path current, Path toReplace) throws IOException{
            if (current == null) {
                Files.deleteIfExists(toReplace);
            } else {
                Files.move(current, toReplace, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        private Path findSibling(String extension) {
            Path candidate = shpFile.resolveSibling(baseName + '.' + extension);
            if (java.nio.file.Files.isRegularFile(candidate)) return candidate;
            candidate = shpFile.resolveSibling(baseName + '.' + extension.toUpperCase());
            if (java.nio.file.Files.isRegularFile(candidate)) return candidate;
            return null;
        }

        private static ChannelDataInput openReadChannel(Path path) throws IOException {
            final SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ);
            return new ChannelDataInput(path.getFileName().toString(), channel, ByteBuffer.allocate(8192), false);
        }

        private static ChannelDataOutput openWriteChannel(Path path, OpenOption ... options) throws IOException, IllegalArgumentException, DataStoreException {
            final WritableByteChannel wbc;
            if (options != null && options.length > 0) {
                wbc = Files.newByteChannel(path, ArraysExt.append(options, StandardOpenOption.WRITE));
            } else {
                wbc = Files.newByteChannel(path, StandardOpenOption.WRITE);
            }
            return new ChannelDataOutput(path.getFileName().toString(), wbc, ByteBuffer.allocate(8192));
        }
    }


    /**
     * Will only split bbox with direct value reference to the geometry.
     *
     * @param filter to split, not null
     * @return entry with key is a BBox filter and value what remains of the filter.
     *        each filter can be null but not both.
     */
    private static Entry<Envelope,Filter> extractBbox(Filter<?> filter) {

        final CodeList operatorType = filter.getOperatorType();

        if (operatorType == SpatialOperatorName.BBOX) {
            Envelope env = isDirectBbox(filter);
            if (env != null) {
                return new AbstractMap.SimpleImmutableEntry<>(env, null);
            } else {
                return new AbstractMap.SimpleImmutableEntry<>(null, filter);
            }

        } else if (operatorType == LogicalOperatorName.AND) {

            boolean rebuildAnd = false;
            List<Filter<?>> lst = (List<Filter<?>>) ((LogicalOperator<?>)filter).getOperands();
            Envelope bbox = null;
            for (int i = 0; i < lst.size(); i++) {
                final Filter<?> f = lst.get(i);
                final Entry<Envelope, Filter> split = extractBbox(f);
                Envelope cdtBbox = split.getKey();
                Filter cdtOther = split.getValue();
                if (cdtBbox != null) {
                    if (bbox == null) {
                        bbox = cdtBbox;
                    } else {
                        throw new RuntimeException("Combine bbox");
                    }

                    //see if we need to rebuild the AND filter
                    if (cdtOther != f) {
                        if (!rebuildAnd) {
                            rebuildAnd = true;
                            lst = new ArrayList<>(lst);
                        }
                        //replace in list
                        if (cdtOther != null) {
                            lst.set(i, cdtOther);
                        } else {
                            lst.remove(i);
                            i--;
                        }
                    }
                }
            }

            if (rebuildAnd) {
                if (lst.isEmpty()) {
                    filter = null;
                } else if (lst.size() == 1) {
                    filter = lst.get(0);
                } else {
                    filter = DefaultFilterFactory.forFeatures().and((List)lst);
                }
            }

            return new AbstractMap.SimpleImmutableEntry<>(bbox, filter);
        } else {
            //can do nothing
            return new AbstractMap.SimpleImmutableEntry<>(null, filter);
        }
    }

    /**
     * Returns envelope if the other expression is a direct value reference.
     * @param bbox
     * @return filter envelope
     */
    private static Envelope isDirectBbox(Filter<?> bbox) {
        Envelope env = null;
        for (Expression exp : bbox.getExpressions()) {
            if (exp instanceof ValueReference) {
                final ValueReference<Object,?> expression = (ValueReference<Object,?>) exp;
                final String propName = expression.getXPath();
                if ( !(GEOMETRY_NAME.equals(propName) || AttributeConvention.GEOMETRY.equals(propName))) {
                    return null;
                }
            } else if (exp instanceof Literal) {
                Object value = ((Literal) exp).getValue();
                env = Geometries.wrap(value).get().getEnvelope();
            }
        }
        return env;
    }

    private class Writer {

        private final ShpFiles tempFiles;
        private final ShapeWriter shpWriter;
        private final DBFWriter dbfWriter;
        private final IndexWriter shxWriter;
        private final ShapeHeader shpHeader;
        private final DBFHeader dbfHeader;
        private String defaultGeomName = null;
        private int inc = 0;

        private Writer(Charset charset) throws DataStoreException{
            try {
                tempFiles = files.createTempFiles();
            } catch (IOException ex) {
                throw new DataStoreException("Failed to create temp files", ex);
            }

            try {
                //get original headers and information
                try (ShapeReader reader = new ShapeReader(ShpFiles.openReadChannel(files.shpFile), null)) {
                    shpHeader = new ShapeHeader(reader.getHeader());
                }
                try (DBFReader reader = new DBFReader(ShpFiles.openReadChannel(files.dbfFile), charset, timezone, null)) {
                    dbfHeader = new DBFHeader(reader.getHeader());
                }

                //unchanged files
                if (files.cpgFile != null) Files.copy(files.cpgFile, tempFiles.getCpg(true), StandardCopyOption.REPLACE_EXISTING);
                if (files.prjFile != null) Files.copy(files.prjFile, tempFiles.getPrj(true), StandardCopyOption.REPLACE_EXISTING);

                //start new files

                //write shapefile
                shpWriter = new ShapeWriter(ShpFiles.openWriteChannel(tempFiles.shpFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
                dbfWriter = new DBFWriter(ShpFiles.openWriteChannel(tempFiles.getDbf(true), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
                shxWriter = new IndexWriter(ShpFiles.openWriteChannel(tempFiles.getShx(true), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
                shpWriter.writeHeader(shpHeader);
                shxWriter.writeHeader(shpHeader);
                dbfWriter.writeHeader(dbfHeader);
            } catch (IOException ex) {
                try {
                    tempFiles.deleteFiles();
                } catch (IOException e) {
                    ex.addSuppressed(e);
                }
                throw new DataStoreException("Failed to create temp files", ex);
            }

        }

        private void write(Feature feature) throws IOException {
            inc++; //number starts at 1
            final ShapeRecord shpRecord = new ShapeRecord();
            final long recordStartPosition = shpWriter.getSteamPosition();

            if (defaultGeomName == null) {
                //search for the geometry name
                for (PropertyType pt : feature.getType().getProperties(true)) {
                    if (pt instanceof AttributeType) {
                        final AttributeType at = (AttributeType) pt;
                        final String attName = at.getName().toString();
                        if (Geometry.class.isAssignableFrom(at.getValueClass())) {
                            defaultGeomName = attName;
                        }
                    }
                }
                if (defaultGeomName == null) {
                    throw new IOException("Failed to find a geometry attribute in given features");
                }
            }

            //write geometry
            Object value = feature.getPropertyValue(defaultGeomName);
            if (value instanceof Geometry) {
                shpRecord.geometry = (Geometry) value;
                shpRecord.recordNumber = inc;
            } else {
                throw new IOException("Feature geometry property is not a geometry");
            }
            shpWriter.writeRecord(shpRecord);
            final long recordEndPosition = shpWriter.getSteamPosition();

            //write index
            final int recordStartPositionWord = Math.toIntExact(recordStartPosition / 2); // divide by 2 for word size
            final int recordEndPositionWord = Math.toIntExact(recordEndPosition / 2); // divide by 2 for word size
            shxWriter.writeRecord(recordStartPositionWord, recordEndPositionWord - recordStartPositionWord);

            //copy dbf fields
            Object[] fields = new Object[dbfHeader.fields.length];
            for (int i = 0; i < fields.length; i++) {
                fields[i] = feature.getPropertyValue(dbfHeader.fields[i].fieldName);
            }
            dbfWriter.writeRecord(fields);
        }

        /**
         * Close file writers and replace original files if true.
         */
        private void finish(boolean replaceOriginals) throws IOException {
            try {
                shpWriter.close();
                dbfWriter.close();
                shxWriter.getHeader().bbox = shpWriter.getHeader().bbox;
                shxWriter.close();
                tempFiles.scan();
                if (replaceOriginals) {
                    lock.writeLock().lock();
                    try {
                        //swap files
                        tempFiles.replace(files);
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            } finally {
                tempFiles.deleteFiles();
            }
        }
    }

}
