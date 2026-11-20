package com.school;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
public class FollowupApplication {

	public static void main(String[] args) {
		useCenterTimeZone();
		SpringApplication.run(FollowupApplication.class, args);
	}

	/**
	 * Follow-up times are stored as the center's local wall-clock time, so the
	 * whole backend must run on the center's clock: "now", "today", overdue
	 * detection and when Meet links / invites go out. APP_TIMEZONE is an IANA
	 * zone id (Montréal = America/Toronto); unset keeps the server's own zone.
	 */
	static void useCenterTimeZone() {
		String zone = System.getenv("APP_TIMEZONE");
		if (zone != null && !zone.isBlank()) {
			TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zone.trim())));
		}
	}

}
