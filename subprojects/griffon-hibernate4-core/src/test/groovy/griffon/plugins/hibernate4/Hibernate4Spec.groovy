/*
 * Copyright 2014-2017 the original author or authors.
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
package griffon.plugins.hibernate4

import griffon.core.GriffonApplication
import griffon.core.RunnableWithArgs
import griffon.core.test.GriffonUnitRule
import griffon.inject.BindTo
import griffon.plugins.hibernate4.exceptions.RuntimeHibernate4Exception
import org.hibernate.Session
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

import javax.inject.Inject

@Unroll
class Hibernate4Spec extends Specification {
    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")
    }

    @Rule
    public final GriffonUnitRule griffon = new GriffonUnitRule()

    @Inject
    private Hibernate4Handler hibernate4Handler

    @Inject
    private GriffonApplication application

    void 'Open and close default hibernate4'() {
        given:
        List eventNames = [
            'Hibernate4ConnectStart', 'DataSourceConnectStart',
            'DataSourceConnectEnd', 'Hibernate4ConfigurationAvailable', 'Hibernate4ConnectEnd',
            'Hibernate4DisconnectStart', 'DataSourceDisconnectStart',
            'DataSourceDisconnectEnd', 'Hibernate4DisconnectEnd'
        ]
        List events = []
        eventNames.each { name ->
            application.eventRouter.addEventListener(name, ({ Object... args ->
                events << [name: name, args: args]
            }) as RunnableWithArgs)
        }

        when:
        hibernate4Handler.withHbm4Session { String sessionFactoryName, Session session ->
            true
        }
        hibernate4Handler.closeHbm4Session()
        // second call should be a NOOP
        hibernate4Handler.closeHbm4Session()

        then:
        events.size() == 9
        events.name == eventNames
    }

    void 'Connect to default SessionFactory'() {
        expect:
        hibernate4Handler.withHbm4Session { String sessionFactoryName, Session session ->
            sessionFactoryName == 'default' && session
        }
    }

    void 'Bootstrap init is called'() {
        given:
        assert !bootstrap.initWitness

        when:
        hibernate4Handler.withHbm4Session { String sessionFactoryName, Session session ->  }

        then:
        bootstrap.initWitness
        !bootstrap.destroyWitness
    }

    void 'Bootstrap destroy is called'() {
        given:
        assert !bootstrap.initWitness
        assert !bootstrap.destroyWitness

        when:
        hibernate4Handler.withHbm4Session { String sessionFactoryName, Session session ->  }
        hibernate4Handler.closeHbm4Session()

        then:
        bootstrap.initWitness
        bootstrap.destroyWitness
    }

    void 'Can connect to #name SessionFactory'() {
        expect:
        hibernate4Handler.withHbm4Session(name) { String sessionFactoryName, Session session ->
            sessionFactoryName == name && session
        }

        where:
        name       | _
        'default'  | _
        'internal' | _
        'people'   | _
    }

    void 'Bogus SessionFactory name (#name) results in error'() {
        when:
        hibernate4Handler.withHbm4Session(name) { String sessionFactoryName, Session session ->
            true
        }

        then:
        thrown(IllegalArgumentException)

        where:
        name    | _
        null    | _
        ''      | _
        'bogus' | _
    }

    void 'Execute statements on people table'() {
        when:
        List peopleIn = hibernate4Handler.withHbm4Session() { String sessionFactoryName, Session session ->
            [[id: 1, name: 'Danno',     lastname: 'Ferrin'],
             [id: 2, name: 'Andres',    lastname: 'Almiray'],
             [id: 3, name: 'James',     lastname: 'Williams'],
             [id: 4, name: 'Guillaume', lastname: 'Laforge'],
             [id: 5, name: 'Jim',       lastname: 'Shingler'],
             [id: 6, name: 'Alexander', lastname: 'Klein'],
             [id: 7, name: 'Rene',      lastname: 'Groeschke']].each { data ->
                session.save(new Person(data))
            }
        }

        List peopleOut = hibernate4Handler.withHbm4Session() { String sessionFactoryName, Session session ->
            session.createQuery('from Person').list() *.asMap()
        }

        then:
        peopleIn == peopleOut
    }

    void 'A runtime exception is thrown within session handling'() {
        when:
        hibernate4Handler.withHbm4Session { String sessionFactoryName, Session session ->
            session.save(new Person())
        }

        then:
        thrown(RuntimeHibernate4Exception)
    }

    @BindTo(Hibernate4Bootstrap)
    private TestHibernate4Bootstrap bootstrap = new TestHibernate4Bootstrap()
}
