package org.chronopolis.earth.serializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.chronopolis.earth.models.Replication;

import java.lang.reflect.Type;

/**
 * Class to deserialize status enums without worrying about their case
 *
 * Created by shake on 6/19/15.
 */
public class ReplicationStatusDeserializer implements JsonDeserializer<Replication.Status> {
    @Override
    public Replication.Status deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return Replication.Status.fromString(jsonElement.getAsJsonPrimitive().getAsString());
    }
}
