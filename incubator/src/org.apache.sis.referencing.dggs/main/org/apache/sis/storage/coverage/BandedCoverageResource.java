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
package org.apache.sis.storage.coverage;

import java.awt.Dimension;
import java.awt.image.DataBufferDouble;
import java.util.List;
import java.util.stream.IntStream;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.coverage.BandedCoverage;
import org.apache.sis.coverage.BandedCoverage.Evaluator;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.util.ArgumentChecks;


/**
 * Experimental interface for banded coverage resources.
 * Waiting for feedback and review before going in SIS.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface BandedCoverageResource extends DataSet {

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     * Sample dimensions contain the following information:
     *
     * <ul class="verbose">
     *   <li>The range of valid <cite>sample values</cite>, typically but not necessarily as positive integers.</li>
     *   <li>A <cite>transfer function</cite> for converting sample values to real values, for example measurements
     *       of a geophysics phenomenon. The transfer function is typically defined by a scale factor and an offset,
     *       but is not restricted to such linear equations.</li>
     *   <li>The units of measurement of "real world" values after their conversions from sample values.</li>
     *   <li>The sample values reserved for missing values.</li>
     * </ul>
     *
     * The returned list should never be empty. If the coverage is an image to be used only for visualization purposes
     * (i.e. the image does not contain any classification data or any measurement of physical phenomenon), then list
     * size should be equal to the {@linkplain java.awt.image.SampleModel#getNumBands() number of bands} in the image
     * and sample dimension names may be "Red", "Green" and "Blue" for instance. Those sample dimensions do not need
     * to contain any {@linkplain SampleDimension#getCategories() category}.
     **
     * @return ranges of sample values together with their mapping to "real values".
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    List<SampleDimension> getSampleDimensions() throws DataStoreException;

    /**
     * Returns the preferred resolutions (in units of CRS axes) for read operations in this data store.
     * If the storage supports pyramid, then the list should contain the resolution at each pyramid level
     * ordered from finest (smallest numbers) to coarsest (largest numbers) resolution.
     * Otherwise the list contains a single element which is the TIN usage resolution,
     * or an empty list if no resolution is not known.
     *
     * <p>Each element shall be an array with a length equals to the number of CRS dimensions.
     * In each array, value at index <var>i</var> is the cell size along CRS dimension <var>i</var>
     * in units of the CRS axis <var>i</var>.</p>
     *
     * <p>Note that arguments given to {@link #subset(CoverageQuery) subset(…)} or {@link #read read(…)} methods
     * are <em>not</em> constrained to the resolutions returned by this method. Those resolutions are only hints
     * about resolution values where read operations are likely to be more efficient.</p>
     *
     * @return preferred resolutions for read operations in this data store, or an empty array if none.
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    default List<double[]> getResolutions() throws DataStoreException {
        return List.of();
    }

    /**
     * Requests a subset of the coverage.
     *
     * No standard queries are defined for {@code BandedCoverageResource} yet.
     * See {@code GridCoverageResource} for specialized queries.
     *
     * <p>The default implementation throws {@link UnsupportedQueryException}.</p>
     *
     * @param  query  definition of domain (grid extent) and range (sample dimensions) filtering applied at reading time.
     * @return resulting coverage resource (never {@code null}).
     * @throws UnsupportedQueryException if this {@code BandedCoverageResource} can not execute the given query.
     *         This includes query validation errors.
     * @throws DataStoreException if another error occurred while processing the query.
     */
    default BandedCoverageResource subset(final Query query) throws UnsupportedQueryException, DataStoreException {
        ArgumentChecks.ensureNonNull("query", query);
        throw new UnsupportedQueryException();
    }

    /**
     * Loads a subset of the coverage represented by this resource. If a non-null grid geometry is specified,
     * then this method will try to return a coverage matching the given grid geometry on a best-effort basis;
     * the coverage actually returned may have a different resolution, cover a different area in a different CRS,
     * <i>etc</i>. The general contract is that the returned coverage should not contain less data than a coverage
     * matching exactly the given geometry.
     *
     * <p>The returned coverage shall contain the exact set of sample dimensions specified by the {@code range} argument,
     * in the specified order (the "best-effort basis" flexibility applies only to the grid geometry, not to the range).
     * All {@code range} values shall be between 0 inclusive and <code>{@linkplain #getSampleDimensions()}.size()</code>
     * exclusive, without duplicated values.</p>
     *
     * <p>While this method name suggests an immediate reading, some implementations may defer the actual reading
     * at a later stage.</p>
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  range   0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the coverage for the specified domain and range.
     * @throws DataStoreException if an error occurred while reading the coverage data.
     */
    BandedCoverage read(GridGeometry domain, int... range) throws DataStoreException;

    /**
     *
     * @param resource
     * @param domain desired grid extent and resolution, not null.
     * @param range 0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return
     * @throws CannotEvaluateException
     * @throws DataStoreException
     */
    public static GridCoverage sample(BandedCoverageResource resource, final GridGeometry domain, int... range) throws CannotEvaluateException, DataStoreException {
        ArgumentChecks.ensureNonNull("resource", resource);
        ArgumentChecks.ensureNonNull("domain", domain);

        try {
            final BandedCoverage coverage = resource.read(domain, range);

            if (coverage instanceof BandedCoverageExt bce) {
                return bce.sample(domain, domain);
            }

            if (coverage.getSampleDimensions().size() != 1) {
                throw new CannotEvaluateException("Only single band sampling supported in current implementation.");
            }

            final GridExtent extent = domain.getExtent();
            final int width = Math.toIntExact(extent.getSize(0));
            final int height = Math.toIntExact(extent.getSize(1));
            final long lowX = extent.getLow(0);
            final long lowY = extent.getLow(1);
            final MathTransform gridToCRS = domain.getGridToCRS(PixelInCell.CELL_CENTER);

            // Verify no overflow is possible before allowing any array
            final int nbPts = Math.multiplyExact(width, height);
            final int xyLength = Math.multiplyExact(nbPts, 2);
            final double[] xyGrid = new double[xyLength];
            final double[] z = new double[nbPts];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = (y * width + x) * 2;
                    xyGrid[idx] = lowX + x;
                    xyGrid[idx+1] = lowY + y;
                }
            }

            //convert to resulting coverage crs
            final double[] xyResult;
            final CoordinateReferenceSystem crs2d = CRS.getHorizontalComponent(resource.getEnvelope().get().getCoordinateReferenceSystem());
            final CoordinateReferenceSystem gridCrs2d = CRS.getHorizontalComponent(domain.getCoordinateReferenceSystem());
            if (!CRS.equivalent(gridCrs2d, crs2d)) {
                MathTransform trs = CRS.findOperation(gridCrs2d, crs2d, null).getMathTransform();
                trs = MathTransforms.concatenate(gridToCRS, trs);
                xyResult = xyGrid;
                trs.transform(xyResult, 0, xyResult, 0, xyResult.length/2);
            } else {
                gridToCRS.transform(xyGrid, 0, xyGrid, 0, xyGrid.length/2);
                xyResult = xyGrid;
            }

            final ThreadLocal<Evaluator> tl = new ThreadLocal<>();
            IntStream.range(0, xyResult.length/2).parallel().forEach((int i) -> {
                Evaluator evaluator = tl.get();
                if (evaluator == null) {
                    evaluator = coverage.evaluator();
                    evaluator.setNullIfOutside(true);
                    tl.set(evaluator);
                }
                final DirectPosition2D dp = new DirectPosition2D();
                dp.x = xyResult[i*2];
                dp.y = xyResult[i*2+1];
                final double[] sample = evaluator.apply(dp);
                if (sample != null) {
                    z[i] = sample[0];
                } else {
                    z[i] = Double.NaN;
                }
            });
            return new GridCoverageBuilder()
                    .setDomain(domain)
                    .setRanges(coverage.getSampleDimensions())
                    .setValues(new DataBufferDouble(z, z.length), new Dimension(width, height))
                    .build();
        } catch (TransformException | FactoryException ex) {
            throw new CannotEvaluateException(ex.getMessage(), ex);
        }
    }

}
