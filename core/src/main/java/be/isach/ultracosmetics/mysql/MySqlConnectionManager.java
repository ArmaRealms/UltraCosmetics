package be.isach.ultracosmetics.mysql;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.cosmetics.suits.ArmorSlot;
import be.isach.ultracosmetics.cosmetics.type.GadgetType;
import be.isach.ultracosmetics.cosmetics.type.PetType;
import be.isach.ultracosmetics.util.SmartLogger;
import be.isach.ultracosmetics.util.SmartLogger.LogLevel;

import org.bukkit.configuration.ConfigurationSection;

import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.sql.DataSource;

/**
 * Package: be.isach.ultracosmetics.mysql
 * Created by: sachalewin
 * Date: 5/08/16
 * Project: UltraCosmetics
 */
public class MySqlConnectionManager {
    private final String database;
    private final String tableName;
    /**
     * UltraCosmetics instance.
     */
    private final UltraCosmetics ultraCosmetics;

    /**
     * MySQL Connection & Table.
     */
    private Table table;

    /**
     * Connecting pooling.
     */
    private final HikariHook hikariHook;
    private final DataSource dataSource;
    private final String CREATE_TABLE;
    private final List<Column<?>> columns = new ArrayList<>();
    private final boolean debug;
    private boolean success = true;

    public MySqlConnectionManager(UltraCosmetics ultraCosmetics) {
        this.ultraCosmetics = ultraCosmetics;
        ConfigurationSection section = SettingsManager.getConfig().getConfigurationSection("MySQL");
        this.debug = section.getBoolean("debug", false);
        String hostname = section.getString("hostname");
        String port = section.getString("port");
        database = section.getString("database");
        String username = section.getString("username");
        String password = section.getString("password");
        tableName = section.getString("table");
        HikariHook hook;

        try {
            hook = new HikariHook(hostname, port, database, username, password);
        } catch (PoolInitializationException e) {
            // We have to do this weirdness to be able to break out of the constructor early.
            hikariHook = null;
            dataSource = null;
            CREATE_TABLE = null;
            reportFailure(e);
            return;
        }

        hikariHook = hook;

        dataSource = hikariHook.getDataSource();

        // "PRIMARY KEY" implies UNIQUE NOT NULL.
        // String form of UUID is always exactly 36 chars so just store it that way.
        // This is not a StringColumn because StringColumns are varchars
        columns.add(new Column<>("uuid", "CHAR(36) CHARACTER SET latin1 PRIMARY KEY", String.class));
        columns.add(new Column<>("gadgetsEnabled", "BOOLEAN DEFAULT TRUE NOT NULL", Boolean.class));
        columns.add(new Column<>("selfmorphview", "BOOLEAN DEFAULT TRUE NOT NULL", Boolean.class));
        columns.add(new Column<>("treasureNotifications", "BOOLEAN DEFAULT TRUE NOT NULL", Boolean.class));
        columns.add(new Column<>("filterByOwned", "BOOLEAN DEFAULT FALSE NOT NULL", Boolean.class));
        columns.add(new Column<>("treasureKeys", "INTEGER DEFAULT 0 NOT NULL", Integer.class));
        for (GadgetType gadgetType : GadgetType.values()) {
            columns.add(new Column<>(gadgetType.getConfigName().toLowerCase(), "INTEGER DEFAULT 0 NOT NULL", Integer.class));
        }
        for (PetType petType : PetType.values()) {
            // Anvil can only hold 50 characters on 1.18
            // But, for 4-byte-per-char encoding, a length of at least 64 (size 256) will cause it to be stored "off page"
            columns.add(new StringColumn(petType.getConfigName().toLowerCase(), 64, false));
        }

        for (Category cat : Category.values()) {
            // 32 because it's about double the longest existing cosmetic name
            if (cat == Category.SUITS) {
                for (ArmorSlot slot : ArmorSlot.values()) {
                    columns.add(new StringColumn(cat.toString().toLowerCase() + "_" + slot.toString().toLowerCase(), 32, true));
                }
                continue;
            }

            columns.add(new StringColumn(cat.toString().toLowerCase(), 32, true));
        }

        StringJoiner columnJoiner = new StringJoiner(", ", "(", ")");
        for (Column<?> column : columns) {
            columnJoiner.add(column.toString());
        }
        CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `" + tableName + "`" + columnJoiner.toString() + " ROW_FORMAT=DYNAMIC";

        startup();
    }

    public void startup() {
        try (Connection co = dataSource.getConnection()) {
            if (isDebug()) {
                ultraCosmetics.getSmartLogger().write("Executing table build SQL: " + CREATE_TABLE);
            }
            try (PreparedStatement sql = co.prepareStatement(CREATE_TABLE)) {
                sql.executeUpdate();
            }

            fixTable(co);

            table = new Table(dataSource, tableName);
        } catch (SQLException e) {
            reportFailure(e);
            return;
        }
    }

    private void reportFailure(Throwable e) {
        success = false;
        UltraCosmeticsData.get().setFileStorage(true);
        SmartLogger log = ultraCosmetics.getSmartLogger();
        log.write(LogLevel.ERROR, "Could not connect to MySQL server!");
        log.write(LogLevel.ERROR, "Error:");
        e.printStackTrace();
    }

    public Table getTable() {
        return table;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean success() {
        return success;
    }

    public List<Column<?>> getColumns() {
        return columns;
    }

    public void shutdown() {
        hikariHook.close();
    }

    /**
     * Based on the field 'columns', adds any missing columns to the database (in order).
     *
     * @param co Connection to work with.
     */
    private void fixTable(Connection co) throws SQLException {
        DatabaseMetaData md = co.getMetaData();
        boolean upgradeAnnounced = false;
        if (columnExists("id", md)) {
            ultraCosmetics.getSmartLogger().write("You have an old UCData table. Attempting to upgrade it...");
            alter(co, "DROP COLUMN id");
            alter(co, "DROP COLUMN username");
            alter(co, "MODIFY uuid CHAR(36) CHARACTER SET latin1");
            alter(co, "ADD PRIMARY KEY (uuid)");
        }

        for (int i = 0; i < columns.size(); i++) {
            Column<?> col = columns.get(i);
            if (isDebug()) {
                ultraCosmetics.getSmartLogger().write("Checking column " + col.getName());
            }
            try (ResultSet rs = md.getColumns(database, null, tableName, col.getName())) {
                if (!rs.next()) {
                    if (isDebug()) {
                        ultraCosmetics.getSmartLogger().write("column " + col.getName() + " does not exist");
                    }
                    if (!upgradeAnnounced) {
                        ultraCosmetics.getSmartLogger().write("Upgrading database...");
                        upgradeAnnounced = true;
                    }
                    int j = i - 1;
                    // Ensure we don't try to create a column after a column that doesn't exist.
                    // This mainly happens when the order of pets is rearranged in PetType
                    while (!columnExists(columns.get(j).getName(), md)) {
                        j--;
                    }
                    String afterPrevious = "AFTER " + columns.get(j).getName();
                    alter(co, "ADD " + col.toString() + " " + afterPrevious);
                    continue;
                }
                if (isDebug()) {
                    ultraCosmetics.getSmartLogger().write("column exists: " + rs.getString("COLUMN_NAME") + " in " + rs.getString("TABLE_NAME") + " in cat " + rs.getString("TABLE_CAT"));
                }
                // if a varchar is the wrong size, adjust it
                if (col instanceof StringColumn && rs.getInt("COLUMN_SIZE") != ((StringColumn) col).getSize()) {
                    if (isDebug()) {
                        ultraCosmetics.getSmartLogger().write("column is wrong size, " + rs.getInt("COLUMN_SIZE") + " != " + ((StringColumn) col).getSize());
                    }
                    if (!upgradeAnnounced) {
                        ultraCosmetics.getSmartLogger().write("Upgrading database...");
                        upgradeAnnounced = true;
                    }
                    alter(co, "MODIFY COLUMN " + col.toString());
                }
            }
        }
        if (upgradeAnnounced) {
            ultraCosmetics.getSmartLogger().write("Upgrade finished.");
        }
    }

    private boolean columnExists(String column, DatabaseMetaData md) throws SQLException {
        try (ResultSet rs = md.getColumns(database, null, tableName, column)) {
            return rs.next();
        }
    }

    private void alter(Connection co, String command) throws SQLException {
        String query = "ALTER TABLE `" + tableName + "` " + command;
        if (isDebug()) {
            UltraCosmeticsData.get().getPlugin().getSmartLogger().write("Executing SQL: " + query);
        }
        PreparedStatement ps = co.prepareStatement(query);
        ps.execute();
        ps.close();
    }
}
