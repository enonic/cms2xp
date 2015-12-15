package com.enonic.cms2xp.export.xml;

import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.mixin.MixinName;
import com.enonic.xp.schema.mixin.MixinNames;
import com.enonic.xp.xml.DomBuilder;
import com.enonic.xp.xml.DomHelper;

public final class XmlContentTypeSerializer
    extends XmlSerializer
{
    private ContentType contentType;

    public XmlContentTypeSerializer()
    {
        super( DomBuilder.create( "content-type" ) );
    }

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
        serializeMetadata();
        serializeForm();
    }

    private void serializeMetadata()
    {
        final MixinNames metadata = contentType.getMetadata();
        if ( metadata != null && metadata.isNotEmpty() )
        {
            this.builder.start( "x-data" );
            for ( final MixinName mixinName : metadata )
            {
                serializeValueElement( "mixin", mixinName );
            }
            this.builder.end();
        }
    }

    private void serializeForm()
    {
        this.builder.start( "form" );
        new XmlFormSerializer( this.builder ).form( contentType.getForm() ).serializeForm();
        this.builder.end();
    }

}
