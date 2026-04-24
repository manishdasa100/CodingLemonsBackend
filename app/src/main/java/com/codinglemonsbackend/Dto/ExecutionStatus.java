package com.codinglemonsbackend.Dto;

public enum ExecutionStatus {
    ACC(10, "Accepted"),
    WA(20, "Wrong Answer"),
    TLE(30, "Time Limit Exceeded"),
    MLE(40, "Memory Limit Exceeded"),
    OLE(50, "Output Limit Exceeded"),
    CE(60, "Compilation Error"),
    RE(70, "Runtime Error"),
    IE(80, "Internal Error");

    private int statusCode;
    private String statusMessage;

    private ExecutionStatus(int statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
}
