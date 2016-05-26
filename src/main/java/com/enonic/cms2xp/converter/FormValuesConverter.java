package com.enonic.cms2xp.converter;

import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.Value;
import com.enonic.xp.data.ValueFactory;

import com.enonic.cms.core.content.contentdata.legacy.LegacyFormContentData;

public class FormValuesConverter
{
    public FormValuesConverter()
    {
    }

    public Value toValue( final LegacyFormContentData formData )
    {
        final Document doc = formData.getContentDataXml();
        final Element root = doc.getRootElement();
        final PropertySet propertySet = new PropertySet();
        addElement( propertySet, root );

        return ValueFactory.newPropertySet( propertySet );
    }

    private void addElement( final PropertySet parent, final Element el )
    {
        final PropertySet element = new PropertySet();
        addAttributes( element, el );

        final List<Element> children = el.getChildren();
        for ( Element child : children )
        {
            addElement( element, child );
        }
        if ( el.getText() != null && !el.getText().isEmpty() )
        {
            element.setString( "#text", el.getText() );
        }

        parent.addSet( el.getName(), element );
    }


    private void addAttributes( final PropertySet element, final Element el )
    {
        final List<Attribute> attributes = el.getAttributes();
        if ( !attributes.isEmpty() )
        {
            for ( Attribute attr : attributes )
            {
                element.addString( "@" + attr.getName(), attr.getValue() );
            }
        }
    }
}
