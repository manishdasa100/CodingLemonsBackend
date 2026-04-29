package com.codinglemonsbackend.Events;

import org.springframework.context.ApplicationEvent;

import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;

public class SubmitCodeCompletedEvent extends ApplicationEvent {

    private final ExecutionReportDto executionReport;
    private final SubmissionMetadata submissionMetadata;

    public SubmitCodeCompletedEvent(Object source, ExecutionReportDto executionReport, SubmissionMetadata submissionMetadata) {
        super(source);
        this.executionReport = executionReport;
        this.submissionMetadata = submissionMetadata;
    }

    public ExecutionReportDto getExecutionReport() {
        return executionReport;
    }

    public SubmissionMetadata getSubmissionMetadata() {
        return submissionMetadata;
    }
}
