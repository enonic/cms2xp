package com.enonic.cms2xp.export;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import com.enonic.cms.framework.xml.XMLException;

public class XmlHelper
{
    private final static TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    public static String convertToString( Source source )
        throws XMLException
    {
        StreamResult result = new StreamResult();
        StringWriter writer = new StringWriter();
        result.setWriter( writer );
        transform( source, result );
        return writer.toString();
    }

    private static void transform( Source input, Result output )
        throws XMLException
    {
        try
        {
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
            transformer.transform( input, output );
        }
        catch ( Exception e )
        {
            throw new XMLException( "Failed to transform xml document", e );
        }
    }

}
