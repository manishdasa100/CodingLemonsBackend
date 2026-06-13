package com.codinglemonsbackend.Dto;

public enum UserOccupation {

    STUDENT("Student"),
    DEVELOPER("Developer"),

    // Specialized Engineering
    DATA_ENGINEER("Data Engineer"),
    ML_ENGINEER("ML Engineer"),
    DEVOPS_ENGINEER("DevOps/SRE Engineer"),
    SECURITY_ENGINEER("Security Engineer"),
    EMBEDDED_SYSTEMS_ENGINEER("Embedded Systems Engineer"),

    // Adjacent Tech
    DATA_SCIENTIST("Data Scientist"),
    QA_ENGINEER("QA Engineer"),
    DATABASE_ENGINEER("Database Engineer"),

    // Non-Tech Upskilling
    RESEARCHER("Researcher/Scientist"),
    ANALYST("Analyst"),

    // Other
    FREELANCER("Freelancer"),
    CAREER_CHANGER("Career Changer");

    private final String label;

    UserOccupation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
