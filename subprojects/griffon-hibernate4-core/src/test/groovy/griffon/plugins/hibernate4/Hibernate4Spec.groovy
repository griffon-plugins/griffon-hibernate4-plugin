/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014-2020 The author and/or original authors.
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
package griffon.plugins.hibernate4

import griffon.annotations.inject.BindTo
import griffon.core.GriffonApplication
import griffon.plugins.datasource.events.DataSourceConnectEndEvent
import griffon.plugins.datasource.events.DataSourceConnectStartEvent
import griffon.plugins.datasource.events.DataSourceDisconnectEndEvent
import griffon.plugins.datasource.events.DataSourceDisconnectStartEvent
import griffon.plugins.hibernate4.events.Hibernate4ConfigurationAvailableEvent
import griffon.plugins.hibernate4.events.Hibernate4ConnectEndEvent
import griffon.plugins.hibernate4.events.Hibernate4ConnectStartEvent
import griffon.plugins.hibernate4.events.Hibernate4DisconnectEndEvent
import griffon.plugins.hibernate4.events.Hibernate4DisconnectStartEvent
import griffon.plugins.hibernate4.exceptions.RuntimeHibernate4Exception
import griffon.test.core.GriffonUnitRule
import org.hibernate.Session
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

import javax.application.event.EventHandler
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
            'Hibernate4ConnectStartEvent', 'DataSourceConnectStartEvent',
            'DataSourceConnectEndEvent', 'Hibernate4ConfigurationAvailableEvent', 'Hibernate4ConnectEndEvent',
            'Hibernate4DisconnectStartEvent', 'DataSourceDisconnectStartEvent',
            'DataSourceDisconnectEndEvent', 'Hibernate4DisconnectEndEvent'
        ]
        TestEventHandler testEventHandler = new TestEventHandler()
        application.eventRouter.subscribe(testEventHandler)

        when:
        hibernate4Handler.withHbm4Session { String sessionFactoryName, Session session ->
            true
        }
        hibernate4Handler.closeHbm4Session()
        // second call should be a NOOP
        hibernate4Handler.closeHbm4Session()

        then:
        testEventHandler.events.size() == 9
        testEventHandler.events == eventNames
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
        hibernate4Handler.withHbm4Session { String sessionFactoryName, Session session -> }

        then:
        bootstrap.initWitness
        !bootstrap.destroyWitness
    }

    void 'Bootstrap destroy is called'() {
        given:
        assert !bootstrap.initWitness
        assert !bootstrap.destroyWitness

        when:
        hibernate4Handler.withHbm4Session { String sessionFactoryName, Session session -> }
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
            [[id: 1, name: 'Danno', lastname: 'Ferrin'],
             [id: 2, name: 'Andres', lastname: 'Almiray'],
             [id: 3, name: 'James', lastname: 'Williams'],
             [id: 4, name: 'Guillaume', lastname: 'Laforge'],
             [id: 5, name: 'Jim', lastname: 'Shingler'],
             [id: 6, name: 'Alexander', lastname: 'Klein'],
             [id: 7, name: 'Rene', lastname: 'Groeschke']].each { data ->
                session.save(new Person(data))
            }
        }

        List peopleOut = hibernate4Handler.withHbm4Session() { String sessionFactoryName, Session session ->
            session.createQuery('from Person').list()*.asMap()
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

    private class TestEventHandler {
        List<String> events = []

        @EventHandler
        void handleDataSourceConnectStartEvent(DataSourceConnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDataSourceConnectEndEvent(DataSourceConnectEndEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDataSourceDisconnectStartEvent(DataSourceDisconnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleDataSourceDisconnectEndEvent(DataSourceDisconnectEndEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleHibernate4ConnectStartEvent(Hibernate4ConnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleHibernate4ConfigurationAvailableEvent(Hibernate4ConfigurationAvailableEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleHibernate4ConnectEndEvent(Hibernate4ConnectEndEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleHibernate4DisconnectStartEvent(Hibernate4DisconnectStartEvent event) {
            events << event.class.simpleName
        }

        @EventHandler
        void handleHibernate4DisconnectEndEvent(Hibernate4DisconnectEndEvent event) {
            events << event.class.simpleName
        }
    }
}
