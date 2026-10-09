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
package org.apache.sis.storage.geopose.binding;

import java.util.Arrays;
import java.util.List;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.storage.json.AbstractBindingTest;


/**
 * Tests the JSON bindings of the OGC GeoPose 1.0 standardization targets.
 *
 * <p>Each test reads a document, compares it with a hand built instance, writes it back and
 * compares the result with the original document.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BindingTest extends AbstractBindingTest {

    private static final String AUTHORITY = "/geopose/1.0";
    private static final String LTP_ENU = "LTP-ENU";
    private static final long INSTANT = 1630560671227L;

    public BindingTest() {
    }

    /**
     * Returns the frame specification used by most fixtures as the outer frame.
     */
    private static FrameSpecification outerFrame() {
        return new FrameSpecification(AUTHORITY, LTP_ENU, "longitude=1.4442&latitude=43.6047&height=150.000");
    }

    private static FrameSpecification innerFrame1() {
        return new FrameSpecification(AUTHORITY, LTP_ENU, "longitude=1.4450&latitude=43.6050&height=151.000");
    }

    private static FrameSpecification innerFrame2() {
        return new FrameSpecification(AUTHORITY, LTP_ENU, "longitude=1.4460&latitude=43.6060&height=152.000");
    }

    private static TransitionModel linear() {
        return new TransitionModel(AUTHORITY, "linear", "");
    }

    /**
     * Tests the Basic-YPR standardization target.
     */
    @Test
    public void testBasicYPR() throws Exception {
        final BasicYPR expected = new BasicYPR(
                new Position(43.6047, 1.4442, 150.0),
                new YawPitchRoll(90.0, -12.5, 0.0));
        compare("basic_ypr.json", expected);
    }

    /**
     * Tests the Basic-Quaternion standardization target.
     */
    @Test
    public void testBasicQuaternion() throws Exception {
        final BasicQuaternion expected = new BasicQuaternion(
                new Position(43.6047, 1.4442, 150.0),
                new Quaternion(0.0, 0.0, 0.7071067811865476, 0.7071067811865476));
        compare("basic_quaternion.json", expected);
    }

    /**
     * Tests the Basic-Euler standardization target.
     */
    @Test
    public void testBasicEuler() throws Exception {
        final List<Double> rotations = Arrays.asList(90.0, -12.5, 0.0);
        final BasicEuler expected = new BasicEuler(1.4442, 43.6047, 150.0, rotations);
        compare("basic_euler.json", expected);
    }

    /**
     * Tests the Advanced standardization target, with and without the optional valid time.
     */
    @Test
    public void testAdvanced() throws Exception {
        final Advanced expected = new Advanced(outerFrame(),
                new Quaternion(0.0, 0.0, 0.7071067811865476, 0.7071067811865476), INSTANT);
        compare("advanced.json", expected);
    }

    /**
     * Tests that an absent valid time is neither read nor written.
     */
    @Test
    public void testAdvancedWithoutValidTime() throws Exception {
        final Advanced expected = new Advanced(outerFrame(), new Quaternion(0.0, 0.0, 0.0, 1.0), null);
        compare("advanced_no_validtime.json", expected);
        assertNull(expected.getValidTime());
    }

    /**
     * Tests the Chain standardization target.
     */
    @Test
    public void testChain() throws Exception {
        final Chain expected = new Chain(INSTANT, outerFrame(),
                Arrays.asList(innerFrame1(), innerFrame2()));
        compare("chain.json", expected);
    }

    /**
     * Tests the Graph standardization target.
     */
    @Test
    public void testGraph() throws Exception {
        final Graph expected = new Graph(INSTANT,
                Arrays.asList(outerFrame(), innerFrame1(), innerFrame2()),
                Arrays.asList(new FrameTransformPair(Arrays.asList(0, 1)),
                              new FrameTransformPair(Arrays.asList(1, 2))));
        compare("graph.json", expected);
    }

    /**
     * Tests the Regular Series standardization target, whose header omits the integrity check.
     */
    @Test
    public void testRegularSeries() throws Exception {
        final SeriesHeader header = new SeriesHeader(2, null, INSTANT, INSTANT + 10000, linear());
        final RegularSeries expected = new RegularSeries(header, 10000, outerFrame(),
                Arrays.asList(innerFrame1(), innerFrame2()), new SeriesTrailer(2, null));
        compare("regular_series.json", expected);
    }

    /**
     * Tests the Irregular Series standardization target, whose second element has no valid time.
     */
    @Test
    public void testIrregularSeries() throws Exception {
        final SeriesHeader header = new SeriesHeader(2, "sha-256:none", INSTANT, INSTANT + 10000, linear());
        final IrregularSeries expected = new IrregularSeries(header, outerFrame(),
                Arrays.asList(new FrameAndTime(innerFrame1(), INSTANT),
                              new FrameAndTime(innerFrame2(), null)),
                new SeriesTrailer(2, "sha-256:none"));
        compare("irregular_series.json", expected);
    }

    /**
     * Tests the Stream standardization target.
     */
    @Test
    public void testStream() throws Exception {
        final Stream expected = new Stream(new StreamHeader(linear(), outerFrame()),
                Arrays.asList(new StreamElement(new FrameAndTime(innerFrame1(), INSTANT))));
        compare("stream.json", expected);
    }

    /**
     * Tests the Stream Element standardization target.
     */
    @Test
    public void testStreamElement() throws Exception {
        final StreamElement expected = new StreamElement(new FrameAndTime(innerFrame1(), INSTANT));
        compare("stream_element.json", expected);
    }
}
