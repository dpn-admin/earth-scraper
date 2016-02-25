package org.chronopolis.earth.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.chronopolis.earth.models.Replication;

import java.lang.reflect.Type;

/**
 * Serializer for handling the Status enum. When talking to the server we want to see
 * the first letter capitalized, followed by all lowercase.
 * ex:
 * 'Receieved'
 *
 * Created by shake on 6/17/15.
 */
public class ReplicationStatusSerializer implements JsonSerializer<Replication.Status> {

    @Override
    public JsonElement serialize(Replication.Status status, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(status.getName());
    }

}
