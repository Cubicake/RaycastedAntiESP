package games.cubi.raycastedantiesp.core.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface Packet {
    //todo note: spawn living entity and painting are really old and no longer used, delete them from code maybe
    enum Packets {
        @Until("1.9")
        SPAWN_LIVING_ENTITY("N/A", "WrapperPlayServerSpawnEntityLiving"),
        SPAWN_ENTITY("https://minecraft.wiki/w/Java_Edition_protocol/Packets#Spawn_Entity", "WrapperPlayServerSpawnEntity"), //Note does not fire for self player
        ENTITY_ANIMATION("https://minecraft.wiki/w/Java_Edition_protocol/Packets#Entity_Animation", "WrapperPlayServerEntityAnimation"),
        ENTITY_EVENT("https://minecraft.wiki/w/Java_Edition_protocol/Packets#Entity_Event", "WrapperPlayServerEntityStatus"),
        HURT_ANIMATION("https://minecraft.wiki/w/Java_Edition_protocol/Packets#Hurt_Animation", "WrapperPlayServerHurtAnimation"),
        UPDATE_ATTRIBUTES("https://minecraft.wiki/w/Java_Edition_protocol/Packets#Update_Attributes", "WrapperPlayServerUpdateAttributes"),
        LEASH_ENTITY("https://minecraft.wiki/w/Java_Edition_protocol/Packets#Link_Entities", "WrapperPlayServerAttachEntity"), //Note in 1.8 and below this was also used for setting passengers
        ;

        Packets(String wikiURL, String PEName) {
        }
    }

    Packets value();

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @interface Since {
        String value();
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @interface Until {
        String value();
    }
}
