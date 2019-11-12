package org.apache.sis.internal.sql.feature;

import org.opengis.filter.spatial.BBOX;

public class PostGISInterpreter extends ANSIInterpreter {

    /**
     * Filter encoding specifies bbox as a filter between envelopes. Default ANSI interpreter performs a standard
     * intersection between geometries, which is not compliant. PostGIS has its own bbox operator:
     * <a href="https://postgis.net/docs/geometry_overlaps.html">Geometry overlapping</a>.
     * @param filter BBox filter specifying properties to compare.
     * @param extraData A context to handle some corner cases. Not used. Can be null.
     * @return A text (sql query) representation of input filter.
     */
    @Override
    public CharSequence visit(BBOX filter, Object extraData) {
        if (filter.getExpression1() == null || filter.getExpression2() == null)
            throw new UnsupportedOperationException("Not supported yet : bbox over all geometric properties");
        return join(filter, "&&", extraData);
    }
}
