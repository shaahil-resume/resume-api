/**
 * Notification module.
 * <p>
 * Owns outbound email/notification delivery, consumed by other
 * modules via published events rather than direct calls.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notification"
)
package com.shaahil.resumeapi.notification;