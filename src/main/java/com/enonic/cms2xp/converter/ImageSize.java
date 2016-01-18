package com.enonic.cms2xp.converter;

import java.util.HashMap;
import java.util.Map;

public enum ImageSize
{
    WIDE( "wide" ),
    SQUARE( "square" ),
    CUSTOM( "custom" ),
    LIST( "list" ),
    THUMBNAIL( "thumbnail" ),
    REGULAR( "regular" ),
    FULL( "full" ),
    ORIGINAL( "original" );

    private static final Map<String, ImageSize> VALUES = new HashMap<>();

    static
    {
        for ( final ImageSize htmlTag : ImageSize.values() )
        {
            VALUES.put( htmlTag.id, htmlTag );
        }
    }

    private final String id;

    ImageSize( final String id )
    {
        this.id = id;
    }


    public static ImageSize from( final String id )
    {
        return VALUES.getOrDefault( id, ORIGINAL );
    }
}
