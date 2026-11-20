package com.school.meetings;

import com.school.followup.events.FollowUpEvents;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** After a reschedule/cancel commits, mirror it to Google Calendar and tell the parent if they had the invite. */
@Component
public class MeetingSyncListener {

    private final MeetingService meetingService;

    public MeetingSyncListener(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(FollowUpEvents.Rescheduled event) {
        meetingService.onRescheduled(event.followUpId(), event.previousStart());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(FollowUpEvents.Cancelled event) {
        meetingService.onCancelled(event.followUpId());
    }
}
