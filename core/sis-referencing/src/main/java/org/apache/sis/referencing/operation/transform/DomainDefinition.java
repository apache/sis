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
package org.apache.sis.referencing.operation.transform;

import java.util.Optional;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;


/**
 * Specification about how to estimate a domain of validity for transforms.
 * Contrarily to {@linkplain CRS#getDomainOfValidity(CoordinateReferenceSystem) CRS domain of validity},
 * this class estimates a domain based on mathematical behavior only, not on "real world" considerations.
 * For example the Mercator projection tends to infinity when approaching poles, so it is recommended to
 * not use it above some latitude threshold, typically 80° or 84°. The exact limit is arbitrary.
 * This is different than the domain of validity of CRS, which is often limited to a particular country.
 * In general, the CRS domain of validity is much smaller than the domain computed by this class.
 *
 * <p>Current implementation does not yet provide ways to describe how a domain is decided.
 * A future version may, for example, allows to specify a maximal deformation tolerated for map projections.
 * In current implementation, the estimation can be customized by overriding the
 * {@link #estimate(MathTransform)} or {@link #intersect(Envelope)} methods.</p>
 *
 * <p>Each {@code DomainDefinition} instance should be used only once for an {@link AbstractMathTransform}
 * instance, unless that transform is a chain of concatenated transforms (this case is handled automatically
 * by Apache SIS). Usage example:</p>
 *
 * {@preformat java
 *     AbstractMathTransform transform = …;
 *     transform.getDomain(new DomainDefinition()).ifPresent((domain) -> {
 *         // Do something here with the transform domain.
 *     });
 * }
 *
 * The {@link MathTransforms#getDomain(MathTransform)} convenience method can be used
 * when the default implementation is sufficient.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 *
 * @see MathTransforms#getDomain(MathTransform)
 * @see AbstractMathTransform#getDomain(DomainDefinition)
 * @see org.opengis.referencing.operation.CoordinateOperation#getDomainOfValidity()
 *
 * @since 1.3
 * @module
 */
public class DomainDefinition {
    /**
     * Limits computed so far, or {@code null} if none.
     */
    private Envelope limits;

    /**
     * The envelope to use for computing intersection, created only if needed.
     */
    private GeneralEnvelope intersection;

    /**
     * If the transform to evaluate is a step in the middle of a chain of transforms,
     * the transform to apply on the envelope computed by the step in order to get an
     * envelope in domain units.
     */
    private ToDomain stepToDomain;

    /**
     * The transform to apply on the envelope computed by a transform step in order to get an envelope
     * in the units of the requested domain. This is a node in a linked list, because there is potentially
     * two or more transforms to concatenate if the transform chain is long.
     *
     * <p>This node lazily creates the concatenated transform when first requested, because it
     * is needed only if an {@link #estimate(MathTransform)} call returned a non-empty value.</p>
     */
    private static final class ToDomain {
        /** The first transform to apply on the envelope. */
        private final MathTransform step;

        /** The second transform to apply on the envelope, or {@code null} if none. */
        private final ToDomain next;

        /** Concatenation of {@link #step} followed by {@loink #next}, computed when first needed. */
        private MathTransform concatenation;

        /**
         * Creates a new node in a chain of transform to potentially concatenate.
         *
         * @param  step  first transform to apply on the envelope.
         * @param  next  second transform to apply on the envelope, or {@code null} if none.
         */
        ToDomain(final MathTransform step, final ToDomain next) {
            this.step = step;
            this.next = next;
        }

        /**
         * Returns the transform to apply on domain envelope computed by a transform step.
         * This is the concatenation of {@link #step} followed by all other steps that have
         * been encountered while traversing a chain of transforms.
         */
        MathTransform concatenation() {
            if (concatenation == null) {
                if (next == null) {
                    concatenation = step;
                } else {
                    concatenation = MathTransforms.concatenate(step, next.concatenation());
                }
            }
            return concatenation;
        }
    }

    /**
     * Creates a new instance using default configuration.
     */
    public DomainDefinition() {
    }

    /**
     * Estimates the domain of the given math transform and intersects it with previously computed domains.
     * The result can be obtained by a call to {@link #result()}.
     *
     * <p>The default implementation invokes {@link AbstractMathTransform#getDomain(DomainDefinition)} if possible,
     * or does nothing otherwise. The domain provided by the transform is given to {@link #intersect(Envelope)}.
     * Subclasses can override for modifying this behavior.</p>
     *
     * @param  evaluated  the transform for which to estimate the domain.
     * @throws TransformException if the domain can not be estimated.
     */
    public void estimate(final MathTransform evaluated) throws TransformException {
        if (evaluated instanceof AbstractMathTransform) {
            Envelope domain = ((AbstractMathTransform) evaluated).getDomain(this).orElse(null);
            if (domain != null) {
                if (stepToDomain != null) {
                    domain = Envelopes.transform(stepToDomain.concatenation(), domain);
                }
                intersect(domain);
            }
        }
    }

    /**
     * Estimates the domain using the inverse of a transform.
     * The input ranges of original transform is the output ranges of inverse transform.
     * Using the inverse is convenient because {@link ConcatenatedTransform#transform2}
     * contains all transform steps down to the end of the chain. By contrast if we did not used inverse,
     * we would have to concatenate ourselves all transform steps up to the beginning of the chain
     * (because {@link ConcatenatedTransform} does not store information about what happened before)
     * in order to convert the envelope provided by a step into the source units of the first step of the chain.
     *
     * <div class="note"><b>Note:</b> {@link ToDomain} records history and does concatenations, but it is
     * for a corner case which would still exist in addition of the above if we didn't used inverse.</div>
     *
     * @param  inverse  inverse of the transform for which to compute domain.
     * @throws TransformException if the domain can not be estimated.
     */
    final void estimateOnInverse(final MathTransform inverse) throws TransformException {
        if (inverse instanceof ConcatenatedTransform) {
            final ConcatenatedTransform ct = (ConcatenatedTransform) inverse;
            estimateOnInverse(ct.transform2);
            estimateOnInverse(ct.transform1, ct.transform2);
        } else {
            final MathTransform forward = inverse.inverse();
            if (forward instanceof ConcatenatedTransform) {
                final ConcatenatedTransform ct = (ConcatenatedTransform) forward;
                final MathTransform transform1 = ct.transform2.inverse();
                final MathTransform transform2 = ct.transform1.inverse();
                estimateOnInverse(transform2);
                estimateOnInverse(transform1, transform2);
            } else {
                estimate(forward);
            }
        }
    }

    /**
     * Estimates the domain using the inverse of a transform and transform that domain using the given suffix.
     * This method is invoked when the {@code inverse} transform is not the last step of a transform chain.
     * The last steps shall be specified in the {@code tail} transform.
     *
     * @param  inverse  inverse of the transform for which to compute domain.
     * @param  tail     transform to use for transforming the domain envelope.
     * @throws TransformException if the domain can not be estimated.
     */
    final void estimateOnInverse(final MathTransform inverse, final MathTransform tail) throws TransformException {
        final ToDomain previous = stepToDomain;
        try {
            stepToDomain = new ToDomain(tail, stepToDomain);
            estimateOnInverse(inverse);
        } finally {
            stepToDomain = previous;
        }
    }

    /**
     * Sets the domain to the intersection of current domain with the specified envelope.
     * The envelope coordinates shall be in units of the inputs of the first {@link MathTransform}
     * given to {@link #estimate(MathTransform)}. If that method is invoked recursively in a chain
     * of transforms, callers are responsible for converting the envelope.
     *
     * @param  domain  the domain to intersect with.
     */
    public void intersect(final Envelope domain) {
        ArgumentChecks.ensureNonNull("domain", domain);
        if (limits == null) {
            limits = domain;
        } else {
            if (intersection == null) {
                limits = intersection = new GeneralEnvelope(limits);
            }
            intersection.intersect(domain);
        }
    }

    /**
     * Transforms the given envelope, then either returns it or delegates to {@link #intersect(Envelope)}.
     * If {@code prefix} was the only transform applied, then the transformed envelope is returned.
     * Otherwise the transformed envelope is intersected with current domain and {@code null} is returned.
     *
     * <p>This method behavior allows opportunistic implementation of
     * {@link org.apache.sis.referencing.operation.transform.AbstractMathTransform.Inverse#getDomain(DomainDefinition)}.
     * When above-cited method is invoked directly by users, they should get the transformed envelope because there is
     * no other transform queued in {@link #stepToDomain}. But if above-cited method is invoked for a transform in the
     * middle of a transforms chain, then the transformed envelope can not be returned because it would not be in the
     * CRS expected by method contract. But because that situation is specific to {@link ConcatenatedTransform},
     * it should be unnoticed by users.</p>
     *
     * @param  domain  the domain to intersect with, or {@code null} if none.
     * @param  prefix  a transform to apply on the envelope before other transforms.
     * @return the transformed envelope if {@code prefix} was the only transform applied, or {@code null} otherwise.
     * @throws TransformException if the envelope can not be transformed to the domain of the first transform step.
     */
    final Envelope intersectOrTransform(Envelope domain, MathTransform prefix) throws TransformException {
        if (domain != null) {
            if (stepToDomain != null) {
                prefix = MathTransforms.concatenate(prefix, stepToDomain.concatenation());
                domain = Envelopes.transform(prefix, domain);
                intersect(domain);
                return null;
            }
            domain = Envelopes.transform(prefix, domain);
        }
        return domain;
    }

    /**
     * Returns the domain computed so far by this instance. The envelope is in units of the
     * inputs of the transform given in the first call to {@link #estimate(MathTransform)}.
     *
     * @return the domain of the transform being evaluated.
     */
    public Optional<Envelope> result() {
        return Optional.ofNullable(limits);
    }

    /**
     * Returns a string representation for debugging purposes.
     *
     * @return string representation of current domain.
     */
    @Override
    public String toString() {
        return (limits != null) ? limits.toString() : "empty";
    }
}
