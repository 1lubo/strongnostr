package com.onelubo.strongnostr.util;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Date;

@Component
@WritingConverter
public class ZoneDateTimeToDocumentConverter implements Converter<OffsetDateTime, Document> {
    static final String DATE_TIME = "dateTime";
    static final String ZONE = "zone";
    static final String OFFSET = "offset";


    public Document convert(@Nullable OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        Document document = new Document();
        document.put(DATE_TIME, Date.from(offsetDateTime.toInstant()));
        document.put(ZONE, offsetDateTime.toZonedDateTime().getZone().getId());
        document.put(OFFSET, offsetDateTime.getOffset().toString());
        return document;
    }
}
