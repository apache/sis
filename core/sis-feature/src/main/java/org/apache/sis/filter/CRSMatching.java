package org.apache.sis.filter;

import java.util.Optional;

import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.BackingStoreException;

import static org.apache.sis.referencing.IdentifiedObjects.getIdentifierOrName;

/**
 * Tries to find a common space for two given CRS operands. The common space is defined using {@link CRS#suggestCommonTarget(GeographicBoundingBox, CoordinateReferenceSystem...)}.
 * Usage: initialize through {@link #left(CoordinateReferenceSystem)}, giving your first (left) operand as argument.
 * Then use {@link #right(CoordinateReferenceSystem)} to specify the other (right) operand.
 *
 * TODO: improve CRS conversion/suggestion by allowing user to input a geographic region of interest.
 */
@FunctionalInterface
public interface CRSMatching {

    static CRSMatching left(final CoordinateReferenceSystem source) {
        if (source == null) {
            return other -> {
                if (other == null) return new NullMatch();
                else throw new IllegalArgumentException("No match can be established with given CRS because source one is null.");
            };
        } else {
            return other -> {
                if (other == null) throw new IllegalArgumentException("No match can be established with previous CRS because input one is null");
                final CoordinateReferenceSystem commonCrs = CRS.suggestCommonTarget(null, source, other);
                if (commonCrs == null) throw new IllegalArgumentException(String.format(
                        "No common space can be found between %s and %s",
                        getIdentifierOrName(source), getIdentifierOrName(other)
                ));
                return new DefaultMatch(commonCrs, source, other);
            };
        }
    }

    Match right(final CoordinateReferenceSystem rightCrs);

    /**
     * Defines that a common space has been found for both input operands.
     */
    abstract class Match {

        /**
         *
         * @return If both input CRS were null, an empty shell. Otherwise, the common space defined by {@link CRS#suggestCommonTarget(GeographicBoundingBox, CoordinateReferenceSystem...)}.
         * Note that it can be one of the original systems (in which cas one of the operations provided will be empty),
         * or a third-party one (common geographic base, for example).
         */
        abstract Optional<CoordinateReferenceSystem> getCommonCRS();

        /**
         *
         * @return Coordinate operation to use for going from left operand system to common space. Can be empty if the
         * common space is equal to left operand system.
         */
        abstract Optional<CoordinateOperation> fromLeft();

        /**
         *
         * @return Coordinate operation to use for going from right operand system to common space. Can be empty if the
         *         common space is equal to right operand system.
         */
        abstract Optional<CoordinateOperation> fromRight();

        public final <L> L transformLeftToCommon(L leftValue, Transformer<L> operator) throws TransformException {
            return transform(leftValue, fromLeft(), operator);
        }

        public final <R> R transformRightToCommon(R rightValue, Transformer<R> operator) throws TransformException {
            return transform(rightValue, fromRight(), operator);
        }
    }

    final class NullMatch extends Match {

        @Override
        public Optional<CoordinateReferenceSystem> getCommonCRS() {
            return Optional.empty();
        }

        @Override
        public Optional<CoordinateOperation> fromLeft() {
            return Optional.empty();
        }

        @Override
        public Optional<CoordinateOperation> fromRight() {
            return Optional.empty();
        }
    }

    final class DefaultMatch extends Match {

        final CoordinateReferenceSystem commonCRS;
        final Optional<CoordinateOperation> fromLeft;
        final Optional<CoordinateOperation> fromRight;

        public DefaultMatch(CoordinateReferenceSystem commonCRS, CoordinateReferenceSystem leftCrs, CoordinateReferenceSystem rightCrs) {
            this.commonCRS = commonCRS;
            try {
                fromLeft = createOp(leftCrs, commonCRS);
                fromRight = createOp(rightCrs, commonCRS);
            } catch (FactoryException e) {
                throw new BackingStoreException(e);
            }
        }

        @Override
        public Optional<CoordinateReferenceSystem> getCommonCRS() {
            return Optional.of(commonCRS);
        }

        @Override
        public Optional<CoordinateOperation> fromLeft() {
            return fromLeft;
        }

        @Override
        public Optional<CoordinateOperation> fromRight() {
            return fromRight;
        }
    }

    static Optional<CoordinateOperation> createOp(final CoordinateReferenceSystem source, final CoordinateReferenceSystem target) throws FactoryException {
        if (Utilities.equalsIgnoreMetadata(source, target)) {
            return Optional.empty();
        } else {
            final CoordinateOperation op = CRS.findOperation(source, target, null);
            return Optional.of(op);
        }
    }

    /**
     * TODO: We should NOT accept factory exception here. The transform uses an already built coordinate operation, no
     * CRS factory should be needed in the process.
     * @param <T> Type of object to transform (envelope, geometry, etc.).
     */
    @FunctionalInterface
    interface Transformer<T> {
        T apply(T operand, CoordinateOperation op) throws TransformException, FactoryException;
    }

    static <T> T transform(T in, Optional<CoordinateOperation> coordOp, Transformer<T> transformer) throws TransformException {
        try {
            return coordOp.map(op -> {
                try {
                    return transformer.apply(in, op);
                } catch (TransformException | FactoryException e) {
                    throw new BackingStoreException(e);
                }
            })
                    .orElse(in);
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(TransformException.class);
        }
    }
}
