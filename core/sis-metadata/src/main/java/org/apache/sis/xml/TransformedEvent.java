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

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Namespace;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;


/**
 * Base class of events that are wrappers over the events emitted during the reading or writing of an XML document.
 * Those wrappers are used for changing the namespace and sometime the name of XML elements or attributes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class TransformedEvent<E extends XMLEvent> implements XMLEvent {
    /**
     * The event to be exported in a different namespace.
     */
    final E event;

    /**
     * Exported name of the attribute or element. Will often (but not necessarily) have
     * the same local part than {@code event.getName()} but a different namespace.
     */
    final QName name;

    /**
     * Exports a new event.
     *
     * @param  event  the event to be exported in a different namespace.
     * @param  name   the exported name of the attribute or element.
     */
    TransformedEvent(final E event, final QName name) {
        this.event = event;
        this.name  = name;
    }

    @Override public boolean      isStartElement()          {return false;}
    @Override public boolean      isAttribute()             {return false;}
    @Override public boolean      isNamespace()             {return false;}
    @Override public boolean      isEndElement()            {return false;}
    @Override public boolean      isEntityReference()       {return false;}
    @Override public boolean      isProcessingInstruction() {return false;}
    @Override public boolean      isCharacters()            {return false;}
    @Override public boolean      isStartDocument()         {return false;}
    @Override public boolean      isEndDocument()           {return false;}
    @Override public StartElement asStartElement()          {throw new ClassCastException();}
    @Override public EndElement   asEndElement()            {throw new ClassCastException();}
    @Override public Characters   asCharacters()            {throw new ClassCastException();}
    @Override public Location     getLocation()             {return event.getLocation();}
    @Override public QName        getSchemaType()           {return event.getSchemaType();}
    public    final  QName        getName()                 {return name;}

    /**
     * Appends the name to the given output.
     * This is a convenience method for {@link #write(Appendable)} implementations.
     */
    final Appendable name(final Appendable out) throws IOException {
        final String prefix = name.getPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            out.append(prefix).append(':');
        }
        return out.append(name.getLocalPart());
    }

    /**
     * Implementation of {@link #writeAsEncodedUnicode(Writer)} and {@link #toString()}.
     */
    abstract void write(Appendable out) throws IOException;

    /**
     * Writes the event as per the XML 1.0 without indentation or whitespace.
     * This implementation delegates to {@link #write(Appendable)}.
     */
    @Override
    public final void writeAsEncodedUnicode(final Writer writer) throws XMLStreamException {
        try {
            write(writer);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    /**
     * Returns the event as per the XML 1.0 without indentation or whitespace.
     * This implementation delegates to {@link #write(Appendable)}.
     */
    @Override
    public final String toString() {
        final StringBuilder out = new StringBuilder();
        try {
            write(out);
        } catch (IOException e) {       // Should never happen since we write to a StringBuilder.
            return e.toString();
        }
        return out.toString();
    }

    /**
     * Wrapper over a namespace emitted during the reading or writing of an XML document.
     * This wrapper is used for changing the namespace URI. The wrapped {@link #event}
     * should be a {@link Namespace}, but this class accepts also the {@link Attribute}
     * super-type for allowing the {@link Type} attribute to create synthetic namespaces.
     */
    static final class NS extends TransformedEvent<Attribute> implements Namespace {
        /** The URI of the namespace. */
        private final String namespaceURI;

        /** Wraps the given event with a different prefix and URI. */
        NS(final Attribute event, final String prefix, final String namespaceURI) {
            super(event, new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix, XMLConstants.XMLNS_ATTRIBUTE));
            this.namespaceURI = namespaceURI;
        }

        @Override public boolean   isNamespace()                   {return true;}
        @Override public int       getEventType()                  {return NAMESPACE;}
        @Override public String    getNamespaceURI()               {return namespaceURI;}
        @Override public String    getValue()                      {return namespaceURI;}
        @Override public String    getDTDType()                    {return event.getDTDType();}
        @Override public boolean   isSpecified()                   {return event instanceof Namespace && event.isSpecified();}
        @Override public String    getPrefix()                     {return (name != null) ?  name.getLocalPart() : null;}
        @Override public boolean   isDefaultNamespaceDeclaration() {return (name != null) && name.getLocalPart().isEmpty();}
        @Override void write(final Appendable out) throws IOException {
            name(out).append("=\"").append(namespaceURI).append('"');
        }
    }

    /**
     * Wrapper over an attribute emitted during the reading or writing of an XML document.
     * This wrapper is used for changing the namespace of the attribute.
     */
    static class Attr extends TransformedEvent<Attribute> implements Attribute {
        /** Wraps the given event with a different name. */
        Attr(final Attribute event, final QName name) {
            super(event, name);
        }

        /** Cast or wrap the given attribute to an {@code Attr} instance. */
        static Attr castOrWrap(final Attribute a) {
            if (a instanceof Attr) return (Attr) a;
            else return new Attr(a, a.getName());
        }

        @Override public boolean   isAttribute()     {return true;}
        @Override public int       getEventType()    {return ATTRIBUTE;}
        @Override public String    getValue()        {return event.getValue();}
        @Override public String    getDTDType()      {return event.getDTDType();}
        @Override public boolean   isSpecified()     {return event.isSpecified();}
        @Override void write(final Appendable out) throws IOException {
            name(out).append("=\"").append(getValue()).append('"');
        }
    }

    /**
     * The {@code "xsi:type"} attribute. Contrarily to other attributes, the name is unchanged compared
     * to the original attribute; instead the value is different. Even in unchanged, the {@link QName}
     * is specified at construction time because it is required by the parent class.
     */
    static final class Type extends Attr {
        /** The attribute value. */
        private final String value;

        /** If the value requires a new prefix to be bound, the namespace declaration for it. */
        Namespace namespace;

        /** Wraps the given event with a different value. */
        Type(final Attribute event, final QName name, final String value) {
            super(event, name);
            this.value = value;
        }

        /** Returns the {@code "xsi:type"} attribute value. */
        @Override public String getValue() {return value;}
    }

    /**
     * Wrapper over an element emitted during the reading or writing of an XML document.
     * This wrapper is used for changing the namespace and sometime the name of the element.
     */
    static final class End extends TransformedEvent<EndElement> implements EndElement {
        /** The namespaces, may or may not be the same than the wrapped event. */
        private final List<Namespace> namespaces;

        /** Wraps the given event with potentially different name and namespaces. */
        End(final EndElement event, final QName name, final List<Namespace> namespaces) {
            super(event, name);
            this.namespaces = namespaces;
        }

        @Override public boolean             isEndElement()  {return true;}
        @Override public EndElement          asEndElement()  {return this;}
        @Override public int                 getEventType()  {return END_ELEMENT;}
        @Override public Iterator<Namespace> getNamespaces() {return namespaces.iterator();}
        @Override void write(final Appendable out) throws IOException {
            name(out.append("</")).append('>');
        }
    }

    /**
     * Wrapper over an element emitted during the reading or writing of an XML document.
     * This wrapper is used for changing the namespace and sometime the name of the element.
     * The attributes may also be modified.
     */
    static class Start extends TransformedEvent<StartElement> implements StartElement {
        /** The namespaces, may or may not be the same than the wrapped event. */
        private final List<Namespace> namespaces;

        /** The attributes, may or may not be the same than the wrapped event. */
        private final List<Attribute> attributes;

        /** The version to export, used for wrapping namespace context. */
        final TransformVersion version;

        /** Wraps the given event with potentially different name, namespaces and attributes. */
        Start(StartElement event, QName name, List<Namespace> namespaces, List<Attribute> attributes, TransformVersion version) {
            super(event, name);
            this.namespaces = namespaces;
            this.attributes = attributes;
            this.version    = version;
        }

        @Override public final boolean             isStartElement() {return true;}
        @Override public final StartElement        asStartElement() {return this;}
        @Override public final int                 getEventType()   {return START_ELEMENT;}
        @Override public final Iterator<Namespace> getNamespaces()  {return namespaces.iterator();}
        @Override public final Iterator<Attribute> getAttributes()  {return attributes.iterator();}

        /**
         * Returns the attribute referred to by the given name, or {@code null} if none.
         * Current implementation is okay on the assumption that there is few attributes.
         */
        @Override
        public final Attribute getAttributeByName(final QName name) {
            for (final Attribute attr : attributes) {
                if (name.equals(attr.getName())) {
                    return attr;
                }
            }
            return null;
        }

        /**
         * Gets the URI used by JAXB annotations for the given prefix used in the XML document.
         * Returns {@code null} if no unique URI can be provided for the given prefix.
         *
         * <div class="note"><b>Example:</b>
         * the {@code "gmd"} prefix from legacy ISO 19139:2007 standard can map to the
         * {@code "http://standards.iso.org/iso/19115/-3/mdb/1.0"}, {@code "…/cit/1.0"}
         * and other namespaces in ISO 19115-3:2016. Because of this ambiguity,
         * this method returns {@code null}.</div>
         *
         * <p>At unmarshalling time, events are created by an arbitrary {@link javax.xml.stream.XMLEventReader}
         * with namespaces used in the XML document. {@link TransformingReader} wraps those events using this
         * class for converting the XML namespaces to the namespaces used by JAXB annotations.</p>
         *
         * <p>At marshalling time, events are created by JAXB using namespaces used in JAXB annotations.
         * {@link TransformingWriter} wraps those events for converting those namespaces to the ones used
         * in the XML document. This is the opposite than the work performed by this default implementation
         * and must be handled by a {@code Start} subclass.</p>
         */
        @Override
        public String getNamespaceURI(final String prefix) {
            return version.importNS(event.getNamespaceURI(prefix));
        }

        /**
         * Returns a context mapping prefixes used in XML document to namespaces used in JAXB annotations.
         * The {@code TransformingNamespaces.Inverse.getNamespaceURI(String)} method in that context shall do
         * the same work than {@link #getNamespaceURI(String)} in this event.
         */
        @Override
        public NamespaceContext getNamespaceContext() {
            return TransformingNamespaces.asJAXB(event.getNamespaceContext(), version);
        }

        /**
         * Writes the event as per the XML 1.0 without indentation or whitespace.
         */
        @Override
        final void write(final Appendable out) throws IOException {
            name(out.append('<'));
            final int n = attributes.size();
            for (int i=0; i<n; i++) {
                out.append(' ');
                Attr.castOrWrap(attributes.get(i)).write(out);
            }
            out.append('>');
        }
    }
}
