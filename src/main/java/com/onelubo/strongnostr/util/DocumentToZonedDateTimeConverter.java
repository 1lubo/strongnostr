package com.onelubo.strongnostr.util;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
@ReadingConverter
public class DocumentToZonedDateTimeConverter implements Converter<Document, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(@Nullable Document document) {
        if (document == null) {
            return null;
        }
        Date dateTime = document.getDate(ZoneDateTimeToDocumentConverter.DATE_TIME);
        String zoneId = document.getString(ZoneDateTimeToDocumentConverter.ZONE);
        ZoneId zone = ZoneId.of(zoneId);

        return OffsetDateTime.ofInstant(dateTime.toInstant(), zone);
    }
}
