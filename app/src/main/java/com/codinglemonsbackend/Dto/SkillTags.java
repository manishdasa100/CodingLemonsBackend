package com.codinglemonsbackend.Dto;

public enum SkillTags {

    BACKEND("Backend Development"),
    FRONTEND("Frontend Development"),
    DATASTRUCTURES("Data Structures"),
    ALGORITHMS("Algorithms"),
    DATASCIENCE("Data Science"),
    MACHINELEARNING("Machine Learning"),
    DATAANALYSIS("Data Analysis"),
    DATABASES("Databases"),
    WEBDEVELOPMENT("Web Development"),
    MOBILEDEVELOPMENT("Mobile Development"),
    DESIGN("Design"),
    UIUXDESIGN("UI/UX"),
    TESTING("Testing"),
    REACTJS("React"),
    NODEJS("Node.js"),
    NEXTJS("Next.js"),
    ANGULAR("Angular"),
    PYTHON("Python"),
    JAVA("Java"),
    CSHARP("CSharp"),
    C("C"),
    CPP("CPP"),
    JAVASCRIPT("JavaScript"),
    GO("Go"),
    RUBY("Ruby"),
    SWIFT("Swift"),
    KOTLIN("Kotlin"),
    PHP("Php"),
    TYPESCRIPT("Typescript"),
    DEVOPS("DevOps"),
    ARTIFICIALINTELLIGENCE("Artificial Intelligence"),
    CLOUDCOMPUTING("Cloud Computing"),
    ARTIFICIALNEURALNETWORKS("Artificial Neural Network"),
    DOCKER("Docker"),
    KUBERNETES("Kubernetes"),
    MICROSERVICES("Microservices"),
    OTHER("Other");

    private String skillName;

    private SkillTags(String skillName){
        this.skillName = skillName;
    }

    public String getSkillName() {
        return skillName;
    }
    
}
