/** Shared OSAS project file (.osp) — format `osas-project` v1 (O-Draft / O-CoreOp). */

export const OSP_FORMAT = 'osas-project' as const;
export const OSP_VERSION = 1;
export const OSP_EXTENSION = '.osp';

export type OspMeta = {
  projectId: string;
  name: string;
  created: string;
  revision: number;
  format: string;
  version: number;
};

export type OspDrawing = {
  name: string;
  ext: string;
  sha256: string;
  bytes: Uint8Array;
  revision: number;
  created: string;
};

export type OspTakeoffHeader = {
  slot: number;
  state: string;
  json: unknown;
};

export type OspTakeoffRow = {
  slot: number;
  kind: number;
  n: number;
  json: unknown;
};

export type OspMarkup = {
  id: string;
  drawing: string;
  author: string;
  created: string;
  kind: string;
  json: unknown;
  text: string;
  resolved: boolean;
};

export type OspAsset = {
  id: string;
  slot: number | null;
  name: string;
  mime: string;
  sha256: string;
  byteLength: number;
  bytes: Uint8Array;
};

export type OspProjectDoc = unknown | null;

export type OspProject = {
  sourceName: string;
  meta: OspMeta;
  drawings: OspDrawing[];
  takeoffHeaders: OspTakeoffHeader[];
  takeoffRows: OspTakeoffRow[];
  markups: OspMarkup[];
  assets: OspAsset[];
  projectDoc: OspProjectDoc;
};

export const TAKEOFF_KIND_LABELS: Record<number, string> = {
  3: 'Wall',
  4: 'Opening',
  5: 'Column',
  6: 'Lintel',
  7: 'Room',
  8: 'Slab',
  10: 'Electrical board',
  11: 'Electrical circuit',
};
