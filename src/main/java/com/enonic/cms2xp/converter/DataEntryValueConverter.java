package com.enonic.cms2xp.converter;

import java.util.Date;
import java.util.List;

import com.enonic.xp.data.PropertySet;
import com.enonic.xp.data.Value;
import com.enonic.xp.data.ValueFactory;
import com.enonic.xp.util.Reference;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.contentdata.custom.BooleanDataEntry;
import com.enonic.cms.core.content.contentdata.custom.DataEntry;
import com.enonic.cms.core.content.contentdata.custom.DataEntrySet;
import com.enonic.cms.core.content.contentdata.custom.DateDataEntry;
import com.enonic.cms.core.content.contentdata.custom.RelationDataEntry;
import com.enonic.cms.core.content.contentdata.custom.relationdataentrylistbased.AbstractRelationDataEntryListBasedInputDataEntry;
import com.enonic.cms.core.content.contentdata.custom.stringbased.AbstractStringBasedInputDataEntry;
import com.enonic.cms.core.content.contentdata.custom.xmlbased.AbstractXmlBasedInputDataEntry;

public class DataEntryValueConverter
{

    public static Value toValue( DataEntry dataEntry )
    {
        if ( dataEntry instanceof DataEntrySet )
        {
            return toValue( ( (DataEntrySet) dataEntry ).getEntries() );
        }

        switch ( dataEntry.getType() )
        {
            case BINARY: //TODO
                break;
            case BOOLEAN:
                final Boolean valueBoolean = ( (BooleanDataEntry) dataEntry ).getValueAsBoolean();
                return ValueFactory.newBoolean( valueBoolean );
            case DATE:
                final Date valueDate = ( (DateDataEntry) dataEntry ).getValue();
                return ValueFactory.newDateTime( valueDate.toInstant() );
            case GROUP: //TODO
                break;
            case FILES:
            case IMAGES:
            case RELATED_CONTENTS:
                final List<RelationDataEntry> relationDataEntries =
                    ( (AbstractRelationDataEntryListBasedInputDataEntry<RelationDataEntry>) dataEntry ).getEntries();
                return toValue( relationDataEntries );
            case KEYWORDS://TODO
                break;
            case MULTIPLE_CHOICE://TODO
                break;
            case FILE:
            case IMAGE:
            case RELATED_CONTENT:
                final ContentKey contentKey = ( (RelationDataEntry) dataEntry ).getContentKey();
                if ( contentKey != null )
                { //TODO Why could that be null
                    return ValueFactory.newReference( Reference.from( contentKey.toString() ) );//TODO Create Map ContentKey -> NodeId
                }
                break;
            case HTML_AREA:
            case TEXT_AREA:
            case SELECTOR:
            case TEXT:
            case URL:
                final String valueString = ( (AbstractStringBasedInputDataEntry) dataEntry ).getValue();
                return ValueFactory.newString( valueString );
            case XML:
                final String valueXml = ( (AbstractXmlBasedInputDataEntry) dataEntry ).getValueAsString();
                return ValueFactory.newXml( valueXml );
        }

        return ValueFactory.newString( "" );
    }

    public static Value toValue( Iterable<? extends DataEntry> dataEntries )
    {
        final PropertySet propertySet = new PropertySet();
        for ( DataEntry dataEntry : dataEntries )
        {
            propertySet.setProperty( dataEntry.getName(), toValue( dataEntry ) );
        }

        return ValueFactory.newPropertySet( propertySet );
    }

}
