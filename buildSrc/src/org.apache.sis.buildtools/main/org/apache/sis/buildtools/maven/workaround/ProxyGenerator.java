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
package org.apache.sis.buildtools.maven.workaround;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/**
 * Generates {@code META-INF/services} declarations and, if needed, proxy classes.
 * The input is one or more JAR files containing {@code module-info.class} files.
 * The output is {@code META-INF/services} and (optionally) Java wrapper classes
 * written in the specified target directory. See MNG-7855 for more information.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @see <a href="https://issues.apache.org/jira/browse/MNG-7855">MNG-7855 on JIRA issue tracker</a>
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
public class ProxyGenerator {
    /**
     * Executes from the command-line. The arguments shall contain {@code --out=dir}
     * where {@code dir} is the target directory where to write generated files.
     * Other arguments that are not options are JAR files.
     *
     * @param  args  command-line arguments as {@code <option>=<value>} pairs followed by JAR files.
     * @throws IOException if an error occurred while reading a JAR file or writing the generated files.
     */
    public static void main(final String[] args) throws IOException {
        final EnumMap<Argument,String> options = Argument.parse(args);
        final String target = options.get(Argument.OUT);
        ProxyGenerator generator = null;
        if (target != null) {
            for (final String arg : args) {
                if (arg != null) {
                    if (generator == null) {
                        generator = new ProxyGenerator(new File(target), options.get(Argument.PACKAGE));
                    }
                    generator.collectProviders(new File(arg));
                }
            }
        }
        if (generator != null) {
            generator.checkStaticProviderMethods();
            generator.generate();
        } else {
            Argument.help();
        }
    }

    /**
     * Class names of all service providers found so far.
     * Keys are names of the classes or interfaces defining the services.
     * Values are names of the implementation classes.
     */
    private final Map<String, List<String>> providers;

    /**
     * {@code true} if we found at least one provider which is not on the classpath.
     * This is used for avoiding to flood the console with too many warning messages.
     */
    private boolean providerNotOnClasspath;

    /**
     * {@code true} if at least one public static {@code provider()} method has been found.
     * This is used for avoiding to flood the console with too many warning messages.
     */
    private boolean providerStaticMethodFound;

    /**
     * Name of the Java package to use if proxy Java classed need to be generated.
     * Null if the {@link Argument#PACKAGE} option has not been specified.
     */
    private final String packageName;

    /**
     * Number of the proxy generated, used in class name.
     */
    private int proxyNumber;

    /**
     * Destination directory where to write the generated files.
     */
    private final File target;

    /**
     * Creates a new generator of {@code META-INF/services} declarations.
     *
     * @param  target       destination directory where to write the generated files.
     * @param  packageName  name of the Java package to use if proxy Java classed need to be generated.
     */
    private ProxyGenerator(final File target, final String packageName) {
        this.target      = target;
        this.packageName = packageName;
        this.providers   = new HashMap<>();
    }

    /**
     * Collects all providers declared in the {@code module-info.class} entry of the specified JAR file.
     *
     * @param  file  the file for which to collect service providers.
     * @throws IOException if an error occurred while reading the JAR file.
     */
    private void collectProviders(final File file) throws IOException {
        try (JarFile zip = new JarFile(file, false, JarFile.OPEN_READ, JarFile.runtimeVersion())) {
            final ZipEntry entry = zip.getEntry("module-info.class");
            if (entry != null) {
                try (InputStream in = zip.getInputStream(entry)) {
                    final ModuleDescriptor desc = ModuleDescriptor.read(in);
                    desc.provides().forEach((provides) -> {
                        providers.computeIfAbsent(provides.service(), (service) -> new ArrayList<>())
                                          .addAll(provides.providers());
                    });
                }
            }
        }
    }

    /**
     * Verifies is any service provider declares a public static {@code provider()} method.
     * Those methods are incompatible with {@code META-INF/services} and need to be replaced
     * by a proxy class.
     *
     * @throws IOException if an error occurred while generating a proxy class.
     */
    private void checkStaticProviderMethods() throws IOException {
        for (final Map.Entry<String, List<String>> entry : providers.entrySet()) {
            final List<String> providers = entry.getValue();
            for (int i=0; i < providers.size(); i++) {
                final Class<?> service, provider;
                try {
                    provider = Class.forName(providers.get(i));
                    service  = Class.forName(entry.getKey());   // Should not fail if above line didn't fail.
                    if (!Modifier.isStatic(provider.getMethod("provider", (Class[]) null).getModifiers())) {
                        continue;
                    }
                } catch (ClassNotFoundException e) {
                    if (!providerNotOnClasspath) {
                        providerNotOnClasspath = true;
                        System.err.printf("The implementation class of at least one service provider could not be found.%n"
                                        + "ProxyGenerator cannot verify if a public static provider() method exists.%n"
                                        + "If this check is desired, add the provider implementations on the classpath.%n"
                                        + "The error is: %s%n", e);
                    }
                    continue;
                } catch (NoSuchMethodException e) {
                    // No public `provider()` method. This is normal.
                    continue;
                }
                if (packageName != null) {
                    final String replacement = writeJavaProxyClass(service, provider);
                    providers.set(i, replacement);
                } else if (!providerStaticMethodFound) {
                    providerStaticMethodFound = true;
                    System.out.printf("A public static provider() method was found%n"
                                    + "but the --package option was not specified.%n"
                                    + "Provider is: %s%n", provider.getCanonicalName());
                }
            }
        }
    }

    /**
     * Generates Java code for the proxy class.
     *
     * @param  service   the service to provide.
     * @param  provider  the provider to wrap.
     * @return fully qualified name of the proxy class.
     * @throws IOException if an error occurred while writing the proxy class.
     */
    private String writeJavaProxyClass(final Class<?> service, final Class<?> provider) throws IOException {
        final String proxyName = String.format("Proxy%03d", ++proxyNumber);
        File file = new File(new File(target, "java"), packageName.replace('.', File.separatorChar));
        file.mkdirs();
        file = new File(file, proxyName);
        try (final var out = new BufferedWriter(new FileWriter(file))) {
            final String ls = System.lineSeparator();
            out.append("package ").append(packageName).append(';').append(ls)
               .append(ls)
               .append("public final class ").append(proxyName)
               .append(" implements ").append(service.getCanonicalName()).append(" {").append(ls)
               .append("    private final ").append(service.getCanonicalName()).append(" provider;").append(ls)
               .append("    public ").append(proxyName).append("() {").append(ls)
               .append("        provider = ").append(provider.getCanonicalName()).append(".provider();").append(ls)
               .append("    }").append(ls);
            for (final Method m : service.getMethods()) {
                out.append("    public ").append(m.getReturnType().getCanonicalName())
                   .append(' ').append(m.getName()).append('(');
                final Class<?>[] args = m.getParameterTypes();
                for (int i=0; i<args.length; i++) {
                    if (i != 0) out.append(", ");
                    out.append(args[i].getCanonicalName()).append(" p").append(Integer.toString(i));
                }
                out.append(')');
                String separator = " throws ";
                for (final Class<?> c : m.getExceptionTypes()) {
                    out.append(separator).append(c.getCanonicalName());
                    separator = ", ";
                }
                out.append(" {").append(ls).append("        ");
                if (m.getReturnType() != Void.TYPE) {
                    out.append("return ");
                }
                out.append("provider.").append(m.getName()).append('(');
                for (int i=0; i<args.length; i++) {
                    if (i != 0) out.append(", ");
                    out.append('p').append(Integer.toString(i));
                }
                out.append(");").append(ls)
                   .append("    }").append(ls);
            }
            out.append('}').append(ls);
        }
        return packageName + '.' + proxyName;
    }

    /**
     * Generates the {@code META-INF/services} files in the target directory.
     *
     * @throws IOException if an error occurred while writing a file.
     */
    private void generate() throws IOException {
        final File services = new File(target, "resources/META-INF/services");
        services.mkdirs();
        for (final Map.Entry<String, List<String>> entry : providers.entrySet()) {
            try (BufferedWriter out = new BufferedWriter(new FileWriter(new File(services, entry.getKey())))) {
                out.write("# Workaround for Maven bug https://issues.apache.org/jira/browse/MNG-7855");        out.newLine();
                out.write("# The content of this file is automatically derived from module-info.class file."); out.newLine();
                out.write("# Should be used only if the JAR file was on class-path rather than module-path."); out.newLine();
                for (final String provider : entry.getValue()) {
                    out.write(provider);
                    out.write(System.lineSeparator());
                }
            }
        }
    }

    /**
     * {@returns the class names of all service providers found so far}.
     * This information is provided for debugging purposes only.
     */
    @Override
    public String toString() {
        final var sb = new StringBuilder(1000);
        providers.entrySet().forEach((entry) -> {
            sb.append(entry.getKey()).append(':').append(System.lineSeparator());
            entry.getValue().forEach((provider) -> {
                sb.append("    ").append(provider).append(System.lineSeparator());
            });
        });
        return sb.toString();
    }
}
