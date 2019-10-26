/*
 * Copyright (c) 2019.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.minecraftforge.lex.cfd;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public class Config {
    public static class Server {
        public static final String PREFIX = "cfd.configgui.";
        public final Tier tier1;
        public final Tier tier2;
        public final Tier tier3;
        public final Tier tier4;
        public final Tier tier5;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Server configuration ssettings")
                   .push("server");
            tier1 = new Tier(builder, "1", 1, 40, 64* 1, true);
            tier2 = new Tier(builder, "2", 1, 20, 64* 2, true);
            tier3 = new Tier(builder, "3", 1, 10, 64* 4, true);
            tier4 = new Tier(builder, "4", 1,  5, 64* 8, true);
            tier5 = new Tier(builder, "5", 1,  1, 64*16, true);
            builder.pop();
        }

        public static class Tier {
            public final IntValue count;
            public final IntValue interval;
            public final IntValue max;
            public final BooleanValue pushes;

            Tier(ForgeConfigSpec.Builder builder, String name, int count, int interval, int max, boolean pushes) {
                builder.comment("Tier: " + name)
                       .push(name);

                this.interval = builder
                        .comment("The number of ticks between every generation update.")
                        .translation(PREFIX + ".tier_" + name + ".interval")
                        .worldRestart()
                        .defineInRange("interval", interval, 1, Integer.MAX_VALUE);

                this.count = builder
                        .comment("The amount of items to generate every update.")
                        .translation(PREFIX + ".tier_" + name + ".count")
                        .worldRestart()
                        .defineInRange("count", count, 1, Integer.MAX_VALUE);

                this.max = builder
                        .comment("The maximum amount of items to hold in the internal buffer.")
                        .translation(PREFIX + ".tier_" + name + ".max")
                        .worldRestart()
                        .defineInRange("max", max, 1, Integer.MAX_VALUE);

                this.pushes = builder
                        .comment("Set to true to enable automatically pushing to inventories above this block.")
                        .translation(PREFIX + ".tier_" + name + ".can_push")
                        .define("pushes", pushes);

                builder.pop();
            }
        }
    }

    static final ForgeConfigSpec serverSpec;
    public static final Server SERVER;
    static {
        final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        serverSpec = specPair.getRight();
        SERVER = specPair.getLeft();
    }
}
