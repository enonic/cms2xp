package com.enonic.cms2xp.converter;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.Value;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.util.Reference;

import com.enonic.cms.core.content.contentdata.legacy.LegacyNewsletterContentData;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;

public class NewsletterValuesConverter
{
    private final NodeIdRegistry nodeIdRegistry;

    public NewsletterValuesConverter( final NodeIdRegistry nodeIdRegistry )
    {
        this.nodeIdRegistry = nodeIdRegistry;
    }

    public Value toValue( final LegacyNewsletterContentData formData )
    {
        final Document doc = formData.getContentDataXml();
        final Element root = doc.getRootElement();
        final PropertySet propertySet = new PropertySet();
        final Element subjectEl = root.getChild( "subject" );
        final Element summaryEl = root.getChild( "summary" );
        final Element newsletterEl = root.getChild( "newsletter" );
        String subject = "";
        String summary = "";
        String newsletter = "";
        int menuitemkey = -1;
        if ( subjectEl != null )
        {
            subject = subjectEl.getValue();
        }
        if ( summaryEl != null )
        {
            summary = summaryEl.getValue();
        }
        if ( newsletterEl != null && !newsletterEl.getChildren().isEmpty() )
        {
            final Element htmlEl = (Element) newsletterEl.getChildren().get( 0 );
            newsletter = new XMLOutputter().outputString( htmlEl );
            final Attribute menuItemAttr = newsletterEl.getAttribute( "menuitemkey" );
            if ( menuItemAttr != null )
            {
                try
                {
                    menuitemkey = Integer.parseInt( menuItemAttr.getValue() );
                }
                catch ( NumberFormatException e )
                {
                    menuitemkey = -1;
                }
            }

        }

        propertySet.setString( "subject", subject );
        propertySet.setString( "summary", summary );
        propertySet.setString( "newsletter", newsletter );
        if ( menuitemkey > 0 )
        {
            final NodeId page = this.nodeIdRegistry.getNodeId( new MenuItemKey( menuitemkey ) );
            propertySet.setReference( "page", new Reference( page ) );
        }

        return ValueFactory.newPropertySet( propertySet );
    }

}
