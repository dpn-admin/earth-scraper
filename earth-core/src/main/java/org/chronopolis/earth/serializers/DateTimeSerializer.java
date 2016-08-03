package org.chronopolis.earth.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Type;

/**
 * GSON serializer for DateTime objects
 *
 * Created by shake on 5/7/15.
 */
@Deprecated
public class DateTimeSerializer implements JsonSerializer<DateTime> {
    DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();

    @Override
    public JsonElement serialize(DateTime dateTime, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(fmt.print(dateTime));
    }
}
