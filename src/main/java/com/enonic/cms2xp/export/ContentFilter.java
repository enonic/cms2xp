package com.enonic.cms2xp.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.enonic.cms2xp.config.ExcludeConfig;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.structure.SiteEntity;

public final class ContentFilter
    implements Predicate<SiteEntity>
{
    private final List<String> excludePaths;

    public ContentFilter( final ExcludeConfig excludeConfig )
    {
        if ( excludeConfig != null && excludeConfig.contentPath != null )
        {
            excludePaths = Arrays.asList( excludeConfig.contentPath );
        }
        else
        {
            excludePaths = new ArrayList<>();
        }
    }

    @Override
    public boolean test( final SiteEntity siteEntity )
    {
        return !excludePaths.contains( siteEntity.getName() );
    }

    public boolean skipCategory( final CategoryEntity category )
    {
        final String path = category.getPathAsString();
        for ( String excludedPath : excludePaths )
        {
            if ( path.equals( excludedPath ) || path.startsWith( excludedPath + "/" ) )
            {
                return true;
            }
        }
        return false;
    }

    public boolean skipContent( final ContentEntity content )
    {
        final String path = content.getPathAsString();
        for ( String excludedPath : excludePaths )
        {
            if ( path.equals( excludedPath ) )
            {
                return true;
            }
        }
        return false;
    }
}
