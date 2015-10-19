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
        for ( PortletEntity portletEntity : portletEntities )
        {
            String portletEntityName = portletEntity.getName();
            portletEntityName = new ContentPathNameGenerator().generatePathName( portletEntityName );

            try
            {
                final URL partControllerUrl = getClass().getResource( "/templates/parts/xsl-part.js" );
                final File partControllerTarget = new File( target, portletEntityName + "/" + portletEntityName + ".js" );
                FileUtils.copyURLToFile( partControllerUrl, partControllerTarget );

                final URL partDescriptorUrl = getClass().getResource( "/templates/parts/xsl-part.xml" );
                final File partDescriptorTarget = new File( target, portletEntityName + "/" + portletEntityName + ".xml" );
                FileUtils.copyURLToFile( partDescriptorUrl, partDescriptorTarget );

                final URL partXslUrl = getClass().getResource( "/templates/parts/xsl-part.xsl" );
                final File partXslTarget = new File( target, portletEntityName + "/xsl-part.xsl" );
                FileUtils.copyURLToFile( partXslUrl, partXslTarget );
            }
            catch ( IOException e )
            {
                logger.error( "Error while exporting PortletEntity \"" + portletEntityName + "\"", e );
            }
        }
    }
}
