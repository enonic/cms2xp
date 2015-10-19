package com.enonic.cms2xp.export;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            copy( "/templates/pages/default/default.js", "../pages/default/default.js" );
            copy( "/templates/pages/default/default.html", "../pages/default/default.html" );
            copy( "/templates/pages/default/default.xml", "../pages/default/default.xml" );
        }
        catch ( Exception e )
        {
            logger.error( "Error while exporting default page", e );
        }

        for ( PortletEntity portletEntity : portletEntities )
        {
            String portletEntityName = portletEntity.getName();
            portletEntityName = new ContentPathNameGenerator().generatePathName( portletEntityName );

            try
            {
                copy( "/templates/parts/xsl-part/xsl-part.js", portletEntityName + "/" + portletEntityName + ".js" );
                copy( "/templates/parts/xsl-part/xsl-part.xml", portletEntityName + "/" + portletEntityName + ".xml" );
                copy( "/templates/parts/xsl-part/xsl-part.xsl", portletEntityName + "/xsl-part.xsl" );
            }
            catch ( Exception e )
            {
                logger.error( "Error while exporting PortletEntity \"" + portletEntityName + "\"", e );
            }
        }
    }

    private void copy( String sourcePath, String targetPath )
        throws IOException
    {
        final URL partControllerUrl = getClass().getResource( sourcePath );
        final File partControllerTarget = new File( target, targetPath );
        FileUtils.copyURLToFile( partControllerUrl, partControllerTarget );
    }
}
