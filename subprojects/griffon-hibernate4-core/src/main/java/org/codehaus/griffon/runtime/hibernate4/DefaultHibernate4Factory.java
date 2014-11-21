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

import griffon.core.GriffonApplication;
import griffon.core.injection.Injector;
import griffon.plugins.datasource.DataSourceFactory;
import griffon.plugins.datasource.DataSourceStorage;
import griffon.plugins.hibernate4.Hibernate4Bootstrap;
import griffon.plugins.hibernate4.Hibernate4Factory;
import griffon.plugins.hibernate4.internal.HibernateConfigurationHelper;
import griffon.util.CollectionUtils;
import org.codehaus.griffon.runtime.core.storage.AbstractObjectFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import java.util.Map;

import static griffon.util.ConfigUtils.getConfigValue;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultHibernate4Factory extends AbstractObjectFactory<SessionFactory> implements Hibernate4Factory {
    @Inject
    private DataSourceFactory dataSourceFactory;

    @Inject
    private DataSourceStorage dataSourceStorage;

    @Inject
    private Injector injector;

    @Inject
    public DefaultHibernate4Factory(@Nonnull @Named("hibernate4") griffon.core.Configuration configuration, @Nonnull GriffonApplication application) {
        super(configuration, application);
    }

    @Nonnull
    @Override
    protected String getSingleKey() {
        return "sessionFactory";
    }

    @Nonnull
    @Override
    protected String getPluralKey() {
        return "sessionFactories";
    }

    @Nonnull
    @Override
    public SessionFactory create(@Nonnull String name) {
        Map<String, Object> config = narrowConfig(name);
        event("Hibernate4ConnectStart", asList(name, config));

        Configuration configuration = createConfiguration(config, name);
        createSchema(name, config, configuration);

        SessionFactory sessionFactory = configuration.buildSessionFactory();
        Session session = null;
        try {
            session = openSession(name, sessionFactory);
            for (Object o : injector.getInstances(Hibernate4Bootstrap.class)) {
                ((Hibernate4Bootstrap) o).init(name, session);
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }

        event("Hibernate4ConnectEnd", asList(name, config, sessionFactory));
        return sessionFactory;
    }

    @Override
    public void destroy(@Nonnull String name, @Nonnull SessionFactory instance) {
        requireNonNull(instance, "Argument 'instance' must not be null");
        Map<String, Object> config = narrowConfig(name);
        event("Hibernate4DisconnectStart", asList(name, config, instance));

        Session session = null;
        try {
            session = openSession(name, instance);
            for (Object o : injector.getInstances(Hibernate4Bootstrap.class)) {
                ((Hibernate4Bootstrap) o).destroy(name, session);
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }

        closeDataSource(name);

        event("Hibernate4DisconnectEnd", asList(name, config));
    }

    @Nonnull
    @SuppressWarnings("ConstantConditions")
    protected Configuration createConfiguration(@Nonnull Map<String, Object> config, @Nonnull String dataSourceName) {
        DataSource dataSource = getDataSource(dataSourceName);
        HibernateConfigurationHelper configHelper = new HibernateConfigurationHelper(getApplication(), config, dataSourceName, dataSource);
        Configuration configuration = configHelper.buildConfiguration();
        getApplication().getEventRouter().publishEvent("Hibernate4ConfigurationAvailable",
            asList(CollectionUtils.map()
                .e("configuration", configuration)
                .e("dataSourceName", dataSourceName)
                .e("sessionConfiguration", config)));
        return configuration;
    }

    protected void createSchema(@Nonnull String dataSourceName, @Nonnull Map<String, Object> config, @Nonnull Configuration configuration) {
        configuration.setProperty("hibernate.hbm2ddl.auto", getConfigValue(config, "schema", "create-drop"));
    }

    protected void closeDataSource(@Nonnull String dataSourceName) {
        DataSource dataSource = dataSourceStorage.get(dataSourceName);
        if (dataSource != null) {
            dataSourceFactory.destroy(dataSourceName, dataSource);
            dataSourceStorage.remove(dataSourceName);
        }
    }

    @Nonnull
    protected DataSource getDataSource(@Nonnull String dataSourceName) {
        DataSource dataSource = dataSourceStorage.get(dataSourceName);
        if (dataSource == null) {
            dataSource = dataSourceFactory.create(dataSourceName);
            dataSourceStorage.set(dataSourceName, dataSource);
        }
        return dataSource;
    }

    @Nonnull
    protected Session openSession(@Nonnull String sessionFactoryName, @Nonnull SessionFactory sqlSessionFactory) {
        return sqlSessionFactory.openSession();
    }
}
