package com.enonic.cms2xp.converter;

import java.util.HashMap;
import java.util.Map;

import com.enonic.cms.core.content.contenttype.ContentTypeKey;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfig;

final class ContentTypeHelpMapper
{
    private static Map<ContentTypeKey, Map<DataEntryConfig, String>> contentTypeHelpFields = new HashMap<>();

    static void setHelpText( final ContentTypeKey contentTypeKey, final DataEntryConfig entry, final String helpText )
    {
        Map<DataEntryConfig, String> helpTextMap = contentTypeHelpFields.computeIfAbsent( contentTypeKey, k -> new HashMap<>() );
        helpTextMap.put( entry, helpText );
    }

    static String getHelpText( final ContentTypeKey contentTypeKey, final DataEntryConfig entry )
    {
        Map<DataEntryConfig, String> helpTextMap = contentTypeHelpFields.get( contentTypeKey );
        if ( helpTextMap == null )
        {
            return null;
        }
        return helpTextMap.get( entry );
    }

}