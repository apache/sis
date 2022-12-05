package org.apache.sis.image;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BandedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.apache.sis.internal.coverage.j2d.ColorModelFactory;
import org.apache.sis.util.ArgumentChecks;

import static java.lang.Math.multiplyExact;

final class BandAggregateImage extends ComputedImage {

    private final Specification spec;

    private BandAggregateImage(Specification spec) {
        super(spec.tileModel, spec.sources.stream().map(TileSource::getSource).toArray(RenderedImage[]::new));
        this.spec = spec;
    }

    @Override
    protected Raster computeTile(int tileX, int tileY, WritableRaster previous) throws Exception {
        final Point tileLocation = tileToPixel(tileX, tileY);
        return spec.strategy.aggregate(tileX, tileY, tileLocation, spec);
    }

    @Override
    public ColorModel getColorModel() { return spec.aggregationColors; }

    @Override
    public int getWidth() {
        return spec.pixelRegion.width;
    }

    @Override
    public int getHeight() {
        return spec.pixelRegion.height;
    }

    @Override
    public int getMinX() { return spec.pixelRegion.x; }

    @Override
    public int getMinY() { return spec.pixelRegion.y; }

    @Override
    public int getMinTileX() { return spec.tileDisposition.x; }

    @Override
    public int getMinTileY() { return spec.tileDisposition.y; }

    @Override
    public int getNumXTiles() { return spec.tileDisposition.width; }

    @Override
    public int getNumYTiles() { return spec.tileDisposition.height; }

    private Point tileToPixel(int tileX, int tileY) {
        int pX = this.getMinX() + multiplyExact((tileX - getMinTileX()), getTileWidth());
        int pY = this.getMinY() + multiplyExact((tileY - getMinTileY()), getTileHeight());
        return new Point(pX, pY);
    }

    /*
     * FACTORY METHODS
     */

    static RenderedImage aggregateBands(RenderedImage[] sources, int[][] bandsToPreserve, ColorModel userColorModel) {
        final ContextInformation info = parseAndValidateInput(sources, bandsToPreserve, userColorModel);
        return tryTileOptimizedStrategy(info)
                .rightOr(reason
                        -> fallbackStrategy(info)
                        .mapLeft(otherReason -> "Reasons: " + reason + ", " + otherReason)
                )
                .mapRight(BandAggregateImage::new)
                .getRightOrThrow(IllegalArgumentException::new);
    }

    /*
     * PRIVATE STATIC METHODS
     */

    /**
     * Initial analysis of input images to aggregate. Note that this method aims to make source information more
     * accessible and easy to use before further processing. It also try to detect incompatibilities early, to
     * raise meaningful errors for users.
     * <p>
     * Note: crunching data into a more dense/accessible shape aims to ease further analysis/optimisations. This should
     * allow more lisible and less coupled code, to ease setup of strategies, readability and maintenance.
     *
     * @param sources         images to aggregate, in order.
     * @param bandsToPreserve Bands to use for each image, in order. Holds same contract as the {@link #aggregateBands(RenderedImage[], int[][], ColorModel) factory method}.
     * @param userColorModel
     * @return Parsed information about data sources.
     * @throws IllegalArgumentException If we detect an incompatibility in source images that make them impossible to merge.
     */
    private static ContextInformation parseAndValidateInput(RenderedImage[] sources, int[][] bandsToPreserve, ColorModel userColorModel) throws IllegalArgumentException {
        if (bandsToPreserve != null && sources.length > bandsToPreserve.length) throw new IllegalArgumentException("More band selections than source images are provided.");
        if (sources.length < 2) throw new IllegalArgumentException("At least two images are required for band aggregation. For band selection on a single image, please use dedicated utility");

        List<Rectangle> domains = new ArrayList<>(sources.length);
        List<SourceSelection> sourcesWithBands = new ArrayList<>(sources.length);
        int commonDataType, minTileWidth, minTileWidthIdx, minTileHeight, minTileHeightIdx;
        RenderedImage source = sources[0];
        int[] bands = bandsToPreserve == null || bandsToPreserve.length < 1 ? null : bandsToPreserve[0];
        SampleModel sourceSM = source.getSampleModel();
        commonDataType = sourceSM.getDataType();
        int numBands = validateAndCountBands(bands, sourceSM);
        sourcesWithBands.add(new SourceSelection(source, bands));
        minTileWidthIdx = minTileHeightIdx = 0;
        minTileWidth = source.getTileWidth();
        minTileHeight = source.getTileHeight();
        domains.add(new Rectangle(source.getMinX(), source.getMinY(), source.getWidth(), source.getHeight()));
        for (int i = 1 ; i < sources.length ; i++) {
            source = sources[i];
            sourceSM = source.getSampleModel();
            int dataType = sourceSM.getDataType();
            if (dataType != commonDataType) throw new IllegalArgumentException("Images to merge define different data types. This is not supported. Please align all images on a single data-type beforehand.");

            bands = bandsToPreserve == null || bandsToPreserve.length <= i ? null : bandsToPreserve[i];
            numBands += validateAndCountBands(bands, sourceSM);

            sourcesWithBands.add(new SourceSelection(source, bands));

            domains.add(new Rectangle(source.getMinX(), source.getMinY(), source.getWidth(), source.getHeight()));

            if (minTileWidth  > source.getTileWidth()) {
                minTileWidth = source.getTileWidth();
                minTileWidthIdx  = i;
            }
            if (minTileHeight > source.getTileHeight()) {
                minTileHeight = source.getTileHeight();
                minTileHeightIdx = i;
            }
        }

        // TODO: with current information, we should be able to adapt domain definition to a user configuration : intersection, union, strict.
        // User could specify if he wants band aggregation image to cover the intersection or union of input images.
        // "strict" mode would serve to prevent band aggregation on images using different domains (raise an error if domains are not the same).
        // For now, we will use intersection.
        final Rectangle intersection = domains.stream()
                .reduce(Rectangle::intersection)
                .filter(it -> !it.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("source images do not intersect."));

        return new ContextInformation(commonDataType, numBands, minTileWidthIdx, minTileHeightIdx, domains, intersection, sourcesWithBands, userColorModel);
    }

    private static int validateAndCountBands(int[] bandSelection, SampleModel model) {
        final int numBands = model.getNumBands();
        if (bandSelection == null || bandSelection.length < 1) return numBands;
        for (int band : bandSelection) ArgumentChecks.ensureValidIndex(numBands, band);
        return bandSelection.length;
    }

    private static Either<String, Specification> tryTileOptimizedStrategy(ContextInformation context) {
        final RenderedImage tilingX = context.sources.get(context.minTileWidthIndex).image;
        final int tileWidth = tilingX.getTileWidth();
        final int minTileX = tilingX.getMinTileX();

        final  RenderedImage tilingY = context.sources.get(context.minTileHeightIndex).image;
        final int tileHeight = tilingY.getTileHeight();
        final int minTileY = tilingY.getMinTileY();
        for (final SourceSelection source : context.sources) {
            final RenderedImage img = source.image;
            if (img.getNumXTiles() > 1 && img.getTileWidth() % tileWidth != 0) {
                return Either.left("no common integer multiplier between sources for tile width");
            }
            else if (img.getNumYTiles() > 1 && img.getTileHeight() % tileHeight != 0) {
                return Either.left("no common integer multiplier between sources for tile height");
            }

            if ((img.getMinX() - tilingX.getMinX()) % tileWidth != 0) {
                return Either.left("Tiles are not aligned on X axis");
            }

            if ((img.getMinY() - tilingX.getMinY()) % tileHeight != 0) {
                return Either.left("Tiles are not aligned on Y axis");
            }
        }

        // Ensure domain covers entire tiles.
        final Rectangle pixelDomain = context.intersection;
        assert pixelDomain.width  % tileWidth  == 0 : "Computed pixel domain does not align with tile borders";
        assert pixelDomain.height % tileHeight == 0 : "Computed pixel domain does not align with tile borders";

        TileAlignedSource[] preparedSources = new TileAlignedSource[context.sources.size()];
        for (int i = 0 ; i < preparedSources.length ; i++) {
            final SourceSelection source = context.sources.get(i);
            preparedSources[i] = new TileAlignedSource(source.image, source.bands);
        }

        final SampleModel tileModel = new BandedSampleModel(context.commonDataType, tileWidth, tileHeight, context.outputBandNumber);

        Rectangle tileDisposition = new Rectangle(minTileX, minTileY, pixelDomain.width / tileWidth, pixelDomain.height / tileHeight);
        ColorModel outColorModel = context.userColorModel;
        if (outColorModel == null) outColorModel = createColorModel(context);
        else if (!context.userColorModel.isCompatibleSampleModel(tileModel)) {
            throw new IllegalArgumentException("User color model is not compatible with band aggregation sample model. Please provide a banded color model.");
        }

        return Either.right(new Specification(Collections.unmodifiableList(Arrays.asList(preparedSources)), outColorModel, tileModel, pixelDomain, tileDisposition, new TileCopy()));
    }

    /**
     * Approximate guess of the output color model:
     * <ol>
     *     <li>
     *         If aggregation result is 3 or 4 bands, and data type is byte or short, we create a RGB color model.
     *         If there's 4 bands, an RGBA color model is defined.
     *     </li>
     *     <li>Otherwise, if the first image is already single banded, we return directly its color model (if non null)</li>
     *     <li>As a last resort, a greyscale color model is made, that try to "guess" value range from the data-type.</li>
     * </ol>
     */
    private static ColorModel createColorModel(ContextInformation context) {
        if (context.outputBandNumber == 3 || context.outputBandNumber == 4) {
            switch (context.commonDataType) {
                case DataBuffer.TYPE_BYTE:
                case DataBuffer.TYPE_SHORT:
                    return ColorModelFactory.createRGB(context.commonDataType * Byte.SIZE, false, context.outputBandNumber == 4);
            }
        }

        final SourceSelection first = context.sources.get(0);
        if (first.image.getSampleModel().getNumBands() == 1 && first.image.getColorModel() != null) {
            return first.image.getColorModel();
        }

        final double vmin, vmax;
        switch (context.commonDataType) {
            case DataBuffer.TYPE_BYTE:   vmin = 0               ; vmax = 255               ; break;
            case DataBuffer.TYPE_SHORT:  vmin = Short.MIN_VALUE ; vmax = Short.MAX_VALUE   ; break;
            case DataBuffer.TYPE_USHORT: vmin = 0               ; vmax = 65535             ; break;
            case DataBuffer.TYPE_INT:    vmin = 0               ; vmax = Integer.MAX_VALUE ; break;
            default:                     vmin = 0.0             ; vmax = 1.0;
        }

        return ColorModelFactory.createGrayScale(context.commonDataType, 1, 0, vmin, vmax);
    }

    private static Either<String, Specification> fallbackStrategy(ContextInformation info) {
        return Either.left("No fallback strategy yet");
    }

    /**
     * INTERNAL TYPES
     */

    private static final class SourceSelection {
        final RenderedImage image;
        final int[] bands;

        private SourceSelection(RenderedImage image, int[] bands) {
            this.image = image;
            this.bands = bands == null ? null : bands.clone();
        }
    }

    private static class ContextInformation {
        /**
         * DataBuffer type used by all sources without exception. If sources used different datatypes, this is -1.
         */
        final int commonDataType;
        /**
         * Number of bands resulting from the image merge.
         */
        final int outputBandNumber;

        /**
         * Index of source image providing minimal tile <em>width</em>
         */
        final int minTileWidthIndex;

        /**
         * Index of source image providing minimal tile <em>height</em>
         */
        final int minTileHeightIndex;

        /**
         * Commodity attribute: provides source images <em>pixel</em> boundaries, in order.
         */
        final List<Rectangle> sourcePxDomains;

        /**
         * Intersection of all {@link #sourcePxDomains source pixel domains}.
         */
        private final Rectangle intersection;

        final List<SourceSelection> sources;

        final ColorModel userColorModel;

        public ContextInformation(int commonDataType, int outputBandNumber, int minTileWidthIndex, int minTileHeightIndex, List<Rectangle> sourcePxDomains, Rectangle intersection, List<SourceSelection> sources, ColorModel userColorModel) {
            this.commonDataType = commonDataType;
            this.outputBandNumber = outputBandNumber;
            this.minTileWidthIndex = minTileWidthIndex;
            this.minTileHeightIndex = minTileHeightIndex;
            this.sourcePxDomains = Collections.unmodifiableList(new ArrayList<>(sourcePxDomains));
            this.intersection = intersection;
            this.sources = Collections.unmodifiableList(new ArrayList<>(sources));
            this.userColorModel = userColorModel;
        }
    }

    static class Specification {
        final List<TileSource> sources;
        final ColorModel aggregationColors;
        final SampleModel tileModel;

        final Rectangle pixelRegion;
        final Rectangle tileDisposition;
        final BandAggregationStrategy strategy;

        public Specification(List<TileSource> sources, ColorModel aggregationColors, SampleModel tileModel, Rectangle pixelRegion, Rectangle tileDisposition, BandAggregationStrategy strategy) {
            this.sources = sources;
            this.aggregationColors = aggregationColors;
            this.tileModel = tileModel;
            this.pixelRegion = pixelRegion;
            this.tileDisposition = tileDisposition;
            this.strategy = strategy;
        }
    }

    /**
     * For now, only available tile source is "aligned", i.e. sources providing tiles covering computed image tiles.
     * In the future, another implementation capable of providing Raster built from multiple crossing tiles could be
     * done, to extend band aggregation capabilities.
     */
    interface TileSource {
        RenderedImage getSource();

        /**
         * Return a raster aligned with <em>parent computed tile</em> described by input offset.
         * @param computedTileX X coordinate of the <em>parent computed tile</em>.
         * @param computedTileY Y coordinate of the <em>parent computed tile</em>.
         * @return A raster whose domain match output computed tile.
         */
        Raster getTile(int computedTileX, int computedTileY, Rectangle computedTileRegion);
    }

    /**
     * WARNING: only work with sources whose tile size is a multiple of computed image tiles, i.e: All and any tiles in
     * computed images MUST be contained in one and only one source tile.
     */
    private static class TileAlignedSource implements TileSource {

        private final RenderedImage source;

        /**
         * Null if all bands in the image are selected. Otherwise,ordered (not necessarily sorted) indices of selected bands.
         */
        private final int[] selectedBands;

        public TileAlignedSource(RenderedImage source, int[] selectedBands) {
            this.source = source;
            this.selectedBands = selectedBands;
        }

        @Override
        public RenderedImage getSource() {
            return source;
        }

        @Override
        public Raster getTile(int computedTileX, int computedTileY, Rectangle computedTileRegion) {
            final double tX = source.getMinTileX() + (computedTileRegion.x - source.getMinX()) / (double) source.getTileWidth();
            final double tY = source.getMinTileY() + (computedTileRegion.y - source.getMinY()) / (double) source.getTileHeight();
            assert regionsAligned(computedTileRegion, new Point2D.Double(tX, tY))
                    : "Source tiles do not align with computed image tiles";
            final Raster sourceTile = source.getTile((int) Math.floor(tX), (int)  Math.floor(tY));
            return sourceTile.createChild(computedTileRegion.x, computedTileRegion.y, computedTileRegion.width, computedTileRegion.height, computedTileRegion.x, computedTileRegion.y, selectedBands);
        }

        private boolean regionsAligned(Rectangle computedTileRegion, Point2D origin) {
            final double sourceX = origin.getX() * source.getTileWidth() + source.getTileGridXOffset();
            final double sourceY = origin.getY() * source.getTileHeight() + source.getTileGridYOffset();
            return Math.abs(sourceX - computedTileRegion.x) < 1 && Math.abs(sourceY - computedTileRegion.y) < 1;
        }
    }

    @FunctionalInterface
    interface BandAggregationStrategy {
        Raster aggregate(int tileX, int tileY, Point tilePixelOrigin, BandAggregateImage.Specification spec);
    }

    private static class TileCopy implements BandAggregationStrategy {

        @Override
        public Raster aggregate(int tileX, int tileY, Point tilePixelOrigin, BandAggregateImage.Specification spec) {
            final WritableRaster output = WritableRaster.createWritableRaster(spec.tileModel, tilePixelOrigin);
            int i = 0;
            for (TileSource source : spec.sources) {
                Raster sourceRaster = source.getTile(tileX, tileY, new Rectangle(tilePixelOrigin, new Dimension(spec.tileModel.getWidth(), spec.tileModel.getHeight())));
                final int sourceNumBands = sourceRaster.getNumBands();
                final int[] targetBands = IntStream.range(i, i + sourceNumBands).toArray();
                WritableRaster outputBandsOfInterest = output.createWritableChild(output.getMinX(), output.getMinY(), output.getWidth(), output.getHeight(), output.getMinX(), output.getMinY(), targetBands);
                outputBandsOfInterest.setRect(sourceRaster);
                i += sourceNumBands;
            }

            assert i == spec.tileModel.getNumBands() : "Mismatch: unexpected number of transferred bands";

            return output;
        }
    }

    private static class Either<L, R> {

        private final L left;
        private final R right;

        Either(L left, R right) {
            if (left == null && right == null || left != null && right != null) throw new IllegalArgumentException("Expect exactly one non-null branch");
            this.left = left;
            this.right = right;
        }

        <V> Either<L, V> mapRight(Function<? super R, ? extends V> transformRightToValue) { return left == null ? right(transformRightToValue.apply(right)) : (Either<L, V>) this; }
        <V> Either<V, R> mapLeft(Function<? super L, ? extends V> transformLeftToValue) { return right == null ? left(transformLeftToValue.apply(left)) : (Either<V, R>) this; }
        public Either<L, R> rightOr(Function<? super L, Either<L, R>> recover) { return right == null ? recover.apply(left) : this; }
        public <E extends Exception> R getRightOrThrow(Function<L, E> createError) throws E { if (right != null) return right; else throw createError.apply(left); }
        static <L, R> Either<L, R> left(L leftValue) { return new Either<>(leftValue, null); }
        static <L, R> Either<L, R> right(R rightValue) { return new Either<>(null, rightValue); }
    }
}
