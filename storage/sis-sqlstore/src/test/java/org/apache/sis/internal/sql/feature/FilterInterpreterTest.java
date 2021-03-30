package org.apache.sis.internal.sql.feature;

import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.SpatialOperator;


public class FilterInterpreterTest extends TestCase {
    private static final FilterFactory<Feature,Object,Object> FF = DefaultFilterFactory.forFeatures();

    @Test
    public void testGeometricFilter() {
        final SpatialOperator<Feature> filter = FF.bbox(FF.property("Toto"),
                new GeneralEnvelope(new DefaultGeographicBoundingBox(-12.3, 2.1, 43.3, 51.7)));
        assertConversion(filter,
                "ST_Intersects(" +
                            "\"Toto\", " +
                            "ST_GeomFromText(" +
                                "'POLYGON ((-12.3 43.3, -12.3 51.7, 2.1 51.7, 2.1 43.3, -12.3 43.3))'" +
                            ")" +
                        ")"
        );
    }

    @Test
    public void testSimpleFilter() {
        Filter<Feature> filter = FF.and(
                FF.greater(FF.property("mySchema/myTable"), FF.property("otter")),
                FF.or(
                        FF.isNull(FF.property("whatever")),
                        FF.equal(FF.literal(3.14), FF.property("π"))
                )
        );
        assertConversion(filter, "((\"mySchema\".\"myTable\" > \"otter\") AND (\"whatever\" IS NULL OR (3.14 = \"π\")))");
    }

    private static void assertConversion(final Filter<Feature> source, final String expected) {
        final StringBuilder sb = new StringBuilder();
        new ANSIInterpreter().visit(source, sb);
        assertEquals(expected, sb.toString());
    }
}
