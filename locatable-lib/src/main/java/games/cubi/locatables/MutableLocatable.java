package games.cubi.locatables;

import java.util.UUID;

public non-sealed interface MutableLocatable extends Locatable {
    MutableLocatable setX(double x);
    MutableLocatable setY(double y);
    MutableLocatable setZ(double z);
    MutableLocatable setWorld(UUID world);

    /**@return The same Locatable, with the same direction but a length of 1.*/
    default MutableLocatable normalize() {
        double length = length();
        return scalarMultiply(1.0 / length);
    }

    /**@return The same Locatable, now mutated.*/
    default MutableLocatable add(Locatable locatable) {
        setX(x() + locatable.x());
        setY(y() + locatable.y());
        setZ(z() + locatable.z());
        return this;
    }

    default MutableLocatable add(double x, double y, double z) {
        setX(x() + x);
        setY(y() + y);
        setZ(z() + z);
        return this;
    }

    /**@return The same Locatable, now mutated.*/
    default MutableLocatable subtract(Locatable locatable) {
        setX(x() - locatable.x());
        setY(y() - locatable.y());
        setZ(z() - locatable.z());
        return this;
    }

    /**
     * @param locatable The locatable to copy the position and world from.
     * @return The same Locatable, now mutated to have the same position and world as the given locatable.
     */
    default MutableLocatable set(Locatable locatable) {
        setX(locatable.x());
        setY(locatable.y());
        setZ(locatable.z());
        setWorld(locatable.world());
        return this;
    }

    default MutableLocatable set(double x, double y, double z) {
        setX(x);
        setY(y);
        setZ(z);
        return this;
    }

    /**
     * @return The same Locatable, now mutated to have the same position and world as the given values.
     */
    default MutableLocatable set(double x, double y, double z, UUID world) {
        setWorld(world);
        setX(x);
        setY(y);
        setZ(z);
        return this;
    }

    /**@return The same Locatable, now mutated.*/
    default MutableLocatable scalarMultiply(double factor) {
        setX(x() * factor);
        setY(y() * factor);
        setZ(z() * factor);
        return this;
    }

    default boolean isMutable() {
        return true;
    }

    default MutableLocatable castToMutableOrNull() {
        return this;
    }
}
