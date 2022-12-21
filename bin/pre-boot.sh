#!/bin/bash

set -e

echo 'Executing pre-boot.sh script.'
#
if [ -z "$SPHAIRAS_PROVIDER" ]; then
    echo "Environment variable SPHAIRAS_PROVIDER not set."
    exit 1
fi
echo "Using sphairas provider name \"$SPHAIRAS_PROVIDER\"."

if [ -f $APP_RESOURCES/host.conf ]; then
    echo 'Found source $APP_RESOURCES/host.conf.'
    source $APP_RESOURCES/host.conf
fi

SERVER_KEY=${SECRETS}/server.key
#In futur, server.p12 should be provided in /run/secrets
SERVER_PKCS12_FILE=${SECRETS_DIR}/server.p12
SERVER_CERT=${SECRETS}/server.crt
SERVER_CSR_TEMP=${SECRETS}/server-csr.csr
SERVER_CERT_TEMP=${SECRETS}/server-new.crt
#SECONDS=$((60 * 60 * 24 * 400))
SECONDS=0

set +x

if test "$AS_ADMIN_MASTERPASSWORD"x = x -a -f "$PASSWORD_FILE"; then
    source "$PASSWORD_FILE"
fi
if test "$AS_ADMIN_MASTERPASSWORD"x = x; then
    AS_ADMIN_MASTERPASSWORD=changeit
fi

#
ALIAS=$SPHAIRAS_PROVIDER
if [ ! -f ${SERVER_KEY} ] || [ ! -f ${SERVER_CERT} ]; then
    printf "CN=${SPHAIRAS_HOSTNAME}\nC=DE\nST=Niedersachsen\nL=Irgendwo\nO=Demo-Schule" > ${SUBJECT}
else
    openssl x509 -in ${SERVER_CERT} -noout -subject | sed 's/^subject=//' | awk '{for (i = 1; i <= NF; i++) print $i}' FS="," OFS="\n" | sed 's/ //g' > ${SUBJECT}
fi
source ${SUBJECT}
if [ ! -f ${SERVER_KEY} ] || [ ! -f ${SERVER_CERT} ]; then
    echo "Create a new RSA key pair."
    openssl req -x509 -newkey rsa:4096 -keyout $SERVER_KEY -passout pass:${AS_ADMIN_MASTERPASSWORD} -out $SERVER_CERT -days 365  -subj "/CN=${CN}/C=${C}/ST=${ST}/L=${L}/O=${O}" ${SPHAIRAS_HOSTNAME_SAN:+ -addext "subjectAltName=${SPHAIRAS_HOSTNAME_SAN}"} 
else
    openssl x509 -in ${SERVER_CERT} -noout -subject | sed 's/^subject=//' | awk '{for (i = 1; i <= NF; i++) print $i}' FS="," OFS="\n" | sed 's/ //g' > ${SUBJECT}
fi

#Replace expiring server.crt
if ! openssl x509 -checkend $SECONDS -noout -in ${SERVER_CERT}
then
    echo "Replacing expired server certificate"
    n=0; while [ -f ${SECRETS}/_server$n.crt ]; do ((++n)); done; 
    BACKUP_FILE=_server$n.crt

    openssl x509 -x509toreq -in ${SERVER_CERT} -passin pass:${AS_ADMIN_MASTERPASSWORD} -signkey ${SERVER_KEY} -out ${SERVER_CSR_TEMP}
    openssl x509 -req -days 365 -in ${SERVER_CSR_TEMP} -signkey ${SERVER_KEY} -passin pass:${AS_ADMIN_MASTERPASSWORD} -out ${SERVER_CERT_TEMP}
    rm ${SERVER_CSR_TEMP}
    mv ${SERVER_CERT} ${SECRETS}/${BACKUP_FILE}
    mv ${SERVER_CERT_TEMP} ${SERVER_CERT} 
    echo "Replaced $SERVER_CERT, saved expired file to $BACKUP_FILE"
fi

openssl pkcs12 -export -in $SERVER_CERT -inkey $SERVER_KEY -passin pass:${AS_ADMIN_MASTERPASSWORD} -out $SERVER_PKCS12_FILE -passout pass:${AS_ADMIN_MASTERPASSWORD} -name $ALIAS

#Import keys
if [ -f ${SECRETS_DIR}/keystore.jks ]; then
    rm ${SECRETS_DIR}/keystore.jks
fi
keytool -importkeystore -srckeystore ${SERVER_PKCS12_FILE} -srcstoretype PKCS12 -srcalias ${ALIAS} -destalias ${CERT_ALIAS} -srcstorepass ${AS_ADMIN_MASTERPASSWORD} -deststorepass ${AS_ADMIN_MASTERPASSWORD} -destkeystore ${SECRETS_DIR}/keystore.jks -deststoretype jks

#Remove the default truststore with lots of trusted certs.
if [ -f ${SECRETS_DIR}/cacerts.jks ]; then
    rm ${SECRETS_DIR}/cacerts.jks
fi
keytool -import -file ${SERVER_CERT} -trustcacerts -noprompt -alias ${ALIAS} -storepass ${AS_ADMIN_MASTERPASSWORD} -keystore ${SECRETS_DIR}/cacerts.jks -storetype jks

echo "imq.keystore.password=${AS_ADMIN_MASTERPASSWORD}" > /tmp/imqpwdfile

#Create folders
mkdir -p ${APP_RESOURCES}/public
mkdir -p ${APP_RESOURCES}/signee
if [ ! -f $APP_RESOURCES/instance.properties ]; then 
    echo "#instance.properties" >> $APP_RESOURCES/instance.properties
fi
cp $SERVER_CERT ${APP_RESOURCES}/public/server.crt

#Configure instance
if [ ${ST} == 'Niedersachsen' ]; then
    /bin/bash ${HOME_DIR}/templates/niedersachsen/configure.sh
elif [ ${ST} == 'Bremen' ]; then
    echo "No template for ${ST}."
    exit 1
fi

expandAsadminConditions(){
    FILE=$1
    echo "Evaluating conditions in $FILE"
    i=1
    while IFS= read -r LINE; do 
        COND=`echo $LINE | sed -n "s/^###\(.*\)###.*/\1/p"`
        if [ ! -z "$COND" ]; then
            if eval $COND; then
                echo $COND" evaluates to true."
                sed -i "${i}s/^\(###.*###\)//" $FILE
            else
                sed -i "${i}s/^\(###.*###\)/#/" $FILE
            fi
        fi
        ((i++))
    done < $FILE
}

expandAsadminConditions ${CONFIG_DIR}/post-boot-commands.asadmin
expandAsadminConditions ${CONFIG_DIR}/pre-boot-commands.asadmin

#Update AS_ENV
#echo "#Payara environment" > ${PAYARAENV}
#echo "export SPHAIRAS_PROVIDER=$SPHAIRAS_PROVIDER" >> ${PAYARAENV}
#echo "export default.signee.suffix=$LOGINSUFFIX" >> ${PAYARAENV}
