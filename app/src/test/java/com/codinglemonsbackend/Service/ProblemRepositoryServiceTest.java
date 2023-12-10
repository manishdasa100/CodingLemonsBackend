package com.codinglemonsbackend.Service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Entities.Difficulty;
import com.codinglemonsbackend.Entities.ProblemEntity;
import com.codinglemonsbackend.Repository.ProblemsRepository;

@ExtendWith(MockitoExtension.class)
public class ProblemRepositoryServiceTest {
    
    @Mock
    private ProblemsRepository mockRepository;

    @InjectMocks
    private ProblemRepositoryService underTest;


    @Test
    public void testGetAllProblems() {
        
        // given
        int page = 1, size = 2;

        //when
        underTest.getProblems(null, null, page, size);

        // then
        verify(mockRepository).findAll(page, size);

    }

    @Test
    public void testAddProblem() {
        
        // given
        ProblemDto problemDto = ProblemDto.builder()
                                .title("Title1")
                                .description("Description1")
                                .testCases(new ArrayList<>())
                                .testCaseOutputs(new ArrayList<>())
                                .difficulty(Difficulty.EASY)
                                .driverCodes(new HashMap<>())
                                .optimalSolutions(new HashMap<>())
                                .topics(new ArrayList<>())
                                .build();
        //when
        underTest.addProblem(problemDto);

        ArgumentCaptor<ProblemEntity> argumentCaptor = ArgumentCaptor.forClass(ProblemEntity.class);

        //then
        verify(mockRepository).addProblem(argumentCaptor.capture());

        ProblemEntity entity = argumentCaptor.getValue();

        assertThat(entity.getTitle()).isEqualTo("Title1");

        assertThat(entity.getDescription()).isEqualTo("Description1");

    }

    @Test
    public void testGetFilteredProblems(){

        //given
        String difficultyStr = "EASY";
        
        String topics = "Arrays,BFS";

        int page = 0, size = 0;

        // when
        ArgumentCaptor<List<Difficulty>> argumentCaptorForDifficulty = ArgumentCaptor.forClass(List.class);

        ArgumentCaptor<String[]> argumentCaptorForString = ArgumentCaptor.forClass(String[].class);

        underTest.getProblems(difficultyStr, topics, page, size);

        // then
        verify(mockRepository).getFilteredProblems(argumentCaptorForDifficulty.capture(), argumentCaptorForString.capture(), eq(page), eq(size));

        assertThat(argumentCaptorForDifficulty.getValue()).isEqualTo(List.of(Difficulty.EASY));
        
        assertThat(argumentCaptorForString.getValue()).isEqualTo(topics.split(","));
    }

    @Test
    public void testGetProblemById_success(){

        //given
        int id = 1;

        ProblemEntity actualEntity = ProblemEntity.builder()
                                .problemId(1)
                                .title("Title1")
                                .description("Description1")
                                .testCases(new ArrayList<>())
                                .testCaseOutputs(new ArrayList<>())
                                .difficulty(Difficulty.EASY)
                                .driverCodes(new HashMap<>())
                                .optimalSolutions(new HashMap<>())
                                .topics(new ArrayList<>())
                                .acceptance(0)
                                .build();

        when(mockRepository.getProblemById(id)).thenReturn(Optional.of(actualEntity));

        //when
        ProblemDto receivedEntity = underTest.getProblem(id);

        //then
        assertThat(actualEntity).isEqualTo(receivedEntity);
    }


    @Test
    public void testGetProblemById_problemNotFound(){

        //given
        int id = 1;

        when(mockRepository.getProblemById(id)).thenThrow(new NoSuchElementException());

        //when
        //then
        assertThatThrownBy(()-> underTest.getProblem(id)).isInstanceOf(NoSuchElementException.class);
    }


    @Test
    public void testRemoveAllProblems(){

        //when
        underTest.removeAllProblems();

        //then
        verify(mockRepository).removeAllProblems();
    }

}
