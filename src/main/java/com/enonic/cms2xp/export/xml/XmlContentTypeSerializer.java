package com.enonic.cms2xp.export.xml;

import com.enonic.xp.form.FieldSet;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormItems;
import com.enonic.xp.form.InlineMixin;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.schema.content.ContentType;
import com.enonic.xp.schema.mixin.MixinName;
import com.enonic.xp.schema.mixin.MixinNames;
import com.enonic.xp.xml.DomBuilder;
import com.enonic.xp.xml.DomHelper;

public class XmlContentTypeSerializer
{
    private final DomBuilder builder = DomBuilder.create( "content-type" );

    private ContentType contentType;

    public XmlContentTypeSerializer contentType( ContentType contentType )
    {
        this.contentType = contentType;
        return this;
    }

    public String serialize()
    {
        serializeContentType();
        return DomHelper.serialize( this.builder.getDocument() );
    }

    private void serializeContentType()
    {
        serializeValueElement( "display-name", contentType.getDisplayName() );
        serializeValueElement( "description", contentType.getDescription() );
        serializeValueElement( "content-display-name-script", contentType.getContentDisplayNameScript() );
        serializeValueElement( "super-type", contentType.getSuperType() );
        serializeValueElement( "is-abstract", contentType.isAbstract() );
        serializeValueElement( "is-final", contentType.isFinal() );
        serializeValueElement( "allow-child-content", contentType.allowChildContent() );
        serializeMetadata();
        serializeForm();
    }

    private void serializeMetadata()
    {
        final MixinNames metadata = contentType.getMetadata();
        if ( metadata != null && metadata.isNotEmpty() )
        {
            this.builder.start( "x-data" );
            for ( final MixinName mixinName : metadata )
            {
                serializeValueElement( "mixin", mixinName );
            }
            this.builder.end();
        }
    }

    private void serializeForm()
    {
        this.builder.start( "form" );
        final Form form = contentType.getForm();
        form.getFormItems().
            forEach( this::serialize );
        this.builder.end();
    }

    private void serialize( FormItem formItem )
    {
        switch ( formItem.getType() )
        {
            case FORM_ITEM_SET:
                this.builder.start( "item-set" );
                serialize( (FormItemSet) formItem );
                this.builder.end();
                break;
            case INPUT:
                this.builder.start( "input" );
                serialize( (Input) formItem );
                this.builder.end();
                break;
            case LAYOUT:
                this.builder.start( "field-set" );
                serialize( (FieldSet) formItem );
                this.builder.end();
                break;
            case MIXIN_REFERENCE:
                this.builder.start( "inline" );
                serialize( (InlineMixin) formItem );
                this.builder.end();
                break;
        }
    }

    private void serialize( FormItemSet formItemSet )
    {
        serializeValueAttribute( "name", formItemSet.getName() );
        serializeValueElement( "label", formItemSet.getLabel() );
        serialize( formItemSet.getFormItems() );
        serialize( formItemSet.getOccurrences() );
    }

    private void serialize( Input input )
    {
        serializeValueAttribute( "type", input.getInputType() );
        serializeValueAttribute( "name", input.getName() );
        serializeValueElement( "label", input.getLabel() );
        serializeValueElement( "custom-text", input.getCustomText() );
        serializeValueElement( "immutable", input.isImmutable() );
        serializeValueElement( "indexed", input.isIndexed() );
        serializeValueElement( "validation-regexp", input.getValidationRegexp() );
        serializeValueElement( "maximize", input.isMaximizeUIInputWidth() );
        serialize( input.getOccurrences() );
    }

    private void serialize( final Occurrences occurrences )
    {
        if ( occurrences != null )
        {
            this.builder.start( "occurrences" );
            serializeValueAttribute( "minimum", occurrences.getMinimum() );
            serializeValueAttribute( "maximum", occurrences.getMaximum() );
            this.builder.end();
        }
    }

    private void serialize( FieldSet fieldSet )
    {
        serializeValueAttribute( "name", fieldSet.getName() );
        serializeValueElement( "label", fieldSet.getLabel() );
        serialize( fieldSet.getFormItems() );
    }

    private void serialize( InlineMixin inlineMixin )
    {
        serializeValueAttribute( "mixin", inlineMixin.getMixinName() );
    }

    private void serialize( final FormItems formItems )
    {
        this.builder.start( "items" );
        formItems.forEach( this::serialize );
        this.builder.end();
    }

    private void serializeValueElement( final String name, final Object value )
    {
        if ( value != null )
        {
            this.builder.start( name );
            this.builder.text( value.toString() );
            this.builder.end();
        }
    }

    private void serializeValueAttribute( final String name, final Object value )
    {
        if ( value != null )
        {
            this.builder.attribute( name, value.toString() );
        }
    }
}
