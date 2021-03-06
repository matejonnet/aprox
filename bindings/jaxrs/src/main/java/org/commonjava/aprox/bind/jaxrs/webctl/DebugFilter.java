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
package org.commonjava.aprox.bind.jaxrs.webctl;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@WebFilter( "/*" )
public class DebugFilter
    implements Filter
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void destroy()
    {
        //        logger.info( "Filter destroy()" );
    }

    @Override
    public void doFilter( final ServletRequest req, final ServletResponse resp, final FilterChain chain )
        throws IOException, ServletException
    {
        final HttpServletRequest request = (HttpServletRequest) req;
        logger.info( "REQUEST:\n  URI: {}\n  Path Translated: {}\n  Path Info: {}\n  Context Path: {}\n\n", request.getRequestURI(),
                     request.getPathTranslated(), request.getPathInfo(), request.getContextPath() );

        chain.doFilter( request, resp );
    }

    @Override
    public void init( final FilterConfig config )
        throws ServletException
    {
        //        logger.info( "Filter init(..)" );
        //        ctx = config.getServletContext();
    }

}
