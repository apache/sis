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
package org.apache.sis.xml;

import java.util.HashSet;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.io.File;
import java.io.Reader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.UnmarshallerHandler;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.attachment.AttachmentUnmarshaller;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.internal.shared.InputFactory;
import org.apache.sis.xml.internal.shared.ExternalLinkHandler;
import org.apache.sis.util.resources.Errors;


/**
 * Wraps a {@link Unmarshaller} in order to have some control on the modifications applied on it.
 * This wrapper serves three purposes:
 *
 * <ul>
 *   <li>Save properties before modification, in order to restore them to their original values
 *       when the unmarshaller is recycled.</li>
 *   <li>Constructs a SIS {@link Context} object on unmarshalling, in order to give
 *       additional information to the SIS object being unmarshalled.</li>
 *   <li>Wraps the input stream in a {@link TransformingReader} if the document GML version
 *       in not the SIS native GML version.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class PooledUnmarshaller extends Pooled implements Unmarshaller {
    /**
     * The wrapped marshaller which does the real work.
     */
    private final Unmarshaller unmarshaller;

    /**
     * Creates a pooled unmarshaller wrapping the given one.
     * Callers shall invoke {@link #reset(Pooled)} after construction for completing the initialization.
     *
     * @param  unmarshaller  the unmarshaller to use for the actual work.
     * @param  template      the {@link PooledTemplate} from which to get the initial values.
     * @throws JAXBException if an error occurred while setting a property.
     */
    PooledUnmarshaller(final Unmarshaller unmarshaller, final Pooled template) throws JAXBException {
        super(template);
        this.unmarshaller = unmarshaller;
        initialize(template);
    }

    /**
     * Resets the given unmarshaller property to its initial state.
     * This method is invoked automatically by {@link #reset(Pooled)}.
     *
     * @param  key    the property to reset.
     * @param  value  the saved initial value to give to the property.
     * @throws JAXBException if an error occurred while restoring a property.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    protected void reset(final Object key, final Object value) throws JAXBException {
        if (key instanceof String) {
            unmarshaller.setProperty((String) key, value);
        } else if (key == AttachmentUnmarshaller.class) {
            unmarshaller.setAttachmentUnmarshaller((AttachmentUnmarshaller) value);
        } else if (key == Schema.class) {
            unmarshaller.setSchema((Schema) value);
        } else if (key == Listener.class) {
            unmarshaller.setListener((Listener) value);
        } else if (key == ValidationEventHandler.class) {
            unmarshaller.setEventHandler((ValidationEventHandler) value);
        } else {
            unmarshaller.setAdapter((Class) key, (XmlAdapter) value);
        }
    }

    /**
     * Unmarshals to the given input with on-the-fly substitution of namespaces.
     * This method is invoked when we may marshal a different GML or metadata version than the one
     * supported natively by SIS, i.e. when {@link #getTransformVersion()} returns a non-null value.
     *
     * @param  input        the reader created by SIS (<b>not</b> the reader given by the user).
     * @param  version      identify the namespace substitutions to perform.
     * @param  linkHandler  the document-dependent resolver of relative URIs, or {@code null}.
     * @return the unmarshalled object.
     */
    private Object unmarshal(XMLEventReader input, final TransformVersion version, final ExternalLinkHandler linkHandler)
            throws XMLStreamException, JAXBException
    {
        input = new TransformingReader(input, version);
        final Context context = begin(linkHandler);
        final Object object;
        try {
            object = resolve(unmarshaller.unmarshal(input), linkHandler, context);
        } finally {
            context.finish();
        }
        input.close();              // Despite its name, this method does not close the underlying input stream.
        return object;
    }

    /**
     * Same as {@link #unmarshal(XMLEventReader, TransformVersion, ExternalLinkHandler)},
     * but delegating to the unmarshaller methods returning a JAXB element instead
     * of the one returning the object.
     */
    private <T> JAXBElement<T> unmarshal(XMLEventReader input, final TransformVersion version,
            final ExternalLinkHandler linkHandler, final Class<T> declaredType)
            throws XMLStreamException, JAXBException
    {
        input = new TransformingReader(input, version);
        final Context context = begin(linkHandler);
        final JAXBElement<T> object;
        try {
            object = unmarshaller.unmarshal(input, declaredType);
        } finally {
            context.finish();
        }
        input.close();              // Despite its name, this method does not close the underlying input stream.
        return object;
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public Object unmarshal(final InputStream input) throws JAXBException {
        final var linkHandler = ExternalLinkHandler.forStream(input);
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            return unmarshal(InputFactory.createXMLEventReader(input), version, linkHandler);
        } catch (XMLStreamException e) {
            throw cannotParse(null, e);
        } else {
            final Context context = begin(linkHandler);
            try {
                return resolve(unmarshaller.unmarshal(input), linkHandler, context);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     * The URL is opened by this method instead of by the wrapped unmarshaller for allowing us to update
     * the URL in case of redirection. This is necessary for resolution of relative {@code xlink:href}.
     */
    @Override
    public Object unmarshal(URL input) throws JAXBException {
        final TransformVersion version = getTransformVersion();
        final var done = new HashSet<URL>();
        for (;;) try {      // Will retry if there is redirect.
            final URLConnection connection = input.openConnection();
            if (connection instanceof HttpURLConnection) {
                final var hc = (HttpURLConnection) connection;
                if (hc.getInstanceFollowRedirects()) {
                    switch (hc.getResponseCode()) {
                        /*
                         * The HTTP_SEE_OTHER case is questionable because the new URI is not considered
                         * equivalent to the original URI. However either we accept this URI, or either
                         * we cannot parse the content.
                         */
                        case HttpURLConnection.HTTP_SEE_OTHER:
                        case HttpURLConnection.HTTP_MOVED_TEMP: case 307:       // Temporary Redirect.
                        case HttpURLConnection.HTTP_MOVED_PERM: case 308: {     // Moved Permanently.
                            if (!done.add(input)) {
                                // Safety against never-ending loop.
                                throw new IOException(Errors.format(Errors.Keys.CanNotConnectTo_1, input));
                            }
                            final String location = hc.getHeaderField("Location");
                            if (location != null) {
                                input = input.toURI().resolve(new URI(location)).toURL();
                                continue;
                            }
                        }
                    }
                }
            }
            final var linkHandler = new ExternalLinkHandler(input);
            try (InputStream s = connection.getInputStream()) {
                if (version != null) {
                    return unmarshal(InputFactory.createXMLEventReader(s), version, linkHandler);
                } else {
                    final Context context = begin(linkHandler);
                    try {
                        return resolve(unmarshaller.unmarshal(s), linkHandler, context);
                    } finally {
                        context.finish();
                    }
                }
            }
        } catch (URISyntaxException | IOException | XMLStreamException e) {
            throw cannotParse(input, e);
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public Object unmarshal(final File input) throws JAXBException {
        final var linkHandler = new ExternalLinkHandler(input);
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            try (InputStream s = new BufferedInputStream(new FileInputStream(input))) {
                return unmarshal(InputFactory.createXMLEventReader(s), version, linkHandler);
            }
        } catch (IOException | XMLStreamException e) {
            throw cannotParse(input, e);
        } else {
            final Context context = begin(linkHandler);
            try {
                // No need to invoke `resolve(…)` because a file cannot have a fragment.
                return unmarshaller.unmarshal(input);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public Object unmarshal(final Reader input) throws JAXBException {
        final var linkHandler = ExternalLinkHandler.forStream(input);
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            return unmarshal(InputFactory.createXMLEventReader(input), version, linkHandler);
        } catch (XMLStreamException e) {
            throw cannotParse(null, e);
        } else {
            final Context context = begin(linkHandler);
            try {
                return resolve(unmarshaller.unmarshal(input), linkHandler, context);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public Object unmarshal(final InputSource input) throws JAXBException {
        final var linkHandler = new ExternalLinkHandler(input.getSystemId());
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            return unmarshal(InputFactory.createXMLEventReader(input), version, linkHandler);
        } catch (XMLStreamException e) {
            throw cannotParse(input.getPublicId(), e);
        } else {
            final Context context = begin(linkHandler);
            try {
                return resolve(unmarshaller.unmarshal(input), linkHandler, context);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public Object unmarshal(final Node input) throws JAXBException {
        final var linkHandler = new ExternalLinkHandler(input.getBaseURI());
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            return unmarshal(InputFactory.createXMLEventReader(input), version, linkHandler);
        } catch (XMLStreamException e) {
            throw cannotParse(input.getNodeName(), e);
        } else {
            final Context context = begin(linkHandler);
            try {
                return resolve(unmarshaller.unmarshal(input), linkHandler, context);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public <T> JAXBElement<T> unmarshal(final Node input, final Class<T> declaredType) throws JAXBException {
        final var linkHandler = new ExternalLinkHandler(input.getBaseURI());
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            return unmarshal(InputFactory.createXMLEventReader(input), version, linkHandler, declaredType);
        } catch (XMLStreamException e) {
            throw cannotParse(input.getNodeName(), e);
        } else {
            final Context context = begin(linkHandler);
            try {
                return unmarshaller.unmarshal(input, declaredType);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public Object unmarshal(final Source input) throws JAXBException {
        final var linkHandler = new ExternalLinkHandler(input);
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            return unmarshal(InputFactory.createXMLEventReader(input), version, linkHandler);
        } catch (XMLStreamException e) {
            throw cannotParse(input.getSystemId(), e);
        } else {
            final Context context = begin(linkHandler);
            try {
                return resolve(unmarshaller.unmarshal(input), linkHandler, context);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public <T> JAXBElement<T> unmarshal(final Source input, final Class<T> declaredType) throws JAXBException {
        final var linkHandler = new ExternalLinkHandler(input);
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            return unmarshal(InputFactory.createXMLEventReader(input), version, linkHandler, declaredType);
        } catch (XMLStreamException e) {
            throw cannotParse(input.getSystemId(), e);
        } else {
            final Context context = begin(linkHandler);
            try {
                return unmarshaller.unmarshal(input, declaredType);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public Object unmarshal(final XMLStreamReader input) throws JAXBException {
        final var linkHandler = ExternalLinkHandler.create(input);
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            return unmarshal(InputFactory.createXMLEventReader(input), version, linkHandler);
        } catch (XMLStreamException e) {
            throw cannotParse(input.getLocalName(), e);
        } else {
            final Context context = begin(linkHandler);
            try {
                return resolve(unmarshaller.unmarshal(input), linkHandler, context);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public <T> JAXBElement<T> unmarshal(final XMLStreamReader input, final Class<T> declaredType) throws JAXBException {
        final var linkHandler = ExternalLinkHandler.create(input);
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            return unmarshal(InputFactory.createXMLEventReader(input), version, linkHandler, declaredType);
        } catch (XMLStreamException e) {
            throw cannotParse(input.getLocalName(), e);
        } else {
            final Context context = begin(linkHandler);
            try {
                return unmarshaller.unmarshal(input, declaredType);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public Object unmarshal(XMLEventReader input) throws JAXBException {
        final ExternalLinkHandler linkHandler;
        try {
            linkHandler = ExternalLinkHandler.create(input);
        } catch (XMLStreamException e) {
            throw cannotParse(null, e);
        }
        final TransformVersion version = getTransformVersion();
        if (version != null) {
            input = new TransformingReader(input, version);
        }
        final Context context = begin(linkHandler);
        try {
            return resolve(unmarshaller.unmarshal(input), linkHandler, context);
        } finally {
            context.finish();
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public <T> JAXBElement<T> unmarshal(XMLEventReader input, final Class<T> declaredType) throws JAXBException {
        final ExternalLinkHandler linkHandler;
        try {
            linkHandler = ExternalLinkHandler.create(input);
        } catch (XMLStreamException e) {
            throw cannotParse(null, e);
        }
        final TransformVersion version = getTransformVersion();
        if (version != null) {
            input = new TransformingReader(input, version);
        }
        final Context context = begin(linkHandler);
        try {
            return unmarshaller.unmarshal(input, declaredType);
        } finally {
            context.finish();
        }
    }

    /**
     * If the input is a fragment, returns the object referenced by the fragment.
     * Otherwise returns the given object unchanged.
     *
     * @param  object       the object parsed from the whole document.
     * @param  linkHandler  the document-dependent resolver of relative URIs, or {@code null}.
     * @param  context      the marshalling context, or {@code null}.
     * @return object referenced by the fragment, or the given {@code object} if no fragment was specified.
     */
    private static Object resolve(final Object object, final ExternalLinkHandler linkHandler, final Context context) {
        if (linkHandler != null) {
            final String fragment = linkHandler.getFragment();
            if (fragment != null) {
                final Object r = Context.getObjectForID(context, fragment);
                if (r != null) {
                    return r;
                }
                Context.warningOccured(context, PooledUnmarshaller.class, "unmarshal",
                        Errors.class, Errors.Keys.NotABackwardReference_1, fragment);
            }
        }
        return object;
    }

    /**
     * Returns the exception to throw for an input file or URL that cannot be parsed.
     */
    private static JAXBException cannotParse(final Object input, final Exception cause) {
        return new JAXBException((input != null) ? Errors.format(Errors.Keys.CanNotParse_1, input) : cause.getMessage(), cause);
    }

    /**
     * Delegates to the wrapped unmarshaller.
     */
    @Override
    public UnmarshallerHandler getUnmarshallerHandler() {
        return unmarshaller.getUnmarshallerHandler();
    }

    /**
     * Delegates to the wrapped unmarshaller. This method is invoked by the parent
     * class if the given name was not one of the {@link XML} constants.
     */
    @Override
    void setStandardProperty(final String name, final Object value) throws PropertyException {
        unmarshaller.setProperty(name, value);
    }

    /**
     * Delegates to the wrapped unmarshaller. This method is invoked by the parent
     * class if the given name was not one of the {@link XML} constants.
     */
    @Override
    Object getStandardProperty(final String name) throws PropertyException {
        return unmarshaller.getProperty(name);
    }

    /**
     * Delegates to the wrapped unmarshaller. The initial state will be saved
     * if it was not already done, for future restoration by {@link #reset(Pooled)}.
     */
    @Override
    public <A extends XmlAdapter<?,?>> void setAdapter(final Class<A> type, final A adapter) {
        super.setAdapter(type, adapter);
        unmarshaller.setAdapter(type, adapter);
    }

    /**
     * Delegates to the wrapped unmarshaller.
     */
    @Override
    public <A extends XmlAdapter<?,?>> A getAdapter(final Class<A> type) {
        return unmarshaller.getAdapter(type);
    }

    /**
     * Delegates to the wrapped unmarshaller. The initial state will be saved
     * if it was not already done, for future restoration by {@link #reset(Pooled)}.
     */
    @Override
    public void setSchema(final Schema schema) {
        super.setSchema(schema);
        unmarshaller.setSchema(schema);
    }

    /**
     * Delegates to the wrapped unmarshaller.
     */
    @Override
    public Schema getSchema() {
        return unmarshaller.getSchema();
    }

    /**
     * Delegates to the wrapped unmarshaller. The initial state will be saved
     * if it was not already done, for future restoration by {@link #reset(Pooled)}.
     */
    @Override
    public void setEventHandler(final ValidationEventHandler handler) throws JAXBException {
        super.setEventHandler(handler);
        unmarshaller.setEventHandler(handler);
    }

    /**
     * Delegates to the wrapped unmarshaller.
     */
    @Override
    public ValidationEventHandler getEventHandler() throws JAXBException {
        return unmarshaller.getEventHandler();
    }

    /**
     * Delegates to the wrapped unmarshaller. The initial state will be saved
     * if it was not already done, for future restoration by {@link #reset(Pooled)}.
     */
    @Override
    public void setAttachmentUnmarshaller(final AttachmentUnmarshaller au) {
        if (!isPropertySaved(AttachmentUnmarshaller.class)) {
            saveProperty(AttachmentUnmarshaller.class, unmarshaller.getAttachmentUnmarshaller());
        }
        unmarshaller.setAttachmentUnmarshaller(au);
    }

    /**
     * Delegates to the wrapped unmarshaller.
     */
    @Override
    public AttachmentUnmarshaller getAttachmentUnmarshaller() {
        return unmarshaller.getAttachmentUnmarshaller();
    }

    /**
     * Delegates to the wrapped marshaller. The initial state will be saved
     * if it was not already done, for future restoration by {@link #reset(Pooled)}.
     */
    @Override
    public void setListener(final Listener listener) {
        if (!isPropertySaved(Listener.class)) {
            saveProperty(Listener.class, unmarshaller.getListener());
        }
        unmarshaller.setListener(listener);
    }

    /**
     * Delegates to the wrapped marshaller.
     */
    @Override
    public Listener getListener() {
        return unmarshaller.getListener();
    }
}
