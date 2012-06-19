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
package org.commonjava.aprox.core.model;

import java.util.HashMap;
import java.util.Map;

public abstract class ArtifactStore
{

    private String name;

    private StoreKey key;

    private final StoreType doctype;

    private transient Map<String, String> metadata;

    protected ArtifactStore( final StoreType type )
    {
        doctype = type;
    }

    protected ArtifactStore( final StoreType doctype, final String name )
    {
        this.doctype = doctype;
        this.name = name;
        this.key = new StoreKey( doctype, name );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.web.maven.proxy.model.ArtifactStore#getName()
     */
    public String getName()
    {
        return name;
    }

    protected void setName( final String name )
    {
        this.name = name;
        this.key = new StoreKey( doctype, name );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.web.maven.proxy.model.ArtifactStore#getDoctype()
     */
    public StoreType getDoctype()
    {
        return doctype;
    }

    public StoreKey getKey()
    {
        return key;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( key == null ) ? 0 : key.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ArtifactStore other = (ArtifactStore) obj;
        if ( key == null )
        {
            if ( other.key != null )
            {
                return false;
            }
        }
        else if ( !key.equals( other.key ) )
        {
            return false;
        }
        return true;
    }

    public synchronized String setMetadata( final String key, final String value )
    {
        if ( key == null || value == null )
        {
            return null;
        }

        if ( metadata == null )
        {
            metadata = new HashMap<String, String>();
        }

        return metadata.put( key, value );
    }

    public String getMetadata( final String key )
    {
        return metadata == null ? null : metadata.get( key );
    }

}
