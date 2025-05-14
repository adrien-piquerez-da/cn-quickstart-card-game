// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.service;

import com.digitalasset.quickstart.api.GameLobbyApi;
import com.digitalasset.quickstart.ledger.LedgerApi;
import com.digitalasset.quickstart.security.AuthenticatedPartyProvider;
import com.digitalasset.quickstart.repository.DamlRepository;
import com.digitalasset.quickstart.utility.LoggingSpanHelper;
import com.digitalasset.transcode.java.Party;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import org.openapitools.model.GameLobby;
import org.openapitools.model.GameLobbyJoin;
import org.openapitools.model.GameLobbyStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.digitalasset.quickstart.utility.ContextAwareCompletableFutures.completeWithin;
import static card_game.cardgame.GameLobby.TEMPLATE_ID;

@Controller
@RequestMapping("${openapi.asset.base-path:}")
public class GameLobbyApiImpl implements GameLobbyApi {

    private static final Logger logger = LoggerFactory.getLogger(GameLobbyApiImpl.class);

    private final LedgerApi ledger;
    private final AuthenticatedPartyProvider authenticatedPartyProvider;
    private final DamlRepository damlRepository;

    @Autowired
    public GameLobbyApiImpl(
            LedgerApi ledger,
            AuthenticatedPartyProvider authenticatedPartyProvider,
            DamlRepository damlRepository
    ) {
        this.ledger = ledger;
        this.authenticatedPartyProvider = authenticatedPartyProvider;
        this.damlRepository = damlRepository;
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<GameLobby>> joinGameLobby(
        @SpanAttribute("joinGameLobby.contractId") String contractId,
        @SpanAttribute("joinGameLobby.commandId") String commandId,
        GameLobbyJoin gameLobbyJoin
    ) {
        Span span = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> attributes = Map.of(
                "contractId", contractId,
                "commandId", commandId,
                "templateId", TEMPLATE_ID.qualifiedName(),
                "choiceName", "JoinGame"
        );

        LoggingSpanHelper.addEventWithAttributes(span, "Starting joinGameLobby", attributes);
        LoggingSpanHelper.setSpanAttributes(span, attributes);
        LoggingSpanHelper.logInfo(logger, "joinGameLobby: received request", attributes);

        return CompletableFuture.completedFuture(authenticatedPartyProvider.getPartyOrFail())
            .thenCompose(providerParty ->
                damlRepository.findGameLobbyById(contractId)
                    .thenCompose(contract -> {
                        span.addEvent("Fetched contract, joining game lobby");
                        var choice = new card_game.cardgame.GameLobby.JoinGame(new Party(gameLobbyJoin.getPlayer()));
                        return ledger.exerciseAndGetResult(providerParty, contract.contractId, choice, commandId);
                    })
            )
            .thenCompose(contractId1 -> {
                span.addEvent("Exercised choice, fetching contract");
                return damlRepository.findGameLobbyById(contractId1.getContractId);
            })
            .thenApply(contract -> {
                span.addEvent("Fetched contract, building game lobby response");
                var gameLobby = new GameLobby();
                gameLobby.setContractId(contract.contractId.getContractId);
                gameLobby.setGamemaster(contract.payload.getGamemaster.getParty);
                gameLobby.setInvitees(contract.payload.getInvitees.stream().map(p -> p.getParty).collect(Collectors.toList()));
                gameLobby.setPlayers(contract.payload.getPlayers.stream().map(p -> p.getParty).collect(Collectors.toList()));
                return ResponseEntity.ok(new GameLobby());
            })
            .whenComplete(
                completeWithin(parentContext, (res, ex) -> {
                    if (ex == null) {
                        LoggingSpanHelper.logDebug(logger, "joinGameLobby: success", attributes);
                    } else {
                        LoggingSpanHelper.logError(logger, "joinGameLobby: failed", attributes, ex);
                        LoggingSpanHelper.recordException(span, ex);
                    }
                })
            );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<List<GameLobby>>> listGameLobbies() {
        Span span = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> attributes = Map.of(
            "templateId", TEMPLATE_ID.qualifiedName()
        );

        LoggingSpanHelper.addEventWithAttributes(span, "Starting listAppInstallRequests", attributes);
        LoggingSpanHelper.setSpanAttributes(span, attributes);
        LoggingSpanHelper.logInfo(logger, "listAppInstallRequests: received request, retrieving active requests", attributes);

        return CompletableFuture.completedFuture(authenticatedPartyProvider.getPartyOrFail())
            .thenCompose(party ->
                damlRepository.findActiveGameLobbies()
                    .thenApply(contracts -> {
                        span.addEvent("Fetched active game lobbies, filtering by current party");

                        List<GameLobby> result = contracts.stream()
                            .filter(contract -> {
                                String gamemaster = contract.payload.getGamemaster.getParty;
                                List<String> invitees = contract.payload.getInvitees.stream().map(p -> p.getParty).collect(Collectors.toList());
                                List<String> players = contract.payload.getPlayers.stream().map(p -> p.getParty).collect(Collectors.toList());
                                return gamemaster.equals(party) || invitees.contains(party) || players.contains(party);
                            })
                            .map(contract -> {
                                GameLobby appInstallRequest = new GameLobby();
                                var gameLobby = new GameLobby();
                                gameLobby.setContractId(contract.contractId.getContractId);
                                gameLobby.setGamemaster(contract.payload.getGamemaster.getParty);
                                gameLobby.setInvitees(contract.payload.getInvitees.stream().map(p -> p.getParty).collect(Collectors.toList()));
                                gameLobby.setPlayers(contract.payload.getPlayers.stream().map(p -> p.getParty).collect(Collectors.toList()));
                                return appInstallRequest;
                            })
                            .toList();
                        return ResponseEntity.ok(result);
                    })
            )
            .whenComplete(
                completeWithin(parentContext, (res, ex) -> {
                    if (ex == null) {
                        int count = (res.getBody() == null) ? 0 : res.getBody().size();
                        logger.atDebug().addKeyValue("recordCount", count).log("listAppInstallRequests: success");
                    } else {
                        LoggingSpanHelper.logError(logger, "listAppInstallRequests: failed", attributes, ex);
                        LoggingSpanHelper.recordException(span, ex);
                    }
                })
            );
    }

    @Override
    @WithSpan
    public CompletableFuture<ResponseEntity<Void>> startGame(
        @SpanAttribute("startGame.contractId") String contractId,
        @SpanAttribute("startGame.commandId") String commandId,
        GameLobbyStart gameLobbyStart
    ) {
        Span span = Span.current();
        Context parentContext = Context.current();

        Map<String, Object> attributes = Map.of(
                "contractId", contractId,
                "commandId", commandId,
                "templateId", TEMPLATE_ID.qualifiedName(),
                "choiceName", "StartGame"
        );

        LoggingSpanHelper.addEventWithAttributes(span, "Starting startGame", attributes);
        LoggingSpanHelper.setSpanAttributes(span, attributes);
        LoggingSpanHelper.logInfo(logger, "startGame: received request", attributes);

        return CompletableFuture.completedFuture(authenticatedPartyProvider.getPartyOrFail())
            .thenCompose(providerParty ->
                damlRepository.findGameLobbyById(contractId)
                    .thenCompose(contract -> {
                        span.addEvent("Fetched contract, starting game");
                        var choice = new card_game.cardgame.GameLobby.StartGame(gameLobbyStart.getSeed());
                        return ledger.exerciseAndGetResult(providerParty, contract.contractId, choice, commandId);
                    })
            )
            .thenApply(appInstallContractId -> {
                span.addEvent("Choice exercised, returning 200 OK");
                return ResponseEntity.ok().<Void>build();
            })
            .whenComplete(
                completeWithin(parentContext, (res, ex) -> {
                    if (ex == null) {
                        LoggingSpanHelper.logDebug(logger, "startGame: success", attributes);
                    } else {
                        LoggingSpanHelper.logError(logger, "startGame: failed", attributes, ex);
                        LoggingSpanHelper.recordException(span, ex);
                    }
                })
            );
    }
}
