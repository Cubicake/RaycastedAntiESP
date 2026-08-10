/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.core.utils;

import java.lang.annotation.*; /**
 * {@code int[]} fields annotated with this are backing arrays for {@code IntArrayLists}. {@code IntArrayLists} are not objects directly, instead just an {@code int[]} array, with static operations provided by {@link PrimitiveIntArrayList}.
 * <p>
 * This is used to avoid the overhead of an actual object for each {@link PrimitiveIntArrayList}. To further reduce overhead, the field may not be initialised, {@link PrimitiveIntArrayList} is null safe.
 *
 * <p> Do not use this array directly, all calls should go through {@link PrimitiveIntArrayList}.
 */
@Documented @Retention(RetentionPolicy.SOURCE) @Target({ElementType.TYPE_USE, ElementType.FIELD, ElementType.PARAMETER})
public @interface IntArrayListMarker {}
