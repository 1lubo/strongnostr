package com.onelubo.strongnostr.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.onelubo.strongnostr.util.DocumentToZonedDateTimeConverter;
import com.onelubo.strongnostr.util.ZoneDateTimeToDocumentConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableMongoRepositories(basePackages = "com.onelubo.strongnostr.repository")
@EnableMongoAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017}")
    private String mongoUri;

    private final ZoneDateTimeToDocumentConverter zoneDateTimeToDocumentConverter;
    private final DocumentToZonedDateTimeConverter documentToZonedDateTimeConverter;

    public MongoConfig(ZoneDateTimeToDocumentConverter zoneDateTimeToDocumentConverter,
                       DocumentToZonedDateTimeConverter documentToZonedDateTimeConverter) {
        this.zoneDateTimeToDocumentConverter = zoneDateTimeToDocumentConverter;
        this.documentToZonedDateTimeConverter = documentToZonedDateTimeConverter;
    }

    @Override
    protected String getDatabaseName() {
        return "strongnostr";
    }

    @Override
    public MongoClient mongoClient() {
            ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings settings = MongoClientSettings.builder()
                                                          .applyConnectionString(connectionString)
                                                          .build();

        return MongoClients.create(settings);
    }

    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }

    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = List.of(
                zoneDateTimeToDocumentConverter,
                documentToZonedDateTimeConverter
                                                  );
        return new MongoCustomConversions(converters);
    }
}
