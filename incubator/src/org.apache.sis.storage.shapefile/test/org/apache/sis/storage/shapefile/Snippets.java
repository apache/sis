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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.FilterFactory;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class Snippets {

    public void read() throws IllegalArgumentException, DataStoreException, IOException{
        // @start region="read"
        //open datastore
        try (ShapefileStore store = new ShapefileStore(Paths.get("/path/to/file.shp"))) {

            //print feature type
            System.out.println(store.getType());

            //print all features
            try (Stream<Feature> features = store.features(false)) {
                features.forEach(System.out::println);
            }

            //print only features in envelope and only selected attributes
            GeneralEnvelope bbox = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            bbox.setRange(0, -10, 30);
            bbox.setRange(1, 45, 55);
            FilterFactory<Feature, Object, Object> ff = DefaultFilterFactory.forFeatures();
            BinarySpatialOperator<Feature> bboxFilter = ff.bbox(ff.property("geometry"), bbox);

            FeatureQuery query = new FeatureQuery();
            query.setProjection("att1", "att4", "att5");
            query.setSelection(bboxFilter);

            //print selected features
            try (Stream<Feature> features = store.subset(query).features(false)) {
                features.forEach(System.out::println);
            }

        }
        // @end

    }

    public void write() throws IllegalArgumentException, DataStoreException, IOException{
        // @start region="write"
        //open a channel
        try (ShapefileStore store = new ShapefileStore(Paths.get("/path/to/file.shp"))) {

            //define the feature type
            FeatureTypeBuilder ftb = new FeatureTypeBuilder();
            ftb.setName("test");
            ftb.addAttribute(Integer.class).setName("id");
            ftb.addAttribute(String.class).setName("text");
            ftb.addAttribute(Point.class).setName("geometry").setCRS(CommonCRS.WGS84.geographic());
            FeatureType type = ftb.build();

            store.updateType(type);
            type = store.getType();

            //create features
            GeometryFactory gf = new GeometryFactory();
            Feature feature = type.newInstance();
            feature.setPropertyValue("geometry", gf.createPoint(new Coordinate(10,20)));
            feature.setPropertyValue("id", 1);
            feature.setPropertyValue("text", "some text 1");

            //add feature in the store
            store.add(List.of(feature).iterator());
        }
        // @end
    }

}
