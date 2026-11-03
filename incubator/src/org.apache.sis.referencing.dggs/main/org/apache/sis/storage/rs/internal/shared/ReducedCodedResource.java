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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.rs.CodeIterator;
import org.apache.sis.storage.rs.CodeTransform;
import org.apache.sis.storage.rs.CodedCoverage;
import org.apache.sis.storage.rs.CodedGeometry;
import org.apache.sis.storage.rs.CodedResource;
import org.apache.sis.storage.rs.WritableCodeIterator;


/**
 * View of a coded resource with one or more components of its reference system hidden.
 * Each hidden component is fixed at a given grid index; the resulting resource
 * only exposes the remaining components.
 *
 * <p>Hidden components must be one-dimensional (typically vertical or temporal components).
 * The horizontal Discrete Global Grid component cannot be hidden.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ReducedCodedResource extends AbstractResource implements CodedResource {

    private final GenericName name;
    private final CodedResource resource;
    /**
     * Single components of the source resource reference system.
     */
    private final List<ReferenceSystem> components;
    /**
     * For each source component, the source geometry slice if the component is kept, null otherwise.
     */
    private final CodedGeometry[] keptSlices;
    /**
     * For each source component, a single cell geometry at the fixed index if the component is hidden, null otherwise.
     */
    private final CodedGeometry[] hiddenSlices;
    private final CodedGeometry geometry;

    /**
     * Creates a new reduced view.
     *
     * @param name      name of the reduced resource.
     * @param resource  the resource to reduce.
     * @param hidden    map of source component index to the grid index to use in this component.
     *                  Component indices are those of {@code ReferenceSystems.getSingleComponents(rs, true)}.
     * @throws DataStoreException if the resource geometry cannot be read.
     * @throws IllegalArgumentException if a component index or grid index is invalid.
     */
    public ReducedCodedResource(GenericName name, CodedResource resource, Map<Integer,Integer> hidden) throws DataStoreException {
        super(null);
        this.name = name;
        this.resource = resource;

        final CodedGeometry resourceGeometry = resource.getGridGeometry();
        components = ReferenceSystems.getSingleComponents(resourceGeometry.getReferenceSystem(), true);
        keptSlices = new CodedGeometry[components.size()];
        hiddenSlices = new CodedGeometry[components.size()];

        for (Map.Entry<Integer,Integer> entry : hidden.entrySet()) {
            final int idx = entry.getKey();
            final int index = entry.getValue();
            if (idx < 0 || idx >= components.size()) {
                throw new IllegalArgumentException("Component " + idx + " does not exist, reference system has " + components.size() + " components");
            }
            final ReferenceSystem component = components.get(idx);
            if (component instanceof DiscreteGlobalGridReferenceSystem) {
                throw new IllegalArgumentException("Component " + idx + " is a Discrete Global Grid, it can not be hidden");
            }
            if (ReferenceSystems.getDimension(component) != 1) {
                throw new IllegalArgumentException("Component " + idx + " is not one-dimensional, it can not be hidden");
            }
            final CodedGeometry slice = resourceGeometry.slice(component).get();
            if (slice.isDefined(CodedGeometry.EXTENT) && !slice.getExtent().contains(index)) {
                throw new IllegalArgumentException("Index " + index + " is outside of component " + idx + " extent " + slice.getExtent());
            }
            final GridExtent cell = new GridExtent(null, new long[] {index}, new long[] {index}, true);
            CodeTransform gridToRS = slice.getGridToRS();
            if (gridToRS instanceof CodeTransforms.Grid g) {
                final GridGeometry grid = g.getGrid();
                gridToRS = CodeTransforms.toTransform(new GridGeometry(cell, PixelInCell.CELL_CENTER,
                        grid.getGridToCRS(PixelInCell.CELL_CENTER), grid.getCoordinateReferenceSystem()));
            }
            hiddenSlices[idx] = new CodedGeometry(component, cell, gridToRS, null);
        }

        final List<CodedGeometry> kept = new ArrayList<>();
        for (int i = 0; i < keptSlices.length; i++) {
            if (hiddenSlices[i] == null) {
                keptSlices[i] = resourceGeometry.slice(components.get(i)).get();
                kept.add(keptSlices[i]);
            }
        }
        if (kept.isEmpty()) {
            throw new IllegalArgumentException("At least one component must be preserved");
        }
        geometry = CodedGeometry.compound(kept.toArray(CodedGeometry[]::new));
    }

    /**
     * Returns the resource being reduced.
     *
     * @return source resource
     */
    public CodedResource getSource() {
        return resource;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.ofNullable(name);
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return resource.getSampleDimensions();
    }

    @Override
    public CodedGeometry getGridGeometry() throws DataStoreException {
        return geometry;
    }

    @Override
    public CodedCoverage read(CodedGeometry query, int... range) throws DataStoreException {
        query = CodedResources.align(this, query);

        //rebuild a query on all source components, inserting the fixed slices
        final CodedGeometry[] parts = new CodedGeometry[components.size()];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = (hiddenSlices[i] != null) ? hiddenSlices[i] : query.slice(components.get(i)).orElse(keptSlices[i]);
        }
        final CodedCoverage baseCoverage = resource.read(CodedGeometry.compound(parts), range);

        //map reduced grid dimensions to source grid dimensions
        final CodedGeometry baseGeometry = baseCoverage.getGeometry();
        final int[] sourcePosition = new int[baseGeometry.getExtent().getDimension()];
        int[] targetToSource = new int[sourcePosition.length];
        final List<CodedGeometry> kept = new ArrayList<>();
        int sourceDim = 0;
        int targetDim = 0;
        try {
            for (ReferenceSystem component : ReferenceSystems.getSingleComponents(baseGeometry.getReferenceSystem(), true)) {
                final CodedGeometry slice = baseGeometry.slice(component).get();
                final int idx = components.indexOf(component);
                if (idx >= 0 && hiddenSlices[idx] != null) {
                    sourcePosition[sourceDim++] = locate(slice, hiddenSlices[idx]);
                } else {
                    kept.add(slice);
                    for (int i = 0, n = ReferenceSystems.getDimension(component); i < n; i++) {
                        targetToSource[targetDim++] = sourceDim++;
                    }
                }
            }
        } catch (TransformException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
        targetToSource = Arrays.copyOf(targetToSource, targetDim);

        final CodedCoverage subCoverage;
        try {
            subCoverage = new ArrayCodedCoverage(name, CodedGeometry.compound(kept.toArray(CodedGeometry[]::new)),
                    baseCoverage.getSampleDimensions().toArray(SampleDimension[]::new));
        } catch (FactoryException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }

        final CodeIterator source = baseCoverage.createIterator();
        try (WritableCodeIterator iterator = subCoverage.createWritableIterator()) {
            double[] cell = null;
            while (iterator.next()) {
                final int[] position = iterator.getPosition();
                for (int i = 0; i < targetToSource.length; i++) {
                    sourcePosition[targetToSource[i]] = position[i];
                }
                source.moveTo(sourcePosition);
                cell = source.getCell(cell);
                iterator.setCell(cell);
            }
        }
        return subCoverage;
    }

    /**
     * Finds the grid index in the given coverage slice matching the hidden slice.
     */
    private static int locate(CodedGeometry slice, CodedGeometry hidden) throws TransformException {
        final GridExtent extent = slice.getExtent();
        if (extent.getSize(0) == 1) {
            //source returned the requested single slice
            return Math.toIntExact(extent.getLow(0));
        }
        //source returned more then the requested slice, search for it
        final int index = Math.toIntExact(hidden.getExtent().getLow(0));
        return slice.getGridToRS().toGrid(hidden.getGridToRS().toCode(new int[] {index}))[0];
    }
}
