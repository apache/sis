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
package org.apache.sis.internal.doclet;

import com.sun.javadoc.RootDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.SourcePosition;


/**
 * Delegates all method calls to the original {@code RootDoc} given at construction time,
 * except the options which are replaced by the array given at construction time.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
final class FilteredRootDoc implements RootDoc {
    /**
     * The original object where to delegate all (except options) method calls.
     */
    private final RootDoc doc;

    /**
     * The options to be returned by {@link #options()}.
     */
    private final String[][] options;

    /**
     * Creates a new instance which will delegate all method calls to the given object,
     * except for the given options.
     */
    FilteredRootDoc(final RootDoc doc, final String[][] options) {
        this.doc = doc;
        this.options = options;
    }

    @Override public String[][]     options()                   {return options.clone();}
    @Override public PackageDoc[]   specifiedPackages()         {return doc.specifiedPackages();}
    @Override public ClassDoc[]     specifiedClasses()          {return doc.specifiedClasses();}
    @Override public ClassDoc[]     classes()                   {return doc.classes();}
    @Override public PackageDoc     packageNamed(String n)      {return doc.packageNamed(n);}
    @Override public ClassDoc       classNamed(String n)        {return doc.classNamed(n);}
    @Override public String         commentText()               {return doc.commentText();}
    @Override public Tag[]          tags()                      {return doc.tags();}
    @Override public Tag[]          tags(String n)              {return doc.tags(n);}
    @Override public SeeTag[]       seeTags()                   {return doc.seeTags();}
    @Override public Tag[]          inlineTags()                {return doc.inlineTags();}
    @Override public Tag[]          firstSentenceTags()         {return doc.firstSentenceTags();}
    @Override public String         getRawCommentText()         {return doc.getRawCommentText();}
    @Override public void           setRawCommentText(String n) {       doc.setRawCommentText(n);}
    @Override public String         name()                      {return doc.name();}
    @Override public int            compareTo(Object o)         {return doc.compareTo(o);}
    @Override public boolean        isField()                   {return doc.isField();}
    @Override public boolean        isEnumConstant()            {return doc.isEnumConstant();}
    @Override public boolean        isConstructor()             {return doc.isConstructor();}
    @Override public boolean        isMethod()                  {return doc.isMethod();}
    @Override public boolean        isAnnotationTypeElement()   {return doc.isAnnotationTypeElement();}
    @Override public boolean        isInterface()               {return doc.isInterface();}
    @Override public boolean        isException()               {return doc.isException();}
    @Override public boolean        isError()                   {return doc.isError();}
    @Override public boolean        isEnum()                    {return doc.isEnum();}
    @Override public boolean        isAnnotationType()          {return doc.isAnnotationType();}
    @Override public boolean        isOrdinaryClass()           {return doc.isOrdinaryClass();}
    @Override public boolean        isClass()                   {return doc.isClass();}
    @Override public boolean        isIncluded()                {return doc.isIncluded();}
    @Override public SourcePosition position()                  {return doc.position();}

    @Override public void printError  (                    String msg)  {doc.printError(msg);}
    @Override public void printError  (SourcePosition pos, String msg)  {doc.printError(pos, msg);}
    @Override public void printWarning(                    String msg)  {doc.printWarning(msg);}
    @Override public void printWarning(SourcePosition pos, String msg)  {doc.printWarning(pos, msg);}
    @Override public void printNotice (                    String msg)  {doc.printNotice(msg);}
    @Override public void printNotice (SourcePosition pos, String msg)  {doc.printNotice(pos, msg);}
}
