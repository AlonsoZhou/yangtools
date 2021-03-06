/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.model.repo.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

/**
 * Utility {@link YinXmlSchemaSource} exposing a W3C {@link DOMSource} representation of YIN model.
 */
public abstract class YinDomSchemaSource implements YinXmlSchemaSource {
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    YinDomSchemaSource() {
        // Prevent outside instantiation
    }

    /**
     * Create a new {@link YinDomSchemaSource} using an identifier and a source.
     *
     * @param identifier Schema source identifier
     * @param source W3C DOM source
     * @return A new {@link YinDomSchemaSource} instance.
     */
    @Nonnull public static YinDomSchemaSource create(@Nonnull final SourceIdentifier identifier, @Nonnull final DOMSource source) {
        return new Simple(identifier, source);
    }

    private static YinDomSchemaSource castSchemaSource(final YinXmlSchemaSource xmlSchemaSource) {
        if (xmlSchemaSource instanceof YinDomSchemaSource) {
            return (YinDomSchemaSource) xmlSchemaSource;
        }

        final Source source = xmlSchemaSource.getSource();
        if (source instanceof DOMSource) {
            return create(xmlSchemaSource.getIdentifier(), (DOMSource) source);
        }

        return null;
    }

    static DOMSource transformSource(final Source source) throws TransformerException {
        final DOMResult result = new DOMResult();
        TRANSFORMER_FACTORY.newTransformer().transform(source, result);

        return new DOMSource(result.getNode(), result.getSystemId());
    }

    /**
     * Create a {@link YinDomSchemaSource} from a {@link YinXmlSchemaSource}. If the argument is already a
     * YinDomSchemaSource, this method returns the same instance. The source will be translated on first access,
     * at which point an {@link IllegalStateException} may be raised.
     *
     * @param xmlSchemaSource Backing schema source
     * @return A {@link YinDomSchemaSource} instance
     */
    @Nonnull public static YinDomSchemaSource lazyTransform(final YinXmlSchemaSource xmlSchemaSource) {
        final YinDomSchemaSource cast = castSchemaSource(xmlSchemaSource);
        return cast != null ? cast : new Transforming(xmlSchemaSource);
    }

    /**
     * Create a {@link YinDomSchemaSource} from a {@link YinXmlSchemaSource}. If the argument is already a
     * YinDomSchemaSource, this method returns the same instance. The source will be translated immediately.
     *
     * @param xmlSchemaSource Backing schema source
     * @return A {@link YinDomSchemaSource} instance
     * @throws TransformerException when the provided source fails to transform
     */
    @Nonnull public static YinDomSchemaSource transform(final YinXmlSchemaSource xmlSchemaSource) throws TransformerException {
        final YinDomSchemaSource cast = castSchemaSource(xmlSchemaSource);
        return cast != null ? cast :
            create(xmlSchemaSource.getIdentifier(), transformSource(xmlSchemaSource.getSource()));
    }

    @Override
    public final Class<? extends YinXmlSchemaSource> getType() {
        return YinDomSchemaSource.class;
    }

    @Override
    @Nonnull public abstract DOMSource getSource();

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this).add("identifier", getIdentifier())).toString();
    }

    /**
     * Add subclass-specific attributes to the output {@link #toString()} output. Since
     * subclasses are prevented from overriding {@link #toString()} for consistency
     * reasons, they can add their specific attributes to the resulting string by attaching
     * attributes to the supplied {@link ToStringHelper}.
     *
     * @param toStringHelper ToStringHelper onto the attributes can be added
     * @return ToStringHelper supplied as input argument.
     */
    protected abstract ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper);

    private static final class Simple extends YinDomSchemaSource {
        private final SourceIdentifier identifier;
        private final DOMSource source;

        Simple(@Nonnull final SourceIdentifier identifier, @Nonnull final DOMSource source) {
            this.identifier = Preconditions.checkNotNull(identifier);
            this.source = Preconditions.checkNotNull(source);
        }

        @Override
        public DOMSource getSource() {
            return source;
        }

        @Override
        public SourceIdentifier getIdentifier() {
            return identifier;
        }

        @Override
        protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
            return toStringHelper.add("source", source);
        }
    }

    private static final class Transforming extends YinDomSchemaSource {
        private final YinXmlSchemaSource xmlSchemaSource;
        private volatile DOMSource source;

        Transforming(final YinXmlSchemaSource xmlSchemaSource) {
            this.xmlSchemaSource = Preconditions.checkNotNull(xmlSchemaSource);
        }

        @Override
        public DOMSource getSource() {
            DOMSource ret = source;
            if (ret == null) {
                synchronized (this) {
                    ret = source;
                    if (ret == null) {
                        try {
                            ret = transformSource(xmlSchemaSource.getSource());
                        } catch (TransformerException e) {
                            throw new IllegalStateException("Failed to transform schema source " + xmlSchemaSource, e);
                        }
                        source = ret;
                    }
                }
            }

            return ret;
        }

        @Override
        public SourceIdentifier getIdentifier() {
            return xmlSchemaSource.getIdentifier();
        }

        @Override
        protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
            return toStringHelper.add("xmlSchemaSource", xmlSchemaSource);
        }
    }
}
