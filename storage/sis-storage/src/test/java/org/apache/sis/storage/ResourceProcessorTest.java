package org.apache.sis.storage;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.util.Collections;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.BufferedGridCoverage;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.Interpolation;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.storage.MemoryGridResource;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.iso.Names;
import org.junit.Test;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.util.GenericName;
import org.opengis.util.LocalName;

import static org.apache.sis.referencing.operation.transform.MathTransforms.identity;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ResourceProcessorTest extends TestCase {

    /**
     * Verify that resampling is activated as ordered when inverting CRS axes.
     *
     * Note: the test assertion is implementation specific. We assume that due to the trivial transform in play, the
     * resample will only modify the conversion from grid to space, without changing associated image data.
     */
    @Test
    public void resampleByCrs() throws Exception {
        final LocalName name = Names.createLocalName(null, null, "resample-by-crs");
        final GridCoverageResource resampled = nearestInterpol().resample(grid1234(), HardCodedCRS.WGS84_LATITUDE_FIRST, name);
        GenericName queriedName = resampled.getIdentifier().orElseThrow(() -> new AssertionError("No name defined, but one was provided"));
        assertEquals("resampled resource name", name, queriedName);
        final GridCoverage read = resampled.read(null);
        assertEquals(new AffineTransform2D(0, 1, 1, 0, 0, 0), read.getGridGeometry().getGridToCRS(PixelInCell.CELL_CENTER));
        final RenderedImage rendered = read.render(null);
        assertEquals("Resample dimensions: width", 2, rendered.getWidth());
        assertEquals("Resample dimensions: height", 2, rendered.getHeight());

        final int[] values = rendered.getData().getPixels(0, 0, 2, 2, (int[]) null);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, values);
    }

    /**
     * Force a simple x2 upsampling to ensure that resample is well activated.
     */
    @Test
    public void resampleByGridGeometry() throws Exception {
        final GridCoverageResource source = grid1234();
        final GridGeometry sourceGG = source.getGridGeometry();
        final GridGeometry upsampledGeom = new GridGeometry(new GridExtent(4, 4), sourceGG.getEnvelope(), GridOrientation.HOMOTHETY);
        final GridCoverageResource resampled = nearestInterpol().resample(source, upsampledGeom, null);
        resampled.getIdentifier().ifPresent(name -> fail("Name should be null, but a value was returned: "+name));
        final RenderedImage rendered = resampled.read(null).render(null);
        assertEquals("Resample dimensions: width", 4, rendered.getWidth());
        assertEquals("Resample dimensions: height", 4, rendered.getHeight());

        final int[] values = rendered.getData().getPixels(0, 0, 4, 4, (int[]) null);
        assertArrayEquals(new int[] {
                1, 1, 2, 2,
                1, 1, 2, 2,
                3, 3, 4, 4,
                3, 3, 4, 4,
        }, values);
    }

    /**
     * Create a trivial 2D grid coverage of dimension 2x2. It uses an identity transform for grid to space conversion,
     * and a common WGS84 coordinate reference system, with longitude first.
     */
    private static GridCoverageResource grid1234() {
        GridGeometry domain = new GridGeometry(new GridExtent(2, 2), PixelInCell.CELL_CENTER, identity(2), HardCodedCRS.WGS84);
        SampleDimension band = new SampleDimension.Builder()
                .setBackground(0)
                .addQuantitative("1-based row-major order pixel number", 1, 4, 1, 0, Units.UNITY)
                .build();
        DataBuffer values = new DataBufferInt(new int[] {1, 2, 3, 4}, 4);
        return new MemoryGridResource(null, new BufferedGridCoverage(domain, Collections.singletonList(band), values));
    }

    private static ResourceProcessor nearestInterpol() {
        final ImageProcessor imp = new ImageProcessor();
        imp.setInterpolation(Interpolation.NEAREST);
        return new ResourceProcessor(new GridCoverageProcessor(imp));
    }
}
