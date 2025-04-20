package com.codinglemonsbackend.Dto;

public enum ProgrammingLanguage {
    
    JAVA(62, "OpenJDK 13.0.1"),
    PYTHON(71, "3.8.1"),
    C(50, "GCC 9.2.0"),
    CPP(54, "GCC 9.2.0"),
    CSHARP(51, "MONO 6.6.0.161"),
    JAVASCRIPT(93, "NodeJs 12.14.0"),
    GO(60, "1.13.5"),
    RUBY(72, "2.7.0"),
    RUST(73, "1.40.0"),
    PHP(68, "7.4.1"),
    TYPESCRIPT(74, "3.7.4");

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
