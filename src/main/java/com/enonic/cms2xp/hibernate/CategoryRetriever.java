package com.enonic.cms2xp.hibernate;

import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms.core.content.category.CategoryEntity;

public class CategoryRetriever
{
    private final static Logger logger = LoggerFactory.getLogger( CategoryRetriever.class );

    public static List<CategoryEntity> retrieveRootCategories( final Session session )
    {

        session.beginTransaction();
        List<CategoryEntity> rootCategoryEntities = session.getNamedQuery( "CategoryEntity.findAllRootCategories" ).list();
        for ( CategoryEntity rootCategoryEntity : rootCategoryEntities )
        {
            logger.info( "Retrieved category: " + rootCategoryEntity.toString() );
        }
        session.getTransaction().commit();

        return rootCategoryEntities;
    }
}
