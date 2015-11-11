package com.enonic.xp.core.impl.security;

import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.index.IndexConfig;
import com.enonic.xp.index.IndexConfigDocument;
import com.enonic.xp.index.PatternIndexConfigDocument;

class PrincipalIndexConfigFactory
{
    public static IndexConfigDocument create()
    {
        // TODO: User correct analyzer when repository system is created
        return PatternIndexConfigDocument.create().
            analyzer( ContentConstants.DOCUMENT_INDEX_DEFAULT_ANALYZER ).
            add( PrincipalPropertyNames.DISPLAY_NAME_KEY, IndexConfig.FULLTEXT ).
            add( PrincipalPropertyNames.PRINCIPAL_TYPE_KEY, IndexConfig.MINIMAL ).
            add( PrincipalPropertyNames.USER_STORE_KEY, IndexConfig.MINIMAL ).
            add( PrincipalPropertyNames.EMAIL_KEY, IndexConfig.FULLTEXT ).
            add( PrincipalPropertyNames.LOGIN_KEY, IndexConfig.MINIMAL ).
            add( PrincipalPropertyNames.PRINCIPAL_KEY, IndexConfig.MINIMAL ).
            add( PrincipalPropertyNames.MEMBER_KEY, IndexConfig.MINIMAL ).
            add( PrincipalPropertyNames.AUTHENTICATION_HASH_KEY, IndexConfig.NONE ).
            defaultConfig( IndexConfig.MINIMAL ).
            build();
    }

}
