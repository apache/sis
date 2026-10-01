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
package org.apache.sis.referencing.operation.gridded;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.DefaultParameterValue;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link GridFile}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class GridFileTest extends TestCase {
    /**
     * Operation parameter descriptor for a dummy file
     */
    private final ParameterDescriptor<URI> param;

    /**
     * Dummy operation parameter.
     */
    private final Parameters group;

    /**
     * Creates a new test case.
     */
    public GridFileTest() {
        final var builder = new ParameterBuilder();
        param = builder.addName("GridFile").create(URI.class, null);
        group = Parameters.castOrWrap(builder.addName("Test parameteters").createGroup(param).createValue());
    }

    /**
     * Creates the grid file with the current parameter values.
     */
    private GridFile newGridFile() throws FactoryException {
        return new GridFile(null, group, param);
    }

    /**
     * Tests construction with a file in the local directory.
     *
     * @throws URISyntaxException if an error occurred during URI construction.
     * @throws FactoryException if the construction failed.
     */
    @Test
    public void testLocalDirectory() throws URISyntaxException, FactoryException {
        final URI file = new URI("file:///tmp/test/dummy.txt");
        group.getOrCreate(param).setValue(file);
        assertMessageContains(
                assertThrows(InvalidGeodeticParameterException.class, () -> newGridFile()),
                file.getPath());
        /*
         * Test again, but replacing the full path by a path local to a dummy directory.
         * The access should no longer be denied.
         */
        makeParameterRelativeToSourceFile(group);
        final GridFile grid = newGridFile();
        assertEquals(new URI("dummy.txt"), grid.parameter);
        assertEquals(Path.of(file), grid.path().orElseThrow());
    }

    /**
     * Replaces absolute <abbr>URI</abbr> by paths relative to a dummy document.
     * This change is needed for avoiding "access denied" during test execution,
     * because {@link GridFile} accepts to open only grid file from the same host
     * as the <abbr>JSON</abbr>, <abbr>GML</abbr> or <abbr>WKT</abbr> document.
     *
     * @param  group  the group of parameters to edit.
     * @throws URISyntaxException if an error occurred during URI construction.
     */
    public static void makeParameterRelativeToSourceFile(final ParameterValueGroup group) throws URISyntaxException {
        for (GeneralParameterValue param : group.values()) {
            final String name = param.getDescriptor().getName().getCode();
            final var dp = assertInstanceOf(DefaultParameterValue.class, param, name);
            if (dp.getDescriptor().getValueClass() == URI.class) {
                final URI file = dp.valueFile();
                if (file.isAbsolute()) {
                    assertTrue(dp.getSourceFile().isEmpty(), name);
                    dp.setSourceFile(file.resolve("ImaginaryDocument.xml"));
                    URI parent = Path.of(file).getParent().toUri();
                    dp.setValue(parent.relativize(file));
                }
            }
        }
    }
}
