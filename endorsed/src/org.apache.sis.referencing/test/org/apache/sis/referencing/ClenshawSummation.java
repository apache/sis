/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.referencing;

import org.apache.sis.math.Fraction;
import org.apache.sis.pending.jdk.JDK21;


/**
 * Optimizes a series expansion in sine terms by using the Clenshaw summation technic.
 * This class is not a JUnit test, but rather a tools used for generating the Java code
 * implemented in {@link GeodesicsOnEllipsoid#computeSeriesExpansionCoefficients()}.
 * This class is hosted in the {@code src/test} directory because we do not want it to
 * be included in the main code, and we do not have a directory dedicated to code generators.
 *
 * <p>The Clenshaw summation technic is summarized in Snyder 3-34 and 3-35. References:</p>
 * <ul>
 *   <li>Clenshaw, 1955. <u>A note on the summation of Chebyshev series.</u>
 *       Math Tables Other Aids Comput 9(51):118–120.</li>
 *   <li>Snyder, John P. <u>Map Projections - A Working Manual.</u>
 *       U.S. Geological Survey Professional Paper 1395, 1987.</li>
 * </ul>
 *
 * The idea is to rewrite some equations using trigonometric identities. Given:
 *
 * <pre class="math">
 *     s  =  A⋅sin(θ) + B⋅sin(2θ) + C⋅sin(3θ) + D⋅sin(4θ) + E⋅sin(5θ) + F⋅sin(6θ)</pre>
 *
 * We rewrite as:
 *
 * <pre class="math">
 *     s  =  sinθ⋅(A′ + cosθ⋅(B′ + cosθ⋅(C′ + cosθ⋅(D′ + cosθ⋅(E′ + cosθ⋅F′)))))
 *     A′ =  A - C + E
 *     B′ =  2B - 4D + 6F
 *     C′ =  4C - 12E
 *     D′ =  8D - 32F
 *     E′ = 16E
 *     F′ = 32F</pre>
 *
 * Some calculations were done in an OpenOffice spreadsheet available on
 * <a href="https://svn.apache.org/repos/asf/sis/analysis/Map%20projection%20formulas.ods">Subversion</a>.
 * This class is used for more complex formulas where the use of spreadsheet become too difficult.
 *
 * <h2>Limitations</h2>
 * Current implementation can handle a maximum of 8 terms in the trigonometric series.
 * This limit is hard-coded in the {@link #compute()} method, but can be expanded by using
 * the {@link Precomputation} inner class for generating the {@code compute()} code.
 *
 * <h4>Possible future evolution</h4>
 * Maybe we should generate series expansions automatically for a given precision and eccentricity,
 * adding more terms until the last added term adds a correction smaller than the desired precision.
 * Then we could use {@link Precomputation} for generating the corresponding Clenshaw summation.
 * This improvement would be needed for planetary CRS, where map projections may be applied on planet
 * with higher eccentricity than Earth.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://issues.apache.org/jira/browse/SIS-465">SIS-465</a>
 */
public final class ClenshawSummation {
    /**
     * The coefficients to be multiplied by the sine values.
     * This is the input before Clenshaw summation is applied.
     */
    private final Coefficient[] sineCoefficients;

    /**
     * The coefficients to be multiplied by the cosine values.
     * This is the output after Clenshaw summation is applied.
     */
    private Coefficient[] cosineCoefficients;

    /**
     * Creates a new series expansion to be optimized by Clenshaw summation.
     * Given the following series:
     *
     * <pre class="math">
     *     s  =  A⋅sin(θ) + B⋅sin(2θ) + C⋅sin(3θ) + D⋅sin(4θ) + E⋅sin(5θ) + F⋅sin(6θ)</pre>
     *
     * the arguments given to this constructor shall be A, B, C, D, E and F in that exact order.
     */
    private ClenshawSummation(final Coefficient... coefficients) {
        this.sineCoefficients = coefficients;
    }

    /**
     * A coefficient to be multiplied by the sine or cosine of an angle. For example in the following expression,
     * each of A, B, C, D, E and F variable is a {@code Coefficient} instance.
     *
     * <pre class="math">
     *     s  =  A⋅sin(θ) + B⋅sin(2θ) + C⋅sin(3θ) + D⋅sin(4θ) + E⋅sin(5θ) + F⋅sin(6θ)</pre>
     *
     * Each {@code Coefficient} is itself defined by a sum of terms, for example:
     *
     * <pre class="math">
     *     A  =  -1/2⋅η  +  3/16⋅η³  +  -1/32⋅η⁵</pre>
     */
    private static final class Coefficient {
        /**
         * The terms to add for computing this coefficient. See constructor javadoc for meaning.
         */
        private final Term[] terms;

        /**
         * Creates a new coefficient defined by the sum of the given term. Each term is intended to be multiplied
         * by some η value raised to a different power. Those η values and their powers do not matter for this class
         * provided that all {@code Coefficient} instances associate the same values and powers at the same indices.
         */
        Coefficient(final Term... terms) {
            this.terms = terms;
        }

        /**
         * Creates a new coefficient as the sum of all given coefficients multiplied by the given factors.
         * The {@code toSum[i]} term is multiplied by the {@code factors[i]} at the same index.
         * This method is used for computing B′ in expressions like:
         *
         * <pre class="math">
         *     B′ = 2B - 4D + 6F</pre>
         *
         * @param toSum    the terms to sum, ignoring null elements.
         * @param factors  multiplication factor for each term to sum.
         *        If shorter than {@code toSum} array, missing factors are assumed zero.
         */
        static Coefficient sum(final Coefficient[] toSum, final Fraction[] factors) {
            int n = 0;
            for (final Coefficient t : toSum) {
                n = Math.max(n, t.terms.length);
            }
            boolean hasTerm = false;
            final Term[] terms = new Term[n];
            final Term[] component = new Term[toSum.length];
            for (int i=0; i<n; i++) {
                for (int j=0; j<toSum.length; j++) {
                    final Term[] t = toSum[j].terms;
                    component[j] = (i < t.length) ? t[i] : null;
                }
                hasTerm |= (terms[i] = Term.sum(component, factors)) != null;
            }
            return hasTerm ? new Coefficient(terms) : null;
        }

        /**
         * Formats a string representation in the given buffer.
         */
        final void appendTo(final StringBuilder b) {
            boolean more = false;
            for (int p = terms.length; --p >= 0;) {       // `p` is power minus one.
                final Term t = terms[p];
                if (t != null) {
                    if (more) {
                        b.append("  +  ");
                    }
                    t.appendTo(b);
                    b.append("*η");
                    if (p != 0) {
                        b.append(p+1);
                    }
                    more = true;
                }
            }
        }

        /**
         * Returns a string representation for debugging purpose.
         */
        @Override
        public String toString() {
            if (terms == null) return "";
            final StringBuilder b = new StringBuilder();
            appendTo(b);
            return b.toString();
        }
    }

    /**
     * One term in in the evaluation of a {@link Coefficient}. This term is usually single fraction.
     * For example, a {@code Coefficient} may be defined as below:
     *
     * <pre class="math">
     *     A  =  -1/2⋅η  +  3/16⋅η³  +  -1/32⋅η⁵</pre>
     *
     * In above example each of -1/2, 3/16 and -1/32 fraction is a {@code Term} instance.
     * However, this class allows a term to be defined by an array of fractions if each term
     * is itself defined by another series expansion.
     */
    private static final class Term {
        /**
         * The term, usually a single fraction. If this array contains more than one element,
         * see {@link #Term(Fraction...)} for the meaning.
         */
        private final Fraction[] term;

        /**
         * Creates a new term defined by a single fraction. This is the usual case.
         */
        public Term(final int numerator, final int denominator) {
            term = new Fraction[] {new Fraction(numerator, denominator)};
        }

        /**
         * Creates a new term which is itself defined by another series expansion. Each fraction is assumed multiplied
         * by some value raised to a different power. Those values and powers do not matter for this class provided
         * that all {@code Term} instances associate the same values and powers at the same indices in the given array.
         */
        public Term(final Fraction... term) {
            this.term = term;
        }

        /**
         * Creates a new term as the sum of all given terms multiplied by the given factors.
         * The {@code toSum[i]} term is multiplied by the {@code factors[i]} at the same index.
         * This method is used for computing B′ in expressions like:
         *
         * <pre class="math">
         *     B′ = 2B - 4D + 6F</pre>
         *
         * Note that B, D and F above usually contain many terms, so this method will need to be invoked in a loop.
         *
         * @param toSum    the terms to sum, ignoring null elements.
         * @param factors  multiplication factor for each term to sum.
         *        If shorter than {@code toSum} array, missing factors are assumed zero.
         */
        static Term sum(final Term[] toSum, final Fraction[] factors) {
            int n = 0;
            for (final Term t : toSum) {
                if (t != null) {
                    n = Math.max(n, t.term.length);
                }
            }
            boolean hasTerm = false;
            final Fraction[] term = new Fraction[n];
            for (int i=Math.min(toSum.length, factors.length); --i >= 0;) {
                final Term t = toSum[i];
                final Fraction f = factors[i];
                if (t != null && f != null) {
                    for (int j=t.term.length; --j >= 0;) {
                        Fraction ti = t.term[j];
                        if (ti != null) {
                            ti = ti.multiply(f);
                            Fraction sum = term[j];
                            if (sum == null) {
                                sum = ti;
                            } else if (ti != null) {
                                sum = sum.add(ti);
                            }
                            term[j] = sum;
                            hasTerm = true;
                        }
                    }
                }
            }
            return hasTerm ? new Term(term) : null;
        }

        /**
         * Formats a string representation in the given buffer.
         */
        final void appendTo(final StringBuilder b) {
            if (term.length != 1) b.append('(');
            for (int i=0; i<term.length; i++) {
                final Fraction t = term[i];
                if (i != 0) b.append(" + n*(");
                if (t == null) {
                    b.append('0');
                } else {
                    b.append(t.numerator).append("./").append(t.denominator);
                }
            }
            if (term.length != 1) {
                JDK21.repeat(b, ')', term.length);
            }
        }

        /**
         * Returns a string representation for debugging purpose.
         */
        @Override
        public String toString() {
            if (term == null) return "";
            final StringBuilder b = new StringBuilder();
            appendTo(b);
            return b.toString();
        }
    }

    /**
     * Returns a string representation of this object, for debugging purposes or for showing the result.
     *
     * @return a string representation of this object before or after summation.
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder().append("  ");
        boolean more = false;
        for (int i=0; i<sineCoefficients.length; i++) {
            final Coefficient t = sineCoefficients[i];
            if (t != null) {
                if (more) b.append("+ ");
                t.appendTo(b.append('('));
                b.append(") * sin(");
                if (i != 0) b.append(i+1).append('*');
                b.append("θ)").append(System.lineSeparator());
                more = true;
            }
        }
        if (cosineCoefficients != null) {
            b.append(System.lineSeparator()).append("= sinθ*");
            for (int i=0; i<cosineCoefficients.length; i++) {
                final Coefficient t = cosineCoefficients[i];
                if (i != 0) {
                    b.append(System.lineSeparator()).append(" + cosθ*");
                }
                b.append('(');
                if (t != null) {
                    t.appendTo(b);
                } else {
                    b.append('0');
                }
            }
            JDK21.repeat(b, ')', cosineCoefficients.length);
        }
        return b.toString();
    }

    /**
     * Computes the sum of coefficients multiplied by given factors.
     */
    private Coefficient sum(final int... factors) {
        final Fraction[] f = new Fraction[factors.length];
        for (int i=0; i<f.length; i++) {
            final int fi = factors[i];
            if (fi != 0) {
                f[i] = new Fraction(fi, 1);
            }
        }
        return Coefficient.sum(sineCoefficients, f);
    }

    /**
     * Performs the Clenshaw summation. Current implementation uses hard-coded coefficients for 8 terms.
     * If more terms are needed, use {@link Precomputation} for generating the code of this method.
     *
     * <h4>Possible future evolution</h4>
     * It would be possible to compute those terms dynamically using {@link Precomputation}.
     * It would be useful if a future Apache SIS version generates series expansion on-the-fly
     * for a specified precision and eccentricity.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-465">SIS-465</a>
     */
    private void compute() {
        cosineCoefficients = new Coefficient[] {
            sum(1,  0, -1,  0,   1,   0,  -1      ),        // A′ =    A -    C +   E -  G
            sum(0,  2,  0, -4,   0,   6,   0,   -8),        // B′ =   2B -   4D +  6F - 8H
            sum(0,  0,  4,  0, -12,   0,  24      ),        // C′ =   4C -  12E + 24G
            sum(0,  0,  0,  8,   0, -32,   0,   80),        // D′ =   8D -  32F + 80H
            sum(0,  0,  0,  0,  16,   0, -80      ),        // E′ =  16E -  80G
            sum(0,  0,  0,  0,   0,  32,   0, -192),        // F′ =  32F - 192H
            sum(0,  0,  0,  0,   0,   0,  64      ),        // G′ =  64G
            sum(0,  0,  0,  0,   0,   0,   0,  128),        // H′ = 128H
        };
        if (sineCoefficients.length > cosineCoefficients.length) {
            throw new UnsupportedOperationException("Too many coefficients for current implementation.");
        }
    }

    /**
     * Computes the coefficients that are hard-coded in the {@link #compute()} method.
     * This class needs to be executed only when the maximal number of terms increased.
     * The formula is given by Charles F. F. Karney, Geodesics on an ellipsoid of revolution (2011) equation 59:
     *
     * <blockquote>f(θ) = ∑(aₙ⋅sin(n⋅θ)</blockquote>
     *
     * where the sum is from <var>n</var>=1 to <var>N</var> and <var>N</var> is the maximum number of terms.
     * The equation can be rewritten as:
     *
     * <blockquote>f(θ) = b₁⋅sin(θ)</blockquote>
     *
     * where bₙ =
     * <ul>
     *   <li>0                             for <var>n</var> &gt; <var>N</var>,</li>
     *   <li>aₙ + 2⋅bₙ₊₁⋅cos(θ) − bₙ₊₂     otherwise.</li>
     * </ul>
     *
     * This class computes b₁.
     */
    public static final class Precomputation {
        /**
         * The factors by which to multiply the aₙ terms.
         * The first array index identifies the factors which is multiplied, in reverse order.
         * For example, if there is 4 terms to compute, then the factors are in the following order:
         *
         * <ul>
         *   <li>{@code multiplicands[0]} = factors by which to multiply <var>D</var> (a₄).</li>
         *   <li>{@code multiplicands[1]} = factors by which to multiply <var>C</var> (a₃).</li>
         *   <li>{@code multiplicands[2]} = factors by which to multiply <var>B</var> (a₂).</li>
         *   <li>{@code multiplicands[3]} = factors by which to multiply <var>A</var> (a₁).</li>
         * </ul>
         *
         * The second array index is the power by which to multiply {@code cos(θ)}.
         */
        private final int[][] multiplicands;

        /**
         * Creates a new step in the iteration process for computing the factors.
         * This constructor shall be invoked with decreasing <var>n</var> values,
         * with b₁ computed last.
         *
         * @param  b1  result from the step at <var>n</var> + 1, or {@code null} if zero.
         * @param  b2  result from the step at <var>n</var> + 2, or {@code null} if zero.
         */
        private Precomputation(final Precomputation b1, final Precomputation b2) {
            final int length = (b1 != null) ? b1.multiplicands.length + 1 : 1;
            multiplicands = new int[length][length];
            multiplicands[length - 1][0] = 1;
            if (b2 != null) {
                // Add −bₙ₊₂ — we add terms at the same power of cos(θ), therefore all indices match.
                for (int term=0; term < b2.multiplicands.length; term++) {
                    final int max = b2.multiplicands[term].length;
                    for (int power=0; power<max; power++) {
                        multiplicands[term][power] -= b2.multiplicands[term][power];
                    }
                }
            }
            if (b1 != null) {
                // Add +2⋅bₙ₊₁⋅cos(θ) — because of cos(θ), power indices are offset by one.
                for (int term=0; term < b1.multiplicands.length; term++) {
                    final int max = b1.multiplicands[term].length;
                    for (int power=0; power<max; power++) {
                        multiplicands[term][power+1] += 2*b1.multiplicands[term][power];
                    }
                }
            }
        }

        /**
         * Returns the factors for Clenshaw summation.
         *
         * @return string representation of the factors for Clenshaw summation.
         */
        @Override
        public String toString() {
            final var sb = new StringBuilder();
            for (int power=0; power < multiplicands[0].length; power++) {
                String separator = "sum(";
                for (int term=multiplicands.length; --term >= 0;) {
                    sb.append(separator).append(multiplicands[term][power]);
                    separator = ", ";
                }
                sb.append("),        // ").append((char) ('A' + power)).append("′ = ");
                char symbol = 'A';
                boolean more = false;
                for (int term=multiplicands.length; --term >= 0; symbol++) {
                    int m = multiplicands[term][power];
                    if (m != 0) {
                        if (more) {
                            sb.append(' ').append(m < 0 ? '-' : '+');
                            m = Math.abs(m);
                        }
                        sb.append(' ');
                        if (m != 1) sb.append(m);
                        sb.append(symbol);
                        more = true;
                    }
                }
                sb.append(System.lineSeparator());
            }
            return sb.toString();
        }

        /**
         * Computes and prints the factors for Clenshaw summation with the given number of terms.
         *
         * @param n  the desired number of terms.
         */
        public static void run(int n) {
            Precomputation b2, b1 = null;
            Precomputation result = null;
            while (--n >= 0) {
                b2 = b1;
                b1 = result;
                result = new Precomputation(b1, b2);
                System.out.println(result);
            }
        }
    }

    /**
     * Shortcut for {@link Term#Term(int, int)}.
     */
    private static Term t(final int numerator, final int denominator) {
        return new Term(numerator, denominator);
    }

    /**
     * Returns the series expansion given by Karney (2013) equation 18. This Clenshaw summation is also available in the
     * <a href="https://svn.apache.org/repos/asf/sis/analysis/Map%20projection%20formulas.ods">Open Office spreadsheet</a>,
     * but is repeated here in order to compare results. We use this equation for testing that this class works.
     * The output should be equal to the code in {@link GeodesicsOnEllipsoid#sphericalToEllipsoidalAngle} method,
     * ignoring {@link GeodesicsOnEllipsoid#A1} and (@code σ} constants.
     *
     * @return Karney (2013) equation 18.
     */
    public static ClenshawSummation Karney18() {
        return new ClenshawSummation(
            new Coefficient(t(-1, 2), null,      t( 3, 16), null,       t(-1,   32)              ),     // C₁₁
            new Coefficient(null,     t(-1, 16), null,      t( 1,  32), null,         t(-9, 2048)),     // C₁₂
            new Coefficient(null,     null,      t(-1, 48), null,       t( 3,  256)              ),     // C₁₃
            new Coefficient(null,     null,      null,      t(-5, 512), null,         t( 3,  512)),     // C₁₄
            new Coefficient(null,     null,      null,      null,       t(-7, 1280)              ),     // C₁₅
            new Coefficient(null,     null,      null,      null,       null,         t(-7, 2048))      // C₁₆
        );
    }

    /**
     * Returns the series expansion given by Karney (2013) equation 25. This series is the reason for this
     * {@code ClenshawSummation} class since it is too complex for the OpenOffice spreadsheet mentioned in
     * {@link #Karney18()}.
     *
     * @return Karney (2013) equation 25.
     */
    public static ClenshawSummation Karney25() {
        return new ClenshawSummation(
            new Coefficient(                                                        // C₃₁
                new Term(new Fraction(1,   4), new Fraction(-1,   4)),
                new Term(new Fraction(1,   8), null,                  new Fraction(-1,  8)),
                new Term(new Fraction(3,  64), new Fraction( 3,  64), new Fraction(-1, 64)),
                new Term(new Fraction(5, 128), new Fraction( 1,  64)),
                new Term(new Fraction(3, 128))),
            new Coefficient(                                                        // C₃₂
                null,
                new Term(new Fraction(1,  16), new Fraction(-3,  32), new Fraction( 1, 32)),
                new Term(new Fraction(3,  64), new Fraction(-1,  32), new Fraction(-3, 64)),
                new Term(new Fraction(3, 128), new Fraction( 1, 128)),
                new Term(new Fraction(5, 256))),
            new Coefficient(                                                        // C₃₃
                null, null,
                new Term(new Fraction(5, 192), new Fraction(-3,  64), new Fraction(5, 192)),
                new Term(new Fraction(3, 128), new Fraction(-5, 192)),
                new Term(new Fraction(7, 512))),
            new Coefficient(                                                        // C₃₄
                null, null, null,
                new Term(new Fraction(7, 512), new Fraction(-7, 256)),
                new Term(new Fraction(7, 512))),
            new Coefficient(                                                        // C₃₅
                null, null, null, null,
                new Term(new Fraction(21, 2560))));
    }

    /**
     * Coefficients for Rhumb line calculation from Bennett (1996) equation 2.
     *
     * @return Bennett (1996) equation 2.
     */
    public static ClenshawSummation Bennett2() {
        return new ClenshawSummation(
            new Coefficient(t(-3, 8), t(-3,  32), t(-45, 1024)),
            new Coefficient(null,     t(15, 256), t( 45, 1024)),
            new Coefficient(null,     null,       t(-35, 3072))
        );
    }

    /**
     * Coefficients for Equidistant Cylindrical (EPSG:1028) map projection.
     *
     * @param inverse {@code false} for the forward projection, or {@code true} for the inverse projection.
     *
     * @return Equidistant Cylindrical equation.
     */
    public static ClenshawSummation equidistantCylindrical(final boolean inverse) {
        if (inverse) {
            return new ClenshawSummation(
                new Coefficient(t(3, 2), null, t(-27, 32), null, t(269, 512), null, t(-6607, 24576)),
                new Coefficient(null, t(21, 16), null, t( -55, 32), null, t(6759, 4096)),
                new Coefficient(null, null, t(151, 96), null, t(-417, 128), null, t(87963, 20480)),
                new Coefficient(null, null, null, t(1097, 512), null, t(-15543, 2560)),
                new Coefficient(null, null, null, null, t(8011, 2560), null, t(-69119, 6144)),
                new Coefficient(null, null, null, null, null, t( 293393, 61440)),
                new Coefficient(null, null, null, null, null, null, t(6845701, 860160))
            );
        }
        return new ClenshawSummation(
            new Coefficient(t(-3, 8), t(-3,  32), t(-45, 1024), t(-105,    4096), t(-2205,   131072), t(  -6237,    524288), t(-297297,  33554432)),
            new Coefficient(null,     t(15, 256), t( 45, 1024), t( 525,   16384), t( 1575,    65536), t( 155925,   8388608), t(495495,   33554432)),
            new Coefficient(null,     null,       t(-35, 3072), t(-175,   12288), t(-3675,   262144), t( -13475,   1048576), t(-385385,  33554432)),
            new Coefficient(null,     null,       null,         t( 315,  131072), t( 2205,   524288), t(  43659,   8388608), t(189189,   33554432)),
            new Coefficient(null,     null,       null,         null,             t(-693, 1310720),   t(-6237,  5242880),    t(-297297, 167772160)),
            new Coefficient(null,     null,       null,         null,             null,               t( 1001,  8388608),    t(  11011,  33554432)),
            new Coefficient(null,     null,       null,         null,             null,               null,                  t(  -6435, 234881024))
        );
    }

    /**
     * Computes the Clenshaw summation of a series and display the code to standard output.
     * This method has been used for generating the Java code implemented in methods such as
     * {@link GeodesicsOnEllipsoid#computeSeriesExpansionCoefficients()} and may be executed
     * again if we need to verify the code.
     *
     * @param args ignored.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        ClenshawSummation s = Karney25();
        s.compute();
        System.out.println(s);
    }
}
