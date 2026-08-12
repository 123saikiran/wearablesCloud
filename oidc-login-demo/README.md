# OIDC Login Demo

A small, self-contained Spring Boot prototype that demonstrates logging a user
in via **OpenID Connect (OIDC)**: click a "Sign in with..." button, authenticate
with the identity provider, and land on a profile page that prints the claims
from your ID token (name, email, subject, issuer, etc).

It uses Spring Security's built-in OAuth2/OIDC login support
(`spring-boot-starter-oauth2-client`) — no custom token handling code.

## Quick start (no external accounts needed)

The default profile points at a throwaway local **Keycloak** instance with a
realm, client, and demo user already configured, so you can try the full flow
in a couple of minutes.

1. Start Keycloak:
   ```bash
   docker compose up -d
   ```
   This starts Keycloak on `http://localhost:8081` and imports the realm in
   `keycloak/realm-export.json` (client `oidc-demo-client`, user `demo` /
   password `demo`).

2. Run the app:
   ```bash
   mvn spring-boot:run
   ```

3. Open `http://localhost:8080`, click **Sign in with Keycloak**, and log in
   with `demo` / `demo`. You'll land on a profile page showing the claims
   from your ID token.

Sign out with the button on the profile page; Keycloak's admin console is at
`http://localhost:8081` (`admin` / `admin`) if you want to poke around.

## Using a real identity provider instead

The app ships with ready-to-activate Spring profiles for Google, Okta, and
Auth0. Pick one, register an OIDC client with that provider, and set the
matching environment variables.

Redirect URI to register with any provider:
`http://localhost:8080/login/oauth2/code/{registrationId}`
(e.g. `.../login/oauth2/code/google`)

### Google

1. Create an **OAuth 2.0 Client ID** (Web application) in the
   [Google Cloud Console](https://console.cloud.google.com/apis/credentials),
   with redirect URI `http://localhost:8080/login/oauth2/code/google`.
2. Run:
   ```bash
   export GOOGLE_CLIENT_ID=...
   export GOOGLE_CLIENT_SECRET=...
   SPRING_PROFILES_ACTIVE=google mvn spring-boot:run
   ```

### Okta

1. Create an OIDC app in your Okta org, redirect URI
   `http://localhost:8080/login/oauth2/code/okta`.
2. Run:
   ```bash
   export OKTA_CLIENT_ID=...
   export OKTA_CLIENT_SECRET=...
   export OKTA_ISSUER_URI=https://dev-xxxxxx.okta.com/oauth2/default
   SPRING_PROFILES_ACTIVE=okta mvn spring-boot:run
   ```

### Auth0

1. Create a "Regular Web Application" in Auth0, redirect URI
   `http://localhost:8080/login/oauth2/code/auth0`.
2. Run:
   ```bash
   export AUTH0_CLIENT_ID=...
   export AUTH0_CLIENT_SECRET=...
   export AUTH0_ISSUER_URI=https://YOUR_DOMAIN.auth0.com/
   SPRING_PROFILES_ACTIVE=auth0 mvn spring-boot:run
   ```

You can activate more than one provider profile at once
(`SPRING_PROFILES_ACTIVE=google,okta`) — the landing page lists a button per
configured provider.

## How it works

1. The landing page (`/`) is public and lists a login button per configured
   provider, built dynamically from `ClientRegistrationRepository`.
2. Clicking a button sends the browser to
   `/oauth2/authorization/{registrationId}`, which Spring Security maps to
   the provider's `/authorize` endpoint.
3. After you authenticate with the provider, it redirects back to
   `/login/oauth2/code/{registrationId}` with an authorization code.
4. Spring Security exchanges the code for an **ID token** (a signed JWT) and
   validates its signature and claims against the provider's discovery
   document (`/.well-known/openid-configuration`).
5. The resulting `OidcUser` principal (and its claims) is available to
   controllers and Thymeleaf views — see `HomeController` and
   `templates/profile.html`. `SecurityConfig` is the only place access rules
   are defined: everything except `/` requires an authenticated session.

## Project layout

```
src/main/java/com/example/oidcdemo/
  OidcLoginDemoApplication.java   entry point
  config/SecurityConfig.java      auth rules + oauth2Login()
  web/HomeController.java         landing page + profile page
src/main/resources/
  application.yml                 base config (default profile: keycloak)
  application-{keycloak,google,okta,auth0}.yml   per-provider registration
  templates/{index,profile}.html  Thymeleaf views
  static/css/style.css
docker-compose.yml                 local Keycloak for the quick start
keycloak/realm-export.json         pre-baked realm/client/demo user
```

## Notes on this being a prototype

- The Keycloak client secret in `realm-export.json` is a throwaway demo value
  for local Docker use only — never reuse it, or any secret in this repo, for
  a real deployment.
- There's no database and no local user store: identity is entirely
  delegated to the OIDC provider, which is the point of the demo.
- For production use you'd add HTTPS, a proper session store, CSRF review,
  and provider-specific logout (RP-initiated logout) beyond the local
  session invalidation done here.
