package com.mirailabs.scheduler.service;

public record DatasetGenerationResult(
        boolean success,
        long companies,
        long students,
        long rooms,
        long panels,
        long companySlots,
        long candidateDecisions,
        long shortlists,
        long interviewCandidates,
        String message
) {
}