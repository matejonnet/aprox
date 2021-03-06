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
package org.commonjava.aprox.flat.data;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileConfiguration;
import org.commonjava.web.json.ser.JsonSerializer;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class FlatTCKFixtureProvider
    extends TemporaryFolder
    implements TCKFixtureProvider
{
    private TestFlatFileDataManager dataManager;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File configDir;

    @Override
    public StoreDataManager getDataManager()
    {
        return dataManager;
    }

    @Override
    protected void before()
        throws Throwable
    {
        super.before();

        configDir = newFolder( "db" );

        final JsonSerializer serializer = new JsonSerializer();

        dataManager = new TestFlatFileDataManager( new FlatFileConfiguration( configDir ), serializer );

        dataManager.install();
        dataManager.clear();
    }

    private static final class TestFlatFileDataManager
        extends FlatFileDataManagerDecorator
    {

        public TestFlatFileDataManager( final FlatFileConfiguration config, final JsonSerializer serializer )
        {
            super( new MemoryStoreDataManager(), config, serializer );
        }

        @Override
        public HostedRepository getHostedRepository( final String name )
            throws ProxyDataException
        {
            return getDataManager().getHostedRepository( name );
        }

        @Override
        public RemoteRepository getRemoteRepository( final String name )
            throws ProxyDataException
        {
            return getDataManager().getRemoteRepository( name );
        }

        @Override
        public Group getGroup( final String name )
            throws ProxyDataException
        {
            return getDataManager().getGroup( name );
        }

        @Override
        public List<Group> getAllGroups()
            throws ProxyDataException
        {
            return getDataManager().getAllGroups();
        }

        @Override
        public List<RemoteRepository> getAllRemoteRepositories()
            throws ProxyDataException
        {
            return getDataManager().getAllRemoteRepositories();
        }

        @Override
        public List<HostedRepository> getAllHostedRepositories()
            throws ProxyDataException
        {
            return getDataManager().getAllHostedRepositories();
        }

        @Override
        public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
            throws ProxyDataException
        {
            return getDataManager().getOrderedConcreteStoresInGroup( groupName );
        }

        @Override
        public Set<Group> getGroupsContaining( final StoreKey repo )
            throws ProxyDataException
        {
            return getDataManager().getGroupsContaining( repo );
        }

        @Override
        public void install()
            throws ProxyDataException
        {
            getDataManager().install();
        }

        @Override
        public void clear()
            throws ProxyDataException
        {
            getDataManager().clear();
        }

        @Override
        public ArtifactStore getArtifactStore( final StoreKey key )
            throws ProxyDataException
        {
            return getDataManager().getArtifactStore( key );
        }

        @Override
        public List<ArtifactStore> getAllArtifactStores()
            throws ProxyDataException
        {
            return getDataManager().getAllArtifactStores();
        }

        @Override
        public List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
            throws ProxyDataException
        {
            return getDataManager().getOrderedStoresInGroup( groupName );
        }

        @Override
        public List<? extends ArtifactStore> getAllArtifactStores( final StoreType type )
            throws ProxyDataException
        {
            return getDataManager().getAllArtifactStores( type );
        }

        @Override
        public boolean storeArtifactStore( final ArtifactStore key )
            throws ProxyDataException
        {
            return getDataManager().storeArtifactStore( key );
        }

        @Override
        public boolean storeArtifactStore( final ArtifactStore key, final boolean skipIfExists )
            throws ProxyDataException
        {
            return getDataManager().storeArtifactStore( key, skipIfExists );
        }

        @Override
        public void deleteArtifactStore( final StoreKey key )
            throws ProxyDataException
        {
            getDataManager().deleteArtifactStore( key );
        }

        @Override
        public boolean hasRemoteRepository( final String name )
        {
            return getDataManager().hasRemoteRepository( name );
        }

        @Override
        public boolean hasGroup( final String name )
        {
            return getDataManager().hasGroup( name );
        }

        @Override
        public boolean hasHostedRepository( final String name )
        {
            return getDataManager().hasHostedRepository( name );
        }

        @Override
        public boolean hasArtifactStore( final StoreKey key )
        {
            return getDataManager().hasArtifactStore( key );
        }

        @Override
        public List<ArtifactStore> getAllConcreteArtifactStores()
            throws ProxyDataException
        {
            return getDataManager().getAllConcreteArtifactStores();
        }

    }

}
