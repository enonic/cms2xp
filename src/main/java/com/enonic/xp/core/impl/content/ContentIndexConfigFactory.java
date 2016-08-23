package com.enonic.xp.core.impl.content;

import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.data.PropertyPath;
import com.enonic.xp.form.Form;
import com.enonic.xp.index.IndexConfig;
import com.enonic.xp.index.IndexConfigDocument;
import com.enonic.xp.index.PatternIndexConfigDocument;
import com.enonic.xp.schema.content.ContentTypeName;

import static com.enonic.xp.content.ContentPropertyNames.ATTACHMENT;
import static com.enonic.xp.content.ContentPropertyNames.CREATED_TIME;
import static com.enonic.xp.content.ContentPropertyNames.CREATOR;
import static com.enonic.xp.content.ContentPropertyNames.DATA;
import static com.enonic.xp.content.ContentPropertyNames.EXTRA_DATA;
import static com.enonic.xp.content.ContentPropertyNames.MODIFIED_TIME;
import static com.enonic.xp.content.ContentPropertyNames.MODIFIER;
import static com.enonic.xp.content.ContentPropertyNames.OWNER;
import static com.enonic.xp.content.ContentPropertyNames.PAGE;
import static com.enonic.xp.content.ContentPropertyNames.SITE;
import static com.enonic.xp.content.ContentPropertyNames.TYPE;

public final class ContentIndexConfigFactory
{
    private static final String PAGE_TEXT_COMPONENT_PROPERTY_PATH_PATTERN = "page.region.**.text";

    private static final String ATTACHMENT_TEXT_COMPONENT = "attachment.text";

    public static IndexConfigDocument create( final Form form, final ContentTypeName contentTypeName )
    {
        final PatternIndexConfigDocument.Builder configDocumentBuilder = PatternIndexConfigDocument.create().
            analyzer( ContentConstants.DOCUMENT_INDEX_DEFAULT_ANALYZER ).
            add( CREATOR, IndexConfig.MINIMAL ).
            add( MODIFIER, IndexConfig.MINIMAL ).
            add( CREATED_TIME, IndexConfig.MINIMAL ).
            add( MODIFIED_TIME, IndexConfig.MINIMAL ).
            add( OWNER, IndexConfig.MINIMAL ).
            add( PAGE, IndexConfig.NONE ).
            add( PAGE_TEXT_COMPONENT_PROPERTY_PATH_PATTERN, IndexConfig.FULLTEXT ).
            add( PropertyPath.from( PAGE, "regions" ), IndexConfig.NONE ).
            add( SITE, IndexConfig.NONE ).
            add( DATA, IndexConfig.BY_TYPE ).
            add( TYPE, IndexConfig.MINIMAL ).
            add( ATTACHMENT, IndexConfig.MINIMAL ).
            add( PropertyPath.from( EXTRA_DATA ), IndexConfig.MINIMAL ).
            defaultConfig( IndexConfig.BY_TYPE );

        addAttachmentTextMapping( contentTypeName, configDocumentBuilder );

        final IndexConfigVisitor indexConfigVisitor = new IndexConfigVisitor( DATA, configDocumentBuilder );
        indexConfigVisitor.traverse( form );

        return configDocumentBuilder.build();
    }

    private static void addAttachmentTextMapping( final ContentTypeName contentTypeName,
                                                  final PatternIndexConfigDocument.Builder configDocumentBuilder )
    {
        if ( isTextualMedia( contentTypeName ) )
        {
            configDocumentBuilder.add( ATTACHMENT_TEXT_COMPONENT, IndexConfig.create().
                enabled( true ).
                fulltext( true ).
                includeInAllText( true ).
                nGram( true ).
                decideByType( false ).
                build() );
        }
        else
        {
            configDocumentBuilder.add( ATTACHMENT_TEXT_COMPONENT, IndexConfig.create().
                enabled( true ).
                fulltext( true ).
                includeInAllText( false ).
                nGram( true ).
                decideByType( false ).
                build() );
        }
    }

    private static boolean isTextualMedia( final ContentTypeName contentTypeName )
    {
        return ContentTypeName.textMedia().equals( contentTypeName ) || ContentTypeName.codeMedia().equals( contentTypeName ) ||
            ContentTypeName.dataMedia().equals( contentTypeName ) || ContentTypeName.spreadsheetMedia().equals( contentTypeName ) ||
            ContentTypeName.presentationMedia().equals( contentTypeName ) || ContentTypeName.documentMedia().equals( contentTypeName );
    }
}
