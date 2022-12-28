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
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;


/**
 * Wraps a {@link WindowListener} into an {@link InternalFrameListener}.
 * This is used by {@link SwingUtilities} in order to have the same methods
 * working seemless on both {@link java.awt.Frame} and {@link javax.swing.JInternalFrame}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
final class InternalWindowListener implements InternalFrameListener {
    /**
     * The underlying {@link WindowListener}.
     */
    private final WindowListener listener;

    /**
     * Constructs a new {@link InternalFrameListener}
     * wrapping the specified {@link WindowListener}.
     */
    private InternalWindowListener(final WindowListener listener) {
        this.listener = listener;
    }

    /**
     * Wraps the specified {@link WindowListener} into an {@link InternalFrameListener}.
     * If the specified object is already an {@link InternalFrameListener}, then it is
     * returned as-is.
     *
     * @param  listener  the window listener.
     * @return the internal frame listener.
     */
    public static InternalFrameListener wrap(final WindowListener listener) {
        if (listener == null) {
            return null;
        }
        if (listener instanceof InternalFrameListener) {
            return (InternalFrameListener) listener;
        }
        return new InternalWindowListener(listener);
    }

    /**
     * Wraps the given internal frame event into a window event.
     */
    private static WindowEvent wrap(final InternalFrameEvent event) {
        /*
         * Don't use javax.swing.SwingUtilities.getWindowAncestor
         * because we want the check to include event.getSource().
         */
        Component c = (Component) event.getSource();
        while (c != null) {
            if (c instanceof Window) {
                return new WindowEvent((Window) c, event.getID());
            }
            c = c.getParent();
        }
        return null;            // We cannot create a WindowEvent with a null source.
    }

    /**
     * Invoked when a internal frame has been opened.
     *
     * @param  event  the event.
     */
    @Override
    public void internalFrameOpened(InternalFrameEvent event) {
        listener.windowOpened(wrap(event));
    }

    /**
     * Invoked when an internal frame is in the process of being closed.
     * The close operation can be overridden at this point.
     *
     * @param  event  the event.
     */
    @Override
    public void internalFrameClosing(InternalFrameEvent event) {
        listener.windowClosing(wrap(event));
    }

    /**
     * Invoked when an internal frame has been closed.
     *
     * @param  event  the event.
     */
    @Override
    public void internalFrameClosed(InternalFrameEvent event) {
        listener.windowClosed(wrap(event));
    }

    /**
     * Invoked when an internal frame is iconified.
     *
     * @param  event  the event.
     */
    @Override
    public void internalFrameIconified(InternalFrameEvent event) {
        listener.windowIconified(wrap(event));
    }

    /**
     * Invoked when an internal frame is de-iconified.
     *
     * @param  event  the event.
     */
    @Override
    public void internalFrameDeiconified(InternalFrameEvent event) {
        listener.windowDeiconified(wrap(event));
    }

    /**
     * Invoked when an internal frame is activated.
     *
     * @param  event  the event.
     */
    @Override
    public void internalFrameActivated(InternalFrameEvent event) {
        listener.windowActivated(wrap(event));
    }

    /**
     * Invoked when an internal frame is de-activated.
     *
     * @param  event  the event.
     */
    @Override
    public void internalFrameDeactivated(InternalFrameEvent event) {
        listener.windowDeactivated(wrap(event));
    }

    /**
     * Removes the given window listener from the given internal frame. This method will look
     * for instances of {@code InternalWindowListener} and unwrap the listener if needed.
     *
     * @param  frame     the frame from which to remove the listener.
     * @param  listener  the listener to remove.
     */
    public static void removeWindowListener(final JInternalFrame frame, final WindowListener listener) {
        for (final InternalFrameListener candidate : frame.getInternalFrameListeners()) {
            if (candidate instanceof InternalWindowListener &&
                    ((InternalWindowListener) candidate).listener.equals(listener))
            {
                frame.removeInternalFrameListener(candidate);
            }
        }
    }
}
