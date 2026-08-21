/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright © 2026 Cubicake.
 * This file is part of RaycastedAntiESP.
 * RaycastedAntiESP is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License v3.0 only, which can be accessed at https://www.gnu.org/licenses/agpl-3.0.html.
 * See README.md for warranty disclaimer and further information.
 */

package games.cubi.raycastedantiesp.paper.internals;

import games.cubi.logs.Logger;
import games.cubi.raycastedantiesp.paper.packets.PacketEventsPaperBlockInfoResolver;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * For 26.1, not currently in use as a PE method which works was found. May be needed for non-PE platforms
 */
public class IDtoBukkitBlockData {
    private static final Method getBlockStateByCombinedIdMethod;
    private static final Method craftBlockDataFromStateMethod;

    public static BlockData getBlockDataByCombinedIdRaw(int combinedId) {
        try {
            Object nmsBlockState = getBlockStateByCombinedIdMethod.invoke(null, combinedId);
            if (nmsBlockState == null) {
                return null;
            }
            return (BlockData) craftBlockDataFromStateMethod.invoke(null, nmsBlockState);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    static {
        Method resolvedGetByCombinedId = null;
        Method resolvedFromData = null;
        try {
            synchronized (PacketEventsPaperBlockInfoResolver.class) {
                Class<?> blockClass = Class.forName("net.minecraft.world.level.block.Block");
                Class<?> blockStateClass = resolveFirstPresentClass(
                        "net.minecraft.world.level.block.state.BlockState",
                        "net.minecraft.world.level.block.state.IBlockData"
                );
                Class<?> craftBlockDataClass = resolveCraftBlockDataClass();

                resolvedGetByCombinedId = findBlockStateByCombinedIdMethod(blockClass, blockStateClass);
                resolvedFromData = findCraftBlockDataFromStateMethod(craftBlockDataClass, blockStateClass);

                resolvedGetByCombinedId.setAccessible(true);
                resolvedFromData.setAccessible(true);
            }
        } catch (ReflectiveOperationException e) {
            Logger.error("Failed to resolve CraftBlockData from combined block state IDs using reflection.", e, 1, PacketEventsPaperBlockInfoResolver.class);
        } finally {
            getBlockStateByCombinedIdMethod = resolvedGetByCombinedId;
            craftBlockDataFromStateMethod = resolvedFromData;
        }
    }

    private static Class<?> resolveFirstPresentClass(String... classNames) throws ClassNotFoundException {
        ClassNotFoundException lastException = null;
        for (String className : classNames) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException exception) {
                lastException = exception;
            }
        }
        throw lastException == null
                ? new ClassNotFoundException("No candidate class names were provided.")
                : lastException;
    }

    private static Class<?> resolveCraftBlockDataClass() throws ClassNotFoundException {
        String obcPackage = Bukkit.getServer().getClass().getPackage().getName();
        return resolveFirstPresentClass(
                "org.bukkit.craftbukkit.block.data.CraftBlockData",
                obcPackage + ".block.data.CraftBlockData"
        );
    }

    private static Method findBlockStateByCombinedIdMethod(Class<?> blockClass, Class<?> blockStateClass) throws NoSuchMethodException {
        try {
            return blockClass.getDeclaredMethod("stateById", int.class);
        } catch (NoSuchMethodException ignored) {
            return Arrays.stream(blockClass.getDeclaredMethods())
                    .filter(method -> Modifier.isStatic(method.getModifiers()))
                    .filter(method -> method.getReturnType().equals(blockStateClass))
                    .filter(method -> Arrays.equals(method.getParameterTypes(), new Class<?>[]{int.class}))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("Could not find Block -> BlockState/IBlockData(int)"));
        }
    }

    private static Method findCraftBlockDataFromStateMethod(Class<?> craftBlockDataClass, Class<?> blockStateClass) throws NoSuchMethodException {
        try {
            return craftBlockDataClass.getDeclaredMethod("fromData", blockStateClass);
        } catch (NoSuchMethodException ignored) {
            return Arrays.stream(craftBlockDataClass.getDeclaredMethods())
                    .filter(method -> Modifier.isStatic(method.getModifiers()))
                    .filter(method -> method.getReturnType().equals(craftBlockDataClass))
                    .filter(method -> Arrays.equals(method.getParameterTypes(), new Class<?>[]{blockStateClass}))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("Could not find CraftBlockData.fromData(BlockState/IBlockData)"));
        }
    }
}
