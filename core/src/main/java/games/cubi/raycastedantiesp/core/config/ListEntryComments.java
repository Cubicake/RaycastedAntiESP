package games.cubi.raycastedantiesp.core.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Adds comments to collection entries matched by their serialized string values, without replacing existing comments. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
public @interface ListEntryComments {
    Entry[] value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Entry {
        String value();

        String comment();
    }
}
