package com.codinglemonsbackend.Dto;

import org.springframework.ai.chat.client.ChatClient;

public record AIProvider(String name, ChatClient chatClient) {}
