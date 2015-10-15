package com.enonic.cms2xp.converter;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.Value;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.node.NodeId;
import com.enonic.xp.util.Reference;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.contentdata.custom.BooleanDataEntry;
import com.enonic.cms.core.content.contentdata.custom.DataEntry;
import com.enonic.cms.core.content.contentdata.custom.DataEntrySet;
import com.enonic.cms.core.content.contentdata.custom.DateDataEntry;
import com.enonic.cms.core.content.contentdata.custom.KeywordsDataEntry;
import com.enonic.cms.core.content.contentdata.custom.RelationDataEntry;
import com.enonic.cms.core.content.contentdata.custom.relationdataentrylistbased.AbstractRelationDataEntryListBasedInputDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.AbstractStringBasedInputDataEntry;
import com.enonic.cms.core.content.contentdata.custom.xmlbased.AbstractXmlBasedInputDataEntry;

public class DataEntryValuesConverter
{
    private final NodeIdRegistry nodeIdRegistry;

    public DataEntryValuesConverter( final NodeIdRegistry nodeIdRegistry )
    {
        this.nodeIdRegistry = nodeIdRegistry;
    }

    public Iterable<Value> toValue( DataEntry dataEntry )
    {
        if ( dataEntry instanceof DataEntrySet )
        {
            final Value dataEntrySetAsValue = toValue( ( (DataEntrySet) dataEntry ).getEntries() );
            return Collections.singleton( dataEntrySetAsValue );
        }

        Value value = ValueFactory.newString( "" );
        switch ( dataEntry.getType() )
        {
            case BINARY: //TODO
                break;
            case BOOLEAN:
                final Boolean booleanValue = ( (BooleanDataEntry) dataEntry ).getValueAsBoolean();
                value = ValueFactory.newBoolean( booleanValue );
                break;
            case DATE:
                final Date valueDate = ( (DateDataEntry) dataEntry ).getValue();
                value = ValueFactory.newDateTime( valueDate.toInstant() );
                break;
            case GROUP: //TODO
                break;
            case FILES:
            case IMAGES:
            case RELATED_CONTENTS:
                final List<RelationDataEntry> relationDataEntries =
                    ( (AbstractRelationDataEntryListBasedInputDataEntry<RelationDataEntry>) dataEntry ).getEntries();
                value = toValue( relationDataEntries );
                break;
            case KEYWORDS:
                return ( (KeywordsDataEntry) dataEntry ).
                    getKeywords().
                    stream().
                    map( ValueFactory::newString ).
                    collect( Collectors.toList() );
            case MULTIPLE_CHOICE://TODO
                break;
            case FILE:
            case IMAGE:
            case RELATED_CONTENT:
                final ContentKey contentKey = ( (RelationDataEntry) dataEntry ).getContentKey();
                if ( contentKey != null )
                { //TODO Why could that be null
                    final NodeId nodeId = nodeIdRegistry.getNodeId( contentKey );
                    value = ValueFactory.newReference( new Reference( nodeId ) );
                }
                break;
            case HTML_AREA:
            case TEXT_AREA:
            case SELECTOR:
            case TEXT:
            case URL:
                final String valueString = ( (AbstractStringBasedInputDataEntry) dataEntry ).getValue();
                value = ValueFactory.newString( valueString );
                break;
            case XML:
                final String valueXml = ( (AbstractXmlBasedInputDataEntry) dataEntry ).getValueAsString();
                value = ValueFactory.newXml( valueXml );
                break;
        }

        return Collections.singleton( value );
    }

    public Value toValue( Iterable<? extends DataEntry> dataEntries )
    {
        final PropertySet propertySet = new PropertySet();
        for ( DataEntry dataEntry : dataEntries )
        {
            propertySet.setValues( dataEntry.getName(), toValue( dataEntry ) );
        }

        return ValueFactory.newPropertySet( propertySet );
    }

}
