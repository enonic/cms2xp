package com.enonic.cms2xp.export;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms2xp.export.xml.XmlContentTypeSerializer;
import com.enonic.xp.icon.Icon;
import com.enonic.xp.schema.content.ContentType;

public class ContentTypeExporter
{
    private final static Logger logger = LoggerFactory.getLogger( ContentTypeExporter.class );

    private final Path target;

    public ContentTypeExporter( final Path target )
    {
        this.target = target;
    }

    public void export( Iterable<ContentType> contentTypes )
    {
        for ( ContentType contentType : contentTypes )
        {
            final String ct = new XmlContentTypeSerializer().contentType( contentType ).serialize();

            final String ctName = contentType.getName().getLocalName();
            try
            {
                final Path dir = Files.createDirectory( target.resolve( ctName ) );
                Files.write( dir.resolve( ctName + ".xml" ), ct.getBytes( StandardCharsets.UTF_8 ) );

                final Icon icon = contentType.getIcon();
                if ( icon != null )
                {
                    Files.write( dir.resolve( ctName + ".png" ), icon.toByteArray() );
                }
            }
            catch ( Exception e )
            {
                logger.error( "Cannot write content type XML '{}'", ctName );
            }
        }
    }
}
