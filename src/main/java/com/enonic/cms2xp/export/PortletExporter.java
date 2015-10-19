package com.enonic.cms2xp.export;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import com.enonic.xp.core.impl.content.ContentPathNameGenerator;

import com.enonic.cms.core.structure.portlet.PortletEntity;

public class PortletExporter
{
    private final static Logger logger = LoggerFactory.getLogger( PortletExporter.class );

    private final File target;

    public PortletExporter( final File target )
    {
        this.target = target;
    }

    public void export( Iterable<PortletEntity> portletEntities )
    {
        //TODO Remove
        try
        {
            copy( null, null, "/templates/pages/default/default", "../pages/default/default" );
        }
        catch ( Exception e )
        {
            logger.error( "Error while exporting default page", e );
        }

        for ( PortletEntity portletEntity : portletEntities )
        {
            final String portletDisplayName = portletEntity.getName();
            final String portletName = new ContentPathNameGenerator().generatePathName( portletDisplayName );
            try
            {
                copy( portletName, portletDisplayName, "/templates/parts/part/part", portletName + "/" + portletName );
            }
            catch ( Exception e )
            {
                logger.error( "Error while exporting PortletEntity \"" + portletDisplayName + "\"", e );
            }
        }
    }

    private void copy( String portletName, String portletDisplayName, String sourcePath, String targetPath )
        throws IOException
    {
        copy( portletName, portletDisplayName, sourcePath, targetPath, ".js" );
        copy( portletName, portletDisplayName, sourcePath, targetPath, ".xml" );
        copy( portletName, portletDisplayName, sourcePath, targetPath, ".html" );
    }

    private void copy( String portletName, String portletDisplayName, String sourcePath, String targetPath, String extension )
        throws IOException
    {
        final URL partControllerUrl = getClass().getResource( sourcePath + extension );
        final File partControllerTarget = new File( target, targetPath + extension );

        final CharSource charSource = Resources.asCharSource( partControllerUrl, Charsets.UTF_8 );
        final String content = charSource.read().
            replaceAll( "\\{\\{portletName\\}\\}", portletName ).
            replaceAll( "\\{\\{portletDisplayName\\}\\}", portletDisplayName );
        Files.createParentDirs( partControllerTarget );
        Files.asCharSink( partControllerTarget, Charsets.UTF_8 ).write( content );
    }
}
