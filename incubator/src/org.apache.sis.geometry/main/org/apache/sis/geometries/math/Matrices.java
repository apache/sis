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

import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;


/**
 * Original code from Unlicense.science
 *
 * @author Johann Sorel
 * @author Bertrand COTE
 */
public final class Matrices {

    public static Matrix2 toMatrix2(double[][] m) {
        return new Matrix2(
                m[0][0], m[0][1],
                m[1][0], m[1][1]);
    }

    public static Matrix3 toMatrix3(double[][] m) {
        return new Matrix3(
                m[0][0], m[0][1], m[0][2],
                m[1][0], m[1][1], m[1][2],
                m[2][0], m[2][1], m[2][2]);
    }

    public static Matrix4 toMatrix4(double[][] m) {
        return new Matrix4(
                m[0][0], m[0][1], m[0][2], m[0][3],
                m[1][0], m[1][1], m[1][2], m[1][3],
                m[2][0], m[2][1], m[2][2], m[2][3],
                m[3][0], m[3][1], m[3][2], m[3][3]);
    }

    public static double[][] toArray(Matrix2 m) {
        return new double[][]
            {{m.m00, m.m01},
             {m.m10, m.m11}};
    }

    public static double[][] toArray(Matrix3 m) {
        return new double[][]
            {{m.m00, m.m01, m.m02},
             {m.m10, m.m11, m.m12},
             {m.m20, m.m21, m.m22}};
    }

    public static double[][] toArray(Matrix4 m) {
        return new double[][]
            {{m.m00, m.m01, m.m02, m.m03},
             {m.m10, m.m11, m.m12, m.m13},
             {m.m20, m.m21, m.m22, m.m13},
             {m.m30, m.m31, m.m32, m.m33}};
    }

    public static double[][] toArray(MatrixSIS m) {
        final double[][] array = new double[m.getNumRow()][m.getNumCol()];
        for (int r = 0; r < array.length; r++) {
            for (int c = 0; c < array[0].length; c++) {
                array[r][c] = m.getElement(r, c);
            }
        }
        return array;
    }

    /**
     * Checks if the given matrix is the identity matrix.
     *
     * @param m matrix to test.
     * @return true if matrix is an Identity matrix.
     */
    public static boolean isIdentity(final double[][] m){
        if ( m.length!=m[0].length ) return false; // m must be a square matrix
        for ( int x=0; x<m[0].length; x++) {
            for ( int y=0; y<m.length; y++) {
                if (x==y){
                    if ( m[y][x] != 1 ) return false;
                } else {
                    if ( m[y][x] != 0 ) return false;
                }
            }
        }
        return true;
    }

    /**
     * Sets the given matrix to identity matrix.
     *
     * @param m a square matrix.
     * @throws IllegalArgumentException when m is not a square matrix.
     */
    public static void setToIdentity( double[][] m ){
        if (m.length != m[0].length) {
            throw new IllegalArgumentException("The m matrix must be a square matrix.");
        }
        for (int x = 0; x < m[0].length; x++) {
            for (int y = 0; y < m.length; y++) {
                if (x == y) {
                    m[y][x] = 1.;
                } else {
                    m[y][x] = 0.;
                }
            }
        }
    }

    /**
     * Creates a n*n identity matrix.
     *
     * @param n number of rows and columns.
     * @return an n*n identity matrix.
     */
    public static double[][] identity(int n) {
        double[][] identity = new double[n][n];
        for (int i = 0; i < n; i++) {
            identity[i][i] = 1.;
        }
        return identity;
    }

    /**
     * Adds m1 and m2, the result is stored in m1, returns m1.
     * see {@link Matrices#add(un.science.math.Matrix, un.science.math.Matrix, un.science.math.Matrix)}
     * @param m1 first matrix
     * @param m2 second matrix
     * @return m1 + m2 result in m1 matrix.
     */
    public static double[][] localAdd(final double[][] m1, final double[][] m2){
        return add(m1,m2,m1);
    }

    /**
     * Adds m1 and m2 matrices, the result is stored in buffer, returns buffer.
     * If buffer is null, a new matrix is created.
     * Matrices must have the same size.
     * @param m1 first matrix
     * @param m2 second matrix
     * @param buffer result buffer, can be null
     * @return m1 + m2 result in buffer matrix.
     * @throws IllegalArgumentException when matrices size differ.
     */
    public static double[][] add(final double[][] m1, final double[][] m2, double[][] buffer) throws IllegalArgumentException {
        if ( m1.length != m2.length || m1[0].length != m2[0].length ) {
            throw new IllegalArgumentException( "The two given matrix must have same dimensions." );
        }

        final int nbRow = m1.length;
        final int nbCol = m1[0].length;

        if (buffer == null){
            buffer = new double[nbRow][nbCol];
        }

        for (int x=0; x<nbCol;x++){
            for (int y=0; y<nbRow; y++){
                buffer[y][x] = m1[y][x] + m2[y][x];
            }
        }
        return buffer;
    }

    /**
     * Subtracts m2 to m1, the result is stored in m1, returns m1.
     * see {@link Matrices#subtract(un.science.math.Matrix, un.science.math.Matrix, un.science.math.Matrix)}
     * @param m1 first matrix
     * @param m2 second matrix
     * @return m1 - m2 result in m1 matrix.
     */
    public static double[][] localSubtract(final double[][] m1, final double[][] m2){
        return subtract(m1,m2,m1);
    }

    /**
     * Subtracts m2 to m2 matrices, result is stored in buffer, returns buffer.
     * If buffer is null, a new matrix is created.
     * Matrices must have the same size.
     * @param m1 first matrix
     * @param m2 second matrix
     * @param buffer result buffer, can be null
     * @return m1 - m2 result in buffer matrix.
     * @throws IllegalArgumentException when matrices size differ
     */
    public static double[][] subtract(final double[][] m1, final double[][] m2, double[][] buffer) throws IllegalArgumentException {
        if ( m1.length != m2.length || m1[0].length != m2[0].length ) {
            throw new IllegalArgumentException( "The two given matrix must have same dimensions." );
        }

        final int nbRow = m1.length;
        final int nbCol = m1[0].length;

        if (buffer == null){
            buffer = new double[nbRow][nbCol];
        }

        for (int x=0; x<nbCol;x++){
            for (int y=0; y<nbRow; y++){
                buffer[y][x] = m1[y][x] - m2[y][x];
            }
        }
        return buffer;
    }

    /**
     * Scales m1 by scaleFactor, result is stored in m1, returns m1.
     *
     * see {@link Matrices#scale(un.science.math.Matrix, un.science.math.Matrix, double)}
     * @param m1 input matrix
     * @param scaleFactor scale factor
     * @return matrix m1
     */
    public static double[][] localScale(final double[][] m1, final double scaleFactor) {
        return scale(m1,scaleFactor,m1);
    }

    /**
     * Scales matrix by columns.
     *
     * @param m1 input matrix
     * @param tuple scale factors (one by columns).
     * @return matrix m1
     */
    public static double[][] localScale(double[][] m1, double[] tuple) {
        return scale(m1,tuple,m1);
    }

    /**
     * Scales matrix by columns.
     * @param m1 input matrix
     * @param tuple scale
     * @param buffer result buffer, can be null
     * @return scaled matrix
     */
    public static double[][] scale( double[][] m1, double[] tuple, double[][] buffer) throws IllegalArgumentException {

        final int nbRow = m1.length;
        final int nbCol = m1[0].length;

        if ( buffer != null && ( nbRow != buffer.length || nbCol != buffer[0].length ) ) {
            throw new IllegalArgumentException( "The two given matrix must have same dimensions." );
        }
        if ( nbCol != tuple.length ) {
            throw new IllegalArgumentException( "tuple's argument size is incorrect." );
        }

        if (buffer == null){
            buffer = new double[nbRow][nbCol];
        }

        for (int x=0; x<nbCol;x++){
            for (int y=0; y<nbRow; y++){
                buffer[y][x] = m1[y][x] * tuple[x];
            }
        }
        return buffer;
    }

    /**
     * Scale m1 by scale, result is stored in buffer.
     * if buffer is null, a new matrix is created.
     * Matrices must have the same size
     * @param m1 input matrix
     * @param scale scale
     * @param buffer result buffer, can be null
     * @return scaled matrix
     * @throws IllegalArgumentException when matrices size differ
     */
    public static double[][] scale(final double[][] m1, final double scale, double[][] buffer) throws IllegalArgumentException {

        final int nbRow = m1.length;
        final int nbCol = m1[0].length;

        if (buffer == null){
            buffer = new double[nbRow][nbCol];
        }

        for (int x=0; x<nbCol;x++){
            for (int y=0; y<nbRow; y++){
                buffer[y][x] = m1[y][x] * scale;
            }
        }
        return buffer;
    }

    /**
     * return the transposed matrix.
     * (flips row/col values)
     * @param m1 input matrix
     * @return result matrix
     */
    public static double[][] transpose(final double[][] m1) throws IllegalArgumentException{

        final int nbRow = m1.length;
        final int nbCol = m1[0].length;

        final double[][] res = new double[nbCol][nbRow];

        for (int x=0; x<nbCol;x++){
            for (int y=0; y<nbRow; y++){
                res[x][y] = m1[y][x];
            }
        }
        return res;
    }

    /**
     * Matrix inversion using Gauss.
     *
     * @param origValues input matrix
     * @return inverted matrix or null if not possible.
     * @author Xavier Philippeau
     */
    public static double[][] localInvert(double[][] origValues) {
        return invert(origValues, origValues);
    }

    /**
     * Matrix inversion using Gauss.
     *
     * @param origValues input matrix
     * @param buffer result buffer, can be null
     * @return inverted matrix or null if not possible.
     * @author Xavier Philippeau
     */
    public static double[][] invert(double[][] origValues, double[][] buffer) {

        final int origNbRow = origValues.length;
        final int origNbCol = origValues[0].length;

        //Matrix can be inverted only if it is a square matrix.
        if (origNbRow != origNbCol) {
            return null;
        }

        // Create a temporary work matrix T = [ this | Identity ]
        final int tNbRow = origNbCol;
        final int tNbCol = origNbCol * 2;
        final double[][] tValues = new double[tNbRow][tNbCol];
        for (int y = 0; y < origNbRow; y++) {
            for (int x = 0; x < origNbCol; x++) {
                tValues[y][x] = origValues[y][x];
                if (x == y) {
                    tValues[y][origNbCol + x] = 1;
                }
            }
        }

        // Pour chaque ligne de la matrice T
        for (int x = 0; x < tNbRow; x++) {

            // Cherche la ligne avec le pivot max (en valeur absolue)
            int bestline = x;
            double pivot = tValues[bestline][x];
            for (int y = x + 1; y < tNbRow; y++) {
                if (Math.abs(tValues[y][x]) > Math.abs(pivot)) {
                    bestline = y;
                    pivot = tValues[bestline][x];
                }
            }

            if (pivot == 0) {
                System.err.println("Inversion : Le pivot est nul,inversion impossible !!");
                return null;
            }

            // Echange des lignes (si necessaire)
            if (bestline != x) {
                double tmp;
                for (int t = 0; t < tNbCol; t++) {
                    tmp = tValues[x][t];
                    tValues[x][t] = tValues[bestline][t];
                    tValues[bestline][t] = tmp;
                }
            }

            // Normalisation de la ligne du pivot
            for (int t = 0; t < tNbCol; t++) {
                tValues[x][t] /= pivot;
            }

            // elimination des autres lignes
            for (int y = 0; y < tNbRow; y++) {
                if (y == x) {
                    continue;
                }
                double coef = tValues[y][x];
                for (int t = 0; t < tNbCol; t++) {
                    tValues[y][t] -= coef * tValues[x][t];
                }
            }
        }

        // recupere la partie droite de T qui contient l'inverse de la matrice
        // (la partie gauche devrait contenir l'identité)
        if (buffer==null){
            buffer = new double[origNbRow][origNbCol];
        }
        for (int y = 0; y < origNbRow; y++) {
            for (int x = 0; x < origNbCol; x++) {
                buffer[y][x] = tValues[y][origNbCol + x];
            }
        }

        return buffer;
    }

    /**
     * Matrices dot product.
     *
     * @param m1 first matrix
     * @param m2 second matrix
     * @return dot product
     */
    public static double dot(double[][] m1, double[][] m2){
        final int nbRow = m1.length;
        final int nbCol = m1[0].length;

        double sum = 0;
        for (int r=0; r<nbRow; r++){
            for (int c=0; c<nbCol; c++){
                sum += m1[r][c] * m2[r][c];
            }
        }

        return sum;
    }

    /**
     * replace valeus close to 0 with zero, removing -0 if present
     * @param matrix
     * @param epsilon
     */
    public static void roundZeros(double[][] matrix, double epsilon){
        for (int x=0;x<matrix.length;x++){
            for (int y=0;y<matrix[0].length;y++){
                if (!(matrix[x][y]>epsilon || matrix[x][y]<-epsilon)){
                    matrix[x][y] = 0.0;
                }
            }
        }
    }

    public static Tuple transformLocal(final double[][] matrix, final Tuple vector) {
        return transform(matrix, vector, vector);
    }

    public static Tuple transform(final double[][] matrix, final Tuple vector, Tuple buffer) {
        if (buffer==null) buffer = Vectors.createDouble(matrix.length);
        final double[] array = new double[matrix.length];
        transform(matrix, vector.toArrayDouble(), array);
        buffer.set(array);
        return buffer;
    }

    /**
     * Transform given vector.
     *
     * @param matrix input matrix
     * @param vector vector to transform
     * @param buffer result vector buffer, can be null
     * @return the product of matrix and vector.
     */
    public static double[] transform(final double[][] matrix, final double[] vector, double[] buffer) {
        if (vector.length != matrix[0].length){
            throw new IllegalArgumentException("matrix column size and vector size differ : "+matrix[0].length+","+vector.length);
        }

        final int nbRow = matrix.length;
        final int nbCol = matrix[0].length;
        final double[] res = new double[nbRow];

        for (int r = 0; r < nbRow; r++) {
            double s = 0;
            for (int c = 0; c < nbCol; c++) {
                s += matrix[r][c] * vector[c];
            }
            res[r] = s;
        }

        if ( buffer == null ) {
            buffer = new double[matrix.length];
        }
        System.arraycopy(res, 0, buffer, 0, res.length);

        return buffer;
    }

    /**
     * Transforms given vector.
     *
     * @param matrix transformation matrix.
     * @param vector considered as a column matrix.
     * @param buffer result vector buffer, can be null
     * @return the product of matrix and vector.
     */
    public static float[] transform(final double[][] matrix, final float[] vector, float[] buffer) {
        if (vector.length != matrix[0].length){
            throw new IllegalArgumentException("matrix column size and vector size differ : "+matrix[0].length+","+vector.length);
        }

        if (buffer == null) {
            buffer = new float[matrix.length];
        }

        final int nbRow = matrix.length;
        final int nbCol = matrix[0].length;
        final float[] res = new float[nbRow];

        for (int r = 0; r < nbRow; r++) {
            double s = 0;
            for (int c = 0; c < nbCol; c++) {
                s += matrix[r][c] * vector[c];
            }
            res[r] = (float) s;
        }

        System.arraycopy(res, 0, buffer, 0, res.length);
        return buffer;
    }

    /**
     * Multiply m1 by m2, result is stored in m1, returns m1.
     * see {@link Matrices#multiply(un.science.math.Matrix, un.science.math.Matrix, un.science.math.Matrix)}
     */
    public static double[][] localMultiply(final double[][] m1, final double[][] m2){
        //
        if ( (m1.length != m1[0].length) || (m2.length != m2[0].length) || (m1.length != m2.length) ){
            throw new IllegalArgumentException("Matrices must both be square and have same size.");
        }
        return multiply(m1,m2, m1);
    }

    /**
     * Multiply m1 by m2 matrices, result is stored in buffer.
     * If buffer is null, a new matrix is created.
     */
    public static double[][] multiply(final double[][] m1, final double[][] m2, double[][] buffer) {

        final int m1r = m1.length;
        final int m1c = m1[0].length;
        final int m2r = m2.length;
        final int m2c = m2[0].length;

        if (m1c != m2r){
            throw new IllegalArgumentException("m1.nbCol is not equal to m2.nbRow");
        }

        if (buffer == null) {
            buffer = new double[m1r][m2c];
        } else {
            //check buffer size
            if (buffer.length != m1r || buffer[0].length != m2[0].length){
                throw new IllegalArgumentException("buffer.nbRow is not equal to m1.nbRow or buffer.nbCol is not equal to m2.nbCol");
            }
        }

        final double[][] temp = new double[m1r][m2c];
        for (int r=0; r<m1r; r++) {
            for (int c=0; c<m2c; c++) {
                temp[r][c] = 0.0;
                for (int k = 0; k < m1c; k++) {
                    temp[r][c] += m1[r][k] * m2[k][c];
                }
            }
        }

        for (int r=0; r<m1r; r++) {
            for (int c=0; c<m2c; c++) {
                buffer[r][c] = temp[r][c];
            }
        }

        return buffer;
    }

    /**
     * Create a rotation matrix from given angle and axis.
     *
     * http://en.wikipedia.org/wiki/Rotation_matrix
     *
     * @param angle rotation angle in radians
     * @param rotationAxis Tuple 3
     * @param buffer Matrix 3x3
     * @return rotation matrix
     */
    public static double[][] createRotation3(final double angle, final Tuple<?> rotationAxis, double[][] buffer) {

        if (buffer == null){
            buffer = new double[3][3];
        }

        final double fCos = Math.cos(angle);
        final double fSin = Math.sin(angle);
        final double fOneMinusCos = (1.0) - fCos;
        final double fX2 = rotationAxis.get(0) * rotationAxis.get(0);
        final double fY2 = rotationAxis.get(1) * rotationAxis.get(1);
        final double fZ2 = rotationAxis.get(2) * rotationAxis.get(2);
        final double fXYM = rotationAxis.get(0) * rotationAxis.get(1) * fOneMinusCos;
        final double fXZM = rotationAxis.get(0) * rotationAxis.get(2) * fOneMinusCos;
        final double fYZM = rotationAxis.get(1) * rotationAxis.get(2) * fOneMinusCos;
        final double fXSin = rotationAxis.get(0) * fSin;
        final double fYSin = rotationAxis.get(1) * fSin;
        final double fZSin = rotationAxis.get(2) * fSin;

        buffer[0][0] = fX2 * fOneMinusCos + fCos;
        buffer[0][1] = fXYM - fZSin;
        buffer[0][2] = fXZM + fYSin;
        buffer[1][0] = fXYM + fZSin;
        buffer[1][1] = fY2 * fOneMinusCos + fCos;
        buffer[1][2] = fYZM - fXSin;
        buffer[2][0] = fXZM - fYSin;
        buffer[2][1] = fYZM + fXSin;
        buffer[2][2] = fZ2 * fOneMinusCos + fCos;

        return buffer;
    }

    /**
     * Create a rotation matrix from given angle and axis.
     *
     * http://en.wikipedia.org/wiki/Rotation_matrix
     *
     * @param angle rotation angle in radians
     * @param rotationAxis Tuple 3
     * @param buffer Matrix 4x4
     * @return rotation matrix
     */
    public static double[][] createRotation4(final double angle, final Tuple<?> rotationAxis, double[][] buffer) {

        if (buffer == null){
            buffer = new double[4][4];
        }

        final double fCos = Math.cos(angle);
        final double fSin = Math.sin(angle);
        final double fOneMinusCos = (1.0) - fCos;
        final double fX2 = rotationAxis.get(0) * rotationAxis.get(0);
        final double fY2 = rotationAxis.get(1) * rotationAxis.get(1);
        final double fZ2 = rotationAxis.get(2) * rotationAxis.get(2);
        final double fXYM = rotationAxis.get(0) * rotationAxis.get(1) * fOneMinusCos;
        final double fXZM = rotationAxis.get(0) * rotationAxis.get(2) * fOneMinusCos;
        final double fYZM = rotationAxis.get(1) * rotationAxis.get(2) * fOneMinusCos;
        final double fXSin = rotationAxis.get(0) * fSin;
        final double fYSin = rotationAxis.get(1) * fSin;
        final double fZSin = rotationAxis.get(2) * fSin;

        buffer[0][0] = fX2 * fOneMinusCos + fCos;
        buffer[0][1] = fXYM - fZSin;
        buffer[0][2] = fXZM + fYSin;
        buffer[0][3] = 0;
        buffer[1][0] = fXYM + fZSin;
        buffer[1][1] = fY2 * fOneMinusCos + fCos;
        buffer[1][2] = fYZM - fXSin;
        buffer[1][3] = 0;
        buffer[2][0] = fXZM - fYSin;
        buffer[2][1] = fYZM + fXSin;
        buffer[2][2] = fZ2 * fOneMinusCos + fCos;
        buffer[2][3] = 0;
        buffer[3][0] = 0;
        buffer[3][1] = 0;
        buffer[3][2] = 0;
        buffer[3][3] = 1;

        return buffer;
    }

    /**
     * Create rotation matrix to move v1 on v2.
     *
     * @param v1
     * @param v2
     * @return
     */
    public static Matrix4 createRotation(Vector<?> v1, Vector<?> v2){
        v1 = v1.normalize();
        v2 = v2.normalize();
        final double angle = Math.acos(v1.dot(v2));
        if (angle==0){
            //vectors are colinear
            Matrix4 identity = new Matrix4();
            return identity;
        }
        final Vector axis = v1.copy().cross(v2).normalize();
        double[][] m = Matrices.createRotation4(angle, axis, null);
        return Matrices.toMatrix4(m);
    }

    /**
     * Build rotation matrix from Euler angle.
     * Sources :
     * http://en.wikipedia.org/wiki/Axes_conventions
     * http://jeux.developpez.com/faq/math/?page=transformations#Q36
     * http://en.wikipedia.org/wiki/Euler_angles
     * http://mathworld.wolfram.com/EulerAngles.html
     * http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToMatrix/
     *
     * Euler angle convention is : (Z-Y’-X’’) ISO 1151–2:1985
     * - Heading (yaw)      [-180°...+180°]
     * - Elevation (pitch)  [ -90°... +90°]
     * - Bank (roll)        [-180°...+180°]
     *
     * @param euler angles in radians (heading/yaw , elevation/pitch , bank/roll)
     * @param buffer size 4x4 or 3x3
     * @return matrix, never null
     */
    public static double[][] fromEuler(final double[] euler, double[][] buffer) {

        if (buffer == null){
            buffer = new double[4][4];
        }

        final double angle_x = euler[2];
        final double angle_y = euler[1];
        final double angle_z = euler[0];
        //check the given values are valid
        //CObjects.ensureBetween(angle_x, -Maths.PI, +Maths.PI);
        //CObjects.ensureBetween(angle_y, -Maths.HALF_PI, +Maths.HALF_PI);
        //CObjects.ensureBetween(angle_z, -Maths.PI, +Maths.PI);

        final double cx = Math.cos(angle_x);
        final double sx = Math.sin(angle_x);
        final double cy = Math.cos(angle_y);
        final double sy = Math.sin(angle_y);
        final double cz = Math.cos(angle_z);
        final double sz = Math.sin(angle_z);


        // cy*cz    sx*sy*cz - cx*sz    cx*sy*cz + sx*sz
        // cy*sz    sx*sy*sz + cx*cz    cx*sy*sz - sx*cz
        // -sy            sx*cy              cx*cy

        buffer[0][0] = cy*cz;
        buffer[0][1] = sx*sy*cz - cx*sz;
        buffer[0][2] = cx*sy*cz + sx*sz;

        buffer[1][0] = cy*sz;
        buffer[1][1] = sx*sy*sz + cx*cz;
        buffer[1][2] = cx*sy*sz - sx*cz;

        buffer[2][0] = -sy;
        buffer[2][1] = sx*cy;
        buffer[2][2] = cx*cy;

        //fill 4 dimension
        if (buffer.length>3){
            buffer[0][3]  =  0;
            buffer[1][3]  =  0;
            buffer[2][3]  =  0;

            buffer[3][0]  =  0;
            buffer[3][1]  =  0;
            buffer[3][2]  =  0;
            buffer[3][3]  =  1;
        }

        return buffer;
    }

    /**
     * Calculate Euler angle of given matrix.
     * Source :
     * http://www.soi.city.ac.uk/~sbbh653/publications/euler.pdf
     * http://jeux.developpez.com/faq/math/?page=transformations#Q37
     *
     * @param mat input matrix
     * @param buffer euler buffer, can be null
     * @return euler angle in radians (heading/yaw , elevation/pitch , bank/roll)
     */
    public static double[] toEuler(double[][] mat, double[] buffer){

        if (buffer == null){
            buffer = new double[3];
        }

        double angle_x;
        double angle_y;
        double angle_z;

        if (mat[2][0] != -1 && mat[2][0] != +1){
            //first possible solution
            angle_y = -Math.asin(mat[2][0]);
            double cosy1 = Math.cos(angle_y);
            angle_x = Math.atan2(mat[2][1]/cosy1, mat[2][2]/cosy1);
            angle_z = Math.atan2(mat[1][0]/cosy1, mat[0][0]/cosy1);

            //second possible solution
            //angle_y = Angles.PI - y1;
            //double cosy2 = Math.cos(angle_y);
            //angle_x = Math.atan2(mat[2][1]/cosy2, mat[2][2]/cosy2);
            //angle_z = Math.atan2(mat[1][0]/cosy2, mat[0][0]/cosy2);

        } else {
            // Gimbal lock
            angle_z = 0;
            if (mat[2][0] == -1){
                angle_y = Math.PI / 2.0;
                angle_x = angle_z + Math.atan2(mat[0][1], mat[0][2]);
            } else {
                angle_y = -Math.PI / 2.0;
                angle_x = -angle_z + Math.atan2(-mat[0][1], -mat[0][2]);
            }
        }


        buffer[0] = angle_z;
        buffer[1] = angle_y;
        buffer[2] = angle_x;
        return buffer;
    }

    public static Matrix3 createFromUpAndRight(Vector<?> v, Vector<?> u) {
        v = v.copy();
        u = u.copy();
        v.normalize();
        u.normalize();

        //W = Normalized(Cross(V,U))
        Vector w = v.cross(u);
        w.normalize();

        //to ensure it is correctly perpendicular
        //U = Normalized(Cross(W,V))
        u = w.cross(v);
        u.normalize();

        return new Matrix3(
                u.get(0), w.get(0), v.get(0),
                u.get(1), w.get(1), v.get(1),
                u.get(2), w.get(2), v.get(2));
    }

    /**
     * Decompose a matrix in rotation, scale and translation.
     */
    public static void decomposeMatrix(Matrix4 trs, Matrix3 rotation, Tuple scale, Tuple translation){
        final double scaleX = Math.sqrt(trs.m00 * trs.m00 + trs.m10 * trs.m10 + trs.m20 * trs.m20);
        final double scaleY = Math.sqrt(trs.m01 * trs.m01 + trs.m11 * trs.m11 + trs.m21 * trs.m21);
        final double scaleZ = Math.sqrt(trs.m02 * trs.m02 + trs.m12 * trs.m12 + trs.m22 * trs.m22);
        final double[] invertScale = new double[]{1d / scaleX, 1d / scaleY, 1d / scaleZ};
        rotation.m00 = trs.m00 * invertScale[0]; rotation.m01 = trs.m01 * invertScale[1]; rotation.m02 = trs.m02 * invertScale[2];
        rotation.m10 = trs.m10 * invertScale[0]; rotation.m11 = trs.m11 * invertScale[1]; rotation.m12 = trs.m12 * invertScale[2];
        rotation.m20 = trs.m20 * invertScale[0]; rotation.m21 = trs.m21 * invertScale[1]; rotation.m22 = trs.m22 * invertScale[2];
        scale.set(0, scaleX);
        scale.set(1, scaleY);
        scale.set(2, scaleZ);
        translation.set(0,trs.getElement(0,3));
        translation.set(1,trs.getElement(1,3));
        translation.set(2,trs.getElement(2,3));
    }
}
