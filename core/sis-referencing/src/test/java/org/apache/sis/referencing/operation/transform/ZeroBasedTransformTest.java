package org.apache.sis.referencing.operation.transform;

import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.test.TestCase;
import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

public class ZeroBasedTransformTest extends TestCase {

    /**
     * Checks that no significant shift is produced by the combination of the following factors:
     * <ol>
     *     <li>A source envelope whose origin is zero</li>
     *     <li>An intermediate geographic transform involving a wrap-around</li>
     *     <li>A Pseudo-Mercator CRS destination</li>
     * </ol>
     *
     * Requires an EPSG database (skipped otherwise)
     */
    @Test
    public void theZeroTheWrapAroundAndThePseudoMercator() throws FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCrs = HardCodedCRS.IMAGE;
        final CoordinateReferenceSystem intermediateCrs = HardCodedCRS.WGS84;
        final CoordinateReferenceSystem targetCrs;
        try {
            targetCrs = CRS.forCode("EPSG:3857");
        } catch (FactoryException e) {
            throw new AssumptionViolatedException("This test requires an EPSG database", e);
        }

        final Envelope sourceEnvelope = new Envelope2D(sourceCrs, 0, 0, 1024, 512);
        final AffineTransform2D sourceToCrs84 = new AffineTransform2D(0.3515625, 0, 0, -0.3515625, -180.00001, 90);

        CoordinateOperation op = CRS.findOperation(intermediateCrs, targetCrs, new DefaultGeographicBoundingBox(-180, 180, -90, 90));
        final MathTransform sourceToTarget = MathTransforms.concatenate(sourceToCrs84, op.getMathTransform());

        final GeneralEnvelope targetEnvelope = Envelopes.transform(sourceToTarget, sourceEnvelope);
        Assert.assertEquals("min X", CRS.getDomainOfValidity(targetCrs).getMinimum(0), targetEnvelope.getMinimum(0), 1E4);
    }
}
