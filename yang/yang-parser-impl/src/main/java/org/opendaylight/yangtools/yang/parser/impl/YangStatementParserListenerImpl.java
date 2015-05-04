/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.impl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.antlr.v4.runtime.tree.ParseTree;
import org.opendaylight.yangtools.antlrv4.code.gen.YangStatementParser;
import org.opendaylight.yangtools.antlrv4.code.gen.YangStatementParserBaseListener;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.YangConstants;
import org.opendaylight.yangtools.yang.parser.spi.source.PrefixToModule;
import org.opendaylight.yangtools.yang.parser.spi.source.QNameToStatementDefinition;
import org.opendaylight.yangtools.yang.parser.spi.source.SourceException;
import org.opendaylight.yangtools.yang.parser.spi.source.StatementSourceReference;
import org.opendaylight.yangtools.yang.parser.spi.source.StatementWriter;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.Utils;
import org.opendaylight.yangtools.yang.model.api.Rfc6020Mapping;

import javax.annotation.concurrent.Immutable;

@Immutable
public class YangStatementParserListenerImpl extends YangStatementParserBaseListener {

    private StatementWriter writer;
    private StatementSourceReference ref;
    private QNameToStatementDefinition stmtDef;
    private PrefixToModule prefixes;
    private List<String> toBeSkipped = new ArrayList<>();
    private boolean isType = false;
    private static final Logger LOG = LoggerFactory.getLogger(YangStatementParserListenerImpl.class);

    public YangStatementParserListenerImpl(StatementSourceReference ref) {
        this.ref = ref;
    }

    public void setAttributes(StatementWriter writer, QNameToStatementDefinition stmtDef) {
        this.writer = writer;
        this.stmtDef = stmtDef;
    }

    public void setAttributes(StatementWriter writer, QNameToStatementDefinition stmtDef, PrefixToModule prefixes) {
        this.writer = writer;
        this.stmtDef = stmtDef;
        this.prefixes = prefixes;
    }

    private static boolean hasNotEmptyBody(List<ParseTree> children) {
        for (ParseTree child : children) {
            if (child instanceof YangStatementParser.StatementContext) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void enterStatement(YangStatementParser.StatementContext ctx) {
        boolean action = true;
        QName identifier;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof YangStatementParser.KeywordContext) {
                try {
                    identifier = new QName(YangConstants.RFC6020_YIN_NAMESPACE,
                            ((YangStatementParser.KeywordContext) child).children.get(0).getText());
                    if (stmtDef != null && Utils.isValidStatementDefinition(prefixes, stmtDef, identifier) && toBeSkipped.isEmpty()) {
                        if (identifier.equals(Rfc6020Mapping.TYPE.getStatementName()) && hasNotEmptyBody(((YangStatementParser.KeywordContext) child).getParent().children)) {
                            isType = true;
                        } else {
                            writer.startStatement(identifier, ref);
                        }
                    } else {
                        action = false;
                        toBeSkipped.add(((YangStatementParser.KeywordContext) child).children.get(0).getText());
                    }
                } catch (SourceException e) {
                    LOG.warn(e.getMessage(), e);
                }
            } else if (child instanceof YangStatementParser.ArgumentContext) {
                try {
                    final String argument = Utils.stringFromStringContext((YangStatementParser.ArgumentContext) child);
                    if (isType) {
                        writer.startStatement(new QName(YangConstants.RFC6020_YIN_NAMESPACE, argument), ref);
                        writer.argumentValue(argument, ref);
                        isType = false;
                    } else if (action) {
                        writer.argumentValue(Utils.stringFromStringContext((YangStatementParser.ArgumentContext) child), ref);
                    } else {
                        action = true;
                    }
                } catch (SourceException e) {
                    LOG.warn(e.getMessage(), e);
                }
            }

        }
    }

    @Override
    public void exitStatement(YangStatementParser.StatementContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof YangStatementParser.KeywordContext) {
                try {
                    String statementName = ((YangStatementParser.KeywordContext) child).children.get(0).getText();
                    QName identifier = new QName(YangConstants.RFC6020_YIN_NAMESPACE, statementName);
                    if (stmtDef != null && Utils.isValidStatementDefinition(prefixes, stmtDef, identifier) && toBeSkipped.isEmpty()) {
                        writer.endStatement(ref);
                    }

                    if (toBeSkipped.contains(statementName)) {
                        toBeSkipped.remove(statementName);
                    }
                } catch (SourceException e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }
    }
}
