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
package org.apache.sis.test;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Comparator;
import net.jcip.annotations.NotThreadSafe;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import static org.apache.sis.util.Arrays.resize;


/**
 * The SIS test runner for individual classes.
 * This class extends the JUnit standard test runner with additional features:
 * <p>
 * <ul>
 *   <li>Support of the {@link DependsOnMethod} annotation.</li>
 * </ul>
 * <p>
 * This runner is not designed for parallel execution of tests.
 *
 * @author  Stephen Connolly
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from <a href="http://github.com/junit-team/junit.contrib/tree/master/assumes">junit-team</a>)
 * @version 0.3
 * @module
 */
@NotThreadSafe
public final class TestRunner extends BlockJUnit4ClassRunner {
    /**
     * The test methods to be executed, sorted according their dependencies.
     * This array is created by {@link #getFilteredChildren()} when first needed.
     */
    private FrameworkMethod[] filteredChildren;

    /**
     * The dependency methods that failed. This set will be created only when first needed.
     *
     * @see #addDependencyFailure(String)
     */
    private Set<String> dependencyFailures;

    /**
     * The listener to use for keeping trace of methods that failed.
     */
    final RunListener listener = new RunListener() {
        @Override
        public void testFailure(final Failure failure) {
            addDependencyFailure(failure.getDescription().getMethodName());
        }

        @Override
        public void testAssumptionFailure(final Failure failure) {
            addDependencyFailure(failure.getDescription().getMethodName());
        }

        @Override
        public void testIgnored(final Description description) {
            addDependencyFailure(description.getMethodName());
        }
    };

    /**
     * Creates a {@code Corollaries} to run {@code klass}.
     *
     * @param  klass The class to run.
     * @throws InitializationError If the test class is malformed.
     */
    public TestRunner(final Class<?> klass) throws InitializationError {
        super(klass);
    }

    /**
     * Returns the test methods to be executed, with dependencies sorted before dependant tests.
     *
     * @return The test method to be executed in dependencies order.
     */
    @Override
    public List<FrameworkMethod> getChildren() {
        return Arrays.asList(getFilteredChildren());
    }

    /**
     * Returns the test methods to be executed, with dependencies sorted before dependant tests.
     *
     * @return The test method to be executed in dependencies order.
     */
    private FrameworkMethod[] getFilteredChildren() {
        if (filteredChildren == null) {
            final List<FrameworkMethod> children = super.getChildren();
            filteredChildren = children.toArray(new FrameworkMethod[children.size()]);
            sortDependantTestsLast(filteredChildren);
        }
        return filteredChildren;
    }

    /**
     * Sorts the tests methods using the given sorter. The resulting order may not be totally
     * conform to the sorter specification, since this method will ensure that dependencies
     * are still sorted before dependant tests.
     *
     * @param sorter The sorter to use for sorting tests.
     */
    @Override
    public void sort(final Sorter sorter) {
        final FrameworkMethod[] children = getFilteredChildren();
        for (final FrameworkMethod method : children) {
            sorter.apply(method);
        }
        Arrays.sort(children, new Comparator<FrameworkMethod>() {
            @Override
            public int compare(FrameworkMethod o1, FrameworkMethod o2) {
                return sorter.compare(describeChild(o1), describeChild(o2));
            }
        });
        sortDependantTestsLast(children);
        filteredChildren = children;
    }

    /**
     * Sorts the given array of methods in dependencies order.
     *
     * @param methods The methods to sort.
     */
    private static void sortDependantTestsLast(final FrameworkMethod[] methods) {
        Set<String> dependencies = null;
        for (int i=methods.length-1; --i>=0;) {
            final FrameworkMethod method = methods[i];
            final DependsOnMethod depend = method.getAnnotation(DependsOnMethod.class);
            if (depend != null) {
                if (dependencies == null) {
                    dependencies = new HashSet<String>();
                }
                dependencies.addAll(Arrays.asList(depend.value()));
                for (int j=methods.length; --j>i;) {
                    if (dependencies.contains(methods[j].getName())) {
                        // Found a method j which is a dependency of i. Move i after j.
                        // The order of other methods relative to j is left unchanged.
                        System.arraycopy(methods, i+1, methods, i, j-i);
                        methods[j] = method;
                        break;
                    }
                }
                dependencies.clear();
            }
        }
    }

    /**
     * Removes tests that don't pass the parameter {@code filter}.
     *
     * @param  filter The filter to apply.
     * @throws NoTestsRemainException If all tests are filtered out.
     */
    @Override
    public void filter(final Filter filter) throws NoTestsRemainException {
        int count = 0;
        FrameworkMethod[] children = getFilteredChildren();
        for (int i=0; i<children.length; i++) {
            final FrameworkMethod method = children[i];
            if (filter.shouldRun(describeChild(method))) {
                try {
                    filter.apply(method);
                } catch (NoTestsRemainException e) {
                    continue;
                }
                children[count++] = method;
            }
        }
        if (count == 0) {
            throw new NoTestsRemainException();
        }
        filteredChildren = resize(children, count);
    }

    /**
     * Returns the {@link Statement} which will execute all the tests in the class given
     * to the constructor.
     *
     * @param  notifier The object to notify about test results.
     * @return The statement to execute for running the tests.
     */
    @Override
    protected Statement childrenInvoker(final RunNotifier notifier) {
        final Statement stmt = super.childrenInvoker(notifier);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                notifier.addListener(listener);
                try {
                    stmt.evaluate();
                } finally {
                    notifier.removeListener(listener);
                }
            }
        };
    }

    /**
     * Before to delegate to the {@linkplain BlockJUnit4ClassRunner#runChild default implementation},
     * check if a dependency of the given method failed. In such case, the test will be ignored.
     *
     * @param method   The test method to execute.
     * @param notifier The object to notify about test results.
     */
    @Override
    protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
        if (dependencyFailures != null) {
            final DependsOnMethod assumptions = method.getAnnotation(DependsOnMethod.class);
            if (assumptions != null) {
                for (final String assumption : assumptions.value()) {
                    if (dependencyFailures.contains(assumption)) {
                        dependencyFailures.add(method.getName());
                        notifier.fireTestIgnored(describeChild(method));
                        return;
                    }
                }
            }
        }
        super.runChild(method, notifier);
    }

    /**
     * Declares that the given method failed. Other methods depending on this method
     * will be ignored.
     *
     * @param methodName The name of the method that failed.
     */
    final void addDependencyFailure(final String methodName) {
        if (dependencyFailures == null) {
            dependencyFailures = new HashSet<String>();
        }
        dependencyFailures.add(methodName);
    }
}
