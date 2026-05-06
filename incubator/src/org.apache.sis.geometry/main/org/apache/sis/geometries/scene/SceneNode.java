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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opengis.feature.Feature;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometries.Geometries;
import org.apache.sis.geometries.math.Similarity;
import org.apache.sis.geometries.math.Similarity3D;
import org.apache.sis.geometries.scene.physics.Collider;
import org.apache.sis.geometries.scene.physics.Joint;
import org.apache.sis.geometries.scene.physics.Motion;
import org.apache.sis.geometries.scene.physics.Trigger;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Utilities;


/**
 * A scene node.
 * This class is used by scenograph rendering pipelines.
 * Each node has a transform and possible childrens.
 *
 * @author Johann Sorel (Geomatys)
 */
public class SceneNode {

    private final Similarity3D parentToNode = new Similarity3D();
    private SceneNode parent = null;
    private final List<SceneNode> children = new NotifiedCheckedList<SceneNode>(){
        @Override
        protected void notifyAdd(SceneNode item, int index) {
            makeChild(item);
        }

        @Override
        protected void notifyAdd(Collection<? extends SceneNode> items, NumberRange<Integer> range) {
            for (SceneNode n : items) {
                makeChild(n);
            }
        }

        @Override
        protected void notifyChange(SceneNode oldItem, SceneNode newItem, int index) {
            unmakeChild(oldItem);
            makeChild(newItem);
        }

        @Override
        protected void notifyRemove(SceneNode item, int index) {
            unmakeChild(item);
        }

        @Override
        protected void notifyRemove(Collection<? extends SceneNode> items, NumberRange<Integer> range) {
            for (SceneNode n : items) {
                unmakeChild(n);
            }
        }

        private void makeChild(SceneNode node) {
            if (!Utilities.equalsIgnoreMetadata(getCoordinateReferenceSystem(), node.getCoordinateReferenceSystem())) {
                super.remove(node);
                throw new IllegalArgumentException("Child node coordinate system do not match scene coordinate system");
            }

            final SceneNode oldParent = node.getParent();
            if (oldParent != null) oldParent.getChildren().remove(node);
            node.setParent(SceneNode.this);
        }

        private void unmakeChild(SceneNode node) {
            node.setParent(null);
        }
    };

    private CoordinateReferenceSystem crs = Geometries.RIGHT_HAND_3D;
    private Camera camera;
    private Model model;
    private String name;
    private Feature feature;
    private Skin skin;
    private Motion motion;
    private Collider collider;
    private Trigger trigger;
    private Joint joint;
    private final List<Animation> animations = new ArrayList<>();
    //user properties
    private Map<String,Object> properties;

    /**
     * Build a 3D Right handed coordinate reference system.
     */
    public SceneNode() {
    }

    /**
     * Build scene node with given system.
     * @param crs not null, must have three dimensions
     */
    public SceneNode(CoordinateReferenceSystem crs) {
        setCoordinateReferenceSystem(crs);
    }

    /**
     * Build scene node with given model.
     * Model coordinate system is copied to scene node.
     * @param model, not null
     */
    public SceneNode(Model model) {
        ArgumentChecks.ensureNonNull("model", model);
        setCoordinateReferenceSystem(model.getCoordinateReferenceSystem());
        this.model = model;
    }

    /**
     * @return node name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name node name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return scene node coordinate system, never null.
     */
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    /**
     * This method will modify :
     * - model crs if defined
     * - children nodes coordinate system recursively.
     * @param crs
     */
    public void setCoordinateReferenceSystem(CoordinateReferenceSystem crs) {
        if (parent != null) {
            throw new IllegalArgumentException("Changing CRS can only be called on root node");
        }
        ArgumentChecks.ensureNonNull("crs", crs);
        ArgumentChecks.ensureCountBetween("dimension", true, 3, 3, crs.getCoordinateSystem().getDimension());
        setCrsNoCheck(crs);
    }

    private void setCrsNoCheck(CoordinateReferenceSystem crs) {
        if (model != null) {
            model.setCoordinateReferenceSystem(crs);
        }
        for (SceneNode sn : children) {
            sn.setCrsNoCheck(crs);
        }
        this.crs = crs;
    }

    /**
     * Get transform from parent node to this node.
     *
     * @return Transform, never null.
     */
    public Similarity3D getTransform() {
        return parentToNode;
    }

    /**
     * @return parent node
     */
    public SceneNode getParent() {
        return parent;
    }

    /**
     * Internal use only.
     * Called by the new parent node only.
     */
    public void setParent(SceneNode parent) {
        this.parent = parent;
    }

    /**
     * @return modifiable list of children nodes.
     */
    public List<SceneNode> getChildren() {
        return children;
    }

    /**
     * @return 3D model attached, may be null
     */
    public Model getModel() {
        return model;
    }

    /**
     * @param model 3D model to attach, may be null
     *        model must have the same crs as the scene node.
     */
    public void setModel(Model model) {
        if (model != null) {
            if (!Utilities.equalsIgnoreMetadata(getCoordinateReferenceSystem(), model.getCoordinateReferenceSystem())) {
                throw new IllegalArgumentException("Model coordinate system do not match scene coordinate system");
            }
        }
        this.model = model;
    }

    /**
     * @return camera attached, may be null
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * @param camera to attach, may be null
     */
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    /**
     * @return skin attached, may be null
     */
    public Skin getSkin() {
        return skin;
    }

    /**
     * @param skin model skin to attach, may be null
     */
    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    /**
     * @return node motion, can be null
     */
    public Motion getMotion() {
        return motion;
    }

    /**
     * @param motion set motion
     */
    public void setMotion(Motion motion) {
        this.motion = motion;
    }

    /**
     * @return node collider, can be null
     */
    public Collider getCollider() {
        return collider;
    }

    /**
     * @param collider set collider
     */
    public void setCollider(Collider collider) {
        this.collider = collider;
    }

    /**
     * @return node trigger, can be null
     */
    public Trigger getTrigger() {
        return trigger;
    }

    /**
     * @param trigger set trigger
     */
    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    /**
     * @return node joint, can be null
     */
    public Joint getJoint() {
        return joint;
    }

    /**
     * @param joint set joint
     */
    public void setJoint(Joint joint) {
        this.joint = joint;
    }

    /**
     * @return animations attached to this node
     */
    public List<Animation> getAnimations() {
        return animations;
    }

    /**
     * @return Feature this node represent
     *         used to attach atttributes on scene models.
     */
    public Feature getFeature() {
        return feature;
    }

    /**
     * @param feature Feature this node represent.
     */
    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    /**
     * Get this scene envelope, including child nodes.
     *
     * @param applyTransform true to transform the envelope with the node transform.
     *                    the envelope will be in the parent coordinate system.
     * @return node envelope, can be null if no model 3d are attached in the graph.
     */
    public Optional<Envelope> getEnvelope(boolean applyTransform) throws NoninvertibleTransformException, TransformException {

        GeneralEnvelope e = null;

        // add model envelope
        final Model model = getModel();
        if (model != null) {
            Envelope me = model.getEnvelope();
            e = new GeneralEnvelope(me);
            //we ignore the model crs, often undefined
            e.setCoordinateReferenceSystem(getCoordinateReferenceSystem());
            if (e.isAllNaN()) {
                e = null;
            }
        }

        // concatenate child node envelopes
        for (SceneNode sn : children) {
            Optional<Envelope> envelope = sn.getEnvelope(true);
            if (envelope.isPresent()) {
                final Envelope ce = envelope.get();
                if (!Utilities.equalsIgnoreMetadata(getCoordinateReferenceSystem(), ce.getCoordinateReferenceSystem())) {
                    throw new IllegalArgumentException("A child node coordinate system do not match scene coordinate system");
                }
                if (e == null) {
                    e = new GeneralEnvelope(ce);
                } else {
                    e.add(ce);
                }
            }
        }

        // apply transform
        if (applyTransform && e != null) {
            final Similarity<?> transform = getTransform();
            if (!transform.isIdentity()) {
                LinearTransform trs = MathTransforms.linear(transform.toMatrix().toMatrixSIS());
                e.setEnvelope(Envelopes.transform(trs, e));
            }
        }

        return Optional.ofNullable(e);
    }

    /**
     * Map of properties for user needs.
     * Those informations may be lost in node processes.
     *
     * @return Map, never null.
     */
    public synchronized Map<String, Object> userProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Node");
        if (name != null) sb.append(' ').append(name).append(' ');
        sb.append('(');
        if (model != null) sb.append(" Model ");
        if (camera != null) sb.append(" Camera ");
        if (feature != null) sb.append(" Feature ");
        sb.append(" Children[").append(children.size()).append("] ");
        sb.append(')');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.parentToNode);
        hash = 97 * hash + Objects.hashCode(this.camera);
        hash = 97 * hash + Objects.hashCode(this.model);
        hash = 97 * hash + Objects.hashCode(this.name);
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
        final SceneNode other = (SceneNode) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.parentToNode, other.parentToNode)) {
            return false;
        }
        if (!Objects.equals(this.children, other.children)) {
            return false;
        }
        if (!Objects.equals(this.camera, other.camera)) {
            return false;
        }
        if (!Objects.equals(this.model, other.model)) {
            return false;
        }
        if (!Objects.equals(this.skin, other.skin)) {
            return false;
        }
        if (!Objects.equals(this.motion, other.motion)) {
            return false;
        }
        if (!Objects.equals(this.collider, other.collider)) {
            return false;
        }
        if (!Objects.equals(this.trigger, other.trigger)) {
            return false;
        }
        if (!Objects.equals(this.joint, other.joint)) {
            return false;
        }
        if (!Objects.equals(this.animations, other.animations)) {
            return false;
        }
        return true;
    }

    private abstract class NotifiedCheckedList<E> extends ArrayList<E>{

        public NotifiedCheckedList() {
            super();
        }

        public NotifiedCheckedList(final int capacity) {
            super(capacity);
        }

        protected abstract void notifyAdd(final E item, int index);

        protected abstract void notifyAdd(final Collection<? extends E> items, NumberRange<Integer> range);

        protected abstract void notifyChange(final E oldItem, E newItem, int index);

        protected abstract void notifyRemove(final E item, int index);

        protected abstract void notifyRemove(final Collection<? extends E> items, NumberRange<Integer> range);

        @Override
        public boolean add(final E element) throws IllegalArgumentException, UnsupportedOperationException {
            if(element == null) return false;
            final boolean added = super.add(element);
            if (added) {
                final int index = super.size() - 1;
                notifyAdd(element, index);
            }
            return added;
        }

        @Override
        public void add(final int index, final E element) throws IllegalArgumentException, UnsupportedOperationException {
            super.add(index, element);
            notifyAdd(element, index);
        }

        @Override
        public E set(int index, E element) throws IllegalArgumentException, UnsupportedOperationException {
            final E old = super.set(index, element);
            notifyChange(old, element, index);
            return old;
        }

        @Override
        public boolean addAll(final Collection<? extends E> collection) throws IllegalArgumentException, UnsupportedOperationException {
            final int startIndex = super.size();
            final boolean added = super.addAll(collection);
            if (added) {
                notifyAdd(collection, NumberRange.create(startIndex, true, super.size()-1, true) );
            }
            return added;
        }

        @Override
        public boolean addAll(final int index, final Collection<? extends E> collection) throws IllegalArgumentException, UnsupportedOperationException {
            final boolean added = super.addAll(index, collection);
            if (added) {
                notifyAdd(collection, NumberRange.create(index, true, index + collection.size(), true) );
            }
            return added;
        }

        @Override
        public boolean remove(final Object o) throws UnsupportedOperationException {
            final int index = super.indexOf(o);
            if (index >= 0) {
                super.remove(index);
                notifyRemove((E) o, index );
                return true;
            }
            return false;
        }

        @Override
        public E remove(final int index) throws UnsupportedOperationException {
            final E removed = super.remove(index);
            notifyRemove(removed, index );
            return removed;
        }

        @Override
        public boolean removeAll(final Collection<?> c) throws UnsupportedOperationException {
            //TODO handle remove by collection events if possible
            // to avoid several calls to remove
            boolean valid = false;
            for(final Object i : c){
                final boolean val = remove(i);
                if(val) valid = val;
            }
            return valid;
        }

        @Override
        public void clear() throws UnsupportedOperationException {
            if(!isEmpty()){
                final Collection<E> copy = new ArrayList<>(this);
                final NumberRange<Integer> range = NumberRange.create(0, true, copy.size()-1, true);
                super.clear();
                notifyRemove(copy, range);
            }
        }

    }

}
