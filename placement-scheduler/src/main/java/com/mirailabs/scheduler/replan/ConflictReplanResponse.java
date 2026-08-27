package com.mirailabs.scheduler.replan;

import java.util.List;

public record ConflictReplanResponse(

        ConflictSummary summary,

        List<ReplanChange> changes

) {
}