package com.enonic.cms2xp.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms.core.content.category.CategoryEntity;

public class CategoryExporter
{
    private final static Logger logger = LoggerFactory.getLogger( CategoryExporter.class );

    public static List<CategoryEntity> retrieveRootCategories( final Session session )
    {

        session.beginTransaction();
        List<CategoryEntity> rootCategoryEntities = session.getNamedQuery( "CategoryEntity.findAllRootCategories" ).list();
        for ( CategoryEntity rootCategoryEntity : rootCategoryEntities )
        {
            logger.info( "Root CategoryEntity: " + rootCategoryEntity.toString() );
        }
        session.getTransaction().commit();

        return rootCategoryEntities;
    }
}
