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
package org.apache.sis.gui.map;

import javafx.concurrent.Task;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;


/**
 * Base class of tasks executed in background thread for doing rendering.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <V>  type of value computed by the task.
 *
 * @see MapCanvas.Renderer
 * @see MapCanvas#renderingCompleted(RenderingTask)
 */
abstract class RenderingTask<V> extends Task<V> {
    /**
     * The {@link MapCanvas#transform} values at the time the {@link MapCanvas#repaint()} method has been invoked.
     * This is a change applied on {@link MapCanvas#objectiveToDisplay} but not yet visible in the map.
     */
    private Transform changeInProgress;

    /**
     * Creates a new rendering task.
     */
    RenderingTask() {
    }

    /**
     * Takes a copy of the given transform. This method may use an implementation simpler than {@link Affine},
     * such as {@link Translate}, because the transform stored by this method may become part of the transform
     * chain of graphics such as {@link MapCanvas.StaticGraphics}.
     *
     * @param  transform  value of {@link MapCanvas#transform}.
     */
    final void setChangeInProgress(final Affine transform) {
        if (transform.getMxx() == 1 && transform.getMxy() == 0 && transform.getMxz() == 0 &&
            transform.getMyx() == 0 && transform.getMyy() == 1 && transform.getMyz() == 0 &&
            transform.getMzx() == 0 && transform.getMzy() == 0 && transform.getMzz() == 1)
        {
            // Pans are a very frequent operations.
            changeInProgress = new Translate(transform.getTx(), transform.getTy(), transform.getTz());
        } else {
            changeInProgress = new Affine(transform);
        }
    }

    /**
     * Returns the transform specified by the call to {@link #setChangeInProgress(Affine)}.
     * The returned value is not copied, caller should not modify it.
     */
    final Transform getChangeInProgress() {
        return changeInProgress;
    }
}
