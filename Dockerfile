FROM payara/server-full:5.182

MAINTAINER boris.heithecer "b.heithecker@gmail.com"

USER root

#MySQL-Java-Connector
#Connector/J 8 not working.... use 5.1.45 series
COPY --chown=payara:payara target/lib/mysql-connector-java-*.jar ${PAYARA_PATH}/glassfish/lib

VOLUME /app-resources/
VOLUME /ssl/

USER payara 

ENV DOMAIN_DIR ${PAYARA_PATH}/glassfish/domains/${PAYARA_DOMAIN}
ENV APP_RESOURCES=${DOMAIN_DIR}/app-resources
ENV SSL=/ssl
ENV CERT_ALIAS="s1as"

RUN ln -s /app-resources/ $APP_RESOURCES

COPY server-admin-authentication/target/server-admin-authentication-0.9-SNAPSHOT.war $DEPLOY_DIR/AdminAuthentication.war
COPY server-ear/target/server-ear-0.9-SNAPSHOT.ear $DEPLOY_DIR/Betula_Server.ear

COPY --chown=payara:payara target/lib/* ${DOMAIN_DIR}/lib/
RUN rm ${DOMAIN_DIR}/lib/mysql-connector-java-*.jar

COPY bin/domain.xml ${DOMAIN_DIR}/config/domain.xml
#COPY bin/login.conf ${DOMAIN_DIR}/config/login.conf
RUN echo "iservRealm { \n\
	org.thespheres.betula.security.iservlogin.IservLoginModule required; \n\
};" >> ${DOMAIN_DIR}/config/login.conf && \
    echo "certRealm { \n\
	org.thespheres.betula.security.iservlogin.AdminCertificateLoginModule required; \n\
};" >> ${DOMAIN_DIR}/config/login.conf

COPY private/password /

COPY --chown=payara:payara bin/post-boot-commands.asadmin ${PAYARA_PATH}/

USER root

RUN mkdir ${PAYARA_PATH}/sbin
RUN chown payara:payara ${PAYARA_PATH}/sbin
ENV PATH="${PATH}:${PAYARA_PATH}/sbin"

#COPY bin/sign-user-key.sh /sign-user-key.sh
#RUN chmod u+x /sign-user-key.sh

COPY --chown=payara:payara bin/genAdminKey ${PAYARA_PATH}/sbin/genAdminKey
RUN chmod u+x ${PAYARA_PATH}/sbin/genAdminKey

#COPY --chown=payara:payara bin/renewCert ${PAYARA_PATH}/sbin/renewCert
#RUN chmod u+x ${PAYARA_PATH}/sbin/renewCert

COPY --chown=payara:payara bin/templates /templates

#Nur für den Demo-Server !!!!!!
COPY --chown=payara:payara bin/keyfile ${DOMAIN_DIR}/config/

USER payara

COPY bin/preBoot /preBoot
RUN sed -i -e '$a\' ${PAYARA_PATH}/generate_deploy_commands.sh
#Insert newline
RUN echo >> ${PAYARA_PATH}/generate_deploy_commands.sh
RUN echo '/bin/bash /preBoot' >> ${PAYARA_PATH}/generate_deploy_commands.sh

#openmq passfile
#RUN echo "imq.keystore.password=changeit" > ${DOMAIN_DIR}/passfile

#RUN echo "imq.keystore.password=changeit" > $PAYARA_PATH/passfile
#RUN ${PAYARA_PATH}/mq/bin/imqusermgr encode -src /passfile -target ${DOMAIN_DIR}/passfile
#RUN rm $PAYARA_PATH/passfile

ENV PAYARAENV=${PAYARA_PATH}/bin/payaraenv.conf
RUN sed -i '/^exec/i source '${PAYARAENV} ${PAYARA_PATH}/bin/startInForeground.sh

EXPOSE 8080 4848 8181 7681 7781
