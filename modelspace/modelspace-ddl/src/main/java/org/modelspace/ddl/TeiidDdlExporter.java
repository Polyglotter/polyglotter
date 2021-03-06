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
package org.modelspace.ddl;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.modelspace.Model;
import org.modelspace.spi.Exporter;

/**
 * 
 */
public class TeiidDdlExporter implements Exporter {

    PrintWriter writer;
    String xsdPrefix;
    final Map< String, String > namespacePrefixByUri = new HashMap<>();
    final Map< String, Node > complexTypeByName = new HashMap<>();
    String targetNamespace;

    /**
     * {@inheritDoc}
     * 
     * @see org.modelspace.spi.Exporter#execute(org.modelspace.Model, java.io.OutputStream)
     */
    @Override
    public void execute( final Model model,
                         final OutputStream stream ) {
        // TODO: BML
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modelspace.spi.Exporter#metamodelId()
     */
    @Override
    public String metamodelId() {
        return TeiidDdlLexicon.DDL_METAMODEL_ID;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modelspace.spi.Exporter#supports(java.lang.String)
     */
    @Override
    public boolean supports( final String mimeType ) {
        return true;
    }
}
