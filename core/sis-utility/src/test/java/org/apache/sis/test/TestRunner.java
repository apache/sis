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
import java.io.PrintWriter;

import org.junit.Test;
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
import org.junit.runners.model.TestClass;

import org.apache.sis.util.ArraysExt;

import static org.apache.sis.util.collection.Containers.isNullOrEmpty;
import static org.apache.sis.util.collection.Containers.hashMapCapacity;


/**
 * The SIS test runner for individual classes.
 * This class extends the JUnit standard test runner with additional features:
 *
 * <ul>
 *   <li>Support of the {@link DependsOn} and {@link DependsOnMethod} annotations.</li>
 * </ul>
 *
 * This runner is <strong>not</strong> designed for parallel execution of tests.
 *
 * @author  Stephen Connolly
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from <a href="http://github.com/junit-team/junit.contrib/tree/master/assumes">junit-team</a>)
 * @version 0.5
 * @module
 */
public final class TestRunner extends BlockJUnit4ClassRunner {
    /**
     * {@code true} if ignoring a test should cause its dependencies to be skipped as well.
     */
    static final boolean TRANSITIVE_IGNORE = false;

    /**
     * The test methods to be executed, sorted according their dependencies.
     * This array is created by {@link #getFilteredChildren()} when first needed.
     */
    private FrameworkMethod[] filteredChildren;

    /**
     * The dependency methods that failed. This set will be created only when first needed.
     * Values are method names.
     *
     * <div class="note"><b>Note:</b>
     * There is no need to prefix the method names by classnames because a new instance of {@code TestRunner}
     * will be created for each test class, even if the the test classes are aggregated in a {@link TestSuite}.</div>
     *
     * @see #addDependencyFailure(String)
     */
    private Set<String> methodDependencyFailures;

    /**
     * The dependency classes that failed. This set will be created only when first needed.
     *
     * @see #addDependencyFailure(String)
     */
    private static Set<Class<?>> classDependencyFailures;

    /**
     * {@code true} if every tests shall be skipped. This happen if at least one test failure
     * occurred in at least one class listed in the {@link DependsOn} annotation.
     *
     * @see #checkClassDependencies()
     */
    private boolean skipAll;

    /**
     * The listener to use for keeping trace of methods that failed.
     */
    final RunListener listener = new RunListener() {
        /**
         * Clears the buffer if it was not already done by {@link #testFinished(Description)}.
         */
        @Override
        public void testStarted(final Description description) {
            if (!TestCase.VERBOSE) {
                TestCase.clearBuffer();
            }
            LogRecordCollector.INSTANCE.setCurrentTest(description);
        }

        /**
         * Prints output only in verbose mode. Otherwise silently discard the output.
         * This method is invoked on failure as well as on success. In case of test
         * failure, this method is invoked after {@link #testFailure(Failure)}.
         */
        @Override
        public void testFinished(final Description description) {
            if (TestCase.VERBOSE) {
                TestCase.flushOutput();
            }
            TestCase.randomSeed = 0;
            LogRecordCollector.INSTANCE.setCurrentTest(null);
        }

        /**
         * Remember that a test failed, and prints output if it was not already done
         */
        @Override
        public void testFailure(final Failure failure) {
            final Description description = failure.getDescription();
            final String methodName = description.getMethodName();
            addDependencyFailure(methodName);
            final long seed = TestCase.randomSeed;
            if (seed != 0) {
                final String className = description.getClassName();
                final PrintWriter out = TestCase.out;
                out.print("Random number generator for ");
                out.print(className.substring(className.lastIndexOf('.') + 1));
                out.print('.');
                out.print(methodName);
                out.print("() was created with seed ");
                out.print(seed);
                out.println('.');
                // Seed we be cleared by testFinished(…).
            }
            if (!TestCase.VERBOSE) {
                TestCase.flushOutput();
            }
            // In verbose mode, the flush will be done by testFinished(…).
        }

        /**
         * Silently record skipped test as if it failed, without printing the output.
         */
        @Override
        public void testAssumptionFailure(final Failure failure) {
            if (TRANSITIVE_IGNORE) {
                addDependencyFailure(failure.getDescription().getMethodName());
            }
        }

        /**
         * Silently record ignored test as if it failed, without printing the output.
         */
        @Override
        public void testIgnored(final Description description) {
            if (TRANSITIVE_IGNORE) {
                addDependencyFailure(description.getMethodName());
            }
        }
    };

    /**
     * Creates a new test runner for the given class.
     *
     * @param  testClass The class to run.
     * @throws InitializationError If the test class is malformed.
     */
    public TestRunner(final Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    /**
     * Validates all tests methods in the test class. This method first performs the default
     * verification documented in {@link BlockJUnit4ClassRunner#validateTestMethods(List)},
     * then ensures that all {@link DependsOnMethod} annotations refer to an existing method.
     *
     * @param errors The list where to report any problem found.
     */
    @Override
    protected void validateTestMethods(final List<Throwable> errors) {
        super.validateTestMethods(errors);
        final TestClass testClass = getTestClass();
        final List<FrameworkMethod> depends = testClass.getAnnotatedMethods(DependsOnMethod.class);
        if (!isNullOrEmpty(depends)) {
            final Set<String> dependencies = new HashSet<String>(hashMapCapacity(depends.size()));
            for (final FrameworkMethod method : depends) {
                for (final String value : method.getAnnotation(DependsOnMethod.class).value()) {
                    dependencies.add(value);
                }
            }
            for (final FrameworkMethod method : testClass.getAnnotatedMethods(Test.class)) {
                dependencies.remove(method.getName());
            }
            for (final String notFound : dependencies) {
                errors.add(new NoSuchMethodException("@DependsOnMethod(\"" + notFound + "\"): "
                        + "method not found in " + testClass.getName()));
            }
        }
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
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
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
        for (final FrameworkMethod method : children) {
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
        filteredChildren = ArraysExt.resize(children, count);
    }

    /**
     * Returns the {@link Statement} which will execute all the tests in the class given
     * to the {@linkplain #TestRunner(Class) constructor}.
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
                checkClassDependencies();
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
        if (skipAll) {
            notifier.fireTestIgnored(describeChild(method));
            return;
        }
        if (methodDependencyFailures != null) {
            final DependsOnMethod assumptions = method.getAnnotation(DependsOnMethod.class);
            if (assumptions != null) {
                for (final String assumption : assumptions.value()) {
                    if (methodDependencyFailures.contains(assumption)) {
                        methodDependencyFailures.add(method.getName());
                        notifier.fireTestIgnored(describeChild(method));
                        return;
                    }
                }
            }
        }
        super.runChild(method, notifier);
    }

    /**
     * Declares that the given method failed.
     * Other methods depending on this method will be ignored.
     *
     * @param methodName The name of the method that failed.
     */
    final void addDependencyFailure(final String methodName) {
        if (methodDependencyFailures == null) {
            methodDependencyFailures = new HashSet<String>();
        }
        methodDependencyFailures.add(methodName);
        synchronized (TestRunner.class) {
            if (classDependencyFailures == null) {
                classDependencyFailures = new HashSet<Class<?>>();
            }
            classDependencyFailures.add(getTestClass().getJavaClass());
        }
    }

    /**
     * If at least one test failure occurred in at least one class listed in the {@link DependsOn}
     * annotation, set the {@link #skipAll} field to {@code true}. This method shall be invoked
     * before the tests are run.
     */
    final void checkClassDependencies() {
        final Class<?> testClass = getTestClass().getJavaClass();
        final DependsOn dependsOn = testClass.getAnnotation(DependsOn.class);
        if (dependsOn != null) {
            synchronized (TestRunner.class) {
                if (classDependencyFailures != null) {
                    for (final Class<?> dependency : dependsOn.value()) {
                        if (classDependencyFailures.contains(dependency)) {
                            classDependencyFailures.add(testClass);
                            skipAll = true;
                        }
                    }
                }
            }
        }
    }
}
