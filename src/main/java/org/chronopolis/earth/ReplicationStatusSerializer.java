package org.chronopolis.earth;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.chronopolis.earth.models.Replication;

import java.lang.reflect.Type;

/**
 * Created by shake on 6/17/15.
 */
public class ReplicationStatusSerializer implements JsonSerializer<Replication.Status> {
    @Override
    public JsonElement serialize(Replication.Status status, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(status.getName());
    }
}
