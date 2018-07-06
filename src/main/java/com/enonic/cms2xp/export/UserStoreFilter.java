package com.enonic.cms2xp.export;

import java.util.HashSet;
import java.util.Set;

import com.enonic.cms2xp.config.ExcludeConfig;

import com.enonic.cms.core.security.userstore.UserStoreEntity;

public final class UserStoreFilter
{
    private final Set<String> excludeUserStoreNames;

    private final Set<Integer> excludeUserStoreKeys;

    public UserStoreFilter( final ExcludeConfig excludeConfig )
    {
        final HashSet<String> excludeNames = new HashSet<>();
        final HashSet<Integer> excludeKeys = new HashSet<>();
        if ( excludeConfig != null && excludeConfig.userStore != null )
        {
            for ( String value : excludeConfig.userStore )
            {
                if ( value.trim().matches( "^\\d+$" ) )
                {
                    excludeKeys.add( new Integer( value.trim() ) );
                }
                else
                {
                    excludeNames.add( value.trim().toLowerCase() );
                }
            }
        }
        excludeUserStoreNames = excludeNames;
        excludeUserStoreKeys = excludeKeys;
    }

    public boolean excludeUserStore( final UserStoreEntity userStoreEntity )
    {
        return excludeUserStoreNames.contains( userStoreEntity.getName().trim().toLowerCase() ) ||
            excludeUserStoreKeys.contains( userStoreEntity.getKey().toInt() );
    }
}
