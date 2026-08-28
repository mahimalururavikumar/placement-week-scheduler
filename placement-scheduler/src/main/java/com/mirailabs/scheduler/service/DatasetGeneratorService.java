package com.mirailabs.scheduler.service;

import com.mirailabs.scheduler.config.DatasetConfig;
import com.mirailabs.scheduler.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.mirailabs.scheduler.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DatasetGeneratorService {

    private final Random random =
            new Random(DatasetConfig.RANDOM_SEED);

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final PanelRepository panelRepository;
    private final RoomRepository roomRepository;
    private final CompanySlotRepository companySlotRepository;
    private final ShortlistRepository shortlistRepository;
    private final InterviewRepository interviewRepository;
    private final  CandidateDecisionRepository candidateDecisionRepository;
    private final ReplanAuditRepository replanAuditRepository;


    public void generateDataset() {

        clearExistingData();

        validateCompanyNames();

        List<Company> companies =
                generateCompanies();

        List<Student> students =
                generateStudents();

        generateRooms();

        generatePanels(companies);

        generateCompanySlots(companies);

        List<CandidateDecision> decisions =
                evaluateCandidateEligibility(
                        students,
                        companies
                );

        List<Shortlist> shortlists =
                generateShortlists(students,
                        companies,
                        decisions
                );

        generateInterviewCandidates(
                shortlists
        );
    }

    private static final List<String> COMPANY_NAMES = List.of(
            "TechNova",
            "FinEdge",
            "CloudSphere",
            "DataForge",
            "CodeVertex",
            "InnoSoft",
            "NexaSystems",
            "BlueOrbit",
            "Quantix",
            "CoreLogic",
            "DevMatrix",
            "ByteCraft",
            "InfoPulse",
            "CloudAxis",
            "SoftGrid",
            "NextGen Labs",
            "AlphaWorks",
            "DigitalCore",
            "PrimeStack",
            "Vertex Solutions",
            "LogicBridge",
            "DataNest",
            "WebMatrix",
            "CodeSphere",
            "TechBridge",
            "InfiCore",
            "AppVertex",
            "CyberNova",
            "CloudWorks",
            "SmartByte",
            "DevSphere",
            "NetForge",
            "StackPoint",
            "AlgoWorks",
            "FutureSoft"
    );

    public List<Company> generateCompanies() {

        List<Company> companies = new ArrayList<>();

        for(int i=0;i<DatasetConfig.COMPANY_COUNT;i++)
        {
            PriorityTier tier;

            if(i < DatasetConfig.TIER_1_COMPANIES){
                tier = PriorityTier.TIER_1;
            } else if(i < DatasetConfig.TIER_2_COMPANIES){
                tier = PriorityTier.TIER_2;
            }else {
                tier = PriorityTier.TIER_3;
            }

            BigDecimal cgpaCutOff = generateCgpaCutoff(tier);
            int interviewDuration = generateInterviewDuration(tier);

            Company company = Company.builder()
                    .companyCode(String.format("CMP%03d", i + 1))
                    .name(COMPANY_NAMES.get(i))
                    .priorityTier(tier)
                    .cgpaCutoff(cgpaCutOff)
                    .interviewDurationMinutes(interviewDuration)
                    .active(true)
                    .build();

            companies.add(company);
        }

        return companyRepository.saveAll(companies);
    }

    private int generateInterviewDuration(PriorityTier tier) {

        return switch (tier) {
            case TIER_1 -> 30;
            case TIER_2 -> 45;
            case TIER_3 -> 60;
        };
    }

    private BigDecimal generateCgpaCutoff(PriorityTier tier) {

        return switch (tier) {

            case TIER_1 ->
                    BigDecimal.valueOf(
                            6.5 + random.nextInt(16) * 0.1
                    ).setScale(2);

            case TIER_2 ->
                    BigDecimal.valueOf(
                            7.0 + random.nextInt(16) * 0.1
                    ).setScale(2);

            case TIER_3 ->
                    BigDecimal.valueOf(
                            7.5 + random.nextInt(16) * 0.1
                    ).setScale(2);
        };
    }

    private void validateCompanyNames() {

        if (COMPANY_NAMES.size() != DatasetConfig.COMPANY_COUNT) {
            throw new IllegalStateException(
                    "Company name count does not match COMPANY_COUNT"
            );
        }
    }

    private void clearExistingData() {

        replanAuditRepository.deleteAllInBatch();

        interviewRepository.deleteAllInBatch();

        shortlistRepository.deleteAllInBatch();

        candidateDecisionRepository.deleteAllInBatch();

        companySlotRepository.deleteAllInBatch();

        panelRepository.deleteAllInBatch();

        roomRepository.deleteAllInBatch();

        companyRepository.deleteAllInBatch();

        studentRepository.deleteAllInBatch();
    }

    private static final List<String> BRANCHES = List.of(
            "CSE",
            "AIML",
            "ECE",
            "EEE",
            "MECH",
            "CIVIL"
    );

    public List<Student> generateStudents() {

        List<Student> students = new ArrayList<>();


        for (int i = 0; i < DatasetConfig.STUDENT_COUNT; i++) {

            Student student = Student.builder()
                    .studentCode(String.format("STU%04d", i + 1))
                    .name(generateStudentName(i))
                    .cgpa(generateCgpa())
                    .branch(generateBranch())
                    .status(StudentStatus.ACTIVE)
                    .build();

            students.add(student);
        }

        return studentRepository.saveAll(students);
    }

    private String generateStudentName(int index) {

        String[] firstNames = {
                "Aarav",
                "Arjun",
                "Rahul",
                "Karthik",
                "Aditya",
                "Rohan",
                "Vikram",
                "Akash",
                "Nikhil",
                "Sanjay",
                "Ananya",
                "Priya",
                "Sneha",
                "Kavya",
                "Divya",
                "Meera"
        };

        String[] lastNames = {
                "Sharma",
                "Kumar",
                "Reddy",
                "Patel",
                "Singh",
                "Verma",
                "Nair",
                "Iyer",
                "Rao",
                "Das"
        };

        String firstName = firstNames[index % firstNames.length];

        String lastName =
                lastNames[(index / firstNames.length) % lastNames.length];

        return firstName + " " + lastName;
    }

    private Double generateCgpa() {

        double cgpa =
                DatasetConfig.CGPA_MIN
                        + random.nextDouble()
                        * (DatasetConfig.CGPA_MAX - DatasetConfig.CGPA_MIN);

        return Math.round(cgpa * 100.0) / 100.0;
    }

    private String generateBranch() {

        return BRANCHES.get(
                random.nextInt(BRANCHES.size())
        );
    }

    private List<Room> generateRooms() {

        List<Room> rooms = new ArrayList<>();

        for (int i = 0; i < DatasetConfig.ROOM_COUNT; i++) {

            Room room = Room.builder()
                    .roomCode(String.format("R%02d", i + 1))
                    .active(true)
                    .build();

            rooms.add(room);
        }

        return roomRepository.saveAll(rooms);
    }

    private List<Panel> generatePanels(List<Company> companies) {

        List<Panel> panels = new ArrayList<>();

        for (Company company : companies) {

            int panelCount = generatePanelCount(company.getPriorityTier());

            for (int i = 0; i < panelCount; i++) {

                Panel panel = Panel.builder()
                        .panelCode(
                                String.format(
                                        "%s-P%02d",
                                        company.getCompanyCode(),
                                        i + 1
                                )
                        )
                        .company(company)
                        .active(true)
                        .build();

                panels.add(panel);
            }
        }

        return panelRepository.saveAll(panels);
    }

    private int generatePanelCount(PriorityTier tier) {

        return switch (tier) {

            case TIER_1 ->
                    3 + random.nextInt(4);   // 3–6

            case TIER_2 ->
                    2 + random.nextInt(3);   // 2–4

            case TIER_3 ->
                    1 + random.nextInt(3);   // 1–3
        };
    }

    private List<CompanySlot> generateCompanySlots(
            List<Company> companies) {

        List<CompanySlot> slots = new ArrayList<>();

        for (Company company : companies) {

            List<LocalDate> availableDates =
                    selectAvailableDates(company);

            for (LocalDate date : availableDates) {

                CompanySlot slot = CompanySlot.builder()
                        .company(company)
                        .date(date)
                        .startTime(DatasetConfig.DEFAULT_START_TIME)
                        .endTime(DatasetConfig.DEFAULT_END_TIME)
                        .active(true)
                        .build();

                slots.add(slot);
            }
        }

        return companySlotRepository.saveAll(slots);
    }

    private List<LocalDate> selectAvailableDates(Company company) {

        List<LocalDate> dates =
                new ArrayList<>(DatasetConfig.PLACEMENT_DATES);

        return switch (company.getPriorityTier()) {

            case TIER_1 ->
                    selectRandomUniqueDates(
                            dates,
                            3 + random.nextInt(2)
                    );

            case TIER_2 ->
                    selectRandomUniqueDates(
                            dates,
                            2
                    );

            case TIER_3 ->
                    selectRandomUniqueDates(
                            dates,
                            1 + random.nextInt(2)
                    );
        };
    }

    private List<LocalDate> selectRandomUniqueDates(
            List<LocalDate> availableDates,
            int count) {

        List<LocalDate> shuffled =
                new ArrayList<>(availableDates);

        Collections.shuffle(shuffled, random);

        return new ArrayList<>(
                shuffled.subList(
                        0,
                        Math.min(count, shuffled.size())
                )
        );
    }

    private List<CandidateDecision> evaluateCandidateEligibility(
            List<Student> students,
            List<Company> companies) {

        List<CandidateDecision> decisions =
                new ArrayList<>();

        for (Student student : students) {

            for (Company company : companies) {

                CandidateDecisionStatus status;
                CandidateDecisionReason reason;

                if (student.getStatus() == StudentStatus.WITHDRAWN) {

                    status = CandidateDecisionStatus.INELIGIBLE;
                    reason = CandidateDecisionReason.STUDENT_WITHDRAWN;

                } else if (student.getStatus() == StudentStatus.PLACED) {

                    status = CandidateDecisionStatus.INELIGIBLE;
                    reason = CandidateDecisionReason.STUDENT_ALREADY_PLACED;

                } else if (
                        student.getCgpa()
                                < company.getCgpaCutoff().doubleValue()
                ) {

                    status = CandidateDecisionStatus.INELIGIBLE;
                    reason = CandidateDecisionReason.CGPA_CUTOFF;

                } else {

                    status = CandidateDecisionStatus.ELIGIBLE;
                    reason = CandidateDecisionReason.ELIGIBLE;
                }

                decisions.add(
                        CandidateDecision.builder()
                                .student(student)
                                .company(company)
                                .status(status)
                                .reason(reason)
                                .build()
                );
            }
        }

        return candidateDecisionRepository.saveAll(decisions);
    }

    private List<Shortlist> generateShortlists(
            List<Student> students,
            List<Company> companies,
            List<CandidateDecision> decisions) {

        List<Shortlist> shortlists = new ArrayList<>();

        Map<Long, List<Student>> eligibleStudentsByCompany =
                decisions.stream()
                        .filter(decision ->
                                decision.getStatus()
                                        == CandidateDecisionStatus.ELIGIBLE)
                        .collect(Collectors.groupingBy(
                                decision ->
                                        decision.getCompany().getId(),
                                Collectors.mapping(
                                        CandidateDecision::getStudent,
                                        Collectors.toList()
                                )
                        ));

        for (Company company : companies) {

            int targetCount =
                    generateShortlistSize(
                            company.getPriorityTier()
                    );

            List<Student> eligibleStudents =
                    eligibleStudentsByCompany.getOrDefault(
                            company.getId(),
                            List.of()
                    );

            List<Student> selectedStudents =
                    selectStudentsForCompany(
                            eligibleStudents,
                            targetCount
                    );

            for (Student student : selectedStudents) {

                Shortlist shortlist = Shortlist.builder()
                        .student(student)
                        .company(company)
                        .build();

                shortlists.add(shortlist);
            }
        }

        return shortlistRepository.saveAll(shortlists);
    }

    private int generateShortlistSize(PriorityTier tier) {

        return switch (tier) {

            case TIER_1 ->
                    DatasetConfig.TIER_1_MIN_SHORTLIST
                            + random.nextInt(
                            DatasetConfig.TIER_1_MAX_SHORTLIST
                                    - DatasetConfig.TIER_1_MIN_SHORTLIST
                                    + 1
                    );

            case TIER_2 ->
                    DatasetConfig.TIER_2_MIN_SHORTLIST
                            + random.nextInt(
                            DatasetConfig.TIER_2_MAX_SHORTLIST
                                    - DatasetConfig.TIER_2_MIN_SHORTLIST
                                    + 1
                    );

            case TIER_3 ->
                    DatasetConfig.TIER_3_MIN_SHORTLIST
                            + random.nextInt(
                            DatasetConfig.TIER_3_MAX_SHORTLIST
                                    - DatasetConfig.TIER_3_MIN_SHORTLIST
                                    + 1
                    );
        };
    }

    private List<Student> selectStudentsForCompany(
            List<Student> eligibleStudents,
            int targetCount) {

        if (eligibleStudents.size() <= targetCount) {
            return new ArrayList<>(eligibleStudents);
        }

        List<StudentWeight> weightedStudents =
                eligibleStudents.stream()
                        .map(student ->
                                new StudentWeight(
                                        student,
                                        calculateStudentWeight(student)
                                ))
                        .toList();

        List<StudentWeight> shuffled =
                new ArrayList<>(weightedStudents);

        Collections.shuffle(shuffled, random);

        shuffled.sort(
                Comparator.comparingDouble(
                        StudentWeight::score
                ).reversed()
        );

        return shuffled.stream()
                .limit(targetCount)
                .map(StudentWeight::student)
                .toList();
    }

    private record StudentWeight(
            Student student,
            double score
    ) {
    }

    private double calculateStudentWeight(Student student) {

        double cgpa = student.getCgpa();

        double weight = cgpa;

        if (cgpa >= 9.0) {
            weight += 2.0;
        } else if (cgpa >= 8.5) {
            weight += 1.0;
        }

        return weight + random.nextDouble();
    }

    private List<Interview> generateInterviewCandidates(
            List<Shortlist> shortlists) {

        List<Interview> interviews = new ArrayList<>();

        for (Shortlist shortlist : shortlists) {

            Interview interview = Interview.builder()
                    .student(shortlist.getStudent())
                    .company(shortlist.getCompany())
                    .status(InterviewStatus.PENDING)
                    .build();

            interviews.add(interview);
        }

        return interviewRepository.saveAll(interviews);
    }



}