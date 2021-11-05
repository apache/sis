package org.apache.sis.image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import org.apache.sis.internal.coverage.j2d.WritableTiledImage;
import org.apache.sis.test.FeatureAssert;
import org.apache.sis.util.ArgumentChecks;
import org.junit.Test;

/**
 * TODO: test with big images
 * TODO: when upgrading to Junit 5 use parameterized tests to avoid duplication of test methods
 */
public class MaskedImageTest {

    private static final Polygon CONVEX_POLYGON = new Polygon(
            new int[] {3, 8, 8, 3, 3},
            new int[] {3, 3, 6, 6, 3},
            5);

    private static final Polygon CONCAVE_POLYGON = new Polygon(
            new int[] { 1, 1, 7, 7, 5, 5, 2, 2 },
            new int[] { 1, 6, 6, 3, 3, 4, 4, 1 },
            8);

    private static final int[] BASE_CANVAS = {
            0, 0, 0, 0, 1, 1, 1, 1,
            0, 0, 0, 0, 1, 1, 1, 1,
            0, 0, 0, 0, 1, 1, 1, 1,
            0, 0, 0, 0, 1, 1, 1, 1,
            3, 3, 3, 3, 2, 2, 2, 2,
            3, 3, 3, 3, 2, 2, 2, 2,
            3, 3, 3, 3, 2, 2, 2, 2,
            3, 3, 3, 3, 2, 2, 2, 2
    };

    @Test
    public void noErrorOnEmptyMasks() {
        final BufferedImage source = monoTile();
        final RenderedImage masked = new MaskedImage(source, new Polygon(), true, new Number[]{ 127 });
        FeatureAssert.assertPixelsEqual(source, null, masked, null);
    }

    @Test
    public void fill_MONO_tile_INside_conVEX_polygon() {
        maskInsideConvexPolygon(monoTile());
    }

    @Test
    public void fill_MONO_tile_OUTside_conVEX_polygon() {
        maskOutsideConvexPolygon(monoTile());
    }

    @Test
    public void fill_MULTI_tile_INside_conVEX_polygon() {
        maskInsideConvexPolygon(multiTile());
    }

    @Test
    public void fill_MULTI_tile_OUTside_conVEX_polygon() {
        maskOutsideConvexPolygon(multiTile());
    }

    @Test
    public void fill_MONO_tile_INside_conCAVE_polygon() {
        fillInsideConcavePolygon(monoTile());
    }

    @Test
    public void fill_MONO_tile_OUTside_conCAVE_polygon() {
        fillOutsideConcavePolygon(monoTile());
    }

    @Test
    public void fill_MULTI_tile_INside_conCAVE_polygon() {
        fillInsideConcavePolygon(multiTile());
    }

    @Test
    public void fill_MULTI_tile_OUTside_conCAVE_polygon() {
        fillOutsideConcavePolygon(multiTile());
    }

    private void maskInsideConvexPolygon(RenderedImage source) {
        final RenderedImage masked = new MaskedImage(source, CONVEX_POLYGON, true, new Number[]{ 4 });
        final RenderedImage expected = monoTile(new int[] {
                0, 0, 0, 0, 1, 1, 1, 1,
                0, 0, 0, 0, 1, 1, 1, 1,
                0, 0, 0, 0, 1, 1, 1, 1,
                0, 0, 0, 4, 4, 4, 4, 4,
                3, 3, 3, 4, 4, 4, 4, 4,
                3, 3, 3, 4, 4, 4, 4, 4,
                3, 3, 3, 3, 2, 2, 2, 2,
                3, 3, 3, 3, 2, 2, 2, 2
        });
        FeatureAssert.assertPixelsEqual(expected, null, masked, null);
    }

    private void maskOutsideConvexPolygon(final RenderedImage source) {
        final RenderedImage masked = new MaskedImage(source, CONVEX_POLYGON, false, new Number[]{ 4 });
        final RenderedImage expected = monoTile(new int[] {
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 0, 1, 1, 1, 1,
                4, 4, 4, 3, 2, 2, 2, 2,
                4, 4, 4, 3, 2, 2, 2, 2,
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4
        });
        FeatureAssert.assertPixelsEqual(expected, null, masked, null);
    }

    private void fillInsideConcavePolygon(final RenderedImage source) {
        final RenderedImage masked = new MaskedImage(source, CONCAVE_POLYGON, true, new Number[]{ 4 });
        final RenderedImage expected = monoTile(new int[] {
                0, 0, 0, 0, 1, 1, 1, 1,
                0, 4, 0, 0, 1, 1, 1, 1,
                0, 4, 0, 0, 1, 1, 1, 1,
                0, 4, 0, 0, 1, 4, 4, 1,
                3, 4, 4, 4, 4, 4, 4, 2,
                3, 4, 4, 4, 4, 4, 4, 2,
                3, 3, 3, 3, 2, 2, 2, 2,
                3, 3, 3, 3, 2, 2, 2, 2
        });
        FeatureAssert.assertPixelsEqual(expected, null, masked, null);
    }

    private void fillOutsideConcavePolygon(final RenderedImage source) {
        final RenderedImage masked = new MaskedImage(source, CONCAVE_POLYGON, false, new Number[]{ 4 });
        final RenderedImage expected = monoTile(new int[] {
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 0, 4, 4, 4, 4, 4, 4,
                4, 0, 4, 4, 4, 4, 4, 4,
                4, 0, 4, 4, 4, 1, 1, 4,
                4, 3, 3, 3, 2, 2, 2, 4,
                4, 3, 3, 3, 2, 2, 2, 4,
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4
        });
        FeatureAssert.assertPixelsEqual(expected, null, masked, null);
    }

    private RenderedImage multiTile() {
        return new WritableTiledImage(null, colorPalette(), 8, 8, 0, 0,
                tile(0), tile(1), tile(3), tile(2));
    }

    private WritableRaster tile(int fillValue) {
        final WritableRaster tile = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 4, 4, 1, null);
        final WritablePixelIterator it = new PixelIterator.Builder().createWritable(tile);
        while (it.next()) it.setSample(0, fillValue);
        return tile;
    }

    /**
     * Base colors are shades of blue. The last (fifth) color used for masks is green.
     * @return
     */
    private static IndexColorModel colorPalette() {
        byte[] reds   = { 0,  0,  0,   0,   0 };
        byte[] greens = { 0,  0,  0,   0, 127 };
        byte[] blues  = { 0, 50, 95, 127,   0 };
        return new IndexColorModel(Byte.SIZE, 5, reds, greens, blues);
    }

    /**
     *
     * @return Base test image, based on {@link #colorPalette() indexed color model}.
     */
    private static BufferedImage monoTile() {
        return monoTile(BASE_CANVAS);
    }

    private static BufferedImage monoTile(int[] pixels) {
        ArgumentChecks.ensureDimensionMatches("Input raster must be 8x8", 64, pixels);
        final BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_INDEXED, colorPalette());
        final WritableRaster raster = image.getRaster();
        raster.setPixels(0, 0, 8, 8, pixels);
        return image;
    }

    /**
     * Draw given polygon into source image, to display expected result in debugger.
     */
    private static BufferedImage debugGeometryImage(final Polygon geometry) {
        final BufferedImage source = monoTile();
        final BufferedImage debugImg = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        final Graphics painter = debugImg.getGraphics();
        painter.drawImage(source, 0, 0, null);

        final Rectangle bounds = geometry.getBounds();
        final Rectangle enlargedBounds = new Rectangle(bounds.x - 1, bounds.y - 1, bounds.width + 2, bounds.height + 2);
        final Rectangle intersection = enlargedBounds.intersection(debugImg.getRaster().getBounds());
        painter.setColor(Color.RED);
        painter.fillRect(intersection.x, intersection.y, intersection.width, intersection.height);
        painter.setColor(Color.GREEN);
        painter.fillPolygon(geometry);
        painter.dispose();

        return debugImg;
    }


    /**
     * Useful in IntelliJ: allow to display input image in debugger: Add a new watch calling this method on wanted image.
     * <em>WARNINGS:</em> works only for current test case:
     * <ul>
     *     <li> it assume input image is compatible with test {@link #colorPalette() color palette}.</li>
     *     <li>Work only on single tile images.</li>
     * </ul>
     *
     * @param source The image to display.
     * @return The image directly displayable through debugger.
     */
    private static BufferedImage debug(final RenderedImage source) {
        Raster tile = source.getTile(source.getMinTileX(), source.getMinTileY());
        final int width, height;

        tile = tile.createTranslatedChild(0, 0);
        width = tile.getWidth();
        height = tile.getHeight();

        final BufferedImage view = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorPalette());
        view.getRaster().setRect(tile);

        return view;
    }
}
