package com.enonic.cms2xp.export;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.xp.core.impl.content.ContentPathNameGenerator;

import com.enonic.cms.core.structure.portlet.PortletEntity;

public class PortletExporter
    extends AbstractAppComponentExporter
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
            final String portletDisplayName = portletEntity.getName();
            final String portletName = new ContentPathNameGenerator().generatePathName( portletDisplayName );

            Map<String, Object> mapping = new HashMap<>();
            mapping.put( "portletName", portletName );
            mapping.put( "portletDisplayName", portletDisplayName );

            try
            {
                copy( "/templates/parts/part/part.html", new File( target, portletName + "/" + portletName + ".html" ), mapping );
                copy( "/templates/parts/part/part.js", new File( target, portletName + "/" + portletName + ".js" ), mapping );
                copy( "/templates/parts/part/part.xml", new File( target, portletName + "/" + portletName + ".xml" ), mapping );
            }
            catch ( Exception e )
            {
                logger.error( "Error while exporting PortletEntity \"" + portletDisplayName + "\"", e );
            }
        }
    }


}
