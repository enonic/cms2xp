package com.enonic.cms2xp.export;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.dom.DOMSource;

import org.apache.commons.io.FilenameUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import com.enonic.cms2xp.converter.FragmentsNodeConverter;
import com.enonic.cms2xp.converter.TemplateParameterConverter;
import com.enonic.cms2xp.export.xml.XmlFormSerializer;
import com.enonic.cms2xp.hibernate.PortletRetriever;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.core.impl.export.NodeExporter;
import com.enonic.xp.form.Form;
import com.enonic.xp.name.NamePrettyfier;
import com.enonic.xp.node.Node;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.node.NodePath;

import com.enonic.cms.framework.xml.XMLDocumentHelper;

import com.enonic.cms.core.structure.SiteEntity;
import com.enonic.cms.core.structure.portlet.PortletEntity;
import com.enonic.cms.core.structure.portlet.PortletKey;

public class PortletExporter
    extends AbstractAppComponentExporter
    implements PortletToPartResolver
{
    private final static Logger logger = LoggerFactory.getLogger( PortletExporter.class );

    private final Session session;

    private final ApplicationKey applicationKey;

    private final NodeExporter nodeExporter;

    private final FragmentsNodeConverter fragmentsNodeConverter;

    private final File target;

    private final Map<String, String> xsltPathToPartNameMapping;

    private final Map<PortletKey, NodeId> fragmentToPortletMapping;

    public PortletExporter( final Session session, final File target, final NodeExporter nodeExporter, final ApplicationKey applicationKey )
    {
        this.session = session;
        this.target = target;
        this.xsltPathToPartNameMapping = new HashMap<>();
        this.fragmentToPortletMapping = new HashMap<>();
        this.applicationKey = applicationKey;
        this.fragmentsNodeConverter = new FragmentsNodeConverter( applicationKey, this );
        this.nodeExporter = nodeExporter;
    }

    @Override
    public String partNameFromPortlet( final PortletEntity portlet )
    {
        final String xsltPath = portlet.getStyleKey().toString();
        return this.xsltPathToPartNameMapping.get( xsltPath );
    }

    @Override
    public NodeId fragmentReferenceFromPortlet( final PortletKey portletKey )
    {
        return fragmentToPortletMapping.get( portletKey );
    }

    public Node export( final SiteEntity siteEntity, final NodePath parentNode )
    {
        final List<PortletEntity> portletEntities = new PortletRetriever( session ).retrievePortlets( siteEntity.getKey() );
        exportParts( portletEntities );
        return exportFragments( portletEntities, parentNode );
    }

    private void exportParts( Iterable<PortletEntity> portletEntities )
    {
        for ( PortletEntity portletEntity : portletEntities )
        {
            final String xsltPath = portletEntity.getStyleKey().toString();
            if ( xsltPathToPartNameMapping.containsKey( xsltPath ) )
            {
                continue;
            }

            String partName = partNameFromXslt( portletEntity );
            int i = 1;
            while ( xsltPathToPartNameMapping.values().contains( partName ) )
            {
                partName = partNameFromXslt( portletEntity ) + "-" + ( ++i );
            }

            exportAsPart( portletEntity, partName );
            xsltPathToPartNameMapping.put( xsltPath, partName );
        }
    }

    private Node exportFragments( final Iterable<PortletEntity> portletEntities, final NodePath parentNode )
    {

        Node templateFolderNode = fragmentsNodeConverter.getFragmentsNode();
        templateFolderNode = Node.create( templateFolderNode ).
            parentPath( parentNode ).
            build();
        templateFolderNode = nodeExporter.exportNode( templateFolderNode );

        for ( PortletEntity portlet : portletEntities )
        {

            Node contentNode = fragmentsNodeConverter.toNode( portlet );
            contentNode = Node.create( contentNode ).
                parentPath( templateFolderNode.path() ).
                build();
            fragmentToPortletMapping.put( portlet.getPortletKey(), contentNode.id() );

            try
            {
                contentNode = nodeExporter.exportNode( contentNode );
            }
            catch ( Exception e )
            {
                logger.warn( "Could not export node '" + contentNode.path() + "'", e );
            }
        }
        return templateFolderNode;
    }

    private void exportAsPart( final PortletEntity portletEntity, final String partName )
    {
        final String portletDisplayName = partName; // portletEntity.getName();

        final Map<String, Object> mapping = new HashMap<>();
        mapping.put( "portletName", partName );
        mapping.put( "portletDisplayName", portletDisplayName );
        final Form form = new TemplateParameterConverter( applicationKey ).toFormXml( portletEntity.getTemplateParameters().values() );
        final String config = new XmlFormSerializer( "config" ).form( form ).serialize().trim();
        mapping.put( "portletConfig", config );
        final String dataSources = getDataSource( portletEntity );
        mapping.put( "dataSources", dataSources );

        try
        {
            copy( "/templates/parts/part/part.html", new File( target, partName + "/" + partName + ".html" ), mapping );
            copy( "/templates/parts/part/part.js", new File( target, partName + "/" + partName + ".js" ), mapping );
            copy( "/templates/parts/part/part.xml", new File( target, partName + "/" + partName + ".xml" ), mapping );
        }
        catch ( Exception e )
        {
            logger.error( "Error while exporting PortletEntity \"" + portletDisplayName + "\"", e );
        }
    }

    private String partNameFromXslt( final PortletEntity portletEntity )
    {
        final String xsltName = FilenameUtils.removeExtension( portletEntity.getStyleKey().getName() );
        return NamePrettyfier.create( xsltName );
    }

    private String getDataSource( final PortletEntity portletEntity )
    {
        final org.w3c.dom.Document xmlDataSource = XMLDocumentHelper.convertToW3CDocument( portletEntity.getXmlDataAsJDOMDocument() );
        final NodeList dataSources = xmlDataSource.getDocumentElement().getElementsByTagName( "datasources" );
        if ( dataSources == null || dataSources.getLength() == 0 )
        {
            return "";
        }
        return XmlHelper.convertToString( new DOMSource( dataSources.item( 0 ) ) );
    }

}
