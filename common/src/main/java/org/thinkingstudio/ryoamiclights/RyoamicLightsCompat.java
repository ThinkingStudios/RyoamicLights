/*
 * Copyright © 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package org.thinkingstudio.ryoamiclights;

import dev.architectury.injectables.annotations.ExpectPlatform;

/**
 * Represents a utility class for compatibility.
 *
 * @author LambdAurora
 * @version 1.3.3
 * @since 1.0.0
 */
public class RyoamicLightsCompat {
    /**
     * Returns whether Canvas is installed.
     *
     * @return {@code true} if Canvas is installed, else {@code false}
     */
    @ExpectPlatform
    public static boolean isCanvasInstalled() {
        throw new AssertionError();
    }

    /**
     * Returns whether Lil Tater Reloaded is installed.
     *
     * @return {@code true} if LTR is installed, else {@code false}
     */
    @ExpectPlatform
    public static boolean isLilTaterReloadedInstalled() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isDevEnvironment() {
        // Just throw an error, the content should get replaced at runtime.
        throw new AssertionError();
    }
}
