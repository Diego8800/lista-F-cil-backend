/**
 * Migrações: cada módulo exporta um nome e uma lista de statements SQL
 * executados sequencialmente (um statement por elemento — sem split de texto).
 */
import type { migration001 } from "./migrations/001_init.js";
import type { migration002 } from "./migrations/002_rls.js";

export interface Migration {
  name: string;
  statements: string[];
}

import { migration001 as m1 } from "./migrations/001_init.js";
import { migration002 as m2 } from "./migrations/002_rls.js";

export const migrations: Migration[] = [m1, m2];
