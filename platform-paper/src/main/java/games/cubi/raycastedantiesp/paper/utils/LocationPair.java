/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.utils;

public final class LocationPair {/*
    private final QuantisedLocation loc1;
    private final QuantisedLocation loc2; // canonical ordering: loc1 ≤ loc2 by hash

    private LocationPair(QuantisedLocation p, QuantisedLocation e) {
        this.loc1 = p;
        this.loc2 = e;
    }

    public static LocationPair of(QuantisedLocation a, QuantisedLocation b) {
        if (!a.world().equals(b.world()))
            throw new IllegalArgumentException("Locations are in different worlds");
        // choose a deterministic order so (A,B)==(B,A) TODO: More perf optimal way than hashing? Or maybe just make sure this is ran async? Or is hashing fine perf wise?
        if (a.hashCode() <= b.hashCode()) {
            return new LocationPair(a, b);
        }
        else return new LocationPair(b, a);
    }

    public QuantisedLocation first() {
        return loc1;
    }

    public QuantisedLocation second() {
        return loc2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocationPair lp)) return false;
        return loc1.equals(lp.loc1) && loc2.equals(lp.loc2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loc1, loc2);
    }*/
}

