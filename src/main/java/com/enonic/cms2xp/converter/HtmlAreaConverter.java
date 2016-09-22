package com.enonic.cms2xp.converter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import com.enonic.xp.data.Value;
import com.enonic.xp.data.ValueFactory;

import com.enonic.cms.core.InvalidKeyException;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.contentdata.custom.stringbased.HtmlAreaDataEntry;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;

import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;

public class HtmlAreaConverter
{
    private static final String CONTENT_TYPE = "content";

    private static final String IMAGE_TYPE = "image";

    private static final String ATTACHMENT_TYPE = "attachment";

    private static final String PAGE_TYPE = "page";

    private static final String KEEP_SIZE_TRUE = "?keepsize=true";

    private final static Logger logger = LoggerFactory.getLogger( HtmlAreaConverter.class );

    private final NodeIdRegistry nodeIdRegistry;

    private final ImageDescriptionResolver imageDescriptionResolver;

    public HtmlAreaConverter( final NodeIdRegistry nodeIdRegistry, final ImageDescriptionResolver imageDescriptionResolver )
    {
        this.nodeIdRegistry = nodeIdRegistry;
        this.imageDescriptionResolver = imageDescriptionResolver;
    }

    public Value toHtmlValue( final HtmlAreaDataEntry htmlEntry )
    {
        final String processedHtml = processLinks( htmlEntry.getValue() );
        return ValueFactory.newString( processedHtml );
    }

    private String processLinks( final String html )
    {
        if ( html == null )
        {
            return null;
        }
        final Document doc = Jsoup.parseBodyFragment( html );
        final Element body = doc.body();

        final Elements links = body.select( "a" );
        for ( Element link : links )
        {
            final String href = link.attr( "href" );

            try
            {
                final String processedUrl = processUrl( href, null );
                if ( !href.equals( processedUrl ) )
                {
                    link.attr( "href", processedUrl );
                }
            }
            catch ( InvalidKeyException e )
            {
                logger.warn( "Invalid key in link URL [" + href + "] : " + e.getKey() );
            }
        }

        final Elements images = body.select( "img" );
        for ( Element image : images )
        {
            final String src = image.attr( "src" );
            try
            {
                final String processedUrl = processUrl( src, image );
                if ( !src.equals( processedUrl ) )
                {
                    image.attr( "src", processedUrl );
                }
            }
            catch ( InvalidKeyException e )
            {
                logger.warn( "Invalid key in image URL [" + src + "] : " + e.getKey() );
            }
        }

        final Document.OutputSettings settings = new Document.OutputSettings().
            syntax( Document.OutputSettings.Syntax.xml ).
            prettyPrint( false );

        return doc.outputSettings( settings ).body().html();
    }

    private String processUrl( final String url, final Element imageElement )
    {
        if ( url.startsWith( CONTENT_TYPE + "://" ) )
        {
            final String id = idFromUrl( url, CONTENT_TYPE );
            final String pageUrl = "content://" + this.nodeIdRegistry.getNodeId( new ContentKey( id ) );
            return pageUrl;
        }
        else if ( url.startsWith( PAGE_TYPE + "://" ) )
        {
            final String id = idFromUrl( url, PAGE_TYPE );
            final String pageUrl = "content://" + this.nodeIdRegistry.getNodeId( new MenuItemKey( id ) );
            return pageUrl;
        }
        else if ( url.startsWith( ATTACHMENT_TYPE + "://" ) )
        {
            final String id = idFromUrl( url, ATTACHMENT_TYPE );
            final String attachmentUrl = "media://download/" + this.nodeIdRegistry.getNodeId( new ContentKey( id ) );
            return attachmentUrl;
        }
        else if ( url.startsWith( IMAGE_TYPE + "://" ) )
        {
            final String id = idFromUrl( url, IMAGE_TYPE );
            final ContentKey contentKey = new ContentKey( id );
            String imageUrl = "image://" + this.nodeIdRegistry.getNodeId( contentKey );
            final Map<String, String> params = getUrlParams( url );
            final String sizeParam = params.get( "_size" );

            final ImageAlignment imageAlignment = getAlignment( imageElement );
            final ImageSize size = ImageSize.from( sizeParam );

            switch ( size )
            {
                case CUSTOM:
                case LIST:
                case THUMBNAIL:
                case REGULAR:
                    setImageAlignment( imageElement, imageAlignment, size, contentKey );
                    break;

                case WIDE:
                case SQUARE:
                    setImageAlignment( imageElement, imageAlignment, size, contentKey );
                    break;

                case FULL:
                    setImageAlignment( imageElement, ImageAlignment.JUSTIFIED, size, contentKey );
                    break;
                case ORIGINAL:
                    // keep size + alignment
                    imageUrl = imageUrl + KEEP_SIZE_TRUE;
                    setImageAlignment( imageElement, imageAlignment, size, contentKey );
                    break;
            }
            return imageUrl;
        }

        return url;
    }

    private void setImageAlignment( Element img, final ImageAlignment alignment, final ImageSize size, final ContentKey contentKey )
    {
        if ( img == null )
        {
            return;
        }

        final String styles = img.attr( "style" );
        final Map<String, String> cssStyles = new LinkedHashMap<>();
        if ( !StringUtils.isEmpty( styles ) )
        {
            final String[] stylePairs = styles.split( ";" );
            for ( String style : stylePairs )
            {
                final String name = substringBefore( style, ":" );
                final String value = substringAfter( style, ":" );
                cssStyles.put( name, value );
            }
        }

        cssStyles.put( "text-align", alignment.getId() );
        cssStyles.put( "width", "100%" );

        Joiner.MapJoiner joiner = Joiner.on( ";" ).withKeyValueSeparator( ": " );
        final String newStyleAttr = joiner.join( cssStyles );
        img.attr( "style", newStyleAttr );

        // figure wrapper
        final boolean isImageInOriginalSize = size == ImageSize.FULL;
        final String styleFormat = "float: %s; margin: %s;" + ( isImageInOriginalSize ? "" : "width: %s;" );
        String styleAttr = "text-align: " + alignment.getId() + ";";
        switch ( alignment )
        {
            case LEFT:
            case RIGHT:
                styleAttr = String.format( styleFormat, alignment.getId(), "15px", "40%" );
                break;
            case CENTER:
                styleAttr = styleAttr + String.format( styleFormat, "none", "auto", "60%" );
                break;
        }
        final Attributes figureAttr = new Attributes();
        figureAttr.put( "style", styleAttr );

        final Element figureEl = new Element( Tag.valueOf( "figure" ), "", figureAttr );
        img.replaceWith( figureEl );
        figureEl.appendChild( img );

        final String caption = imageDescriptionResolver.getImageDescription( contentKey );
        if ( StringUtils.isNotBlank( caption ) )
        {
            final Attributes figCaptionAttr = new Attributes();
            figCaptionAttr.put( "style", "text-align: center" );
            final Element figCaption = new Element( Tag.valueOf( "figcaption" ), "", figCaptionAttr );
            figCaption.text( caption );
            figureEl.appendChild( figCaption );
        }
    }

    private ImageAlignment getAlignment( final Element imageElement )
    {
        if ( imageElement == null || !"img".equals( imageElement.tagName() ) )
        {
            return ImageAlignment.LEFT;
        }

        if ( imageElement.hasClass( "editor-image-right" ) )
        {
            return ImageAlignment.RIGHT;
        }
        if ( imageElement.hasClass( "editor-image-left" ) )
        {
            return ImageAlignment.LEFT;
        }

        final Element parent = imageElement.parent();
        if ( parent != null && "p".equals( parent.tagName() ) )
        {
            if ( parent.hasClass( "editor-p-center" ) )
            {
                return ImageAlignment.CENTER;
            }
            if ( parent.hasClass( "editor-p-block" ) )
            {
                return ImageAlignment.JUSTIFIED;
            }
        }

        return ImageAlignment.LEFT;
    }

    private Map<String, String> getUrlParams( final String url )
    {
        final URI uri;
        try
        {
            uri = new URI( url );
        }
        catch ( URISyntaxException e )
        {
            return Collections.emptyMap();
        }

        try
        {
            List<NameValuePair> params = URLEncodedUtils.parse( uri, "UTF-8" );
            return params.stream().collect( Collectors.toMap( NameValuePair::getName, NameValuePair::getValue ) );
        }
        catch ( IllegalArgumentException e )
        {
            logger.warn( "HtmlArea: could not parse URL parameters in HtmlArea '" + url + "'" );
            return Collections.emptyMap();
        }
    }

    private String idFromUrl( final String url, final String type )
    {
        String id = substringAfter( url, type + "://" );
        id = substringBefore( id, "/" );
        id = substringBefore( id, "?" );
        id = substringBefore( id, "&" );
        id = substringBefore( id, "#" );
        return id.trim();
    }

}
