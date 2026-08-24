import { request } from "./http";

/**
 * Backend: StudyMaterialController -> /api/materials
 *
 * The list only ever contains what this caller is allowed - the server filters it
 * in SQL. There is deliberately no "all materials" call here; that is admin-only
 * and lives in adminApi.
 */
export const materialApi = {
  mine: () => request("/materials"),
};
