/*
 * Copyright 2020 NAFU_at
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package page.nafuchoco.teammessageboard;

import org.bukkit.configuration.file.FileConfiguration;
import page.nafuchoco.soloservercore.SoloServerCoreConfig;

public class TeamMessageBoardConfig {
    private static final TeamMessageBoard instance = TeamMessageBoard.getInstance();
    private InitConfig initConfig;

    public void reloadConfig() {
        instance.reloadConfig();
        FileConfiguration config = instance.getConfig();

        if (initConfig == null) {
            SoloServerCoreConfig.DatabaseType databaseType = SoloServerCoreConfig.DatabaseType.valueOf(config.getString("initialization.database.type"));
            String address = config.getString("initialization.database.address");
            int port = config.getInt("initialization.database.port", 3306);
            String database = config.getString("initialization.database.database");
            String username = config.getString("initialization.database.username");
            String password = config.getString("initialization.database.password");
            String tablePrefix = config.getString("initialization.database.tablePrefix");
            initConfig = new InitConfig(databaseType, address, port, database, username, password, tablePrefix);
        }
    }

    public InitConfig getInitConfig() {
        return initConfig;
    }

    public static class InitConfig {
        private final SoloServerCoreConfig.DatabaseType databaseType;
        private final String address;
        private final int port;
        private final String database;
        private final String username;
        private final String password;
        private final String tablePrefix;

        public InitConfig(SoloServerCoreConfig.DatabaseType databaseType, String address, int port, String database, String username, String password, String tablePrefix) {
            this.databaseType = databaseType;
            this.address = address;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
            this.tablePrefix = tablePrefix;
        }

        public SoloServerCoreConfig.DatabaseType getDatabaseType() {
            return databaseType;
        }

        public String getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public String getDatabase() {
            return database;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getTablePrefix() {
            return tablePrefix;
        }
    }
}
