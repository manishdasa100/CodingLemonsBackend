package com.codinglemonsbackend.Config;

import java.time.LocalDate;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
            new LocalDateToStringConverter(),
            new StringToLocalDateConverter()
        ));
    }

    static class LocalDateToStringConverter implements Converter<LocalDate, String> {
        @Override
        public String convert(LocalDate source) {
            return source.toString(); // YYYY-MM-DD
        }
    }

    static class StringToLocalDateConverter implements Converter<String, LocalDate> {
        @Override
        public LocalDate convert(String source) {
            return LocalDate.parse(source);
        }
    }
}
