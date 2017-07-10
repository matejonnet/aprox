package org.commonjava.indy.mem.data;

import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.PackageTypes;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.util.UrlInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;

/**
 * This query interface is intended to be reusable across any {@link StoreDataManager} implementation. It contains logic
 * for working with the {@link ArtifactStore}s contained in the StoreDataManager, but this logic is not tied directly to
 * any data manager implementation. Separating these methods out and providing a single {@link StoreDataManager#query()}
 * method to provide access to an instance of this class drastically simplifies the task of implementing a new type of
 * data manager.
 * <p>
 * This class is intended to function as a fluent api. It does keep state on packageType, storeType(s), and
 * enabled / disabled selection. However, methods that pertain to specific types of ArtifactStore (indicated by their
 * names) DO NOT change the internal state of the query instance on which they are called.
 * <p>
 * Created by jdcasey on 5/10/17.
 */
// TODO: Eventually, it should probably be an error if packageType isn't set explicitly
public class MemoryArtifactStoreQuery<T extends ArtifactStore>
        implements ArtifactStoreQuery<T>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private StoreDataManager dataManager;

    private String packageType = MAVEN_PKG_KEY;

    private Set<StoreType> types;

    private Boolean enabled;

    public MemoryArtifactStoreQuery( StoreDataManager dataManager )
    {
        this.dataManager = dataManager;
    }

    private MemoryArtifactStoreQuery( final StoreDataManager dataManager, final String packageType,
                                      final Boolean enabled, final Class<T> storeCls )
    {
        this.dataManager = dataManager;
        this.packageType = packageType;
        this.enabled = enabled;
        storeType( storeCls );
    }

    @Override
    public ArtifactStoreQuery<T> rewrap( final StoreDataManager manager )
    {
        this.dataManager = manager;
        return this;
    }

    @Override
    public MemoryArtifactStoreQuery<T> packageType( String packageType )
            throws IndyDataException
    {
        if ( !PackageTypes.contains( packageType ) )
        {
            throw new IndyDataException( "Invalid package type: %s. Supported values are: %s", packageType,
                                         PackageTypes.getPackageTypes() );
        }

        this.packageType = packageType;
        return this;
    }

    @Override
    public <C extends ArtifactStore> MemoryArtifactStoreQuery<C> storeType( Class<C> storeCls )
    {
        if ( RemoteRepository.class.equals( storeCls ) )
        {
            this.types = Collections.singleton( remote );
        }
        else if ( HostedRepository.class.equals( storeCls ) )
        {
            this.types = Collections.singleton( hosted );
        }
        else
        {
            this.types = Collections.singleton( group );
        }

        return (MemoryArtifactStoreQuery<C>) this;
    }

    @Override
    public MemoryArtifactStoreQuery<T> storeTypes( StoreType... types )
    {
        this.types = new HashSet<>( Arrays.asList( types ) );
        return this;
    }

    @Override
    public ArtifactStoreQuery<T> concreteStores()
    {
        return storeTypes( remote, hosted );
    }

    @Override
    public ArtifactStoreQuery<T> enabledState( Boolean enabled )
    {
        this.enabled = enabled;
        return this;
    }

    @Override
    public List<T> getAll()
            throws IndyDataException
    {
        return stream().collect( Collectors.toList() );
    }

    @Override
    public Stream<T> stream()
            throws IndyDataException
    {
        return stream( store -> true );
    }

    @Override
    public Stream<T> stream( Predicate<ArtifactStore> filter )
            throws IndyDataException
    {
        /* @formatter:off */
        return dataManager.streamArtifactStores().filter( ( store ) ->
        {
            logger.debug( "Checking whether {} is included in stream...", store.getKey() );

            // Tricky condition here: The flag in the store we're checking is true when DISABLED, while the
            // condition we're checking against in this query is true when it's ENABLED. If the two flags equal on another
            // that actually means they DISAGREE about the state vs. desired state of the store.
            if ( enabled != null && enabled == store.isDisabled() )
            {
                logger.debug( "Rejected. Store is {}, and we're only looking for enabled state of: {}", store.isDisabled(), enabled );
                return false;
            }

            if ( packageType != null && !packageType.equals( store.getPackageType() ) )
            {
                logger.debug( "Rejected. Store package type is: {}, and we're only looking for package type of: {}", store.getPackageType(), packageType );
                return false;
            }

            if ( types != null && !types.contains( store.getType() ) )
            {
                logger.debug( "Rejected. Store is of type: {}, and we're only looking for: {}", store.getType(), types );
                return false;
            }

            if ( filter != null && !filter.test( store ))
            {
                logger.debug( "Rejected. Additional filtering failed for store: {}", store.getKey() );
                return false;
            }

            logger.debug( "Store accepted for stream: {}", store.getKey() );
            return true;
        } ).map( store -> (T) store );
        /* @formatter:on */
    }

    @Override
    public List<T> getAll( Predicate<ArtifactStore> filter )
            throws IndyDataException
    {
        return stream( filter ).collect( Collectors.toList() );
    }

    @Override
    public List<String> allDefaultPackageTypes()
    {
        return Arrays.asList( MAVEN_PKG_KEY, NPM_PKG_KEY );
    }

    @Override
    public List<T> getAllByDefaultPackageTypes()
            throws IndyDataException
    {
        List<T> result = new ArrayList<T>();
        List<String> defaults = allDefaultPackageTypes();
        for ( String packageType : defaults )
        {
            this.packageType = packageType;
            result.addAll( stream().collect( Collectors.toList() ) );
        }
        return result;
    }

    @Override
    public T getByName( String name )
            throws IndyDataException
    {
        return stream( store -> name.equals( store.getName() ) ).findFirst().orElse( null );
    }

    @Override
    public boolean containsByName( String name )
            throws IndyDataException
    {
        return getByName( name ) != null;
    }

    @Override
    public Set<Group> getGroupsContaining( StoreKey storeKey )
            throws IndyDataException
    {
        return new MemoryArtifactStoreQuery<Group>( dataManager, storeKey.getPackageType(), enabled,
                                                    Group.class ).stream(
                store -> ( (Group) store ).getConstituents().contains( storeKey ) ).collect( Collectors.toSet() );
    }

    @Override
    public RemoteRepository getRemoteRepositoryByUrl( String url )
            throws IndyDataException
    {
        /*
           This filter does these things:
             * First compare ip, if ip same, and the path(without last slash) same too, the repo is found
             * If ip not same, then compare the url without scheme and last slash (if has) to find the repo
         */
        UrlInfo temp = null;
        try
        {
            temp = new UrlInfo( url );
        }
        catch ( IllegalArgumentException error )
        {
            logger.error( "Failed to find repository for: '{}'. Reason: {}", error, url, error.getMessage() );
        }

        final UrlInfo urlInfo = temp;

        /* @formatter:off */
        return new MemoryArtifactStoreQuery<>( dataManager, packageType, enabled, RemoteRepository.class ).stream(
                store -> {
                    if ( ( remote == store.getType() ) && urlInfo != null )
                    {
                        final String targetUrl = ( (RemoteRepository) store ).getUrl();
                        UrlInfo targetUrlInfo = null;
                        try
                        {
                            targetUrlInfo = new UrlInfo( targetUrl );
                        }
                        catch ( IllegalArgumentException error )
                        {
                            logger.error( "Failed to find repository for: '{}'. Reason: {}", error, targetUrl, error.getMessage() );
                        }

                        if (  targetUrlInfo != null )
                        {
                            String ipForUrl = null;
                            String ipForTargetUrl = null;
                            try
                            {
                                ipForUrl = urlInfo.getIpForUrl();
                                ipForTargetUrl = targetUrlInfo.getIpForUrl();
                                if ( ipForUrl != null && ipForUrl.equals( ipForTargetUrl ) )
                                {
                                    if ( urlInfo.getFileWithNoLastSlash().equals( targetUrlInfo.getFileWithNoLastSlash() ) )
                                    {
                                        logger.debug( "Repository found because of same ip, url is {}, store key is {}", url,
                                                      store.getKey() );
                                        return true;
                                    }
                                    else
                                    {
                                        return false;
                                    }
                                }
                            }
                            catch ( UnknownHostException ue )
                            {
                                logger.warn( "Failed to filter remote: ip fetch error.", ue );
                            }

                            logger.debug( "ip not same: ip for url:{}-{}; ip for searching repo: {}-{}", url, ipForUrl,
                                          store.getKey(), ipForTargetUrl );

                            if ( urlInfo.getUrlWithNoSchemeAndLastSlash()
                                        .equals( targetUrlInfo.getUrlWithNoSchemeAndLastSlash() ) )
                            {
                                logger.debug( "Repository found because of same host, url is {}, store key is {}", url,
                                              store.getKey() );
                                return true;
                            }
                        }
                    }

                    return false;
                } ).findFirst().orElse( null );
        /* @formatter:on */
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
            throws IndyDataException
    {
        Map<StoreKey, ArtifactStore> stores = new HashMap<>();
        stream().forEach( s -> stores.put( s.getKey(), s ) );

        return getGroupOrdering( groupName, stores, false, true );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
            throws IndyDataException
    {
        Map<StoreKey, ArtifactStore> stores = new HashMap<>();
        stream().forEach( s -> stores.put( s.getKey(), s ) );

        return getGroupOrdering( groupName, stores, true, false );
    }

    @Override
    public Set<Group> getGroupsAffectedBy( StoreKey... keys )
            throws IndyDataException
    {
        return getGroupsAffectedBy( Arrays.asList( keys ) );
    }

    @Override
    public Set<Group> getGroupsAffectedBy( Collection<StoreKey> keys )
            throws IndyDataException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Getting groups affected by: {}", keys );

        List<StoreKey> toProcess = new ArrayList<>();
        toProcess.addAll( keys.stream().collect( Collectors.toSet() ) );

        Set<Group> groups = new HashSet<>();
        if ( toProcess.isEmpty() )
        {
            return groups;
        }

        Set<StoreKey> processed = new HashSet<>();

        Set<Group> all = new MemoryArtifactStoreQuery<Group>( dataManager, toProcess.get( 0 ).getPackageType(), null,
                                                              Group.class ).stream().collect( Collectors.toSet() );

        while ( !toProcess.isEmpty() )
        {
            // as long as we have another key to process, pop it off the list (remove it) and process it.
            StoreKey next = toProcess.remove( 0 );
            if ( processed.contains( next ) )
            {
                // if we've already handled this group (via another branch in the group membership tree, etc. then don't bother.
                continue;
            }

            // use this to avoid reprocessing groups we've already encountered.
            processed.add( next );

            for ( ArtifactStore store : all )
            {
                if ( !processed.contains( store.getKey() ) && ( store instanceof Group ) )
                {
                    Group g = (Group) store;
                    if ( g.getConstituents() != null && g.getConstituents().contains( next ) )
                    {
                        groups.add( g );

                        // add this group as another one to process for groups that contain it...and recurse upwards
                        toProcess.add( g.getKey() );
                    }
                }
            }
        }

        return groups;
    }

    @Override
    public List<RemoteRepository> getAllRemoteRepositories()
            throws IndyDataException
    {
        return new MemoryArtifactStoreQuery<RemoteRepository>( dataManager, packageType, enabled,
                                                               RemoteRepository.class ).getAll();
    }

    @Override
    public List<HostedRepository> getAllHostedRepositories()
            throws IndyDataException
    {
        return new MemoryArtifactStoreQuery<HostedRepository>( dataManager, packageType, enabled,
                                                               HostedRepository.class ).getAll();
    }

    @Override
    public List<Group> getAllGroups()
            throws IndyDataException
    {
        return new MemoryArtifactStoreQuery<Group>( dataManager, packageType, enabled, Group.class ).getAll();
    }

    @Override
    public RemoteRepository getRemoteRepository( final String name )
            throws IndyDataException
    {
        return (RemoteRepository) dataManager.getArtifactStore( new StoreKey( packageType, remote, name ) );
    }

    @Override
    public HostedRepository getHostedRepository( final String name )
            throws IndyDataException
    {
        return (HostedRepository) dataManager.getArtifactStore( new StoreKey( packageType, hosted, name ) );
    }

    @Override
    public Group getGroup( final String name )
            throws IndyDataException
    {
        return (Group) dataManager.getArtifactStore( new StoreKey( packageType, group, name ) );
    }

    @Override
    public MemoryArtifactStoreQuery<T> noPackageType()
    {
        packageType = null;
        return this;
    }

    private List<ArtifactStore> getGroupOrdering( final String groupName, final Map<StoreKey, ArtifactStore> stores,
                                                  final boolean includeGroups, final boolean recurseGroups )
            throws IndyDataException
    {
        if ( packageType == null )
        {
            throw new IndyDataException( "packageType must be set on the query before calling this method!" );
        }

        final Group master = (Group) stores.get( new StoreKey( packageType, StoreType.group, groupName ) );
        if ( master == null )
        {
            return Collections.emptyList();
        }

        final List<ArtifactStore> result = new ArrayList<>();
        recurseGroup( master, stores, result, new HashSet<>(), includeGroups, recurseGroups );

        return result;
    }

    private void recurseGroup( final Group master, final Map<StoreKey, ArtifactStore> stores,
                               final List<ArtifactStore> result, final Set<StoreKey> seen, final boolean includeGroups,
                               final boolean recurseGroups )
    {
        if ( master == null || master.isDisabled() && enabled )
        {
            return;
        }

        List<StoreKey> members = new ArrayList<>( master.getConstituents() );
        if ( includeGroups )
        {
            result.add( master );
        }

        members.forEach( ( key ) ->
                         {
                             if ( !seen.contains( key ) )
                             {
                                 seen.add( key );
                                 final StoreType type = key.getType();
                                 if ( recurseGroups && type == StoreType.group )
                                 {
                                     // if we're here, we're definitely recursing groups...
                                     recurseGroup( (Group) stores.get( key ), stores, result, seen, includeGroups,
                                                   true );
                                 }
                                 else
                                 {
                                     final ArtifactStore store = stores.get( key );
                                     if ( store != null && !( store.isDisabled() && enabled ) )
                                     {
                                         result.add( store );
                                     }
                                 }
                             }
                         } );
    }

}
