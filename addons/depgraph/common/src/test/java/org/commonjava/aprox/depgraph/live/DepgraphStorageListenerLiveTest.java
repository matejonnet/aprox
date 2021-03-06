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
package org.commonjava.aprox.depgraph.live;

import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;

import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.DepgraphStorageListener;
import org.commonjava.aprox.depgraph.fixture.TestConfigProvider;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.web.json.test.WebFixture;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

//@RunWith( Arquillian.class )
@Ignore
public class DepgraphStorageListenerLiveTest
{

    @Inject
    private DepgraphStorageListener listener;

    @Inject
    private StoreDataManager aproxData;

    @Inject
    private TransferManager transfers;

    private static File repoRoot;

    @Rule
    public WebFixture webFixture = new WebFixture();

    public @Rule
    TestName testName = new TestName();

    @BeforeClass
    public static void setRepoRootDir()
        throws IOException
    {
        repoRoot = File.createTempFile( "repo.root.", ".dir" );
        System.setProperty( TestConfigProvider.REPO_ROOT_DIR, repoRoot.getAbsolutePath() );
    }

    @AfterClass
    public static void clearRepoRootDir()
        throws IOException
    {
        if ( repoRoot != null && repoRoot.exists() )
        {
            forceDelete( repoRoot );
        }
    }

    @Before
    public final void setupAProxLiveTest()
        throws Exception
    {
        System.out.println( "[" + testName.getMethodName() + "] Setting up..." );
        aproxData.install();
        aproxData.clear();
    }

    //    @Deployment
    //    public static WebArchive createWar()
    //    {
    //        return new TestWarArchiveBuilder( new File( "target/test-assembly.war" ), DepgraphStorageListenerLiveTest.class ).withExtraClasses( TestConfigProvider.class/*,
    //                                                                                                                                                                    TestDiscoverer.class*/)
    //                                                                                                                         .withLog4jProperties()
    //                                                                                                                         //                                                                                                                       .withClassloaderResources( "arquillian.xml" )
    //                                                                                                                         .withBeansXml( "META-INF/beans.xml" )
    //                                                                                                                         .build();
    //    }

    @Test
    public void readPomWithoutParent()
        throws Exception
    {
        final InputStream is = Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResourceAsStream( "identity/no-parent-1.0.pom" );
        final HostedRepository dp = new HostedRepository( "test" );

        aproxData.storeHostedRepository( dp );

        final String path = "/org/test/no-parent/1.0/no-parent-1.0.pom";
        final Transfer ref = transfers.getCacheReference( new ConcreteResource( LocationUtils.toLocation( dp ), path ) );
        OutputStream os = null;
        try
        {
            os = ref.openOutputStream( TransferOperation.DOWNLOAD, false );
            copy( is, os );
        }
        finally
        {
            closeQuietly( is );
            closeQuietly( os );
        }

        listener.handleFileAccessEvent( new FileAccessEvent( ref ) );
    }

    @Test
    public void readPomWithParent()
        throws Exception
    {
        final InputStream is = Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResourceAsStream( "identity/with-parent-1.0.pom" );
        final HostedRepository dp = new HostedRepository( "test" );

        aproxData.storeHostedRepository( dp );

        final String path = "/org/test/with-parent/1.0/with-parent-1.0.pom";
        final Transfer ref = transfers.getCacheReference( new ConcreteResource( LocationUtils.toLocation( dp ), path ) );
        OutputStream os = null;
        try
        {
            os = ref.openOutputStream( TransferOperation.DOWNLOAD, false );
            copy( is, os );
        }
        finally
        {
            closeQuietly( is );
            closeQuietly( os );
        }

        listener.handleFileAccessEvent( new FileAccessEvent( ref ) );
    }

    //    @Test
    //    public void injectDependenciesOfDownloadedPOM()
    //        throws Exception
    //    {
    //        proxyManager.storeRepository( new Repository( "central", "http://repo.maven.apache.org/maven2/" ) );
    //        proxyManager.storeGroup( new Group( "test", new StoreKey( StoreType.repository, "central" ) ) );
    //
    //        webFixture.get( webFixture.resourceUrl( "group", "test",
    //                                                "org/apache/maven/maven-core/3.0.3/maven-core-3.0.3.pom" ), 200 );
    //
    //        // dataManager.get
    //    }

}
