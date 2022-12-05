package org.apache.sis.storage;

import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.util.ComparisonMode;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.util.GenericName;

/**
 * Merge homogeneous {@link GridCoverageResource grid resources} by "stacking" their bands.
 *
 * <h3>Limitation</h3>
 * For now, only datasets with <em>strictly</em> the same {@link GridCoverageResource#getGridGeometry() domain} can be merged.
 *
 * @see ImageProcessor#aggregateBands(List, List, ColorModel)
 */
class BandAggregateGridResource extends MultiSourceGridResource {
    private final List<BandSelection> sources;
    private final GridGeometry domain;
    private final ColorModel userColors;

    BandAggregateGridResource(GenericName name, List<BandSelection> sources, ColorModel userColors) throws DataStoreException {
        super(name);
        this.sources = sources;
        this.domain = verifyDomainEquality(sources);
        this.userColors = userColors;
    }

    @Override
    List<GridCoverageResource> sources() {
        return sources.stream().map(it -> it.data).collect(Collectors.toList());
    }

    @Override
    public GridGeometry getGridGeometry() { return domain; }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return sources.stream()
                .flatMap(BandSelection::selectSampleDimensions)
                .collect(Collectors.toList());
    }

    @Override
    public GridCoverage read(GridGeometry domain, int... ranges) throws DataStoreException {
        if (domain == null) domain = getGridGeometry();
        else domain = getGridGeometry().derive().subgrid(domain).build();

        final List<BandSelection> selection = select(ranges);
        assert !selection.isEmpty();

        final BandSelection firstSelection = selection.get(0);
        final GridCoverage first = firstSelection.data.read(domain, firstSelection.selectedBands);
        if (selection.size() == 1) return first;

        List<GridCoverage> readData = new ArrayList<>(selection.size());
        readData.add(first);
        for (int i = 1 ; i < selection.size() ; i++) {
            final BandSelection source = selection.get(i);
            final GridCoverage data = source.data.read(domain, source.selectedBands);
            if (!data.getGridGeometry().equals(first.getGridGeometry(), ComparisonMode.IGNORE_METADATA)) {
                throw new UnsupportedOperationException("Band aggregation require all source datasets to provide the same domain");
            }
            readData.add(data);
        }

        final List<SampleDimension> outputSamples = readData.stream().flatMap(it -> it.getSampleDimensions().stream()).collect(Collectors.toList());
        return new BandAggregateGridCoverage(domain, outputSamples, readData);
    }

    private GridGeometry verifyDomainEquality(List<BandSelection> sources) throws DataStoreException {
        final GridGeometry first = sources.get(0).data.getGridGeometry();
        for (int i = 1 ; i < sources.size() ; i++) {
            final GridGeometry other = sources.get(i).data.getGridGeometry();
            // TODO: rather than equality, we should check "alignment". It means that the coverage cells should be spatially aligned,
            //  but we should not require their grid extent to use the same offsets.
            if (!first.equals(other, ComparisonMode.IGNORE_METADATA)) {
                throw new IllegalArgumentException("Band merge only allow aligned datasets to be merged. Please resample your resources on a common grid beforehand");
            }
        }

        return first;
    }

    private List<BandSelection> select(int... bands) throws DataStoreException {
        if (bands == null || bands.length < 1) return sources;

        class BandToData {
            final int band; final GridCoverageResource source;

            BandToData(int band, GridCoverageResource source) {
                this.band = band;
                this.source = source;
            }
        }

        List<BandToData> perBandIndex = new ArrayList<>();
        for (BandSelection source : sources) {
            final int[] sourceBands = source.selectedBands == null || source.selectedBands.length < 1
                    ? IntStream.range(0, source.data.getSampleDimensions().size()).toArray()
                    : source.selectedBands;
            for (int i : sourceBands) perBandIndex.add(new BandToData(i, source.data));
        }

        List<BandSelection> consolidated = new ArrayList<>(bands.length);
        int previousIdx = 0;
        BandToData previous = perBandIndex.get(bands[0]);
        // Commodity: to avoid manipulating too many cursors, but also to avoid too many transformations,
        // We use an array with a bigger size than needed to contain temporary source band indices.
        // Its indices match target selected band numbers.
        // Its content is the associated source band number for this target band.
        int[] sourceSelectedBands = new int[bands.length];
        sourceSelectedBands[0] = previous.band;
        for (int i = 1 ; i < bands.length ; i++) {
            int band = bands[i];
            BandToData current = perBandIndex.get(band);
            if (current.source != perBandIndex.get(bands[previousIdx]).source) {
                final int[] sourceBands = Arrays.copyOfRange(sourceSelectedBands, previousIdx, i);
                consolidated.add(new BandSelection(previous.source, sourceBands));
                previous = current;
                previousIdx = i;
            }
            sourceSelectedBands[i] = current.band;
        }

        consolidated.add(new BandSelection(previous.source, Arrays.copyOfRange(sourceSelectedBands, previousIdx, sourceSelectedBands.length)));

        return consolidated;
    }

    private class BandAggregateGridCoverage extends GridCoverage {

        private final List<GridCoverage> sources;

        protected BandAggregateGridCoverage(GridGeometry domain, List<? extends SampleDimension> ranges, List<GridCoverage> sources) {
            super(domain, ranges);
            this.sources = sources;
            assert sources != null && sources.size() > 1;
        }


        @Override
        public RenderedImage render(GridExtent sliceExtent) throws CannotEvaluateException {
            final List<RenderedImage> sourceImages = sources.stream()
                    .map(it -> it.render(sliceExtent))
                    .collect(Collectors.toList());
            // TODO: parent resource should keep a reference to the resource processor that created it.
            // Then, we should retrieve the embedded image processor and use it, instead of using a fresh image processor.
            // However, that require an API change somewhere, and I do not know where yet.
            return new ImageProcessor().aggregateBands(sourceImages, null, userColors);
        }
    }

    static class BandSelection {
        final GridCoverageResource data;
        final int[] selectedBands;
        final List<SampleDimension> samples;

        BandSelection(GridCoverageResource data, int[] selectedBands) throws DataStoreException {
            this.data = data;
            this.selectedBands = selectedBands;
            this.samples = data.getSampleDimensions();
            if (selectedBands != null) {
                for (int band : selectedBands) {
                    if (band >= samples.size()) throw new IllegalArgumentException("Provided band selection is invalid. Input data provide only "+samples.size()+" bands, but band "+band+" was requested");
                }
            }
        }

        Stream<SampleDimension> selectSampleDimensions() {
            if (selectedBands == null || selectedBands.length < 1) return samples.stream();
            return Arrays.stream(selectedBands).mapToObj(samples::get);
        }
    }
}
