package be.isach.ultracosmetics.mysql.tables;

import be.isach.ultracosmetics.mysql.column.Column;
import be.isach.ultracosmetics.mysql.column.UUIDColumn;
import be.isach.ultracosmetics.mysql.column.VirtualUUIDColumn;
import be.isach.ultracosmetics.player.profile.ProfileKey;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import javax.sql.DataSource;

public class PlayerDataTable extends Table {

    public PlayerDataTable(DataSource dataSource, String name) {
        super(dataSource, name);
    }

    @Override
    protected void setupTableInfo() {
        tableInfo.add(new UUIDColumn("uuid", "BINARY(16) PRIMARY KEY"));
        tableInfo.add(new VirtualUUIDColumn());
        tableInfo.add(new Column<>("gadgetsEnabled", "BOOLEAN NOT NULL DEFAULT 1", Boolean.class));
        tableInfo.add(new Column<>("selfMorphView", "BOOLEAN NOT NULL DEFAULT 1", Boolean.class));
        tableInfo.add(new Column<>("treasureNotifications", "BOOLEAN NOT NULL DEFAULT 0", Boolean.class));
        tableInfo.add(new Column<>("filterByOwned", "BOOLEAN NOT NULL DEFAULT 0", Boolean.class));
        tableInfo.add(new Column<>("treasureKeys", "INTEGER NOT NULL DEFAULT 0", Integer.class));
    }

    public void addPlayer(UUID uuid) {
        insertIgnore("uuid").insert(insertUUID(uuid)).execute();
    }

    public Map<String,Object> getSettings(UUID uuid) {
        StringJoiner columns = new StringJoiner(", ");
        for (ProfileKey key : ProfileKey.values()) {
            if (key.getSqlKey() == null) continue;
            columns.add(key.getSqlKey());
        }
        return select(columns.toString())
                .uuid(uuid).getResults(r -> {
                    Map<String,Object> settings = new HashMap<>();
                    for (ProfileKey key : ProfileKey.values()) {
                        if (key.getSqlKey() == null) continue;
                        if (key == ProfileKey.KEYS) {
                            settings.put(key.getSqlKey(), r.getInt(key.getSqlKey()));
                        }
                        settings.put(key.getSqlKey(), r.getBoolean(key.getSqlKey()));
                    }

                    return settings;
                });
    }

    public boolean getSetting(UUID uuid, ProfileKey key) {
        return select(key.getSqlKey()).asBool();
    }

    public void setSetting(UUID uuid, ProfileKey key, Object value) {
        update().set(key.getSqlKey(), value).execute();
    }

    public int getKeys(UUID uuid) {
        return select("treasureKeys").uuid(uuid).asInt();
    }

    public void setKeys(UUID uuid, int keys) {
        update().set("treasureKeys", keys).execute();
    }
}
