#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/simulate-user-input.sh
/app/create-game-lobby.sh $APP_PROVIDER_PARTY $APP_USER_PARTY "$APP_PROVIDER_WALLET_ADMIN_TOKEN"