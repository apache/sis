package org.apache.sis.internal.feature;

public enum WrapResolution {
    /**
     * Convert the coordinates without checking the antemeridian.
     * If the envelope crosses the antemeridian (lower corner values {@literal >} upper corner values)
     * the created polygon will be wrong since it will define a different area then the envelope.
     * Use this method only knowing the envelopes do not cross the antemeridian.
     *
     * Example :
     * ENV(+170 +10,  -170 -10)
     * POLYGON(+170 +10,  -170 +10,  -170 -10,  +170 -10,  +170 +10)
     *
     */
    NONE,
    /**
     * Convert the coordinates checking the antemeridian.
     * If the envelope crosses the antemeridian (lower corner values {@literal >} upper corner values)
     * the created polygon will go from axis minimum value to axis maximum value.
     * This ensure the create polygon contains the envelope but is wider.
     *
     * Example :
     * ENV(+170 +10,  -170 -10)
     * POLYGON(-180 +10,  +180 +10,  +180 -10,  -180 -10,  -180 +10)
     */
    EXPAND,
    /**
     * Convert the coordinates checking the antemeridian.
     * If the envelope crosses the antemeridian (lower corner values {@literal >} upper corner values)
     * the created polygon will be cut in 2 polygons on each side of the coordinate system.
     * This ensure the create polygon exactly match the envelope but with a more
     * complex geometry.
     *
     * Example :
     * ENV(+170 +10,  -170 -10)
     * MULTI-POLYGON(
     *     (-180 +10,  -170 +10,  -170 -10,  -180 -10,  -180 +10)
     *     (+170 +10,  +180 +10,  +180 -10,  +170 -10,  +170 +10)
     * )
     */
    SPLIT,
    /**
     * Convert the coordinates checking the antemeridian.
     * If the envelope crosses the antemeridian (lower corner values {@literal >} upper corner values)
     * the created polygon coordinate will increase over the antemeridian making
     * a contiguous geometry.
     *
     * Example :
     * ENV(+170 +10,  -170 -10)
     * POLYGON(+170 +10,  +190 +10,  +190 -10,  +170 -10,  +170 +10)
     */
    CONTIGUOUS
}
