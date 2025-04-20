package com.codinglemonsbackend.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Exceptions.UserAlreadyExistException;
import com.codinglemonsbackend.Repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserRepositoryServiceTest {
    
    @Mock
    private UserRepository mockRepository;

    @InjectMocks
    private UserService underTest;

    
    @Test
    public void testSaveUser_success() throws UserAlreadyExistException{

        //given
        UserEntity user = UserEntity.builder()
                            .username("user1")
                            .firstName("Virat")
                            .lastName("Kohli")
                            .email("email@example.com")
                            .build();

        ArgumentCaptor<UserEntity> agrCaptor = ArgumentCaptor.forClass(UserEntity.class);

        when(mockRepository.getUser("user1")).thenReturn(Optional.empty());
        
        //when
        underTest.saveUser(user);

        //then
        verify(mockRepository).saveUser(agrCaptor.capture());

        assertThat(agrCaptor.getValue()).isEqualTo(user);
    }

    @Test
    public void testSaveUser_user_already_exist() throws UserAlreadyExistException {

        //given 
        UserEntity user = UserEntity.builder()
                            .username("user1")
                            .firstName("Virat")
                            .lastName("Kohli")
                            .email("email@example.com")
                            .build();

        when(mockRepository.getUser("user1")).thenReturn(Optional.of(user));
        
        //when
        //then
        assertThatThrownBy(()-> underTest.saveUser(user)).hasMessage("Username already exists").isInstanceOf(UserAlreadyExistException.class);
    }

    @Test
    public void testUpdateUser(){

        //given
        UserEntity user = UserEntity.builder()
                            .username("user1")
                            .firstName("Virat")
                            .lastName("Kohli")
                            .email("email@example.com")
                            .build();
        
        ArgumentCaptor<UserEntity> agrCaptor = ArgumentCaptor.forClass(UserEntity.class);

        //when
        underTest.updateUser(user);

        //then
        verify(mockRepository).saveUser(agrCaptor.capture());

        assertThat(agrCaptor.getValue()).isEqualTo(user);
    }

    @Test
    public void testLoadUserByUsername_username_exist() {

        //given
        UserEntity user = UserEntity.builder()
                            .username("user1")
                            .firstName("Virat")
                            .lastName("Kohli")
                            .email("email@example.com")
                            .build();

        when(mockRepository.getUser("user1")).thenReturn(Optional.of(user));

        //when
        UserDetails actualUser = underTest.loadUserByUsername("user1");

        //then
        assertThat(actualUser.getUsername()).isEqualTo(user.getUsername());

    }


    @Test
    public void testLoadUserByUsername_username_does_not_exist() {

        //given
        when(mockRepository.getUser("user1")).thenReturn(Optional.empty());

        //when
        //then
        assertThatThrownBy(() -> underTest.loadUserByUsername("user1")).hasMessage("User not found").isInstanceOf(UsernameNotFoundException.class);
    }
}
