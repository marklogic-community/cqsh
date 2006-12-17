/*
 * Copyright 2005 Andrew Bruno <aeb@qnot.org> 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.marklogic.shell;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;

public abstract class AbstractEnvironment implements Environment {
    /**
     * Default port to use when connecting to Mark Logic
     */
    public static final int DEFAULT_PORT = 8003;
    protected ContentSource contentSource;
    protected PropertiesConfiguration properties;

    public AbstractEnvironment() {
        properties = new PropertiesConfiguration();
        try {
            properties.load(".cqshrc");
        } catch(ConfigurationException ignored) {
        }
    }

    public PropertiesConfiguration getProperties() {
        return properties;
    }

    public ContentSource getContentSource() {
        return ContentSourceFactory.newContentSource(properties
                .getString("host"), properties.getInt("port", DEFAULT_PORT),
                properties.getString("user"), properties.getString("password"),
                properties.getString("database"));
    }
}
