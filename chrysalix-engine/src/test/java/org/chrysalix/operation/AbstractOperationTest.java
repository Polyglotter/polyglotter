/*
 * Chrysalix
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Chrysalix is free software. Unless otherwise indicated, all code in Chrysalix
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * Chrysalix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.chrysalix.operation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Iterator;

import org.chrysalix.ChrysalixException;
import org.chrysalix.transformation.TransformationTestFactory;
import org.chrysalix.transformation.Value;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modelspace.ModelObject;

@Ignore
@SuppressWarnings( "javadoc" )
public final class AbstractOperationTest {

    private TransformationTestFactory factory;
    private ModelObject modelObject;
    private AbstractOperation< Integer > operation;

    @Before
    public void beforeEach() throws Exception {
        this.factory = new TransformationTestFactory();
        this.modelObject = mock( ModelObject.class );
        this.operation =
            ( AbstractOperation< Integer > ) OperationTestConstants.DESCRIPTOR.newInstance( this.modelObject,
                                                                                            OperationTestConstants.TEST_TRANSFORMATION );
    }

    @Test
    public void shouldAddMultipleinputs() throws Exception {
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(), OperationTestConstants.STRING_1_TERM );
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(), OperationTestConstants.STRING_2_TERM );
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(), OperationTestConstants.STRING_3_TERM );
        assertThat( this.operation.inputs( OperationTestConstants.STRING_DESCRIPTOR.name() ).size(), is( 3 ) );
        assertThat( this.operation.inputs().length, is( 3 ) );
    }

    @Test
    public void shouldAddMultipleTerms2() throws Exception {
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(),
                                 OperationTestConstants.STRING_1_TERM,
                                 OperationTestConstants.STRING_2_TERM,
                                 OperationTestConstants.STRING_3_TERM );
        assertThat( this.operation.inputs( OperationTestConstants.STRING_DESCRIPTOR.name() ).size(), is( 3 ) );
        assertThat( this.operation.inputs().length, is( 3 ) );
    }

    @Test
    public void shouldBeAbleToUseOperationAsTerm() throws Exception {
        this.operation.addInput( OperationTestConstants.INT_DESCRIPTOR.name(), OperationTestConstants.INT_1_TERM );

        final long term = 2;
        final Add addOp = new Add( mock( ModelObject.class ), OperationTestConstants.TEST_TRANSFORMATION );
        addOp.addInput( Add.TERM_DESCRIPTOR.name(), this.operation );
        addOp.addInput( Add.TERM_DESCRIPTOR.name(), this.factory.createNumberValue( "/my/path/", Add.TERM_DESCRIPTOR, term ) );

        assertThat( addOp.get(), is( ( Number ) ( term + OperationTestConstants.INT_1_VALUE ) ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailAddingEmptyTerms() throws Exception {
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(), new Object[ 0 ] );
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(), ( Object[] ) new Value< ? >[ 0 ] );
    }

    @Test( expected = ChrysalixException.class )
    public void shouldFailAddingNullTerm() throws Exception {
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(),
                                 new Object[] { OperationTestConstants.STRING_1_TERM,
                                                 null,
                                                 OperationTestConstants.STRING_2_TERM } );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailAddingNullTerms() throws Exception {
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(), ( Object[] ) null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailIfNullDescriptor() throws Exception {
        this.operation.inputs( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailRemovingEmptyTerms() throws Exception {
        this.operation.removeInput( OperationTestConstants.STRING_DESCRIPTOR.name(), ( Object[] ) new Value< ? >[ 0 ] );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldFailRemovingInputUsingIterator() throws Exception {
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(), OperationTestConstants.STRING_1_TERM );

        final Iterator< Value< ? > > itr = this.operation.iterator();
        itr.next();
        itr.remove();
    }

    @Test( expected = ChrysalixException.class )
    public void shouldFailRemovingInutThatWasNotAdded() throws Exception {
        this.operation.removeInput( OperationTestConstants.STRING_DESCRIPTOR.name(),
                                    OperationTestConstants.STRING_1_TERM );
    }

    @Test( expected = ChrysalixException.class )
    public void shouldFailRemovingNullTerm() throws Exception {
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(),
                                 new Object[] { OperationTestConstants.STRING_1_TERM,
                                                 OperationTestConstants.STRING_2_TERM } );
        this.operation.removeInput( OperationTestConstants.STRING_DESCRIPTOR.name(),
                                    ( Object[] ) new Value< ? >[] { OperationTestConstants.STRING_1_TERM,
                                                    null,
                                                    OperationTestConstants.STRING_2_TERM } );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailRemovingNullTerms() throws Exception {
        this.operation.removeInput( OperationTestConstants.STRING_DESCRIPTOR.name(), ( Object[] ) null );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldFailSettingValue() throws Exception {
        this.operation.set( 2 );
    }

    @Test
    public void shouldHaveEmpyIteratorAfterConstruction() {
        assertThat( this.operation.iterator().hasNext(), is( false ) );
    }

    @Test
    public void shouldHaveIteratorSizeEqualToTermSize() throws Exception {
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(),
                                 OperationTestConstants.STRING_1_TERM,
                                 OperationTestConstants.STRING_2_TERM,
                                 OperationTestConstants.STRING_3_TERM );

        final Iterator< Value< ? >> itr = this.operation.iterator();

        for ( int i = 0, size = this.operation.inputs().length; i < size; ++i ) {
            assertThat( itr.next(), is( notNullValue() ) );
        }

        assertThat( itr.hasNext(), is( false ) );
    }

    @Test
    public void shouldObtainDescriptor() throws Exception {
        assertThat( this.operation.descriptor(), is( notNullValue() ) );
        assertThat( this.operation.descriptor(), is( sameInstance( OperationTestConstants.DESCRIPTOR ) ) );
    }

    @Test
    public void shouldRemoveMultipleInputs() throws Exception {
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(),
                                 OperationTestConstants.STRING_1_TERM,
                                 OperationTestConstants.STRING_2_TERM );
        this.operation.removeInput( OperationTestConstants.STRING_DESCRIPTOR.name(),
                                    OperationTestConstants.STRING_1_TERM,
                                    OperationTestConstants.STRING_2_TERM );
        assertThat( this.operation.inputs().length, is( 0 ) );
    }

    @Test
    public void shouldRemoveOneTerm() throws Exception {
        this.operation.addInput( OperationTestConstants.STRING_DESCRIPTOR.name(), OperationTestConstants.STRING_1_TERM );
        this.operation.removeInput( OperationTestConstants.STRING_DESCRIPTOR.name(), OperationTestConstants.STRING_1_TERM );
        assertThat( this.operation.inputs().length, is( 0 ) );
    }

    @Test
    public void shouldSetIdAtConstruction() throws Exception {
        assertThat( this.operation.transformationId(), is( OperationTestConstants.TRANSFORM_ID ) );
    }

    @Test
    public void shouldSetTransformIdAtConstruction() {
        assertThat( this.operation.transformation(), is( OperationTestConstants.TEST_TRANSFORMATION ) );
    }

}
