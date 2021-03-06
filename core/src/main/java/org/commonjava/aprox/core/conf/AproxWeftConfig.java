/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.conf;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxMapConfig;
import org.commonjava.aprox.inject.Production;
import org.commonjava.cdi.util.weft.config.DefaultWeftConfig;
import org.commonjava.web.config.ConfigurationException;

@ApplicationScoped
@Named( AproxWeftConfig.SECTION_NAME )
public class AproxWeftConfig
    extends AbstractAproxMapConfig
{

    public static final String SECTION_NAME = "threadpools";

    public static final String DEFAULT_THREADS = "defaultThreads";

    public static final String DEFAULT_PRIORITY = "defaultPriority";

    public static final String THREADS_SUFFIX = ".threads";

    public static final String PRIORITY_SUFFIX = ".priority";

    private final DefaultWeftConfig weftConfig = new DefaultWeftConfig();

    private final String numericPattern = "[0-9]+";

    public AproxWeftConfig()
    {
        super( SECTION_NAME );
    }

    @Override
    public void parameter( final String name, final String value )
        throws ConfigurationException
    {

        if ( !value.matches( numericPattern ) )
        {
            throw new ConfigurationException( "Invalid value: '{}' for parameter: '{}'. Only numeric values are accepted for section: '{}'.", value,
                                              name, SECTION_NAME );
        }

        final int v = Integer.parseInt( value );

        if ( DEFAULT_THREADS.equals( name ) )
        {
            weftConfig.configureDefaultThreads( v );
        }
        else if ( DEFAULT_PRIORITY.equals( name ) )
        {
            weftConfig.configureDefaultPriority( v );
        }
        else
        {
            final int lastIdx = name.lastIndexOf( '.' );
            if ( lastIdx > -1 && name.length() > lastIdx )
            {
                final String pool = name.substring( 0, lastIdx );
                final String suffix = name.substring( lastIdx );

                if ( THREADS_SUFFIX.equals( suffix ) )
                {
                    weftConfig.configureThreads( pool, v );
                }
                else if ( PRIORITY_SUFFIX.equals( suffix ) )
                {
                    weftConfig.configurePriority( pool, v );
                }
            }
        }
    }

    @Produces
    @Production
    @Default
    public DefaultWeftConfig getWeftConfig()
    {
        return weftConfig;
    }

    @Override
    public void sectionStarted( final String name )
        throws ConfigurationException
    {
        // NOP; just block map init in the underlying implementation.
    }

}
