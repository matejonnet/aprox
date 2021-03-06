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
package org.commonjava.aprox.core.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.RemoteRepository;
import org.junit.Test;

public abstract class RepositoryDataManagerTCK
    extends AbstractProxyDataManagerTCK
{

    @Test
    public void createAndRetrieveCentralRepoProxy()
        throws ProxyDataException
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRemoteRepository( repo, false );

        final RemoteRepository result = manager.getRemoteRepository( repo.getName() );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.getUrl(), equalTo( repo.getUrl() ) );
        assertThat( result.getUser(), nullValue() );
        assertThat( result.getPassword(), nullValue() );
    }

    @Test
    public void createCentralRepoProxyTwiceAndRetrieveOne()
        throws ProxyDataException
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRemoteRepository( repo, true );

        manager.storeRemoteRepository( repo, true );

        final List<? extends RemoteRepository> result = manager.getAllRemoteRepositories();

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 1 ) );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByName()
        throws ProxyDataException
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRemoteRepository( repo, false );

        manager.deleteRemoteRepository( repo.getName() );

        final ArtifactStore result = manager.getRemoteRepository( repo.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByObject()
        throws ProxyDataException
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRemoteRepository( repo, false );

        manager.deleteRemoteRepository( repo );

        final ArtifactStore result = manager.getRemoteRepository( repo.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createTwoReposAndRetrieveAll()
        throws ProxyDataException
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRemoteRepository( repo );

        final RemoteRepository repo2 = new RemoteRepository( "test", "http://www.google.com" );
        manager.storeRemoteRepository( repo2 );

        final List<? extends RemoteRepository> repositories = manager.getAllRemoteRepositories();

        assertThat( repositories, notNullValue() );
        assertThat( repositories.size(), equalTo( 2 ) );

        Collections.sort( repositories, new Comparator<RemoteRepository>()
        {

            @Override
            public int compare( final RemoteRepository r1, final RemoteRepository r2 )
            {
                return r1.getName()
                         .compareTo( r2.getName() );
            }
        } );

        ArtifactStore r = repositories.get( 0 );
        assertThat( r.getName(), equalTo( repo.getName() ) );

        r = repositories.get( 1 );
        assertThat( r.getName(), equalTo( repo2.getName() ) );
    }

}
