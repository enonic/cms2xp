package com.enonic.cms2xp.converter;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms2xp.export.ContentKeyResolver;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.content.ContentPropertyNames;
import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.util.Reference;

import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.section.SectionContentEntity;

public class MenuItemNodeConverter
    extends AbstractNodeConverter
{
    private final static Logger logger = LoggerFactory.getLogger( MenuItemNodeConverter.class );

    private final ApplicationKey applicationKey;

    private final ContentKeyResolver contentKeyResolver;

    public MenuItemNodeConverter( final ApplicationKey applicationKey, final ContentKeyResolver contentKeyResolver )
    {
        this.applicationKey = applicationKey;
        this.contentKeyResolver = contentKeyResolver;
    }

    public Node convertToNode( final MenuItemEntity menuItemEntity )
    {
        return createNode( new NodeId(), menuItemEntity.getName(), toData( menuItemEntity ) );
    }

    private PropertyTree toData( final MenuItemEntity menuItem )
    {
        final ContentTypeName type = ContentTypeName.from( this.applicationKey, menuItem.isSection() ? "section" : "page" );

        final PropertyTree data = new PropertyTree();
        data.setBoolean( ContentPropertyNames.VALID, true );
        data.setString( ContentPropertyNames.DISPLAY_NAME, menuItem.getName() );
        data.setString( ContentPropertyNames.TYPE, type.toString() );
        if ( menuItem.getLanguage() != null )
        {
            data.setString( ContentPropertyNames.LANGUAGE, menuItem.getLanguage().getCode() );
        }
        data.setInstant( ContentPropertyNames.MODIFIED_TIME, menuItem.getTimestamp().toInstant() );
        data.setString( ContentPropertyNames.MODIFIER, SUPER_USER_KEY );
        data.setString( ContentPropertyNames.CREATOR, SUPER_USER_KEY );
        //TODO No created time info?
        data.setInstant( ContentPropertyNames.CREATED_TIME, menuItem.getTimestamp().toInstant() );

        final PropertySet subData = new PropertySet();
        final boolean isSite = menuItem.isRootPage();
        if ( isSite )
        {
            final PropertySet siteConfig = new PropertySet();
            siteConfig.setProperty( "applicationKey", ValueFactory.newString( applicationKey.toString() ) );
            siteConfig.setProperty( "config", ValueFactory.newPropertySet( new PropertySet() ) );

            subData.setProperty( "description", ValueFactory.newString( menuItem.getName() ) ); //TODO No description?
            subData.setProperty( "siteConfig", ValueFactory.newPropertySet( siteConfig ) );
        }
        else if ( menuItem.isSection() )
        {
            final Set<SectionContentEntity> sectionContents = menuItem.getSectionContents();
            final Comparator<SectionContentEntity> byOrder = ( s1, s2 ) -> Integer.compare( s1.getOrder(), s2.getOrder() );
            final List<SectionContentEntity> sortedSections = sectionContents.stream().sorted( byOrder ).collect( Collectors.toList() );

            for ( SectionContentEntity sectionContent : sortedSections )
            {
                final NodeId sectionContentId = this.contentKeyResolver.resolve( sectionContent.getContent().getKey() );
                if ( sectionContentId == null )
                {
                    logger.warn( "Cannot resolve NodeId for content with key '" + sectionContent.getContent().getKey() +
                                     "', published in section '" + sectionContent.getKey() + "'" );
                    continue;
                }
                subData.addProperty( "sectionContents", ValueFactory.newReference( Reference.from( sectionContentId.toString() ) ) );
            }
        }

        data.setSet( ContentPropertyNames.DATA, subData );

        data.setSet( ContentPropertyNames.EXTRA_DATA, new PropertySet() );
        return data;
    }

}
