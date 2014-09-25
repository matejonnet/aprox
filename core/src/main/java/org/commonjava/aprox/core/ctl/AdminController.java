/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.core.ctl;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.action.start.AproxInitException;
import org.commonjava.aprox.action.start.MigrationAction;
import org.commonjava.aprox.action.start.StartupAction;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.core.expire.ScheduleManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AdminController
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    /* Injected to make sure this gets initialized up front. */
    @SuppressWarnings( "unused" )
    @Inject
    private ScheduleManager scheduleManager;

    @Inject
    private AProxVersioning versioning;

    @Inject
    private Instance<MigrationAction> migrationActions;

    @Inject
    private Instance<StartupAction> startupActions;

    protected AdminController()
    {
    }

    public AdminController( final StoreDataManager storeManager, final ScheduleManager scheduleManager,
                            final AProxVersioning versioning )
    {
        this.storeManager = storeManager;
        this.scheduleManager = scheduleManager;
        this.versioning = versioning;
    }

    public boolean store( final ArtifactStore store, final String user, final boolean skipExisting )
        throws AproxWorkflowException
    {
        try
        {
            String changelog = store.getMetadata( ArtifactStore.METADATA_CHANGELOG );
            if ( changelog == null )
            {
                changelog = "Changelog not provided";
            }

            final ChangeSummary summary = new ChangeSummary( user, changelog );

            return storeManager.storeArtifactStore( store, summary, skipExisting );
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

    public void delete( final StoreKey key, final String user, final String changelog )
        throws AproxWorkflowException
    {
        try
        {
            storeManager.deleteArtifactStore( key, new ChangeSummary( user, changelog ) );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to delete: {}. Reason: {}", e, key, e.getMessage() );
        }
    }

    public void started()
        throws AproxInitException
    {
        logger.info( "\n\n\n\n\n STARTING AProx\n    Version: {}\n    Built-By: {}\n    Commit-ID: {}\n    Built-On: {}\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(),
                     versioning.getTimestamp() );

        runMigrationActions();
        runStartupActions();

        final ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Initializing default data." );

        try
        {
            logger.info( "Verfiying that AProx DB + basic data is installed..." );
            storeManager.install();

            if ( !storeManager.hasRemoteRepository( "central" ) )
            {
                final RemoteRepository central =
                    new RemoteRepository( "central", "http://repo.maven.apache.org/maven2/" );
                central.setCacheTimeoutSeconds( 86400 );
                storeManager.storeRemoteRepository( central, summary, true );
            }

            if ( !storeManager.hasHostedRepository( "local-deployments" ) )
            {
                final HostedRepository local = new HostedRepository( "local-deployments" );
                local.setAllowReleases( true );
                local.setAllowSnapshots( true );
                local.setSnapshotTimeoutSeconds( 86400 );

                storeManager.storeHostedRepository( local, summary, true );
            }

            if ( !storeManager.hasGroup( "public" ) )
            {
                final Group pub = new Group( "public" );
                pub.addConstituent( new StoreKey( StoreType.remote, "central" ) );
                pub.addConstituent( new StoreKey( StoreType.hosted, "local-deployments" ) );

                storeManager.storeGroup( pub, summary, true );
            }
        }
        catch ( final ProxyDataException e )
        {
            throw new RuntimeException( "Failed to boot aprox components: " + e.getMessage(), e );
        }

        logger.info( "...done." );
    }

    private void runMigrationActions()
        throws AproxInitException
    {
        boolean changed = false;
        if ( migrationActions != null )
        {
            logger.info( "Running migration actions..." );
            for ( final MigrationAction action : migrationActions )
            {
                logger.info( "Running migration action: '{}'", action.getId() );
                changed = action.migrate() || changed;
            }
        }
    }

    private void runStartupActions()
        throws AproxInitException
    {
        if ( startupActions != null )
        {
            logger.info( "Running startup actions..." );
            for ( final StartupAction action : startupActions )
            {
                logger.info( "Running startup action: '{}'", action.getId() );
                action.start();
            }
        }
    }

    public void stopped()
    {
        logger.info( "\n\n\n\n\n SHUTTING DOWN AProx\n    Version: {}\n    Built-By: {}\n    Commit-ID: {}\n    Built-On: {}\n\n\n\n\n",
                     versioning.getVersion(), versioning.getBuilder(), versioning.getCommitId(), versioning.getTimestamp() );
    }

}