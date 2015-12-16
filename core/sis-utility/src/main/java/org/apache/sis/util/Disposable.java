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
 * A resource that can be disposed when waiting for the garbage collector would be overly conservative.
 * Invoking the {@link #dispose()} method allows any resources held by this object to be released.
 * The result of calling any other method subsequent to a call to this method is undefined.
 *
 * <p>Data integrity shall not depend on {@code dispose()} method invocation.
 * If some data may need to be {@linkplain java.io.OutputStream#flush() flushed to a stream}
 * or {@linkplain java.sql.Connection#commit() committed to a database},
 * then a {@code close()} method should be used instead.</p>
 *
 * <div class="section">Relationship with {@code Closeable}</div>
 * Some classes may implement both the {@code Disposeable} and {@link java.io.Closeable} interfaces.
 * While very similar, those two interfaces serve slightly different purposes. The {@code Closeable}
 * interface closes a stream or a connection, but some classes allow the object to be reused with a
 * different stream. However once an object has been disposed, it can not be used anymore.
 *
 * <div class="note"><b>Example:</b>
 * {@link javax.imageio.ImageReader} and {@link javax.imageio.ImageWriter} allow to reuse the same instance
 * many times for reading or writing different images in the same format. New streams can be created, given
 * to the {@code ImageReader} or {@code ImageWriter} and closed many times as long as {@code dispose()} has
 * not been invoked.</div>
 *
 * Another difference is that {@link #dispose()} does not throw any checked exception.
 * That method may be invoked in a background thread performing cleanup tasks,
 * which would not know what to do in case of failure.
 * Error during {@code dispose()} execution should not result in any lost of data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
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
