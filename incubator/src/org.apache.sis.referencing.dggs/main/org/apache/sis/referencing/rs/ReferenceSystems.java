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
package org.apache.sis.referencing.rs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.ReferenceSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.gazetteer.ReferencingByIdentifiers;
import org.apache.sis.util.Utilities;
import org.apache.sis.referencing.dggs.DiscreteGlobalGridReferenceSystem;
import org.apache.sis.referencing.rs.internal.shared.CodeOperations;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class ReferenceSystems {

    private ReferenceSystems(){}

    public static ReferenceSystem createCompound(ReferenceSystem ... rss) {
        if (rss.length == 0) return null;
        if (rss.length == 1) return rss[0];

        final List<ReferenceSystem> lst = new ArrayList<>();
        for (ReferenceSystem rs : rss) {
            if (rs instanceof CompoundRS crs) {
                lst.addAll(crs.getComponents());
            } else {
                lst.add(rs);
            }
        }
        return new CompoundRS(null, lst);
    }

    public static int getDimension(ReferenceSystem rs) {
        int dim = 0;
        for (ReferenceSystem single : getSingleComponents(rs, false)) {
            if (single instanceof ReferencingByIdentifiers) {
                dim += 1;
            } else if (single instanceof CoordinateReferenceSystem crs) {
                dim += crs.getCoordinateSystem().getDimension();
            } else {
                throw new UnsupportedOperationException("Unexpected reference system " + rs.getClass().getName());
            }
        }
        return dim;
    }

    public static List<ReferenceSystem> getSingleComponents(ReferenceSystem rs, boolean splitVertical) {
        List<ReferenceSystem> rss;
        if (rs instanceof CompoundRS crs) {
            rss = crs.getSingleComponents();
        } else if (rs instanceof CoordinateReferenceSystem crs) {
            rss = (List) CRS.getSingleComponents(crs);
        } else {
            rss = List.of(rs);
        }

        if (splitVertical) {
            rss = new ArrayList<>(rss);
            for (int i = 0; i < rss.size(); i++) {
                ReferenceSystem cdt = rss.get(i);
                if (cdt instanceof CoordinateReferenceSystem crs) {
                    VerticalCRS vertical = CRS.getVerticalComponent(crs, true);
                    if (vertical != null && vertical != cdt) {
                        SingleCRS horizontal = CRS.getHorizontalComponent(crs);
                        if ((horizontal.getCoordinateSystem().getDimension() + vertical.getCoordinateSystem().getDimension()) != crs.getCoordinateSystem().getDimension()) {
                            throw new UnsupportedOperationException("Splited vertical crs + horizontal does not match original crs size");
                        }
                        if (Utilities.equalsIgnoreMetadata(horizontal, CommonCRS.WGS84.normalizedGeographic())) {
                            horizontal = CommonCRS.WGS84.normalizedGeographic();
                        } else if (Utilities.equalsIgnoreMetadata(horizontal, CommonCRS.WGS84.geographic())) {
                            horizontal = CommonCRS.WGS84.geographic();
                        }
                        if (Utilities.equalsIgnoreMetadata(vertical, CommonCRS.Vertical.ELLIPSOIDAL.crs())) {
                            vertical = CommonCRS.Vertical.ELLIPSOIDAL.crs();
                        }
                        rss.set(i, horizontal);
                        rss.add(i+1, vertical);
                        i++;
                    }
                }
            }
        }

        return rss;
    }

    public static Optional<ReferenceSystem> getHorizontalComponent(ReferenceSystem rs) {
        for (ReferenceSystem single : getSingleComponents(rs, true)) {
            if (single instanceof ReferencingByIdentifiers
              ||((single instanceof CoordinateReferenceSystem c) && CRS.isHorizontalCRS(c))) {
                return Optional.of(single);
            }
        }
        return Optional.empty();
    }

    public static Optional<VerticalCRS> getVerticalComponent(ReferenceSystem rs) {
        for (ReferenceSystem single : getSingleComponents(rs, true)) {
            if (single instanceof VerticalCRS vcrs) {
                return Optional.of(vcrs);
            }
        }
        return Optional.empty();
    }

    public static Optional<TemporalCRS> getTemporalComponent(ReferenceSystem rs) {
        for (ReferenceSystem single : getSingleComponents(rs, true)) {
            if (single instanceof TemporalCRS tcrs) {
                return Optional.of(tcrs);
            }
        }
        return Optional.empty();
    }

    /**
     * Any reference system have a leaning real world CRS somehow.
     * In the worst case scenario this crs will be based on CRS:84.
     */
    public static CoordinateReferenceSystem getLeaningCRS(ReferenceSystem rs) throws FactoryException {
        if (rs instanceof CoordinateReferenceSystem crs) return crs;
        if (rs instanceof CompoundRS drs) return drs.getLeaningCrs();
        if (rs instanceof DiscreteGlobalGridReferenceSystem dggrs) return dggrs.getGridSystem().getCrs();

        final List<ReferenceSystem> singleComponents = ReferenceSystems.getSingleComponents(rs, true);
        final List<CoordinateReferenceSystem> crss = new ArrayList<>(singleComponents.size()+1);
        for (int i = 0; i < singleComponents.size(); i++) {
            final ReferenceSystem srs = singleComponents.get(i);
            if (srs instanceof CoordinateReferenceSystem crs) {
                crss.add(crs);
            } else if (srs instanceof DiscreteGlobalGridReferenceSystem dggrs) {
                crss.add(dggrs.getGridSystem().getCrs());
            } else if (srs instanceof ReferencingByIdentifiers rbi) {
                crss.add(CommonCRS.WGS84.normalizedGeographic());
            } else {
                throw new UnsupportedOperationException("todo");
            }
        }
        return CRS.compound(crss.toArray(CoordinateReferenceSystem[]::new));
    }

    /**
     * Find operation from one Reference System to another.
     *
     * @param source
     * @param target
     * @return
     */
    public static CodeOperation findOperation(ReferenceSystem source, ReferenceSystem target, GeographicBoundingBox areaOfInterest) throws FactoryException {

        //check of identity operation
        if (Utilities.equalsIgnoreMetadata(source, target)) {
            return CodeOperations.identity(source, target);
        }

        //check for crs to crs operation
        if (source instanceof CoordinateReferenceSystem crs1 && target instanceof CoordinateReferenceSystem crs2) {
            CoordinateOperation op = CRS.findOperation(crs1, crs2, areaOfInterest);
            return CodeOperations.CrsToCrs(op);
        }

        //find system mapping from source to target by decomposing it
        final List<ReferenceSystem> sources = getSingleComponents(source, false);
        final List<ReferenceSystem> targets = getSingleComponents(target, false);

        //check one to one mapping
        if (sources.size() == 1 && targets.size() == 1) {
            final CoordinateReferenceSystem sourceCrs = getLeaningCRS(source);
            final CoordinateReferenceSystem targetCrs = getLeaningCRS(target);
            try {
                //see if we can map them going through a CRS
                final List<CodeOperation> concat = new ArrayList<>();

                if (source != sourceCrs) {
                    if (source instanceof ReferencingByIdentifiers rbi) {
                        concat.add(CodeOperations.RbiToCrs(rbi.createCoder()));
                    } else {
                        throw new FactoryException("No mapping found from " + source + " to " + sourceCrs);
                    }
                }
                concat.add(findOperation(sourceCrs, targetCrs, areaOfInterest));
                if (target != targetCrs) {
                    if (target instanceof ReferencingByIdentifiers rbi) {
                        concat.add(CodeOperations.CrsToRbi(rbi.createCoder()));
                    } else {
                        throw new FactoryException("No mapping found from " + target + " to " + targetCrs);
                    }
                }

                return CodeOperations.concatenate(concat.toArray(CodeOperation[]::new));

            } catch (FactoryException ex) {
                //do nothing
                throw new FactoryException("No mapping found from " + source + " to " + target);
            }
        }



        final int[] srcMapping = new int[sources.size()];
        Arrays.fill(srcMapping, -1);
        final int[] tgtMapping = new int[targets.size()];

        final List<CodeOperation> compound = new ArrayList<>();

        boolean ordered = true;
        targetLoop:
        for (int i = 0; i < tgtMapping.length; i++) {
            final ReferenceSystem cdt = targets.get(i);
            for (int k = 0, n = sources.size(); k < n ; k++) {
                final ReferenceSystem src = sources.get(k);
                try {
                    CodeOperation subop = findOperation(src, cdt, areaOfInterest);
                    if (srcMapping[k] != -1) throw new FactoryException("Source system " + k + "has been mapped to more then one target system");
                    compound.add(subop);
                    tgtMapping[i] = k;
                    srcMapping[k] = i;
                    ordered &= (k == i);
                    continue targetLoop;
                } catch (FactoryException e) {
                    //do nothing, continue
                }
            }
            throw new FactoryException("No mapping found for axe : " + cdt);
        }

        CodeOperation operation = CodeOperations.compound(compound.toArray(CodeOperation[]::new));

        if (!ordered || (srcMapping.length != tgtMapping.length)) {
            CodeOperation reorder = CodeOperations.reorder(source, target, tgtMapping);
            operation = CodeOperations.concatenate(reorder, operation);
        }

        return operation;
    }

}
