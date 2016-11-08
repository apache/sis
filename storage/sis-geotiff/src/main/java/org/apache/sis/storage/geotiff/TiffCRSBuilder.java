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
package org.apache.sis.storage.geotiff;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.measure.Unit;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.apache.sis.internal.geotiff.Resources;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.Units;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.crs.DefaultProjectedCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.referencing.datum.DefaultGeodeticDatum;
import org.apache.sis.referencing.factory.GeodeticAuthorityFactory;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Vocabulary;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;

/**
 *
 * @author Remi Marechal (Geomatys).
 * @since   0.8
 * @version 0.8
 * @module
 */
public class TiffCRSBuilder {

    /** EPSG Factory for creating {@link GeodeticDatum}objects. */
    private DatumFactory datumObjFactory;
    /**
     * Cached {@link MathTransformFactory} for building {@link MathTransform}
     * objects.
     */
    private MathTransformFactory mtFactory ;
    /** EPSG factories for various purposes. */
    private GeodeticAuthorityFactory epsgFactory;

    private GeodeticObjectFactory objFactory;

    private CoordinateOperationFactory operationFactory;

    private final Reader reader;

    /**
     * References the needed "GeoKeys" to build CRS.
     */
    private Vector geoKeyDirectoryTag = null;

    /**
     * This tag is used to store all of the DOUBLE valued GeoKeys, referenced by the GeoKeyDirectoryTag.
     */
    private Vector geoDoubleParamsTag = null;

    /**
     * This tag is used to store all of the ASCII valued GeoKeys, referenced by the GeoKeyDirectoryTag.
     */
    private String geoAsciiParamsTag = null;

    final ValueMap geoKeys = new ValueMap();

    private short keyDirectoryVersion;
    private short keyRevision;
    private short minorRevision;
    private short numberOfKey;
    private int geoKeyDirectorySize;

    TiffCRSBuilder(final Reader reader) {
        this.reader = reader;
    }

    //----------------------------- Private ------------------------------------
    /**
     * Check if given string is null, empty or equals to zero.
     * Geotiff tags are often badly defined, this ensure we skip "0.0" tags in the
     * hope another tag will define a proper value.
     * In the worst case if no valid tags are found the 0.0 value will be used anyway.
     *
     * @param code
     * @return true if value is zero
     */
    private static boolean isZero(final String code) {
        if (code == null || code.isEmpty()) return true;
        final double d = Double.parseDouble(code);
        return d == 0.0;
    }

    /**
     * Reports a warning represented by the given message and key.
     * At least one of message and exception shall be non-null.
     *
     * @param reader reader which manage exception and message.
     * @param message - the message to log, or null if none.
     * @param exception - the exception to log, or null if none.
     * @see Resources
     */
    private void warning(final Reader reader, final Level level, final short key, final Object ...message) {
        final LogRecord r = reader.resources().getLogRecord(level, key, message);
        reader.owner.warning(r);
    }

    /**
     * Initialyse all requested factories to build {@link CoordinateReferenceSystem}.
     *
     * @throws FactoryException if problem during factories instanciation.
     */
    private void initFactories()
            throws FactoryException {

        if (mtFactory == null)
            mtFactory = DefaultFactories.forBuildin(MathTransformFactory.class);
        if (epsgFactory == null)
            epsgFactory = (GeodeticAuthorityFactory) CRS.getAuthorityFactory("EPSG");
        if (objFactory == null)
            objFactory = DefaultFactories.forBuildin(CRSFactory.class, GeodeticObjectFactory.class);
        if (operationFactory == null)
            operationFactory = DefaultFactories.forBuildin(CoordinateOperationFactory.class);

//        mtFactory.getDefaultParameters("Geotiff:"+GeoKeys.CRS.CT_LambertAzimEqualArea);
    }

    private static Map<String,?> name(final String name) {
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, name);
    }

    /**
     * @todo we should somehow try to to support user defined coordinate
     *       transformation even if for the moment is not so clear to me how we
     *       could achieve that since if we have no clue about the coordinate
     *       transform what we are supposed to do in order to build a
     *       conversion, guess it? How could we pick up the parameters, should
     *       look for all and then guess the right transformation?
     *
     * @param name indicates the name for the projection.
     * @param metadata to use for building this {@link ParameterValueGroup}.
     * @return a {@link ParameterValueGroup} that can be used to trigger this
     *         projection.
     * @throws IOException
     * @throws FactoryException
     */
    private ParameterValueGroup createUserDefinedProjectionParameter(
            final String name)
            throws FactoryException, NoSuchIdentifierException, DataStoreContentException {
        // //
        //
        // Trying to get the name for the coordinate transformation involved.
        //
        // ///
        final String coordTrans = geoKeys.getAsString(GeoKeys.CRS.ProjCoordTransGeoKey);

        // throw descriptive exception if ProjCoordTransGeoKey not defined
        if ((coordTrans == null)
                || coordTrans.equalsIgnoreCase(GeoKeys.Configuration.GTUserDefinedGeoKey_String)) {
            throw new DataStoreContentException("User defined projections must specify"
                    + " coordinate transformation code in ProjCoordTransGeoKey");
        }

        // getting math transform factory
        return setParametersForProjection(name, coordTrans);
    }

    /**
     * Set the projection parameters basing its decision on the projection name.
     * I found a complete list of projections on the geotiff website at address
     * http://www.remotesensing.org/geotiff/proj_list.
     *
     * I had no time to implement support for all of them therefore you will not
     * find all of them. If you want go ahead and add support for the missing
     * ones. I have tested this code against some geotiff files you can find on
     * the geotiff website under the ftp sample directory but I can say that
     * they are a real mess! I am respecting the specification strictly while
     * many of those fields do not! I could make this method trickier and use
     * workarounds in order to be less strict but I will not do this, since I
     * believe it is may lead us just on a very dangerous path.
     *
     *
     * @param name
     * @param metadata to use fo building this {@link ParameterValueGroup}.
     * @param coordTrans
     *            a {@link ParameterValueGroup} that can be used to trigger this
     *            projection.
     *
     * @return
     * @throws GeoTiffException
     */
    private ParameterValueGroup setParametersForProjection(final String name, final String coordTransCode)
            throws NoSuchIdentifierException, DataStoreContentException {

        if (name == null && coordTransCode == null)
            throw new DataStoreContentException("bla bla bla");//-- TODO : do better

        final String projName = (name == null)
                                ? GeoKeys.CRS.getName(Integer.parseInt(coordTransCode))
                                : name;

        final ParameterValueGroup parameters = mtFactory.getDefaultParameters(projName);

        //-- particularity cases
        for (int key : geoKeys.keySet()) {
            if (GeoKeys.CRS.contain(key)) {
                String keyName = GeoKeys.CRS.getName(key);
                keyName = keyName.substring(4, keyName.length()-6);
                parameters.parameter(keyName).setValue(geoKeys.getAsString(key));
            }
        }

        //-- maybe particularity case
//            /**
//             * Mercator_1SP
//             * Mercator_2SP
//             */
//            if (name.equalsIgnoreCase("mercator_1SP")
//                    || name.equalsIgnoreCase("Mercator_2SP")
//                    || code == CT_Mercator) {
//
//                final double standard_parallel_1 = metadata.getAsDouble(ProjStdParallel1GeoKey);
//                boolean isMercator2SP = false;
//                if (!Double.isNaN(standard_parallel_1)) {
//                    parameters = mtFactory.getDefaultParameters("Mercator_2SP");
//                    isMercator2SP = true;
//                } else {
//                    parameters = mtFactory.getDefaultParameters("Mercator_1SP");
//                }
//
//                parameters.parameter(code(Mercator1SP.LONGITUDE_OF_ORIGIN)).setValue(getOriginLong(metadata));
//                parameters.parameter(code(Mercator1SP.LATITUDE_OF_ORIGIN)).setValue(getOriginLat(metadata));
//                parameters.parameter(code(Mercator2SP.FALSE_EASTING)).setValue(getFalseEasting(metadata));
//                parameters.parameter(code(Mercator2SP.FALSE_NORTHING)).setValue(getFalseNorthing(metadata));
//                if (isMercator2SP) {
//                    parameters.parameter(code(Mercator2SP.STANDARD_PARALLEL)).setValue(standard_parallel_1);
//                } else {
//                    parameters.parameter(code(Mercator1SP.SCALE_FACTOR)).setValue(getScaleFactor(metadata));
//                }
//                return parameters;
//            }
//
//            /**
//             * POLAR_STEREOGRAPHIC variant A B and C
//             */
//            if (code == CT_PolarStereographic) {
//
//                /**
//                 * They exist 3 kind of polar StereoGraphic projections,define the case
//                 * relative to existing needed attributs
//                 */
//                //-- set the mutual projection attributs
//                //-- all polar stereographic formulas share LONGITUDE_OF_ORIGIN
//                final double longitudeOfOrigin = metadata.getAsDouble(ProjStraightVertPoleLongGeoKey);
//
//                /*
//                * For polar Stereographic variant A only latitudeOfNaturalOrigin expected values are {-90; +90}.
//                * In some case, standard parallele is stipulate into latitudeOfNaturalOrigin tiff tag by error.
//                * To avoid CRS problem creation, try to anticipe this comportement by switch latitudeOfNaturalOrigin into standard parallele.
//                * HACK FOR USGS LANDSAT 8 difference between geotiff tag and Landsat 8 metadata MTL.txt file.
//                */
//                double standardParallel                 = metadata.getAsDouble(ProjStdParallel1GeoKey);
//                final double latitudeOfNaturalOrigin    = metadata.getAsDouble(ProjNatOriginLatGeoKey);
//                final boolean isVariantALatitudeConform = (Math.abs(Latitude.MAX_VALUE - Math.abs(latitudeOfNaturalOrigin)) <  Formulas.ANGULAR_TOLERANCE);
//
//                if (!isVariantALatitudeConform && Double.isNaN(standardParallel)) {
//                    LOGGER.log(Level.WARNING, "The latitudeOfNaturalOrigin for Polar Stereographic variant A is not conform.\n"
//                            + "Expected values are {-90; +90}, found : "+latitudeOfNaturalOrigin+"\n"
//                            + "Switch latitudeOfNaturalOrigin by Latitude of standard parallel to try building of Polar Stereographic Variant B or C.");
//                    standardParallel = latitudeOfNaturalOrigin;
//                }
//
//                if (Double.isNaN(standardParallel)) {
//                    //-- no standard parallele : PolarStereoGraphic VARIANT A
//                    final OperationMethod method = DefaultFactories.forBuildin(CoordinateOperationFactory.class)
//                    .getOperationMethod("Polar Stereographic (variant A)");
//
//                    parameters = method.getParameters().createValue();
//                    parameters.parameter(code(PolarStereographicA.LONGITUDE_OF_ORIGIN)).setValue(longitudeOfOrigin);
//                    parameters.parameter(code(PolarStereographicA.LATITUDE_OF_ORIGIN)).setValue(latitudeOfNaturalOrigin);
//                    parameters.parameter(code(PolarStereographicA.SCALE_FACTOR)).setValue(metadata.getAsDouble(ProjScaleAtNatOriginGeoKey));
//                    parameters.parameter(code(PolarStereographicA.FALSE_EASTING)).setValue(metadata.getAsDouble(ProjFalseEastingGeoKey));
//                    parameters.parameter(code(PolarStereographicA.FALSE_NORTHING)).setValue(metadata.getAsDouble(ProjFalseNorthingGeoKey));
//
//                } else {
//
//                    //-- Variant B and C share STANDARD_PARALLEL
//
//                    final double falseOriginEasting = metadata.getAsDouble(ProjFalseOriginEastingGeoKey);
//                    if (Double.isNaN(falseOriginEasting)) {
//                        //-- no false Origin Easting : PolarStereoGraphic VARIANT B
//                        final OperationMethod method = DefaultFactories.forBuildin(CoordinateOperationFactory.class)
//                              .getOperationMethod("Polar Stereographic (variant B)");
//
//                        parameters = method.getParameters().createValue();
//                        parameters.parameter(code(PolarStereographicB.STANDARD_PARALLEL)).setValue(standardParallel);
//                        parameters.parameter(code(PolarStereographicB.LONGITUDE_OF_ORIGIN)).setValue(longitudeOfOrigin);
//                        parameters.parameter(code(PolarStereographicB.FALSE_EASTING)).setValue(metadata.getAsDouble(ProjFalseEastingGeoKey));
//                        parameters.parameter(code(PolarStereographicB.FALSE_NORTHING)).setValue(metadata.getAsDouble(ProjFalseNorthingGeoKey));
//                    } else {
//                        //-- PolarStereoGraphic VARIANT C
//                        final OperationMethod method = DefaultFactories.forBuildin(CoordinateOperationFactory.class)
//                              .getOperationMethod("Polar Stereographic (variant C)");
//
//                        parameters = method.getParameters().createValue();
//                        parameters.parameter(code(PolarStereographicB.STANDARD_PARALLEL)).setValue(standardParallel);
//                        parameters.parameter(code(PolarStereographicB.LONGITUDE_OF_ORIGIN)).setValue(longitudeOfOrigin);
//                        parameters.parameter(code(PolarStereographicC.EASTING_AT_FALSE_ORIGIN)).setValue(metadata.getAsDouble(ProjFalseOriginEastingGeoKey));
//                        parameters.parameter(code(PolarStereographicC.NORTHING_AT_FALSE_ORIGIN)).setValue(metadata.getAsDouble(ProjFalseNorthingGeoKey));
//                    }
//                }
//            }

        return parameters;
    }

    private void parseGeoKeyDirectory() {
        int p   = 4;
        int key = 0;
        while (p < geoKeyDirectorySize && key++ < numberOfKey) {
            setKey(geoKeyDirectoryTag.intValue(p++),
                   geoKeyDirectoryTag.intValue(p++),
                   geoKeyDirectoryTag.intValue(p++),
                   geoKeyDirectoryTag.intValue(p++));
        }
    }

    private void setKey(final int KeyID, final int tiffTagLocation, final int count, final int value_Offset) {
        if (tiffTagLocation == 0) {
            //-- tiff taglocation = 0 mean offset is the stored value
            //-- and count normaly equal to 1
            assert count == 1;//-- maybe warning
            geoKeys.put(KeyID, value_Offset);
        } else {
            switch (tiffTagLocation) {
                case Tags.GeoDoubleParamsTag : {
                    assert count == 1;
                    geoKeys.put(KeyID, geoDoubleParamsTag.doubleValue(value_Offset));
                    break;
                }
                case Tags.GeoAsciiParamsTag : {
                    geoKeys.put(KeyID, geoAsciiParamsTag.substring(value_Offset, value_Offset + count));
                    break;
                }
            }
        }
    }

    /**
     * This code creates an <code>javax.Units.Unit</code> object out of the
     * <code>ProjLinearUnitsGeoKey</code> and the
     * <code>ProjLinearUnitSizeGeoKey</code>. The unit may either be
     * specified as a standard EPSG recognized unit, or may be user defined.
     *
     * @param key
     * @param userDefinedKey
     * @param base
     * @param def
     * @return <code>Unit</code> object representative of the tags in the file.
     * @throws IOException
     *             if the<code>ProjLinearUnitsGeoKey</code> is not specified
     *             or if unit is user defined and
     *             <code>ProjLinearUnitSizeGeoKey</code> is either not defined
     *             or does not contain a number.
     */
    private Unit createUnit(final int key, final int userDefinedKey, final Unit base, final Unit def)
            throws FactoryException, DataStoreContentException {
        final String unitCode = geoKeys.getAsString(key);

        //-- if not defined, return the default unit of measure
        if (unitCode == null
         || unitCode.trim().isEmpty()) return def;

       /*
        * If specified, retrieve the appropriate unit code. Exist two cases
        * to keep into account,
        * first case is when the unit of measure has an EPSG code,
        * secondly it can be instantiated as a conversion from meter.
        */
        if (unitCode.equals(GeoKeys.Configuration.GTUserDefinedGeoKey_String)) {
            final String unitSize = geoKeys.getAsString(userDefinedKey);

            //-- missing needed key
            if (unitSize == null)
                throw new DataStoreContentException(reader.resources().getString(Resources.Keys.UnexpectedKeyValue_3,
                        GeoKeys.CRS.getName(key)+" ("+key+")", "non null value", "null"));

            final double sz = Double.parseDouble(unitSize);
            return base.multiply(sz);
        }

        //-- using epsg code for this unit
        return epsgFactory.createUnit(String.valueOf(unitCode));
    }

    /**
     *
     * @return
     */
    private CoordinateReferenceSystem createProjectedCRS()
            throws FactoryException, DataStoreContentException {

        String tempCode = geoKeys.getAsString(GeoKeys.CRS.ProjectedCSTypeGeoKey);
        if (tempCode == null) tempCode = "unnamed";

        //-- getting the linear unit used by this coordinate reference system.
        final Unit linearUnit = createUnit(GeoKeys.CRS.ProjLinearUnitsGeoKey,
                                GeoKeys.CRS.ProjLinearUnitSizeGeoKey, Units.METRE, Units.METRE);

        //--------------------------- USER DEFINE -----------------------------//
        //-- if it's user defined, we have to parse many informations and
        //-- try to build appropriate projected CRS from theses parsed informations.
        //-- like base gcrs, datum, unit ...
        if (tempCode.equalsIgnoreCase("unnamed")
         || tempCode.equals(GeoKeys.Configuration.GTUserDefinedGeoKey_String)) {
            return createUserDefinedProjectedCRS(linearUnit);
        }
        //---------------------------------------------------------------------//

        //---------------------- EPSG CODE PERTINENCY -------------------------//
        //-- do a decode
        final StringBuffer projCode = new StringBuffer(tempCode.trim().intern());
        if (!tempCode.startsWith("EPSG") && !tempCode.startsWith("epsg")) {
            projCode.insert(0, "EPSG:");
        }

        //TODO : jsorel : are we sure of this ? always long/lat order ?
        ProjectedCRS pcrs = (ProjectedCRS) AbstractCRS.castOrCopy(CRS.forCode(projCode.toString())).forConvention(AxesConvention.RIGHT_HANDED);

        //-- if 'tiff defined unit' does not match with decoded Projected CRS, build another converted projected CRS.
        if (linearUnit != null && !linearUnit.equals(pcrs.getCoordinateSystem().getAxis(0).getUnit())) {
            //-- Creating anew projected CRS
            pcrs = new DefaultProjectedCRS(
                    java.util.Collections.singletonMap("name", IdentifiedObjects.getName(pcrs, new DefaultCitation("EPSG"))),
                    (GeographicCRS) pcrs.getBaseCRS(),
                    pcrs.getConversionFromBase(),
                    createProjectedCS(linearUnit));
        }
        return pcrs;
    }

    /**
     * We have a user defined {@link ProjectedCRS} (means, exist some additionals geokeys to build this projectedCRS).
     *
     * @param linearUnit
     *            is the UoM that this {@link ProjectedCRS} will use. It could
     *            be null.
     *
     * @return a user-defined {@link ProjectedCRS}.
     * @throws IOException
     * @throws FactoryException
     */
    private ProjectedCRS createUserDefinedProjectedCRS(final Unit linearUnit)
            throws FactoryException, DataStoreContentException {
        //-- get projected CRS Name or code
        String projectedCrsName = geoKeys.getAsString(GeoKeys.CRS.PCSCitationGeoKey);
        if (projectedCrsName == null) {
            projectedCrsName = "unnamed".intern();
        }

        // /////////////////////////////////////////////////////////////////////
        // PROJECTION geo key for this projected coordinate reference system.
        // get the projection code for this PCRS to build it from the GCS.
        //
        // In case i is user defined it requires:
        // PCSCitationGeoKey
        // ProjCoordTransGeoKey
        // ProjLinearUnitsGeoKey
        // /////////////////////////////////////////////////////////////////////
        final String projCode = geoKeys.getAsString(GeoKeys.CRS.ProjectionGeoKey).trim().intern();
        final boolean projUserDefined = (projCode == null
                                      || projCode.equals(GeoKeys.Configuration.GTUserDefinedGeoKey_String));


        //--------------------------------------------------------------------//
        //                       GEOGRAPHIC BASE CRS                          //
        //--------------------------------------------------------------------//
        final GeographicCRS gcs = createGeographicBaseCRS();

        //-- is it user defined?
        Conversion projection = null;
        final ParameterValueGroup parameters;
        if (projUserDefined) {
            //-- A user defined projection is made up by
            //-- PCSCitationGeoKey (NAME)
            //-- ProjCoordTransGeoKey
            //-- ProjLinearUnitsGeoKey
            String projectionName = geoKeys.getAsString(GeoKeys.CRS.PCSCitationGeoKey);
            if (projectionName == null) projectionName = "unnamed";

            //-- getting default parameters for this projection and filling them
            //-- with the values found inside the geokeys list.
            final String coordTrans               = geoKeys.getAsString(GeoKeys.CRS.ProjCoordTransGeoKey);
            final OperationMethod operationMethod = operationFactory.getOperationMethod(coordTrans);
            parameters                            = operationMethod.getParameters().createValue();
            projection                            = operationFactory.createDefiningConversion(name(projectionName), operationMethod, parameters);

            //-- is a user define projection
            //-- we need to set the remaining parameters.
            final GeodeticDatum tempDatum = ((GeodeticDatum) gcs.getDatum());
            final DefaultEllipsoid tempEll = (DefaultEllipsoid) tempDatum.getEllipsoid();
            double inverseFlattening = tempEll.getInverseFlattening();
            double semiMajorAxis = tempEll.getSemiMajorAxis();
            //-- setting missing parameters
            parameters.parameter("semi_minor").setValue(
                    semiMajorAxis * (1 - (1 / inverseFlattening)));
            parameters.parameter("semi_major").setValue(semiMajorAxis);

            //-- manage Units
            CartesianCS cs = getPredefineCartesianCS();
            if(linearUnit != null && !linearUnit.equals(Units.METRE)){
                cs = (CartesianCS) CoordinateSystems.replaceLinearUnit(cs, linearUnit);//-- TODO : retrouver un code epsg corespondant a l'ensemble (cs + unité)
            }

            return objFactory.createProjectedCRS(
                    Collections.singletonMap("name", projectedCrsName),
                    gcs, projection, cs);
        }

        projection = (Conversion) epsgFactory.createCoordinateOperation(String.valueOf(projCode));

        //-- standard projection
        //-- manage unit if necessary
        if (linearUnit != null && !linearUnit.equals(Units.METRE)) {
            return objFactory.createProjectedCRS(Collections.singletonMap(
                    "name", projectedCrsName), gcs, projection,
                    (CartesianCS) CoordinateSystems.replaceLinearUnit(getPredefineCartesianCS(), linearUnit)); //-- TODO : retrouver un code epsg corespondant a l'ensemble (cs + unité)
        }
        return objFactory.createProjectedCRS(Collections.singletonMap("name",
                projectedCrsName), gcs, projection,
                getPredefineCartesianCS());
    }

    private CartesianCS getPredefineCartesianCS()
            throws FactoryException {
        return epsgFactory.createCartesianCS("EPSG:4400");
    }

    /**
     * Creates a {@link CartesianCS} for a {@link ProjectedCRS} given the
     * provided {@link Unit}.
     *
     * @todo consider caching this items
     * @param linearUnit
     *            to be used for building this {@link CartesianCS}.
     * @return an instance of {@link CartesianCS} using the provided
     *         {@link Unit},
     */
    private DefaultCartesianCS createProjectedCS(final Unit linearUnit) {
        ArgumentChecks.ensureNonNull("ProjectedCS: LinearUnit", linearUnit);
        if (!linearUnit.isCompatible(Units.METRE)) {
            throw new IllegalArgumentException(
                    "Error when trying to create a PCS using this linear UoM "
                    + linearUnit.toString());
        }
        //-- TODO Vocabulary.Keys.Projected Easting Northing
        return new DefaultCartesianCS(name(Vocabulary.formatInternational(/*Vocabulary.Keys.Projected*/(short)208).toString()),
                new DefaultCoordinateSystemAxis(name(Vocabulary.formatInternational(/*Vocabulary.Keys.Easting*/(short)74).toString()), "E",
                AxisDirection.EAST, linearUnit),
                new DefaultCoordinateSystemAxis(name(Vocabulary.formatInternational(/*Vocabulary.Keys.Northing*/(short)178).toString()), "N",
                AxisDirection.NORTH, linearUnit));
    }


    //----------------------------- GEOGRAPHIC ---------------------------------
    /**
     *
     * @return
     */
    private CoordinateReferenceSystem createGeographicCRS() {
        return null;
    }

    /**
     * Creating an ellipsoid following the GeoTiff spec.
     *
     * @param unit to build this {@link Ellipsoid}..
     * @return an {@link Ellipsoid}.
     * @throws GeoTiffException
     */
    private Ellipsoid createEllipsoid(final Unit unit) throws FactoryException { //-- TODO refactor this method
        // /////////////////////////////////////////////////////////////////////
        // Getting the ellipsoid key in order to understand if we are working
        // against a common ellipsoid or a user defined one.
        // /////////////////////////////////////////////////////////////////////
        // ellipsoid key
        final String ellipsoidKey = geoKeys.getAsString(GeoKeys.CRS.GeogEllipsoidGeoKey);
        String temp = null;
        // is the ellipsoid user defined?
        if (ellipsoidKey.equalsIgnoreCase(GeoKeys.Configuration.GTUserDefinedGeoKey_String)) {
            // /////////////////////////////////////////////////////////////////////
            // USER DEFINED ELLIPSOID
            // /////////////////////////////////////////////////////////////////////
            String nameEllipsoid = geoKeys.getAsString(GeoKeys.CRS.GeogCitationGeoKey);
            if (nameEllipsoid == null) {
                nameEllipsoid = "unnamed";
            }
            // is it the default for WGS84?
            if (nameEllipsoid.trim().equalsIgnoreCase("WGS84")) {
                return CommonCRS.WGS84.ellipsoid();
            }

            // //
            // It is worth to point out that I ALWAYS use the inverse flattening
            // along with the semi-major axis to builde the Flattened Sphere.
            // This
            // has to be done in order to comply with the opposite process of
            // going from CRS to metadata where this coupls is always used.
            // //
            // getting temporary parameters
            temp = geoKeys.getAsString(GeoKeys.CRS.GeogSemiMajorAxisGeoKey);
            final double semiMajorAxis = (temp != null ? Double.parseDouble(temp) : Double.NaN);
            temp = geoKeys.getAsString(GeoKeys.CRS.GeogInvFlatteningGeoKey);
            final double inverseFlattening;
            if (temp != null) {
                inverseFlattening = (temp != null ? Double.parseDouble(temp)
                        : Double.NaN);
            } else {
                temp = geoKeys.getAsString(GeoKeys.CRS.GeogSemiMinorAxisGeoKey);
                final double semiMinorAxis = (temp != null ? Double.parseDouble(temp) : Double.NaN);
                inverseFlattening = semiMajorAxis
                        / (semiMajorAxis - semiMinorAxis);

            }
            // look for the Ellipsoid first then build the datum
            return DefaultEllipsoid.createFlattenedSphere(
                    Collections.singletonMap(DefaultEllipsoid.NAME_KEY, nameEllipsoid),
                    semiMajorAxis, inverseFlattening, unit);
        }

        // /////////////////////////////////////////////////////////////////////
        // EPSG STANDARD ELLIPSOID
        // /////////////////////////////////////////////////////////////////////
        return epsgFactory.createEllipsoid(String.valueOf(ellipsoidKey));
    }

    /**
     * Creation of a geographic coordinate reference system as specified in the
     * GeoTiff specification. User defined values are supported for all the
     * possible levels of the above mentioned specification.
     *
     * @param metadata
     *            to use for building a {@link GeographicCRS}.
     *
     * @return
     * @throws IOException
     */
    private GeographicCRS createGeographicBaseCRS()
            throws FactoryException, DataStoreContentException {
        GeographicCRS gcs = null;

        // ////////////////////////////////////////////////////////////////////
        // Get the crs code
        // ////////////////////////////////////////////////////////////////////
        final String tempCode = geoKeys.getAsString(GeoKeys.CRS.GeographicTypeGeoKey);
        // lookup the angular units used in this geotiff image
        Unit angularUnit = createUnit(GeoKeys.CRS.GeogAngularUnitsGeoKey,
                    GeoKeys.CRS.GeogAngularUnitSizeGeoKey, Units.RADIAN,
                    Units.DEGREE);

        // linear unit
        Unit linearUnit = createUnit(GeoKeys.CRS.GeogLinearUnitsGeoKey,
                    GeoKeys.CRS.GeogLinearUnitSizeGeoKey, Units.METRE,
                    Units.METRE);
        // if it's user defined, there's a lot of work to do
        if (tempCode == null
                || tempCode.equals(GeoKeys.Configuration.GTUserDefinedGeoKey_String)) {
            // ////////////////////////////////////////////////////////////////////
            // it is user-defined we have to parse a lot of information in order
            // to built it.
            // ////////////////////////////////////////////////////////////////////
            return createUserDefinedGCS(linearUnit, angularUnit);
        }

        // ////////////////////////////////////////////////////////////////////
        // If it's not user defined, just use the EPSG factory to create
        // the coordinate system but check if the user specified a
        // different angular unit. In this case we need to create a
        // user-defined GCRS.
        // ////////////////////////////////////////////////////////////////////
        final StringBuffer geogCode = new StringBuffer(tempCode);
        if (!tempCode.startsWith("EPSG")
                && !tempCode.startsWith("epsg")) {
            geogCode.insert(0, "EPSG:");
        }

        final CoordinateReferenceSystem decCRS = AbstractCRS.castOrCopy(CRS.forCode(geogCode.toString())).forConvention(AxesConvention.RIGHT_HANDED);
        //-- all CRS must be Geodetic
        if (!(decCRS instanceof GeodeticCRS))
            throw new IllegalArgumentException("Impossible to define CRS from none Geodetic base. found : "+decCRS.toWKT());

        if (decCRS instanceof GeographicCRS) {
            gcs = (GeographicCRS) AbstractCRS.castOrCopy(CRS.forCode(geogCode.toString())).forConvention(AxesConvention.RIGHT_HANDED);
        } else {
            //-- Try to build it from datum and re-create Geographic CRS.
//            LOGGER.log(Level.WARNING, "Impossible to build Projected CRS from none Geographic base CRS, replaced by Geographic CRS."); //-- TODO log rning
            final GeodeticCRS geodeticCrs = (GeodeticCRS) decCRS;
            final GeodeticDatum datum = geodeticCrs.getDatum();
            final HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(GeographicCRS.NAME_KEY, decCRS.getName());

            gcs = objFactory.createGeographicCRS(properties, datum, org.apache.sis.referencing.CommonCRS.defaultGeographic().getCoordinateSystem());
        }

        if (angularUnit != null && !angularUnit.equals(gcs.getCoordinateSystem().getAxis(0).getUnit())) {
            //-- Create a user-defined GCRS using the provided angular unit.
            gcs = objFactory.createGeographicCRS(name(IdentifiedObjects.getName(gcs, new DefaultCitation("EPSG"))),
                    (GeodeticDatum) gcs.getDatum(),
                    (EllipsoidalCS) CoordinateSystems.replaceAngularUnit(CommonCRS.defaultGeographic().getCoordinateSystem(), angularUnit));
        }

        return gcs;
    }

    /**
     * The GeoTIFFWritingUtilities spec requires that a user defined GCS be
     * comprised of the following:
     *
     * <ul>
     * <li> a citation </li>
     * <li> a datum definition </li>
     * <li> a prime meridian definition (if not Greenwich) </li>
     * <li> an angular unit definition (if not degrees) </li>
     * </ul>
     *
     * @param geoKeys to use for building this {@link GeographicCRS}.
     * @param linearUnit
     * @param angularUnit
     * @return a {@link GeographicCRS}.
     *
     * @throws IOException
     */
    private GeographicCRS createUserDefinedGCS(final Unit linearUnit, final Unit angularUnit)
            throws FactoryException, DataStoreContentException {
        ///-- Geographic CRS given name from tiff tag (GeogCitationGeoKey)
        String name = geoKeys.getAsString(GeoKeys.CRS.GeogCitationGeoKey);
        if (name == null) name = "unnamed";

        final GeodeticDatum datum = createGeodeticDatum(linearUnit); //-- TODO ?? is it conform ??
        CoordinateSystem replaceAngularUnit = CoordinateSystems.replaceAngularUnit(CommonCRS.defaultGeographic().getCoordinateSystem(),
                angularUnit);

        // make the user defined GCS from all the components...
        return objFactory.createGeographicCRS(Collections.singletonMap("name", name),
                                              datum,
                                              (EllipsoidalCS) CoordinateSystems.replaceAngularUnit(CommonCRS.defaultGeographic().getCoordinateSystem(),
                                                                                                   angularUnit));
    }

    /**
     * Looks up the Geodetic Datum as specified in the GeoTIFFWritingUtilities
     * file. The geotools definition of the geodetic datum includes both an
     * ellipsoid and a prime meridian, but the code in the
     * GeoTIFFWritingUtilities file does NOT include the prime meridian, as it
     * is specified separately. This code currently does not support user
     * defined datum.
     *
     * @param unit to use for building this {@link GeodeticDatum}.
     * @return a {@link GeodeticDatum}.
     * @throws IOException
     * @throws GeoTiffException
     *
     */
    private GeodeticDatum createGeodeticDatum(final Unit unit)
            throws DataStoreContentException, FactoryException { //-- TODO : refactor this method
        // lookup the datum (w/o PrimeMeridian), error if "user defined"
        GeodeticDatum datum = null;
        final String datumCode = geoKeys.getAsString(GeoKeys.CRS.GeogGeodeticDatumGeoKey);

        if (datumCode == null) {
            throw new DataStoreContentException("A user defined Geographic Coordinate system must include a predefined datum!");
        }

        if (datumCode.equals(GeoKeys.Configuration.GTUserDefinedGeoKey_String)) {
            /**
             * USER DEFINED DATUM
             */
            // datum name
            final String datumName = (geoKeys.getAsString(GeoKeys.CRS.GeogCitationGeoKey) != null ? geoKeys.getAsString(GeoKeys.CRS.GeogCitationGeoKey)
                    : "unnamed");

            // is it WGS84?
            if (datumName.trim().equalsIgnoreCase("WGS84")) {
                return CommonCRS.WGS84.datum();
            }

            // ELLIPSOID
            final Ellipsoid ellipsoid = createEllipsoid(unit);

            // PRIME MERIDIAN
            // lookup the Prime Meridian.
            final PrimeMeridian primeMeridian = createPrimeMeridian(unit);

            // DATUM
            datum = new DefaultGeodeticDatum(Collections.singletonMap(GeodeticDatum.NAME_KEY, datumName), ellipsoid,
                    primeMeridian);
        } else {
            /**
             * NOT USER DEFINED DATUM
             */
            // we are going to use the provided EPSG code
            datum = (GeodeticDatum) (epsgFactory.createDatum(String.valueOf(datumCode)));
        }

        return datum;
    }

    /**
     * Creating a prime meridian for the gcs we are creating at an higher level.
     * As usual this method tries to follow the geotiff specification.
     *
     * @param linearUnit
     *            to use for building this {@link PrimeMeridian}.
     * @return a {@link PrimeMeridian} built using the provided {@link Unit} and
     *         the provided metadata.
     * @throws IOException
     */
    private PrimeMeridian createPrimeMeridian(final Unit linearUnit) //-- TODO refactor this method
            throws DataStoreContentException, FactoryException {
        // look up the prime meridian:
        // + could be an EPSG code
        // + could be user defined
        // + not defined = greenwich
        final String pmCode = geoKeys.getAsString(GeoKeys.CRS.GeogPrimeMeridianGeoKey);
        PrimeMeridian pm = null;

        if (pmCode != null) {
            if (pmCode.equals(GeoKeys.Configuration.GTUserDefinedGeoKey_String)) {
                try {
                    final String name = geoKeys.getAsString(GeoKeys.CRS.GeogCitationGeoKey);
                    final String pmValue = geoKeys.getAsString(GeoKeys.CRS.GeogPrimeMeridianLongGeoKey);
                    final double pmNumeric = Double.parseDouble(pmValue);
                    // is it Greenwich?
                    if (pmNumeric == 0) {
                        return CommonCRS.WGS84.primeMeridian();
                    }
                    final Map props = new HashMap();
                    props.put("name", (name != null) ? name
                            : "User Defined GEOTIFF Prime Meridian");
                    pm = datumObjFactory.createPrimeMeridian(props,
                            pmNumeric, linearUnit);
                } catch (NumberFormatException nfe) {
                    throw new DataStoreContentException("Invalid user-defined prime meridian spec.",nfe);
                }
            } else {
                pm = epsgFactory.createPrimeMeridian(String.valueOf(pmCode));
            }
        } else {
            pm = CommonCRS.WGS84.primeMeridian();
        }

        return pm;
    }


    //------------------------------- GEOCENTRIQUE -----------------------------
    /**
     *
     * @return
     */
    private CoordinateReferenceSystem createGeocentricCRS() {
        return null;
    }


    //------------------------------- API --------------------------------------

    final void setGeoKeyDirectoryTag(final Vector geoKeyDirectoryTag)
            throws DataStoreContentException {
        final int gDTS = geoKeyDirectoryTag.size();
        if (gDTS < 4)
            throw new DataStoreContentException(reader.resources().getString(
                    Resources.Keys.MismatchedLength_4, "GeoKeyDirectoryTag size", "GeoKeyDirectoryTag",
                    "> 4", geoKeyDirectoryTag.size()));

        final short kDV = geoKeyDirectoryTag.shortValue(0);
        if (kDV!= 1)
            warning(reader, Level.FINE, Resources.Keys.UnexpectedKeyValue_3, "KeyDirectoryVersion", 1, kDV);
        keyDirectoryVersion     = kDV;
        keyRevision             = geoKeyDirectoryTag.shortValue(1);
        minorRevision           = geoKeyDirectoryTag.shortValue(2);
        numberOfKey             = geoKeyDirectoryTag.shortValue(3);

        final int expectedGeoKeyDirectorySize = ((numberOfKey + 1) << 2);//-- (number of key + head) * 4 --- 1 key = 4 informations
        if (gDTS != expectedGeoKeyDirectorySize)
            warning(reader, Level.WARNING,Resources.Keys.MismatchedLength_4,
                    "GeoKeyDirectoryTag size", "GeoKeyDirectoryTag", expectedGeoKeyDirectorySize, gDTS);
        this.geoKeyDirectoryTag  = geoKeyDirectoryTag;
        this.geoKeyDirectorySize = gDTS;
    }

    /**
     * Set contents of previously read {@link Tags#GeoDoubleParamsTag}.
     * Contents is about Geographic keys needed to build appropriate {@link CoordinateReferenceSystem}.
     *
     * @param geoDoubleParamsTag Vector of double value coefficients.
     */
    final void setGeoDoubleParamsTag(final Vector geoDoubleParamsTag) {
        this.geoDoubleParamsTag = geoDoubleParamsTag;
    }

    /**
     *
     * @param geoAsciiParamsTag
     */
    final void setGeoAsciiParamsTag(final String geoAsciiParamsTag) {
        this.geoAsciiParamsTag = geoAsciiParamsTag;
    }

    final CoordinateReferenceSystem build()
            throws IOException, FactoryException, NoSuchIdentifierException, DataStoreContentException {
        if (geoKeyDirectoryTag == null)
            return null; //-- TODO image CRS2D ??

        initFactories();//-- TODO : only build needed factories

        //-- build Coordinate Reference System keys
        parseGeoKeyDirectory();

        final int crsType = geoKeys.getAsInteger(GeoKeys.Configuration.GTModelTypeGeoKey);

        final CoordinateReferenceSystem crs;
        switch (crsType) {
            case GeoKeys.Configuration.ModelTypeProjected  : return createProjectedCRS();
            case GeoKeys.Configuration.ModelTypeGeographic : throw new DataStoreContentException("not implemented yet : Geographic CRS");//return null;
            case GeoKeys.Configuration.ModelTypeGeocentric : throw new DataStoreContentException("not implemented yet : Geocentric CRS");//return null;
            default : {
                return null; //-- TODO image CRS2D ??
            }
        }
    }

//------------------------------------------------------------------------------
    /**
     * Map to store all keys from GeoKeyDirectory needed to build CRS.
     */
    private class ValueMap extends HashMap<Integer, Object> {

        /**
         * Returns expected {@link GeoKeys} value as a {@link String}.
         *
     * @param key Tiff Extension keys.
     * @return A string representing the value, or {@code null} if the key was not
     *         found or failed to parse.
     */
    final String getAsString(final int key) {

            final Object value = get(key);

            if (value instanceof String) return (String) value;
            if (value instanceof Number) return ((Number)value).toString();

            return null;
        }

        /**
         * Returns expected {@link GeoKeys} value as a {@link Integer}.
         *
         * @param key Tiff extension key (not a tag)
         * @return A integer representing the value, or {@code Integer.minValue} if the key was not
         *         found or failed to parse.
         */
        final int getAsInteger(final int key) {
            final Object value = get(key);

            if (value == null)           return Integer.MIN_VALUE;
            if (value instanceof Number) return ((Number)value).intValue();

            try {
                final String geoKey = value.toString();
                return Integer.parseInt(geoKey);
            }  catch (Exception e) {
                warning(reader, Level.WARNING, Resources.Keys.UnexpectedKeyValue_3, GeoKeys.CRS.getName(key)+" ("+key+")",
                        "Integer value", value.getClass().getName()+" --> "+value);
                return Integer.MIN_VALUE;
            }
        }

        /**
         * Returns expected {@link GeoKeys} value as a {@link Double}.
         *
         * @param key Tiff extension key (not a tag)
         * @return A double representing the value, or {@code Double.NAN} if the key was not
         *         found or failed to parse.
         */
        final double getAsDouble(final int key) {
            final Object value = get(key);

            if (value == null)           return Double.NaN;
            if (value instanceof Number) return ((Number)value).doubleValue();

            try {
                final String geoKey = value.toString();
                return Double.parseDouble(geoKey);
            } catch (Exception e) {
                warning(reader, Level.WARNING, Resources.Keys.UnexpectedKeyValue_3, GeoKeys.CRS.getName(key)+" ("+key+")",
                        "Double value", value.getClass().getName()+" --> "+value);
                return Double.NaN;
            }
        }

        @Override
        public final String toString() {
            final StringBuilder strBuild = new StringBuilder("/************************ GeoKeys for CoordinateReferenceSystem *************************/");
            strBuild.append(Character.LINE_SEPARATOR);
            for (int key : keySet()) {
                strBuild.append(GeoKeys.CRS.getName(key)+" ("+key+") = "+getAsString(key));
                strBuild.append(Character.LINE_SEPARATOR);
            }
            strBuild.append("/*****************************************************************************************/");
            return strBuild.toString();
        }
    }
}
