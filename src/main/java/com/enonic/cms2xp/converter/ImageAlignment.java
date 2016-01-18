package com.enonic.cms2xp.converter;

enum ImageAlignment
{
    LEFT( "left" ),
    RIGHT( "right" ),
    CENTER( "center" ),
    JUSTIFIED( "justified" );

    private final String id;

    ImageAlignment( final String id )
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }
}
