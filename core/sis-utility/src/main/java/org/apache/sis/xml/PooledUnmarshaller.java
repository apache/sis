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

import java.net.URL;
import java.io.File;
import java.io.Reader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.system.XMLInputFactory;


/**
 * Wraps a {@link Unmarshaller} in order to have some control on the modifications applied on it.
 * This wrapper serves three purposes:
 *
 * <ul>
 *   <li>Save properties before modification, in order to restore them to their original values
 *       when the unmarshaller is recycled.</li>
 *   <li>Constructs a SIS {@link Context} object on unmarshalling, in order to give
 *       additional information to the SIS object being unmarshalled.</li>
 *   <li>Wraps the input stream in a {@link FilteredStreamReader} if the document GML version
 *       in not the SIS native GML version.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
final class PooledUnmarshaller extends Pooled implements Unmarshaller {
    /**
     * The wrapper marshaller which does the real work.
     */
    private final Unmarshaller unmarshaller;

    /**
     * Creates a pooled unmarshaller wrapping the given one.
     * Callers shall invoke {@link #reset(Pooled)} after construction for completing the initialization.
     *
     * @param  unmarshaller The unmarshaller to use for the actual work.
     * @param  template The {@link PooledTemplate} from which to get the initial values.
     * @throws JAXBException If an error occurred while setting a property.
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
     * @param  key   The property to reset.
     * @param  value The saved initial value to give to the property.
     * @throws JAXBException If an error occurred while restoring a property.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes","deprecation"})
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
        } else if (key == Boolean.class) {
            unmarshaller.setValidating((Boolean) value);
        } else {
            unmarshaller.setAdapter((Class) key, (XmlAdapter) value);
        }
    }

    /**
     * Unmarshals to the given input with on-the-fly substitution of namespaces.
     * This method is invoked only when the user asked to marshal from a different GML version
     * than the one supported natively by SIS, i.e. when {@link #getFilterVersion()} returns a
     * non-null value.
     *
     * @param  input   The reader created by SIS (<b>not</b> the reader given by the user).
     * @param  version Identify the namespace substitutions to perform.
     * @return The unmarshalled object.
     */
    private Object unmarshal(XMLStreamReader input, final FilterVersion version)
            throws XMLStreamException, JAXBException
    {
        input = new FilteredStreamReader(input, version);
        final Context context = begin();
        final Object object;
        try {
            object = unmarshaller.unmarshal(input);
        } finally {
            context.finish();
        }
        input.close(); // Despite its name, this method does not close the underlying input stream.
        return object;
    }

    /**
     * Same as {@link #unmarshal(XMLStreamReader, FilterVersion)}, but delegating to the unmarshaller
     * methods returning a JAXB element instead than the one returning the object.
     */
    private <T> JAXBElement<T> unmarshal(XMLStreamReader input, final FilterVersion version, final Class<T> declaredType)
            throws XMLStreamException, JAXBException
    {
        input = new FilteredStreamReader(input, version);
        final Context context = begin();
        final JAXBElement<T> object;
        try {
            object = unmarshaller.unmarshal(input, declaredType);
        } finally {
            context.finish();
        }
        input.close(); // Despite its name, this method does not close the underlying input stream.
        return object;
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public Object unmarshal(final InputStream input) throws JAXBException {
        final FilterVersion version = getFilterVersion();
        if (version != null) try {
            return unmarshal(XMLInputFactory.createXMLStreamReader(input), version);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            final Context context = begin();
            try {
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
    public Object unmarshal(final URL input) throws JAXBException {
        final FilterVersion version = getFilterVersion();
        if (version != null) try {
            final InputStream s = input.openStream();
            try {
                return unmarshal(XMLInputFactory.createXMLStreamReader(s), version);
            } finally {
                s.close();
            }
        } catch (Exception e) { // (IOException | XMLStreamException) on the JDK7 branch.
            throw new JAXBException(e);
        } else {
            final Context context = begin();
            try {
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
    public Object unmarshal(final File input) throws JAXBException {
        final FilterVersion version = getFilterVersion();
        if (version != null) try {
            final InputStream s = new BufferedInputStream(new FileInputStream(input));
            try {
                return unmarshal(XMLInputFactory.createXMLStreamReader(s), version);
            } finally {
                s.close();
            }
        } catch (Exception e) { // (IOException | XMLStreamException) on the JDK7 branch.
            throw new JAXBException(e);
        } else {
            final Context context = begin();
            try {
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
        final FilterVersion version = getFilterVersion();
        if (version != null) try {
            return unmarshal(XMLInputFactory.createXMLStreamReader(input), version);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            final Context context = begin();
            try {
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
    public Object unmarshal(final InputSource input) throws JAXBException {
        final FilterVersion version = getFilterVersion();
        if (version != null) try {
            return unmarshal(XMLInputFactory.createXMLStreamReader(input), version);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            final Context context = begin();
            try {
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
    public Object unmarshal(final Node input) throws JAXBException {
        final FilterVersion version = getFilterVersion();
        if (version != null) try {
            return unmarshal(XMLInputFactory.createXMLStreamReader(input), version);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            final Context context = begin();
            try {
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
    public <T> JAXBElement<T> unmarshal(final Node input, final Class<T> declaredType) throws JAXBException {
        final FilterVersion version = getFilterVersion();
        if (version != null) try {
            return unmarshal(XMLInputFactory.createXMLStreamReader(input), version, declaredType);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            final Context context = begin();
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
        final FilterVersion version = getFilterVersion();
        if (version != null) try {
            return unmarshal(XMLInputFactory.createXMLStreamReader(input), version);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            final Context context = begin();
            try {
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
    public <T> JAXBElement<T> unmarshal(final Source input, final Class<T> declaredType) throws JAXBException {
        final FilterVersion version = getFilterVersion();
        if (version != null) try {
            return unmarshal(XMLInputFactory.createXMLStreamReader(input), version, declaredType);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            final Context context = begin();
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
    public Object unmarshal(XMLStreamReader input) throws JAXBException {
        final FilterVersion version = getFilterVersion();
        if (version != null) {
            input = new FilteredStreamReader(input, version);
        }
        final Context context = begin();
        try {
            return unmarshaller.unmarshal(input);
        } finally {
            context.finish();
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public <T> JAXBElement<T> unmarshal(XMLStreamReader input, final Class<T> declaredType) throws JAXBException {
        final FilterVersion version = getFilterVersion();
        if (version != null) {
            input = new FilteredStreamReader(input, version);
        }
        final Context context = begin();
        try {
            return unmarshaller.unmarshal(input, declaredType);
        } finally {
            context.finish();
        }
    }

    /**
     * Delegates the unmarshalling to the wrapped unmarshaller.
     */
    @Override
    public Object unmarshal(final XMLEventReader input) throws JAXBException {
        final FilterVersion version = getFilterVersion();
        if (version != null) try {
            return unmarshal(XMLInputFactory.createXMLStreamReader(input), version);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            final Context context = begin();
            try {
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
    public <T> JAXBElement<T> unmarshal(final XMLEventReader input, final Class<T> declaredType) throws JAXBException {
        final FilterVersion version = getFilterVersion();
        if (version != null) try {
            return unmarshal(XMLInputFactory.createXMLStreamReader(input), version, declaredType);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            final Context context = begin();
            try {
                return unmarshaller.unmarshal(input, declaredType);
            } finally {
                context.finish();
            }
        }
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
    @SuppressWarnings("rawtypes")
    public <A extends XmlAdapter> void setAdapter(final Class<A> type, final A adapter) {
        super.setAdapter(type, adapter);
        unmarshaller.setAdapter(type, adapter);
    }

    /**
     * Delegates to the wrapped unmarshaller.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public <A extends XmlAdapter> A getAdapter(final Class<A> type) {
        return unmarshaller.getAdapter(type);
    }

    /**
     * Delegates to the wrapped unmarshaller. The initial state will be saved
     * if it was not already done, for future restoration by {@link #reset(Pooled)}.
     *
     * @deprecated Replaced by {@link #setSchema(javax.xml.validation.Schema)} in JAXB 2.0.
     */
    @Override
    @Deprecated
    public void setValidating(final boolean validating) throws JAXBException {
        if (!isPropertySaved(Boolean.class)) {
            saveProperty(Boolean.class, unmarshaller.isValidating());
        }
        unmarshaller.setValidating(validating);
    }

    /**
     * Delegates to the wrapped unmarshaller.
     *
     * @deprecated Replaced by {@link #getSchema()} in JAXB 2.0.
     */
    @Override
    @Deprecated
    public boolean isValidating() throws JAXBException {
        return unmarshaller.isValidating();
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
