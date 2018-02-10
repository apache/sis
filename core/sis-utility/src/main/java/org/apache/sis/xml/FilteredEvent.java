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
abstract class FilteredEvent<E extends XMLEvent> implements XMLEvent {
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
    FilteredEvent(final E event, final QName name) {
        this.event  = event;
        this.name   = name;
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
     * This wrapper is used for changing the namespace URI.
     */
    static final class NS extends FilteredEvent<Namespace> implements Namespace {
        /** The URI of the namespace. */
        private final String namespaceURI;

        /** Wraps the given event with a different prefix and URI. */
        NS(final Namespace event, final String prefix, final String namespaceURI) {
            super(event, new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix, XMLConstants.XMLNS_ATTRIBUTE));
            this.namespaceURI = namespaceURI;
        }

        @Override public boolean   isNamespace()                   {return true;}
        @Override public int       getEventType()                  {return NAMESPACE;}
        @Override public String    getNamespaceURI()               {return namespaceURI;}
        @Override public String    getValue()                      {return namespaceURI;}
        @Override public String    getDTDType()                    {return event.getDTDType();}
        @Override public boolean   isSpecified()                   {return event.isSpecified();}
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
    static final class Attr extends FilteredEvent<Attribute> implements Attribute {
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
            name(out).append("=\"").append(event.getValue()).append('"');
        }
    }

    /**
     * Wrapper over an element emitted during the reading or writing of an XML document.
     * This wrapper is used for changing the namespace and sometime the name of the element.
     */
    static final class End extends FilteredEvent<EndElement> implements EndElement {
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
    static final class Start extends FilteredEvent<StartElement> implements StartElement {
        /** The namespaces, may or may not be the same than the wrapped event. */
        private final List<Namespace> namespaces;

        /** The attributes, may or may not be the same than the wrapped event. */
        private final List<Attribute> attributes;

        /** The version to export, used for wrapping namespace context. */
        private final FilterVersion version;

        /** Wraps the given event with potentially different name, namespaces and attributes. */
        Start(StartElement event, QName name, List<Namespace> namespaces, List<Attribute> attributes, FilterVersion version) {
            super(event, name);
            this.namespaces = namespaces;
            this.attributes = attributes;
            this.version    = version;
        }

        @Override public boolean             isStartElement() {return true;}
        @Override public StartElement        asStartElement() {return this;}
        @Override public int                 getEventType()   {return START_ELEMENT;}
        @Override public Iterator<Namespace> getNamespaces()  {return namespaces.iterator();}
        @Override public Iterator<Attribute> getAttributes()  {return attributes.iterator();}

        /**
         * Returns the attribute referred to by the given name, or {@code null} if none.
         * Current implementation is okay on the assumption that there is few attributes.
         */
        @Override
        public Attribute getAttributeByName(final QName name) {
            for (final Attribute attr : attributes) {
                if (name.equals(attr.getName())) {
                    return attr;
                }
            }
            return null;
        }

        /** Gets a read-only namespace context. */
        @Override public NamespaceContext getNamespaceContext() {
            final NamespaceContext context = event.getNamespaceContext();
            return (context != null) ? new FilteredNamespaces(context, version, false) : null;
        }

        /** Gets the value that the prefix is bound to in the context of this element. */
        @Override public String getNamespaceURI(final String prefix) {
            final NamespaceContext context = event.getNamespaceContext();
            if (context != null) {
                final String uri = context.getNamespaceURI(prefix);
                return version.exports.getOrDefault(uri, uri);
            }
            return null;
        }

        /** Writes the event as per the XML 1.0 without indentation or whitespace. */
        @Override void write(final Appendable out) throws IOException {
            name(out.append('<'));
            final int n = attributes.size();
            for (int i=0; i<n; i++) {
                if (i != 0) out.append(' ');
                Attr.castOrWrap(attributes.get(i)).write(out);
            }
            out.append('>');
        }
    }
}
