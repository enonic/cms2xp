package com.enonic.cms2xp.converter;

import java.util.Collection;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.form.Form;
import com.enonic.xp.form.Input;
import com.enonic.xp.inputtype.InputTypeConfig;
import com.enonic.xp.inputtype.InputTypeName;
import com.enonic.xp.inputtype.InputTypeProperty;
import com.enonic.xp.schema.content.ContentTypeName;

import com.enonic.cms.core.structure.TemplateParameter;
import com.enonic.cms.core.structure.TemplateParameterType;

public class TemplateParameterConverter
{
    private final ApplicationKey applicationKey;

    private final ContentTypeName pageContentType;

    private final ContentTypeName fragmentContentType;

    public TemplateParameterConverter( final ApplicationKey applicationKey )
    {
        this.applicationKey = applicationKey;
        this.pageContentType = ContentTypeName.from( this.applicationKey, "page" );
        this.fragmentContentType = ContentTypeName.from( this.applicationKey, "fragment" );
    }

    public Form toFormXml( final Collection<TemplateParameter> parameters )
    {
        final Form.Builder form = Form.create();

        for ( TemplateParameter parameter : parameters )
        {
            final Input input = createParameterFormInput( parameter );
            form.addFormItem( input );
        }

        return form.build();
    }

    private Input createParameterFormInput( final TemplateParameter parameter )
    {
        final Input.Builder input = Input.create();
        input.name( parameter.getName() );
        input.label( parameter.getName() );

        TemplateParameterType paramType = parameter.getType();
        if ( paramType == TemplateParameterType.CATEGORY )
        {
            input.inputType( InputTypeName.CONTENT_SELECTOR );
            InputTypeConfig.create().
                property( InputTypeProperty.create( "allowContentType", ContentTypeName.folder().toString() ).build() );
        }
        else if ( paramType == TemplateParameterType.CONTENT )
        {
            input.inputType( InputTypeName.CONTENT_SELECTOR );
        }
        else if ( paramType == TemplateParameterType.OBJECT )
        {
            input.inputType( InputTypeName.CONTENT_SELECTOR );
            InputTypeConfig.create().
                property( InputTypeProperty.create( "allowContentType", fragmentContentType.toString() ).build() );
        }
        else if ( paramType == TemplateParameterType.PAGE )
        {
            input.inputType( InputTypeName.CONTENT_SELECTOR );
            InputTypeConfig.create().
                property( InputTypeProperty.create( "allowContentType", pageContentType.toString() ).build() );
        }
        else
        {
            input.inputType( InputTypeName.TEXT_LINE );
        }
        return input.build();
    }
}
