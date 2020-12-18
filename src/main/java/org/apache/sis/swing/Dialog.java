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
import java.text.ParseException;


/**
 * Interface for widgets that can be used as a dialog box.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @version 1.1
 * @since   1.1
 */
public interface Dialog {
    /**
     * Shows a dialog box requesting input from the user. The dialog box will be parented to {@code owner}.
     * If {@code owner} is contained into a {@link javax.swing.JDesktopPane}, the dialog box will appears
     * as an internal frame.
     *
     * <h4>Multi-threading</h4>
     * Apache SIS implementations allow this method to be invoked from any thread. If the caller
     * thread is not the <cite>Swing</cite> thread, then the execution of this method will be
     * registered in the AWT Event Queue and the caller thread will block until completion.
     *
     * @param  owner  the parent component for the dialog box, or {@code null} if there is no parent.
     * @param  title  the dialog box title.
     * @return {@code true} if user pressed the "<cite>Ok</cite>" button, or {@code false} otherwise
     *         (e.g. pressing "<cite>Cancel</cite>" or closing the dialog box from the title bar).
     */
    boolean showDialog(Component owner, String title);

    /**
     * Forces the current value to be taken from the editable fields and set them as the current values.
     * If this operation fails for at least one field, this method will set the focus on the offending
     * field before to throw the exception.
     *
     * <p>This method is typically invoked after {@link #showDialog(Component, String)}
     * returned {@code true} and before to read the values from the dialog widget.</p>
     *
     * @throws ParseException if at least one values couldn't be committed.
     *
     * @see javax.swing.JFormattedTextField#commitEdit()
     * @see javax.swing.JSpinner#commitEdit()
     */
    void commitEdit() throws ParseException;
}
