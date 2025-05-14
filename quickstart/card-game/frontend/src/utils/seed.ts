// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

/**
 * Generate a random int32 seed
 * TODO: it could be an int64 as well
 */
export function generateSeed(): number {
  return Math.floor(Math.random() * 0x100000000);
}
