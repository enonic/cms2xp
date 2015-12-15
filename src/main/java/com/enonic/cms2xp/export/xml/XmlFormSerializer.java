package com.enonic.cms2xp.export.xml;

import java.util.Map;

import com.enonic.xp.form.FieldSet;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormItems;
import com.enonic.xp.form.InlineMixin;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeConfig;
import com.enonic.xp.inputtype.InputTypeProperty;
import com.enonic.xp.xml.DomBuilder;
import com.enonic.xp.xml.DomHelper;

public final class XmlFormSerializer
    extends XmlSerializer
{
    private Form form;

    public XmlFormSerializer( final DomBuilder builder )
    {
        super( builder );
    }

    public XmlFormSerializer( final String rootName )
    {
        super( DomBuilder.create( rootName ) );
    }

    public XmlFormSerializer form( final Form form )
    {
        this.form = form;
        return this;
    }

    public String serialize()
    {
        serializeForm();
        return DomHelper.serialize( this.builder.getDocument() );
    }

    protected void serializeForm()
    {
        this.form.getFormItems().forEach( this::serialize );
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
        serializeValueElement( "custom-text", formItemSet.getCustomText() );
        serializeValueElement( "help-text", formItemSet.getHelpText() );
        serializeValueElement( "immutable", formItemSet.isImmutable() );
        serialize( formItemSet.getOccurrences() );
        serialize( formItemSet.getFormItems() );
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
        serialize( input.getInputTypeConfig() );

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

    private void serialize( final InputTypeConfig inputTypeConfig )
    {
        if ( inputTypeConfig != null )
        {
            this.builder.start( "config" );
            for ( InputTypeProperty inputTypeProperty : inputTypeConfig )
            {
                this.builder.start( inputTypeProperty.getName() );
                for ( Map.Entry<String, String> attribute : inputTypeProperty.getAttributes().entrySet() )
                {
                    serializeValueAttribute( attribute.getKey(), attribute.getValue() );
                }
                if ( inputTypeProperty.getValue() != null )
                {
                    this.builder.text( inputTypeProperty.getValue() );
                }
                this.builder.end();
            }
            this.builder.end();
        }
    }

    private void serialize( final FormItems formItems )
    {
        this.builder.start( "items" );
        formItems.forEach( this::serialize );
        this.builder.end();
    }
}
