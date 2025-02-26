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
package org.apache.sis.storage.geoheif;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.opengis.util.GenericName;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.storage.tiling.TileMatrix;


/**
 * SortedMap of TileMatrix sorted by first axe resolution.
 *
 * @author Johann Sorel (Geomatys)
 */
final class ScaleSortedMap<T extends TileMatrix> extends TreeMap<GenericName, T>{

    public ScaleSortedMap() {
        super(new ScaleComparator());
        comparator();
    }

    public void insertByScale(T tileMatrix) {
        final GenericName id = tileMatrix.getIdentifier();
        ArgumentChecks.ensureNonNull("identifier", id);
        final ScaleComparator comparator = (ScaleComparator) comparator();
        if (comparator.matricesByScale.containsKey(id)) {
            throw new IllegalArgumentException("Key already exist : " + id);
        }
        final double resolution = tileMatrix.getResolution()[0];
        comparator.matricesByScale.put(id, resolution);
        super.put(id, tileMatrix);
    }

    public void removeByScale(T tileMatrix) {
        final GenericName id = tileMatrix.getIdentifier();
        ArgumentChecks.ensureNonNull("identifier", id);
        final ScaleComparator comparator = (ScaleComparator) comparator();
        if (comparator.matricesByScale.remove(id) != null) {
            super.remove(id);
        }
    }

    @Override
    public T put(GenericName key, T value) {
        throw new IllegalArgumentException("Should not be used");
    }

    @Override
    public void putAll(Map<? extends GenericName, ? extends T> map) {
        throw new IllegalArgumentException("Should not be used");
    }

    @Override
    public T putIfAbsent(GenericName key, T value) {
        throw new IllegalArgumentException("Should not be used");
    }

    @Override
    public T remove(Object key) {
        throw new IllegalArgumentException("Should not be used");
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new IllegalArgumentException("Should not be used");
    }

    /**
     * Compare TileMatrix by scale.
     * Entries are sorted from coarser resolution (highest scale denominator) to most detailed resolution (lowest scale denominator).
     */
    private static class ScaleComparator implements Comparator<GenericName> {
        private final Map<GenericName,Double> matricesByScale = new HashMap<>();

        @Override
        public int compare(GenericName o1, GenericName o2) {
            Double d1 = matricesByScale.get(o1);
            Double d2 = matricesByScale.get(o2);
            if (d1 == null) d1 = Double.NaN;
            if (d2 == null) d2 = Double.NaN;
            int v = Double.compare(d2, d1);
            if (v != 0) return v;
            //we NEED ordering, otherwise entry will be replaced.
            return o1.toString().compareTo(o2.toString());
        }
    }
}
