package com.codinglemonsbackend.Dto;

import java.util.List;

import com.codinglemonsbackend.Entities.ProblemEntity;

public record ProblemsPage(long total, List<ProblemEntity> entities) {}
