// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.repository;

import com.digitalasset.quickstart.pqs.Contract;
import com.digitalasset.quickstart.pqs.Pqs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import card_game.cardgame.GameLobby;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for accessing active Daml contracts via PQS.
 */
@Repository
public class DamlRepository {

    private final Pqs pqs;

    @Autowired
    public DamlRepository(Pqs pqs) {
        this.pqs = pqs;
    }

    /**
     * Finds all active GameLobby contracts.
     */
    public CompletableFuture<List<Contract<GameLobby>>> findActiveGameLobbies() {
        return pqs.active(GameLobby.class);
    }

    /**
     * Fetches a GameLobby contract by contract ID.
     */
    public CompletableFuture<Contract<GameLobby>> findGameLobbyById(String contractId) {
        return pqs.byContractId(GameLobby.class, contractId);
    }
}
