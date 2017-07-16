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
#include <math.h>
#include <string.h>
#include <projects.h>
#include "org_apache_sis_storage_gdal_PJ.h"


#define PJ_FIELD_NAME "ptr"
#define PJ_FIELD_TYPE "J"


/*!
 * \brief
 * Internal method returning the address of the PJ structure wrapped by the given Java object.
 * This function looks for a field named "ptr" and of type "long" (Java signature "J") in the
 * given object.
 *
 * \param  env    - The JNI environment.
 * \param  object - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \return The address of the PJ structure, or NULL if the operation fails (for example
 *         because the "ptr" field was not found).
 */
PJ *getPJ(JNIEnv *env, jobject object)
{
    jfieldID id = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, object), PJ_FIELD_NAME, PJ_FIELD_TYPE);
    return (id) ? (PJ*) (*env)->GetLongField(env, object, id) : NULL;
}

/*!
 * \brief
 * Returns the Proj4 release number.
 *
 * \param  env   - The JNI environment.
 * \param  class - The class from which this method has been invoked.
 * \return The Proj4 release number, or NULL.
 */
JNIEXPORT jstring JNICALL Java_org_apache_sis_storage_gdal_PJ_getRelease
  (JNIEnv *env, jclass class)
{
    const char *desc = pj_get_release();
    return (desc) ? (*env)->NewStringUTF(env, desc) : NULL;
}

/*!
 * \brief
 * Allocates a new PJ structure from a definition string.
 *
 * \param  env        - The JNI environment.
 * \param  class      - The class from which this method has been invoked.
 * \param  definition - The string definition to be given to Proj4.
 * \return The address of the new PJ structure, or 0 in case of failure.
 */
JNIEXPORT jlong JNICALL Java_org_apache_sis_storage_gdal_PJ_allocatePJ
  (JNIEnv *env, jclass class, jstring definition)
{
    const char *def_utf = (*env)->GetStringUTFChars(env, definition, NULL);
    if (!def_utf) return 0;             // OutOfMemoryError already thrown.
    PJ *pj = pj_init_plus(def_utf);
    (*env)->ReleaseStringUTFChars(env, definition, def_utf);
    return (jlong) pj;
}

/*!
 * \brief
 * Allocates a new geographic PJ structure from an existing one.
 *
 * \param  env       - The JNI environment.
 * \param  class     - The class from which this method has been invoked.
 * \param  projected - The PJ object from which to derive a new one.
 * \return The address of the new PJ structure, or 0 in case of failure.
 */
JNIEXPORT jlong JNICALL Java_org_apache_sis_storage_gdal_PJ_allocateGeoPJ
  (JNIEnv *env, jclass class, jobject projected)
{
    PJ *pj = getPJ(env, projected);
    return (pj) ? (jlong) pj_latlong_from_proj(pj) : 0;
}

/*!
 * \brief
 * Returns the definition string.
 *
 * \param  env    - The JNI environment.
 * \param  object - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \return The definition string.
 */
JNIEXPORT jstring JNICALL Java_org_apache_sis_storage_gdal_PJ_getCode
  (JNIEnv *env, jobject object)
{
    PJ *pj = getPJ(env, object);
    if (pj) {
        const char *desc = pj_get_def(pj, 0);
        if (desc) {
            jstring str = (*env)->NewStringUTF(env, desc);
            pj_dalloc((char*) desc);
            return str;
        }
    }
    return NULL;
}

/*!
 * \brief
 * Returns the description associated to the PJ structure.
 *
 * \param  env    - The JNI environment.
 * \param  object - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \return The description associated to the PJ structure.
 */
JNIEXPORT jstring JNICALL Java_org_apache_sis_storage_gdal_PJ_getName
  (JNIEnv *env, jobject object)
{
    PJ *pj = getPJ(env, object);
    if (pj) {
        const char *desc = pj->descr;
        if (desc) {
            return (*env)->NewStringUTF(env, desc);
        }
    }
    return NULL;
}

/*!
 * \brief
 * Returns the CRS type as one of the PJ.Type enum: GEOGRAPHIC, GEOCENTRIC or PROJECTED.
 * This function should never return NULL, unless class or fields have been renamed in
 * such a way that we can not find anymore the expected enum values.
 *
 * \param  env    - The JNI environment.
 * \param  object - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \return The CRS type as one of the PJ.Type enum.
 */
JNIEXPORT jobject JNICALL Java_org_apache_sis_storage_gdal_PJ_getType
  (JNIEnv *env, jobject object)
{
    PJ *pj = getPJ(env, object);
    if (pj) {
        const char *type;
        if (pj_is_latlong(pj)) {
            type = "GEOGRAPHIC";
        } else if (pj_is_geocent(pj)) {
            type = "GEOCENTRIC";
        } else {
            type = "PROJECTED";
        }
        jclass c = (*env)->FindClass(env, "org/apache/sis/storage/gdal/PJ$Type");
        if (c) {
            jfieldID id = (*env)->GetStaticFieldID(env, c, type, "Lorg/apache/sis/storage/gdal/PJ$Type;");
            if (id) {
                return (*env)->GetStaticObjectField(env, c, id);
            }
        }
    }
    return NULL;
}

/*!
 * \brief
 * Returns the semi-major axis length.
 *
 * \param  env    - The JNI environment.
 * \param  object - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \return The semi-major axis length.
 */
JNIEXPORT jdouble JNICALL Java_org_apache_sis_storage_gdal_PJ_getSemiMajorAxis
  (JNIEnv *env, jobject object)
{
    PJ *pj = getPJ(env, object);
    return pj ? pj->a_orig : NAN;
}

/*!
 * \brief
 * Computes the semi-minor axis length from the semi-major axis length and the eccentricity
 * squared.
 *
 * \param  env    - The JNI environment.
 * \param  object - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \return The semi-minor axis length.
 */
JNIEXPORT jdouble JNICALL Java_org_apache_sis_storage_gdal_PJ_getSemiMinorAxis
  (JNIEnv *env, jobject object)
{
    PJ *pj = getPJ(env, object);
    if (!pj) return NAN;
    double a = pj->a_orig;
    return sqrt(a*a * (1.0 - pj->es_orig));
}

/*!
 * \brief
 * Returns the eccentricity squared.
 *
 * \param  env    - The JNI environment.
 * \param  object - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \return The eccentricity.
 */
JNIEXPORT jdouble JNICALL Java_org_apache_sis_storage_gdal_PJ_getEccentricitySquared
  (JNIEnv *env, jobject object)
{
    PJ *pj = getPJ(env, object);
    return pj ? pj->es_orig : NAN;
}

/*!
 * \brief
 * Returns an array of character indicating the direction of each axis.
 *
 * \param  env    - The JNI environment.
 * \param  object - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \return The axis directions.
 */
JNIEXPORT jcharArray JNICALL Java_org_apache_sis_storage_gdal_PJ_getAxisDirections
  (JNIEnv *env, jobject object)
{
    PJ *pj = getPJ(env, object);
    if (pj) {
        int length = strlen(pj->axis);
        jcharArray array = (*env)->NewCharArray(env, length);
        if (array) {
            jchar* axis = (*env)->GetCharArrayElements(env, array, NULL);
            if (axis) {
                // Don't use memcp because the type may not be the same.
                int i;
                for (i=0; i<length; i++) {
                    axis[i] = pj->axis[i];
                }
                (*env)->ReleaseCharArrayElements(env, array, axis, 0);
            }
            return array;
        }
    }
    return NULL;
}

/*!
 * \brief
 * Longitude of the prime meridian measured from the Greenwich meridian, positive eastward.
 *
 * \param env    - The JNI environment.
 * \param object - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \return The prime meridian longitude, in degrees.
 */
JNIEXPORT jdouble JNICALL Java_org_apache_sis_storage_gdal_PJ_getGreenwichLongitude
  (JNIEnv *env, jobject object)
{
    PJ *pj = getPJ(env, object);
    return (pj) ? (pj->from_greenwich)*(180/M_PI) : NAN;
}

/*!
 * \brief
 * Returns the conversion factor from linear units to metres.
 *
 * \param env      - The JNI environment.
 * \param object   - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \param vertical - JNI_FALSE for horizontal axes, or JNI_TRUE for the vertical axis.
 * \return The conversion factor to metres.
 */
JNIEXPORT jdouble JNICALL Java_org_apache_sis_storage_gdal_PJ_getLinearUnitToMetre
  (JNIEnv *env, jobject object, jboolean vertical)
{
    PJ *pj = getPJ(env, object);
    if (pj) {
        return (vertical) ? pj->vto_meter : pj->to_meter;
    }
    return NAN;
}

/*!
 * \brief
 * Converts input values from degrees to radians before coordinate operation, or the output
 * values from radians to degrees after the coordinate operation.
 *
 * \param pj        - The Proj.4 PJ structure.
 * \param data      - The coordinate array to transform.
 * \param numPts    - Number of points to transform.
 * \param dimension - Dimension of points in the coordinate array.
 * \param factor    - The scale factor to apply: M_PI/180 for inputs or 180/M_PI for outputs.
 */
void convertAngularOrdinates(PJ *pj, double* data, jint numPts, int dimension, double factor) {
    int dimToSkip;
    if (pj_is_latlong(pj)) {
        // Convert only the 2 first ordinates and skip all the other dimensions.
        dimToSkip = dimension - 2;
    } else if (pj_is_geocent(pj)) {
        // Convert only the 3 first ordinates and skip all the other dimensions.
        dimToSkip = dimension - 3;
    } else {
        // Not a geographic or geocentric CRS: nothing to convert.
        return;
    }
    double *stop = data + dimension*numPts;
    if (dimToSkip > 0) {
        while (data != stop) {
            (*data++) *= factor;
            (*data++) *= factor;
            data += dimToSkip;
        }
    } else {
        while (data != stop) {
            (*data++) *= factor;
        }
    }
}

/*!
 * \brief
 * Transforms in-place the coordinates in the given array.
 *
 * \param env         - The JNI environment.
 * \param object      - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \param target      - The target CRS.
 * \param dimension   - The dimension of each coordinate value. Must be equals or greater than 2.
 * \param coordinates - The coordinates to transform, as a sequence of (x,y,<z>,...) tuples.
 * \param offset      - Offset of the first coordinate in the given array.
 * \param numPts      - Number of points to transform.
 */
JNIEXPORT void JNICALL Java_org_apache_sis_storage_gdal_PJ_transform
  (JNIEnv *env, jobject object, jobject target, jint dimension, jdoubleArray coordinates, jint offset, jint numPts)
{
    if (!target || !coordinates) {
        jclass c = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (c) (*env)->ThrowNew(env, c, "The target CRS and the coordinates array can not be null.");
        return;
    }
    if (dimension < 2 || dimension > 100) { // Arbitrary upper value for catching potential misuse.
        jclass c = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (c) (*env)->ThrowNew(env, c, "Illegal dimension. Must be in the [2-100] range.");
        return;
    }
    if ((offset < 0) || (numPts < 0) || (offset + dimension*numPts) > (*env)->GetArrayLength(env, coordinates)) {
        jclass c = (*env)->FindClass(env, "java/lang/ArrayIndexOutOfBoundsException");
        if (c) (*env)->ThrowNew(env, c, "Illegal offset or illegal number of points.");
        return;
    }
    PJ *src_pj = getPJ(env, object);
    PJ *dst_pj = getPJ(env, target);
    if (src_pj && dst_pj) {
        // Using GetPrimitiveArrayCritical/ReleasePrimitiveArrayCritical rather than
        // GetDoubleArrayElements/ReleaseDoubleArrayElements increase the chances that
        // the JVM returns direct reference to its internal array without copying data.
        // However we must promise to run the "critical" code fast, to not make any
        // system call that may wait for the JVM and to not invoke any other JNI method.
        double *data = (*env)->GetPrimitiveArrayCritical(env, coordinates, NULL);
        if (data) {
            double *x = data + offset;
            double *y = x + 1;
            double *z = (dimension >= 3) ? y+1 : NULL;
            convertAngularOrdinates(src_pj, x, numPts, dimension, M_PI/180);
            int err = pj_transform(src_pj, dst_pj, numPts, dimension, x, y, z);
            convertAngularOrdinates(dst_pj, x, numPts, dimension, 180/M_PI);
            (*env)->ReleasePrimitiveArrayCritical(env, coordinates, data, 0);
            if (err) {
                jclass c = (*env)->FindClass(env, "org/opengis/referencing/operation/TransformException");
                if (c) (*env)->ThrowNew(env, c, pj_strerrno(err));
            }
        }
    }
}

/*!
 * \brief
 * Returns a description of the last error that occurred, or NULL if none.
 *
 * \param  env    - The JNI environment.
 * \param  object - The Java object wrapping the PJ structure (not allowed to be NULL).
 * \return The last error, or NULL.
 */
JNIEXPORT jstring JNICALL Java_org_apache_sis_storage_gdal_PJ_getLastError
  (JNIEnv *env, jobject object)
{
    PJ *pj = getPJ(env, object);
    if (pj) {
        int err = pj_ctx_get_errno(pj->ctx);
        if (err) {
            return (*env)->NewStringUTF(env, pj_strerrno(err));
        }
    }
    return NULL;
}

/*!
 * \brief
 * Deallocate the PJ structure. This method is invoked by the garbage collector exactly once.
 * This method will also set the Java "ptr" final field to 0 as a safety. In theory we are not
 * supposed to change the value of a final field. But no Java code should use this field, and
 * the PJ object is being garbage collected anyway. We set the field to 0 as a safety in case
 * some user invoked the finalize() method explicitely despite our warning in the Javadoc to
 * never do such thing.
 *
 * \param env    - The JNI environment.
 * \param object - The Java object wrapping the PJ structure (not allowed to be NULL).
 */
JNIEXPORT void JNICALL Java_org_apache_sis_storage_gdal_PJ_finalize
  (JNIEnv *env, jobject object)
{
    jfieldID id = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, object), PJ_FIELD_NAME, PJ_FIELD_TYPE);
    if (id) {
        PJ *pj = (PJ*) (*env)->GetLongField(env, object, id);
        if (pj) {
            (*env)->SetLongField(env, object, id, (jlong) 0);
            pj_free(pj);
        }
    }
}
