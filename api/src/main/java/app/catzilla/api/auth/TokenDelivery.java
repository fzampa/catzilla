package app.catzilla.api.auth;

/** How the client wants to receive the refresh token (ADR-004, section 7). */
enum TokenDelivery {
    /** In the JSON response; native apps keep it in secure storage. */
    BODY,
    /** In an HttpOnly cookie that JavaScript can't read; for the web app. */
    COOKIE
}
