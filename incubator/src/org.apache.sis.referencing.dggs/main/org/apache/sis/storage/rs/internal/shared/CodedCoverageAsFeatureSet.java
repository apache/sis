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
package org.apache.sis.storage.rs.internal.shared;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.internal.shared.AttributeConvention;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.dggs.Zone;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.dggs.DiscreteGlobalGridSystems;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodeTransform;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.WritableCodeIterator;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.internal.shared.AbstractIterator;
import org.apache.sis.util.iso.Names;


/**
 * View a DGGS Coverage as a FeatureSet.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CodedCoverageAsFeatureSet extends AbstractFeatureSet {

    /**
     * For vector output formats, retrieve the zone centroid as geometry.
     */
    public static final String GEOMETRY_ZONE_CENTROID = "zone-centroid";
    /**
     * For vector output formats, retrieve the zone region as geometry.
     */
    public static final String GEOMETRY_ZONE_REGION = "zone-region";
    /**
     * For vector output formats, do not return the geometry.
     */
    public static final String GEOMETRY_ZONE_NONE = "none";

    private final String geometryType;
    private final CodedCoverage coverage;
    private final CodedGeometry coverageGeometry;
    private final CodeTransform gridToRs;
    private final ReferenceSystem[] singleRS;
    private DiscreteGlobalGridReferenceSystem horizontal;
    private final FeatureType type;
    private final String[] sampleNames;
    private final boolean idAsLong;
    /**
     * Negative values point to ordinate (with an offset -1), positive to sample dimensions
     */
    private final Map<String,Integer> index = new HashMap();

    /**
     *
     * @param coverage
     * @param idAsLong
     * @param geometryType one of GEOMETRY_X constants
     */
    public CodedCoverageAsFeatureSet(CodedCoverage coverage, boolean idAsLong, String geometryType) {
        super(null);
        this.coverage = coverage;
        this.idAsLong = idAsLong;
        coverageGeometry = coverage.getGeometry();
        gridToRs = coverageGeometry.getGridToRS();
        if (geometryType == null) geometryType = GEOMETRY_ZONE_REGION;
        this.geometryType = geometryType;

        singleRS = ReferenceSystems.getSingleComponents(coverageGeometry.getReferenceSystem(), true).toArray(ReferenceSystem[]::new);

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("rs");

        //convert location components to properties
        for (int i = 0, n = singleRS.length; i < n ;i++) {
            ReferenceSystem rs  = singleRS[i];
            if (rs instanceof DiscreteGlobalGridReferenceSystem dggrs) {
                horizontal = dggrs;
                if (idAsLong) {
                    ftb.addAttribute(Long.class).setName(AttributeConvention.IDENTIFIER_PROPERTY).addRole(AttributeRole.IDENTIFIER_COMPONENT);
                } else {
                    ftb.addAttribute(String.class).setName(AttributeConvention.IDENTIFIER_PROPERTY).addRole(AttributeRole.IDENTIFIER_COMPONENT);
                }
                index.put(AttributeConvention.IDENTIFIER, -i-1);

                switch (geometryType) {
                    case GEOMETRY_ZONE_REGION :
                        ftb.addAttribute(Geometry.class).setName(AttributeConvention.GEOMETRY_PROPERTY).setCRS(coverage.getCoordinateReferenceSystem()).addRole(AttributeRole.DEFAULT_GEOMETRY);
                        index.put(AttributeConvention.GEOMETRY, -i-1);
                        break;
                    case GEOMETRY_ZONE_CENTROID :
                        ftb.addAttribute(Point.class).setName(AttributeConvention.GEOMETRY_PROPERTY).setCRS(coverage.getCoordinateReferenceSystem()).addRole(AttributeRole.DEFAULT_GEOMETRY);
                        index.put(AttributeConvention.GEOMETRY, -i-1);
                        break;
                    case GEOMETRY_ZONE_NONE :
                }
            } else {
                String name = rs.getName().toString();
                ftb.addAttribute(Object.class).setName(name);
                index.put(name, -i-1);
            }
        }

        //fill the index
        final AttributeTypeBuilder<?>[] atts = toFeatureType(ftb, coverage.getSampleDimensions());
        sampleNames = new String[atts.length];
        for (int i = 0; i < sampleNames.length; i++) {
            sampleNames[i] = atts[i].getName().toString();
            index.put(sampleNames[i], i);
        }

        type = ftb.build();
    }

    @Override
    public FeatureType getType() throws DataStoreException {
        return type;
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        final CodeIterator iterator = coverage.createIterator();
        final DiscreteGlobalGridReferenceSystem.Coder coder = horizontal.createCoder();

        final Iterator<CodeIterator> ite = new AbstractIterator() {
            @Override
            public boolean hasNext() {
                if (next != null) return true;
                if (iterator.next()) {
                    next = iterator;
                }
                return next != null;
            }
        };

        final Stream<CodeIterator> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(ite, Spliterator.ORDERED), false);
        return stream.map((CodeIterator t) -> new CellAsFeature(t, coder));
    }

    /**
     * View code iterator samples as a Feature.
     */
    public Feature viewAsFeature(CodeIterator iterator) {
        return new CellAsFeature(iterator, horizontal.createCoder());
    }

    private class CellAsFeature extends AbstractFeature {

        private final CodeIterator iterator;
        private final DiscreteGlobalGridReferenceSystem.Coder coder;
        private Object[] ordinates;
        private double[] cell;

        public CellAsFeature(CodeIterator iterator, DiscreteGlobalGridReferenceSystem.Coder coder) {
            super(type);
            this.iterator = iterator;
            this.coder = coder;
        }

        private synchronized Object[] getOrdinate() throws TransformException {
            if (ordinates == null) {
                int[] pos = iterator.getPosition();
                ordinates = gridToRs.toCode(pos).getOrdinates();
            }
            return ordinates;
        }

        private synchronized double[] getCell() {
            if (cell == null) {
                cell = iterator.getCell(new double[iterator.getNumBands()]);
            }
            return cell;
        }

        @Override
        public Object getPropertyValue(String propName) {
            final Integer idx = index.get(propName);
            if (idx == null) {
                throw new PropertyNotFoundException(propName);
            }
            if (idx < 0) {
                try {
                    Object obj = getOrdinate()[Math.abs(idx+1)];
                    if (AttributeConvention.IDENTIFIER.equals(propName)) {
                        Zone zone = coder.decode(obj);
                        if (idAsLong) {
                            obj = zone.getLongIdentifier();
                        } else {
                            obj = zone.getTextIdentifier().toString();
                        }
                    } else if (AttributeConvention.GEOMETRY.equals(propName)) {
                        Zone zone = coder.decode(obj);
                        switch (geometryType) {
                            case GEOMETRY_ZONE_REGION :
                                obj = DiscreteGlobalGridSystems.toJTSPolygon(zone.getGeographicExtent());
                                break;
                            case GEOMETRY_ZONE_CENTROID :
                                Point pt = DiscreteGlobalGridSystems.toJTSPolygon(zone.getGeographicExtent()).getCentroid();
                                //force 2d, bad habit from JTS to add a NaN Z dimension
                                obj = pt.getFactory().createPoint(new CoordinateXY(pt.getX(), pt.getY()));
                                break;
                            case GEOMETRY_ZONE_NONE :
                                throw new PropertyNotFoundException(propName);
                        }

                    }
                    return obj;
                } catch (TransformException ex) {
                    throw new BackingStoreException(ex);
                }
            } else {
                return getCell()[idx];
            }
        }

        @Override
        public void setPropertyValue(String propName, Object o) throws IllegalArgumentException {
            final Integer idx = index.get(propName);
            if (idx == null) throw new IllegalArgumentException("Property not found or can not be edited");
            if (idx < 0) {
                throw new IllegalArgumentException("Property " + propName + "can not be edited");
            } else {
                getCell();
                ((WritableCodeIterator)iterator).setSample(idx, ((Number)o).doubleValue());
            }
        }
    }

    /**
     * Convert SampleDimension to feature attributes.
     *
     * @param ftb to append attributes to
     * @param sampleDimensions coverage sample dimensions
     * @return the created attributes
     */
    public static AttributeTypeBuilder<?>[] toFeatureType(FeatureTypeBuilder ftb, List<SampleDimension> sampleDimensions) {
        final AttributeTypeBuilder[] samples = new AttributeTypeBuilder[sampleDimensions.size()];
        for (int i = 0; i < samples.length; i++) {
            final GenericName name = Names.createLocalName(null, null, sampleDimensions.get(i).getName().tip().toString());
            samples[i] = ftb.addAttribute(Double.class).setName(name);
        }
        return samples;
    }

}
