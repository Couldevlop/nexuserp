export const environment = {
  production: true,
  apiUrl: '',  // Empty — uses relative URLs via ingress
  keycloakUrl: '${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}',
  aiEnabled: true,
  version: '${APP_VERSION}',
  buildDate: '${BUILD_DATE}'
};
