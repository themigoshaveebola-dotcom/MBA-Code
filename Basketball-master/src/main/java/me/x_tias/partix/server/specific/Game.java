/*
 * Decompiled with CFR 0.152.
 */
package me.x_tias.partix.server.specific;

import me.x_tias.partix.mini.basketball.BasketballGame;
import me.x_tias.partix.mini.factories.Hub;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.server.Place;

import java.util.ArrayList;
import java.util.UUID;

public abstract class Game
        extends Place {
    public UUID owner;

    public void kickAll() {
        for (Athlete athlete : new ArrayList<>(this.getAthletes())) {
            // Check if this is a Rec game
            if (this instanceof BasketballGame) {
                BasketballGame bbGame = (BasketballGame) this;
                if (bbGame.isRecGame) {
                    // Return to Rec lobby
                    Hub.recLobby.onRecGameEnd(athlete);
                    continue;
                }
                if (bbGame.isPhysicalQueueGame) {
                    // Physical queue game - handle winner-stays logic
                    // Basketball lobby will handle teleporting winners/losers
                    Hub.basketballLobby.onPhysicalQueueGameEnd(bbGame, athlete);
                    continue;
                }
            }
            // Default behavior: return to main hub
            Hub.hub.join(athlete);
        }
    }
}

