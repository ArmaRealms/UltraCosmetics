package be.isach.ultracosmetics.mysql;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.mysql.tables.AmmoTable;
import be.isach.ultracosmetics.mysql.tables.CosmeticTable;
import be.isach.ultracosmetics.mysql.tables.EquippedTable;
import be.isach.ultracosmetics.mysql.tables.PetNameTable;
import be.isach.ultracosmetics.mysql.tables.PlayerDataTable;
import be.isach.ultracosmetics.mysql.tables.Table;
import be.isach.ultracosmetics.mysql.tables.UnlockedTable;
import be.isach.ultracosmetics.util.SmartLogger;
import be.isach.ultracosmetics.util.SmartLogger.LogLevel;

import org.bukkit.configuration.ConfigurationSection;

import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Package: be.isach.ultracosmetics.mysql
 * Created by: sachalewin
 * Date: 5/08/16
 * Project: UltraCosmetics
 */
public class MySqlConnectionManager {
    public static final int MAX_NAME_SIZE = 256;
    private final String database;
    /**
     * UltraCosmetics instance.
     */
    private final UltraCosmetics ultraCosmetics;

    /**
     * Table for storing cosmetic IDs
     */
    private CosmeticTable cosTable;

    /**
     * Stores keys and settings
     */
    private PlayerDataTable playerData;

    /**
     * Stores ammo :)
     */
    private AmmoTable ammoTable;

    /**
     * Stores pet names :)
     */
    private PetNameTable petNames;

    /**
     * Stores equipped cosmetics :)
     */
    private EquippedTable equippedTable;

    /**
     * Table for storing unlocked cosmetics
     */
    private UnlockedTable unlockedTable;

    /**
     * Connecting pooling.
     */
    private final HikariHook hikariHook;
    private final DataSource dataSource;
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
        HikariHook hook;

        try {
            hook = new HikariHook(hostname, port, database, username, password);
        } catch (PoolInitializationException e) {
            // We have to do this weirdness to be able to break out of the constructor early.
            hikariHook = null;
            dataSource = null;
            reportFailure(e);
            return;
        }

        hikariHook = hook;

        dataSource = hikariHook.getDataSource();

        playerData = new PlayerDataTable(dataSource, section.getString("player-data-table"));
        cosTable = new CosmeticTable(dataSource, section.getString("cosmetics-table"));
        if (UltraCosmeticsData.get().isAmmoEnabled()) {
            ammoTable = new AmmoTable(dataSource, section.getString("ammo-table"), playerData, cosTable);
        }
        if (SettingsManager.getConfig().getBoolean("Pets-Rename.Enabled")) {
            petNames = new PetNameTable(dataSource, section.getString("pet-names-table"), playerData, cosTable);
        }
        if (UltraCosmeticsData.get().areCosmeticsProfilesEnabled()) {
            equippedTable = new EquippedTable(dataSource, section.getString("equipped-cosmetics-table"), playerData, cosTable);
        }

        if (SettingsManager.getConfig().getString("TreasureChests.Permission-Add-Command").isEmpty()) {
            unlockedTable = new UnlockedTable(dataSource, section.getString("unlocked-cosmetics-table"), playerData, cosTable);
        }
    }

    public void start() {
        try (Connection conn = dataSource.getConnection()) {
            create(conn, playerData);
            create(conn, cosTable);
            create(conn, unlockedTable);
            create(conn, ammoTable);
            create(conn, petNames);
            create(conn, equippedTable);
        } catch (SQLException e) {
            reportFailure(e);
        }
    }

    private void create(Connection conn, Table table) throws SQLException {
        if (table == null) return;
        table.setupTableInfo();
        String statement = table.getCreateTableStatement();
        if (debug) {
            ultraCosmetics.getSmartLogger().write("Executing create table: " + statement);
        }
        conn.createStatement().execute(statement);
        table.loadBaseData();
    }

    private void reportFailure(Throwable e) {
        success = false;
        UltraCosmeticsData.get().setFileStorage(true);
        SmartLogger log = ultraCosmetics.getSmartLogger();
        log.write(LogLevel.ERROR, "Could not connect to MySQL server!");
        log.write(LogLevel.ERROR, "Error:");
        e.printStackTrace();
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

    public void shutdown() {
        hikariHook.close();
    }

    public CosmeticTable getCosTable() {
        return cosTable;
    }

    public PlayerDataTable getPlayerData() {
        return playerData;
    }

    public AmmoTable getAmmoTable() {
        return ammoTable;
    }

    public PetNameTable getPetNames() {
        return petNames;
    }

    public EquippedTable getEquippedTable() {
        return equippedTable;
    }

    public UnlockedTable getUnlockedTable() {
        return unlockedTable;
    }
}
