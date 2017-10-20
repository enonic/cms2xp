package com.enonic.cms2xp.converter;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.form.FieldSet;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeConfig;
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
import com.enonic.cms.core.content.contenttype.dataentryconfig.BinaryDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.CheckboxDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfigType;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DateDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DropdownDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.FileDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.HtmlAreaDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.ImageDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.ImagesDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.MultipleChoiceDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.RadioButtonDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.RelatedContentDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.SelectorDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.TextAreaDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.TextDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.UrlDataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.XmlDataEntryConfig;

import static com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfigType.IMAGES;
import static org.apache.commons.lang.StringUtils.substringBeforeLast;

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
            filter( this::isConvertible ).
            forEach( ( ct ) ->
                     {
                         final ContentType contentType = convert( ct );
                         this.typeResolver.put( ct.getContentTypeKey(), contentType );
                         logger.info( "Converted content type: {}", contentType.getName() );
                     } );
        return ImmutableList.copyOf( typeResolver.values() );
    }

    @Override
    public ContentType getContentType( final ContentTypeKey contentTypeKey )
    {
        return typeResolver.get( contentTypeKey );
    }

    private boolean isConvertible( final ContentTypeEntity ct )
    {
        final ContentHandlerName name = ct.getContentHandlerName();
        return name == ContentHandlerName.CUSTOM || name == ContentHandlerName.FORM || name == ContentHandlerName.NEWSLETTER;
    }

    private ContentType convert( final ContentTypeEntity ct )
    {
        Form form;

        if ( ct.getContentHandlerName() == ContentHandlerName.FORM )
        {
            form = Form.create().build();
        }
        else if ( ct.getContentHandlerName() == ContentHandlerName.NEWSLETTER )
        {
            form = newsletterForm();
        }
        else
        {
            try
            {
                form = convertConfig( ct.getContentTypeKey(), parseContentTypeConfig( ct ) );
            }
            catch ( IllegalArgumentException e )
            {
                logger.warn( "Error converting config for content type " + ct.getName(), e );
                form = Form.create().build();
            }
            catch ( InvalidContentTypeConfigException e )
            {
                logger.warn( "Cannot get config for content type " + ct.getName(), e );
                form = Form.create().build();
            }
        }
        final ContentType.Builder contentType = ContentType.create().
            name( ContentTypeName.from( appKey, ct.getName() ) ).
            displayName( ct.getName() ).
            description( ct.getDescription() ).
            createdTime( ct.getTimestamp().toInstant() ).
            superType( ContentTypeName.structured() ).
            form( form );
        return contentType.build();
    }

    // extracted from com.enonic.cms.core.content.contenttype.ContentTypeEntity
    public static ContentTypeConfig parseContentTypeConfig( final ContentTypeEntity ct )
    {
        Document configData = ct.getData();
        ContentHandlerName contentHandlerName = ct.getContentHandlerName();

        if ( !ContentHandlerName.CUSTOM.equals( contentHandlerName ) )
        {
            throw new IllegalStateException( "This method is only supported when the content type based on the custom handler" );
        }

        if ( configData == null )
        {
            return null;
        }
        Document contentTypeDoc = configData;
        if ( contentTypeDoc == null )
        {
            return null;
        }

        Element contentTypeRootEl = contentTypeDoc.getRootElement();
        if ( "config".equals( contentTypeRootEl.getName() ) )
        {
            return ContentTypeConfigParser.parse( ct.getContentTypeKey(), contentHandlerName, contentTypeRootEl );
        }

        Element contentTypeConfigEl = contentTypeRootEl.getChild( "config" );
        if ( contentTypeConfigEl == null )
        {
            return null;
        }

        return ContentTypeConfigParser.parse( ct.getContentTypeKey(), contentHandlerName, contentTypeConfigEl );
    }

    private Form newsletterForm()
    {
        final Form.Builder form = Form.create();
        form.addFormItem( Input.create().
            name( "subject" ).
            label( "Subject" ).
            inputType( InputTypeName.TEXT_LINE ).
            required( true ).
            multiple( false ).
            build() );
        form.addFormItem( Input.create().
            name( "summary" ).
            label( "Summary" ).
            inputType( InputTypeName.TEXT_AREA ).
            required( false ).
            multiple( false ).
            build() );
        form.addFormItem( Input.create().
            name( "newsletter" ).
            label( "Newsletter" ).
            inputType( InputTypeName.HTML_AREA ).
            required( true ).
            multiple( false ).
            build() );
        final ContentTypeName pageContentType = ContentTypeName.from( this.appKey, "page" );
        final ContentTypeName sectionContentType = ContentTypeName.from( this.appKey, "section" );
        form.addFormItem( Input.create().
            name( "page" ).
            label( "Newsletter page" ).
            inputType( InputTypeName.CONTENT_SELECTOR ).
            inputTypeConfig( InputTypeConfig.create().
                property( InputTypeProperty.create( "allowContentType", pageContentType.toString() ).build() ).
                property( InputTypeProperty.create( "allowContentType", sectionContentType.toString() ).build() ).
                build() ).
            required( false ).
            multiple( false ).
            build() );

        return form.build();
    }

    private Form convertConfig( final ContentTypeKey contentTypeKey, final ContentTypeConfig contentTypeConfig )
    {
        if ( contentTypeConfig == null )
        {
            return Form.create().build();
        }

        Form.Builder form = Form.create();
        final Set<String> addedItemNames = new HashSet<>();

        final List<CtySetConfig> setConfig = contentTypeConfig.getSetConfig();
        for ( CtySetConfig ctyConfig : setConfig )
        {
            final FormItem formItem;
            if ( ctyConfig.hasGroupXPath() )
            {
                formItem = addFormItemSet( contentTypeKey, ctyConfig );
            }
            else if ( blockHasCommonXpathPrefix( ctyConfig ) )
            {
                formItem = addVisualBlockAsFormItemSet( contentTypeKey, ctyConfig );
            }
            else
            {
                formItem = addFieldSet( contentTypeKey, ctyConfig );
            }

            if ( addedItemNames.contains( formItem.getName() ) && formItem instanceof FormItemSet )
            {
                final Form.Builder newForm = mergeItemSets( form.build(), (FormItemSet) formItem );
                if ( newForm == null )
                {
                    logger.warn(
                        "Duplicated item name in Content Type form: " + formItem.getName() + " (" + ( (FormItemSet) formItem ).getLabel() +
                            ")" );
                }
                else
                {
                    form = newForm;
                    addedItemNames.add( formItem.getName() );
                }
            }
            else
            {
                form.addFormItem( formItem );
                addedItemNames.add( formItem.getName() );
            }
        }

        return form.build();
    }

    private Form.Builder mergeItemSets( final Form form, final FormItemSet newFormItemSet )
    {
        final Form.Builder newForm = Form.create();
        for ( FormItem item : form )
        {
            if ( item.getName().equals( newFormItemSet.getName() ) )
            {
                if ( item instanceof FormItemSet )
                {
                    final FormItemSet.Builder fis = FormItemSet.create( (FormItemSet) item );
                    for ( FormItem fi : newFormItemSet )
                    {
                        fis.addFormItem( fi.copy() );
                    }
                    try
                    {
                        newForm.addFormItem( fis.build() );
                        logger.info( "Item sets merged: " + newFormItemSet.getPath() + " <- " + item.getPath() );
                    }
                    catch ( Exception e )
                    {
                        logger.error( "Could not merge item sets: ", e );
                        return null;
                    }
                }
                else
                {
                    return null;
                }
            }
            else
            {
                newForm.addFormItem( item.copy() );
            }
        }
        return newForm;
    }

    private boolean blockHasCommonXpathPrefix( final CtySetConfig ctyConfig )
    {
        final List<String> xpathPrefixes = ctyConfig.getInputConfigs().stream().
            map( DataEntryConfig::getXpath ).
            map( this::normalizedXpathPrefix ).
            collect( Collectors.toList() );

        if ( xpathPrefixes.stream().allMatch( Objects::isNull ) )
        {
            return false; // xpath without prefix, only one level
        }
        final String firstElement = xpathPrefixes.get( 0 );
        return xpathPrefixes.stream().allMatch( ( xpath ) -> Objects.equals( firstElement, xpath ) );
    }

    private String normalizedXpathPrefix( final String xpath )
    {
        final List<String> xpathParts = Stream.of( xpath.split( "/" ) ).filter( ( s ) -> !s.isEmpty() ).collect( Collectors.toList() );
        if ( !xpathParts.isEmpty() && xpathParts.get( 0 ).equals( "contentdata" ) )
        {
            xpathParts.remove( 0 );
        }
        if ( xpathParts.size() == 1 )
        {
            return null;
        }
        xpathParts.remove( xpathParts.size() - 1 );
        return "/" + Joiner.on( "/" ).join( xpathParts );
    }

    private String normalizeXpath( final String xpath )
    {
        final List<String> xpathParts = Stream.of( xpath.split( "/" ) ).filter( ( s ) -> !s.isEmpty() ).collect( Collectors.toList() );
        if ( !xpathParts.isEmpty() && xpathParts.get( 0 ).equals( "contentdata" ) )
        {
            xpathParts.remove( 0 );
        }
        return "/" + Joiner.on( "/" ).join( xpathParts );
    }

    private FormItem addFieldSet( final ContentTypeKey contentTypeKey, final CtySetConfig ctyConfig )
    {
        final FieldSet.Builder fieldSet = FieldSet.create();
        fieldSet.name( ctyConfig.getName().replace( ".", " " ).trim() );
        fieldSet.label( ctyConfig.getName() );

        // group in item-sets if multilevel xpath
        final Multimap<String, DataEntryConfig> configEntriesByXpath = groupConfigEntries( ctyConfig.getInputConfigs() );
        final Map<String, FormItemSet.Builder> xpathPrefixItemSets = new HashMap<>();
        for ( String xpathPrefix : configEntriesByXpath.keySet() )
        {
            final Collection<DataEntryConfig> entries = configEntriesByXpath.get( xpathPrefix );
            if ( "".equals( xpathPrefix ) )
            {
                for ( DataEntryConfig entry : entries )
                {
                    fieldSet.addFormItem( convertConfigEntry( contentTypeKey, entry ) );
                }
            }
            else
            {
                final List<String> xpathParts = Splitter.on( "/" ).omitEmptyStrings().splitToList( xpathPrefix );

                FormItemSet.Builder is = xpathPrefixItemSets.get( xpathPrefix );
                if ( is == null )
                {
                    is = FormItemSet.create();
                    is.name( xpathParts.get( xpathParts.size() - 1 ) );
                    is.label( xpathParts.get( xpathParts.size() - 1 ) );
                    is.occurrences( 0, 1 );
                    xpathPrefixItemSets.put( xpathPrefix, is );
                }
                for ( DataEntryConfig entry : entries )
                {
                    is.addFormItem( convertConfigEntry( contentTypeKey, entry ) );
                }
            }
        }

        // wrap inputs with multilevel xpath in item-sets, so data structure matches schema
        final Map<String, FormItemSet> itemSetWrappers = new HashMap<>();
        for ( String xpathPrefix : xpathPrefixItemSets.keySet() )
        {
            final FormItemSet.Builder is = xpathPrefixItemSets.get( xpathPrefix );
            final List<String> xpathParts = Splitter.on( "/" ).omitEmptyStrings().splitToList( xpathPrefix );

            FormItemSet child = is.build();
            for ( int i = xpathParts.size() - 2; i >= 0; i-- )
            {
                final String itemPath = xpathParts.subList( 0, i + 1 ).stream().collect( Collectors.joining( "/", "/", "" ) );
                FormItemSet wrappingItemSet = itemSetWrappers.get( itemPath );
                if ( wrappingItemSet == null )
                {
                    child = FormItemSet.create().
                        name( xpathParts.get( i ) ).
                        label( xpathParts.get( i ) ).
                        occurrences( 1, 1 ).
                        addFormItem( child ).
                        build();
                    itemSetWrappers.put( itemPath, child );
                }
                else
                {
                    child = FormItemSet.create( wrappingItemSet ).
                        addFormItem( child ).
                        build();
                    itemSetWrappers.put( itemPath, child );
                }

            }
        }
        for ( FormItemSet fis : itemSetWrappers.values() )
        {
            if ( fis.getParent() == null )
            {
                fieldSet.addFormItem( fis );
            }
        }

        return fieldSet.build();
    }

    private Multimap<String, DataEntryConfig> groupConfigEntries( final List<DataEntryConfig> configEntries )
    {
        final Multimap<String, DataEntryConfig> map = LinkedListMultimap.create();
        for ( DataEntryConfig entry : configEntries )
        {
            final String xpathPrefix = normalizedXpathPrefix( entry.getXpath() );
            map.put( xpathPrefix == null ? "" : xpathPrefix, entry );
        }
        return map;
    }

    private FormItem addFormItemSet( final ContentTypeKey contentTypeKey, final CtySetConfig ctyConfig )
    {
        final String blockName = StringUtils.substringAfterLast( ctyConfig.getGroupXPath(), "/" );
        final FormItemSet.Builder formItemSet = FormItemSet.create();
        formItemSet.name( blockName );
        formItemSet.label( ctyConfig.getName() );
        formItemSet.occurrences( 0, 0 );

        for ( DataEntryConfig entry : ctyConfig.getInputConfigs() )
        {
            formItemSet.addFormItem( convertConfigEntry( contentTypeKey, entry ) );
        }
        final FormItemSet res = formItemSet.build();

        final List<String> blockNameParts =
            Splitter.on( "/" ).omitEmptyStrings().splitToList( normalizeXpath( ctyConfig.getGroupXPath() ) );
        if ( blockNameParts.size() > 1 )
        {
            FormItem child = res;
            for ( int i = blockNameParts.size() - 2; i >= 0; i-- )
            {
                final FormItemSet.Builder wrapperFormItemSet = FormItemSet.create();
                wrapperFormItemSet.name( blockNameParts.get( i ) );
                wrapperFormItemSet.label( ctyConfig.getName() );
                wrapperFormItemSet.occurrences( 0, 1 );
                wrapperFormItemSet.addFormItem( child );
                child = wrapperFormItemSet.build();
            }
            return child;
        }

        return res;
    }

    private FormItem addVisualBlockAsFormItemSet( final ContentTypeKey contentTypeKey, final CtySetConfig ctyConfig )
    {
        String groupXpath = substringBeforeLast( normalizeXpath( ctyConfig.getInputConfigs().get( 0 ).getXpath() ), "/" );
        groupXpath = groupXpath.substring( 0, groupXpath.length() );

        final String blockName = StringUtils.substringAfterLast( groupXpath, "/" );
        final FormItemSet.Builder formItemSet = FormItemSet.create();
        formItemSet.name( blockName );
        formItemSet.label( ctyConfig.getName() );
        formItemSet.occurrences( 0, 0 );

        for ( DataEntryConfig entry : ctyConfig.getInputConfigs() )
        {
            formItemSet.addFormItem( convertConfigEntry( contentTypeKey, entry ) );
        }
        final FormItemSet res = formItemSet.build();

        final List<String> blockNameParts = Splitter.on( "/" ).omitEmptyStrings().splitToList( groupXpath );
        if ( blockNameParts.size() > 2 )
        {
            final FormItemSet.Builder wrapperFormItemSet = FormItemSet.create();
            wrapperFormItemSet.name( blockNameParts.get( 1 ) );
            wrapperFormItemSet.label( ctyConfig.getName() );
            wrapperFormItemSet.occurrences( 0, 1 );
            wrapperFormItemSet.addFormItem( res );
            return wrapperFormItemSet.build();
        }

        return res;
    }

    private FormItem convertConfigEntry( final ContentTypeKey contentTypeKey, final DataEntryConfig entry )
    {
        final String label = Strings.isNullOrEmpty( entry.getDisplayName() ) ? entry.getName() : entry.getDisplayName();
        final String entryPathName = StringUtils.substringAfterLast( entry.getXpath(), "/" );
        final String inputName = StringUtils.isBlank( entryPathName ) ? entry.getName() : entryPathName;

        if ( entry.getType() == IMAGES )
        {
            // convert to ItemSet with ImageSelector + TextArea
            return convertImagesEntry( inputName, label, (ImagesDataEntryConfig) entry );
        }

        final String helpText = ContentTypeHelpMapper.getHelpText( contentTypeKey, entry );
        final Input.Builder input = Input.create().
            name( inputName ).
            label( label ).
            required( entry.isRequired() ).
            customText( entry.getXpath() ).
            helpText( helpText ).
            inputType( InputTypeName.TEXT_LINE );

        final DataEntryConfigType type = entry.getType();
        switch ( type )
        {
            case BINARY:
                convertBinaryEntry( (BinaryDataEntryConfig) entry, input );
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
        input.inputType( InputTypeName.CONTENT_SELECTOR );
        input.maximumOccurrences( 1 );
//        final String allowedType = ContentTypeName.media().toString();
//        input.inputTypeProperty( InputTypeProperty.create( "allow-content-type", allowedType ).build() );
    }

    private void convertBinaryEntry( final BinaryDataEntryConfig entry, final Input.Builder input )
    {
        input.inputType( InputTypeName.ATTACHMENT_UPLOADER );
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
            input.inputTypeProperty( InputTypeProperty.create( "allow-content-type", allowedType ).build() );
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

    private FormItemSet convertImagesEntry( final String inputName, final String label, final ImagesDataEntryConfig entry )
    {
        final Input imageInput = Input.create().
            name( "image" ).
            label( "Image" ).
            required( true ).
            customText( entry.getXpath() ).
            inputType( InputTypeName.IMAGE_SELECTOR ).
            build();
        final Input imageDescriptionInput = Input.create().
            name( "text" ).
            label( "Image text" ).
            required( false ).
            customText( entry.getXpath() ).
            inputType( InputTypeName.TEXT_AREA ).
            build();

        final FormItemSet.Builder formItemSet = FormItemSet.create().
            name( inputName ).
            label( label ).
            customText( entry.getXpath() );
        if ( entry.isRequired() )
        {
            formItemSet.occurrences( Occurrences.create( 1, 0 ) );
        }
        else
        {
            formItemSet.occurrences( Occurrences.create( 0, 0 ) );
        }
        formItemSet.addFormItem( imageInput );
        formItemSet.addFormItem( imageDescriptionInput );
        return formItemSet.build();
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
