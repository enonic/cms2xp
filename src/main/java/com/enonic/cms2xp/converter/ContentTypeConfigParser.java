package com.enonic.cms2xp.converter;

import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfig;
import com.enonic.cms.core.content.contenttype.ContentTypeImportConfigParser;
import com.enonic.cms.core.content.contenttype.ContentTypeKey;
import com.enonic.cms.core.content.contenttype.CtyFormConfig;
import com.enonic.cms.core.content.contenttype.CtyImportConfig;
import com.enonic.cms.core.content.contenttype.CtySetConfig;
import com.enonic.cms.core.content.contenttype.InputConfigParser;
import com.enonic.cms.core.content.contenttype.InvalidContentTypeConfigException;
import com.enonic.cms.core.content.contenttype.dataentryconfig.AbstractBaseDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfigType;

// Copy from com.enonic.cms.core.content.contenttype.ContentTypeConfigParser
public class ContentTypeConfigParser
{

    public static ContentTypeConfig parse( final ContentTypeKey contentTypeKey, final ContentHandlerName contentHandlerName,
                                           final Element configEl )
    {

        final String rootElementName = configEl.getName();
        if ( !"config".equals( rootElementName ) )
        {
            throw new InvalidContentTypeConfigException( "Expected config element to be named 'config', was: " + rootElementName );
        }

        final ContentTypeConfig config = new ContentTypeConfig( contentHandlerName, configEl.getAttributeValue( "name" ) );

        /* Parse form */
        final ContentTypeConfigParser parser = new ContentTypeConfigParser();
        final CtyFormConfig form = parser.parseForm( contentTypeKey, configEl, config );
        config.setForm( form );

        /* Parse imports */
        final List<CtyImportConfig> imports = ContentTypeImportConfigParser.parseAllImports( form, configEl );
        config.setImports( imports );

        return config;
    }


    private ContentTypeConfigParser()
    {
        // no access
    }

    private CtyFormConfig parseForm( final ContentTypeKey contentTypeKey, Element configEl, ContentTypeConfig config )
    {

        Element formEl = configEl.getChild( "form" );
        if ( formEl == null )
        {
            throw new InvalidContentTypeConfigException( "Form element not found, expected as child to the config element." );
        }
        CtyFormConfig form = new CtyFormConfig( config );
        Element titleEl = formEl.getChild( "title" );
        if ( titleEl == null )
        {
            throw new InvalidContentTypeConfigException( "Title element not found, expected as child to the form element." );
        }
        final String titleInputName = titleEl.getAttributeValue( "name" );
        form.setTitleInputName( titleInputName );
        List<Element> blockEls = formEl.getChildren( "block" );
        int blockPosition = 1;
        for ( Element blockEl : blockEls )
        {
            form.addBlock( parseBlock( contentTypeKey, form, blockEl, blockPosition++, titleInputName ) );
        }

        if ( form.getTitleInput() == null )
        {
            throw new InvalidContentTypeConfigException(
                "Referred input field for title '" + form.getTitleInputName() + "' does not exist." );
        }

        DataEntryConfig titleInputConfig = form.getTitleInput();
        if ( !titleInputConfig.isRequired() )
        {
            throw new InvalidContentTypeConfigException(
                "Referred input field for title '" + form.getTitleInputName() + "' must be configured to be required." );
        }
        return form;
    }

    private CtySetConfig parseBlock( final ContentTypeKey contentTypeKey, final CtyFormConfig form, final Element blockEl,
                                     int blockPosition, String titleField )
    {
        String blockName = parseBlockName( blockEl, blockPosition );
        CtySetConfig block = new CtySetConfig( form, blockName, blockEl.getAttributeValue( "group" ) );

        @SuppressWarnings({"unchecked"}) List<Element> inputEls = blockEl.getChildren( "input" );
        int inputConfigPosition = 1;
        for ( Element inputEl : inputEls )
        {
            InputConfigParser inputConfigParser = new InputConfigParser( inputConfigPosition++ );
            DataEntryConfig inputConfig = inputConfigParser.parserInputConfigElement( inputEl );
            if ( inputEl.getChild( "help" ) != null && inputEl.getChild( "help" ).getText() != null )
            {
                final String help = inputEl.getChild( "help" ).getText();
                if ( inputConfig instanceof AbstractBaseDataEntryConfig )
                {
                    ContentTypeHelpMapper.setHelpText( contentTypeKey, inputConfig, help );
                }
            }

            if ( inputConfig.getName().equals( titleField ) )
            {
                DataEntryConfigType type = inputConfig.getType();
                if ( !( type.equals( DataEntryConfigType.URL ) || type.equals( DataEntryConfigType.DATE ) ||
                    type.equals( DataEntryConfigType.TEXT ) || type.equals( DataEntryConfigType.RADIOBUTTON ) ||
                    type.equals( DataEntryConfigType.DROPDOWN ) || type.equals( DataEntryConfigType.RELATEDCONTENT ) ) )
                {
                    throw new InvalidContentTypeConfigException(
                        "Illegal datatype for title. The title element must be of type text, url, date, radiobutton or dropdown." );
                }
            }
            block.addInput( inputConfig );
        }
        return block;
    }

    private String parseBlockName( final Element blockEl, final int blockPosition )
    {
        Attribute nameAttr = blockEl.getAttribute( "name" );
        if ( nameAttr == null )
        {
            throw new InvalidContentTypeConfigException( "Missing name attribute in block: " + blockPosition );
        }
        String name = nameAttr.getValue();
        if ( name == null || name.trim().length() == 0 )
        {
            throw new InvalidContentTypeConfigException( "Missing name in block: " + blockPosition );
        }
        return name;
    }

}
