package com.school.meetings;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;

/**
 * Settings for online check-ins (app.meetings.*). See
 * docs/features/google-meet.md for the Google setup.
 *
 * @param automationEnabled run the background job that creates Meet links and sends invites
 * @param pollInterval      how often the job runs
 * @param createLead        create the Google event + Meet link this long before the check-in
 * @param inviteLead        email the link to the parent this long before the check-in
 * @param maxAttempts       stop retrying a failing meeting after this many tries
 * @param timezone          the center's time zone (follow-up times are stored without one); blank = server zone
 * @param centerName        shown in emails and event titles
 * @param mailFrom          sender address for parent emails
 * @param frontendUrl       where the browser returns after connecting Google (e.g. http://localhost:4200)
 * @param google            Google OAuth client settings
 */
@ConfigurationProperties(prefix = "app.meetings")
public record MeetingProperties(
        boolean automationEnabled,
        Duration pollInterval,
        Duration createLead,
        Duration inviteLead,
        int maxAttempts,
        String timezone,
        String centerName,
        String mailFrom,
        String frontendUrl,
        Google google
) {

    /**
     * An OAuth client ("Web application") from Google Cloud Console — free,
     * works with ordinary Gmail accounts.
     *
     * @param clientId     OAuth client ID
     * @param clientSecret OAuth client secret
     * @param redirectUri  must be listed in the client's "Authorized redirect URIs"
     */
    public record Google(String clientId, String clientSecret, String redirectUri) {

        public boolean isConfigured() {
            return notBlank(clientId) && notBlank(clientSecret) && notBlank(redirectUri);
        }
    }

    public ZoneId zone() {
        return notBlank(timezone) ? ZoneId.of(timezone) : ZoneId.systemDefault();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
