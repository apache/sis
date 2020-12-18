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
package org.apache.sis.swing;

import java.awt.Component;
import java.awt.event.WindowListener;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.WindowConstants;


/**
 * Interfaces for windows created by {@link WindowCreator}. This interface is typically implemented
 * by {@link JDialog}, {@link JFrame} or {@link JInternalFrame} subclasses, but users can provide other
 * implementation. For example an application developed on top of the <cite>NetBeans platform</cite>
 * may need to provide their own implementation for better integration with their platform.
 *
 * <p>Instances of {@code Window} are created by
 * {@link WindowCreator.Handler#createWindow(Component, Component, String)}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public interface Window extends WindowConstants {
    /**
     * Return {@code true} if this window is visible. The default value on
     * {@linkplain WindowCreator.Handler#createWindow window creation} is {@code false}.
     *
     * @return {@code true} if this window is visible.
     *
     * @see Component#isVisible()
     */
    boolean isVisible();

    /**
     * Sets whatever this window should be visible. New windows created by {@link WindowCreator.Handler}
     * needs an explicit call to this method in order to be visible.
     *
     * @param  visible  {@code true} for showing this window, or {@code false} for hiding it.
     *
     * @see Component#setVisible(boolean)
     */
    void setVisible(boolean visible);

    /**
     * Sets the window size.
     *
     * @param  width   the width in pixels.
     * @param  height  the height in pixels.
     *
     * @see Component#setSize(int, int)
     */
    void setSize(int width, int height);

    /**
     * Returns the current title, or {@code null} if none.
     *
     * @return the current title, or {@code null} if none.
     *
     * @see java.awt.Frame#getTitle()
     * @see JInternalFrame#getTitle()
     */
    String getTitle();

    /**
     * Sets the window title.
     *
     * @param  title  the new title, or {@code null} if none.
     *
     * @see java.awt.Frame#setTitle(String)
     * @see JInternalFrame#setTitle(String)
     */
    void setTitle(String title);

    /**
     * Adds the specified window listener to receive window events from this window.
     * If {@code listener} is null, no exception is thrown and no action is performed.
     *
     * <p>The listener given to this method shall be tolerant to null {@code WindowEvent}
     * argument value, since it is not guaranteed that the events fired by the actual
     * window implementation can be converted to {@code WindowEvent} in every cases.</p>
     *
     * @param  listener  the window listener to add.
     *
     * @see java.awt.Window#addWindowListener(WindowListener)
     */
    void addWindowListener(WindowListener listener);

    /**
     * Removes the specified window listener.
     *
     * @param  listener  the window listener to remove.
     *
     * @see java.awt.Window#removeWindowListener(WindowListener)
     */
    void removeWindowListener(WindowListener listener);

    /**
     * Returns the default operation that occurs when the user initiates a "close" on this window.
     * The default value on {@linkplain WindowCreator.Handler#createWindow window creation} is
     * {@link WindowConstants#DISPOSE_ON_CLOSE}.
     *
     * @return the operation that will occur when the user closes the window,
     *         as one of the {@link WindowConstants}.
     *
     * @see JFrame#getDefaultCloseOperation()
     * @see JInternalFrame#getDefaultCloseOperation()
     */
    int getDefaultCloseOperation();

    /**
     * Sets the default operation that occurs when the user initiates a "close" on this window.
     * This method may be invoked after {@linkplain WindowCreator.Handler#createWindow window creation}
     * in order to set the default close operation to {@link WindowConstants#HIDE_ON_CLOSE}.
     * In such case, the caller is responsible for {@linkplain #dispose() disposing} this window
     * when it is no longer needed.
     *
     * @param  operation  the operation that will occur when the user closes the window,
     *         as one of the {@link WindowConstants}.
     *
     * @see JFrame#setDefaultCloseOperation(int)
     * @see JInternalFrame#setDefaultCloseOperation(int)
     */
    void setDefaultCloseOperation(int operation);

    /**
     * Releases the resources used by this window.
     *
     * @see java.awt.Window#dispose()
     * @see JInternalFrame#dispose()
     */
    void dispose();
}
