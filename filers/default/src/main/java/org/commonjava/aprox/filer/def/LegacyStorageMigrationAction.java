package org.commonjava.aprox.filer.def;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.action.start.MigrationAction;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.aprox.model.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named( "legacy-storage-migration" )
public class LegacyStorageMigrationAction
    implements MigrationAction
{

    private static final String LEGACY_HOSTED_REPO_PREFIX = "deploy_point";

    private static final String LEGACY_REMOTE_REPO_PREFIX = "repository";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DefaultStorageProviderConfiguration config;

    @Override
    public String getId()
    {
        return "Legacy storage-location migrator";
    }

    @Override
    public boolean execute()
    {
        final File basedir = config.getStorageRootDirectory();
        if ( !basedir.exists() )
        {
            return false;
        }

        final File[] dirs = basedir.listFiles();
        if ( dirs == null || dirs.length < 1 )
        {
            return false;
        }

        boolean changed = false;
        for ( final File dir : dirs )
        {
            final String name = dir.getName();
            String newName = null;
            if ( name.startsWith( LEGACY_HOSTED_REPO_PREFIX ) )
            {
                newName = StoreType.hosted.singularEndpointName() + name.substring( LEGACY_HOSTED_REPO_PREFIX.length() );
            }
            else if ( name.startsWith( LEGACY_REMOTE_REPO_PREFIX ) )
            {
                newName = StoreType.remote.singularEndpointName() + name.substring( LEGACY_REMOTE_REPO_PREFIX.length() );
            }

            if ( newName != null )
            {
                logger.info( "Migrating storage: '{}' to '{}'", name, newName );

                final File newDir = new File( basedir, newName );
                dir.renameTo( newDir );

                changed = true;
            }
        }

        return changed;

    }

}
