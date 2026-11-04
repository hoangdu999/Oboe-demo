# Debug Session: blank-login-page

- **Status**: [OPEN]
- **Symptoms**: Login page is blank (header/footer visible, content area empty).
- **Session ID**: blank-login-page

## 1. Hypotheses
1. **Firebase initialization error**: `firebase.js` throws an error during initialization when config is missing.
2. **Null reference in FormAuthen**: `FormAuthen.vue` attempts to access `ui` or `auth` when they are null.
3. **Runtime JS error**: A generic runtime error prevents the component from mounting.

## 2. Reproduction Steps
1. Start MySQL, Backend, and Frontend.
2. Navigate to `http://localhost:5175/login`.
3. Observe blank content.

## 3. Evidence Collection (Instrumentation)
- [ ] Instrument `firebase.js`
- [ ] Instrument `FormAuthen.vue`

## 4. Log Analysis
(Pending logs)

## 5. Fix & Verification
(Pending analysis)
