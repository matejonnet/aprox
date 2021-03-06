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
package org.commonjava.aprox.util;

public enum ApplicationStatus
{

    /* @formatter:off */
    OK( 200, "Ok" ), 
    CREATED( 201, "Created" ), 
    NO_CONTENT(204, "No Content"),
    
    MOVED_PERMANENTLY( 301, "Moved Permanently" ),
    FOUND( 302, "Found" ),
    
    NOT_MODIFIED( 304, "Not Modified" ),
    
    BAD_REQUEST( 400, "Bad Request" ), 
    
    NOT_FOUND( 404, "Not Found" ), 
    
    CONFLICT( 409, "Conflict" ),
    
    SERVER_ERROR( 500, "Internal Server Error" );
    /* @formatter:on */

    private int status;

    private String message;

    private ApplicationStatus( final int status, final String messsage )
    {
        this.status = status;
        this.message = messsage;
    }

    public int code()
    {
        return status;
    }

    public String message()
    {
        return message;
    }

    public static ApplicationStatus getStatus( final int status )
    {
        for ( final ApplicationStatus as : values() )
        {
            if ( as.code() == status )
            {
                return as;
            }
        }

        return null;
    }

}
