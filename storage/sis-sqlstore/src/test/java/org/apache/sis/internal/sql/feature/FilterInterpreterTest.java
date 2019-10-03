package org.apache.sis.internal.sql.feature;

import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;

import org.apache.sis.filter.DefaultFilterFactory;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.test.Assert;

import org.junit.Test;

public class FilterInterpreterTest {
    private static final FilterFactory2 FF = new DefaultFilterFactory();

    @Test
    public void testGeometricFilter() {
        final ANSIInterpreter interpreter = new ANSIInterpreter();
        final BBOX filter = FF.bbox(FF.property("Toto"), new GeneralEnvelope(new DefaultGeographicBoundingBox(-12.3, 2.1, 43.3, 51.7)));
        final Object result = filter.accept(interpreter, null);
        Assert.assertTrue("Result filter should be a text", result instanceof CharSequence);
        Assert.assertEquals(
                "Filter as SQL condition: ",
                "ST_Intersect(" +
                            "ST_Envelope(\"Toto\"), " +
                            "ST_Envelope(" +
                                "ST_GeomFromText(" +
                                    "POLYGON((-12.3 43.3, -12.3 51.7, 2.1 51.7, 2.1 43.3, -12.3 43.3))" +
                                ")" +
                            ")" +
                        ")",
                result.toString()
        );
    }
}
