package net.toyknight.aeii.server;

import com.esotericsoftware.minlog.Log;
import net.toyknight.aeii.GameException;
import net.toyknight.aeii.entity.GameCore;
import net.toyknight.aeii.entity.Map;
import net.toyknight.aeii.network.NetworkConstants;
import net.toyknight.aeii.network.entity.LeaderboardRecord;
import net.toyknight.aeii.network.entity.RoomSetting;
import net.toyknight.aeii.network.entity.RoomSnapshot;
import net.toyknight.aeii.server.entities.Player;
import net.toyknight.aeii.server.entities.Room;
import net.toyknight.aeii.server.managers.MapManager;
import net.toyknight.aeii.server.utils.PacketBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author toyknight 8/16/2016.
 */
public class RequestHandler {

    private static final String TAG = "REQUEST HANDLER";

    private final ExecutorService executor;

    private final ServerContext context;

    public RequestHandler(ServerContext context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public ServerContext getContext() {
        return context;
    }

    public void doHandleRequest(Player player, JSONObject request) {
        try {
            switch (request.getInt("operation")) {
                case NetworkConstants.AUTHENTICATION:
                    onAuthenticationRequested(player, request);
                    break;
                case NetworkConstants.LIST_ROOMS:
                    onRoomListRequested(player);
                    break;
                case NetworkConstants.CREATE_ROOM:
                    onRoomCreationRequested(player, request);
                    break;
                case NetworkConstants.JOIN_ROOM:
                    onRoomJoinRequested(player, request);
                    break;
                case NetworkConstants.PLAYER_LEAVING:
                    getContext().getRoomManager().onPlayerLeaveRoom(player);
                    break;
                case NetworkConstants.ALLOCATION_UPDATING:
                    onAllocationUpdateRequested(player, request);
                    break;
                case NetworkConstants.START_GAME:
                    onGameStartRequested(player);
                    break;
                case NetworkConstants.GAME_EVENT:
                    onGameEventSubmitted(player, request);
                    break;
                case NetworkConstants.MESSAGE:
                    onMessageSubmitted(player, request);
                    break;
                case NetworkConstants.LIST_MAPS:
                    onMapListRequested(player, request);
                    break;
                case NetworkConstants.UPLOAD_MAP:
                    onMapUploadRequest(player, request);
                    break;
                case NetworkConstants.DOWNLOAD_MAP:
                    onMapDownloadRequested(player, request);
                    break;
                case NetworkConstants.LIST_IDLE_PLAYERS:
                    onIdlePlayerListRequested(player);
                    break;
                case NetworkConstants.GLOBAL_MESSAGE:
                    onGlobalMessageSubmitted(request);
                    break;
                case NetworkConstants.DELETE_MAP:
                    onMapDeleteRequested(player, request);
                    break;
                case NetworkConstants.UPDATE_MAP:
                    onMapUpdateRequested(player, request);
                    break;
                case NetworkConstants.RECONNECT:
                    onReconnectRequested(player, request);
                    break;
                case NetworkConstants.DISCONNECT_PLAYER:
                    onDisconnectPlayerRequested(player, request);
                    break;
                case NetworkConstants.SUBMIT_RECORD:
                    onRecordSubmitted(player, request);
                    break;
                case NetworkConstants.GET_BEST_RECORD:
                    onBestRecordRequested(player, request);
                    break;
                default:
                    Log.error(TAG, String.format("Illegal request from %s [undefined operation]", player.toString()));
            }
        } catch (JSONException ex) {
            Log.error(TAG, String.format("Illegal request from %s [request format error]", player.toString()), ex);
        } catch (Exception ex) {
            Log.error(TAG, String.format("Exception occurred while handling request from %s", player.toString()), ex);
        }
    }

    public void onAuthenticationRequested(Player player, JSONObject request) throws JSONException {
        String username = request.getString("username");
        String v_string = request.getString("v_string");

        player.setUsername(username);

        JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
        if (getContext().getVerificationString().equals(v_string)) {
            player.setAuthenticated(true);
            response.put("approved", true);
            response.put("service_id", player.getID());
            Log.info(TAG, String.format("%s authenticated.", player.toString()));
        } else {
            response.put("approved", false);
            Log.info(TAG, String.format("%s authentication failed.", player.toString()));
        }
        player.sendTCP(response.toString());
    }

    public void onRoomListRequested(Player player) {
        if (player.isAuthenticated()) {
            JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
            JSONArray rooms = new JSONArray();
            for (RoomSnapshot snapshot : getContext().getRoomManager().getRoomSnapshots()) {
                rooms.put(snapshot.toJson());
            }
            response.put("rooms", rooms);
            player.sendTCP(response.toString());
        }
    }

    public void onRoomCreationRequested(Player player, JSONObject request) {
        if (player.isAuthenticated() && player.getRoomID() < 0) {
            JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);

            String username = player.getUsername();
            String password = request.has("password") ? request.getString("password") : null;
            int player_capacity = request.getInt("player_capacity");

            RoomSetting room_setting;
            if (request.getBoolean("new_game")) {
                //create a new game room
                Map map = new Map(request.getJSONObject("map"));
                String map_name = request.getString("map_name");
                int unit_capacity = request.getInt("unit_capacity");
                int start_gold = request.getInt("start_gold");
                room_setting = getContext().getRoomManager().createRoom(
                        map, username, map_name, password, player_capacity, unit_capacity, start_gold, player);
            } else {
                //create a saved game room
                GameCore game = new GameCore(request.getJSONObject("game"));
                String save_name = request.getString("save_name");
                room_setting = getContext().getRoomManager().createRoom(
                        game, username, save_name, password, player_capacity, player);
            }
            if (room_setting == null) {
                response.put("approved", false);
            } else {
                Log.info(TAG, String.format("%s creates game room [%d]", player.toString(), room_setting.room_id));
                response.put("room_setting", room_setting.toJson());
                response.put("approved", true);
            }
            player.sendTCP(response.toString());
        }
    }

    public void onRoomJoinRequested(Player player, JSONObject request) {
        if (player.isAuthenticated()) {
            JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);

            long room_id = request.getLong("room_id");
            String password = request.getString("password");
            RoomSetting room_setting = getContext().getRoomManager().onPlayerJoinRoom(player, room_id, password);
            if (room_setting == null) {
                response.put("approved", false);
            } else {
                response.put("room_setting", room_setting.toJson());
                response.put("approved", true);
            }
            player.sendTCP(response.toString());
        }
    }

    public void onAllocationUpdateRequested(Player player, JSONObject request) {
        if (player.isAuthenticated()) {
            JSONArray types = request.getJSONArray("types");
            JSONArray alliance = request.getJSONArray("alliance");
            JSONArray allocation = request.getJSONArray("allocation");
            getContext().getRoomManager().onAllocationUpdate(player, types, alliance, allocation);
        }
    }

    public void onGameEventSubmitted(Player player, JSONObject request) {
        if (player.isAuthenticated()) {
            JSONArray events = request.getJSONArray("events");
            getContext().getRoomManager().submitGameEvents(player, events);
        }
    }

    public void onMessageSubmitted(Player player, JSONObject request) {
        if (player.isAuthenticated()) {
            Room room = getContext().getRoomManager().getRoom(player.getRoomID());
            String message = request.getString("message");
            if (room == null) {
                getContext().getNotificationSender().notifyLobbyMessage(player.getUsername(), message);
            } else {
                getContext().getNotificationSender().notifyRoomMessage(room, player.getUsername(), message);
            }
        }
    }

    public void onGameStartRequested(Player player) {
        if (player.isAuthenticated()) {
            JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
            boolean approved = getContext().getRoomManager().tryStartGame(player);
            response.put("approved", approved);
            player.sendTCP(response.toString());
        }
    }

    public void onMapListRequested(Player player, JSONObject request) {
        if (getContext().getConfiguration().isDatabaseEnabled()) {
            JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
            boolean symmetric = request.has("symmetric") && request.getBoolean("symmetric");
            if (request.has("author")) {
                String author = request.getString("author");
                response.put("maps", getContext().getMapManager().getSerializedMapList(author, symmetric));
            } else {
                String prefix = request.has("prefix") ? request.getString("prefix") : null;
                response.put("maps", getContext().getMapManager().getSerializedAuthorList(prefix, symmetric));
            }
            player.sendTCP(response.toString());
        }
    }

    public void onMapUploadRequest(Player player, JSONObject request) {
        if (getContext().getConfiguration().isDatabaseEnabled()) {
            JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
            Map map = new Map(request.getJSONObject("map"));
            String map_name = request.getString("map_name");
            try {
                getContext().getMapManager().addMap(map, map_name);
                response.put("code", NetworkConstants.CODE_OK);
            } catch (MapManager.MapExistingException ex) {
                response.put("code", NetworkConstants.CODE_MAP_EXISTING);
            } catch (Exception ex) {
                response.put("code", NetworkConstants.CODE_SERVER_ERROR);
            }
            player.sendTCP(response.toString());
        }
    }

    public void onMapDownloadRequested(Player player, JSONObject request) {
        if (getContext().getConfiguration().isDatabaseEnabled()) {
            JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
            int map_id = request.getInt("id");
            boolean approved;
            try {
                Map map = getContext().getMapManager().getMap(map_id);
                response.put("map", map.toJson());
                approved = true;
            } catch (IOException ex) {
                approved = false;
            } catch (GameException ex) {
                approved = false;
            }
            response.put("approved", approved);
            player.sendTCP(response.toString());
        }
    }

    public void onIdlePlayerListRequested(Player player) {
        if (player.isAuthenticated()) {
            JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
            JSONArray players = new JSONArray();
            for (Player target : getContext().getPlayerManager().getPlayers()) {
                if (target.isAuthenticated() && target.getRoomID() < 0) {
                    players.put(target.createSnapshot().toJson());
                }
            }
            response.put("players", players);
            player.sendTCP(response.toString());
        }
    }

    public void onGlobalMessageSubmitted(JSONObject request) {
        String token = request.getString("token");
        if (getContext().verifyAdminToken(token)) {
            String message = request.getString("message");
            getContext().getNotificationSender().notifyGlobalMessage(message);
        }
    }

    public void onMapDeleteRequested(Player player, JSONObject request) {
        String token = request.getString("token");
        if (getContext().verifyAdminToken(token)) {
            JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
            int map_id = request.getInt("id");
            try {
                boolean success = getContext().getMapManager().removeMap(map_id);
                response.put("success", success);
            } catch (SQLException e) {
                response.put("success", false);
            }
            player.sendTCP(response.toString());
        }
    }

    public void onMapUpdateRequested(Player player, JSONObject request) {
        String token = request.getString("token");
        if (getContext().verifyAdminToken(token)) {
            JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
            int map_id = request.getInt("id");
            String author = request.has("author") ? request.getString("author") : null;
            String filename = request.has("filename") ? request.getString("filename") : null;
            try {
                boolean success = getContext().getMapManager().updateMap(map_id, author, filename);
                response.put("success", success);
            } catch (Exception e) {
                response.put("success", false);
            }
            player.sendTCP(response.toString());
        }
    }

    public void onReconnectRequested(Player player, JSONObject request) {
        String username = request.getString("username");
        String v_string = request.getString("v_string");

        player.setUsername(username);

        JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
        if (getContext().getVerificationString().equals(v_string)) {
            player.setAuthenticated(true);
            long room_id = request.getLong("room_id");
            int previous_id = request.getInt("previous_id");
            RoomSetting room_setting = getContext().getRoomManager().onPlayerReconnect(player, previous_id, room_id);
            if (room_setting == null) {
                response.put("approved", false);
            } else {
                response.put("room_setting", room_setting.toJson());
                response.put("service_id", player.getID());
                response.put("approved", true);
            }
        } else {
            response.put("approved", false);
        }
        player.sendTCP(response.toString());
    }

    public void onDisconnectPlayerRequested(Player player, JSONObject request) {
        String token = request.getString("token");
        if (getContext().verifyAdminToken(token)) {
            JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
            int player_id = request.getInt("player_id");
            Player target_player;
            if ((target_player = getContext().getPlayerManager().getPlayer(player_id)) == null) {
                response.put("success", false);
            } else {
                target_player.getConnection().close();
                response.put("success", true);
            }
            player.sendTCP(response.toString());
        }
    }

    public void onRecordSubmitted(Player player, JSONObject request) {
        JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
        String v_string = request.getString("v_string");
        if (getContext().getVerificationString().equals(v_string)) {
            String username = request.getString("username");
            String campaign_code = request.getString("campaign_code");
            int stage_number = request.getInt("stage_number");
            int turns = request.getInt("turns");
            int actions = request.getInt("actions");
            if (stage_number >= 0 && turns > 0 && actions > 0) {
                try {
                    getContext().getDatabaseManager().submitRecord(
                            username, player.getAddress(), campaign_code, stage_number, turns, actions);
                    response.put("approved", true);
                } catch (SQLException ex) {
                    Log.error(TAG, "Error submitting record", ex);
                    response.put("approved", false);
                }
            } else {
                response.put("approved", false);
            }
        } else {
            response.put("approved", false);
        }
        player.sendTCP(response.toString());
    }

    public void onBestRecordRequested(Player player, JSONObject request) {
        JSONObject response = PacketBuilder.create(NetworkConstants.RESPONSE);
        String campaign_code = request.getString("campaign_code");
        int stage_number = request.getInt("stage_number");
        try {
            LeaderboardRecord record = getContext().getDatabaseManager().getBestRecord(campaign_code, stage_number);
            response.put("response_code", NetworkConstants.CODE_OK);
            response.put("record", record.toJson());
        } catch (SQLException ex) {
            Log.error(TAG, "Error getting best record", ex);
            response.put("response_code", NetworkConstants.CODE_SERVER_ERROR);
        }
        player.sendTCP(response.toString());
    }

    public void submitRequest(Player player, String request_content) throws JSONException {
        executor.submit(new RequestProcessingTask(player, new JSONObject(request_content)));
    }

    private class RequestProcessingTask implements Runnable {

        private final Player player;
        private final JSONObject request;

        public RequestProcessingTask(Player player, JSONObject request) {
            this.player = player;
            this.request = request;
        }

        public Player getPlayer() {
            return player;
        }

        public JSONObject getRequest() {
            return request;
        }

        @Override
        public void run() {
            doHandleRequest(getPlayer(), getRequest());
        }
    }

}
