/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.conf;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.DefaultConfigurationListener;
import org.commonjava.web.config.dotconf.DotConfConfigurationReader;
import org.commonjava.web.config.io.ConfigFileUtils;

@Singleton
public class DefaultAproxConfigFactory
    extends DefaultConfigurationListener
    implements AproxConfigFactory
{

    private static final String CONFIG_PATH = "/etc/aprox/main.conf";

    @Inject
    private Instance<AproxConfigSection<?>> configSections;

    public DefaultAproxConfigFactory()
        throws ConfigurationException
    {
    }

    @PostConstruct
    protected void load()
        throws ConfigurationException
    {
        for ( final AproxConfigSection<?> section : configSections )
        {
            with( section.getSectionName(), section.getConfigurationClass() );
        }

        InputStream stream = null;
        try
        {
            stream = ConfigFileUtils.readFileWithIncludes( CONFIG_PATH );
            new DotConfConfigurationReader( this ).loadConfiguration( stream );
        }
        catch ( final IOException e )
        {
            throw new ConfigurationException( "Cannot open configuration file: %s. Reason: %s", e, CONFIG_PATH,
                                              e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }
    }

}