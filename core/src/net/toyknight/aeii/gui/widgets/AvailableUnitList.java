package net.toyknight.aeii.gui.widgets;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.Array;
import net.toyknight.aeii.entity.GameCore;
import net.toyknight.aeii.renderer.CanvasRenderer;
import net.toyknight.aeii.system.AER;

/**
 * @author toyknight 5/29/2015.
 */
public class AvailableUnitList extends Widget {

    private final int item_height;

    private final int big_circle_width;
    private final int big_circle_height;
    private final int bc_offset;
    private final int unit_offset;


    private GameCore game;
    private Array<Integer> available_units;

    private float prefWidth;
    private float prefHeight;

    private int selected_index = 0;

    private UnitListListener listener;

    public AvailableUnitList() {
        int ts = AER.ts;
        this.item_height = ts / 2 * 3;
        this.big_circle_width = ts * 32 / 24;
        this.big_circle_height = ts * 33 / 24;
        this.bc_offset = (item_height - big_circle_height) / 2;
        this.unit_offset = (item_height - ts) / 2;
        addListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (pointer == 0 && button != 0) return false;
                onSelect(y);
                return true;
            }
        });
    }

    private void onSelect(float y) {
        float height = getHeight();
        int index = (int) ((height - y) / item_height);
        if (index != selected_index) {
            selected_index = index;
            updateSelection();
        }
    }

    private void updateSelection() {
        int index = 0;
        for (Integer unit_index : available_units) {
            if (index == selected_index) {
                if (listener != null) {
                    listener.onUnitSelected(unit_index);
                }
                return;
            }
            index++;
        }
    }

    public void setGame(GameCore game) {
        this.game = game;
    }

    private GameCore getGame() {
        return game;
    }

    public void setUnitListListener(UnitListListener listener) {
        this.listener = listener;
    }

    public void setAvailableUnits(Array<Integer> list) {
        this.available_units = list;
        this.selected_index = 0;
        this.updateSelection();
        this.prefWidth = getWidth();
        this.prefHeight = available_units.size * item_height;
        invalidateHierarchy();
    }

    @Override
    public float getPrefWidth() {
        validate();
        return prefWidth;
    }

    @Override
    public float getPrefHeight() {
        validate();
        return prefHeight;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        int ts = AER.ts;
        int index = 0;
        int current_team = getGame().getCurrentTeam();
        float x = getX(), y = getY(), width = getWidth();
        float itemY = getHeight();
        for (Integer unit_index : available_units) {
            if (index == selected_index) {
                batch.draw(AER.resources.getListSelectedBackground(), x, y + itemY - item_height, width, item_height);
            }
            batch.draw(AER.resources.getBigCircleTexture(0),
                    x + bc_offset, y + itemY - item_height + bc_offset, big_circle_width, big_circle_height);
            batch.draw(AER.resources.getUnitTexture(current_team, unit_index, 0),
                    x + unit_offset, y + itemY - item_height + unit_offset, ts, ts);
            if (AER.units.isCommander(unit_index)) {
                CanvasRenderer.drawHead_(batch, getGame().getCommander(current_team).getHead(),
                        x + unit_offset, y + itemY - item_height + unit_offset, 0, ts);
            }
            batch.flush();
            AER.font.drawTextCenter(batch, AER.lang.getUnitName(unit_index),
                    x + big_circle_width + bc_offset, y + itemY - item_height, width - big_circle_width - bc_offset, item_height);
            index++;
            itemY -= item_height;
        }
        super.draw(batch, parentAlpha);
    }

}