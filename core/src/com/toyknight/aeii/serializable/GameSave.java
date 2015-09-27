package com.toyknight.aeii.serializable;

import com.toyknight.aeii.entity.GameCore;

import java.io.Serializable;

/**
 * @author toyknight 9/17/2015.
 */
public class GameSave implements Serializable {

    private static final long serialVersionUID = 9172015L;

    public final int type;

    public final GameCore game;

    public GameSave(GameCore game, int type) {
        this.type = type;
        this.game = game;
    }

}