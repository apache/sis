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
package org.apache.sis.geometries.math;

import org.apache.sis.util.Static;


/**
 *
 * Original code from Unlicense.science
 */
public final class Quaternions extends Static {

    private static final double DELTA = 0.00001;

    public static double[] add(final double[] quaternion, final double[] other, double[] buffer){
        return Vectors.add(quaternion, other, buffer);
    }

    public static double[] subtract(final double[] quaternion, final double[] other, double[] buffer){
        return Vectors.subtract(quaternion, other, buffer);
    }

    public static double dot(final double[] q1, final double[] q2){
        return Vectors.dot(q1, q2);
    }

    /**
     * Normalize quaternion, result is stored in buffer.
     * if buffer is null, a new quaternion is created.
     * Arrays must have the same size
     * @param quaternion input quaternion
     * @param buffer result buffer, can be null
     * @return normalized quaternion
     */
    public static double[] normalize(final double[] quaternion, double[] buffer){
        return Vectors.normalize(quaternion, buffer);
    }

    public static double[] conjugate(final double[] quaternion, double[] buffer){
        if (buffer == null){
            buffer = new double[4];
        }
        buffer[0] = -quaternion[0];
        buffer[1] = -quaternion[1];
        buffer[2] = -quaternion[2];
        buffer[3] =  quaternion[3];
        return buffer;
    }

    public static double[] scale(final double[] quaternion, final double scale, double[] buffer){
        return Vectors.scale(quaternion, scale, buffer);
    }

    public static double length(final double[] quaternion){
        return Vectors.length(quaternion);
    }

    public static double[] multiplyQuaternion(double[] l, double[] r, double[] buffer){
        if (buffer == null){
            buffer = new double[4];
        }

        double w = (l[3] * r[3]) - (l[0] * r[0]) - (l[1] * r[1]) - (l[2] * r[2]);
        double x = (l[0] * r[3]) + (l[3] * r[0]) + (l[1] * r[2]) - (l[2] * r[1]);
        double y = (l[1] * r[3]) + (l[3] * r[1]) + (l[2] * r[0]) - (l[0] * r[2]);
        double z = (l[2] * r[3]) + (l[3] * r[2]) + (l[0] * r[1]) - (l[1] * r[0]);

        buffer[0] = x;
        buffer[1] = y;
        buffer[2] = z;
        buffer[3] = w;

        return buffer;
    }

    public static double[] multiplyVector(double[] q, double[] v, double[] buffer){
        if (buffer == null){
            buffer = new double[v.length];
        }

        double w = - (q[0] * v[0]) - (q[1] * v[1]) - (q[2] * v[2]);
        double x =   (q[3] * v[0]) + (q[1] * v[2]) - (q[2] * v[1]);
        double y =   (q[3] * v[1]) + (q[2] * v[0]) - (q[0] * v[2]);
        double z =   (q[3] * v[2]) + (q[0] * v[1]) - (q[1] * v[0]);

        buffer[0] = x;
        buffer[1] = y;
        buffer[2] = z;
        if (v.length>3){
            buffer[3] = w;
        }

        return buffer;
    }

    public static double[] inverse(double[] values, double[] buffer){
        if (buffer == null){
            buffer = new double[4];
        }
        final double k = values[0]*values[0] + values[1]*values[1] + values[2]*values[2] + values[3]*values[3];
        buffer[0] = -values[0]/k;
        buffer[1] = -values[1]/k;
        buffer[2] = -values[2]/k;
        buffer[3] =  values[3]/k;
        return buffer;
    }

    /**
     * Linear quaternion interpolation.
     *
     * @param q1 first quaternion
     * @param q2 second quaternion
     * @param ratio : 0 is close to first vector, 1 is on second vector
     * @param buffer result buffer, not null
     * @return interpolated quaternion
     */
    public static double[] lerp(final double[] q1, final double[] q2, double ratio, double[] buffer){
        buffer = scale(q1, 1-ratio, buffer);
        final double[] p2 = scale(q2, ratio, null);
        add(buffer, p2, buffer);
        normalize(buffer, buffer);
        return buffer;
    }

    /**
     * Spherical linear interpolation.
     *
     * @param q1 first quaternion
     * @param q2 second quaternion
     * @param ratio : 0 is close to first vector, 1 is on second vector
     * @param buffer result buffer, can be null
     * @return interpolated quaternion
     */
    public static double[] slerp(final double[] q1, final double[] q2, double ratio, double[] buffer){
        if (buffer==null) buffer = new double[4];

        //quick shortcuts, avoid several math operations and buffer creations
        if (ratio<=0.0){
            System.arraycopy(q1, 0, buffer, 0, q1.length);
            return buffer;
        } else if (ratio>=1.0){
            System.arraycopy(q2, 0, buffer, 0, q2.length);
            return buffer;
        }

        double dot = dot(q1, q2);
        final double[] q3;
        if (dot < 0){
            dot = -dot;
            q3 = scale(q2, -1, null);
        } else {
            q3 = q2;
        }

        //set ratio using lerp
        double ratio1 = 1.0-ratio;
        double ratio2 = ratio;
        if ((1.0-dot) > 0.00001) {
            //if angle is big enough use slerp, otherwise use the lerp values
            final double angle = Math.acos(dot);
            final double sinAngle = Math.sin(angle);
            ratio1 = Math.sin(angle*ratio1) / sinAngle;
            ratio2 = Math.sin(angle*ratio2) / sinAngle;
        }

        buffer = scale(q1, ratio1, buffer);
        final double[] b = scale(q3, ratio2, null);
        return add(buffer, b, buffer);
    }

    /**
     * Create quaternion from axe and angle.
     * Source : http://jeux.developpez.com/faq/math/?page=quaternions#Q56
     *
     * @param axis rotation axis
     * @param angle rotation angle
     * @param buffer result buffer
     * @return quaternion, never null
     */
    public static double[] fromAngle(Tuple axis, double angle, double[] buffer){

        if (buffer == null){
            buffer = new double[4];
        }

        final double sin_a = Math.sin(angle / 2);
        final double cos_a = Math.cos(angle / 2);

        buffer[0] = axis.get(0) * sin_a;
        buffer[1] = axis.get(1) * sin_a;
        buffer[2] = axis.get(2) * sin_a;
        buffer[3] = cos_a;

        normalize(buffer, buffer);
        return buffer;
    }

    /**
     * Quaternion to matrix.
     * Source : http://jeux.developpez.com/faq/math/?page=quaternions#Q54
     *
     * @param quaternion input quaternion
     * @param matrix result buffer, can be null
     * @return result matrix
     */
    public static double[][] toMatrix(double[] quaternion, double[][] matrix){

        if (matrix==null){
            matrix = new double[3][3];
        }

        final double xx = quaternion[0] * quaternion[0];
        final double xy = quaternion[0] * quaternion[1];
        final double xz = quaternion[0] * quaternion[2];
        final double xw = quaternion[0] * quaternion[3];
        final double yy = quaternion[1] * quaternion[1];
        final double yz = quaternion[1] * quaternion[2];
        final double yw = quaternion[1] * quaternion[3];
        final double zz = quaternion[2] * quaternion[2];
        final double zw = quaternion[2] * quaternion[3];

        matrix[0][0] = 1 - 2 * (yy + zz);
        matrix[0][1] =     2 * (xy - zw);
        matrix[0][2] =     2 * (xz + yw);

        matrix[1][0] =     2 * (xy + zw);
        matrix[1][1] = 1 - 2 * (xx + zz);
        matrix[1][2] =     2 * (yz - xw);

        matrix[2][0] =     2 * (xz - yw);
        matrix[2][1] =     2 * (yz + xw);
        matrix[2][2] = 1 - 2 * (xx + yy);

        return matrix;
    }

    /**
     *
     * @param quaternion must be normalized
     * @param axis axis will be written in this buffer
     * @return angle in radians
     */
    public static double toAxisAngle(double[] quaternion, double[] axis){
        final double w = quaternion[3];
        final double angle = 2.0 * Math.acos(w);
        final double s = Math.sqrt(1-w*w);

        axis[0] = quaternion[0];
        axis[1] = quaternion[1];
        axis[2] = quaternion[2];

        if (s>=DELTA){
            Vectors.scale(axis, 1.0/s, axis);
        }

        return angle;
    }
}
