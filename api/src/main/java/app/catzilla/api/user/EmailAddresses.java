package app.catzilla.api.user;

import java.util.Locale;

/** Emails are compared and stored in one form, so "Ana@Example.com " and "ana@example.com" are the same account. */
public final class EmailAddresses {

    private EmailAddresses() {}

    public static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
