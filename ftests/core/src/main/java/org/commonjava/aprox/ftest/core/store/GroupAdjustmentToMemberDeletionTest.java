/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.junit.Test;

public class GroupAdjustmentToMemberDeletionTest
    extends AbstractStoreManagementTest
{

    @Test
    public void groupAdjustsToConstituentDeletion()
        throws Exception
    {
        final HostedRepository repo = new HostedRepository( newName() );
        final Group group = new Group( newName() );
        group.addConstituent( repo );

        assertThat( client.stores()
                          .create( repo, name.getMethodName(), HostedRepository.class ), notNullValue() );
        assertThat( client.stores()
                          .create( group, name.getMethodName(), Group.class ), notNullValue() );

        client.stores()
              .delete( repo.getKey()
                           .getType(), repo.getName(), name.getMethodName() );

        final Group result = client.stores()
                                           .load( group.getKey()
                                               .getType(), group.getName(), Group.class );

        assertThat( result.getConstituents() == null || result.getConstituents()
                                                              .isEmpty(), equalTo( true ) );
    }
}
