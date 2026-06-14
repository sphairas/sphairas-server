# Local Development & Frontend Interaction Guidelines

## Environment & URLs
- The primary development frontend runs locally at: `http://localhost:8080/web`
- When using browser/screenshot automation tools, always default to testing on `http://localhost:8080/web` unless a specific staging URL is provided.

## Authentication & Login Routines

### Credential Storage
Credentials are stored in `.vscode/mcp.json` (gitignored). The `run_playwright_code` tool runs in a sandboxed context where `process.env` is **not** available, so credentials cannot be read programmatically from within playwright code.

### Login Procedure
When a session is needed or has expired:
1. Check if already logged in by navigating to `http://localhost:8080/web/main.xhtml`. If redirected to the login page, the session has expired.
2. **Notify the user** that a login is required and ask them to log in manually in the browser. Do NOT attempt to type credentials through the model.
3. Wait for the user to confirm they have logged in before continuing with browser automation.

### Form Selectors (for user reference)
- Username: `#loginForm:username`
- Password: `#loginForm:password`
- Submit: `#loginForm:submit`

### Post-Login Navigation
- After login, the app lands on `http://localhost:8080/web/main.xhtml`.
- Deep-linking to sub-views may not work. Use the sidebar menu to navigate: click the hamburger button (top-left) to open it if collapsed, then click the target menu item.

### Session Expiry
The dev server has a short idle timeout. If a browser automation sequence is interrupted and the page shows the login form, stop and ask the user to log in again before proceeding.