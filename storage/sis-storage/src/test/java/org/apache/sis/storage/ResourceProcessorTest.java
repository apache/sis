package org.apache.sis.storage;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.BufferedGridCoverage;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.image.Interpolation;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.storage.MemoryGridResource;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.Assert;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.iso.Names;
import org.junit.Test;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.util.GenericName;
import org.opengis.util.LocalName;

import static org.apache.sis.referencing.operation.transform.MathTransforms.identity;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

    @Test
    public void aggregateBandsFromSingleBandSources() throws Exception {
        GridCoverageResource first = singleValuePerBand(1);
        GridCoverageResource second = singleValuePerBand(2);

        final GridCoverageResource aggregation = nearestInterpol().aggregateBands(first, second);
        final RenderedImage rendering = aggregation.read(null).render(null);
        assertNotNull(rendering);
        assertArrayEquals(
                new int[] {
                        1, 2, 1, 2,
                        1, 2, 1, 2
                },
                rendering.getData().getPixels(0, 0, 2, 2, (int[]) null)
        );

        assertArrayEquals(
                new int[] {
                        1, 1,
                        1, 1
                },
                aggregation.read(null, 0).render(null).getData().getPixels(0, 0, 2, 2, (int[]) null)
        );


        assertArrayEquals(
                new int[] {
                        2, 2,
                        2, 2
                },
                aggregation.read(null, 1).render(null).getData().getPixels(0, 0, 2, 2, (int[]) null)
        );
    }

    @Test
    public void aggregateBandsFromMultiBandSources() throws Exception {
        GridCoverageResource firstAndSecondBands = singleValuePerBand(1, 2);
        GridCoverageResource thirdAndFourthBands = singleValuePerBand(3, 4);
        GridCoverageResource fifthAndSixthBands  = singleValuePerBand(5, 6);

        GridCoverageResource aggregation = nearestInterpol().aggregateBands(firstAndSecondBands, thirdAndFourthBands, fifthAndSixthBands);
        aggregation.getIdentifier().ifPresent(name -> fail("No name provided at creation, but one was returned: "+name));
        int[] values = aggregation.read(null).render(null).getData().getPixels(0, 0, 2, 2, (int[]) null);
        assertArrayEquals(
                new int[] {
                        1, 2, 3, 4, 5, 6,  1, 2, 3, 4, 5, 6,
                        1, 2, 3, 4, 5, 6,  1, 2, 3, 4, 5, 6,
                },
                values
        );

        values = aggregation.read(null, 1, 2, 4, 5).render(null).getData().getPixels(0, 0, 2, 2, (int[]) null);
        assertArrayEquals(
                new int[] {
                        2, 3, 5, 6,  2, 3, 5, 6,
                        2, 3, 5, 6,  2, 3, 5, 6
                },
                values
        );

        values = aggregation.read(null, 3, 4).render(null).getData().getPixels(0, 0, 2, 2, (int[]) null);
        assertArrayEquals(
                new int[] {
                        4, 5,  4, 5,
                        4, 5,  4, 5
                },
                values
        );

        final LocalName testName = Names.createLocalName(null, null, "test-name");
        aggregation = nearestInterpol().aggregateBands(testName, Arrays.asList(firstAndSecondBands, thirdAndFourthBands, fifthAndSixthBands), Arrays.asList(null, new int[] { 0, 1 }, new int[] { 1 }), null);

        assertEquals(testName, aggregation.getIdentifier().orElse(null));

        values = aggregation.read(null).render(null).getData().getPixels(0, 0, 2, 2, (int[]) null);
        assertArrayEquals(
                new int[] {
                    1, 2, 3, 4, 6,  1, 2, 3, 4, 6,
                    1, 2, 3, 4, 6,  1, 2, 3, 4, 6
                },
                values
        );

        values = aggregation.read(null, 2, 4).render(null).getData().getPixels(0, 0, 2, 2, (int[]) null);
        assertArrayEquals(
                new int[] {
                        3, 6,  3, 6,
                        3, 6,  3, 6
                },
                values
        );
    }

    @Test
    public void testDimensionSelection() throws Exception {
        final GridExtent extent4d = new GridExtent(null, new long[4], new long[]{2, 2, 1, 1}, false);
        final GeneralEnvelope env4d = new GeneralEnvelope(HardCodedCRS.WGS84_4D);
        env4d.setEnvelope(0, 1, 2, 3, 4, 5, 6, 7);
        final GridGeometry domain4d = new GridGeometry(extent4d, env4d, GridOrientation.HOMOTHETY);
        final GridCoverageResource data4d = grid1234(domain4d);

        final GridCoverageResource squeezed = nearestInterpol().squeeze(null, data4d);
        final GridGeometry squeezedDomain = squeezed.getGridGeometry();
        assertEquals("Only 2 dimensions should remain", 2, squeezedDomain.getDimension());
        Assert.assertEqualsIgnoreMetadata(HardCodedCRS.WGS84, squeezedDomain.getCoordinateReferenceSystem());
        GridCoverage loaded = squeezed.read(null);
        assertEquals(squeezedDomain, loaded.getGridGeometry());
        int[] values = loaded.render(null).getData().getPixels(0, 0, 2, 2, (int[]) null);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, values);

        final GridCoverageResource selected = nearestInterpol().selectDimensions(null, data4d, 0, 1, 3);
        final GridGeometry selectedDomain = selected.getGridGeometry();
        assertEquals("Only 3 dimensions should remain", 3, selectedDomain.getDimension());
        Assert.assertEqualsIgnoreMetadata(HardCodedCRS.WGS84_WITH_TIME, selectedDomain.getCoordinateReferenceSystem());
        loaded = selected.read(null);
        assertEquals(selectedDomain, loaded.getGridGeometry());
        values = loaded.render(null).getData().getPixels(0, 0, 2, 2, (int[]) null);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, values);

        final GridCoverageResource removed = nearestInterpol().removeDimensions(null, data4d, 3);
        final GridGeometry removedDomain = removed.getGridGeometry();
        assertEquals("Only 3 dimensions should remain", 3, removedDomain.getDimension());
        Assert.assertEqualsIgnoreMetadata(HardCodedCRS.WGS84_3D, removedDomain.getCoordinateReferenceSystem());
        loaded = removed.read(null);
        assertEquals(removedDomain, loaded.getGridGeometry());
        values = loaded.render(null).getData().getPixels(0, 0, 2, 2, (int[]) null);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, values);
    }

    private static GridCoverageResource singleValuePerBand(int... bandValues) {
        GridGeometry domain = new GridGeometry(new GridExtent(2, 2), PixelInCell.CELL_CENTER, identity(2), HardCodedCRS.WGS84);
        final List<SampleDimension> samples = IntStream.of(bandValues)
                .mapToObj(b -> new SampleDimension.Builder()
                    .setBackground(-1)
                    .addQuantitative("band-value", b, b + 1, 1, 0, Units.UNITY)
                    .build()
                )
                .collect(Collectors.toList());

        DataBuffer values = new DataBufferInt(IntStream.range(0, 4).flatMap(it -> Arrays.stream(bandValues)).toArray(), 4 * bandValues.length);
        return new MemoryGridResource(null, new BufferedGridCoverage(domain, samples, values));
    }


    /**
     * Create a trivial 2D grid coverage of dimension 2x2. It uses an identity transform for grid to space conversion,
     * and a common WGS84 coordinate reference system, with longitude first.
     */
    private static GridCoverageResource grid1234() {
        GridGeometry domain = new GridGeometry(new GridExtent(2, 2), PixelInCell.CELL_CENTER, identity(2), HardCodedCRS.WGS84);
        return grid1234(domain);
    }

    /**
     * Same as {@link #grid1234()}, but allow to override output domain, mostly to allow additional flat dimensions.
     *
     * @param domain A 2D+ domain whose x and y axes (rendering axes) are 2 cells each.
     */
    private static GridCoverageResource grid1234(GridGeometry domain) {
        SampleDimension band = new SampleDimension.Builder()
                .setBackground(0)
                .addQuantitative("1-based row-major order pixel number", 1, 5, 1, 0, Units.UNITY)
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
