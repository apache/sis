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
import org.apache.sis.util.StringBuilders;


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
 * {@preformat math
 *     s  =  A⋅sin(θ) + B⋅sin(2θ) + C⋅sin(3θ) + D⋅sin(4θ) + E⋅sin(5θ) + F⋅sin(6θ)
 * }
 *
 * We rewrite as:
 *
 * {@preformat math
 *     s  =  sinθ⋅(A′ + cosθ⋅(B′ + cosθ⋅(C′ + cosθ⋅(D′ + cosθ⋅(E′ + cosθ⋅F′)))))
 *     A′ =  A - C + E
 *     B′ =  2B - 4D + 6F
 *     C′ =  4C - 12E
 *     D′ =  8D - 32F
 *     E′ = 16E
 *     F′ = 32F
 * }
 *
 * Some calculations were done in an OpenOffice spreadsheet available on
 * <a href="https://svn.apache.org/repos/asf/sis/analysis/Map%20projection%20formulas.ods">Subversion</a>.
 * This class is used for more complex formulas where the use of spreadsheet become too difficult.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
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
     * {@preformat math
     *     s  =  A⋅sin(θ) + B⋅sin(2θ) + C⋅sin(3θ) + D⋅sin(4θ) + E⋅sin(5θ) + F⋅sin(6θ)
     * }
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
     * {@preformat math
     *     s  =  A⋅sin(θ) + B⋅sin(2θ) + C⋅sin(3θ) + D⋅sin(4θ) + E⋅sin(5θ) + F⋅sin(6θ)
     * }
     *
     * Each {@code Coefficient} is itself defined by a sum of terms, for example:
     *
     * {@preformat math
     *     A  =  -1/2⋅ε  +  3/16⋅ε³  +  -1/32⋅ε⁵
     * }
     */
    private static final class Coefficient {
        /**
         * The terms to add for computing this coefficient. See constructor javadoc for meaning.
         */
        private final Term[] terms;

        /**
         * Creates a new coefficient defined by the sum of the given term. Each term is intended to be multiplied
         * by some ε value raised to a different power. Those ε values and their powers do not matter for this class
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
         * {@preformat math
         *     B′ = 2B - 4D + 6F
         * }
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
            for (int i=terms.length; --i >= 0;) {
                final Term t = terms[i];
                if (t != null) {
                    if (more) {
                        b.append("  +  ");
                    }
                    t.appendTo(b);
                    b.append(" * ε");
                    if (i != 0) {
                        b.append(i+1);
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
     * For example a {@code Coefficient} may be defined as below:
     *
     * {@preformat math
     *     A  =  -1/2⋅ε  +  3/16⋅ε³  +  -1/32⋅ε⁵
     * }
     *
     * In above example each of -1/2, 3/16 and -1/32 fraction is a {@code Term} instance.
     * However this class allows a term to be defined by an array of fractions if each term
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
         * {@preformat math
         *     B′ = 2B - 4D + 6F
         * }
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
                StringBuilders.repeat(b, ')', term.length);
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
     * Returns a string representation for debugging purpose.
     *
     * @return a string representation of this object before summation.
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
            StringBuilders.repeat(b, ')', cosineCoefficients.length);
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
     * Performs the Clenshaw summation. Current implementation uses hard-coded coefficients for 6 terms.
     * See Karney (2010) equation 59 if generalization to an arbitrary number of coefficients is desired.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-465">SIS-465</a>
     */
    private void compute() {
        if (sineCoefficients.length > 6) {
            throw new UnsupportedOperationException("Too many coefficients for current implementation.");
        }
        cosineCoefficients = new Coefficient[] {
            sum(1,  0, -1,  0,   1    ),            // A′ =   A -   C +  E
            sum(0,  2,  0, -4,   0,  6),            // B′ =  2B -  4D + 6F
            sum(0,  0,  4,  0, -12    ),            // C′ =  4C - 12E
            sum(0,  0,  0,  8,  0, -32),            // D′ =  8D - 32F
            sum(0,  0,  0,  0,  16    ),            // E′ = 16E
            sum(0,  0,  0,  0,  0,  32),            // F′ = 32F
        };
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
