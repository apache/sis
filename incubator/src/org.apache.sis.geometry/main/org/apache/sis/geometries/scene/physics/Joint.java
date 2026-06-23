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
package org.apache.sis.geometries.scene.physics;

import java.util.List;
import org.apache.sis.geometries.scene.SceneNode;


/**
 * Constrains the motion of this node relative to another.
 *
 * @author Johann Sorel
 * @spec ISO_12113 KHR_physics_rigid_bodies extension, Joint
 * @see https://github.com/eoineoineoin/glTF_Physics/tree/master/extensions/2.0/Khronos/KHR_physics_rigid_bodies#joints
 */
public final class Joint {

    private SceneNode connectedNode;
    private List<Limit> limits;
    private List<Drive> drives;
    private boolean enableCollision;

    /**
     * Node to use for one attachment frame.
     *
     * @return the connectedNode
     */
    public SceneNode getConnectedNode() {
        return connectedNode;
    }

    /**
     * @param connectedNode the connectedNode to set
     */
    public void setConnectedNode(SceneNode connectedNode) {
        this.connectedNode = connectedNode;
    }

    /**
     * @return the limits
     */
    public List<Limit> getLimits() {
        return limits;
    }

    /**
     * @param limits the limits to set
     */
    public void setLimits(List<Limit> limits) {
        this.limits = limits;
    }

    /**
     * @return the drives
     */
    public List<Drive> getDrives() {
        return drives;
    }

    /**
     * @param drives the drives to set
     */
    public void setDrives(List<Drive> drives) {
        this.drives = drives;
    }

    /**
     * Flag which controls collision detection between the constrained objects
     *
     * @return the enableCollision
     */
    public boolean isEnableCollision() {
        return enableCollision;
    }

    /**
     * @param enableCollision the enableCollision to set
     * @see #isEnableCollision()
     */
    public void setEnableCollision(boolean enableCollision) {
        this.enableCollision = enableCollision;
    }

    public static final class Limit {
        private int[] linearAxes;
        private int[] angularAxes;
        private double min;
        private double max;
        private double stiffness;
        private double damping;

        /**
         * The linear axes to constrain (0=X, 1=Y, 2=Z).
         *
         * @return the linearAxes
         */
        public int[] getLinearAxes() {
            return linearAxes;
        }

        /**
         * @param linearAxes the linearAxes to set
         */
        public void setLinearAxes(int[] linearAxes) {
            this.linearAxes = linearAxes;
        }

        /**
         * The angular axes to constrain (0=X, 1=Y, 2=Z).
         *
         * @return the angularAxes
         */
        public int[] getAngularAxes() {
            return angularAxes;
        }

        /**
         * @param angularAxes the angularAxes to set
         */
        public void setAngularAxes(int[] angularAxes) {
            this.angularAxes = angularAxes;
        }

        /**
         * The minimum allowed relative distance/angle.
         *
         * @return the min
         */
        public double getMin() {
            return min;
        }

        /**
         * @param min the min to set
         */
        public void setMin(double min) {
            this.min = min;
        }

        /**
         * The maximum allowed relative distance/angle.
         *
         * @return the max
         */
        public double getMax() {
            return max;
        }

        /**
         * @param max the max to set
         */
        public void setMax(double max) {
            this.max = max;
        }

        /**
         * Optional softness of the limits when beyond the limits.
         * In Newton per meter (N·m-1) for linear limits .
         * In Newton meter per radian (N·m·rad-1) for angular limits.
         *
         * @return the stiffness
         */
        public double getStiffness() {
            return stiffness;
        }

        /**
         * @param stiffness the stiffness to set
         */
        public void setStiffness(double stiffness) {
            this.stiffness = stiffness;
        }

        /**
         * Optional spring damping applied when beyond the limits.
         * In Newton second per meter (N·s·m-1) for linear limits.
         * In Newton second meter per radian (N·s·m·rad-1) for angular limits.
         *
         * @return the damping
         */
        public double getDamping() {
            return damping;
        }

        /**
         * @param damping the damping to set
         */
        public void setDamping(double damping) {
            this.damping = damping;
        }

    }

    public static final class Drive {
        public static enum Type {
            LINEAR,
            ANGULAR
        }

        public static enum Mode {
            FORCE,
            ACCELERATION
        }

        private Type type;
        private Mode mode;
        private int axis;
        private double maxForce;
        private double positionTarget;
        private double velocityTarget;
        private double stiffness;
        private double damping;

        /**
         * Determines if the drive affects is a linear or angular drive
         *
         * @return the type
         */
        public Type getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(Type type) {
            this.type = type;
        }

        /**
         * Determines if the drive is operating in force or acceleration mode
         *
         * @return the mode
         */
        public Mode getMode() {
            return mode;
        }

        /**
         * @param mode the mode to set
         */
        public void setMode(Mode mode) {
            this.mode = mode;
        }

        /**
         * The index of the axis which this drive affects
         *
         * @return the axis
         */
        public int getAxis() {
            return axis;
        }

        /**
         * @param axis the axis to set
         */
        public void setAxis(int axis) {
            this.axis = axis;
        }

        /**
         * The maximum force that the drive can apply
         *
         * @return the maxForce
         */
        public double getMaxForce() {
            return maxForce;
        }

        /**
         * @param maxForce the maxForce to set
         */
        public void setMaxForce(double maxForce) {
            this.maxForce = maxForce;
        }

        /**
         * The desired relative target between the pivot axes
         *
         * @return the positionTarget
         */
        public double getPositionTarget() {
            return positionTarget;
        }

        /**
         * @param positionTarget the positionTarget to set
         */
        public void setPositionTarget(double positionTarget) {
            this.positionTarget = positionTarget;
        }

        /**
         * The desired relative velocity of the pivot axes
         *
         * @return the velocityTarget
         */
        public double getVelocityTarget() {
            return velocityTarget;
        }

        /**
         * @param velocityTarget the velocityTarget to set
         */
        public void setVelocityTarget(double velocityTarget) {
            this.velocityTarget = velocityTarget;
        }

        /**
         * The drive's stiffness, used to achieve the position target
         * In Newton per meter (N·m-1) for linear limits .
         * In Newton meter per radian (N·m·rad-1) for angular limits.
         *
         * @return the stiffness
         */
        public double getStiffness() {
            return stiffness;
        }

        /**
         * @param stiffness the stiffness to set
         */
        public void setStiffness(double stiffness) {
            this.stiffness = stiffness;
        }

        /**
         * The damping factor applied to reach the velocity target
         * In Newton second per meter (N·s·m-1) for linear limits.
         * In Newton second meter per radian (N·s·m·rad-1) for angular limits.
         *
         * @return the damping
         */
        public double getDamping() {
            return damping;
        }

        /**
         * @param damping the damping to set
         */
        public void setDamping(double damping) {
            this.damping = damping;
        }

    }
}
