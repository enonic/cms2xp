package com.enonic.cms2xp.converter;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;

import com.enonic.xp.data.Value;
import com.enonic.xp.data.ValueFactory;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.contentdata.custom.stringbased.HtmlAreaDataEntry;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;

import static com.google.common.base.Strings.nullToEmpty;

public class HtmlAreaConverter
{
    private static final String CONTENT_TYPE = "content";

    private static final String IMAGE_TYPE = "image";

    private static final String ATTACHMENT_TYPE = "attachment";

    private static final String PAGE_TYPE = "page";

    private final static Pattern CONTENT_PATTERN = Pattern.compile(
        "(?:href|src)=(\"((" + CONTENT_TYPE + "|" + IMAGE_TYPE + "|" + ATTACHMENT_TYPE + "|" + PAGE_TYPE + ")://([0-9]+)(\\?[^\"]+)?)\")",
        Pattern.MULTILINE | Pattern.UNIX_LINES );

    private static final int MATCH_INDEX = 1;

    private static final int LINK_INDEX = MATCH_INDEX + 1;

    private static final int TYPE_INDEX = LINK_INDEX + 1;

    private static final int ID_INDEX = TYPE_INDEX + 1;

    private static final int PARAMS_INDEX = ID_INDEX + 1;

    private static final int NB_GROUPS = ID_INDEX;

    private static final String KEEP_SIZE_TRUE = "?keepsize=true";


    private final NodeIdRegistry nodeIdRegistry;

    public HtmlAreaConverter( final NodeIdRegistry nodeIdRegistry )
    {
        this.nodeIdRegistry = nodeIdRegistry;
    }

    public Value toHtmlValue( final HtmlAreaDataEntry htmlEntry )
    {
        final String processedHtml = processLinks( htmlEntry.getValue() );
        return ValueFactory.newString( processedHtml );
    }

    private String processLinks( final String html )
    {
        final Matcher contentMatcher = CONTENT_PATTERN.matcher( html );

        String processedHtml = html;
        while ( contentMatcher.find() )
        {
            if ( contentMatcher.groupCount() >= NB_GROUPS )
            {
                final String match = contentMatcher.group( MATCH_INDEX );
                final String link = contentMatcher.group( LINK_INDEX );
                final String type = contentMatcher.group( TYPE_INDEX );
                final String id = contentMatcher.group( ID_INDEX );
                String urlParams = contentMatcher.groupCount() == PARAMS_INDEX ? contentMatcher.group( PARAMS_INDEX ) : null;

                if ( CONTENT_TYPE.equals( type ) )
                {
                    final String pageUrl = "content://" + this.nodeIdRegistry.getNodeId( new ContentKey( id ) );
                    processedHtml = processedHtml.replaceFirst( Pattern.quote( match ), "\"" + pageUrl + "\"" );
                }
                if ( PAGE_TYPE.equals( type ) )
                {
                    final String pageUrl = "content://" + this.nodeIdRegistry.getNodeId( new MenuItemKey( id ) );
                    processedHtml = processedHtml.replaceFirst( Pattern.quote( match ), "\"" + pageUrl + "\"" );
                }
                else if ( IMAGE_TYPE.equals( type ) )
                {
                    String imageUrl = "image://" + this.nodeIdRegistry.getNodeId( new ContentKey( id ) );
                    final Map<String, String> params = parseParams( urlParams );
                    if ( "full".equals( params.get( "_size" ) ) ) // _format, _filter
                    {
                        imageUrl = imageUrl + KEEP_SIZE_TRUE;
                    }
                    processedHtml = processedHtml.replaceFirst( Pattern.quote( match ), "\"" + imageUrl + "\"" );
                }
                else
                {
                    final String attachmentUrl = "media://download/" + this.nodeIdRegistry.getNodeId( new ContentKey( id ) );
                    processedHtml = processedHtml.replaceFirst( Pattern.quote( match ), "\"" + attachmentUrl + "\"" );
                }
            }
        }
        return processedHtml;
    }

    private Map<String, String> parseParams( final String urlParams )
    {
        String query = nullToEmpty( urlParams );
        if ( query.startsWith( "?" ) )
        {
            query = query.substring( 1 );
        }
        query = query.replace( "&amp;", "&" );
        return Splitter.on( '&' ).trimResults().withKeyValueSeparator( "=" ).split( query );
    }
}
