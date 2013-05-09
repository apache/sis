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
package org.apache.sis.util;


/**
 * A resource that can be disposed when waiting for the garbage collector would be overly
 * conservative. Invoking the {@link #dispose()} method allows any resources held by this
 * object to be released. The result of calling any other method subsequent to a call to
 * this method is undefined.
 *
 * {@section Relationship with <code>Closeable</code>}
 * Some SIS classes may implement both the {@code Disposeable} and {@link java.io.Closeable}
 * interfaces. While very similar, those two interfaces serve slightly different purposes.
 * The {@code Closeable} interface closes a stream or a connection, but some classes can be
 * reused with a different stream. For example an {@link javax.imageio.ImageReader} can be
 * instantiated once and reused many time for reading different image streams of the same
 * format. However once an object has been disposed, it can not be used anymore.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.10)
 * @version 0.3
 * @module
 *
 * @see java.awt.Graphics#dispose()
 * @see javax.imageio.ImageReader#dispose()
 * @see javax.imageio.ImageWriter#dispose()
 */
public interface Disposable {
    /**
     * Allows any resources held by this object to be released. The result of calling any other
     * method (other than {@code finalize()}) subsequent to a call to this method is undefined.
     */
    void dispose();
}
