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
import java.io.IOException;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileConfiguration;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Decorator
public abstract class FlatFileDataManagerDecorator
    implements StoreDataManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Delegate
    @Any
    @Inject
    private StoreDataManager dataManager;

    @Inject
    private FlatFileConfiguration config;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    protected FlatFileDataManagerDecorator()
    {
    }

    protected FlatFileDataManagerDecorator( final StoreDataManager dataManager, final FlatFileConfiguration config, final JsonSerializer serializer )
    {
        this.dataManager = dataManager;
        this.config = config;
        this.serializer = serializer;
    }

    protected final StoreDataManager getDataManager()
    {
        return dataManager;
    }

    @PostConstruct
    public void readDefinitions()
        throws ProxyDataException
    {
        final File basedir = config.getStorageDir( FlatFileStoreDataManager.APROX_STORE );
        final File ddir = new File( basedir, StoreType.hosted.name() );

        final String[] dFiles = ddir.list();
        if ( dFiles != null )
        {
            for ( final String file : dFiles )
            {
                final File f = new File( ddir, file );
                try
                {
                    final String json = FileUtils.readFileToString( f );
                    final HostedRepository dp = serializer.fromString( json, HostedRepository.class );
                    if ( dp == null )
                    {
                        f.delete();
                    }
                    else
                    {
                        dataManager.storeHostedRepository( dp );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error( String.format( "Failed to load deploy point: %s. Reason: %s", f, e.getMessage() ), e );
                }
            }
        }

        final File rdir = new File( basedir, StoreType.remote.name() );
        final String[] rFiles = rdir.list();
        if ( rFiles != null )
        {
            for ( final String file : rFiles )
            {
                final File f = new File( rdir, file );
                try
                {
                    final String json = FileUtils.readFileToString( f );
                    final RemoteRepository r = serializer.fromString( json, RemoteRepository.class );
                    if ( r == null )
                    {
                        f.delete();
                    }
                    else
                    {
                        dataManager.storeRemoteRepository( r );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error( String.format( "Failed to load repository: %s. Reason: %s", f, e.getMessage() ), e );
                }
            }
        }

        final File gdir = new File( basedir, StoreType.group.name() );
        final String[] gFiles = gdir.list();
        if ( gFiles != null )
        {
            for ( final String file : gFiles )
            {
                final File f = new File( gdir, file );
                try
                {
                    final String json = FileUtils.readFileToString( f );
                    final Group g = serializer.fromString( json, Group.class );
                    if ( g == null )
                    {
                        f.delete();
                    }
                    else
                    {
                        dataManager.storeGroup( g );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error( String.format( "Failed to load group: %s. Reason: %s", f, e.getMessage() ), e );
                }
            }
        }
    }

    @Override
    public void storeHostedRepositories( final Collection<HostedRepository> deploys )
        throws ProxyDataException
    {
        dataManager.storeHostedRepositories( deploys );
        store( false, deploys.toArray( new ArtifactStore[] {} ) );
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository deploy )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeHostedRepository( deploy );
        if ( result )
        {
            store( false, deploy );
        }

        return result;
    }

    @Override
    public boolean storeHostedRepository( final HostedRepository deploy, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeHostedRepository( deploy, skipIfExists );
        if ( result )
        {
            store( skipIfExists, deploy );
        }

        return result;
    }

    @Override
    public void storeRemoteRepositories( final Collection<RemoteRepository> repos )
        throws ProxyDataException
    {
        dataManager.storeRemoteRepositories( repos );
        store( false, repos.toArray( new ArtifactStore[] {} ) );
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository proxy )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeRemoteRepository( proxy );
        if ( result )
        {
            store( false, proxy );
        }

        return result;
    }

    @Override
    public boolean storeRemoteRepository( final RemoteRepository repository, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeRemoteRepository( repository, skipIfExists );
        if ( result )
        {
            store( skipIfExists, repository );
        }

        return result;
    }

    @Override
    public void storeGroups( final Collection<Group> groups )
        throws ProxyDataException
    {
        dataManager.storeGroups( groups );
        store( false, groups.toArray( new ArtifactStore[] {} ) );
    }

    @Override
    public boolean storeGroup( final Group group )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeGroup( group );
        if ( result )
        {
            store( false, group );
        }

        return result;
    }

    @Override
    public boolean storeGroup( final Group group, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeGroup( group, skipIfExists );
        if ( result )
        {
            store( false, group );
        }

        return result;
    }

    @Override
    public void deleteHostedRepository( final HostedRepository deploy )
        throws ProxyDataException
    {
        dataManager.deleteHostedRepository( deploy );
        delete( deploy );
    }

    @Override
    public void deleteHostedRepository( final String name )
        throws ProxyDataException
    {
        dataManager.deleteHostedRepository( name );
        delete( StoreType.hosted, name );
    }

    @Override
    public void deleteRemoteRepository( final RemoteRepository repo )
        throws ProxyDataException
    {
        dataManager.deleteRemoteRepository( repo );
        delete( repo );
    }

    @Override
    public void deleteRemoteRepository( final String name )
        throws ProxyDataException
    {
        dataManager.deleteRemoteRepository( name );
        delete( StoreType.remote, name );
    }

    @Override
    public void deleteGroup( final Group group )
        throws ProxyDataException
    {
        dataManager.deleteGroup( group );
        delete( group );
    }

    @Override
    public void deleteGroup( final String name )
        throws ProxyDataException
    {
        dataManager.deleteGroup( name );
        delete( StoreType.group, name );
    }

    private void store( final boolean skipIfExists, final ArtifactStore... stores )
        throws ProxyDataException
    {
        final File basedir = config.getStorageDir( FlatFileStoreDataManager.APROX_STORE );
        for ( final ArtifactStore store : stores )
        {
            final File dir = new File( basedir, store.getDoctype()
                                                     .name() );
            if ( !dir.isDirectory() && !dir.mkdirs() )
            {
                throw new ProxyDataException( "Cannot create storage directory: {} for definition: {}", dir, store );
            }

            final File f = new File( dir, store.getName() + ".json" );
            if ( skipIfExists && f.exists() )
            {
                continue;
            }

            try
            {
                FileUtils.write( f, serializer.toString( store ), "UTF-8" );
            }
            catch ( final IOException e )
            {
                throw new ProxyDataException( "Cannot write definition: {} to: {}. Reason: {}", e, store, f, e.getMessage() );
            }
        }
    }

    private void delete( final ArtifactStore... stores )
    {
        final File basedir = config.getStorageDir( FlatFileStoreDataManager.APROX_STORE );
        for ( final ArtifactStore store : stores )
        {
            final File dir = new File( basedir, store.getDoctype()
                                                     .name() );

            final File f = new File( dir, store.getName() + ".json" );
            if ( f.exists() )
            {
                f.delete();
            }

        }
    }

    private void delete( final StoreType type, final String name )
    {
        final File basedir = config.getStorageDir( FlatFileStoreDataManager.APROX_STORE );
        final File dir = new File( basedir, type.name() );

        final File f = new File( dir, name + ".json" );
        if ( f.exists() )
        {
            f.delete();
        }
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore store )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeArtifactStore( store );
        if ( result )
        {
            store( false, store );
        }

        return result;
    }

    @Override
    public boolean storeArtifactStore( final ArtifactStore store, final boolean skipIfExists )
        throws ProxyDataException
    {
        final boolean result = dataManager.storeArtifactStore( store );
        if ( result )
        {
            store( false, store );
        }

        return result;
    }

    @Override
    public void deleteArtifactStore( final StoreKey key )
        throws ProxyDataException
    {
        dataManager.deleteArtifactStore( key );
        delete( key.getType(), key.getName() );
    }

    @Override
    public void clear()
        throws ProxyDataException
    {
        dataManager.clear();

        final File basedir = config.getStorageDir( FlatFileStoreDataManager.APROX_STORE );
        try
        {
            FileUtils.forceDelete( basedir );
        }
        catch ( final IOException e )
        {
            throw new ProxyDataException( "Failed to delete AProx storage files: {}", e, e.getMessage() );
        }
    }

    @Override
    public void install()
        throws ProxyDataException
    {
        if ( !config.getStorageDir( FlatFileStoreDataManager.APROX_STORE )
                    .isDirectory() )
        {
            dataManager.storeRemoteRepository( new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" ), true );
            dataManager.storeGroup( new Group( "public", new StoreKey( StoreType.remote, "central" ) ), true );
        }
    }

    @Override
    public void reload()
        throws ProxyDataException
    {
        dataManager.clear();
        readDefinitions();
    }

}
