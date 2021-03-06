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
package org.codehaus.griffon.runtime.hibernate4.internal;

import griffon.core.GriffonApplication;
import griffon.plugins.hibernate4.Hibernate4Mapping;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.kordamp.jipsy.util.TypeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static griffon.util.ConfigUtils.getConfigValue;
import static griffon.util.ConfigUtils.getConfigValueAsBoolean;
import static griffon.util.GriffonNameUtils.isBlank;

/**
 * Sets up a shared Hibernate SessionFactory.
 * Based on Spring's {@code org.springframework.orm.hibernate4.LocalSessionFactoryBean}
 * Original author: Juergen Hoeller (Spring 1.2)
 *
 * @author Andres Almiray
 */
public class HibernateConfigurationHelper {
    public static final String ENTITY_INTERCEPTOR = "entityInterceptor";
    public static final String NAMING_STRATEGY = "namingStrategy";
    public static final String PROPS = "props";
    private static final Logger LOG = LoggerFactory.getLogger(HibernateConfigurationHelper.class);
    private static final String HBM_XML_SUFFIX = ".hbm.xml";

    private final Map<String, Object> sessionConfig;
    private final String dataSourceName;
    private final DataSource dataSource;
    private final GriffonApplication application;

    public HibernateConfigurationHelper(GriffonApplication application, Map<String, Object> sessionConfig, String dataSourceName, DataSource dataSource) {
        this.application = application;
        this.sessionConfig = sessionConfig;
        this.dataSourceName = dataSourceName;
        this.dataSource = dataSource;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public Map<String, Object> getSessionConfig() {
        return sessionConfig;
    }

    public Configuration buildConfiguration() {
        // Create Configuration instance.
        Configuration config = newConfiguration();

        applyEntityInterceptor(config);
        applyNamingStrategy(config);
        applyProperties(config);
        applyDialect(config);
        applyMappings(config);

        return config;
    }

    private void applyEntityInterceptor(Configuration config) {
        Object entityInterceptor = getConfigValue(sessionConfig, ENTITY_INTERCEPTOR, null);
        if (entityInterceptor instanceof Class) {
            config.setInterceptor((Interceptor) newInstanceOf((Class) entityInterceptor));
        } else if (entityInterceptor instanceof String) {
            config.setInterceptor((Interceptor) newInstanceOf((String) entityInterceptor));
        }
    }

    private void applyNamingStrategy(Configuration config) {
        Object namingStrategy = getConfigValue(sessionConfig, NAMING_STRATEGY, null);
        if (namingStrategy instanceof Class) {
            config.setNamingStrategy((NamingStrategy) newInstanceOf((Class) namingStrategy));
        } else if (namingStrategy instanceof String) {
            config.setNamingStrategy((NamingStrategy) newInstanceOf((String) namingStrategy));
        }
    }

    private void applyProperties(Configuration config) {
        Object props = getConfigValue(sessionConfig, PROPS, null);
        if (props instanceof Properties) {
            config.setProperties((Properties) props);
        } else if (props instanceof Map) {
            for (Map.Entry<String, String> entry : ((Map<String, String>) props).entrySet()) {
                config.setProperty(entry.getKey(), entry.getValue());
            }
        }

        if (getConfigValueAsBoolean(sessionConfig, "logSql", false)) {
            config.setProperty("hibernate.show_sql", "true");
        }
        if (getConfigValueAsBoolean(sessionConfig, "formatSql", false)) {
            config.setProperty("hibernate.format_sql", "true");
        }
    }

    private void applyDialect(Configuration config) {
        Object dialect = getConfigValue(sessionConfig, "dialect", null);
        if (dialect instanceof Class) {
            config.setProperty("hibernate.dialect", ((Class) dialect).getName());
        } else if (dialect != null) {
            config.setProperty("hibernate.dialect", dialect.toString());
        } else {
            DialectDetector dialectDetector = new DialectDetector(dataSource);
            config.setProperty("hibernate.dialect", dialectDetector.getDialect());
        }
    }

    private void applyMappings(final Configuration config) {
        TypeLoader.load(application.getApplicationClassLoader().get(), "META-INF/types", Hibernate4Mapping.class, new TypeLoader.LineProcessor() {
            @Override
            public void process(ClassLoader classLoader, Class<?> type, String line) {
                line = line.trim();
                if (isBlank(line)) return;
                line = line.replace('.', '/');
                LOG.debug("Registering {} as hibernate resource", line + HBM_XML_SUFFIX);
                config.addResource(line + HBM_XML_SUFFIX);
            }
        });

        for (String mapping : getConfigValue(sessionConfig, "mappings", Collections.<String>emptyList())) {
            mapping = mapping.replace('.', '/');
            if (!mapping.endsWith(HBM_XML_SUFFIX)) {
                mapping = mapping + HBM_XML_SUFFIX;
            }
            LOG.debug("Registering {} as hibernate resource", mapping);
            config.addResource(mapping);
        }
    }

    private Object newInstanceOf(String className) {
        try {
            return newInstanceOf(Thread.currentThread().getContextClassLoader().loadClass(className));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot instantiate class " + className, e);
        }
    }

    private Object newInstanceOf(Class klass) {
        try {
            return klass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot instantiate " + klass, e);
        }
    }

    // -------------------------------------------------

    private Configuration newConfiguration() throws HibernateException {
        Configuration configuration = new Configuration();
        configuration.getProperties().put(Environment.DATASOURCE, dataSource);
        return configuration;
    }
}
