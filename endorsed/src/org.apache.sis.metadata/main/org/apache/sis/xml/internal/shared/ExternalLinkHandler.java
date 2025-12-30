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
package org.apache.sis.xml.internal.shared;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.stream.Location;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXSource;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.ReferenceResolver;
import org.apache.sis.xml.bind.Context;


/**
 * Resolves relative or absolute {@code xlink:href} attribute as an absolute URI.
 * This class is used for resolving {@code xlink:href} values referencing a fragment
 * outside the document being parsed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class ExternalLinkHandler {
    /**
     * The default resolver used when URIs cannot be resolved.
     * This resolver lets absolute URIs pass-through, and returns {@code null} for all others.
     */
    public static final ExternalLinkHandler DEFAULT = new ExternalLinkHandler((String) null);

    /**
     * The base URI as a {@link String}, {@link File}, {@link URL} or {@link URI}, or {@code null}.
     * If the value is not already an URI, then it will be converted to {@link URI} when first needed.
     * If the conversion fails, then this value is set to {@code null} for avoiding to try again.
     *
     * <p>Note that the URI is a path to the sibling document rather than a path to the parent directory.
     * This is okay, {@link URI#resolve(URI)} appears to behave as intended for deriving relative paths.
     * Fragment (the text after '#'), if any, will be ignored.</p>
     *
     * @see #resolve(URI)
     */
    private Object base;

    /**
     * The fragment (without leading dash) from a {@link URL}, {@link URI} or {@link CharSequence} instance.
     * If no fragment is found, or if the {@linkplain #base} does not support fragments, this is {@code null}.
     *
     * @see #getFragment()
     */
    private String fragment;

    /**
     * Creates a new resolver for documents relative to the document in the specified URL.
     * The given URL can be what StAX, SAX and DOM call {@code systemId}.
     * According StAX documentation, {@code systemId} value is used to resolve relative URIs.
     * {@link javax.xml.transform.stream.StreamSource} sets it to {@link URI#toASCIIString()}.
     *
     * @param  sibling  URL to the sibling document, or {@code null} if none.
     */
    public ExternalLinkHandler(final String sibling) {
        base = sibling;
    }

    /**
     * Creates a new resolver for documents relative to the document in the specified file.
     *
     * @param  sibling  path to the sibling document.
     */
    public ExternalLinkHandler(final File sibling) {
        base = sibling;
    }

    /**
     * Creates a new resolver for documents relative to the document at the specified URL.
     *
     * @param  sibling  URL to the sibling document.
     */
    public ExternalLinkHandler(final URL sibling) {
        base = sibling;
        fragment = sibling.getRef();
    }

    /**
     * Creates a new resolver for documents relative to the document read from the specified source.
     *
     * @param  sibling  source to the sibling document.
     */
    public ExternalLinkHandler(final Source sibling) {
        if (sibling instanceof URISource) {
            final var s = (URISource) sibling;
            base = s.document;
            fragment = s.fragment;
        } else {
            base = sibling.getSystemId();
        }
    }

    /**
     * Creates a new resolver for documents relative to the document written to the specified result.
     *
     * @param  sibling  result of the sibling document.
     */
    public ExternalLinkHandler(final Result sibling) {
        base = sibling.getSystemId();
    }

    /**
     * Returns the base URI of the link handler. The returned object may be an instance of
     * {@link String}, {@link File}, {@link URL} or {@link URI}, or it may be {@code null}.
     *
     * @return the base <abbr>URI</abbr>, or {@code null}.
     */
    public final Object getBase() {
        return base;
    }

    /**
     * Returns the fragment (without leading dash) from a {@link URL}, {@link URI} or {@link CharSequence} instance.
     * If no fragment is found, or if the {@linkplain #base} does not support fragments, return {@code null}.
     *
     * @return the fragment in the base URI, or {@code null} if none.
     */
    public final String getFragment() {
        if (fragment == null) {
            final URI uri = getURI();
            if (uri != null) {
                fragment = uri.getFragment();
            }
        }
        return fragment;
    }

    /**
     * Returns the base URI of the link handler. This is the same value as {@link #getBase()},
     * but converted to an {@link URI} object when first invoked.
     *
     * @return the base URI of the link handler, or {@code null} if none.
     */
    public final URI getURI() {
        final Object b = base;
        if (b == null) {
            return null;
        }
        URI baseURI;
        if (b instanceof URI) {         // `instanceof` check of final classes are efficient.
            baseURI = (URI) b;
        } else {
            base = null;                // Clear first in case of failure, for avoiding to try again later.
            try {
                if (b instanceof String) {
                    baseURI = new URI((String) b);
                } else if (b instanceof URL) {
                    baseURI = ((URL) b).toURI();
                } else if (b instanceof File) {
                    baseURI = ((File) b).toURI();
                } else {
                    return null;
                }
            } catch (URISyntaxException e) {
                warningOccured(b, e);
                return null;
            }
            baseURI = baseURI.normalize();
            base = baseURI;
        }
        return baseURI;
    }

    /**
     * Resolves the given path as an URI. This method behaves as specified in {@link URI#resolve(URI)},
     * with the URI given at construction-time as the base URI. If the given path is relative and there
     * is no base URI, then the path cannot be resolved and this method returns {@code null}.
     *
     * @param  path  path to resolve.
     * @return resolved path, or {@code null} it it cannot be resolved.
     *
     * @see URI#resolve(URI)
     */
    final URI resolve(final URI path) {
        final URI baseURI = getURI();
        if (baseURI != null) {
            return baseURI.resolve(path);
        }
        return path.isAbsolute() ? path : null;
    }

    /**
     * Reports a warning about a URI that cannot be parsed.
     * This method declares {@link ReferenceResolver} as the public source of the warning.
     * The latter assumption is valid if {@code ReferenceResolver.resolve(…)} is the only
     * code invoking, directly or indirectly, this {@code warning(…)} method.
     *
     * @param  href  the URI that cannot be parsed.
     * @param  cause the exception that occurred while trying to process the document.
     */
    public static void warningOccured(final Object href, final Exception cause) {
        new ExceptionSimplifier(href, cause).report(Context.current(), ReferenceResolver.class, "resolve");
    }

    /**
     * Returns the source of the XML document at the given path.
     *
     * @param  path  relative or absolute path to the XML document to read.
     * @return source of the XML document, or {@code null} if the path cannot be resolved.
     * @throws Exception if an error occurred while creating the source.
     */
    public Source openReader(URI path) throws Exception {
        path = resolve(path);
        return (path != null) ? new URISource(path) : null;
    }

    /*
     * A future version may add `openWriter(URI)` for splitting a large document into many smaller documents.
     */

    /**
     * Creates a link resolver for a XML document reads from the given input stream or character reader.
     * Also invoked for document written to the given output stream or character writer.
     *
     * @param  input  the {@link java.io.InputStream} or {@link java.io.Reader}, or {@code null} if none.
     * @return the resolver for the given input stream or character reader, or {@code null} if none.
     */
    public static ExternalLinkHandler forStream(final Object input) {
        // TODO: define an interface for allowing us to fetch this information.
        return null;
    }

    /**
     * Creates a link resolver for a XML document reads from the given stream.
     *
     * @param  input  the XML stream reader.
     * @return the resolver for the given input, or {@code null} if none.
     */
    public static ExternalLinkHandler create(final XMLStreamReader input) {
        return forStAX(input.getProperty(XMLInputFactory.RESOLVER), input.getLocation());
    }

    /**
     * Creates a link resolver for a XML document reads from the given events.
     *
     * @param  input  the XML event reader.
     * @return the resolver for the given input, or {@code null} if none.
     * @throws XMLStreamException if an error occurred while inspecting the reader.
     */
    public static ExternalLinkHandler create(final XMLEventReader input) throws XMLStreamException {
        return forStAX(input.getProperty(XMLInputFactory.RESOLVER), input.peek().getLocation());
    }

    /**
     * Creates a link resolver for a XML document reads a StAX stream or event reader.
     *
     * @param  property  value of the {@value XMLInputFactory#RESOLVER} property. May be null.
     * @param  location  current location of the reader, or {@code null} if unknown.
     * @return the resolver for the stream or even reader, or {@code null} if none.
     */
    private static ExternalLinkHandler forStAX(final Object property, final Location location) {
        final String base;
        if (location == null || (base = location.getSystemId()) == null) {
            return null;
        }
        if (!(property instanceof XMLResolver)) {
            return new ExternalLinkHandler(base);
        }
        final var resolver = (XMLResolver) property;
        return new ExternalLinkHandler(base) {
            @Override public Source openReader(final URI path) throws Exception {
                /*
                 * According StAX specification, the return type can be either InputStream,
                 * XMLStreamReader or XMLEventReader. We additionally accept Source as well.
                 * Types are tested from highest-level to lowest-level.
                 *
                 * TODO: need to provide a non-null namespace (the last argument).
                 */
                final Object source = resolver.resolveEntity(null, path.toString(), base, null);
                if (source == null || source instanceof Source) {
                    return (Source) source;
                } else if (source instanceof XMLEventReader) {
                    return new StAXSource((XMLEventReader) source);
                } else if (source instanceof XMLStreamReader) {
                    return new StAXSource((XMLStreamReader) source);
                } else if (source instanceof InputStream) {
                    return URISource.create((InputStream) source, resolve(path));
                } else {
                    throw new XMLStreamException(Errors.format(Errors.Keys.UnsupportedType_1, source.getClass()));
                }
            }
        };
    }

    /**
     * Returns a string representation of this link handler for debugging purposes.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        return Strings.toString(getClass(), "base", base);
    }
}
