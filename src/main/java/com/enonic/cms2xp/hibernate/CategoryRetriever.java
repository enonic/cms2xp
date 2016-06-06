package com.enonic.cms2xp.hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;

import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.content.category.CategoryKey;

public class CategoryRetriever
{
    public static List<CategoryKey> retrieveRootCategories( final Session session )
    {
        final List<CategoryKey> categories = new ArrayList<>();

        session.beginTransaction();
        List<CategoryEntity> rootCategoryEntities = session.getNamedQuery( "CategoryEntity.findAllRootCategories" ).list();
        categories.addAll( rootCategoryEntities.stream().map( CategoryEntity::getKey ).collect( Collectors.toList() ) );
        session.getTransaction().commit();

        return categories;
    }

    public static CategoryEntity retrieveCategory( final Session session, final CategoryKey categoryKey )
    {
        session.beginTransaction();
        CategoryEntity category = (CategoryEntity) session.get( CategoryEntity.class, categoryKey );
        if ( category == null )
        {
            return null;
        }

        if ( category.isDeleted() )
        {
            return null;
        }
        return category;
    }

    public static List<CategoryKey> retrieveSubCategories( final Session session, final CategoryKey parentCategoryKey )
    {
        session.beginTransaction();
        List<CategoryKey> categoryKeys = session.getNamedQuery( "CategoryEntity.findChildrenByCategoryKey" ).
            setParameter( "categoryKey", parentCategoryKey.toInt() ).list();
        session.getTransaction().commit();
        return categoryKeys;
    }
}
