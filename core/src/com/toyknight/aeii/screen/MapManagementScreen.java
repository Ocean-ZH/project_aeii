package com.toyknight.aeii.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.toyknight.aeii.AEIIException;
import com.toyknight.aeii.Callable;
import com.toyknight.aeii.GameContext;
import com.toyknight.aeii.ResourceManager;
import com.toyknight.aeii.concurrent.AsyncTask;
import com.toyknight.aeii.entity.Map;
import com.toyknight.aeii.network.NetworkManager;
import com.toyknight.aeii.network.entity.MapSnapshot;
import com.toyknight.aeii.network.server.ServerConfiguration;
import com.toyknight.aeii.renderer.BorderRenderer;
import com.toyknight.aeii.screen.dialog.ConfirmDialog;
import com.toyknight.aeii.screen.dialog.MessageDialog;
import com.toyknight.aeii.screen.dialog.MiniMapDialog;
import com.toyknight.aeii.screen.widgets.StringList;
import com.toyknight.aeii.utils.FileProvider;
import com.toyknight.aeii.utils.Language;
import com.toyknight.aeii.utils.MapFactory;

import java.io.IOException;

/**
 * @author toyknight 6/10/2016.
 */
public class MapManagementScreen extends StageScreen {

    private final ServerConfiguration map_server_configuration =
//            new ServerConfiguration("127.0.0.1", 5438, "aeii server - NA");
            new ServerConfiguration("45.56.93.69", 5438, "aeii server - NA");

    private final MessageDialog message_dialog;
    private final MiniMapDialog map_preview_dialog;
    private final ConfirmDialog confirm_dialog;

    private final StringList<MapSnapshot> server_map_list;
    private final ScrollPane sp_server_map_list;
    private final StringList<MapFactory.MapSnapshot> local_map_list;
    private final ScrollPane sp_local_map_list;

    private final TextButton btn_download;
    private final TextButton btn_preview_server;
    private final TextButton btn_back;
    private final TextButton btn_refresh;
    private final TextButton btn_upload;
    private final TextButton btn_preview_local;
    private final TextButton btn_delete;

    public MapManagementScreen(GameContext context) {
        super(context);
        Label label_online_map = new Label(Language.getText("LB_ONLINE_MAPS"), getContext().getSkin());
        label_online_map.setAlignment(Align.center);
        label_online_map.setBounds(ts / 2, Gdx.graphics.getHeight() - ts, ts * 8, ts);
        addActor(label_online_map);
        Label label_local_map = new Label(Language.getText("LB_LOCAL_MAPS"), getContext().getSkin());
        label_local_map.setAlignment(Align.center);
        label_local_map.setBounds(ts * 9, Gdx.graphics.getHeight() - ts, ts * 8, ts);
        addActor(label_local_map);

        server_map_list = new StringList<MapSnapshot>(ts);
        sp_server_map_list = new ScrollPane(server_map_list, getContext().getSkin());
        sp_server_map_list.getStyle().background =
                new TextureRegionDrawable(new TextureRegion(ResourceManager.getListBackground()));
        sp_server_map_list.setScrollBarPositions(true, true);
        sp_server_map_list.setBounds(ts / 2, ts * 2, ts * 8, Gdx.graphics.getHeight() - ts * 3);
        addActor(sp_server_map_list);
        local_map_list = new StringList<MapFactory.MapSnapshot>(ts);
        sp_local_map_list = new ScrollPane(local_map_list, getContext().getSkin());
        sp_local_map_list.getStyle().background =
                new TextureRegionDrawable(new TextureRegion(ResourceManager.getListBackground()));
        sp_local_map_list.setScrollBarPositions(true, true);
        sp_local_map_list.setBounds(
                Gdx.graphics.getWidth() - ts * 8 - ts / 2, ts * 2, ts * 8, Gdx.graphics.getHeight() - ts * 3);
        addActor(sp_local_map_list);

        btn_download = new TextButton(Language.getText("LB_DOWNLOAD"), getContext().getSkin());
        btn_download.setBounds(ts / 2, ts / 2, ts * 2, ts);
        btn_download.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                downloadSelectedMap();
            }
        });
        addActor(btn_download);
        btn_preview_server = new TextButton(Language.getText("LB_PREVIEW"), getContext().getSkin());
        btn_preview_server.setBounds(ts * 3, ts / 2, ts * 2, ts);
        btn_preview_server.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                previewSelectedServerMap();
            }
        });
        addActor(btn_preview_server);
        btn_back = new TextButton(Language.getText("LB_BACK"), getContext().getSkin());
        btn_back.setBounds(ts * 5 + ts / 2, ts / 2, ts * 2, ts);
        btn_back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                back();
            }
        });
        addActor(btn_back);
        btn_refresh = new TextButton(Language.getText("LB_REFRESH"), getContext().getSkin());
        btn_refresh.setBounds((Gdx.graphics.getWidth() - ts * 2) / 2, ts / 2, ts * 2, ts);
        btn_refresh.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                refresh();
            }
        });
        addActor(btn_refresh);
        btn_upload = new TextButton(Language.getText("LB_UPLOAD"), getContext().getSkin());
        btn_upload.setBounds(Gdx.graphics.getWidth() - ts * 2 - ts / 2, ts / 2, ts * 2, ts);
        btn_upload.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uploadSelectedMap();
            }
        });
        addActor(btn_upload);
        btn_preview_local = new TextButton(Language.getText("LB_PREVIEW"), getContext().getSkin());
        btn_preview_local.setBounds(Gdx.graphics.getWidth() - ts * 5, ts / 2, ts * 2, ts);
        btn_preview_local.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                previewSelectedLocalMap();
            }
        });
        addActor(btn_preview_local);
        btn_delete = new TextButton(Language.getText("LB_DELETE"), getContext().getSkin());
        btn_delete.setBounds(Gdx.graphics.getWidth() - ts * 7 - ts / 2, ts / 2, ts * 2, ts);
        btn_delete.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                delete();
            }
        });
        addActor(btn_delete);

        message_dialog = new MessageDialog(this);
        addDialog("message", message_dialog);

        map_preview_dialog = new MiniMapDialog(this);
        map_preview_dialog.addClickListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeDialog("preview");
            }
        });
        addDialog("preview", map_preview_dialog);

        confirm_dialog = new ConfirmDialog(this);
        addDialog("confirm", confirm_dialog);
    }

    private void back() {
        getContext().gotoMainMenuScreen();
        if (NetworkManager.isConnected()) {
            NetworkManager.disconnect();
        }
    }

    private void downloadSelectedMap() {
        message_dialog.setMessage(Language.getText("LB_DOWNLOADING"));
        showDialog("message");
        getContext().submitAsyncTask(new AsyncTask<Map>() {
            @Override
            public Map doTask() throws Exception {
                return NetworkManager.requestDownloadMap(server_map_list.getSelected().getFilename());
            }

            @Override
            public void onFinish(Map map) {
                closeDialog("message");
                if (map == null) {
                    showPrompt(Language.getText("MSG_ERR_FDM"), null);
                } else {
                    tryWriteMap(map, server_map_list.getSelected().getFilename());
                }
            }

            @Override
            public void onFail(String message) {
                closeDialog("message");
                showPrompt(Language.getText("MSG_ERR_FDM"), null);
            }
        });
    }

    private void uploadSelectedMap() {
        message_dialog.setMessage(Language.getText("LB_UPLOADING"));
        showDialog("message");
        getContext().submitAsyncTask(new AsyncTask<Boolean>() {
            @Override
            public Boolean doTask() throws Exception {
                FileHandle map_file = local_map_list.getSelected().file;
                Map map = MapFactory.createMap(map_file);
                return NetworkManager.requestUploadMap(map, map_file.nameWithoutExtension());
            }

            @Override
            public void onFinish(Boolean success) {
                closeDialog("message");
                if (success) {
                    showPrompt(Language.getText("MSG_INFO_MU"), null);
                } else {
                    showPrompt(Language.getText("MSG_ERR_FUM"), null);
                }
            }

            @Override
            public void onFail(String message) {
                closeDialog("message");
                showPrompt(Language.getText("MSG_ERR_FUM"), null);
            }
        });
    }

    private void tryWriteMap(Map map, String filename) {
        FileHandle map_file = FileProvider.getUserFile("map/" + filename);
        if (map_file.exists()) {
            showPrompt(Language.getText("MSG_ERR_MAE"), null);
        } else {
            try {
                MapFactory.writeMap(map, map_file);
                refreshLocalMaps();
                showPrompt(Language.getText("MSG_INFO_MD"), null);
            } catch (IOException e) {
                showPrompt(Language.getText("MSG_ERR_FDM"), null);
            }
        }
    }

    private void previewSelectedServerMap() {
        message_dialog.setMessage(Language.getText("LB_DOWNLOADING"));
        showDialog("message");
        getContext().submitAsyncTask(new AsyncTask<Map>() {
            @Override
            public Map doTask() throws Exception {
                return NetworkManager.requestDownloadMap(server_map_list.getSelected().getFilename());
            }

            @Override
            public void onFinish(Map map) {
                closeDialog("message");
                if (map == null) {
                    showPrompt(Language.getText("MSG_ERR_FDM"), null);
                } else {
                    preview(map);
                }
            }

            @Override
            public void onFail(String message) {
                closeDialog("message");
                showPrompt(Language.getText("MSG_ERR_FDM"), null);
            }
        });
    }

    private void previewSelectedLocalMap() {
        FileHandle map_file = local_map_list.getSelected().file;
        try {
            Map map = MapFactory.createMap(map_file);
            preview(map);
        } catch (AEIIException e) {
            showPrompt(Language.getText("MSG_ERR_BMF"), null);
        }
    }

    private void preview(Map map) {
        map_preview_dialog.setMap(map);
        map_preview_dialog.updateBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        showDialog("preview");
    }

    private void refresh() {
        message_dialog.setMessage(Language.getText("LB_REFRESHING"));
        showDialog("message");
        getContext().submitAsyncTask(new AsyncTask<Array<MapSnapshot>>() {
            @Override
            public Array<MapSnapshot> doTask() throws Exception {
                refreshLocalMaps();
                return NetworkManager.requestMapList();
            }

            @Override
            public void onFinish(Array<MapSnapshot> map_list) {
                closeDialog("message");
                server_map_list.setItems(map_list);
            }

            @Override
            public void onFail(String message) {
                closeDialog("message");
                showPrompt(Language.getText("MSG_ERR_CNGML"), null);
            }
        });
    }

    private void refreshLocalMaps() {
        local_map_list.setItems(MapFactory.getUserMapSnapshots());
    }

    private void delete() {
        confirm_dialog.setMessage(Language.getText("MSG_INFO_DSM"));
        confirm_dialog.setYesCallback(new Callable() {
            @Override
            public void call() {
                closeDialog("confirm");
                deleteSelectedMap();
                refreshLocalMaps();
            }
        });
        confirm_dialog.setNoCallable(new Callable() {
            @Override
            public void call() {
                closeDialog("confirm");
            }
        });
        showDialog("confirm");
    }

    private void deleteSelectedMap() {
        FileHandle map_file = local_map_list.getSelected().file;
        map_file.delete();
    }

    @Override
    public void act(float delta) {
        map_preview_dialog.update(delta);
        super.act(delta);
    }

    @Override
    public void draw() {
        batch.begin();
        batch.draw(ResourceManager.getPanelBackground(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        BorderRenderer.drawBorder(batch, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.draw(
                ResourceManager.getBorderDarkColor(),
                sp_server_map_list.getX() - ts / 24, sp_server_map_list.getY() - ts / 24,
                sp_server_map_list.getWidth() + ts / 12, sp_server_map_list.getHeight() + ts / 12);
        batch.draw(
                ResourceManager.getBorderDarkColor(),
                sp_local_map_list.getX() - ts / 24, sp_local_map_list.getY() - ts / 24,
                sp_local_map_list.getWidth() + ts / 12, sp_local_map_list.getHeight() + ts / 12);
        batch.end();
        super.draw();
    }

    @Override
    public void show() {
        super.show();
        setNetworkRelatedButtonsEnabled(true);
        message_dialog.setMessage(Language.getText("LB_CONNECTING"));
        showDialog("message");
        getContext().submitAsyncTask(new AsyncTask<Boolean>() {
            @Override
            public Boolean doTask() throws Exception {
                return NetworkManager.connect(
                        map_server_configuration, getContext().getUsername(), getContext().getVerificationString());
            }

            @Override
            public void onFinish(Boolean success) {
                closeDialog("message");
                if (success) {
                    refresh();
                } else {
                    setNetworkRelatedButtonsEnabled(false);
                    showPrompt(Language.getText("MSG_ERR_CCS"), new Callable() {
                        @Override
                        public void call() {
                            refreshLocalMaps();
                        }
                    });
                }
            }

            @Override
            public void onFail(String message) {
                closeDialog("message");
                setNetworkRelatedButtonsEnabled(false);
                showPrompt(Language.getText("MSG_ERR_CCS"), new Callable() {
                    @Override
                    public void call() {
                        refreshLocalMaps();
                    }
                });
            }
        });
    }

    private void setNetworkRelatedButtonsEnabled(boolean enabled) {
        GameContext.setButtonEnabled(btn_upload, enabled);
        GameContext.setButtonEnabled(btn_download, enabled);
        GameContext.setButtonEnabled(btn_preview_server, enabled);
        GameContext.setButtonEnabled(btn_refresh, enabled);
    }

}
