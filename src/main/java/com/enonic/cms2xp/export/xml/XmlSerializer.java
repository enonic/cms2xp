package com.enonic.cms2xp.export.xml;

import com.enonic.xp.xml.DomBuilder;

abstract class XmlSerializer
{
    protected final DomBuilder builder;

    public XmlSerializer( final DomBuilder builder )
    {
        this.builder = builder;
    }

    protected void serializeValueElement( final String name, final Object value )
    {
        if ( value != null )
        {
            this.builder.start( name );
            this.builder.text( value.toString() );
            this.builder.end();
        }
    }

    protected void serializeValueAttribute( final String name, final Object value )
    {
        if ( value != null )
        {
            this.builder.attribute( name, value.toString() );
        }
    }
}
