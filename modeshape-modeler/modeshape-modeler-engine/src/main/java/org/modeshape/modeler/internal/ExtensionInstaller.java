/*
 * Polyglotter (http://polyglotter.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Polyglotter is free software. Unless otherwise indicated, all code in Polyglotter
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * Polyglotter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.modeler.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.modeler.ModelType;
import org.modeshape.modeler.ModelerLexicon;
import org.modeshape.modeler.extensions.DependencyProcessor;
import org.modeshape.modeler.extensions.Desequencer;
import org.modeshape.modeler.internal.ModelTypeManagerImpl.LibraryClassLoader;
import org.polyglotter.common.Logger;

/**
 * A class that installs model type dependency processors and desequencers.
 */
public class ExtensionInstaller {

    // pass in category then version
    private static final String ARCHIVE_NAME = "modeshape-modeler-%s-%s-module-with-dependencies.zip";

    // pass in archive name
    private static final String ARCHIVE_PATH = "org/modeshape/modeler/%s";

    static final Logger LOGGER = Logger.getLogger( ExtensionInstaller.class );

    private boolean archiveExists( final Node categoryNode,
                                   final String archiveName ) throws Exception {
        final NodeIterator itr = archivesNode( categoryNode ).getNodes();

        for ( Node archiveNode = itr.nextNode(); itr.hasNext(); archiveNode = itr.nextNode() ) {
            if ( archiveNode.getName().equals( archiveName ) ) {
                return true;
            }
        }

        return false;
    }

    private Node archivesNode( final Node categoryNode ) throws Exception {
        return categoryNode.getNode( ModelerLexicon.Category.ARCHIVES );
    }

    private ModelType findModelType( final String modelTypeId,
                                     final Set< ModelType > modelTypes ) {
        for ( final ModelType type : modelTypes ) {
            if ( type.id().equals( modelTypeId ) ) {
                return type;
            }
        }

        return null;
    }

    /**
     * @param categoryNode
     *        the model type category whose extensions are being installed (cannot be <code>null</code>)
     * @param libraryClassLoader
     *        the class loader used for extensions classpath (cannot be <code>null</code>)
     * @param library
     *        the path where the extensions classpath is located (cannot be <code>null</code>)
     * @param modelTypeRepositories
     *        a collection of repositories to look for the extension archive (cannot be <code>null</code> or empty)
     * @param version
     *        the version of the extension to look for (cannot be <code>null</code> or empty)
     * @param modelTypes
     *        the model types cache (cannot be <code>null</code> or empty)
     * @return <code>true</code> if an extension was installed and the session should be saved
     * @throws Exception
     *         if an error occurs
     */
    boolean install( final Node categoryNode,
                     final LibraryClassLoader libraryClassLoader,
                     final Path library,
                     final Collection< URL > modelTypeRepositories,
                     final String version,
                     final Set< ModelType > modelTypes ) throws Exception {
        CheckArg.isNotNull( categoryNode, "categoryNode" );
        CheckArg.isNotNull( libraryClassLoader, "libraryClassLoader" );
        CheckArg.isNotNull( library, "library" );
        CheckArg.isNotEmpty( modelTypeRepositories, "modelTypeRepositories" );
        CheckArg.isNotEmpty( version, "version" );
        CheckArg.isNotNull( modelTypes, "modelTypes" );

        // will not have model types if sequencer jar didn't have installable sequencer
        if ( modelTypes.isEmpty() ) return false;

        final String category = categoryNode.getName();
        final String archiveName = String.format( ARCHIVE_NAME, category, version );
        final Path archivePath = library.resolve( archiveName );
        final String fullArchivePath = String.format( ARCHIVE_PATH, archiveName );
        boolean extensionInstalled = false;

        // don't install if already installed
        if ( archiveExists( categoryNode, archiveName ) ) {
            LOGGER.debug( "Archive '%s' already exists", archiveName );
            return false;
        }

        // loop through repositories until we find the sequencer archive
        for ( final URL repositoryUrl : modelTypeRepositories ) {
            final URL url = new URL( path( repositoryUrl.toString(), fullArchivePath ) );
            InputStream urlStream = null;
            Exception err = null;

            try {
                try {
                    urlStream = url.openStream();
                    LOGGER.debug( "Archive found at URL '%s'", url );
                } catch ( final IOException e ) {
                    LOGGER.debug( "Archive at URL '%s' was NOT found in repository", url );
                    continue; // not found
                }

                // copy archive over to library if found at this repository
                Files.copy( urlStream, archivePath );

                // add to classpath
                libraryClassLoader.addURL( url );

                // add to modeler workspace repository
                new JcrTools().uploadFile( categoryNode.getSession(),
                                           archivesNode( categoryNode ).getPath() + '/' + archivePath.getFileName().toString(),
                                           urlStream );

                archivePath.toFile().delete();
            } catch ( final IOException | RepositoryException e ) {
                err = e;
            } finally {
                if ( urlStream != null ) {
                    try {
                        urlStream.close();
                    } catch ( final IOException e ) {
                        if ( err == null ) throw e;
                        err.addSuppressed( e );
                        throw err;
                    }
                } else if ( err != null ) {
                    throw err;
                }
            }

            // Iterate through entries looking for appropriate extension classes
            final Collection< String > desequencerNames = new ArrayList<>( 3 );
            final Collection< String > dependencyProcessorNames = new ArrayList<>( 3 );

            try ( final ZipFile archive = new ZipFile( url.getFile() ) ) {
                for ( final Enumeration< ? extends ZipEntry > jarIter = archive.entries(); jarIter.hasMoreElements(); ) {
                    final ZipEntry jarEntry = jarIter.nextElement();
                    if ( jarEntry.isDirectory() ) continue;

                    String name = jarEntry.getName();

                    if ( isJarFile( name ) && !name.endsWith( "-tests.jar" ) && !name.endsWith( "-sources.jar" ) ) {
                        final Path jarPath = library.resolve( name.substring( name.lastIndexOf( '/' ) + 1 ) );

                        // see if this jar has already been installed
                        if ( jarPath.toFile().exists() ) {
                            LOGGER.debug( "Jar already installed: %s", jarPath );
                            continue;
                        }

                        // copy to library path
                        try ( final InputStream stream = archive.getInputStream( jarEntry ) ) {
                            Files.copy( stream, jarPath );
                            jarPath.toFile().deleteOnExit();
                        }

                        // add to classpath
                        libraryClassLoader.addURL( jarPath.toUri().toURL() );
                        LOGGER.debug( "Added jar '%s' to classpath", jarPath.toUri().toURL() );

                        // add jar to category node in repository
                        try ( final InputStream stream = archive.getInputStream( jarEntry ) ) {
                            final String nodePath =
                                ( archivesNode( categoryNode ).getPath() + '/' + jarPath.getFileName().toString() );
                            new JcrTools().uploadFile( categoryNode.getSession(), nodePath, stream );
                            LOGGER.debug( "Uploaded jar '%s' to category node", nodePath );
                        }

                        // Iterate through entries looking for appropriate sequencer and extension classes
                        try ( final ZipFile jar = new ZipFile( jarPath.toFile() ) ) {
                            for ( final Enumeration< ? extends ZipEntry > itr = jar.entries(); itr.hasMoreElements(); ) {
                                final ZipEntry entry = itr.nextElement();
                                if ( entry.isDirectory() ) continue;

                                name = entry.getName();

                                // see if class is a possible sequencer or desequencer
                                if ( isDesequencerName( name ) ) {
                                    desequencerNames.add( name.replace( '/', '.' ).substring( 0, name.length() - ".class".length() ) );
                                    LOGGER.debug( "Found potential desequencer '%s'", name );
                                } else if ( isDependencyProcessorName( name ) ) {
                                    dependencyProcessorNames.add( name.replace( '/', '.' ).substring( 0, name.length() - ".class".length() ) );
                                    LOGGER.debug( "Found potential dependency processor '%s'", name );
                                }
                            }
                        }
                    } else if ( isDesequencerName( name ) ) {
                        desequencerNames.add( name.replace( '/', '.' ).substring( 0, name.length() - ".class".length() ) );
                        LOGGER.debug( "Found potential desequencer '%s'", name );
                    } else if ( isDependencyProcessorName( name ) ) {
                        dependencyProcessorNames.add( name.replace( '/', '.' ).substring( 0, name.length() - ".class".length() ) );
                        LOGGER.debug( "Found potential dependency processor '%s'", name );
                    }
                }
            }

            // try and load potential desequencer classes
            for ( final String className : desequencerNames ) {
                Class< ? > clazz = null;

                try {
                    clazz = libraryClassLoader.loadClass( className );

                    if ( Desequencer.class.isAssignableFrom( clazz )
                         && !Modifier.isAbstract( clazz.getModifiers() ) ) {
                        final Desequencer desequencer = ( Desequencer ) clazz.newInstance();
                        final String modelType = desequencer.modelType();
                        final Node modelTypeNode = modelTypeNode( categoryNode, modelType );
                        modelTypeNode.setProperty( ModelerLexicon.ModelType.DESEQUENCER_CLASS_NAME, className );

                        final ModelTypeImpl type = ( ModelTypeImpl ) findModelType( desequencer.modelType(), modelTypes );
                        type.setDesequencer( desequencer );

                        extensionInstalled = true;
                        LOGGER.debug( "Installed desequencer '%s' for model type '%s'", className, modelType );
                    }
                } catch ( final NoClassDefFoundError | ClassNotFoundException ignored ) {
                    LOGGER.debug( "Potential desequencer class '%s' cannot be loaded", clazz );
                }
            }

            // try and load potential dependency processor classes
            for ( final String className : dependencyProcessorNames ) {
                Class< ? > clazz = null;

                try {
                    clazz = libraryClassLoader.loadClass( className );

                    if ( DependencyProcessor.class.isAssignableFrom( clazz )
                         && !Modifier.isAbstract( clazz.getModifiers() ) ) {
                        final DependencyProcessor dependencyProcessor = ( DependencyProcessor ) clazz.newInstance();
                        final String modelType = dependencyProcessor.modelType();

                        final Node modelTypeNode = modelTypeNode( categoryNode, modelType );
                        modelTypeNode.setProperty( ModelerLexicon.ModelType.DEPENDENCY_PROCESSOR_CLASS_NAME, className );

                        final ModelTypeImpl type = ( ModelTypeImpl ) findModelType( dependencyProcessor.modelType(), modelTypes );
                        type.setDependencyProcessor( dependencyProcessor );

                        extensionInstalled = true;
                        LOGGER.debug( "Installed dependency processor '%s' for model type '%s'", className, modelType );
                    }
                } catch ( final NoClassDefFoundError | ClassNotFoundException ignored ) {
                    LOGGER.debug( "Potential dependency processor class '%s' cannot be loaded", className );
                }
            }
        }

        return extensionInstalled;
    }

    private boolean isDependencyProcessorName( final String name ) {
        return name.endsWith( "DependencyProcessor.class" );
    }

    private boolean isDesequencerName( final String name ) {
        return name.endsWith( "Desequencer.class" );
    }

    private boolean isJarFile( final String name ) {
        final String ext = ".jar";

        if ( name.length() > ext.length() ) {
            return name.toLowerCase().endsWith( ext );
        }

        return false;
    }

    Node modelTypeNode( final Node categoryNode,
                        final String modelType ) throws Exception {
        return categoryNode.getNode( ModelerLexicon.Category.MODEL_TYPES ).getNode( modelType );
    }

    private String path( final String prefix,
                         final String suffix ) {
        if ( prefix.charAt( prefix.length() - 1 ) == '/' ) {
            return ( ( suffix.charAt( 0 ) == '/' ) ? ( prefix + suffix.substring( 1 ) ) : ( prefix + suffix ) );
        }

        return ( suffix.charAt( 0 ) == '/' ? ( prefix + suffix ) : ( prefix + '/' + suffix ) );
    }

}