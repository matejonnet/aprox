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
package org.commonjava.aprox.core.rest;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.action.start.MigrationAction;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.stats.AProxVersioning;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.shelflife.ExpirationManager;
import org.commonjava.shelflife.ExpirationManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AdminController
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private ExpirationManager expirationManager;

    @Inject
    private AProxVersioning versioning;

    @Inject
    private Instance<MigrationAction> migrationActions;

    protected AdminController()
    {
    }

    public AdminController( final StoreDataManager storeManager, final ExpirationManager expirationManager, final AProxVersioning versioning )
    {
        this.storeManager = storeManager;
        this.expirationManager = expirationManager;
        this.versioning = versioning;
    }

    public boolean store( final ArtifactStore store, final boolean skipExisting )
        throws AproxWorkflowException
    {
        try
        {
            return storeManager.storeArtifactStore( store, skipExisting );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to store: {}. Reason: {}", e, store.getKey(), e.getMessage() );
        }
    }

    public List<? extends ArtifactStore> getAllOfType( final StoreType type )
        throws AproxWorkflowException
    {
        try
        {
            return storeManager.getAllArtifactStores( type );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to list: {}. Reason: {}", e, type, e.getMessage() );
        }
    }

    public ArtifactStore get( final StoreKey key )
        throws AproxWorkflowException
    {
        try
        {
            return storeManager.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve: {}. Reason: {}", e, key, e.getMessage() );
        }
    }

    public void delete( final StoreKey key )
        throws AproxWorkflowException
    {
        try
        {
            storeManager.deleteArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to delete: {}. Reason: {}", e, key, e.getMessage() );
        }
    }

    public void started()
    {
        logger.info( "\n\n\n\n\n STARTING AProx\n    Version: {}\n    Built-By: {}\n    Commit-ID: {}\n    Built-On: {}\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(), versioning.getTimestamp() );

        runMigrationActions();

        try
        {
            logger.info( "Verfiying that AProx DB + basic data is installed..." );
            storeManager.install();

            final RemoteRepository central = new RemoteRepository( "central", "http://repo.maven.apache.org/maven2/" );
            central.setCacheTimeoutSeconds( 86400 );
            storeManager.storeRemoteRepository( central, true );

            final HostedRepository local = new HostedRepository( "local-deployments" );
            local.setAllowReleases( true );
            local.setAllowSnapshots( true );
            local.setSnapshotTimeoutSeconds( 86400 );

            storeManager.storeHostedRepository( local, true );

            final Group pub = new Group( "public" );
            pub.addConstituent( central );
            pub.addConstituent( local );

            storeManager.storeGroup( pub, true );

            // make sure the expiration manager is running...
            expirationManager.loadNextExpirations();
        }
        catch ( final ExpirationManagerException e )
        {
            throw new RuntimeException( "Failed to boot aprox components: " + e.getMessage(), e );
        }
        catch ( final ProxyDataException e )
        {
            throw new RuntimeException( "Failed to boot aprox components: " + e.getMessage(), e );
        }

        logger.info( "...done." );
    }

    private void runMigrationActions()
    {
        boolean changed = false;
        if ( migrationActions != null )
        {
            logger.info( "Running migration actions..." );
            for ( final MigrationAction action : migrationActions )
            {
                logger.info( "Running migration action: '{}'", action.getId() );
                changed = action.execute() || changed;
            }
        }
    }

    public void stopped()
    {
        logger.info( "\n\n\n\n\n SHUTTING DOWN AProx\n    Version: {}\n    Built-By: {}\n    Commit-ID: {}\n    Built-On: {}\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(), versioning.getTimestamp() );
    }

}
