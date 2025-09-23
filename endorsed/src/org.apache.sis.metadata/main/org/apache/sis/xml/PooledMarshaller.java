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

import java.io.File;
import java.io.Writer;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.function.UnaryOperator;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.validation.Schema;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.attachment.AttachmentMarshaller;
import org.xml.sax.ContentHandler;
import org.w3c.dom.Node;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.UseLegacyMetadata;
import org.apache.sis.xml.internal.shared.ExternalLinkHandler;


/**
 * Wraps a {@link Marshaller} in order to have some control on the modifications applied on it.
 * This wrapper serves three purposes:
 *
 * <ul>
 *   <li>Save properties before modification, in order to restore them to their original values
 *       when the marshaller is recycled.</li>
 *   <li>Constructs a SIS {@link Context} object on marshalling, in order to give
 *       additional information to the SIS object being marshalled.</li>
 *   <li>Wraps the output stream in a {@link TransformingWriter} if the desired GML version
 *       in not the SIS native GML version.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class PooledMarshaller extends Pooled implements Marshaller {
    /**
     * The wrapper marshaller which does the real work.
     */
    private final Marshaller marshaller;

    /**
     * Bit masks specific to the object being marshalled. This mask will be combined with the
     * bit masks managed by the {@link Pooled} base class.  This is used mostly for mandating
     * legacy metadata format (ISO 19139:2007) for some object to marshal.
     */
    private int specificBitMasks;

    /**
     * Creates a pooled marshaller wrapping the given one.
     * Callers shall invoke {@link #reset(Pooled)} after construction for completing the initialization.
     *
     * @param  marshaller  the marshaller to use for the actual work.
     * @param  template    the {@link PooledTemplate} from which to get the initial values.
     * @throws JAXBException if an error occurred while setting a property.
     */
    PooledMarshaller(final Marshaller marshaller, final Pooled template) throws JAXBException {
        super(template);
        this.marshaller = marshaller;
        initialize(template);
    }

    /**
     * Resets the given marshaller property to its initial state.
     * This method is invoked automatically by {@link #reset(Pooled)}.
     *
     * @param  key    the property to reset.
     * @param  value  the saved initial value to give to the property.
     * @throws JAXBException if an error occurred while restoring a property.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    protected void reset(final Object key, Object value) throws JAXBException {
        if (key instanceof String) {
            final String k = (String) key;
            if (value == null && (k.endsWith(".xmlHeaders") || k.equals(JAXB_SCHEMA_LOCATION))) {
                value = "";      // Null value does not seem to be accepted for those properties.
            }
            marshaller.setProperty(k, value);
        } else if (key == AttachmentMarshaller.class) {
            marshaller.setAttachmentMarshaller((AttachmentMarshaller) value);
        } else if (key == Schema.class) {
            marshaller.setSchema((Schema) value);
        } else if (key == Listener.class) {
            marshaller.setListener((Listener) value);
        } else if (key == ValidationEventHandler.class) {
            marshaller.setEventHandler((ValidationEventHandler) value);
        } else {
            marshaller.setAdapter((Class) key, (XmlAdapter) value);
        }
    }

    /**
     * Returns the encoding of the XML document to write.
     */
    private String getEncoding() throws PropertyException {
        return (String) marshaller.getProperty(JAXB_ENCODING);
    }

    /**
     * Returns a non-zero bitmask if the object given in last call to {@link #toImplementation(Object)} should use
     * legacy metadata. This is a hack for marshalling metadata in GML. May be deleted if we implement SIS-401.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-401">SIS-401</a>
     */
    @Override
    final int specificBitMasks() {
        return specificBitMasks;
    }

    /**
     * Converts the given arbitrary object to an implementation having JAXB annotations.
     * If the given object is not recognized or is already an instance of the expected class,
     * then it is returned unchanged.
     */
    private Object toImplementation(final Object value) {
        specificBitMasks = value.getClass().isAnnotationPresent(UseLegacyMetadata.class) ? Context.LEGACY_METADATA : 0;
        final UnaryOperator<Object>[] converters = getRootAdapters();
        if (converters != null) {
            for (final UnaryOperator<Object> t : converters) {
                final Object c = t.apply(value);
                if (c != null) return c;
            }
        }
        return value;
    }

    /**
     * Marshals to the given output with on-the-fly substitution of namespaces.
     * This method is invoked when the user asked to marshal to a different GML or metadata version than the
     * one supported natively by SIS, i.e. when {@link #getTransformVersion()} returns a non-null value.
     *
     * @param object       the object to marshal.
     * @param output       the writer created by SIS (<b>not</b> the writer given by the user).
     * @param version      identifies the namespace substitutions to perform.
     * @param linkHandler  the document-dependent creator of relative URIs, or {@code null}.
     */
    private void marshal(Object object, XMLEventWriter output, final TransformVersion version, final ExternalLinkHandler linkHandler)
            throws XMLStreamException, JAXBException
    {
        output = new TransformingWriter(output, version);
        final Context context = begin(linkHandler);
        try {
            marshaller.marshal(object, output);
        } finally {
            context.finish();
        }
        output.close();         // Despite its name, this method does not close the underlying output stream.
    }

    /**
     * Delegates the marshalling to the wrapped marshaller.
     */
    @Override
    public void marshal(Object object, final Result output) throws JAXBException {
        object = toImplementation(object);                          // Must be call before getTransformVersion()
        final var linkHandler = new ExternalLinkHandler(output);
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            marshal(object, OutputFactory.createXMLEventWriter(output), version, linkHandler);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            // Marshalling to the default GML version.
            final Context context = begin(linkHandler);
            try {
                marshaller.marshal(object, output);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the marshalling to the wrapped marshaller.
     */
    @Override
    public void marshal(Object object, final OutputStream output) throws JAXBException {
        object = toImplementation(object);                          // Must be call before getTransformVersion()
        final var linkHandler = ExternalLinkHandler.forStream(output);
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            marshal(object, OutputFactory.createXMLEventWriter(output, getEncoding()), version, linkHandler);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            // Marshalling to the default GML version.
            final Context context = begin(linkHandler);
            try {
                marshaller.marshal(object, output);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the marshalling to the wrapped marshaller.
     */
    @Override
    public void marshal(Object object, final File output) throws JAXBException {
        object = toImplementation(object);                          // Must be call before getTransformVersion()
        final var linkHandler = new ExternalLinkHandler(output);
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            try (OutputStream s = new BufferedOutputStream(new FileOutputStream(output))) {
                marshal(object, OutputFactory.createXMLEventWriter(s, getEncoding()), version, linkHandler);
            }
        } catch (IOException | XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            // Marshalling to the default GML version.
            final Context context = begin(linkHandler);
            try {
                marshaller.marshal(object, output);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the marshalling to the wrapped marshaller.
     */
    @Override
    public void marshal(Object object, final Writer output) throws JAXBException {
        object = toImplementation(object);                          // Must be call before getTransformVersion()
        final var linkHandler = ExternalLinkHandler.forStream(output);
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            marshal(object, OutputFactory.createXMLEventWriter(output), version, linkHandler);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            // Marshalling to the default GML version.
            final Context context = begin(linkHandler);
            try {
                marshaller.marshal(object, output);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the marshalling to the wrapped marshaller.
     */
    @Override
    public void marshal(Object object, final ContentHandler output) throws JAXBException {
        object = toImplementation(object);                          // Must be call before getTransformVersion()
        final ExternalLinkHandler linkHandler = null;               // We don't know how to get the base URI.
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            marshal(object, OutputFactory.createXMLEventWriter(output), version, linkHandler);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            // Marshalling to the default GML version.
            final Context context = begin(linkHandler);
            try {
                marshaller.marshal(object, output);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the marshalling to the wrapped marshaller.
     */
    @Override
    public void marshal(Object object, final Node output) throws JAXBException {
        object = toImplementation(object);                          // Must be call before getTransformVersion()
        final var linkHandler = new ExternalLinkHandler(output.getBaseURI());
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            marshal(object, OutputFactory.createXMLEventWriter(output), version, linkHandler);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            // Marshalling to the default GML version.
            final Context context = begin(linkHandler);
            try {
                marshaller.marshal(object, output);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the marshalling to the wrapped marshaller.
     */
    @Override
    public void marshal(Object object, final XMLStreamWriter output) throws JAXBException {
        object = toImplementation(object);                          // Must be call before getTransformVersion()
        final ExternalLinkHandler linkHandler = null;               // We don't know how to get the base URI.
        final TransformVersion version = getTransformVersion();
        if (version != null) try {
            marshal(object, OutputFactory.createXMLEventWriter(output), version, linkHandler);
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        } else {
            // Marshalling to the default GML version.
            final Context context = begin(linkHandler);
            try {
                marshaller.marshal(object, output);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates the marshalling to the wrapped marshaller.
     */
    @Override
    public void marshal(Object object, XMLEventWriter output) throws JAXBException {
        object = toImplementation(object);                          // Must be call before getTransformVersion()
        final ExternalLinkHandler linkHandler = null;               // We don't know how to get the base URI.
        final TransformVersion version = getTransformVersion();
        if (version != null) {
            output = new TransformingWriter(output, version);
        }
        final Context context = begin(linkHandler);
        try {
            marshaller.marshal(object, output);
        } finally {
            context.finish();
        }
    }

    /**
     * Delegates the marshalling to the wrapped marshaller.
     */
    @Override
    public Node getNode(Object object) throws JAXBException {
        object = toImplementation(object);                          // Must be call before getTransformVersion()
        final ExternalLinkHandler linkHandler = null;               // We don't know how to get the base URI.
        final TransformVersion version = getTransformVersion();
        if (version != null) {
            // This exception is thrown by jakarta.xml.bind.helpers.AbstractMarshallerImpl anyway.
            throw new UnsupportedOperationException();
        } else {
            final Context context = begin(linkHandler);
            try {
                return marshaller.getNode(object);
            } finally {
                context.finish();
            }
        }
    }

    /**
     * Delegates to the wrapped marshaller. This method is invoked by the parent
     * class if the given name was not one of the {@link XML} constants.
     */
    @Override
    void setStandardProperty(final String name, final Object value) throws PropertyException {
        marshaller.setProperty(name, value);
    }

    /**
     * Delegates to the wrapped marshaller. This method is invoked by the parent
     * class if the given name was not one of the {@link XML} constants.
     */
    @Override
    Object getStandardProperty(final String name) throws PropertyException {
        return marshaller.getProperty(name);
    }

    /**
     * Delegates to the wrapped marshaller. The initial state will be saved
     * if it was not already done, for future restoration by {@link #reset(Pooled)}.
     */
    @Override
    public <A extends XmlAdapter<?,?>> void setAdapter(final Class<A> type, final A adapter) {
        super.setAdapter(type, adapter);
        marshaller.setAdapter(type, adapter);
    }

    /**
     * Delegates to the wrapped marshaller.
     */
    @Override
    public <A extends XmlAdapter<?,?>> A getAdapter(final Class<A> type) {
        return marshaller.getAdapter(type);
    }

    /**
     * Delegates to the wrapped marshaller. The initial state will be saved
     * if it was not already done, for future restoration by {@link #reset(Pooled)}.
     */
    @Override
    public void setSchema(final Schema schema) {
        super.setSchema(schema);
        marshaller.setSchema(schema);
    }

    /**
     * Delegates to the wrapped marshaller.
     */
    @Override
    public Schema getSchema() {
        return marshaller.getSchema();
    }

    /**
     * Delegates to the wrapped marshaller. The initial state will be saved
     * if it was not already done, for future restoration by {@link #reset(Pooled)}.
     */
    @Override
    public void setEventHandler(final ValidationEventHandler handler) throws JAXBException {
        super.setEventHandler(handler);
        marshaller.setEventHandler(handler);
    }

    /**
     * Delegates to the wrapped marshaller.
     */
    @Override
    public ValidationEventHandler getEventHandler() throws JAXBException {
        return marshaller.getEventHandler();
    }

    /**
     * Delegates to the wrapped marshaller. The initial state will be saved
     * if it was not already done, for future restoration by {@link #reset(Pooled)}.
     */
    @Override
    public void setAttachmentMarshaller(final AttachmentMarshaller am) {
        if (!isPropertySaved(AttachmentMarshaller.class)) {
            saveProperty(AttachmentMarshaller.class, marshaller.getAttachmentMarshaller());
        }
        marshaller.setAttachmentMarshaller(am);
    }

    /**
     * Delegates to the wrapped marshaller.
     */
    @Override
    public AttachmentMarshaller getAttachmentMarshaller() {
        return marshaller.getAttachmentMarshaller();
    }

    /**
     * Delegates to the wrapped marshaller. The initial state will be saved
     * if it was not already done, for future restoration by {@link #reset(Pooled)}.
     */
    @Override
    public void setListener(final Listener listener) {
        if (!isPropertySaved(Listener.class)) {
            saveProperty(Listener.class, marshaller.getListener());
        }
        marshaller.setListener(listener);
    }

    /**
     * Delegates to the wrapped marshaller.
     */
    @Override
    public Listener getListener() {
        return marshaller.getListener();
    }
}
