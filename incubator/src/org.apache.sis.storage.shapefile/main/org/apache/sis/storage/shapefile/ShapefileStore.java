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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
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

import org.apache.sis.geometry.wrapper.Geometries;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.internal.AttributeConvention;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.filter.Optimization;
import org.apache.sis.filter.internal.FunctionNames;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.io.stream.ChannelDataInput;
import org.apache.sis.io.stream.ChannelDataOutput;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.storage.WritableFeatureSet;
import org.apache.sis.storage.shapefile.cpg.CpgFiles;
import org.apache.sis.storage.shapefile.dbf.DBFField;
import org.apache.sis.storage.shapefile.dbf.DBFHeader;
import org.apache.sis.storage.shapefile.dbf.DBFReader;
import org.apache.sis.storage.shapefile.dbf.DBFRecord;
import org.apache.sis.storage.shapefile.shp.ShapeGeometryEncoder;
import org.apache.sis.storage.shapefile.shp.ShapeHeader;
import org.apache.sis.storage.shapefile.shp.ShapeReader;
import org.apache.sis.storage.shapefile.shp.ShapeRecord;
import org.apache.sis.util.collection.BackingStoreException;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.Literal;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.filter.ValueReference;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.CodeList;


/**
 * Shapefile datastore.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ShapefileStore extends DataStore implements FeatureSet {

    private static final String GEOMETRY_NAME = "geometry";

    private final Path shpPath;
    private final ShpFiles files;
    /**
     * Internal class to inherit AbstractFeatureSet.
     */
    private final AsFeatureSet featureSetView = new AsFeatureSet(null, true, null);
    private Charset charset;

    /**
     * Lock to control read and write operations.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ShapefileStore(Path path) {
        this.shpPath = path;
        this.files = new ShpFiles(shpPath);
    }

    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final Parameters parameters = Parameters.castOrWrap(ShapefileProvider.PARAMETERS_DESCRIPTOR.createValue());
        parameters.parameter(ShapefileProvider.LOCATION).setValue(shpPath.toUri());
        return Optional.of(parameters);
    }

    @Override
    public void close() throws DataStoreException {
    }


    /*
    Redirect FeatureSet interface to View
    */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return featureSetView.getIdentifier();
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return featureSetView.getMetadata();
    }

    @Override
    public FeatureType getType() throws DataStoreException {
        return featureSetView.getType();
    }

    @Override
    public FeatureSet subset(Query query) throws UnsupportedQueryException, DataStoreException {
        return featureSetView.subset(query);
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        return featureSetView.features(parallel);
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return featureSetView.getEnvelope();
    }

    private class AsFeatureSet extends AbstractFeatureSet implements WritableFeatureSet {

        private final Rectangle2D.Double filter;
        private final Set<String> dbfProperties;
        private int[] dbfPropertiesIndex;
        private final boolean readShp;
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
                    try (DBFReader reader = new DBFReader(ShpFiles.openReadChannel(dbfFile), charset, null)) {
                        final DBFHeader header = reader.getHeader();
                        boolean hasId = false;

                        if (dbfProperties == null) {
                            dbfPropertiesIndex = new int[header.fields.length];
                        } else {
                            dbfPropertiesIndex = new int[dbfProperties.size()];
                        }

                        for (int i = 0,idx=0; i < header.fields.length; i++) {
                            final DBFField field = header.fields[i];
                            if (dbfProperties != null && !dbfProperties.contains(field.fieldName)) {
                                //skip unwanted fields
                                continue;
                            }
                            dbfPropertiesIndex[idx] = i;
                            idx++;

                            final AttributeTypeBuilder atb = ftb.addAttribute(field.getEncoder().getValueClass()).setName(field.fieldName);
                            //no official but 'id' field is common
                            if (!hasId && "id".equalsIgnoreCase(field.fieldName) || "identifier".equalsIgnoreCase(field.fieldName)) {
                                idField = field.fieldName;
                                atb.addRole(AttributeRole.IDENTIFIER_COMPONENT);
                                hasId = true;
                            }
                        }
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
        public Stream<Feature> features(boolean parallel) throws DataStoreException {
            final FeatureType type = getType();
            final ShapeReader shpreader;
            final DBFReader dbfreader;
            try {
                shpreader = readShp ? new ShapeReader(ShpFiles.openReadChannel(files.shpFile), filter) : null;
                dbfreader = (dbfPropertiesIndex.length > 0) ? new DBFReader(ShpFiles.openReadChannel(files.getDbf(false)), charset, dbfPropertiesIndex) : null;
            } catch (IOException ex) {
                throw new DataStoreException("Faild to open shp and dbf files.", ex);
            }

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
                            dbfreader.moveToOffset(header.headerSize + (shpRecord.recordNumber-1) * header.recordSize);
                            final DBFRecord dbfRecord = dbfreader.next();
                            final Feature next = type.newInstance();
                            next.setPropertyValue(GEOMETRY_NAME, shpRecord.geometry);
                            for (int i = 0; i < dbfPropertiesIndex.length; i++) {
                                next.setPropertyValue(header.fields[dbfPropertiesIndex[i]].fieldName, dbfRecord.fields[i]);
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
                            final DBFRecord dbfRecord = dbfreader.next();
                            if (dbfRecord == null) return false;
                            final Feature next = type.newInstance();
                            for (int i = 0; i < dbfPropertiesIndex.length; i++) {
                                next.setPropertyValue(header.fields[dbfPropertiesIndex[i]].fieldName, dbfRecord.fields[i]);
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
                    properties = new HashSet<>();
                    if (selection!=null) ListingPropertyVisitor.VISITOR.visit((Filter) selection, properties);
                    for (FeatureQuery.NamedExpression ne : projection) {
                        ListingPropertyVisitor.VISITOR.visit((Expression) ne.expression, properties);
                        simpleSelection &= (ne.alias == null);
                        simpleSelection &= (ne.expression.getFunctionName().tip().toString().equals(FunctionNames.ValueReference));
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

                    //combine this area with the one we already have since this is a subset
                    if (filter != null) {
                        area = (Rectangle2D.Double) area.createIntersection(filter);
                    }
                }

                FeatureSet fs = new AsFeatureSet(area, readShp, properties);
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

                return needSubProcessing ? fs.subset(subQuery) : fs;
            }

            return super.subset(query);
        }



        @Override
        public void updateType(FeatureType newType) throws DataStoreException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void add(Iterator<? extends Feature> features) throws DataStoreException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removeIf(Predicate<? super Feature> filter) throws DataStoreException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void replaceIf(Predicate<? super Feature> filter, UnaryOperator<Feature> updater) throws DataStoreException {
            throw new UnsupportedOperationException("Not supported yet.");
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
                return shpFile.getParent().resolve(baseName + "." + (baseUpper ? "SHX" : "shx"));
            }
            return shxFile;
        }

        /**
         * @param create true to create the path even if file do not exist.
         * @return file if it exist or create is true, null otherwise
         */
        public Path getDbf(boolean create) {
            if (create && dbfFile == null) {
                return shpFile.getParent().resolve(baseName + "." + (baseUpper ? "DBF" : "dbf"));
            }
            return dbfFile;
        }

        /**
         * @param create true to create the path even if file do not exist.
         * @return file if it exist or create is true, null otherwise
         */
        public Path getPrj(boolean create) {
            if (create && prjFile == null) {
                return shpFile.getParent().resolve(baseName + "." + (baseUpper ? "PRJ" : "prj"));
            }
            return prjFile;
        }

        /**
         * @param create true to create the path even if file do not exist.
         * @return file if it exist or create is true, null otherwise
         */
        public Path getCpg(boolean create) {
            if (create && cpgFile == null) {
                return shpFile.getParent().resolve(baseName + "." + (baseUpper ? "CPG" : "cpg"));
            }
            return cpgFile;
        }

        private Path findSibling(String extension) {
            Path candidate = shpFile.getParent().resolve(baseName + "." + extension);
            if (java.nio.file.Files.isRegularFile(candidate)) return candidate;
            candidate = shpFile.getParent().resolve(baseName + "." + extension.toUpperCase());
            if (java.nio.file.Files.isRegularFile(candidate)) return candidate;
            return null;
        }

        private static ChannelDataInput openReadChannel(Path path) throws IOException {
            final SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ);
            return new ChannelDataInput(path.getFileName().toString(), channel, ByteBuffer.allocate(8192), false);
        }

        private static ChannelDataOutput openWriteChannel(Path path) throws IOException, IllegalArgumentException, DataStoreException {
            final WritableByteChannel wbc = Files.newByteChannel(path, StandardOpenOption.WRITE);
            return new ChannelDataOutput(path.getFileName().toString(), wbc, ByteBuffer.allocate(8000));
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

        if (SpatialOperatorName.BBOX.equals(operatorType)) {
            Envelope env = isDirectBbox(filter);
            if (env != null) {
                return new AbstractMap.SimpleImmutableEntry<>(env, null);
            } else {
                return new AbstractMap.SimpleImmutableEntry<>(null, filter);
            }

        } else if (LogicalOperatorName.AND.equals(operatorType)) {

            boolean rebuildAnd = false;
            List<Filter<?>> lst = (List<Filter<?>>) ((LogicalOperator<?>)filter).getOperands();
            Envelope bbox = null;
            for (int i = 0, n = lst.size(); i < n; i++) {
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


}
