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
package org.apache.sis.gui.setup;


/**
 * An identifier of the page to show in {@link Wizard}.
 * Pages are shown in enumeration order.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
enum WizardPage {
    /**
     * Plain text saying what this wizard will do.
     */
    INTRODUCTION("Introduction",
            "<html><h1 style=\"font-size:1.2em;\">Welcome to Apache SIS™</h1>"
            + "<p>"
            + "This wizard will configure Apache Spatial Information System (SIS) "
            + "JavaFX application on your computer. "
            + "This configuration needs to be done only once. "
            + "Click <u>Next</u> to continue, or <u>Cancel</u> to exit setup."
            + "</p><br><p style=\"font-size:0.9em; color:#909090;\">"
            + "Apache SIS is licensed under the Apache License, version 2.0."
            + "</p></html>"),

    /**
     * Page proposing to download JavaFX, with a "Download" button.
     * Those instructions a completed by {@link #downloadSteps()}.
     */
    DOWNLOAD_JAVAFX("Download",
            "<html><p style=\"padding-top:10px;\">"
            + "This application requires <i>JavaFX</i> (or <i>OpenJFX</i>) version " + FXFinder.JAVAFX_VERSION + " or later. "
            + "OpenJFX is free software, licensed under the GPL with the class path exception. "
            + "Click on <u>Open JavaFX home page</u> for opening the JavaFX home page. "
            + "If JavaFX has already been downloaded on this computer, "
            + "skip download and click on <u>Next</u> for specifying its installation directory."
            + "</p></html>"),

    /**
     * Page asking to specify the installation directory.
     */
    JAVAFX_LOCATION("JavaFX location",
            "<html><p style=\"padding-top:10px;\">"
            + "Specify the downloaded ZIP file, or the directory where JavaFX or OpenJFX has been installed. "
            + "You can drag and drop the file or directory below or click on <u>Browse</u>."
            + "</p></html>"),

    /**
     * Page notifying user that a decompression is in progress.
     * This page is skipped if the user specified an existing directory instead of a ZIP file.
     */
    DECOMPRESS("Decompress",
            "<html><p style=\"padding-top:10px;\">"
            + "Decompressing ZIP file."
            + "</p></html>"),

    /**
     * Final page saying that the configuration is completed.
     */
    COMPLETED("Summary",
            "<html><p style=\"padding-top:10px;\">"
            + "Apache SIS setup is completed. "
            + "Environment variables relevant to SIS are listed below."
            + "</p></html>");

    /**
     * Complement to {@link #DOWNLOAD_JAVAFX}.
     */
    static String downloadSteps() {
        return "<html><ul>"
            + "<li>Click on <b>Download</b>.</li>"
            + "<li>Scroll down to <b>Latest releases</b>.</li>"
            + "<li>Download <b>" + FXFinder.getJavafxBundleName() + "</b>.</li>"
            + "<li><em>(Optional)</em> decompress the ZIP file in any directory.</li>"
            + "<li>Click <u>Next</u> to continue.</li>"
            + "</ul></html>";
    }

    /**
     * Title for this page.
     */
    final String title;

    /**
     * The text to show on the page.
     */
    final String text;

    /**
     * Creates a new enumeration for a page showing the specified text.
     */
    private WizardPage(final String title, final String text) {
        this.title = title;
        this.text  = text;
    }
}
