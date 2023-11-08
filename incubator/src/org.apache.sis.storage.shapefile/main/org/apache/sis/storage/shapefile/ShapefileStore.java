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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
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
import org.opengis.filter.SpatialOperatorName;


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
    private final AsFeatureSet featureSetView = new AsFeatureSet(null, null);
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
        private FeatureType type;

        /**
         * @param filter optional shape filter, must be in data CRS
         * @param properties dbf properties to read, null for all properties
         */
        private AsFeatureSet(Rectangle2D.Double filter, Set<String> properties) {
            super(null);
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
                final CoordinateReferenceSystem crs;
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
                shpreader = new ShapeReader(ShpFiles.openReadChannel(files.shpFile), filter);
                dbfreader = new DBFReader(ShpFiles.openReadChannel(files.getDbf(false)), charset, dbfPropertiesIndex);
            } catch (IOException ex) {
                throw new DataStoreException("Faild to open shp and dbf files.", ex);
            }
            final DBFHeader header = dbfreader.getHeader();

            final Spliterator spliterator = new Spliterators.AbstractSpliterator(Long.MAX_VALUE, Spliterator.ORDERED) {
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
                            next.setPropertyValue(header.fields[i].fieldName, dbfRecord.fields[i]);
                        }
                        action.accept(next);
                        return true;
                    } catch (IOException ex) {
                        throw new BackingStoreException(ex.getMessage(), ex);
                    }
                }
            };
            final Stream<Feature> stream = StreamSupport.stream(spliterator, false);
            return stream.onClose(new Runnable() {
                @Override
                public void run() {
                    try {
                        shpreader.close();
                        dbfreader.close();
                    } catch (IOException ex) {
                        throw new BackingStoreException(ex.getMessage(), ex);
                    }
                }
            });

        }

        @Override
        public FeatureSet subset(Query query) throws UnsupportedQueryException, DataStoreException {
            //try to optimise the query for common cases
            if (query instanceof FeatureQuery) {
                final FeatureQuery fq = (FeatureQuery) query;
                //todo
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

}
