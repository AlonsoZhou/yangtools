/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.stmt.rfc6020.effective;

import com.google.common.base.Verify;
import org.opendaylight.yangtools.yang.model.api.meta.DeclaredStatement;
import org.opendaylight.yangtools.yang.model.api.meta.StatementDefinition;
import org.opendaylight.yangtools.yang.model.api.meta.StatementSource;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContext;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.StatementContextBase;

public abstract class DeclaredEffectiveStatementBase<A, D extends DeclaredStatement<A>> extends
        EffectiveStatementBase<A, D> {

    private final StatementSource statementSource;
    private final A argument;
    private final D declaredInstance;

    protected DeclaredEffectiveStatementBase(final StmtContext<A, D, ?> ctx) {
        this(ctx, true);
    }

    /**
     * Constructor.
     *
     * @param ctx
     *            context of statement.
     * @param buildUnknownSubstatements
     *            if it is false, the unknown substatements are omitted from
     *            build of effective substatements till the call of either
     *            effectiveSubstatements or getOmittedUnknownSubstatements
     *            method of EffectiveStatementBase class. The main purpose of
     *            this is to allow the build of recursive extension definitions.
     */
    protected DeclaredEffectiveStatementBase(StmtContext<A, D, ?> ctx, final boolean buildUnknownSubstatements) {
        super(ctx, buildUnknownSubstatements);

        this.argument = ctx.getStatementArgument();
        this.statementSource = ctx.getStatementSource();

        /*
         * Share original instance of declared statement between all effective
         * statements which have been copied or derived from this original
         * declared statement.
         */
        @SuppressWarnings("unchecked")
        final StatementContextBase<A, D, ?> originalCtx = (StatementContextBase<A, D, ?>) ctx.getOriginalCtx();
        if (originalCtx != null) {
            ctx = originalCtx;
        }
        declaredInstance = Verify.verifyNotNull(ctx.buildDeclared(), "Statement %s failed to build declared statement",
                ctx);
    }

    @Override
    public final StatementDefinition statementDefinition() {
        return declaredInstance.statementDefinition();
    }

    @Override
    public A argument() {
        return argument;
    }

    @Override
    public final StatementSource getStatementSource() {
        return statementSource;
    }

    @Override
    public final D getDeclared() {
        return declaredInstance;
    }
}