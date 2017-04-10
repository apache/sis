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
package org.apache.sis.measure;

import java.util.Set;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.measure.Dimension;
import javax.measure.Quantity;
import javax.measure.quantity.*;
import javax.measure.quantity.Angle;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.format.UnitFormat;
import tec.units.tck.util.ServiceConfiguration;


/**
 * Provides information but Apache SIS implementation.
 * This is required for JSR-363 TCK execution.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@SuppressWarnings({"rawtypes", "unchecked"})      // Generic array creation
public final strictfp class Configuration implements ServiceConfiguration {
    /**
     * Invoked by {@link java.util.ServiceLoader}.
     */
    public Configuration() {
    }

    /**
     * Return the list of {@link Quantity} types that are supported. This method returns all quantities
     * listed in the {@link javax.measure.quantity} package for which a {@link Units} constant is defined.
     *
     * @return  all quantity types having a {@link Units} constant.
     */
    @Override
    public Collection<Class<? extends Quantity>> getSupportedQuantityTypes() {
        return Arrays.asList(new Class[] {
//              Acceleration.class,
                AmountOfSubstance.class,
                Angle.class,
                Area.class,
//              CatalyticActivity.class,
                Dimensionless.class,
                ElectricCapacitance.class,
                ElectricCharge.class,
                ElectricConductance.class,
                ElectricCurrent.class,
                ElectricInductance.class,
                ElectricPotential.class,
                ElectricResistance.class,
                Energy.class,
                Force.class,
                Frequency.class,
                Illuminance.class,
                Length.class,
                LuminousFlux.class,
                LuminousIntensity.class,
                MagneticFlux.class,
                MagneticFluxDensity.class,
                Mass.class,
                Power.class,
                Pressure.class,
//              RadiationDoseAbsorbed.class,
//              RadiationDoseEffective.class,
//              Radioactivity.class,
                SolidAngle.class,
                Speed.class,
                Temperature.class,
                Time.class,
                Volume.class
        });
    }

    /**
     * Return the list of {@link Quantity} implementation classes, except the ones created by proxy.
     * Note that those classes are package-private.
     *
     * @return  all quantity implementation classes.
     */
    @Override
    public Collection<Class> getQuantityClasses() {
        return Arrays.asList(new Class[] {
            Scalar.Angle.class,
            Scalar.Area.class,
            Scalar.Dimensionless.class,
            Scalar.Energy.class,
            Scalar.Force.class,
            Scalar.Frequency.class,
            Scalar.Length.class,
            Scalar.Mass.class,
            Scalar.Power.class,
            Scalar.Pressure.class,
            Scalar.Speed.class,
            Scalar.Temperature.class,
            Scalar.Time.class,
            Scalar.Volume.class
        });
    }

    /**
     * Return the list of {@link Dimension} implementation classes.
     * Note that those classes are package-private.
     *
     * @return  all dimension implementation classes.
     */
    @Override
    public Collection<Class> getDimensionClasses() {
        return Collections.singleton(UnitDimension.class);
    }

    /**
     * Return the list of {@link Unit} implementation classes.
     * Note that those classes are package-private.
     *
     * @return  all unit implementation classes.
     */
    @Override
    public Collection<Class> getUnitClasses() {
        return Arrays.asList(new Class[] {
            SystemUnit.class,
            ConventionalUnit.class
        });
    }

    /**
     * Returns the unit for the specified quantity type.
     *
     * @param  <Q>   the type of the quantity for which to create a unit.
     * @param  type  the type of the quantity for which to create a unit.
     * @return the system unit for the specified quantity.
     *
     * @see Units#get(Class)
     */
    @Override
    public <Q extends Quantity<Q>> Unit<Q> getUnit4Type(final Class<Q> type) {
        return Units.get(type);
    }

    /**
     * Returns units to test for requirements and recommendations.
     * This method returns all units defined in the {@link Units} class.
     *
     * @return all units defined in {@link Units} class.
     */
    @Override
    public Collection<? extends Unit<?>> getUnits4Test() {
        final Field[] fields = Units.class.getDeclaredFields();
        final List<Unit<?>> units = new ArrayList<>(fields.length);
        try {
            for (final Field field : fields) {
                final int modifier = field.getModifiers();
                if (Modifier.isStatic(modifier) && !Modifier.isPrivate(modifier)
                        && Unit.class.isAssignableFrom(field.getType()))
                {
                    units.add((Unit<?>) field.get(null));
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);                    // Should never happen.
        }
        return units;
    }

    /**
     * Returns the base dimensions to be tested for requirements and recommendations.
     *
     * @return the list of base dimensions to be checked.
     */
    @Override
    public Collection<Dimension> getBaseDimensions() {
        return Arrays.asList(
                Units.METRE   .getDimension(),
                Units.KILOGRAM.getDimension(),
                Units.SECOND  .getDimension(),
                Units.AMPERE  .getDimension(),
                Units.KELVIN  .getDimension(),
                Units.MOLE    .getDimension(),
                Units.CANDELA .getDimension()
        );
    }

    /**
     * Returns {@code UnitConverter} instances to be tested for requirements and recommendations.
     *
     * @return unit converters to test.
     */
    @Override
    public Collection<UnitConverter> getUnitConverters4Test() {
        final Set<UnitConverter> converters = new LinkedHashSet<>();
        for (final Unit<?> unit : getUnits4Test()) {
            if (unit instanceof ConventionalUnit<?>) {
                converters.add(((ConventionalUnit<?>) unit).toTarget);
            }
        }
        return converters;
    }

    /**
     * Returns {@code UnitFormat} instances to be tested for requirements and recommendations.
     *
     * @return a single {@link org.apache.sis.measure.UnitFormat} instance.
     */
    @Override
    public Collection<UnitFormat> getUnitFormats4Test() {
        return Collections.singleton(new org.apache.sis.measure.UnitFormat(Locale.US));
    }
}
