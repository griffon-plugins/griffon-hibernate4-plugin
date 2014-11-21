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
package griffon.plugins.hibernate4;

import griffon.plugins.hibernate4.exceptions.RuntimeHibernate4Exception;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Andres Almiray
 */
public interface Hibernate4Handler {
    // tag::methods[]
    @Nullable
    <R> R withHbm4Session(@Nonnull Hibernate4Callback<R> callback)
        throws RuntimeHibernate4Exception;

    @Nullable
    <R> R withHbm4Session(@Nonnull String sessionFactoryName, @Nonnull Hibernate4Callback<R> callback)
        throws RuntimeHibernate4Exception;

    void closeHbm4Session();

    void closeHbm4Session(@Nonnull String sessionFactoryName);
    // end::methods[]
}