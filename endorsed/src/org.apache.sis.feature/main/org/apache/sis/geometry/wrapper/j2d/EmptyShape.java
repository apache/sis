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
package org.apache.sis.geometry.wrapper.j2d;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.util.NoSuchElementException;
import org.apache.sis.referencing.internal.shared.AbstractShape;


/**
 * An empty shape.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class EmptyShape extends AbstractShape implements Serializable, PathIterator {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -45089382216341034L;

    /**
     * The unique empty shape instance.
     */
    public static final EmptyShape INSTANCE = new EmptyShape();

    /**
     * For {@link #INSTANCE} construction only.
     */
    private EmptyShape() {
    }

    /** Returns an empty bounds. */
    @Override public Rectangle    getBounds()                                          {return new Rectangle();}
    @Override public Rectangle2D  getBounds2D()                                        {return new Rectangle();}
    @Override public int          getWindingRule()                                     {return WIND_NON_ZERO;}
    @Override public boolean      contains  (Point2D p)                                {return false;}
    @Override public boolean      contains  (Rectangle2D r)                            {return false;}
    @Override public boolean      intersects(Rectangle2D r)                            {return false;}
    @Override public boolean      contains  (double x, double y)                       {return false;}
    @Override public boolean      contains  (double x, double y, double w, double h)   {return false;}
    @Override public boolean      intersects(double x, double y, double w, double h)   {return false;}
    @Override public PathIterator getPathIterator(AffineTransform at)                  {return this;}
    @Override public PathIterator getPathIterator(AffineTransform at, double flatness) {return this;}
    @Override public boolean      isDone()                                             {return true;}
    @Override public void         next()                                               {throw new NoSuchElementException();}
    @Override public int          currentSegment( float[] coords)                      {throw new NoSuchElementException();}
    @Override public int          currentSegment(double[] coords)                      {throw new NoSuchElementException();}

    /**
     * Invoked at deserialization time for obtaining the unique instance of this shape.
     *
     * @return the unique {@code Shape} instance for this class.
     * @throws ObjectStreamException if the object state is invalid.
     */
    private Object readResolve() throws ObjectStreamException {
        return INSTANCE;
    }
}
