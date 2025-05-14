// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import React, { createContext, useContext, useState, useCallback } from 'react';
import api from '../api';
import { generateCommandId } from '../utils/commandId';
import { generateSeed } from "../utils/seed";
import { useToast } from './toastStore';
import type {
    GameLobby,
    GameLobbyStart,
    Client,
} from '../openapi.d.ts';
// import { AppInstallUnified } from '../types';

interface GameLobbyState {
    gameLobbies: GameLobby[];
}

interface GameLobbyContextType extends GameLobbyState {
  fetchAll: () => Promise<void>;
  join: (contractId: string) => Promise<void>;
  start: (contractId: string) => Promise<void>;
}

const GameLobbyContext = createContext<GameLobbyContextType | undefined>(
  undefined
);

export const GameLobbyProvider = ({ children }: { children: React.ReactNode }) => {
    const [gameLobbies, setGameLobbies] = useState<GameLobby[]>([]);
    const toast = useToast();

    const fetchAll = useCallback(async () => {
        try {
            const client: Client = await api.getClient();
            const lobbiesResponse = await client.listGameLobbies();
            const lobbies: GameLobby[] = lobbiesResponse.data;

            setGameLobbies(lobbies);
        } catch (error) {
            toast.displayError('Error fetching game lobbies');
        }
    }, [toast]);

    const join = useCallback(
        async (contractId: string) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                await client.joinGameLobby(
                    { contractId, commandId }
                );
                await fetchAll();
                toast.displaySuccess(`Joined GameLobby ${contractId}`);
            } catch (error) {
                toast.displayError('Error joining GameLobby');
            }
        },
        [toast, fetchAll]
    );

    const start = useCallback(
        async (contractId: string) => {
            try {
                const client: Client = await api.getClient();
                const commandId = generateCommandId();
                const seed = generateSeed();
                await client.startGame(
                    { contractId, commandId },
                    { seed } as GameLobbyStart
                );
                await fetchAll();
                toast.displaySuccess(`Game started ${contractId}`);
            } catch (error) {
                toast.displayError('Error starting Game');
            }
        },
        [toast, fetchAll]
    );

    return (
      <GameLobbyContext.Provider
        value={{
          gameLobbies,
          fetchAll,
          join,
          start,
        }}
      >
        {children}
      </GameLobbyContext.Provider>
    );
};

export const useGameLobbyStore = () => {
    const context = useContext(GameLobbyContext);
    if (context === undefined) {
        throw new Error(
          "useGameLobbyStore must be used within an GameLobbyProvider"
        );
    }
    return context;
};
