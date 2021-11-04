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
package org.apache.sis.internal.gui;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.util.Static;
import org.apache.sis.util.Workaround;


/**
 * Miscellaneous utility methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.1
 * @module
 */
public final class GUIUtilities extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private GUIUtilities() {
    }

    /**
     * Returns the window of the bean associated to the given property.
     *
     * @param  property  the property for which to get the window of the control, or {@code null}.
     * @return the window, or {@code null} if unknown.
     */
    public static Window getWindow(final ObservableValue<?> property) {
        if (property instanceof ObjectProperty<?>) {
            final Object bean = ((ObjectProperty<?>) property).getBean();
            if (bean instanceof Node) {
                final Scene scene = ((Node) bean).getScene();
                if (scene != null) {
                    return scene.getWindow();
                }
            } else if (bean instanceof MenuItem) {
                ContextMenu parent = ((MenuItem) bean).getParentPopup();
                if (parent != null) {
                    for (;;) {
                        final Window owner = parent.getOwnerWindow();
                        if (!(owner instanceof ContextMenu)) return owner;
                        parent = (ContextMenu) owner;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Sets on the given pane a clip defined to the pane bounds. This method is invoked for pane having content
     * that may be drawn outside the pane bounds (typically images). We use this method as a workaround for the
     * fact that JavaFX pane does not apply clip by itself.
     *
     * @param  pane  the pane on which to set the clip.
     */
    public static void setClipToBounds(final Pane pane) {
        final Rectangle clip = new Rectangle();
        clip.widthProperty() .bind(pane.widthProperty());
        clip.heightProperty().bind(pane.heightProperty());
        pane.setClip(clip);
    }

    /**
     * Forces a {@link TreeItem} to update the {@code TreeView} when its value has been externally modified.
     * This is a workaround for situations where the item's value is unchanged, but some state of the value
     * has been modified.
     *
     * @param  <T>   type of values in the tree item.
     * @param  item  the item for which to force an update.
     */
    @Workaround(library = "JavaFX", version = "17")
    public static <T> void forceCellUpdate(final TreeItem<T> item) {
        final T value = item.getValue();
        item.setValue(null);
        item.setValue(value);
    }

    /**
     * Copies all elements from the given source list to the specified target list,
     * but with the application of insertion and removal operations only.
     * This method is useful when the two lists should be similar.
     * The intend is to causes as few change events as possible.
     *
     * @param  <E>     type of elements to copy.
     * @param  source  the list of elements to copy in the target.
     * @param  target  the list to modify with as few operations as possible.
     * @return {@code true} if the target changed as a result of this method call.
     */
    @SuppressWarnings("empty-statement")
    public static <E> boolean copyAsDiff(final List<? extends E> source, final ObservableList<E> target) {
        if (source.isEmpty()) {
            final boolean empty = target.isEmpty();
            target.clear();
            return !empty;
        }
        if (target.isEmpty()) {
            return target.setAll(source);           // Return value is correct only if source is not empty.
        }
        final List<? extends E> lcs = longestCommonSubsequence(source, target);
        /*
         * Remove elements before to add new ones, because some listeners
         * seem to be confused when a list contains duplicated elements
         * (the removed elements may be inserted elsewhere).
         */
        boolean modified = false;
        int upper = target.size();
        for (int i = lcs.size(); --i >= 0;) {
            final E keep = lcs.get(i);
            int lower = upper;
            while (target.get(--lower) != keep);    // A negative index here would be a bug in LCS computation.
            if (lower + 1 < upper) {
                target.remove(lower + 1, upper);
                modified = true;
            }
            upper = lower;
        }
        if (upper != 0) {
            target.remove(0, upper);
            modified = true;
        }
        assert lcs.equals(target);                  // Because we removed all elements that were not present in LCS.
        /*
         * Now insert the new elements. We move forward for reducing the
         * number of elements that `ObservableList` will have to shift.
         * (We moved backward in the removal phase for the same reason).
         */
        int lower = 0;
        for (int i=0; i<target.size(); i++) {
            final E skip = target.get(i);
            upper = lower;
            while (source.get(upper) != skip) upper++;  // An index out of bounds would be a bug in LCS computation.
            if (lower < upper) {
                target.addAll(i, source.subList(lower, upper));
                i += upper - lower;
                modified = true;
            }
            lower = upper + 1;
        }
        upper = source.size();
        if (lower < upper) {
            target.addAll(source.subList(lower, upper));
            modified = true;
        }
        assert source.equals(target);
        return modified;
    }

    /**
     * Returns the longest subsequence common to both specified sequences.
     * This is known as <cite>longest common subsequence</cite> (LCS) problem.
     * The LCS elements are not required to occupy consecutive positions within the original sequences.
     *
     * <div class="note"><b>Example:</b>
     * for the two following lists <var>x</var> and <var>y</var>,
     * the longest common subsequence if given by <var>lcs</var> below:
     *
     * {@preformat text
     *   x   :  1 2   4 6 7   9
     *   y   :  1 2 3     7 8
     *   lcs :  1 2       7
     * }
     * </div>
     *
     * This algorithm is useful for computing the differences between two sequences.
     *
     * @param  <E>  the type of elements in the sequences.
     * @param  x    the first sequence for which to compute LCS.
     * @param  y    the second sequence for which to compute LCS.
     * @return longest common subsequence (LCS) between the two given sequences.
     *         May be one of the given lists.
     *
     * <a href="https://en.wikipedia.org/wiki/Longest_common_subsequence_problem">Longest common subsequence problem</a>
     */
    static <E> List<? extends E> longestCommonSubsequence(List<? extends E> x, List<? extends E> y) {
        final List<? extends E> ox = x;     // The whole list, before sublisting that may be applied.
        final List<? extends E> oy = y;     // Idem.
        /*
         * This method can be optimized by excluding the common prefix and common suffix before
         * to build the matrix. It can reduce a lot the matrix size and the number of iterations.
         */
        int nx = x.size();
        int ny = y.size();
        final List<? extends E> prefix;
        for (int i=0; ; i++) {
            if (i >= nx) return x;
            if (i >= ny) return y;
            if (x.get(i) != y.get(i)) {
                if (i == 0) {
                    prefix = Collections.emptyList();
                } else {
                    prefix = x.subList(0, i);
                    assert   y.subList(0, i).equals(prefix);
                    x = x.subList(i, nx);
                    y = y.subList(i, ny);
                    nx -= i;
                    ny -= i;
                }
                break;
            }
        }
        final List<? extends E> suffix;
        for (int i=0; ; i++) {
            final int sx = nx - i;
            final int sy = ny - i;
            if (sx == 0) return ox;                 // Concatenation of prefix + suffix = original list.
            if (sy == 0) return oy;
            if (x.get(sx - 1) != y.get(sy - 1)) {
                if (i == 0) {
                    suffix = Collections.emptyList();
                } else {
                    suffix = x.subList(sx, nx);
                    assert   y.subList(sy, ny).equals(suffix);
                    x = x.subList(0, sx);
                    y = y.subList(0, sy);
                    nx -= i;
                    ny -= i;
                }
                break;
            }
        }
        /*
         * We need a matrix of size (nx x ny) for storing LCS lengths for all (x[i], y[j]) pairs of elements.
         * The matrix is augmented by one row and one column where all values in the first row and first column
         * are zero. We could omit that row and that column for saving space, but it would complexify this code.
         * For now we don't do that, but we may revisit in the future if this code is used for longer sequences.
         */
        final int[][] lengths = new int[nx + 1][ny + 1];
        for (int i=1; i<=nx; i++) {
            final int im = i - 1;
            final E xim = x.get(im);
            for (int j=1; j<=ny; j++) {
                final int jm = j - 1;
                lengths[i][j] = (y.get(jm) == xim)
                              ? Math.incrementExact(lengths[im][jm])
                              : Math.max(lengths[i][jm], lengths[im][j]);
            }
        }
        /*
         * The last cell contains the length of longest subsequence common to both lists.
         * Following loop is the "traceback" procedure: starting from last cell, follows
         * the direction where the length decrease.
         */
        final List<E> lcs = new ArrayList<>(lengths[nx][ny] + prefix.size() + suffix.size());
        while (nx > 0 && ny > 0) {
            final int lg = lengths[nx][ny];
            if (lengths[nx-1][ny] >= lg) {
                nx--;
            } else if (lengths[nx][--ny] < lg) {
                final E ex = x.get(--nx);
                assert ex == y.get(ny);
                lcs.add(ex);
            }
        }
        Collections.reverse(lcs);
        lcs.addAll(0, prefix);
        lcs.addAll(   suffix);
        return lcs;
    }

    /**
     * Modify the quantity unit for showing a smaller value.
     *
     * @param  quantity  the quantity to modify, or {@code null}.
     * @param  m         the quantity value in metres.
     * @return the simplified quantity.
     */
    public static Quantity<Length> shorter(final Quantity<Length> quantity, double m) {
        final Unit<Length> unit;
        if (m < 1) {
            unit = Units.CENTIMETRE;
        } else if (m < 1000) {
            unit = Units.METRE;
        } else {
            unit = Units.KILOMETRE;
        }
        if (quantity != null && unit.equals(quantity.getUnit())) {
            return quantity;
        }
        m = Units.METRE.getConverterTo(unit).convert(Math.max(m, Formulas.LINEAR_TOLERANCE));
        return Quantities.create(m, unit);
    }

    /**
     * Returns a color from a ARGB value packed in an integer.
     *
     * @param  code  the ARGB value.
     * @return color for the given ARGB value.
     */
    public static Color fromARGB(final int code) {
        return Color.rgb(0xFF & (code >>> Byte.SIZE*2),     // Red
                         0xFF & (code >>> Byte.SIZE),       // Green
                         0xFF & (code));                    // Blue
    }

    /**
     * Returns a ARGB value packed in an integer.
     *
     * @param  color  color for which to get the ARGB value.
     * @return ARGB value for the given color.
     */
    public static int toARGB(final Color color) {
        return (toByte(color.getOpacity()) << 3*Byte.SIZE)
             | (toByte(color.getRed())     << 2*Byte.SIZE)
             | (toByte(color.getGreen())   <<   Byte.SIZE)
             |  toByte(color.getBlue());
    }

    /**
     * Converts a floating point value in the 0 … 1 range to an integer value in the 0 … 255 range.
     */
    private static int toByte(final double value) {
        return (int) Math.round(value * 255);
    }
}
