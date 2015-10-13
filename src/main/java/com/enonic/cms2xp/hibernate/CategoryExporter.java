package com.enonic.cms2xp.hibernate;

import java.util.List;

import org.hibernate.Session;

import com.enonic.cms.core.content.category.CategoryEntity;

/**
 * Created by gri on 13/10/15.
 */
public class CategoryExporter
{
    public static List<CategoryEntity> retrieveRootCategories( final Session session )
    {
        session.beginTransaction();
        List<CategoryEntity> result = session.createQuery( "from com.enonic.cms.core.content.category.CategoryEntity" ).list();
        for ( CategoryEntity categoryEntity : result )
        {
            System.out.println( "CategoryEntity: " + categoryEntity.toString() );
        }
        session.getTransaction().commit();

        return result;
    }
}
