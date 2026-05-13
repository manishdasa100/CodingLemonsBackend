package com.codinglemonsbackend.Dto;

import java.util.List;

public record ProblemsPage(long total, List<ProblemDto> entities) {}
