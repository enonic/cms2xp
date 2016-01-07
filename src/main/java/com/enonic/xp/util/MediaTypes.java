package com.enonic.xp.util;

import java.util.HashMap;

import com.google.common.io.Files;
import com.google.common.net.MediaType;

public final class MediaTypes
    extends HashMap<String, MediaType>
{
    private final static MediaTypes INSTANCE = new MediaTypes();

    private final static MediaType DEFAULT = MediaType.OCTET_STREAM;

    private MediaTypes()
    {
        put( "gif", MediaType.GIF );
        put( "png", MediaType.PNG );
        put( "jpeg", MediaType.JPEG );
        put( "jpg", MediaType.JPEG );
        put( "tiff", MediaType.TIFF );
        put( "tif", MediaType.TIFF );
        put( "bmp", MediaType.BMP );
        put( "pdf", MediaType.PDF );
        put( "json", MediaType.JSON_UTF_8 );
        put( "js", MediaType.JAVASCRIPT_UTF_8 );
        put( "css", MediaType.CSS_UTF_8 );
        put( "html", MediaType.HTML_UTF_8 );
        put( "htm", MediaType.HTML_UTF_8 );
        put( "xml", MediaType.XML_UTF_8 );
        put( "svg", MediaType.SVG_UTF_8 );
        put( "mp4", MediaType.MP4_VIDEO );
        put( "swf", MediaType.SHOCKWAVE_FLASH );

        // Audio
        put( "mp3", MediaType.MPEG_AUDIO );

        // Video
        put( "avi", MediaType.MPEG_VIDEO );
        put( "mpeg", MediaType.MPEG_VIDEO );

        // Archive
        put( "zip", MediaType.ZIP );
        put( "gzip", MediaType.ZIP );
        put( "gz", MediaType.ZIP );
        put( "rar", MediaType.create( "application", "x-rar-compressed" ) );

        // Text
        put( "txt", MediaType.PLAIN_TEXT_UTF_8 );
        put( "csv", MediaType.CSV_UTF_8 );

        // Data
        put( "rtf", MediaType.RTF_UTF_8 );

        // Document
        put( "odt", MediaType.OPENDOCUMENT_TEXT );
        put( "docx", MediaType.MICROSOFT_WORD );
        put( "doc", MediaType.MICROSOFT_WORD );

        // Presentation
        put( "odp", MediaType.OPENDOCUMENT_PRESENTATION );
        put( "pptx", MediaType.MICROSOFT_POWERPOINT );
        put( "ppsx", MediaType.MICROSOFT_POWERPOINT );
        put( "ppt", MediaType.MICROSOFT_POWERPOINT );

        // Spreadsheet
        put( "ods", MediaType.OPENDOCUMENT_SPREADSHEET );
        put( "xlsx", MediaType.MICROSOFT_EXCEL );
        put( "xls", MediaType.MICROSOFT_EXCEL );

        // Vector
        put( "eps", MediaType.POSTSCRIPT );
    }

    public MediaType fromExt( final String ext )
    {
        final MediaType type = get( ext );
        return type != null ? type : DEFAULT;
    }

    public MediaType fromFile( final String fileName )
    {
        return fromExt( Files.getFileExtension( fileName ) );
    }

    @Override
    public MediaType put( final String ext, final MediaType value )
    {
        return super.put( ext, value.withoutParameters() );
    }

    public static MediaTypes instance()
    {
        return INSTANCE;
    }
}
