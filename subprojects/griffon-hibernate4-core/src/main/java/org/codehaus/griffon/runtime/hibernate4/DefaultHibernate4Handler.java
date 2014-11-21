/*
 * Copyright 2014 the original author or authors.
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
package org.codehaus.griffon.runtime.hibernate4;

import griffon.plugins.hibernate4.Hibernate4Callback;
import griffon.plugins.hibernate4.Hibernate4Factory;
import griffon.plugins.hibernate4.Hibernate4Handler;
import griffon.plugins.hibernate4.Hibernate4Storage;
import griffon.plugins.hibernate4.exceptions.RuntimeHibernate4Exception;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultHibernate4Handler implements Hibernate4Handler {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultHibernate4Handler.class);
    private static final String ERROR_SESSION_FACTORY_NAME_BLANK = "Argument 'sessionFactoryName' must not be blank";
    private static final String ERROR_CALLBACK_NULL = "Argument 'callback' must not be null";

    private final Hibernate4Factory hibernate4Factory;
    private final Hibernate4Storage hibernate4Storage;

    @Inject
    public DefaultHibernate4Handler(@Nonnull Hibernate4Factory hibernate4Factory, @Nonnull Hibernate4Storage hibernate4Storage) {
        this.hibernate4Factory = requireNonNull(hibernate4Factory, "Argument 'hibernate4Factory' must not be null");
        this.hibernate4Storage = requireNonNull(hibernate4Storage, "Argument 'hibernate4Storage' must not be null");
    }

    @Nullable
    @Override
    public <R> R withHbm4Session(@Nonnull Hibernate4Callback<R> callback) throws RuntimeHibernate4Exception {
        return withHbm4Session(DefaultHibernate4Factory.KEY_DEFAULT, callback);
    }

    @Nullable
    @Override
    @SuppressWarnings("ThrowFromFinallyBlock")
    public <R> R withHbm4Session(@Nonnull String sessionFactoryName, @Nonnull Hibernate4Callback<R> callback) throws RuntimeHibernate4Exception {
        requireNonBlank(sessionFactoryName, ERROR_SESSION_FACTORY_NAME_BLANK);
        requireNonNull(callback, ERROR_CALLBACK_NULL);

        SessionFactory sf = getSessionFactory(sessionFactoryName);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing statements on session '{}'", sessionFactoryName);
        }
        Session session = sf.openSession();
        try {
            session.beginTransaction();
            return callback.handle(sessionFactoryName, session);
        } catch (Exception e) {
            throw new RuntimeHibernate4Exception(sessionFactoryName, e);
        } finally {
            try {
                if (!session.getTransaction().wasRolledBack()) {
                    session.getTransaction().commit();
                }
                session.close();
            } catch (Exception e) {
                throw new RuntimeHibernate4Exception(sessionFactoryName, e);
            }
        }
    }

    @Override
    public void closeHbm4Session() {
        closeHbm4Session(DefaultHibernate4Factory.KEY_DEFAULT);
    }

    @Override
    public void closeHbm4Session(@Nonnull String sessionFactoryName) {
        requireNonBlank(sessionFactoryName, ERROR_SESSION_FACTORY_NAME_BLANK);
        SessionFactory hibernate4 = hibernate4Storage.get(sessionFactoryName);
        if (hibernate4 != null) {
            hibernate4Factory.destroy(sessionFactoryName, hibernate4);
            hibernate4Storage.remove(sessionFactoryName);
        }
    }

    @Nonnull
    private SessionFactory getSessionFactory(@Nonnull String sessionFactoryName) {
        SessionFactory sessionFactory = hibernate4Storage.get(sessionFactoryName);
        if (sessionFactory == null) {
            sessionFactory = hibernate4Factory.create(sessionFactoryName);
            hibernate4Storage.set(sessionFactoryName, sessionFactory);
        }
        return sessionFactory;
    }
}
