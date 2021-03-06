/*
 * Copyright 2021 NAFU_at
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

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import page.nafuchoco.soloservercore.event.PlayersTeamDisappearanceEvent;
import page.nafuchoco.teammessageboard.database.MessagesTable;

import java.sql.SQLException;
import java.util.logging.Level;

public class PlayersTeamDisappearanceEventListener implements Listener {
    private final MessagesTable messagesTable;

    public PlayersTeamDisappearanceEventListener(MessagesTable messagesTable) {
        this.messagesTable = messagesTable;
    }

    @EventHandler
    public void onPlayersTeamDisappearanceEvent(PlayersTeamDisappearanceEvent event) {
        try {
            messagesTable.deleteAllMessages(event.getPlayersTeam().getId());
        } catch (SQLException e) {
            TeamMessageBoard.getInstance().getLogger().log(Level.WARNING, "An error occurred while deleting inventory data.", e);
        }
    }
}
