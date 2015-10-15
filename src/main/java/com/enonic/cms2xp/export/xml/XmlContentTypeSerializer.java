package com.enonic.cms2xp.export.xml;

import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.mixin.MixinName;
import com.enonic.xp.xml.DomBuilder;
import com.enonic.xp.xml.DomHelper;

public class XmlContentTypeSerializer
{
    private final DomBuilder builder = DomBuilder.create( "content-type" );

    private ContentType contentType;

    public XmlContentTypeSerializer contentType( ContentType contentType )
    {
        this.contentType = contentType;
        return this;
    }

    public String serialize()
    {
        serializeContentType();
        return DomHelper.serialize( this.builder.getDocument() );
    }

    private void serializeContentType()
    {
        serializeValueElement( "display-name", contentType.getDisplayName() );
        serializeValueElement( "description", contentType.getDescription() );
        serializeValueElement( "content-display-name-script", contentType.getContentDisplayNameScript() );
        serializeValueElement( "super-type", contentType.getSuperType() );
        serializeValueElement( "is-abstract", contentType.isAbstract() );
        serializeValueElement( "is-final", contentType.isFinal() );
        serializeValueElement( "allow-child-content", contentType.allowChildContent() );
        serializeValueElement( "super-type", contentType.getSuperType() );
        serializeMetadata();
    }

    private void serializeMetadata()
    {
        this.builder.start( "x-data" );
        for ( final MixinName mixinName : contentType.getMetadata() )
        {
            serializeValueElement( "mixin", mixinName );
        }
        this.builder.end();
    }

    private void serializeValueElement( final String name, final Object value )
    {
        if ( value != null )
        {
            this.builder.start( name );
            this.builder.text( value.toString() );
            this.builder.end();
        }
    }
}
