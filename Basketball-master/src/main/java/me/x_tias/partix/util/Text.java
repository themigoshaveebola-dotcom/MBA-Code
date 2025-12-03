/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.TextComponent$Builder
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  net.kyori.adventure.util.RGBLike
 */
package me.x_tias.partix.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class Text {
    public static String serialize(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static Component get(Section... sections) {
        TextComponent c = Component.empty();
        for (Section section : sections) {
            c = c.append(section.text().color(section.colour()));
        }
        return c;
    }

    public static Section section(String n, TextColor color) {
        return new Section(Component.text(n), color);
    }

    public static Section section(Component n, TextColor color) {
        return new Section(n, color);
    }

    public static Component gradient(String message, TextColor startColor, TextColor endColor, boolean bold) {
        TextComponent.Builder builder = Component.text();
        double step = 1.0 / (double) (message.length() - 1);
        for (int i = 0; i < message.length(); ++i) {
            double ratio = step * (double) i;
            TextColor color = TextColor.lerp((float) ratio, startColor, endColor);
            if (bold) {
                builder.append(Component.text(message.charAt(i), color, TextDecoration.BOLD));
                continue;
            }
            builder.append(Component.text(message.charAt(i), color));
        }
        return builder.build();
    }
}

