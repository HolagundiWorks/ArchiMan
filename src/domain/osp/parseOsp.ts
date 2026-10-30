import initSqlJs, { type Database, type SqlJsStatic } from 'sql.js';
import sqlWasmUrl from 'sql.js/dist/sql-wasm.wasm?url';
import {
  OSP_EXTENSION,
  OSP_FORMAT,
  OSP_VERSION,
  type OspAsset,
  type OspDrawing,
  type OspMarkup,
  type OspMeta,
  type OspProject,
  type OspTakeoffHeader,
  type OspTakeoffRow,
} from './types';

let sqlPromise: Promise<SqlJsStatic> | null = null;

function getSql(): Promise<SqlJsStatic> {
  if (!sqlPromise) {
    sqlPromise = initSqlJs({ locateFile: () => sqlWasmUrl });
  }
  return sqlPromise;
}

const SQLITE_MAGIC = 'SQLite format 3';

function isSqlite(bytes: Uint8Array): boolean {
  if (bytes.length < 16) return false;
  const head = String.fromCharCode(...bytes.subarray(0, 15));
  return head === SQLITE_MAGIC;
}

function fileExt(name: string): string {
  const i = name.lastIndexOf('.');
  return i >= 0 ? name.slice(i).toLowerCase() : '';
}

function parseJsonSafe(raw: string | null | undefined): unknown {
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}

function readMetaMap(db: Database): Map<string, string> {
  const map = new Map<string, string>();
  const res = db.exec('SELECT key, value FROM meta');
  if (!res[0]) return map;
  for (const row of res[0].values) {
    map.set(String(row[0]), String(row[1] ?? ''));
  }
  return map;
}

function buildMeta(map: Map<string, string>): OspMeta {
  const format = map.get('format') ?? '';
  const version = Number(map.get('version') ?? '0');
  if (format !== OSP_FORMAT) {
    throw new Error(
      `Unsupported project format "${format || '(missing)'}". Expected ${OSP_FORMAT} (${OSP_EXTENSION}).`
    );
  }
  if (version > OSP_VERSION) {
    throw new Error(`Unsupported ${OSP_FORMAT} version ${version} (reader supports ≤ ${OSP_VERSION}).`);
  }
  return {
    projectId: map.get('project_id') ?? '',
    name: map.get('name') ?? 'Untitled',
    created: map.get('created') ?? '',
    revision: Number(map.get('revision') ?? '0'),
    format,
    version,
  };
}

function blobToUint8(value: unknown): Uint8Array {
  if (value instanceof Uint8Array) return value;
  if (value instanceof ArrayBuffer) return new Uint8Array(value);
  if (Array.isArray(value)) return Uint8Array.from(value as number[]);
  return new Uint8Array();
}

function readDrawings(db: Database): OspDrawing[] {
  const res = db.exec(
    'SELECT name, ext, sha256, bytes, revision, created FROM drawing ORDER BY name'
  );
  if (!res[0]) return [];
  return res[0].values.map((row) => ({
    name: String(row[0]),
    ext: String(row[1]),
    sha256: String(row[2]),
    bytes: blobToUint8(row[3]),
    revision: Number(row[4] ?? 0),
    created: String(row[5] ?? ''),
  }));
}

function readTakeoffHeaders(db: Database): OspTakeoffHeader[] {
  const res = db.exec('SELECT slot, state, json FROM takeoff_header ORDER BY slot');
  if (!res[0]) return [];
  return res[0].values.map((row) => ({
    slot: Number(row[0]),
    state: String(row[1]),
    json: parseJsonSafe(String(row[2] ?? '')),
  }));
}

function readTakeoffRows(db: Database): OspTakeoffRow[] {
  const res = db.exec('SELECT slot, kind, n, json FROM takeoff_row ORDER BY slot, kind, n');
  if (!res[0]) return [];
  return res[0].values.map((row) => ({
    slot: Number(row[0]),
    kind: Number(row[1]),
    n: Number(row[2]),
    json: parseJsonSafe(String(row[3] ?? '')),
  }));
}

function readMarkups(db: Database): OspMarkup[] {
  const res = db.exec(
    'SELECT id, drawing, author, created, kind, json, text, resolved FROM markup ORDER BY created'
  );
  if (!res[0]) return [];
  return res[0].values.map((row) => ({
    id: String(row[0]),
    drawing: String(row[1]),
    author: String(row[2]),
    created: String(row[3]),
    kind: String(row[4]),
    json: parseJsonSafe(String(row[5] ?? '')),
    text: String(row[6] ?? ''),
    resolved: Number(row[7] ?? 0) !== 0,
  }));
}

function readAssets(db: Database): OspAsset[] {
  const res = db.exec(
    'SELECT id, slot, name, mime, sha256, length(bytes), bytes FROM asset ORDER BY name'
  );
  if (!res[0]) return [];
  return res[0].values.map((row) => ({
    id: String(row[0]),
    slot: row[1] == null ? null : Number(row[1]),
    name: String(row[2]),
    mime: String(row[3]),
    sha256: String(row[4]),
    byteLength: Number(row[5] ?? 0),
    bytes: blobToUint8(row[6]),
  }));
}

function readProjectDoc(db: Database): unknown | null {
  const res = db.exec('SELECT json FROM project_doc WHERE id = 1');
  if (!res[0]?.values[0]) return null;
  return parseJsonSafe(String(res[0].values[0][0] ?? ''));
}

function tableExists(db: Database, name: string): boolean {
  const res = db.exec(
    `SELECT 1 FROM sqlite_master WHERE type='table' AND name='${name.replace(/'/g, "''")}' LIMIT 1`
  );
  return Boolean(res[0]?.values?.length);
}

/** Parse an O-Draft / O-CoreOp `.osp` (SQLite `osas-project` v1) buffer. */
export async function parseOspBytes(
  bytes: Uint8Array,
  sourceName: string
): Promise<OspProject> {
  if (!isSqlite(bytes)) {
    throw new Error('Not a SQLite database — expected an O-Draft project (.osp).');
  }

  const SQL = await getSql();
  const db = new SQL.Database(bytes);
  try {
    if (!tableExists(db, 'meta')) {
      throw new Error('Missing meta table — not an osas-project file.');
    }
    const meta = buildMeta(readMetaMap(db));
    return {
      sourceName,
      meta,
      drawings: tableExists(db, 'drawing') ? readDrawings(db) : [],
      takeoffHeaders: tableExists(db, 'takeoff_header') ? readTakeoffHeaders(db) : [],
      takeoffRows: tableExists(db, 'takeoff_row') ? readTakeoffRows(db) : [],
      markups: tableExists(db, 'markup') ? readMarkups(db) : [],
      assets: tableExists(db, 'asset') ? readAssets(db) : [],
      projectDoc: tableExists(db, 'project_doc') ? readProjectDoc(db) : null,
    };
  } finally {
    db.close();
  }
}

/** Wrap a standalone DXF as a synthetic project (geometry download only). */
export function parseStandaloneDxf(bytes: Uint8Array, sourceName: string): OspProject {
  const base = sourceName.replace(/\.dxf$/i, '') || 'drawing';
  const now = new Date().toISOString();
  return {
    sourceName,
    meta: {
      projectId: '',
      name: base,
      created: now,
      revision: 0,
      format: 'standalone-dxf',
      version: 0,
    },
    drawings: [
      {
        name: sourceName.toLowerCase().endsWith('.dxf') ? sourceName : `${base}.dxf`,
        ext: 'dxf',
        sha256: '',
        bytes,
        revision: 0,
        created: now,
      },
    ],
    takeoffHeaders: [],
    takeoffRows: [],
    markups: [],
    assets: [],
    projectDoc: null,
  };
}

export type OpenDraftResult =
  | { ok: true; project: OspProject; kind: 'osp' | 'dxf' }
  | { ok: false; error: string };

/** Open a dropped / picked file as O-Draft project or standalone DXF. */
export async function openDraftFile(file: File): Promise<OpenDraftResult> {
  const ext = fileExt(file.name);
  const buf = new Uint8Array(await file.arrayBuffer());

  try {
    if (ext === '.osp') {
      const project = await parseOspBytes(buf, file.name);
      return { ok: true, project, kind: 'osp' };
    }
    if (ext === '.dxf') {
      return { ok: true, project: parseStandaloneDxf(buf, file.name), kind: 'dxf' };
    }
    if (ext === '.dwg') {
      return {
        ok: false,
        error:
          'DWG preview is not supported in the web reader. Save as .osp or .dxf from O-Draft, or embed the drawing inside an .osp project.',
      };
    }
    if (ext === '.odraft') {
      return {
        ok: false,
        error:
          'There is no .odraft file extension. O-Draft’s native project file is .osp (format osas-project). Drop a .osp or .dxf instead.',
      };
    }
    // Heuristic: SQLite magic → try as .osp
    if (isSqlite(buf)) {
      const project = await parseOspBytes(buf, file.name);
      return { ok: true, project, kind: 'osp' };
    }
    return {
      ok: false,
      error: `Unsupported file type "${ext || file.name}". Accepts .osp (O-Draft project) or .dxf.`,
    };
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    return { ok: false, error: message };
  }
}

export function formatByteSize(n: number): string {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / (1024 * 1024)).toFixed(2)} MB`;
}

export function downloadBytes(filename: string, bytes: Uint8Array, mime = 'application/octet-stream') {
  const blob = new Blob([bytes], { type: mime });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
