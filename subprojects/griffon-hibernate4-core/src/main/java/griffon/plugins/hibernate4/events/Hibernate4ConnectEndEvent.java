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
package griffon.plugins.hibernate4.events;

import griffon.annotations.core.Nonnull;
import griffon.core.event.Event;
import org.hibernate.SessionFactory;

import java.util.Map;

import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 * @since 3.0.0
 */
public class Hibernate4ConnectEndEvent extends Event {
    private final String name;
    private final Map<String, Object> config;
    private final SessionFactory sessionFactory;

    public Hibernate4ConnectEndEvent(@Nonnull String name, @Nonnull Map<String, Object> config, @Nonnull SessionFactory sessionFactory) {
        this.name = requireNonBlank(name, "Argument 'name' must not be blank");
        this.config = requireNonNull(config, "Argument 'config' must not be null");
        this.sessionFactory = requireNonNull(sessionFactory, "Argument 'sessionFactory' must not be null");
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public Map<String, Object> getConfig() {
        return config;
    }

    @Nonnull
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Nonnull
    public static Hibernate4ConnectEndEvent of(@Nonnull String name, @Nonnull Map<String, Object> config, @Nonnull SessionFactory sessionFactory) {
        return new Hibernate4ConnectEndEvent(name, config, sessionFactory);
    }
}
