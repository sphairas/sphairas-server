FROM payara/server-full:5.201

MAINTAINER boris.heithecer "b.heithecker@gmail.com"

USER root

RUN apt-get update && \
    apt-get -y install openssl

COPY bin/generate-admin-key /usr/local/bin

VOLUME ["/app-resources" "/run/secrets"]

RUN chmod +x /usr/local/bin/generate-admin-key && \
    mkdir /app-resources && \
    mkdir -p /run/secrets && \
    chown -R payara:payara /run/secrets && \
    chown -R payara:payara /app-resources

USER payara 

ENV DOMAIN_DIR=${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}\
    APP_RESOURCES=${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/app-resources\
    SECRETS_DIR=${HOME_DIR}/app-secrets\
    SECRETS=/run/secrets\
    SUBJECT=${HOME_DIR}/subject\
    CERT_ALIAS="s1as"

#MySQL-Java-Connector
#Connector/J 8 not working.... use 5.1.45 series
COPY --chown=payara:payara target/lib/mysql-connector-java-*.jar ${PAYARA_DIR}/glassfish/lib

COPY --chown=payara:payara server-admin-authentication/target/server-admin-authentication-0.9-SNAPSHOT.war $DEPLOY_DIR/AdminAuthentication.war
COPY --chown=payara:payara server-ear/target/server-ear-0.9-SNAPSHOT.ear $DEPLOY_DIR/Betula_Server.ear

COPY --chown=payara:payara target/lib/* ${DOMAIN_DIR}/lib/

RUN rm ${DOMAIN_DIR}/lib/mysql-connector-java-*.jar && \
    ln -s /app-resources/ $APP_RESOURCES && \
    mkdir -p ${SECRETS_DIR} && \
    sed -i 's#\bdefault-jms-host="default_JMS_host"#& start-args="-Dimq.service.activelist=jms,admin,wssjms,wsjms -Dimq.wssjms.wss.port=7781 -Dimq.wsjms.ws.port=7681 -Dimq.keystore.file.dirpath=${ENV=SECRETS_DIR} -Dimq.keystore.file.name=keystore.jks -passfile /tmp/imqpwdfile -Dimq.wssjms.wss.requireClientAuth=true -Djavax.net.ssl.trustStore=${ENV=SECRETS_DIR}/cacerts.jks"#' ${DOMAIN_DIR}/config/domain.xml && \
    printf "iservRealm { \n\
	org.thespheres.betula.security.iservlogin.IservLoginModule required; \n\
    };" >> ${DOMAIN_DIR}/config/login.conf

COPY --chown=payara:payara bin/pre-boot-commands.asadmin bin/post-boot-commands.asadmin ${CONFIG_DIR}/

COPY --chown=payara:payara bin/templates ${HOME_DIR}/templates

#Nur für den Demo-Server !!!!!!
COPY --chown=payara:payara bin/keyfile ${DOMAIN_DIR}/config/

COPY bin/pre-boot.sh ${SCRIPT_DIR}/init_0_pre-boot.sh

#RUN echo "imq.keystore.password=changeit" > $PAYARA_PATH/passfile
#RUN ${PAYARA_PATH}/mq/bin/imqusermgr encode -src /passfile -target ${DOMAIN_DIR}/passfile
#RUN rm $PAYARA_PATH/passfile

EXPOSE 8080 4848 8181 7781
