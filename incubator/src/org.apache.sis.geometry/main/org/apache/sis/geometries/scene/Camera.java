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
package org.apache.sis.geometries.scene;

import java.util.Objects;

/**
 * Base class for Camera types.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract sealed class Camera permits Camera.Orthographic, Camera.Perspective {

    Camera(){

    }

    /**
    * Orthographic camera definition.
    */
   public static final class Orthographic extends Camera {

       private double xmag;
       private double ymag;
       private double zfar;
       private double znear;

       /**
        * @return The floating-point horizontal magnification of the view.Must not be zero.
        */
       public double getXmag() {
           return xmag;
       }

       /**
        * @param xmag The floating-point horizontal magnification of the view.Must not be zero.
        */
       public void setXmag(double xmag) {
           this.xmag = xmag;
       }

       /**
        * @return The floating-point vertical magnification of the view.Must not be zero.
        */
       public double getYmag() {
           return ymag;
       }

       /**
        * @param ymag The floating-point vertical magnification of the view.Must not be zero.
        */
       public void setYmag(double ymag) {
           this.ymag = ymag;
       }

       /**
        * @return The floating-point distance to the far clipping plane.`zfar` must be greater than `znear`.
        */
       public double getZfar() {
           return zfar;
       }

       /**
        * @param zfar The floating-point distance to the far clipping plane.`zfar` must be greater than `znear`.
        */
       public void setZfar(double zfar) {
           this.zfar = zfar;
       }

       /**
        * @return The floating-point distance to the near clipping plane.
        */
       public double getZnear() {
           return znear;
       }

       /**
        * @param znear The floating-point distance to the near clipping plane.
        */
       public void setZnear(double znear) {
           this.znear = znear;
       }

       @Override
       public int hashCode() {
           int hash = 7;
           hash = 53 * hash + (int) (Double.doubleToLongBits(this.xmag) ^ (Double.doubleToLongBits(this.xmag) >>> 32));
           hash = 53 * hash + (int) (Double.doubleToLongBits(this.ymag) ^ (Double.doubleToLongBits(this.ymag) >>> 32));
           hash = 53 * hash + (int) (Double.doubleToLongBits(this.zfar) ^ (Double.doubleToLongBits(this.zfar) >>> 32));
           hash = 53 * hash + (int) (Double.doubleToLongBits(this.znear) ^ (Double.doubleToLongBits(this.znear) >>> 32));
           return hash;
       }

       @Override
       public boolean equals(Object obj) {
           if (this == obj) {
               return true;
           }
           if (obj == null) {
               return false;
           }
           if (getClass() != obj.getClass()) {
               return false;
           }
           final Orthographic other = (Orthographic) obj;
           if (Double.doubleToLongBits(this.xmag) != Double.doubleToLongBits(other.xmag)) {
               return false;
           }
           if (Double.doubleToLongBits(this.ymag) != Double.doubleToLongBits(other.ymag)) {
               return false;
           }
           if (Double.doubleToLongBits(this.zfar) != Double.doubleToLongBits(other.zfar)) {
               return false;
           }
           if (Double.doubleToLongBits(this.znear) != Double.doubleToLongBits(other.znear)) {
               return false;
           }
           return true;
       }

   }

   /**
    * Perspective camera definition.
    */
   public static final class Perspective extends Camera {

       private Double aspectRatio;
       private double yfov;
       private Double zfar;
       private double znear;

       /**
        * @return The floating-point aspect ratio of the field of view.
        */
       public Double getAspectRatio() {
           return aspectRatio;
       }

       /**
        * @param aspectRatio The floating-point aspect ratio of the field of view.
        */
       public void setAspectRatio(Double aspectRatio) {
           this.aspectRatio = aspectRatio;
       }

       /**
        * @return The floating-point vertical field of view in radians.(Required)
        */
       public double getYfov() {
           return yfov;
       }

       /**
        * @param yfov The floating-point vertical field of view in radians.(Required)
        */
       public void setYfov(double yfov) {
           this.yfov = yfov;
       }

       /**
        * @return The floating-point distance to the far clipping plane.
        */
       public Double getZfar() {
           return zfar;
       }

       /**
        * @param zfar The floating-point distance to the far clipping plane.
        */
       public void setZfar(Double zfar) {
           this.zfar = zfar;
       }

       /**
        * @return The floating-point distance to the near clipping plane.(Required)
        */
       public double getZnear() {
           return znear;
       }

       /**
        * @param znear The floating-point distance to the near clipping plane.(Required)
        */
       public void setZnear(double znear) {
           this.znear = znear;
       }

       @Override
       public int hashCode() {
           int hash = 7;
           hash = 71 * hash + Objects.hashCode(this.aspectRatio);
           hash = 71 * hash + (int) (Double.doubleToLongBits(this.yfov) ^ (Double.doubleToLongBits(this.yfov) >>> 32));
           hash = 71 * hash + Objects.hashCode(this.zfar);
           hash = 71 * hash + (int) (Double.doubleToLongBits(this.znear) ^ (Double.doubleToLongBits(this.znear) >>> 32));
           return hash;
       }

       @Override
       public boolean equals(Object obj) {
           if (this == obj) {
               return true;
           }
           if (obj == null) {
               return false;
           }
           if (getClass() != obj.getClass()) {
               return false;
           }
           final Perspective other = (Perspective) obj;
           if (Double.doubleToLongBits(this.yfov) != Double.doubleToLongBits(other.yfov)) {
               return false;
           }
           if (Double.doubleToLongBits(this.znear) != Double.doubleToLongBits(other.znear)) {
               return false;
           }
           if (!Objects.equals(this.aspectRatio, other.aspectRatio)) {
               return false;
           }
           if (!Objects.equals(this.zfar, other.zfar)) {
               return false;
           }
           return true;
       }

   }

}
