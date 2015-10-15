package com.enonic.cms2xp.export;

import com.enonic.xp.schema.content.ContentType;

import com.enonic.cms.core.content.contenttype.ContentTypeKey;

public interface ContentTypeResolver
{

    ContentType getContentType( ContentTypeKey contentTypeKey );

}
