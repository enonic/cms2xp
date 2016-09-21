package com.enonic.cms2xp.migrate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.enonic.cms2xp.config.ExcludeConfig;

import com.enonic.cms.core.structure.SiteEntity;

public final class SiteFilter
    implements Predicate<SiteEntity>
{
    private final Set<String> menuNames;

    public SiteFilter( final ExcludeConfig excludeConfig )
    {
        if ( excludeConfig != null && excludeConfig.site != null )
        {
            menuNames = new HashSet<>( Arrays.asList( excludeConfig.site ) );
        }
        else
        {
            menuNames = Collections.EMPTY_SET;
        }
    }

    @Override
    public boolean test( final SiteEntity siteEntity )
    {
        return !menuNames.contains( siteEntity.getName() );
    }
}
