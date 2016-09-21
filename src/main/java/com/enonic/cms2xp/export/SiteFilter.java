package com.enonic.cms2xp.export;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.enonic.cms2xp.config.ExcludeConfig;

import com.enonic.cms.core.structure.SiteEntity;

public final class SiteFilter
    implements Predicate<SiteEntity>
{
    private final Set<String> siteNames;

    private final Set<Integer> siteKeys;

    public SiteFilter( final ExcludeConfig excludeConfig )
    {
        final HashSet<String> names = new HashSet<>();
        final HashSet<Integer> keys = new HashSet<>();
        if ( excludeConfig != null && excludeConfig.site != null )
        {
            for ( String value : excludeConfig.site )
            {
                if ( value.matches( "^\\d+$" ) )
                {
                    keys.add( new Integer( value ) );
                }
                else
                {
                    names.add( value );
                }
            }
        }
        siteNames = names;
        siteKeys = keys;
    }

    @Override
    public boolean test( final SiteEntity siteEntity )
    {
        return !siteNames.contains( siteEntity.getName() ) && !siteKeys.contains( siteEntity.getKey().toInt() );
    }
}
