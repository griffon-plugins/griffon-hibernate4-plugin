/*
 * Copyright 2014-2015 the original author or authors.
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
package org.codehaus.griffon.compile.hibernate4.ast.transform

import griffon.plugins.hibernate4.Hibernate4Handler
import spock.lang.Specification

import java.lang.reflect.Method

/**
 * @author Andres Almiray
 */
class Hibernate4AwareASTTransformationSpec extends Specification {
    def 'Hibernate4AwareASTTransformation is applied to a bean via @Hibernate4Aware'() {
        given:
        GroovyShell shell = new GroovyShell()

        when:
        def bean = shell.evaluate('''
        @griffon.transform.Hibernate4Aware
        class Bean { }
        new Bean()
        ''')

        then:
        bean instanceof Hibernate4Handler
        Hibernate4Handler.methods.every { Method target ->
            bean.class.declaredMethods.find { Method candidate ->
                candidate.name == target.name &&
                candidate.returnType == target.returnType &&
                candidate.parameterTypes == target.parameterTypes &&
                candidate.exceptionTypes == target.exceptionTypes
            }
        }
    }

    def 'Hibernate4AwareASTTransformation is not applied to a Hibernate4Handler subclass via @Hibernate4Aware'() {
        given:
        GroovyShell shell = new GroovyShell()

        when:
        def bean = shell.evaluate('''
        import griffon.plugins.hibernate4.Hibernate4Callback
        import griffon.plugins.hibernate4.exceptions.RuntimeHibernate4Exception
        import griffon.plugins.hibernate4.Hibernate4Handler

        import javax.annotation.Nonnull
        @griffon.transform.Hibernate4Aware
        class Hibernate4HandlerBean implements Hibernate4Handler {
            @Override
            public <R> R withHbm4Session(@Nonnull Hibernate4Callback<R> callback) throws RuntimeHibernate4Exception {
                return null
            }
            @Override
            public <R> R withHbm4Session(@Nonnull String sessionFactoryName, @Nonnull Hibernate4Callback<R> callback) throws RuntimeHibernate4Exception {
                return null
            }
            @Override
            void closeHbm4Session(){}
            @Override
            void closeHbm4Session(@Nonnull String sessionFactoryName){}
        }
        new Hibernate4HandlerBean()
        ''')

        then:
        bean instanceof Hibernate4Handler
        Hibernate4Handler.methods.every { Method target ->
            bean.class.declaredMethods.find { Method candidate ->
                candidate.name == target.name &&
                    candidate.returnType == target.returnType &&
                    candidate.parameterTypes == target.parameterTypes &&
                    candidate.exceptionTypes == target.exceptionTypes
            }
        }
    }
}
