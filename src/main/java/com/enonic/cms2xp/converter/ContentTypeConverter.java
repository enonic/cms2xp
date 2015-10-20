package com.enonic.cms2xp.converter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.form.FieldSet;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.inputtype.InputTypeProperty;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfig;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.content.contenttype.ContentTypeKey;
import com.enonic.cms.core.content.contenttype.CtySetConfig;
import com.enonic.cms.core.content.contenttype.InvalidContentTypeConfigException;
import com.enonic.cms.core.content.contenttype.dataentryconfig.CheckboxDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfigType;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DateDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DropdownDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.FileDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.HtmlAreaDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.ImageDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.MultipleChoiceDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.RadioButtonDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.RelatedContentDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.SelectorDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.TextAreaDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.TextDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.UrlDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.XmlDataEntryConfig;

public final class ContentTypeConverter
    implements ContentTypeResolver
{
    private final static Logger logger = LoggerFactory.getLogger( ContentTypeConverter.class );

    private final Map<ContentTypeKey, ContentType> typeResolver;

    private final ApplicationKey appKey;

    public ContentTypeConverter( final ApplicationKey appKey )
    {
        this.appKey = appKey;
        this.typeResolver = new HashMap<>();
    }

    public ImmutableList<ContentType> export( final List<ContentTypeEntity> contentTypeEntities )
    {
        contentTypeEntities.stream().
            filter( ( ct ) -> ct.getContentHandlerName() == ContentHandlerName.CUSTOM ).
            forEach( ( ct ) -> {
                final ContentType contentType = convert( ct );
                this.typeResolver.put( ct.getContentTypeKey(), contentType );
                logger.info( "Converted content type: {}", contentType );
            } );
        return ImmutableList.copyOf( typeResolver.values() );
    }

    @Override
    public ContentType getContentType( final ContentTypeKey contentTypeKey )
    {
        return typeResolver.get( contentTypeKey );
    }

    private ContentType convert( final ContentTypeEntity contentTypeEntity )
    {
        Form form;
        try
        {
            form = convertConfig( contentTypeEntity.getContentTypeConfig() );
        }
        catch ( InvalidContentTypeConfigException e )
        {
            logger.warn( "Cannot get config for content type " + contentTypeEntity.getName() );
            form = Form.create().build();
        }

        final ContentType.Builder contentType = ContentType.create().
            name( ContentTypeName.from( appKey, contentTypeEntity.getName() ) ).
            displayName( contentTypeEntity.getName() ).
            description( contentTypeEntity.getDescription() ).
            createdTime( contentTypeEntity.getTimestamp().toInstant() ).
            superType( ContentTypeName.unstructured() ).
            form( form );
        return contentType.build();
    }

    private Form convertConfig( final ContentTypeConfig contentTypeConfig )
    {
        if ( contentTypeConfig == null )
        {
            return Form.create().build();
        }

        final Form.Builder form = Form.create();
        final List<CtySetConfig> setConfig = contentTypeConfig.getSetConfig();
        for ( CtySetConfig ctyConfig : setConfig )
        {
            final FormItem formItem;
            if ( ctyConfig.hasGroupXPath() )
            {
                formItem = addFormItemSet( ctyConfig );
            }
            else
            {
                formItem = addFieldSet( ctyConfig );
            }
            form.addFormItem( formItem );
        }

        return form.build();
    }

    private FormItem addFieldSet( final CtySetConfig ctyConfig )
    {
        final FieldSet.Builder fieldSet = FieldSet.create();
        fieldSet.name( ctyConfig.getName() );
        fieldSet.label( ctyConfig.getName() );

        for ( DataEntryConfig entry : ctyConfig.getInputConfigs() )
        {
            fieldSet.addFormItem( convertConfigEntry( entry ) );
        }
        return fieldSet.build();
    }

    private FormItem addFormItemSet( final CtySetConfig ctyConfig )
    {
        final String blockName = StringUtils.substringAfterLast( ctyConfig.getGroupXPath(), "/" );
        final FormItemSet.Builder formItemSet = FormItemSet.create();
        formItemSet.name( blockName );
        formItemSet.label( ctyConfig.getName() );

        for ( DataEntryConfig entry : ctyConfig.getInputConfigs() )
        {
            formItemSet.addFormItem( convertConfigEntry( entry ) );
        }
        return formItemSet.build();
    }

    private FormItem convertConfigEntry( final DataEntryConfig entry )
    {
        final String label = Strings.isNullOrEmpty( entry.getDisplayName() ) ? entry.getName() : entry.getDisplayName();
        final Input.Builder input = Input.create().
            name( entry.getName() ).
            label( label ).
            required( entry.isRequired() ).
            customText( entry.getXpath() ).
            inputType( InputTypeName.TEXT_LINE );

        final DataEntryConfigType type = entry.getType();
        switch ( type )
        {
            case BINARY:
                // deprecated
                break;
            case CHECKBOX:
                convertCheckBoxEntry( (CheckboxDataEntryConfig) entry, input );
                break;
            case DATE:
                convertDateEntry( (DateDataEntryConfig) entry, input );
                break;
            case DROPDOWN:
                convertDropDownEntry( (DropdownDataEntryConfig) entry, input );
                break;
            case FILE:
                convertFileEntry( (FileDataEntryConfig) entry, input );
                break;
            case FILES:
                // deprecated
                break;
            case HTMLAREA:
                convertHtmlAreaEntry( (HtmlAreaDataEntryConfig) entry, input );
                break;
            case IMAGE:
                convertImageEntry( (ImageDataEntryConfig) entry, input );
                break;
            case IMAGES:
                // deprecated
                break;
            case KEYWORDS:
                // deprecated
                break;
            case MULTIPLE_CHOICE:
                convertMultipleChoiceEntry( (MultipleChoiceDataEntryConfig) entry, input );
                break;
            case RADIOBUTTON:
                convertRadioEntry( (RadioButtonDataEntryConfig) entry, input );
                break;
            case RELATEDCONTENT:
                convertRelatedContentEntry( (RelatedContentDataEntryConfig) entry, input );
                break;
            case TEXT:
                convertTextEntry( (TextDataEntryConfig) entry, input );
                break;
            case TEXT_AREA:
                convertTextAreaEntry( (TextAreaDataEntryConfig) entry, input );
                break;
            case URL:
                convertUrlEntry( (UrlDataEntryConfig) entry, input );
                break;
            case XML:
                convertXmlEntry( (XmlDataEntryConfig) entry, input );
                break;
        }
        return input.build();
    }

    private void convertFileEntry( final FileDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.FILE_UPLOADER );
    }

    private void convertMultipleChoiceEntry( final MultipleChoiceDataEntryConfig entry, final Input.Builder input )
    {
        // TODO ignore?
        input.inputType( InputTypeName.COMBO_BOX );
    }

    private void convertRelatedContentEntry( final RelatedContentDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.CONTENT_SELECTOR );
        input.maximumOccurrences( entry.isMultiple() ? 0 : 1 );
        for ( String allowedContentTypeName : entry.getContentTypeNames() )
        {
            final String allowedType = ContentTypeName.from( appKey, allowedContentTypeName ).toString();
            input.inputTypeProperty( InputTypeProperty.create( "allowedContentType", allowedType ).build() );
        }
    }

    private void convertDateEntry( final DateDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.DATE );
    }

    private void convertImageEntry( final ImageDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.IMAGE_SELECTOR );
    }

    private void convertCheckBoxEntry( final CheckboxDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.CHECK_BOX );
    }

    private void convertDropDownEntry( final DropdownDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.COMBO_BOX );
        setSelectorOptions( entry, input );
    }

    private void convertRadioEntry( final RadioButtonDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.RADIO_BUTTON );
        setSelectorOptions( entry, input );
    }

    private void setSelectorOptions( final SelectorDataEntryConfig entry, final Input.Builder input )
    {
        // get option values using reflection
        try
        {
            final Field field = SelectorDataEntryConfig.class.getDeclaredField( "optionValuesWithDescriptions" );
            field.setAccessible( true );
            LinkedHashMap<String, String> optionValuesWithDescriptions = (LinkedHashMap<String, String>) field.get( entry );
            optionValuesWithDescriptions.entrySet().stream().
                forEach( e -> input.inputTypeProperty( InputTypeProperty.create( "option", e.getValue() ).
                    attribute( "value", e.getKey() ).
                    build() ) );
        }
        catch ( IllegalAccessException | NoSuchFieldException e )
        {
            e.printStackTrace();
        }
    }

    private void convertHtmlAreaEntry( final HtmlAreaDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.HTML_AREA );
    }

    private void convertTextEntry( final TextDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.TEXT_LINE );
    }

    private void convertTextAreaEntry( final TextAreaDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.TEXT_AREA );
    }

    private void convertUrlEntry( final UrlDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.TEXT_LINE );
    }

    private void convertXmlEntry( final XmlDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.TEXT_AREA );
    }
}
