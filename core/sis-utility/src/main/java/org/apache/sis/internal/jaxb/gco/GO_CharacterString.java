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
package org.apache.sis.internal.jaxb.gco;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import org.w3c.dom.Element;
import org.opengis.util.CodeList;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gmx.Anchor;
import org.apache.sis.internal.jaxb.gmx.FileName;
import org.apache.sis.internal.jaxb.gmx.MimeFileType;
import org.apache.sis.internal.jaxb.gmd.CodeListUID;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Messages;


/**
 * JAXB wrapper for string value in a {@code <gco:CharacterString>}, {@code <gmx:Anchor>},
 * {@code <gmx:FileName>} or {@code <gmx:MimeFileType>} element, for ISO-19139 compliance.
 *
 * <p>{@code FileName} and {@code MimeFileType} are possible substitutions for {@code CharacterString}.
 * They make sense only in {@link org.apache.sis.metadata.iso.identification.DefaultBrowseGraphic} or
 * other classes using URI, but the XML schema does not prevent their usage in place of other strings.
 * Consequently we unconditionally accept {@code FileName} and {@code MimeFileType} at unmarshalling time.
 * However marshalling will use the appropriate element for the kind of property to marshal.</p>
 *
 * <p>{@code <gco:CharacterString>} can also be replaced by {@link org.opengis.util.CodeList} or some
 * {@link java.lang.Enum} instances. See {@link Types} javadoc for an example.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.internal.jaxb.gmd.PT_FreeText
 */
@XmlType(name = "CharacterString_PropertyType")
@XmlSeeAlso({
    org.apache.sis.internal.jaxb.gmd.PT_FreeText.class,
    org.apache.sis.internal.jaxb.gmd.LanguageCode.class,
    org.apache.sis.internal.jaxb.gmd.Country.class
})
public class GO_CharacterString {
    /*
     * Numerical values below are ordered: if two or more values are defined (thoerically not legal,
     * but we try to be robust), the value associated to the highest constant has precedence.
     */
    /**
     * Value assigned to {@link #type} if the character string
     * shall be marshalled as a {@code <gmx:MimeFileType>} element.
     */
    public static final byte MIME_TYPE = 1;

    /**
     * Value assigned to {@link #type} if the character string
     * shall be marshalled as a {@code <gmx:FileName>} element.
     */
    public static final byte FILENAME = 2;

    /**
     * Value assigned to {@link #type} if the current {@link #text}
     * has been found in a {@code <gmx:Anchor>} element.
     */
    private static final byte ANCHOR = 3;

    /**
     * Value assigned to {@link #type} if the current {@link #text}
     * has been found in an enumeration or code list element.
     */
    private static final byte ENUM = 4;

    /**
     * The XML element names for each possible {@link #type} values.
     * Used for formatting error messages.
     */
    private static String nameOf(final byte type) {
        switch (type) {
            case 0:         return "CharacterString";
            case MIME_TYPE: return "MimeFileType";
            case FILENAME:  return "FileName";
            case ANCHOR:    return "Anchor";
            case ENUM:      return "ControlledVocabulary";
            default:        throw new AssertionError(type);
        }
    }

    /**
     * The text, code list or anchor value, or {@code null} if none.
     * The following types need to be handled in a special way:
     *
     * <ul>
     *   <li>{@link Anchor}</li>
     *   <li>Instances for which {@link Types#forCodeTitle(CharSequence)} returns a non-null value.</li>
     * </ul>
     */
    private CharSequence text;

    /**
     * 0 if the text shall be marshalled as a {@code <gco:CharacterString>},
     * or one of the static constants in this class otherwise.
     *
     * @see #FILENAME
     * @see #MIME_TYPE
     */
    public byte type;

    /**
     * Empty constructor for JAXB and subclasses.
     */
    protected GO_CharacterString() {
    }

    /**
     * Builds a wrapper for the given text.
     *
     * @param text The string to marshal, or {@code null} if none.
     */
    protected GO_CharacterString(final CharSequence text) {
        this.text = text;
        if (text instanceof Anchor) {
            type = ANCHOR;
        } else if (Types.forCodeTitle(text) != null) {
            type = ENUM;
        }
    }

    /**
     * Sets the {@link #text} field to the given value.
     * If the given value overwrites a previous one, a warning is emitted.
     *
     * @param value    The value to set.
     * @param property 0 or one of the {@link #MIME_TYPE}, {@link #FILENAME} or {@link #ANCHOR} constants.
     */
    private void setText(CharSequence value, byte property) {
        value = CharSequences.trimWhitespaces(value);
        if (value != null && value.length() != 0) {
            if (text != null && !value.equals(text)) {
                /*
                 * The given value overwrite a previous one. Determine which value will be discarded
                 * using the 'type' value as a criterion, then emit a warning.
                 */
                byte discarded = type;
                boolean noset = false;
                if (discarded > property) {
                    discarded = property;
                    property  = type;
                    noset     = true;
                }
                Context.warningOccured(Context.current(), getClass(), "setText", Messages.class,
                        Messages.Keys.DiscardedExclusiveProperty_2, nameOf(discarded), nameOf(property));
                if (noset) {
                    return;
                }
            }
            text = value;
            type = property;
        }
    }

    /**
     * Returns the text in a {@code <gco:CharacterString>}, {@code <gmx:FileName>} or {@code <gmx:MimeFileType>}
     * element, or {@code null} if none. This method does not return anything for {@code Enum} or {@code CodeList}
     * instances, as the later are handled by {@link #getCodeList()}.
     *
     * <p>This method is invoked by JAXB at marshalling time and should not need to be invoked directly.</p>
     */
    @XmlElements({
        @XmlElement(type = String.class,       name = "CharacterString"),
        @XmlElement(type = Anchor.class,       name = "Anchor",       namespace = Namespaces.GMX),
        @XmlElement(type = FileName.class,     name = "FileName",     namespace = Namespaces.GMX),
        @XmlElement(type = MimeFileType.class, name = "MimeFileType", namespace = Namespaces.GMX)
    })
    private Object getValue() {
        switch (type) {
            case 0:         return StringAdapter.toString(text);
            case FILENAME:  return new FileName(text.toString());
            case MIME_TYPE: return new MimeFileType(text.toString());
            case ANCHOR:    return text;    // Shall be an instance of Anchor.
            default:        return null;    // CodeList or Enum.
        }
    }

    /**
     * Sets the {@code <gco:CharacterString>}, {@code <gmx:FileName>} or {@code <gmx:MimeFileType>} value.
     *
     * <p>This method is invoked by JAXB at unmarshalling time and should not need to be invoked directly.</p>
     */
    private void setValue(final Object value) {
        if (value instanceof Anchor) {
            setText((Anchor) value, ANCHOR);
        } else if (value instanceof FileName) {
            setText(value.toString(), FILENAME);
        } else if (value instanceof MimeFileType) {
            setText(value.toString(), MIME_TYPE);
        } else {
            setText((CharSequence) value, (byte) 0);
        }
    }

    /**
     * Returns the code list wrapped in a JAXB element, or {@code null} if the {@link #text} is not a wrapper for
     * a code list. Only one of {@link #getValue()} and {@code getCodeList()} should return a non-null value.
     *
     * <div class="note"><b>Note:</b>
     * we have to rely on a somewhat complicated mechanism because the code lists implementations in GeoAPI
     * do not hae JAXB annotations. If those annotations are added in a future GeoAPI implementation, then
     * we could replace this mechanism by a simple property annotated with {@code XmlElementRef}.</div>
     *
     * @since 0.7
     */
    @XmlAnyElement
    @Workaround(library = "GeoAPI", version = "3.0")
    private Object getCodeList() {
        if (type != ENUM) {
            return null;
        }
        final CodeList<?> code = Types.forCodeTitle(text);
        final String name = Types.getListName(code);
        final String namespace;
        /*
         * The namespace is usually GMD, but we also have some other namespaces link GMI.
         * The real namespace is declared in the @XmlElement annotation of the getElement
         * method in the JAXB adapter. We could use reflection, but we do not in order to
         * avoid potential class loading issue and also because not all CodeList are in the
         * same package.
         */
        if (name.startsWith("MD_") || name.startsWith("CI_") || name.startsWith("DS_")) {
            namespace = Namespaces.GMD;
        } else if (name.startsWith("MI_")) {
            namespace = Namespaces.GMI;
        } else if (name.startsWith("SV_") || name.equals("DCPList")) {
            namespace = Namespaces.SRV;
        } else if (name.startsWith("CS_") || name.startsWith("CD_") || name.startsWith("SC_")) {
            namespace = Namespaces.GML;
        } else {
            namespace = XMLConstants.NULL_NS_URI;
        }
        return new JAXBElement<CodeListUID>(new QName(namespace, name), CodeListUID.class,
                new CodeListUID(Context.current(), code));
    }

    /**
     * Invoked by JAXB for any XML element which is not a {@code <gco:CharacterString>}, {@code <gmx:FileName>}
     * or {@code <gmx:MimeFileType>}. This method presumes that the element name is the CodeList standard name.
     * If not, the element will be ignored.
     */
    @SuppressWarnings("unchecked")
    private void setCodeList(final Object value) {
        final Element e = (Element) value;
        if (e.getNodeType() == Element.ELEMENT_NODE) {
            final Class<?> ct = Types.forStandardName(e.getLocalName());
            if (ct != null && CodeList.class.isAssignableFrom(ct)) {
                final String attribute = e.getAttribute("codeListValue");
                if (!attribute.isEmpty()) {
                    text = Types.getCodeTitle(Types.forCodeName((Class) ct, attribute, true));
                    type = ENUM;
                    return;
                }
            }
            Context.warningOccured(Context.current(), GO_CharacterString.class, "setCodeList",
                    Errors.class, Errors.Keys.UnknownType_1, e.getNodeName());
        }
    }

    /**
     * Returns the content of this {@code <gco:CharacterString>} as a {@code String},
     * an {@code InternationalString} or an {@code Anchor}. This method is overridden
     * by {@code PT_FreeText} in order to handle the international string case.
     *
     * @return The character sequence for this {@code <gco:CharacterString>}.
     */
    protected CharSequence toCharSequence() {
        final CharSequence text = CharSequences.trimWhitespaces(this.text);
        if (text != null && (text.length() != 0 || text instanceof Anchor)) {       // Anchor may contain attributes.
            return text;
        }
        return null;
    }

    /**
     * Returns the text as a string, or {@code null} if none.
     * The null value is expected by various {@code PT_FreeText}.
     *
     * <div class="note"><b>Note:</b>
     * Returning {@code null} is unusual and not a recommended practice.
     * But precedents exist (for example {@link javax.swing.tree.DefaultMutableTreeNode})
     * and this class is not for public usage.</div>
     *
     * @return The text as a string (may be null).
     */
    @Override
    public final String toString() {
        final CharSequence text = this.text;
        return (text != null) ? text.toString() : null;     // We really want to return null here.
    }
}
