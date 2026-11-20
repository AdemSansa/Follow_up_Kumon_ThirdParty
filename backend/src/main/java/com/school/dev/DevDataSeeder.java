package com.school.dev;

import com.school.actions.model.ActionPriority;
import com.school.auth.model.Role;
import com.school.auth.model.User;
import com.school.auth.repository.UserRepository;
import com.school.followup.dto.CompleteFollowUpRequest;
import com.school.followup.dto.FollowUpPlanRequest;
import com.school.followup.dto.FollowUpRequest;
import com.school.followup.dto.RescheduleRequest;
import com.school.followup.model.Concern;
import com.school.followup.model.FollowUp;
import com.school.followup.model.FollowUpPlan;
import com.school.followup.model.FrequencyType;
import com.school.followup.model.MeetingType;
import com.school.followup.model.ReminderOffset;
import com.school.followup.repository.FollowUpRepository;
import com.school.followup.service.CheckInService;
import com.school.followup.service.FollowUpPlanService;
import com.school.followup.service.FollowUpService;
import com.school.parents.model.ContactMethod;
import com.school.parents.model.Parent;
import com.school.parents.service.ParentService;
import com.school.students.dto.StudentRequest;
import com.school.students.model.Student;
import com.school.students.model.StudentStatus;
import com.school.students.model.Subject;
import com.school.students.repository.StudentRepository;
import com.school.students.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Local-development sample data: a Kumon center in Montréal (QC, Canada)
 * with realistic families — children aged 5 to 15 studying Math and/or
 * Reading, their parents, the center's staff, follow-up plans and a history
 * of progress meetings.
 *
 * <p>Contact details are fictional by construction: phone numbers use the
 * 555-01xx range reserved for fiction in North America, and emails use the
 * reserved example.com/.org/.net domains. No VIDEO_CALL check-ins are seeded,
 * so the Meet automation never creates real Google events or sends real
 * emails for mock families.
 *
 * <p>Off by default. Enabled with {@code SEED_DEV_DATA=true}
 * ({@code app.seed.enabled}), and even then it only runs when the students
 * table is empty, so it never touches real data. Everything goes through the
 * normal services, so the data follows the same rules as data entered in
 * the app.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE)
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final FollowUpRepository followUpRepository;
    private final PasswordEncoder passwordEncoder;
    private final ParentService parentService;
    private final StudentService studentService;
    private final FollowUpPlanService followUpPlanService;
    private final FollowUpService followUpService;
    private final CheckInService checkInService;
    private final String staffPassword;

    /** Fixed seed: the same sample data on every fresh database. */
    private final Random random = new Random(2026);

    public DevDataSeeder(StudentRepository studentRepository, UserRepository userRepository,
                         FollowUpRepository followUpRepository, PasswordEncoder passwordEncoder,
                         ParentService parentService, StudentService studentService,
                         FollowUpPlanService followUpPlanService, FollowUpService followUpService,
                         CheckInService checkInService,
                         @Value("${app.bootstrap-staff.password:changeme}") String staffPassword) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.followUpRepository = followUpRepository;
        this.passwordEncoder = passwordEncoder;
        this.parentService = parentService;
        this.studentService = studentService;
        this.followUpPlanService = followUpPlanService;
        this.followUpService = followUpService;
        this.checkInService = checkInService;
        this.staffPassword = staffPassword;
    }

    // --- Data ----------------------------------------------------------------------------------

    private record StaffSeed(String first, String last, String email) {
    }

    /** Center instructor + assistants (log in with STAFF_PASSWORD). */
    private static final List<StaffSeed> STAFF = List.of(
            new StaffSeed("Marie-Ève", "Lefebvre", "marie-eve.lefebvre@example.com"),
            new StaffSeed("David", "Cohen", "david.cohen@example.com"),
            new StaffSeed("Amira", "Saleh", "amira.saleh@example.com"),
            new StaffSeed("Julien", "Bergeron", "julien.bergeron@example.com"));

    private record ParentSeed(String first, String last, String relationship, ContactMethod contact,
                              String phone, String email) {
    }

    /**
     * @param age     age in years today (5–15)
     * @param math    Kumon Math level, or null if not enrolled in Math
     * @param reading Kumon Reading level, or null if not enrolled in Reading
     */
    private record ChildSeed(String first, String gender, int age, String math, String reading,
                             StudentStatus status, int monthsEnrolled, String notes) {
    }

    private record FamilySeed(String lastName, String address, List<ParentSeed> parents, List<ChildSeed> children) {
    }

    private static final StudentStatus ACTIVE = StudentStatus.ACTIVE;

    private static final List<FamilySeed> FAMILIES = List.of(
            new FamilySeed("Tremblay", "4815 rue Saint-Denis, Montréal, QC H2J 2L6",
                    List.of(new ParentSeed("Isabelle", "Tremblay", "Mother", ContactMethod.EMAIL,
                                    "(514) 555-0101", "isabelle.tremblay@example.com"),
                            new ParentSeed("Mathieu", "Tremblay", "Father", ContactMethod.PHONE,
                                    "(514) 555-0102", "mathieu.tremblay@example.net")),
                    List.of(new ChildSeed("Léa", "Female", 9, "E", "CII", ACTIVE, 26,
                                    "Bilingual French/English. Comes Tuesdays and Thursdays after school."),
                            new ChildSeed("Félix", "Male", 6, "3A", null, ACTIVE, 7,
                                    "Started in kindergarten; works best in short sessions."))),
            new FamilySeed("Nguyen", "6230 avenue de Darlington, Montréal, QC H3S 2J4",
                    List.of(new ParentSeed("Thu", "Nguyen", "Mother", ContactMethod.SMS,
                            "(438) 555-0103", "thu.nguyen@example.com")),
                    List.of(new ChildSeed("Minh", "Male", 12, "H", null, ACTIVE, 30,
                                    "Working two years ahead of his school grade in math."),
                            new ChildSeed("Linh", "Female", 10, "F", "DI", ACTIVE, 22, null))),
            new FamilySeed("Joseph", "8455 boulevard Saint-Michel, Montréal, QC H1Z 3E7",
                    List.of(new ParentSeed("Nadège", "Joseph", "Mother", ContactMethod.PHONE,
                            "(514) 555-0104", "nadege.joseph@example.org")),
                    List.of(new ChildSeed("Kevens", "Male", 8, "C", "BII", ACTIVE, 11,
                                    "Attends a French school; reading in English is new for him."))),
            new FamilySeed("Haddad", "1270 rue Fleury Ouest, Montréal, QC H3L 1T8",
                    List.of(new ParentSeed("Rania", "Haddad", "Mother", ContactMethod.WHATSAPP,
                                    "(514) 555-0105", "rania.haddad@example.com"),
                            new ParentSeed("Karim", "Haddad", "Father", ContactMethod.EMAIL,
                                    "(514) 555-0106", "karim.haddad@example.com")),
                    List.of(new ChildSeed("Yasmine", "Female", 14, "J", null, ACTIVE, 34,
                                    "Preparing for secondary-school math enrichment program."),
                            new ChildSeed("Adam", "Male", 11, "G", "EI", ACTIVE, 19, null),
                            new ChildSeed("Sami", "Male", 7, "A", "2A", ACTIVE, 5, null))),
            new FamilySeed("Chen", "5560 avenue Victoria, Montréal, QC H3W 2R4",
                    List.of(new ParentSeed("Wei", "Chen", "Father", ContactMethod.EMAIL,
                            "(438) 555-0107", "wei.chen@example.net")),
                    List.of(new ChildSeed("Olivia", "Female", 13, "I", "FII", ACTIVE, 40,
                            "Excellent self-learner; mentors younger students during class time."))),
            new FamilySeed("Gagnon", "3340 rue Beaubien Est, Montréal, QC H1X 1G8",
                    List.of(new ParentSeed("Valérie", "Gagnon", "Mother", ContactMethod.SMS,
                            "(514) 555-0108", "valerie.gagnon@example.com")),
                    List.of(new ChildSeed("Thomas", "Male", 10, "D", null, ACTIVE, 9,
                                    "Needs encouragement with timed worksheets."),
                            new ChildSeed("Zoé", "Female", 8, null, "BI", StudentStatus.PAUSED, 12,
                                    "Paused for the summer — family away until September."))),
            new FamilySeed("Patel", "780 avenue Querbes, Montréal, QC H2V 3W9",
                    List.of(new ParentSeed("Priya", "Patel", "Mother", ContactMethod.EMAIL,
                            "(438) 555-0109", "priya.patel@example.org")),
                    List.of(new ChildSeed("Arjun", "Male", 9, "E", "CI", ACTIVE, 15, null))),
            new FamilySeed("Rossi", "6945 rue Saint-Dominique, Montréal, QC H2S 3B3",
                    List.of(new ParentSeed("Giulia", "Rossi", "Mother", ContactMethod.PHONE,
                            "(514) 555-0110", "giulia.rossi@example.com")),
                    List.of(new ChildSeed("Luca", "Male", 15, "J", null, StudentStatus.COMPLETED, 48,
                            "Completed the core Math program — graduated in June."))),
            new FamilySeed("Côté", "2215 rue Sherbrooke Est, Montréal, QC H2K 1E1",
                    List.of(new ParentSeed("Sophie", "Côté", "Mother", ContactMethod.EMAIL,
                                    "(514) 555-0111", "sophie.cote@example.net"),
                            new ParentSeed("Patrick", "Côté", "Father", ContactMethod.SMS,
                                    "(514) 555-0112", "patrick.cote@example.net")),
                    List.of(new ChildSeed("Charlotte", "Female", 11, "F", "DII", ACTIVE, 24, null),
                            new ChildSeed("Émile", "Male", 5, "4A", "5A", ACTIVE, 3,
                                    "Youngest at the center; learning number writing and phonics."))),
            new FamilySeed("Benali", "9150 rue Lajeunesse, Montréal, QC H2M 1S4",
                    List.of(new ParentSeed("Samira", "Benali", "Mother", ContactMethod.WHATSAPP,
                            "(438) 555-0113", "samira.benali@example.com")),
                    List.of(new ChildSeed("Ilyes", "Male", 12, "G", "EII", ACTIVE, 20, null),
                            new ChildSeed("Nour", "Female", 9, "D", "CI", ACTIVE, 14, null))),
            new FamilySeed("Kim", "4420 avenue Wilson, Montréal, QC H4A 2V2",
                    List.of(new ParentSeed("Ji-woo", "Kim", "Mother", ContactMethod.EMAIL,
                            "(514) 555-0114", "jiwoo.kim@example.org")),
                    List.of(new ChildSeed("Daniel", "Male", 7, "B", null, ACTIVE, 10, null))),
            new FamilySeed("Martínez", "3875 rue Ontario Est, Montréal, QC H1W 1S2",
                    List.of(new ParentSeed("Carolina", "Martínez", "Mother", ContactMethod.PHONE,
                                    "(514) 555-0115", "carolina.martinez@example.com"),
                            new ParentSeed("Diego", "Martínez", "Father", ContactMethod.WHATSAPP,
                                    "(514) 555-0116", "diego.martinez@example.com")),
                    List.of(new ChildSeed("Sofía", "Female", 10, null, "DI", ACTIVE, 8,
                            "Spanish at home, French at school — focusing on English reading."))),
            new FamilySeed("O'Brien", "5122 avenue Coronation, Montréal, QC H4V 2E1",
                    List.of(new ParentSeed("Siobhan", "O'Brien", "Mother", ContactMethod.EMAIL,
                            "(438) 555-0117", "siobhan.obrien@example.net")),
                    List.of(new ChildSeed("Liam", "Male", 13, "H", "GI", ACTIVE, 28, null),
                            new ChildSeed("Ava", "Female", 6, "2A", "4A", ACTIVE, 6, null))),
            new FamilySeed("Lavoie", "1680 rue Jean-Talon Est, Montréal, QC H2E 1T3",
                    List.of(new ParentSeed("Annie", "Lavoie", "Guardian", ContactMethod.PHONE,
                            "(514) 555-0118", "annie.lavoie@example.com")),
                    List.of(new ChildSeed("Noah", "Male", 8, "B", "AII", ACTIVE, 4,
                            "Lives with his aunt (guardian), who handles all communication."))),
            new FamilySeed("Singh", "7410 rue Hutchison, Montréal, QC H3N 1Z1",
                    List.of(new ParentSeed("Harpreet", "Singh", "Father", ContactMethod.SMS,
                            "(438) 555-0119", "harpreet.singh@example.org")),
                    List.of(new ChildSeed("Simran", "Female", 11, "F", "DII", ACTIVE, 17, null),
                            new ChildSeed("Arman", "Male", 14, "I", null, ACTIVE, 32, null))));

    // --- Run --------------------------------------------------------------------------------------

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (studentRepository.count() > 0) {
            log.info("Dev seed skipped: students table is not empty.");
            return;
        }

        List<User> staff = createStaff();
        int parents = 0;
        int plans = 0;
        List<Student> students = new ArrayList<>();
        int index = 0;
        for (FamilySeed family : FAMILIES) {
            List<Parent> familyParents = new ArrayList<>();
            for (ParentSeed p : family.parents()) {
                familyParents.add(parentService.create(new Parent(p.first(), p.last(), p.phone(), p.email(),
                        p.relationship(), p.contact(), null)));
                parents++;
            }
            for (ChildSeed child : family.children()) {
                User teacher = staff.get(index % staff.size());
                Student student = createStudent(family, child, familyParents, teacher);
                students.add(student);
                if (createPlanAndHistory(student, child, familyParents.get(0), teacher, index)) {
                    plans++;
                }
                index++;
            }
        }
        createTodayAndTomorrow(students, staff);

        log.warn("Dev seed data created (Kumon Montréal): {} staff, {} parents, {} students, {} follow-up plans. "
                + "Staff log in with STAFF_PASSWORD — see docs/TEST_ACCOUNTS.md.", staff.size(), parents,
                students.size(), plans);
    }

    private List<User> createStaff() {
        List<User> users = new ArrayList<>();
        for (StaffSeed s : STAFF) {
            users.add(userRepository.findByEmailIgnoreCase(s.email()).orElseGet(() -> userRepository.save(
                    new User(s.email(), passwordEncoder.encode(staffPassword), s.first(), s.last(), Role.STAFF))));
        }
        return users;
    }

    private Student createStudent(FamilySeed family, ChildSeed child, List<Parent> parents, User teacher) {
        LocalDate today = LocalDate.now();
        LocalDate dateOfBirth = today.minusYears(child.age()).minusDays(30 + random.nextInt(300));
        LocalDate enrolled = today.minusMonths(child.monthsEnrolled()).minusDays(random.nextInt(20));

        Set<Subject> subjects = EnumSet.noneOf(Subject.class);
        List<String> levels = new ArrayList<>();
        if (child.math() != null) {
            subjects.add(Subject.MATH);
            levels.add("Math " + child.math());
        }
        if (child.reading() != null) {
            subjects.add(Subject.ENGLISH);
            levels.add("Reading " + child.reading());
        }

        Set<UUID> parentIds = new LinkedHashSet<>();
        parents.forEach(p -> parentIds.add(p.getId()));

        return studentService.create(new StudentRequest(
                child.first(), family.lastName(), dateOfBirth, child.gender(),
                null, null, family.address(), enrolled, child.status(),
                String.join(" · ", levels), subjects, teacher.getId(), parentIds, child.notes()));
    }

    // --- Plans + history ---------------------------------------------------------------------------

    /** Overdue/cancelled/rescheduled examples are spread over a few students so the dashboard looks lived-in. */
    private int overdueLeft = 2;
    private int cancelledLeft = 2;
    private int rescheduledLeft = 1;

    private boolean createPlanAndHistory(Student student, ChildSeed child, Parent parent, User teacher, int index) {
        if (child.status() == StudentStatus.COMPLETED) {
            return false; // graduated: history only via past meetings would need a closed plan — keep it simple
        }
        // Kumon habit: first progress meeting ~1 month after enrollment, then monthly for new students,
        // every 3 months once settled. Phone check-ins for some busy families.
        boolean newStudent = child.monthsEnrolled() <= 6;
        FrequencyType frequency = newStudent ? FrequencyType.MONTHLY
                : index % 4 == 0 ? FrequencyType.EVERY_2_MONTHS : FrequencyType.EVERY_3_MONTHS;
        MeetingType type = index % 5 == 3 ? MeetingType.PHONE_CALL : MeetingType.PARENT_MEETING;
        DayOfWeek day = index % 3 == 0 ? DayOfWeek.SATURDAY : index % 3 == 1 ? DayOfWeek.WEDNESDAY : DayOfWeek.THURSDAY;
        LocalTime time = day == DayOfWeek.SATURDAY ? LocalTime.of(10 + index % 3, 0) : LocalTime.of(16, 30 + (index % 2) * 15);
        LocalDate start = student.getEnrollmentDate().plusMonths(1);

        FollowUpPlan plan = followUpPlanService.create(new FollowUpPlanRequest(
                student.getId(), parent.getId(), teacher.getId(), frequency, null, null, start, day, time, 30,
                type == MeetingType.PARENT_MEETING ? "Centre Kumon — salle " + (1 + index % 2) : null,
                type, EnumSet.of(ReminderOffset.TWENTY_FOUR_HOURS_BEFORE), true,
                newStudent ? 6 : null));

        LocalDateTime now = LocalDateTime.now();
        List<FollowUp> occurrences = followUpRepository.findByFollowUpPlanIdOrderByScheduledStartAsc(plan.getId());
        List<FollowUp> past = occurrences.stream().filter(f -> f.getScheduledEnd().isBefore(now)).toList();
        List<FollowUp> future = occurrences.stream().filter(f -> !f.getScheduledEnd().isBefore(now)).toList();

        int trend = 2 + index % 3; // 2 = needs support, 3 = steady, 4 = strong
        for (int p = 0; p < past.size(); p++) {
            FollowUp followUp = past.get(p);
            boolean latest = p == past.size() - 1;
            if (latest && overdueLeft > 0 && index % 6 == 2) {
                overdueLeft--; // not done yet → shows as overdue
            } else if (!latest && p > 0 && cancelledLeft > 0 && index % 5 == 1) {
                cancelledLeft--;
                followUpService.cancel(followUp.getId()); // e.g. family away; next meeting kept as planned
            } else {
                checkIn(followUp, student, child, teacher, trend, p);
            }
        }

        if (!future.isEmpty() && rescheduledLeft > 0 && index % 7 == 4) {
            rescheduledLeft--;
            FollowUp next = future.get(0);
            followUpService.reschedule(next.getId(), new RescheduleRequest(
                    next.getScheduledStart().plusDays(2), next.getScheduledEnd().plusDays(2)));
        }
        return true;
    }

    private static final String[] TOPICS_MATH = {"addition with carrying", "multiplication tables", "long division",
            "fractions", "decimals", "order of operations", "linear equations"};
    private static final String[] TOPICS_READING = {"reading comprehension", "vocabulary", "sentence structure",
            "summarizing passages", "reading aloud with expression"};

    private void checkIn(FollowUp followUp, Student student, ChildSeed child, User teacher, int trend, int round) {
        String name = child.first();
        boolean math = child.math() != null;
        String topic = math ? TOPICS_MATH[(round + child.age()) % TOPICS_MATH.length]
                : TOPICS_READING[(round + child.age()) % TOPICS_READING.length];
        int progress = clamp(trend + (round % 3 == 2 ? 1 : 0) - (round % 4 == 1 ? 1 : 0));
        int engagement = clamp(trend + (random.nextInt(3) - 1));
        int satisfaction = clamp(Math.max(progress, 3) + (random.nextInt(2)));

        Set<Concern> concerns = EnumSet.noneOf(Concern.class);
        String summary;
        String recommendation;
        if (progress >= 4) {
            summary = name + " is completing worksheets within the standard completion time with very few "
                    + "corrections. Confident with " + topic + ".";
            recommendation = "Advance to the next level after the achievement test next week.";
        } else if (progress == 3) {
            summary = "Steady progress. " + name + " is accurate but a little slow on " + topic
                    + "; daily practice at home is consistent.";
            recommendation = "Stay on the current level two more weeks to build speed, then reassess.";
        } else {
            concerns.add(Concern.PROGRESS);
            summary = name + " is finding " + topic + " challenging and made several repeated errors this month.";
            recommendation = "Repeat the review set on " + topic + " before moving on; shorter daily sessions.";
        }
        if (engagement <= 2) {
            concerns.add(Concern.MOTIVATION);
            summary += " Motivation dipped during the school exam period.";
        }
        if (round % 5 == 3) {
            concerns.add(Concern.HOMEWORK);
            summary += " A few homework days were missed; agreed on a fixed 15-minute routine after school.";
        }

        List<CompleteFollowUpRequest.ActionItemInput> actions = new ArrayList<>();
        if (concerns.contains(Concern.PROGRESS)) {
            actions.add(new CompleteFollowUpRequest.ActionItemInput("Prepare review worksheets on " + topic,
                    null, followUp.getScheduledStart().toLocalDate().plusDays(5), ActionPriority.HIGH, teacher.getId()));
        }
        if (concerns.contains(Concern.HOMEWORK)) {
            actions.add(new CompleteFollowUpRequest.ActionItemInput("Call parent in two weeks to check the homework routine",
                    null, followUp.getScheduledStart().toLocalDate().plusDays(14), ActionPriority.MEDIUM, teacher.getId()));
        }

        checkInService.complete(followUp.getId(), new CompleteFollowUpRequest(
                progress, engagement, satisfaction, concerns, summary, recommendation, actions), teacher.getEmail());
    }

    private static int clamp(int rating) {
        return Math.max(1, Math.min(5, rating));
    }

    /** A couple of in-person meetings today and tomorrow so the dashboard isn't empty. */
    private void createTodayAndTomorrow(List<Student> students, List<User> staff) {
        LocalDate today = LocalDate.now();
        Object[][] meetings = {
                {0, today, LocalTime.of(16, 0), MeetingType.IN_PERSON},
                {3, today, LocalTime.of(17, 30), MeetingType.PHONE_CALL},
                {6, today.plusDays(1), LocalTime.of(10, 0), MeetingType.IN_PERSON},
        };
        for (Object[] m : meetings) {
            Student student = students.get((Integer) m[0]);
            LocalDateTime start = LocalDateTime.of((LocalDate) m[1], (LocalTime) m[2]);
            followUpService.create(new FollowUpRequest(student.getId(), null,
                    student.getParents().iterator().next().getId(),
                    staff.get((Integer) m[0] % staff.size()).getId(),
                    (MeetingType) m[3], start, start.plusMinutes(30)));
        }
    }
}
