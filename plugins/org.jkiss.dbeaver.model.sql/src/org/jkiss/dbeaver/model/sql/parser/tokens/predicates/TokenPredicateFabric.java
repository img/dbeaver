/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.sql.parser.tokens.predicates;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPCharacterScanner;
import org.jkiss.dbeaver.model.text.parser.TPRule;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Dialect-specific predicate node producer
 */
class SQLTokenPredicateFabric extends TokenPredicateFabric {
    private static class StringScanner implements TPCharacterScanner {
        private final String string;
        private int pos = 0;

        public StringScanner(String string) {
            this.string = string;
        }

        @Override
        public char[][] getLegalLineDelimiters() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getColumn() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return pos < string.length() ? string.charAt(pos++) : -1;
        }

        @Override
        public void unread() {
            pos--;
        }

        public void reset() {
            pos = 0;
        }
    }

    private final TPRule[] allRules;

    public SQLTokenPredicateFabric(@NotNull SQLRuleManager ruleManager) {
        super();
        allRules = ruleManager.getAllRules();
    }

    @Override
    @NotNull
    protected SQLTokenEntry classifyToken(@NotNull String string) {
        StringScanner scanner = new StringScanner(string);
        for (TPRule fRule : allRules) {
            try {
                scanner.reset();
                TPToken token = fRule.evaluate(scanner);
                if (!token.isUndefined()) {
                    SQLTokenType tokenType = token instanceof TPTokenDefault ? (SQLTokenType) ((TPTokenDefault) token).getData() : SQLTokenType.T_OTHER;
                    return new SQLTokenEntry(string, tokenType);
                }
            } catch (Throwable e) {
                // some rules raise exceptions in a certain situations when the string does not correspond the rule
                // e.printStackTrace();
            }
        }
        return new SQLTokenEntry(string, null);
    }
}

/**
 * Default predicate node producer
 */
class DefaultTokenPredicateFabric extends TokenPredicateFabric {
    public DefaultTokenPredicateFabric() {
        super();
    }

    @Override
    @NotNull
    protected TokenPredicateNode classifyToken(@NotNull String tokenString) {
        // Knows nothing about particular dialect or used in dialect-agnostic context
        return new SQLTokenEntry(tokenString, SQLTokenType.T_UNKNOWN);
    }
}

/**
 * The producer of all predicate nodes responsible for exact string token entries classification according to dialect.
 * <p>
 * Producing methods accept different possible arguments in any combinations:
 * <ul>
 *     <li>{@literal null} - entry corresponding to any token
 *     <li>{@link String} - entry corresponding to the exact token
 *     <li>{@link SQLTokenType} - entry corresponding to the any token of given token type
 *     <li>{@link TokenPredicateNode} - entry corresponding to the given subsequence of tokens
 * </ul>
 * </p>
 */
public abstract class TokenPredicateFabric {

    /**
     * Create dialect-agnostinc {@link TokenPredicateFabric}
     */
    public static TokenPredicateFabric makeDefaultFabric() {
        return new DefaultTokenPredicateFabric();
    }

    /**
     * Create dialect-specific {@link TokenPredicateFabric}
     */
    public static TokenPredicateFabric makeDialectSpecificFabric(SQLRuleManager ruleManager) {
        return new SQLTokenPredicateFabric(ruleManager);
    }

    protected TokenPredicateFabric() {
    }

    /**
     * Materialize token predicate node describing given token string with a dialect-specific token type classification
     * @param tokenString to classify
     * @return predicate node carrying information about the token entry
     */
    @NotNull
    protected abstract TokenPredicateNode classifyToken(@NotNull String tokenString);

    /**
     * Materialize token predicate node carrying information about token entry described in a certain way.
     * @param obj some information about the token entry (see {@link TokenPredicateFabric} for the details)
     * @return predicate node carrying information about the token entry
     */
    @NotNull
    private TokenPredicateNode makeNode(@Nullable Object obj) {
        if (obj == null) {
            return new SQLTokenEntry(null, null);
        } else if (obj instanceof TokenPredicateNode) {
            return (TokenPredicateNode)obj;
        } else if (obj instanceof String) {
            return this.classifyToken((String)obj);
        } else if (obj instanceof SQLTokenType) {
            return new SQLTokenEntry(null, (SQLTokenType) obj);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @NotNull
    private TokenPredicateNode[] makeGroup(Object ... objs) {
        return Arrays.stream(objs).map(o -> makeNode(o)).collect(Collectors.toList()).toArray(new TokenPredicateNode[0]);
    }

    @NotNull
    public TokenPredicateNode token(Object obj) {
        return this.makeNode(obj);
    }

    @NotNull
    public TokenPredicateNode sequence(TokenPredicateNode ... nodes) {
        return new SequenceTokenPredicateNode(nodes);
    }

    @NotNull
    public TokenPredicateNode sequence(Object ... objs) {
        return new SequenceTokenPredicateNode(this.makeGroup(objs));
    }

    @NotNull
    public TokenPredicateNode alternative(TokenPredicateNode ... nodes) {
        return new AlternativeTokenPredicateNode(nodes);
    }

    @NotNull
    public TokenPredicateNode alternative(Object ... objs) {
        return new AlternativeTokenPredicateNode(this.makeGroup(objs));
    }

    @NotNull
    public TokenPredicateNode optional(@NotNull TokenPredicateNode node) {
        return new OptionalTokenPredicateNode(node);
    }

    @NotNull
    public TokenPredicateNode optional(Object ... obj) {
        return new OptionalTokenPredicateNode(obj.length == 1 ? this.makeNode(obj[0]) : this.sequence(obj));
    }
}