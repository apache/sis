package org.apache.sis.internal.sql.feature;

import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;

import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.test.Assert;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.iso.Names;

import org.junit.Test;

public class FilterInterpreterTest extends TestCase {
    private static final FilterFactory2 FF = new DefaultFilterFactory();

    @Test
    public void testGeometricFilter() {
        final BBOX filter = FF.bbox(FF.property("Toto"), new GeneralEnvelope(new DefaultGeographicBoundingBox(-12.3, 2.1, 43.3, 51.7)));
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
        Filter filter = FF.and(
                FF.greater(FF.property(Names.createGenericName(null, ":", "mySchema", "myTable")), FF.property("otter")),
                FF.or(
                        FF.isNull(FF.property("whatever")),
                        FF.equals(FF.literal(3.14), FF.property("π"))
                )
        );

        assertConversion(filter, "((\"mySchema\".\"myTable\" > \"otter\") AND (\"whatever\" IS NULL OR (3.14 = \"π\")))");
    }

    public void assertConversion(final Filter source, final String expected) {
        final Object result = source.accept(new ANSIInterpreter(), null);
        Assert.assertTrue("Result filter should be a text", result instanceof CharSequence);
        Assert.assertEquals("Filter as SQL condition: ", expected, result.toString());
    }
}
