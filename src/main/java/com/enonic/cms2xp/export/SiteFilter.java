package com.enonic.cms2xp.export;

import java.util.HashSet;
import java.util.Set;

import com.enonic.cms2xp.config.ExcludeConfig;
import com.enonic.cms2xp.config.IncludeConfig;

import com.enonic.cms.core.structure.SiteEntity;

public final class SiteFilter
{
    private final Set<String> excludeSiteNames;

    private final Set<Integer> excludeSiteKeys;

    private final Set<String> includeSiteNames;

    private final Set<Integer> includeSiteKeys;

    private boolean excludeMode;

    public SiteFilter( final ExcludeConfig excludeConfig, final IncludeConfig includeConfig )
    {
        final HashSet<String> excludeNames = new HashSet<>();
        final HashSet<Integer> excludeKeys = new HashSet<>();
        if ( excludeConfig != null && excludeConfig.site != null )
        {
            for ( String value : excludeConfig.site )
            {
                if ( value.matches( "^\\d+$" ) )
                {
                    excludeKeys.add( new Integer( value ) );
                }
                else
                {
                    excludeNames.add( value );
                }
            }
        }
        excludeSiteNames = excludeNames;
        excludeSiteKeys = excludeKeys;

        final HashSet<String> includeNames = new HashSet<>();
        final HashSet<Integer> includeKeys = new HashSet<>();
        if ( includeConfig != null && includeConfig.site != null )
        {
            for ( String value : includeConfig.site )
            {
                if ( value.matches( "^\\d+$" ) )
                {
                    includeKeys.add( new Integer( value ) );
                }
                else
                {
                    includeNames.add( value );
                }
            }
        }
        includeSiteNames = includeNames;
        includeSiteKeys = includeKeys;

        excludeMode = includeSiteNames.isEmpty() && includeSiteKeys.isEmpty();
    }

    public boolean includeSite( final SiteEntity siteEntity )
    {
        return excludeMode ? !isSiteExcluded( siteEntity ) : isSiteIncluded( siteEntity );
    }

    private boolean isSiteExcluded( final SiteEntity siteEntity )
    {
        return excludeSiteNames.contains( siteEntity.getName() ) || excludeSiteKeys.contains( siteEntity.getKey().toInt() );
    }

    private boolean isSiteIncluded( final SiteEntity siteEntity )
    {
        return includeSiteNames.contains( siteEntity.getName() ) || includeSiteKeys.contains( siteEntity.getKey().toInt() );
    }
}
