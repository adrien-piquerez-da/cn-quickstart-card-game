#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail
APP_PROVIDER_PARTY=$1
APP_USER_PARTY=$2
APP_PROVIDER_WALLET_ADMIN_TOKEN=$3

source /app/utils.sh

create_game_lobby() {
  local token=$1
  local appUserParty=$2
  local appProviderParty=$3
  local participantUserId=$4
  local participant=$5

  # Add a timestamp for a unique command ID to allow resubmission
  local time="$(date +%s%N)"

  echo "create_game_lobby $appUserParty $appProviderParty $participant" >&2

  curl_check "http://$participant/v2/commands/submit-and-wait" "$token" "application/json" \
    --data-raw '{
        "commands": [
          {
            "CreateCommand": {
              "templateId": "#card-game:CardGame:GameLobby",
              "createArguments": {
                "gamemaster": "'$appProviderParty'",
                "players": [],
                "invitees": ["'$appUserParty'"]
              }
            }
          }
        ],
        "workflowId": "create-foo",
        "applicationId": "'$participantUserId'",
        "commandId": "create-foo-'$time'",
        "deduplicationPeriod": {
          "Empty": {}
        },
        "actAs": [
          "'$appProviderParty'"
        ],
        "readAs": [
          "'$appProviderParty'"
        ],
        "submissionId": "create-foo",
        "disclosedContracts": [],
        "domainId": "",
        "packageIdSelectionPreference": []
    }'
}

# create_game_lobby "$APP_USER_WALLET_ADMIN_TOKEN" $APP_USER_PARTY $APP_PROVIDER_PARTY $AUTH_APP_USER_WALLET_ADMIN_USER_ID "canton:2${PARTICIPANT_JSON_API_PORT}"
create_game_lobby "$APP_PROVIDER_WALLET_ADMIN_TOKEN" $APP_USER_PARTY $APP_PROVIDER_PARTY $AUTH_APP_PROVIDER_WALLET_ADMIN_USER_ID "canton:3${PARTICIPANT_JSON_API_PORT}"
# create_game_lobby "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $APP_USER_PARTY $APP_PROVIDER_PARTY $AUTH_APP_PROVIDER_VALIDATOR_USER_ID "canton:3${PARTICIPANT_JSON_API_PORT}"
