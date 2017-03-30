package org.commonjava.indy.relate.ftest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by ruhan on 2/17/17.
 *
 * <b>GIVEN:</b>
 * <ul>
 *     <li>{@link RemoteRepository} A proxy an upstream server</li>
 *     <li>Path P in {@link RemoteRepository} A points to an POM, registered with an upstream 404 error</li>
 *     <li>Path R in {@link RemoteRepository} A points to the Rel file of the target POM</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Path R is requested from {@link RemoteRepository} A</li>
 *     <li>Path P is registered again with an upstream 200 response</li>
 *     <li>{@link RemoteRepository} A is reset (set disabled to false)</li>
 *     <li>Path R is requested from {@link RemoteRepository} A</li>
 *     <li>Path P is requested from {@link RemoteRepository} A</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>{@link RemoteRepository} A returns null before reset to 200</li>
 *     <li>{@link RemoteRepository} A returns notNull (exists) for Path R after reset</li>
 *     <li>{@link RemoteRepository} A returns notNull (exists) for Path P after reset</li>
 * </ul>
 */
public class RelDownloadPomNotFound404Test
                extends AbstractIndyFunctionalTest
{
    private static final String path = "org/foo/bar/1/bar-1.pom";

    private static final String pathRel = path + ".rel";

    private static final String content =
                    "<project><modelVersion>4.0.0</modelVersion><groupId>org.foo</groupId><artifactId>bar</artifactId><version>1</version></project>";

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void run() throws Exception
    {
        final String repo1 = "repo1";

        server.expect( server.formatUrl( repo1, path ), 404, "not found" );

        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        client.stores().create( remote1, "adding remote", RemoteRepository.class );

        // Download .rel before even touching POM
        InputStream rel = client.content().get( remote, repo1, pathRel );
        assertThat( rel, nullValue() );

        logger.debug( ">>> register again ..." );
        server.expect( server.formatUrl( repo1, path ), 200, content ); // upstream restored
        remote1.setDisabled( false );
        client.stores().update( remote1, "enable it" ); // disabled due to previous error, need to enable it again

        // Retrieve it again
        rel = client.content().get( remote, repo1, pathRel );
        assertThat( rel, notNullValue() );
        String s = IOUtils.toString( rel );
        logger.debug( ">>> " + s );
        assertThat( StringUtils.isNotEmpty( s ), equalTo( true ) );

        // check POM is downloaded
        boolean exists = client.content().exists( remote, repo1, path, true );
        assertThat( exists, equalTo( true ) );
    }
}
