#!/bin/bash
#
cd "$(dirname "$0")"
source ${SUBJECT}

if [ ! -f $APP_RESOURCES/web-ui-configuration.xml ]; then
    #sed kann mehrfach -e haben !
    sed -e "s/\${PROVIDER}/$SPHAIRAS_PROVIDER/" -e "s/\${login-provider-display-label}/${LOGINDOMAIN:-$SPHAIRAS_HOSTNAME}/" web-ui-configuration.xml > $APP_RESOURCES/web-ui-configuration.xml
    echo "Created web-ui-configuration.xml"
fi
#Schulvorlage, Klassenlehrer-Liste
if [ ! -f $APP_RESOURCES/schulvorlage.xml ]; then
    sed -e "s#\${AUTHORITY}#${SPHAIRAS_PROVIDER}/1#" schulvorlage.xml > $APP_RESOURCES/schulvorlage.xml
    echo "Created schulvorlage.xml"
fi
#
if [ ! -f $APP_RESOURCES/default.properties ]; then
    cp default.properties $APP_RESOURCES/default.properties
    echo "authority=${SPHAIRAS_PROVIDER}/1" >> $APP_RESOURCES/default.properties
    echo "custom.report.notes.conventions=${SPHAIRAS_PROVIDER}.zeugnis.bemerkungen" >> $APP_RESOURCES/default.properties
    echo "Created default.properties"
fi
#
if [ ! -f $APP_RESOURCES/layer.xml ]; then
    sed -e "s/\${PROVIDER}/$SPHAIRAS_PROVIDER/" -e "s/\${PROVIDER_NAME}/${CN:-Schule}/" layer.xml > $APP_RESOURCES/layer.xml
    echo "Created layer.xml for ${SPHAIRAS_PROVIDER} (${CN})"
fi
if [ ! -f $APP_RESOURCES/layer-references ]; then 
    echo "#layer-references" >> $APP_RESOURCES/layer-references
fi
#Zeungnisbemerkungen
if [ ! -f $APP_RESOURCES/signee/custom-report-notes.properties ]; then 
    echo "#Zeugnis Bemerkungen von ${SPHAIRAS_PROVIDER}" >> $APP_RESOURCES/signee/custom-report-notes.properties
fi
if [ ! -f $APP_RESOURCES/signee/bemerkungen.xml ]; then 
    cp bemerkungen.xml $APP_RESOURCES/signee/bemerkungen.xml
fi
#Imports
if [ ! -f $APP_RESOURCES/common-import.properties ]; then
    echo "#common-import.properties" > $APP_RESOURCES/common-import.properties
    echo "students.authority=${SPHAIRAS_PROVIDER}/students/1" >> $APP_RESOURCES/common-import.properties
    echo "signee.suffix=${LOGINDOMAIN}" >> $APP_RESOURCES/common-import.properties
fi
if [ ! -f $APP_RESOURCES/defaultGrades.xml ]; then
    cp defaultGrades.xml $APP_RESOURCES/defaultGrades.xml
fi
if [ ! -f $APP_RESOURCES/processorHints.xml ]; then
    cp processorHints.xml $APP_RESOURCES/processorHints.xml
fi
