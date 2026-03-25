FROM quay.io/keycloak/keycloak:23.0

# Copy custom themes
COPY themes/ecclesiaflow-base /opt/keycloak/themes/ecclesiaflow-base
COPY themes/ecclesiaflow-admin /opt/keycloak/themes/ecclesiaflow-admin
COPY themes/ecclesiaflow-user /opt/keycloak/themes/ecclesiaflow-user

# Copy realm template and init script
COPY realm-ecclesiaflow.json /opt/keycloak/data/template/realm-ecclesiaflow.json
COPY init-realm.sh /opt/keycloak/scripts/init-realm.sh

ENTRYPOINT ["sh", "/opt/keycloak/scripts/init-realm.sh"]
