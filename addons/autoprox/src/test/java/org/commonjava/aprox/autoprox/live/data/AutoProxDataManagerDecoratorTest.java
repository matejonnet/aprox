package org.commonjava.aprox.autoprox.live.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.log4j.Level;
import org.commonjava.aprox.autoprox.conf.AutoProxConfiguration;
import org.commonjava.aprox.autoprox.conf.DefaultAutoDeployConfiguration;
import org.commonjava.aprox.autoprox.conf.DefaultAutoGroupConfiguration;
import org.commonjava.aprox.autoprox.conf.DefaultAutoProxConfiguration;
import org.commonjava.aprox.autoprox.conf.DefaultAutoRepoConfiguration;
import org.commonjava.aprox.autoprox.live.fixture.TargetUrlResponder;
import org.commonjava.aprox.core.conf.AproxConfiguration;
import org.commonjava.aprox.core.conf.DefaultAproxConfiguration;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.util.logging.Log4jUtil;
import org.commonjava.web.json.test.WebFixture;
import org.commonjava.web.test.fixture.TestWarArchiveBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class AutoProxDataManagerDecoratorTest
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    @Inject
    protected StoreDataManager proxyManager;

    @Inject
    protected AutoProxConfiguration config;

    @Inject
    protected TargetUrlResponder targetResponder;

    @Rule
    public final WebFixture http = new WebFixture();

    @Before
    public final void setup()
        throws Exception
    {
        Log4jUtil.configure( Level.DEBUG );
        proxyManager.install();

        Repository repo = new Repository( "first", "http://foo.bar/first" );
        proxyManager.storeRepository( repo );

        repo = new Repository( "second", "http://foo.bar/second" );
        proxyManager.storeRepository( repo );
    }

    @After
    public final void teardown()
    {
        targetResponder.clearTargets();
    }

    @Deployment
    public static WebArchive createWar()
    {
        return new TestWarArchiveBuilder( new File( "target/test.war" ), AutoProxDataManagerDecoratorTest.class ).withExtraClasses( TargetUrlResponder.class )
                                                                                                                 .withLog4jProperties()
                                                                                                                 .withBeansXml( "beans.xml.autoprox" )
                                                                                                                 .build();
    }

    @Singleton
    public static final class ConfigProvider
    {
        private AutoProxConfiguration autoProxConfig;

        private AproxConfiguration proxyConfig;

        private final WebFixture http = new WebFixture();

        @Produces
        @Default
        public synchronized AproxConfiguration getProxyConfig()
        {
            if ( proxyConfig == null )
            {
                proxyConfig =
                    new DefaultAproxConfiguration(
                                                   new File(
                                                             System.getProperty( REPO_ROOT_DIR, "target/repo-downloads" ) ) );
            }
            return proxyConfig;
        }

        @Produces
        @Default
        public synchronized AutoProxConfiguration getAutoProxConfig()
            throws MalformedURLException
        {
            if ( autoProxConfig == null )
            {
                autoProxConfig =
                    new DefaultAutoProxConfiguration(
                                                      true,
                                                      new DefaultAutoRepoConfiguration( http.resourceUrl( "target" ) ),
                                                      new DefaultAutoDeployConfiguration( true ),
                                                      new DefaultAutoGroupConfiguration(
                                                                                         new StoreKey(
                                                                                                       StoreType.repository,
                                                                                                       "first" ),
                                                                                         new StoreKey(
                                                                                                       StoreType.repository,
                                                                                                       "second" ) ) );
                System.out.println( "\n\n\n\nSet Autoprox URL: " + autoProxConfig.getRepo()
                                                                                 .getBaseUrl() + "\n\n\n\n" );
            }

            return autoProxConfig;
        }
    }

    @Test
    public void repositoryAutoCreated()
        throws Exception
    {
        final String testUrl = http.resourceUrl( "target", "test" );
        http.get( testUrl, 404 );
        targetResponder.approveTargets( "test" );
        http.get( testUrl, 200 );

        config.setEnabled( false );
        assertThat( proxyManager.getRepository( "test" ), nullValue() );
        config.setEnabled( true );

        final Repository repo = proxyManager.getRepository( "test" );

        assertThat( repo, notNullValue() );
        assertThat( repo.getName(), equalTo( "test" ) );
        assertThat( repo.getUrl(), equalTo( testUrl ) );

    }

    @Test
    public void groupAutoCreatedWithDeployPointAndTwoRepos()
        throws Exception
    {
        final String testUrl = http.resourceUrl( "target", "test" );
        http.get( testUrl, 404 );
        targetResponder.approveTargets( "test" );
        http.get( testUrl, 200 );

        config.setEnabled( false );
        assertThat( proxyManager.getGroup( "test" ), nullValue() );
        config.setEnabled( true );

        final Group group = proxyManager.getGroup( "test" );

        assertThat( group, notNullValue() );
        assertThat( group.getName(), equalTo( "test" ) );

        final List<StoreKey> constituents = group.getConstituents();

        assertThat( constituents, notNullValue() );
        assertThat( constituents.size(), equalTo( 4 ) );

        int idx = 0;
        StoreKey key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.deploy_point ) );
        assertThat( key.getName(), equalTo( "test" ) );

        idx++;
        key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.repository ) );
        assertThat( key.getName(), equalTo( "test" ) );

        idx++;
        key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.repository ) );
        assertThat( key.getName(), equalTo( "first" ) );

        idx++;
        key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.repository ) );
        assertThat( key.getName(), equalTo( "second" ) );
    }

    @Test
    public void repositoryNotAutoCreatedWhenTargetIsInvalid()
        throws Exception
    {
        final String testUrl = http.resourceUrl( "target", "test" );
        http.get( testUrl, 404 );

        config.setEnabled( false );
        assertThat( proxyManager.getRepository( "test" ), nullValue() );
        config.setEnabled( true );

        final Repository repo = proxyManager.getRepository( "test" );

        assertThat( repo, nullValue() );

    }

    @Test
    public void groupNotAutoCreatedWhenTargetIsInvalid()
        throws Exception
    {
        final String testUrl = http.resourceUrl( "target", "test" );
        http.get( testUrl, 404 );

        config.setEnabled( false );
        assertThat( proxyManager.getGroup( "test" ), nullValue() );
        config.setEnabled( true );

        final Group group = proxyManager.getGroup( "test" );

        assertThat( group, nullValue() );
    }

}
