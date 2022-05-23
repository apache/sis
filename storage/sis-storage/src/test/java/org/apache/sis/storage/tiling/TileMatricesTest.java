/*
 * (C) 2022, Geomatys
 */
package org.apache.sis.storage.tiling;

import java.util.Map;
import java.util.Optional;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;

import static org.junit.Assert.*;

/**
 * Tests for {@link TileMatrices}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.3
 * @since   1.3
 *
 * @author Johann Sorel (Geomatys)
 */
public final class TileMatricesTest extends TestCase {

    public TileMatricesTest() {
    }

    /**
     * Test tiling scheme detection from tiles.
     */
    @Test
    public void testToTilingScheme() {

        final long tileWidth = 256;
        final long tileHeight = 256;
        final CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
        final double scaleX = 0.1;
        final double scaleY = 0.1;
        final double translateX = 0.0;
        final double translateY = 0.0;

        //tile {0,0}
        final GridGeometry tile00 = new GridGeometry(new GridExtent(tileWidth, tileHeight), PixelInCell.CELL_CORNER,
                new AffineTransform2D(scaleX, 0, 0, scaleY, translateX, translateY), crs);
        //tile {1,0}
        final GridGeometry tile10 = new GridGeometry(new GridExtent(tileWidth, tileHeight), PixelInCell.CELL_CORNER,
            new AffineTransform2D(scaleX, 0, 0, scaleY, translateX+tileWidth*scaleX, translateY), crs);
        //tile {-4,-2} using gridToCRS
        final GridGeometry tilem42 = new GridGeometry(new GridExtent(tileWidth, tileHeight), PixelInCell.CELL_CORNER,
            new AffineTransform2D(scaleX, 0, 0, scaleY, translateX + -4 * tileWidth * scaleX, translateY + -2 * tileHeight * scaleY), crs);
        //tile {-4,-2} using gridExtent
        final GridGeometry tilem42bis = new GridGeometry(new GridExtent(null, new long[]{-4*tileWidth,-2*tileHeight}, new long[]{-3*tileWidth,-1*tileHeight}, false), PixelInCell.CELL_CORNER,
            new AffineTransform2D(scaleX, 0, 0, scaleY, translateX, translateY), crs);

        { // test single tile
            final Optional<Map.Entry<GridGeometry, Map<GridGeometry, long[]>>> result = TileMatrices.toTilingScheme(tile00);
            assertTrue(result.isPresent());
            final GridGeometry tilingScheme = result.get().getKey();
            final Map<GridGeometry, long[]> indices = result.get().getValue();

            final GridGeometry expected = new GridGeometry(new GridExtent(null, null, new long[]{1, 1}, false), PixelInCell.CELL_CORNER,
                    new AffineTransform2D(tileWidth*scaleX, 0, 0, tileHeight*scaleY, 0, 0), crs);
            //assertEquals(expected, tilingScheme);
            assertTrue(expected.equals(tilingScheme));
            assertArrayEquals(new long[]{0,0}, indices.get(tile00));
        }


        { // test with a valid second tile
            final Optional<Map.Entry<GridGeometry, Map<GridGeometry, long[]>>> result = TileMatrices.toTilingScheme(tile00, tile10);
            assertTrue(result.isPresent());
            final GridGeometry tilingScheme = result.get().getKey();
            final Map<GridGeometry, long[]> indices = result.get().getValue();

            final GridGeometry expected = new GridGeometry(new GridExtent(null, null, new long[]{2, 1}, false), PixelInCell.CELL_CORNER,
                    new AffineTransform2D(tileWidth*scaleX, 0, 0, tileHeight*scaleY, 0, 0), crs);
            //assertEquals(expected, tilingScheme);
            assertTrue(expected.equals(tilingScheme));
            assertArrayEquals(new long[]{0,0}, indices.get(tile00));
            assertArrayEquals(new long[]{1,0}, indices.get(tile10));
        }

        { // test with a valid third tile
            final Optional<Map.Entry<GridGeometry, Map<GridGeometry, long[]>>> result = TileMatrices.toTilingScheme(tile00, tile10, tilem42);
            assertTrue(result.isPresent());
            final GridGeometry tilingScheme = result.get().getKey();
            final Map<GridGeometry, long[]> indices = result.get().getValue();

            final GridGeometry expected = new GridGeometry(new GridExtent(null, null, new long[]{6, 3}, false), PixelInCell.CELL_CORNER,
                    new AffineTransform2D(tileWidth*scaleX, 0, 0, tileHeight*scaleY, -4 * tileWidth * scaleX, -2 * tileHeight * scaleY), crs);
            //assertEquals(expected, tilingScheme);
            assertTrue(expected.equals(tilingScheme));
            assertArrayEquals(new long[]{4,2}, indices.get(tile00));
            assertArrayEquals(new long[]{5,2}, indices.get(tile10));
            assertArrayEquals(new long[]{0,0}, indices.get(tilem42));
        }

        { // test with a valid third tile, same as previous test but tile translation is expressed in the grid extent
            final Optional<Map.Entry<GridGeometry, Map<GridGeometry, long[]>>> result = TileMatrices.toTilingScheme(tile00, tile10, tilem42bis);
            assertTrue(result.isPresent());
            final GridGeometry tilingScheme = result.get().getKey();
            final Map<GridGeometry, long[]> indices = result.get().getValue();

            final GridGeometry expected = new GridGeometry(new GridExtent(null, null, new long[]{6, 3}, false), PixelInCell.CELL_CORNER,
                    new AffineTransform2D(tileWidth*scaleX, 0, 0, tileHeight*scaleY, -4 * tileWidth * scaleX, -2 * tileHeight * scaleY), crs);
            //assertEquals(expected, tilingScheme);
            assertTrue(expected.equals(tilingScheme));
            assertArrayEquals(new long[]{4,2}, indices.get(tile00));
            assertArrayEquals(new long[]{5,2}, indices.get(tile10));
            assertArrayEquals(new long[]{0,0}, indices.get(tilem42bis));
        }

        { // test a tile with a different crs
            final GridGeometry badTile = new GridGeometry(new GridExtent(tileWidth, tileHeight), PixelInCell.CELL_CORNER,
                new AffineTransform2D(scaleX, 0, 0, scaleY, translateX+tileWidth*scaleX, translateY), CommonCRS.WGS84.geographic());

            final Optional<Map.Entry<GridGeometry, Map<GridGeometry, long[]>>> result = TileMatrices.toTilingScheme(tile00, badTile);
            assertFalse(result.isPresent());
        }

        { // test a tile with a different size
            final GridGeometry badTile = new GridGeometry(new GridExtent(tileWidth-1, tileHeight), PixelInCell.CELL_CORNER,
                new AffineTransform2D(scaleX, 0, 0, scaleY, translateX+tileWidth*scaleX, translateY), crs);

            final Optional<Map.Entry<GridGeometry, Map<GridGeometry, long[]>>> result = TileMatrices.toTilingScheme(tile00, badTile);
            assertFalse(result.isPresent());
        }

        { // test a tile with a different scale
            final GridGeometry badTile = new GridGeometry(new GridExtent(tileWidth, tileHeight), PixelInCell.CELL_CORNER,
                new AffineTransform2D(scaleX, 0, 0, scaleY+0.001, translateX, translateY), crs);

            final Optional<Map.Entry<GridGeometry, Map<GridGeometry, long[]>>> result = TileMatrices.toTilingScheme(tile00, badTile);
            assertFalse(result.isPresent());
        }

        { // test a tile with a none aligned translation
            final GridGeometry badTile = new GridGeometry(new GridExtent(tileWidth, tileHeight), PixelInCell.CELL_CORNER,
                new AffineTransform2D(scaleX, 0, 0, scaleY, translateX+0.0001, translateY), crs);

            final Optional<Map.Entry<GridGeometry, Map<GridGeometry, long[]>>> result = TileMatrices.toTilingScheme(tile00, badTile);
            assertFalse(result.isPresent());
        }

    }

}
