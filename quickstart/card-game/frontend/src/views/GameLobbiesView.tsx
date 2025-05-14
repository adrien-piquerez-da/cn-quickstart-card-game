// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { useEffect } from 'react';
import { useGameLobbyStore } from '../stores/gameLobbyStore';
import { useUserStore } from '../stores/userStore';
import type { GameLobby } from "../openapi.d.ts";

const GameLobbiesView: React.FC = () => {
  const {
    gameLobbies,
    fetchAll,
    join,
    start,
  } = useGameLobbyStore();
  const { user, fetchUser } = useUserStore();

  useEffect(() => {
    fetchUser();
    fetchAll();
    const intervalId = setInterval(() => {
      fetchAll();
    }, 1000);
    return () => clearInterval(intervalId);
  }, [fetchUser, fetchAll]);

  return (
    <div>
      <h2>Game Lobbies</h2>
      <div className="alert alert-info" role="alert">
        <strong>Note:</strong> Run <code>make create-game-lobby</code> to create
        a GameLobby
      </div>
      <div className="mt-4">
        <table className="table table-fixed" id="game-lobbies-table">
          <thead>
            <tr>
              <th style={{ width: "150px" }}>Contract ID</th>
              <th style={{ width: "100px" }}>Gamemaster</th>
              <th style={{ width: "150px" }}>Players</th>
              <th style={{ width: "150px" }}>Invitees</th>
              <th style={{ width: "310px" }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {gameLobbies.map((item: GameLobby) => (
              <tr key={item.contractId} className="app-lobby-row">
                <td className="ellipsis-cell game-lobby-contract-id">
                  {item.contractId}
                </td>
                <td className="ellipsis-cell game-lobby-gamemaster">
                  {item.gamemaster}
                </td>
                <td className="ellipsis-cell game-lobby-players">
                  {item.players}
                </td>
                <td className="ellipsis-cell game-lobby-invitees">
                  {item.invitees}
                </td>
                <td className="game-lobby-actions">
                  <div className="btn-group" role="group">
                    {user?.party === item.gamemaster ? (
                      <button
                        className="btn btn-success btn-start-game"
                        onClick={() => start(item.contractId)}
                      >
                        Start Game
                      </button>
                    ) : null}
                    {item.invitees.includes(user?.party ?? "") ? (
                      <button
                        className="btn btn-success btn-join-game"
                        onClick={() => join(item.contractId)}
                      >
                        Join Game
                      </button>
                    ) : null}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default GameLobbiesView;