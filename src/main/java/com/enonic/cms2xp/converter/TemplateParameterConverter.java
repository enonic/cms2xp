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

import static com.enonic.cms2xp.migrate.ExportData.PAGE_TYPE;

public class TemplateParameterConverter
{
    private final ApplicationKey applicationKey;

    private final ContentTypeName pageContentType;


    public TemplateParameterConverter( final ApplicationKey applicationKey )
    {
        this.applicationKey = applicationKey;
        this.pageContentType = ContentTypeName.from( this.applicationKey, PAGE_TYPE );
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
        input.name( parameter.getName().replace( ".", "_" ) );
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
                property( InputTypeProperty.create( "allowContentType", ContentTypeName.fragment().toString() ).build() );
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
