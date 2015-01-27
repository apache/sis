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
package org.apache.sis.util.logging;

import java.util.logging.Level;


/**
 * A dummy implementation of {@link LoggerAdapter} class for testing purpose.
 * This class is used by {@link LoggerAdapterTest}.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3
 * @version 0.3
 * @module
 */
final strictfp class DummyLogger extends LoggerAdapter {
    /**
     * The level of the last logging event.
     */
    Level level;

    /**
     * The last logged message.
     */
    String last;

    /**
     * Creates a dummy logger.
     */
    DummyLogger() {
        super("org.apache.sis.util.logging");
        clear();
    }

    /**
     * Clears the logger state, for testing purpose only.
     */
    public void clear() {
        level = Level.OFF;
        last  = null;
    }

    @Override
    public void setLevel(Level level) {
        this.level = level;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public boolean isLoggable(Level level) {
        return level.intValue() > this.level.intValue();
    }

    @Override
    public void severe(String message) {
        level = Level.SEVERE;
        last  = message;
    }

    @Override
    public void warning(String message) {
        level = Level.WARNING;
        last  = message;
    }

    @Override
    public void info(String message) {
        level = Level.INFO;
        last  = message;
    }

    @Override
    public void config(String message) {
        level = Level.CONFIG;
        last  = message;
    }

    @Override
    public void fine(String message) {
        level = Level.FINE;
        last  = message;
    }

    @Override
    public void finer(String message) {
        level = Level.FINER;
        last  = message;
    }

    @Override
    public void finest(String message) {
        level = Level.FINEST;
        last  = message;
    }
}
