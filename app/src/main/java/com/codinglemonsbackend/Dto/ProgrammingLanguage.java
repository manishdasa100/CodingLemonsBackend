package com.codinglemonsbackend.Dto;

public enum ProgrammingLanguage {
    
    JAVA(62, "OpenJDK 13.0.1"),
    PYTHON(71, "3.8.1"),
    CPP(54, "GCC 9.2.0");

    private Integer languagId;

    private String languageVersion;

    private ProgrammingLanguage(Integer languageId, String languageVersion){
        this.languagId = languageId;
        this.languageVersion = languageVersion;
    }

    public Integer getLanguagId() {
        return languagId;
    }

    public String getLanguageVersion() {
        return languageVersion;
    }

}
