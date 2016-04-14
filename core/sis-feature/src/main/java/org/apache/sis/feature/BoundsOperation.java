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
package org.apache.sis.feature;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.esri.core.geometry.Geometry;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;

// Branch-dependent imports
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.MultiValuedPropertyException;
import org.opengis.feature.Property;
import org.opengis.feature.PropertyType;


/**
 * Calculate feature bounds operation.
 * <br>
 * This operation loops on all first level geometry properties and compute the
 * resulting envelope.
 * <br>
 * Geometries can be in different coordinate reference systems, they will be
 * transformed to the operation crs.
 * <br>
 * If the operation CRS has not been defined then the first none-empty geometry
 * crs will be used.
 * <br>
 * This operation can only be read, not setted.
 *
 * @author  Johann Sorel (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class BoundsOperation extends AbstractOperation {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6250548001562807671L;

    private static final AttributeType<Envelope> TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(NAME_KEY, "Envelope"),Envelope.class,1,1,null);

    /**
     * The parameter descriptor for the "Bounds" operation, which does not take any parameter.
     */
    private static final ParameterDescriptorGroup EMPTY_PARAMS = LinkOperation.parameters("Bounds", 1);

    private final CoordinateReferenceSystem crs;

    /**
     *
     * @param identification The name and other information to be given to this operation.
     * @param crs result envelope CRS, can be {@code null}
     */
    BoundsOperation(Map<String,?> identification, CoordinateReferenceSystem crs) {
        super(identification);
        this.crs = crs;
    }

    /**
     * Bounds operation do not require any parameter.
     *
     * @return empty parameter group.
     */
    @Override
    public ParameterDescriptorGroup getParameters() {
        return EMPTY_PARAMS;
    }

    /**
     * Bounds operation generates a Envelope result, or {@code null}.
     *
     * @return bounds envelope type
     */
    @Override
    public IdentifiedType getResult() {
        return TYPE;
    }

    /**
     * {@inheritDoc }
     *
     * @param  feature    The feature on which to execute the operation.
     *                    Can not be {@code null}.
     * @param  parameters The parameters to use for executing the operation.
     *                    Can be {@code null}.
     * @return The operation result, never null.
     */
    @Override
    public Property apply(Feature feature, ParameterValueGroup parameters) {
        return new BoundsAttribute(feature);
    }

    /**
     * Operation attribute.
     * Value is calculated each time it is accessed.
     */
    private final class BoundsAttribute extends AbstractAttribute<Envelope> {

        private final Feature feature;

        public BoundsAttribute(final Feature feature) {
            super(TYPE);
            this.feature = feature;
        }

        @Override
        public Envelope getValue() throws MultiValuedPropertyException {
            try {
                return calculate(feature, crs);
            } catch (TransformException ex) {
                Logger.getLogger("org.apache.sis.feature").log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }
            return null;
        }

        @Override
        public void setValue(Envelope value) {
            throw new UnsupportedOperationException("Bounds operation attribute can not be set.");
        }

    }

    GeneralEnvelope calculate(Feature feature, CoordinateReferenceSystem crs) throws TransformException {

        GeneralEnvelope bounds = null;

        final FeatureType type = feature.getType();
        for(PropertyType pt : type.getProperties(true)){
            if(!pt.getName().equals(AttributeConvention.ATTRIBUTE_DEFAULT_GEOMETRY)){
                if(!AttributeConvention.isGeometryAttribute(pt)) continue;
            }

            final Object val = feature.getPropertyValue(pt.getName().toString());

            if(val instanceof Geometry){
                final Geometry geom = (Geometry)val;

                final com.esri.core.geometry.Envelope env = new com.esri.core.geometry.Envelope();
                geom.queryEnvelope(env);
                if(env.isEmpty()) continue;

                //extract geometry enveloppe
                final CoordinateReferenceSystem geomCrs = AttributeConvention.getCRSCharacteristic(pt);
                Envelope genv;
                if(geomCrs!=null){
                    genv = new GeneralEnvelope(geomCrs);
                }else{
                    genv = new GeneralEnvelope(2);
                }
                ((GeneralEnvelope)genv).setRange(0, env.getXMin(), env.getXMax());
                ((GeneralEnvelope)genv).setRange(1, env.getYMin(), env.getYMax());

                //ensure it is in the wanted crs
                if(crs!=null){
                    genv = Envelopes.transform(genv, crs);
                }

                if(bounds==null){
                    if(crs==null){
                        bounds = (GeneralEnvelope)genv;
                    }else{
                        bounds = new GeneralEnvelope(crs);
                        bounds.setEnvelope(genv);
                    }
                }else{
                    bounds.add(genv);
                }
            }
        }

        return bounds;
    }

}
