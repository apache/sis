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
package org.apache.sis.referencing.rs.internal.shared;

import java.util.Arrays;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.gazetteer.Location;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.referencing.gazetteer.ReferencingByIdentifiers;
import org.apache.sis.referencing.rs.Code;
import org.apache.sis.referencing.rs.CodeOperation;
import org.apache.sis.referencing.rs.ReferenceSystems;
import org.apache.sis.util.iso.Names;

// Specific to the geoapi-4.0 branch:
import org.opengis.metadata.Identifier;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CodeOperations {

    private CodeOperations(){}

    public static CodeOperation identity(ReferenceSystem sourceRs, ReferenceSystem targetRs) {
        return new Identity(sourceRs, targetRs);
    }

    public static CodeOperation RbiToCrs(ReferencingByIdentifiers.Coder coder) throws FactoryException {
        return new RbiToCrs(coder);
    }

    public static CodeOperation CrsToRbi(ReferencingByIdentifiers.Coder coder) throws FactoryException {
        return new CrsToRbi(coder);
    }

    public static CodeOperation CrsToCrs(CoordinateOperation op) throws FactoryException {
        return new CrsToCrs(op);
    }

    public static CodeOperation concatenate(CodeOperation op1, CodeOperation op2) throws FactoryException {
        if (op1 instanceof Identity) return op2;
        if (op2 instanceof Identity) return op1;
        return new Concatenate(op1, op2);
    }

    public static CodeOperation concatenate(CodeOperation... ops) throws FactoryException {
        if (ops.length == 1) return ops[0];
        CodeOperation cop = concatenate(ops[0], ops[1]);
        for (int i = 2; i < ops.length; i++) {
            cop = concatenate(cop, ops[i]);
        }
        return cop;
    }

    public static CodeOperation compound(CodeOperation op1, CodeOperation op2) throws FactoryException {
        return new Compound(op1, op2, null, null);
    }

    public static CodeOperation compound(CodeOperation... ops) throws FactoryException {
        if (ops.length == 1) return ops[0];
        CodeOperation cop = compound(ops[0], ops[1]);
        for (int i = 2; i < ops.length; i++) {
            cop = compound(cop, ops[i], null, null);
        }
        return cop;
    }

    public static CodeOperation reorder(ReferenceSystem sourceRs, ReferenceSystem targetRs, int[] targetMapping) {
        return new Reorder(sourceRs, targetRs, targetMapping);
    }

    public static final class Identity implements CodeOperation {

        private final ReferenceSystem sourceRs;
        private final ReferenceSystem targetRs;

        public Identity(ReferenceSystem sourceRs, ReferenceSystem targetRs) {
            this.sourceRs = sourceRs;
            this.targetRs = targetRs;
        }

        @Override
        public ReferenceSystem getSourceRS() {
            return sourceRs;
        }

        @Override
        public ReferenceSystem getTargetRS() {
            return targetRs;
        }

        @Override
        public CodeOperation inverse() throws NoninvertibleTransformException {
            return new Identity(targetRs, sourceRs);
        }

        @Override
        public Code transform(Code source, Code target) throws TransformException {
            if (target == null) return new Code(targetRs, source.getOrdinates());
            target.setOrdinates(source.getOrdinates());
            return target;
        }

        @Override
        public void transform(Code[] source, int soffset, Code[] target, int toffset, int nb) throws TransformException {
            for (int i = 0; i < nb; i++) {
                target[toffset+i] = transform(source[soffset+i], target[toffset+i]);
            }
        }

        @Override
        public Identifier getName() {
            return new NamedIdentifier(Names.createLocalName(null, null, "Identity"));
        }

        @Override
        public String toString() {
            return "Identity";
        }

    }

    public static final class Reorder implements CodeOperation {

        private final ReferenceSystem sourceRs;
        private final ReferenceSystem targetRs;
        private final int[] targetMapping;

        public Reorder(ReferenceSystem sourceRs, ReferenceSystem targetRs, int[] targetMapping) {
            this.sourceRs = sourceRs;
            this.targetRs = targetRs;
            this.targetMapping = targetMapping;
        }

        @Override
        public ReferenceSystem getSourceRS() {
            return sourceRs;
        }

        @Override
        public ReferenceSystem getTargetRS() {
            return targetRs;
        }

        public int[] getTargetMapping() {
            return targetMapping.clone();
        }

        @Override
        public CodeOperation inverse() throws NoninvertibleTransformException {
            if (targetMapping.length != ReferenceSystems.getDimension(sourceRs)) {
                throw new NoninvertibleTransformException();
            }
            final int[] inv = new int[targetMapping.length];
            for (int i = 0; i < targetMapping.length; i++) {
                inv[targetMapping[i]] = i;
            }
            return new Reorder(targetRs, sourceRs, inv);
        }

        @Override
        public Code transform(Code source, Code target) throws TransformException {
            if (target == null) return new Code(targetRs);
            target.setOrdinates(source.getOrdinates());
            return target;
        }

        @Override
        public void transform(Code[] source, int soffset, Code[] target, int toffset, int nb) throws TransformException {
            for (int i = 0; i < nb; i++) {
                target[toffset+i] = transform(source[soffset+i], target[toffset+i]);
            }
        }

        @Override
        public Identifier getName() {
            return new NamedIdentifier(Names.createLocalName(null, null, "Reorder"));
        }

        @Override
        public String toString() {
            return "Reorder \n  targetMapping : "+ Arrays.toString(targetMapping);
        }
    }

    public static final class RbiToCrs implements CodeOperation {

        private final ReferencingByIdentifiers.Coder coder;
        private final CoordinateReferenceSystem leaningCrs;

        public RbiToCrs(ReferencingByIdentifiers.Coder coder, CoordinateReferenceSystem leaningCrs) {
            this.coder = coder;
            this.leaningCrs = leaningCrs;
        }

        public RbiToCrs(ReferencingByIdentifiers.Coder coder) throws FactoryException {
            this(coder, ReferenceSystems.getLeaningCRS(coder.getReferenceSystem()));
        }

        @Override
        public ReferenceSystem getSourceRS() {
            return coder.getReferenceSystem();
        }

        @Override
        public ReferenceSystem getTargetRS() {
            return leaningCrs;
        }

        @Override
        public CodeOperation inverse() throws NoninvertibleTransformException {
            return new CrsToRbi(leaningCrs, coder);
        }

        @Override
        public Code transform(Code source, Code target) throws TransformException {
            final Location location = coder.decode((CharSequence) source.getOrdinate(0));
            if (target == null) target = new Code(leaningCrs, new Object[1]);
            target.setOrdinate(0, location.getPosition());
            return target;
        }

        @Override
        public void transform(Code[] source, int soffset, Code[] target, int toffset, int nb) throws TransformException {
            for (int i = 0; i < nb; i++) {
                target[toffset+i] = transform(source[soffset+i], target[toffset+i]);
            }
        }

        @Override
        public Identifier getName() {
            return new NamedIdentifier(Names.createLocalName(null, null, getSourceRS().getName().getCode() + " to " + leaningCrs.getName().toString()));
        }

        @Override
        public String toString() {
            return "ReferenceByIdentifiers to leaning CRS"
                    + "\n  Source : " + getSourceRS().toString().replaceAll("\n", "\n    ")
                    + "\n  Target : " + getTargetRS().toString().replaceAll("\n", "\n    ");
        }
    }

    public static final class CrsToCrs implements CodeOperation {

        private final CoordinateOperation op;
        private final MathTransform transform;

        public CrsToCrs(CoordinateOperation op) {
            this.op = op;
            transform = op.getMathTransform();
        }

        @Override
        public ReferenceSystem getSourceRS() {
            return op.getSourceCRS();
        }

        @Override
        public ReferenceSystem getTargetRS() {
            return op.getTargetCRS();
        }

        @Override
        public CodeOperation inverse() throws NoninvertibleTransformException {
            try {
                return new CrsToCrs(CRS.findOperation(op.getTargetCRS(), op.getSourceCRS(), null));
            } catch (FactoryException ex) {
                throw new NoninvertibleTransformException(ex);
            }
        }

        @Override
        public Code transform(Code source, Code target) throws TransformException {
            final DirectPosition dp = transform.transform((DirectPosition) source.getOrdinate(0), null);
            if (target == null) target = new Code(getTargetRS(), new Object[1]);
            target.setOrdinate(0, dp);
            return target;
        }

        @Override
        public void transform(Code[] source, int soffset, Code[] target, int toffset, int nb) throws TransformException {
            for (int i = 0; i < nb; i++) {
                target[toffset+i] = transform(source[soffset+i], target[toffset+i]);
            }
        }

        @Override
        public Identifier getName() {
            return new NamedIdentifier(Names.createLocalName(null, null, getSourceRS().getName().toString() + " to " + getTargetRS().getName().getCode()));
        }

        @Override
        public String toString() {
            return "Crs to CRS"
                    + "\n  Source : " + getSourceRS().toString().replaceAll("\n", "\n    ")
                    + "\n  Target : " + getTargetRS().toString().replaceAll("\n", "\n    ");
        }
    }

    public static final class CrsToRbi implements CodeOperation {

        private final ReferencingByIdentifiers.Coder coder;
        private final CoordinateReferenceSystem leaningCrs;

        public CrsToRbi(ReferencingByIdentifiers.Coder coder) throws FactoryException {
            this(ReferenceSystems.getLeaningCRS(coder.getReferenceSystem()), coder);
        }

        public CrsToRbi(CoordinateReferenceSystem leaningCrs, ReferencingByIdentifiers.Coder coder) {
            this.leaningCrs = leaningCrs;
            this.coder = coder;
        }

        @Override
        public ReferenceSystem getSourceRS() {
            return leaningCrs;
        }

        @Override
        public ReferenceSystem getTargetRS() {
            return coder.getReferenceSystem();
        }

        @Override
        public CodeOperation inverse() throws NoninvertibleTransformException {
            return new RbiToCrs(coder, leaningCrs);
        }

        @Override
        public Code transform(Code source, Code target) throws TransformException {
            final String code = coder.encode((DirectPosition) source.getOrdinate(0));
            if (target == null) target = new Code(coder.getReferenceSystem(), new Object[1]);
            target.setOrdinate(0, code);
            return target;
        }

        @Override
        public void transform(Code[] source, int soffset, Code[] target, int toffset, int nb) throws TransformException {
            for (int i = 0; i < nb; i++) {
                target[toffset+i] = transform(source[soffset+i], target[toffset+i]);
            }
        }

        @Override
        public Identifier getName() {
            return new NamedIdentifier(Names.createLocalName(null, null, leaningCrs.getName().toString() + " to " + getTargetRS().getName().getCode()));
        }

        @Override
        public String toString() {
            return "Leaning CRS to ReferenceByIdentifiers "
                    + "\n  Source : " + getSourceRS().toString().replaceAll("\n", "\n    ")
                    + "\n  Target : " + getTargetRS().toString().replaceAll("\n", "\n    ");
        }
    }

    public static final class Concatenate implements CodeOperation {

        private final CodeOperation op1;
        private final CodeOperation op2;

        public Concatenate(CodeOperation op1, CodeOperation op2) {
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public ReferenceSystem getSourceRS() {
            return op1.getSourceRS();
        }

        @Override
        public ReferenceSystem getTargetRS() {
            return op2.getTargetRS();
        }

        public CodeOperation getOperation1() {
            return op1;
        }

        public CodeOperation getOperation2() {
            return op2;
        }

        @Override
        public CodeOperation inverse() throws NoninvertibleTransformException {
            return new Concatenate(op2.inverse(), op1.inverse());
        }

        @Override
        public Code transform(Code source, Code target) throws TransformException {
            final Code r = op1.transform(source, null);
            return op2.transform(r, target);
        }

        @Override
        public void transform(Code[] source, int soffset, Code[] target, int toffset, int nb) throws TransformException {
            for (int i = 0; i < nb; i++) {
                target[toffset+i] = transform(source[soffset+i], target[toffset+i]);
            }
        }

        @Override
        public Identifier getName() {
            return new NamedIdentifier(Names.createLocalName(null, null, "Concatenate"));
        }

        @Override
        public String toString() {
            return "Concatenate "
                    + "\n  First : " + op1.toString().replaceAll("\n", "\n    ")
                    + "\n  Second : " + op2.toString().replaceAll("\n", "\n    ");
        }
    }

    public static final class Compound implements CodeOperation {

        private final ReferenceSystem sourceRS;
        private final ReferenceSystem targetRS;
        private final CodeOperation op1;
        private final CodeOperation op2;
        private final int dim1;
        private final int dim2;
        private final int dimRes;

        public Compound(CodeOperation op1, CodeOperation op2, ReferenceSystem sourceRS, ReferenceSystem targetRS) {
            this.sourceRS = sourceRS == null ? ReferenceSystems.createCompound(op1.getSourceRS(), op2.getSourceRS()) : sourceRS;
            this.targetRS = targetRS == null ? ReferenceSystems.createCompound(op1.getTargetRS(), op2.getTargetRS()) : sourceRS;
            this.op1 = op1;
            this.op2 = op2;
            this.dim1 = ReferenceSystems.getDimension(op1.getSourceRS());
            this.dim2 = ReferenceSystems.getDimension(op2.getSourceRS());
            this.dimRes = ReferenceSystems.getDimension(this.targetRS);
        }

        @Override
        public ReferenceSystem getSourceRS() {
            return sourceRS;
        }

        @Override
        public ReferenceSystem getTargetRS() {
            return targetRS;
        }

        public CodeOperation getOperation1() {
            return op1;
        }

        public CodeOperation getOperation2() {
            return op2;
        }

        @Override
        public CodeOperation inverse() throws NoninvertibleTransformException {
            return new Compound(op1.inverse(), op2.inverse(), targetRS, sourceRS);
        }

        @Override
        public Code transform(Code source, Code target) throws TransformException {
            final Object[] ordinates = source.getOrdinates();
            final Code s1 = new Code(op1.getSourceRS(), Arrays.copyOfRange(ordinates, 0, dim1));
            final Code s2 = new Code(op2.getSourceRS(), Arrays.copyOfRange(ordinates, dim1, dim1+dim2));
            final Code r1 = op1.transform(s1, null);
            final Code r2 = op2.transform(s2, null);
            if (target == null) target = new Code(targetRS, new Object[dimRes]);
            int k = 0;
            for (int i = 0, n = r1.getDimension(); i < n; i++) {
                target.setOrdinate(k++, r1.getOrdinate(i));
            }
            for (int i = 0, n = r2.getDimension(); i < n; i++) {
                target.setOrdinate(k++, r2.getOrdinate(i));
            }
            return target;
        }

        @Override
        public void transform(Code[] source, int soffset, Code[] target, int toffset, int nb) throws TransformException {
            for (int i = 0; i < nb; i++) {
                target[toffset+i] = transform(source[soffset+i], target[toffset+i]);
            }
        }

        @Override
        public Identifier getName() {
            return new NamedIdentifier(Names.createLocalName(null, null, "Compound"));
        }

        @Override
        public String toString() {
            return "Compound "
                    + "\n  First : " + op1.toString().replaceAll("\n", "\n    ")
                    + "\n  Second : " + op2.toString().replaceAll("\n", "\n    ");
        }
    }
}
