/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014-2021 The author and/or original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.compile.hibernate4.ast.transform;

import griffon.annotations.core.Nonnull;
import griffon.plugins.hibernate4.Hibernate4Handler;
import griffon.transform.hibernate4.Hibernate4Aware;
import org.codehaus.griffon.compile.core.AnnotationHandler;
import org.codehaus.griffon.compile.core.AnnotationHandlerFor;
import org.codehaus.griffon.compile.core.ast.transform.AbstractASTTransformation;
import org.codehaus.griffon.compile.hibernate4.Hibernate4AwareConstants;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codehaus.griffon.compile.core.ast.GriffonASTUtils.injectInterface;

/**
 * Handles generation of code for the {@code @Hibernate4Aware} annotation.
 *
 * @author Andres Almiray
 */
@AnnotationHandlerFor(Hibernate4Aware.class)
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class Hibernate4AwareASTTransformation extends AbstractASTTransformation implements Hibernate4AwareConstants, AnnotationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(Hibernate4AwareASTTransformation.class);
    private static final ClassNode HIBERNATE4_HANDLER_CNODE = makeClassSafe(Hibernate4Handler.class);
    private static final ClassNode HIBERNATE4_AWARE_CNODE = makeClassSafe(Hibernate4Aware.class);

    /**
     * Handles the bulk of the processing, mostly delegating to other methods.
     *
     * @param nodes  the ast nodes
     * @param source the source unit for the nodes
     */
    public void visit(ASTNode[] nodes, SourceUnit source) {
        checkNodesForAnnotationAndType(nodes[0], nodes[1]);
        addHibernate4HandlerIfNeeded(source, (AnnotationNode) nodes[0], (ClassNode) nodes[1]);
    }

    /**
     * Convenience method to see if an annotated node is {@code @Hibernate4Aware}.
     *
     * @param node the node to check
     * @return true if the node is an event publisher
     */
    public static boolean hasHibernate4AwareAnnotation(AnnotatedNode node) {
        for (AnnotationNode annotation : node.getAnnotations()) {
            if (HIBERNATE4_AWARE_CNODE.equals(annotation.getClassNode())) {
                return true;
            }
        }
        return false;
    }

    public static void addHibernate4HandlerIfNeeded(SourceUnit source, AnnotationNode annotationNode, ClassNode classNode) {
        if (needsDelegate(classNode, source, METHODS, Hibernate4Aware.class.getSimpleName(), HIBERNATE4_HANDLER_TYPE)) {
            LOG.debug("Injecting {} into {}", HIBERNATE4_HANDLER_TYPE, classNode.getName());
            apply(classNode);
        }
    }

    /**
     * Adds the necessary field and methods to support hibernate4 handling.
     *
     * @param declaringClass the class to which we add the support field and methods
     */
    public static void apply(@Nonnull ClassNode declaringClass) {
        injectInterface(declaringClass, HIBERNATE4_HANDLER_CNODE);
        Expression hibernate4Handler = injectedField(declaringClass, HIBERNATE4_HANDLER_CNODE, HIBERNATE4_HANDLER_FIELD_NAME);
        addDelegateMethods(declaringClass, HIBERNATE4_HANDLER_CNODE, hibernate4Handler);
    }
}