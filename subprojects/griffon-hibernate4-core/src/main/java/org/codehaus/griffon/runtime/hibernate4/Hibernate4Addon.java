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
package org.codehaus.griffon.runtime.hibernate4;

import griffon.core.GriffonApplication;
import griffon.inject.DependsOn;
import griffon.plugins.hibernate4.Hibernate4Callback;
import griffon.plugins.hibernate4.Hibernate4Factory;
import griffon.plugins.hibernate4.Hibernate4Handler;
import org.codehaus.griffon.runtime.core.addon.AbstractGriffonAddon;
import org.hibernate.Session;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static griffon.util.ConfigUtils.getConfigValueAsBoolean;

/**
 * @author Andres Almiray
 * @since 1.1.0
 */
@DependsOn("datasource")
@Named("hibernate4")
public class Hibernate4Addon extends AbstractGriffonAddon {
    @Inject
    private Hibernate4Handler hibernate4Handler;

    @Inject
    private Hibernate4Factory hibernate4Factory;

    public void onStartupStart(@Nonnull GriffonApplication application) {
        for (String sessionFactoryName : hibernate4Factory.getSessionFactoryNames()) {
            Map<String, Object> config = hibernate4Factory.getConfigurationFor(sessionFactoryName);
            if (getConfigValueAsBoolean(config, "connect_on_startup", false)) {
                hibernate4Handler.withHbm4Session(sessionFactoryName, new Hibernate4Callback<Void>() {
                    @Override
                    public Void handle(@Nonnull String sessionFactoryName, @Nonnull Session session) {
                        return null;
                    }
                });
            }
        }
    }

    public void onShutdownStart(@Nonnull GriffonApplication application) {
        for (String sessionFactoryName : hibernate4Factory.getSessionFactoryNames()) {
            hibernate4Handler.closeHbm4Session(sessionFactoryName);
        }
    }
}
