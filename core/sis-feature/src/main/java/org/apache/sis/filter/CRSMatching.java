package org.apache.sis.filter;

import java.util.Optional;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.util.FactoryException;

import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.collection.BackingStoreException;

import static org.apache.sis.referencing.IdentifiedObjects.getIdentifierOrName;

/**
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

    Match right(final CoordinateReferenceSystem other);

    interface Match {
        Optional<CoordinateReferenceSystem> getCommonCRS();
        Optional<CoordinateOperation> fromLeft();
        Optional<CoordinateOperation> fromRight();
    }

    final class NullMatch implements Match {

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

    final class DefaultMatch implements Match {

        final CoordinateReferenceSystem commonCRS;
        final Optional<CoordinateOperation> fromLeft;
        final Optional<CoordinateOperation> fromRight;

        public DefaultMatch(CoordinateReferenceSystem commonCRS, CoordinateReferenceSystem left, CoordinateReferenceSystem right) {
            this.commonCRS = commonCRS;
            try {
                fromLeft = createOp(left, commonCRS);
                fromRight = createOp(right, commonCRS);
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
}
