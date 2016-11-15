package com.enonic.cms2xp.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.enonic.cms2xp.config.ExcludeConfig;
import com.enonic.cms2xp.config.IncludeConfig;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.category.CategoryEntity;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.trimTrailingCharacter;

public final class ContentFilter
{
    private final List<String> excludePaths;

    private final List<String> includePaths;

    private boolean excludeMode;

    public ContentFilter( final ExcludeConfig excludeConfig, final IncludeConfig includeConfig )
    {
        if ( excludeConfig != null && excludeConfig.contentPath != null )
        {
            excludePaths = Arrays.stream( excludeConfig.contentPath ).map( ( cp ) -> trimTrailingCharacter( cp, '/' ) ).collect( toList() );
        }
        else
        {
            excludePaths = new ArrayList<>();
        }
        if ( includeConfig != null && includeConfig.contentPath != null )
        {
            includePaths = Arrays.stream( includeConfig.contentPath ).map( ( cp ) -> trimTrailingCharacter( cp, '/' ) ).collect( toList() );
        }
        else
        {
            includePaths = new ArrayList<>();
        }
        excludeMode = includePaths.isEmpty();
    }


    public boolean skipCategory( final CategoryEntity category )
    {
        return excludeMode ? isCategoryExcluded( category ) : !isCategoryIncluded( category );
    }

    private boolean isCategoryExcluded( final CategoryEntity category )
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
        return excludeMode ? isContentExcluded( content ) : !isContentIncluded( content );
    }

    private boolean isContentExcluded( final ContentEntity content )
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

    private boolean isCategoryIncluded( final CategoryEntity category )
    {
        final String path = category.getPathAsString();
        for ( String includePath : includePaths )
        {
            if ( path.equals( includePath ) || path.startsWith( includePath + "/" ) )
            {
                return true;
            }
        }
        return false;
    }

    private boolean isContentIncluded( final ContentEntity content )
    {
        final String path = content.getPathAsString();
        for ( String includePath : includePaths )
        {
            if ( path.equals( includePath ) || path.startsWith( includePath + "/" ) )
            {
                return true;
            }
        }
        return false;
    }

}
