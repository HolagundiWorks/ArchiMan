import React, { useCallback, useRef, useState } from 'react';
import {
  FileBox,
  Upload,
  Download,
  AlertCircle,
  CheckCircle2,
  Layers,
  Ruler,
  MessageSquare,
  Paperclip,
  X,
  FolderOpen,
} from 'lucide-react';
import { Button, StatusChip, Surface } from '../components/ui';
import {
  TAKEOFF_KIND_LABELS,
  openDraftFile,
  formatByteSize,
  downloadBytes,
  type OspProject,
  type OspTakeoffRow,
} from '../domain/osp';

type ReaderTab = 'overview' | 'takeoff' | 'drawings' | 'markups' | 'assets';

function asRecord(v: unknown): Record<string, unknown> {
  return v && typeof v === 'object' && !Array.isArray(v) ? (v as Record<string, unknown>) : {};
}

function str(v: unknown): string {
  if (v == null) return '—';
  if (typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean') return String(v);
  return JSON.stringify(v);
}

function groupRowsByKind(rows: OspTakeoffRow[]): Map<number, OspTakeoffRow[]> {
  const map = new Map<number, OspTakeoffRow[]>();
  for (const row of rows) {
    const list = map.get(row.kind) ?? [];
    list.push(row);
    map.set(row.kind, list);
  }
  return map;
}

function takeoffColumns(kind: number): { key: string; label: string }[] {
  switch (kind) {
    case 3:
      return [
        { key: 'ref', label: 'Ref' },
        { key: 'length_mm', label: 'Length mm' },
        { key: 'height_mm', label: 'Height mm' },
        { key: 'thickness_mm', label: 'Thick mm' },
        { key: 'level', label: 'Level' },
        { key: 'layer', label: 'Layer' },
      ];
    case 4:
      return [
        { key: 'ref', label: 'Ref' },
        { key: 'mark', label: 'Mark' },
        { key: 'kind', label: 'Kind' },
        { key: 'width_mm', label: 'W mm' },
        { key: 'height_mm', label: 'H mm' },
        { key: 'sill_mm', label: 'Sill mm' },
        { key: 'level', label: 'Level' },
      ];
    case 5:
      return [
        { key: 'ref', label: 'Ref' },
        { key: 'mark', label: 'Mark' },
        { key: 'width_mm', label: 'W mm' },
        { key: 'depth_mm', label: 'D mm' },
        { key: 'level', label: 'Level' },
      ];
    case 6:
      return [
        { key: 'ref', label: 'Ref' },
        { key: 'mark', label: 'Mark' },
        { key: 'opening_mm', label: 'Opening' },
        { key: 'width_mm', label: 'W mm' },
        { key: 'depth_mm', label: 'D mm' },
        { key: 'bearing_mm', label: 'Bearing' },
        { key: 'level', label: 'Level' },
      ];
    case 7:
      return [
        { key: 'ref', label: 'Ref' },
        { key: 'name', label: 'Name' },
        { key: 'area_m2', label: 'Area m²' },
        { key: 'length_mm', label: 'L mm' },
        { key: 'breadth_mm', label: 'B mm' },
        { key: 'level', label: 'Level' },
      ];
    case 8:
      return [
        { key: 'ref', label: 'Ref' },
        { key: 'area_m2', label: 'Area m²' },
        { key: 'thickness_mm', label: 'Thick mm' },
        { key: 'length_mm', label: 'L mm' },
        { key: 'breadth_mm', label: 'B mm' },
        { key: 'level', label: 'Level' },
      ];
    default:
      return [
        { key: 'ref', label: 'Ref' },
        { key: 'mark', label: 'Mark' },
        { key: 'name', label: 'Name' },
        { key: 'level', label: 'Level' },
      ];
  }
}

interface ODraftReaderViewProps {
  /** Compact embed (project drawings / Siteman) vs full portfolio page */
  compact?: boolean;
  onClose?: () => void;
  title?: string;
}

export const ODraftReaderView: React.FC<ODraftReaderViewProps> = ({
  compact = false,
  onClose,
  title = 'O-Draft project reader',
}) => {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [project, setProject] = useState<OspProject | null>(null);
  const [kind, setKind] = useState<'osp' | 'dxf' | null>(null);
  const [tab, setTab] = useState<ReaderTab>('overview');

  const ingest = useCallback(async (file: File) => {
    setLoading(true);
    setError(null);
    const result = await openDraftFile(file);
    setLoading(false);
    if (!result.ok) {
      setProject(null);
      setKind(null);
      setError(result.error);
      return;
    }
    setProject(result.project);
    setKind(result.kind);
    setTab('overview');
  }, []);

  const onDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setDragging(false);
      const file = e.dataTransfer.files?.[0];
      if (file) void ingest(file);
    },
    [ingest]
  );

  const onPick = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      e.target.value = '';
      if (file) void ingest(file);
    },
    [ingest]
  );

  const loadSample = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch('/examples/small.osp');
      if (!res.ok) throw new Error(`Sample fetch failed (${res.status})`);
      const buf = new Uint8Array(await res.arrayBuffer());
      const file = new File([buf], 'small.osp', { type: 'application/octet-stream' });
      await ingest(file);
    } catch (err) {
      setLoading(false);
      setError(err instanceof Error ? err.message : String(err));
    }
  }, [ingest]);

  const dropZone = (
    <div
      onDragOver={(e) => {
        e.preventDefault();
        setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={onDrop}
      className={`border border-dashed rounded-sm p-6 sm:p-8 text-center transition ${
        dragging
          ? 'border-primary bg-primary-container/40'
          : 'border-outline-variant bg-surface-container-lowest'
      }`}
    >
      <FileBox className="w-8 h-8 mx-auto text-primary mb-3" />
      <p className="type-title-md text-on-surface">
        Drop an O-Draft project here
      </p>
      <p className="type-body-sm mt-1 max-w-md mx-auto">
        Accepts <code className="font-mono">.osp</code> (native OSAS project — drawing + take-off)
        and standalone <code className="font-mono">.dxf</code>. There is no{' '}
        <code className="font-mono">.odraft</code> extension.
      </p>
      <div className="flex flex-wrap items-center justify-center gap-2 mt-4">
        <Button
          size="sm"
          leadingIcon={<Upload className="w-3.5 h-3.5" />}
          onClick={() => inputRef.current?.click()}
          disabled={loading}
        >
          Choose file
        </Button>
        <Button size="sm" variant="outlined" onClick={() => void loadSample()} disabled={loading}>
          Open sample .osp
        </Button>
      </div>
      <input
        ref={inputRef}
        type="file"
        accept=".osp,.dxf,.dwg,application/octet-stream"
        className="hidden"
        onChange={onPick}
      />
      {loading && <p className="type-body-sm mt-3 text-primary">Reading project…</p>}
    </div>
  );

  if (!project) {
    return (
      <div className={compact ? 'space-y-3' : 'space-y-5'}>
        {!compact && (
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="type-headline-sm">{title}</h2>
              <p className="type-body-sm mt-1">
                Field reader for O-Draft / O-CoreOp shared <code className="font-mono">.osp</code>{' '}
                files (format <code className="font-mono">osas-project</code> v1). Geometry canvas
                is deferred — meta, take-off, markups, and drawing export are available.
              </p>
            </div>
            {onClose && (
              <Button size="sm" variant="text" onClick={onClose} leadingIcon={<X className="w-3.5 h-3.5" />}>
                Close
              </Button>
            )}
          </div>
        )}
        {dropZone}
        {error && (
          <div className="flex items-start gap-2 rounded-sm border border-error/40 bg-error-container/40 px-3 py-2 text-[12px] text-on-error-container">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}
      </div>
    );
  }

  const headerJson = asRecord(project.takeoffHeaders[0]?.json);
  const projectInfo = asRecord(headerJson.project);
  const levels = Array.isArray(headerJson.levels) ? headerJson.levels : [];
  const openingSchedule = Array.isArray(headerJson.opening_schedule)
    ? headerJson.opening_schedule
    : [];
  const bbs = asRecord(project.projectDoc);
  const bbsProject = asRecord(bbs.project);
  const byKind = groupRowsByKind(project.takeoffRows);
  const recording = project.takeoffHeaders.some((h) => h.state === 'recording');

  const tabs: { id: ReaderTab; label: string; count?: number }[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'takeoff', label: 'Take-off', count: project.takeoffRows.length },
    { id: 'drawings', label: 'Drawings', count: project.drawings.length },
    { id: 'markups', label: 'Markups', count: project.markups.length },
    { id: 'assets', label: 'Assets', count: project.assets.length },
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2 mb-1">
            <StatusChip tone={kind === 'dxf' ? 'warning' : 'success'}>
              {kind === 'dxf' ? 'Standalone DXF' : 'osas-project'}
            </StatusChip>
            {recording && <StatusChip tone="warning">Take-off recording</StatusChip>}
            <span className="type-label-sm font-mono">rev {project.meta.revision}</span>
          </div>
          <h2 className="type-headline-sm truncate">{project.meta.name || project.sourceName}</h2>
          <p className="type-body-sm truncate font-mono">{project.sourceName}</p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <Button
            size="sm"
            variant="outlined"
            leadingIcon={<FolderOpen className="w-3.5 h-3.5" />}
            onClick={() => {
              setProject(null);
              setKind(null);
              setError(null);
            }}
          >
            Open another
          </Button>
          {onClose && (
            <Button size="sm" variant="text" onClick={onClose} leadingIcon={<X className="w-3.5 h-3.5" />}>
              Close
            </Button>
          )}
        </div>
      </div>

      {error && (
        <div className="flex items-start gap-2 rounded-sm border border-error/40 bg-error-container/40 px-3 py-2 text-[12px] text-on-error-container">
          <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      <div className="flex overflow-x-auto gap-1 border-b border-outline-variant">
        {tabs.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => setTab(t.id)}
            className={`px-3 py-2 text-[12px] font-semibold border-b-2 whitespace-nowrap transition ${
              tab === t.id
                ? 'border-primary text-primary'
                : 'border-transparent text-on-surface-variant hover:text-on-surface'
            }`}
          >
            {t.label}
            {t.count !== undefined ? (
              <span className="ml-1 font-mono text-[11px] text-on-surface-variant/70">({t.count})</span>
            ) : null}
          </button>
        ))}
      </div>

      {tab === 'overview' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          <Surface className="p-4 space-y-3 lg:col-span-1">
            <h3 className="type-label-md flex items-center gap-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-primary" /> Meta
            </h3>
            <dl className="space-y-2 type-body-md">
              <div className="flex justify-between gap-2">
                <dt className="text-on-surface-variant">Project ID</dt>
                <dd className="font-mono text-[11px] truncate max-w-[55%]">{project.meta.projectId || '—'}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-on-surface-variant">Format</dt>
                <dd className="font-mono text-[11px]">
                  {project.meta.format} v{project.meta.version}
                </dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-on-surface-variant">Created</dt>
                <dd className="font-mono text-[11px]">{project.meta.created || '—'}</dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-on-surface-variant">Revision</dt>
                <dd className="font-mono">{project.meta.revision}</dd>
              </div>
            </dl>
          </Surface>

          <Surface className="p-4 space-y-3 lg:col-span-2">
            <h3 className="type-label-md flex items-center gap-1.5">
              <Layers className="w-3.5 h-3.5 text-primary" /> Project summary
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 type-body-md">
              <div>
                <div className="type-body-sm">Client</div>
                <div className="font-semibold">
                  {str(projectInfo.client_name || bbsProject.client_name)}
                </div>
              </div>
              <div>
                <div className="type-body-sm">Location</div>
                <div className="font-semibold">
                  {str(projectInfo.location || bbsProject.location)}
                </div>
              </div>
              <div>
                <div className="type-body-sm">Units</div>
                <div className="font-semibold font-mono">{str(headerJson.units || 'mm')}</div>
              </div>
              <div>
                <div className="type-body-sm">Plot use</div>
                <div className="font-semibold">{str(projectInfo.plot_use)}</div>
              </div>
              <div>
                <div className="type-body-sm">Site area</div>
                <div className="font-semibold font-mono">
                  {projectInfo.site_area_m2 != null ? `${str(projectInfo.site_area_m2)} m²` : '—'}
                </div>
              </div>
              <div>
                <div className="type-body-sm">Take-off slots</div>
                <div className="font-semibold font-mono">{project.takeoffHeaders.length}</div>
              </div>
            </div>

            {levels.length > 0 && (
              <div className="pt-2 border-t border-outline-variant/60">
                <div className="type-label-sm mb-2">Levels</div>
                <div className="flex flex-wrap gap-2">
                  {levels.map((lv, i) => {
                    const L = asRecord(lv);
                    return (
                      <span
                        key={i}
                        className="inline-flex items-center gap-1.5 px-2 py-1 rounded-sm bg-surface text-[11px] border border-outline-variant"
                      >
                        <span className="font-semibold">{str(L.name)}</span>
                        <span className="font-mono text-on-surface-variant">
                          {str(L.height_mm)} mm
                        </span>
                      </span>
                    );
                  })}
                </div>
              </div>
            )}

            {openingSchedule.length > 0 && (
              <div className="pt-2 border-t border-outline-variant/60">
                <div className="type-label-sm mb-2">Opening schedule ({openingSchedule.length})</div>
                <div className="overflow-x-auto max-h-40">
                  <table className="w-full text-left text-[11px]">
                    <thead className="text-on-surface-variant">
                      <tr>
                        <th className="py-1 pr-2">Kind</th>
                        <th className="py-1 pr-2">Mark</th>
                        <th className="py-1 pr-2">W</th>
                        <th className="py-1 pr-2">H</th>
                        <th className="py-1">Nos</th>
                      </tr>
                    </thead>
                    <tbody>
                      {openingSchedule.map((o, i) => {
                        const O = asRecord(o);
                        return (
                          <tr key={i} className="border-t border-outline-variant/40">
                            <td className="py-1 pr-2">{str(O.kind)}</td>
                            <td className="py-1 pr-2 font-mono">{str(O.mark)}</td>
                            <td className="py-1 pr-2 font-mono">{str(O.width_mm)}</td>
                            <td className="py-1 pr-2 font-mono">{str(O.height_mm)}</td>
                            <td className="py-1 font-mono">{str(O.nos)}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </Surface>
        </div>
      )}

      {tab === 'takeoff' && (
        <div className="space-y-4">
          {project.takeoffHeaders.length === 0 ? (
            <Surface className="p-6 text-center type-body-sm">
              No take-off headers in this file. Record take-off in O-Draft or open a project that
              includes complete slots.
            </Surface>
          ) : (
            project.takeoffHeaders.map((h) => (
              <Surface key={h.slot} className="p-4 space-y-3">
                <div className="flex items-center justify-between gap-2">
                  <h3 className="type-title-md flex items-center gap-2">
                    <Ruler className="w-4 h-4 text-primary" />
                    Slot {h.slot}
                  </h3>
                  <StatusChip
                    tone={h.state === 'complete' ? 'success' : h.state === 'recording' ? 'warning' : 'neutral'}
                  >
                    {h.state}
                  </StatusChip>
                </div>
                {h.state === 'recording' && (
                  <p className="type-body-sm text-warning">
                    Take-off is still recording — O-CoreOp refuses import until state is complete.
                  </p>
                )}
                {[...byKind.entries()]
                  .filter(([_, rows]) => rows.some((r) => r.slot === h.slot))
                  .map(([kindId, allRows]) => {
                    const rows = allRows.filter((r) => r.slot === h.slot);
                    const cols = takeoffColumns(kindId);
                    return (
                      <div key={kindId} className="space-y-1.5">
                        <div className="type-label-sm">
                          {TAKEOFF_KIND_LABELS[kindId] ?? `Kind ${kindId}`} ({rows.length})
                        </div>
                        <div className="overflow-x-auto border border-outline-variant/60 rounded-sm">
                          <table className="w-full text-left text-[11px]">
                            <thead className="bg-surface text-on-surface-variant">
                              <tr>
                                <th className="px-2 py-1.5 font-mono">#</th>
                                {cols.map((c) => (
                                  <th key={c.key} className="px-2 py-1.5">
                                    {c.label}
                                  </th>
                                ))}
                              </tr>
                            </thead>
                            <tbody>
                              {rows.map((row) => {
                                const j = asRecord(row.json);
                                return (
                                  <tr key={`${row.kind}-${row.n}`} className="border-t border-outline-variant/40">
                                    <td className="px-2 py-1 font-mono text-on-surface-variant">{row.n}</td>
                                    {cols.map((c) => (
                                      <td key={c.key} className="px-2 py-1 font-mono">
                                        {str(j[c.key])}
                                      </td>
                                    ))}
                                  </tr>
                                );
                              })}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    );
                  })}
              </Surface>
            ))
          )}
        </div>
      )}

      {tab === 'drawings' && (
        <div className="space-y-2">
          {project.drawings.length === 0 ? (
            <Surface className="p-6 text-center type-body-sm">No drawings embedded.</Surface>
          ) : (
            project.drawings.map((d) => (
              <Surface
                key={d.name}
                className="p-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div className="min-w-0">
                  <div className="type-title-sm truncate font-mono">{d.name}</div>
                  <div className="type-body-sm">
                    .{d.ext} · {formatByteSize(d.bytes.byteLength)} · rev {d.revision}
                    {d.sha256 ? (
                      <span className="ml-2 font-mono text-[10px] truncate inline-block max-w-[12rem] align-bottom">
                        {d.sha256.slice(0, 12)}…
                      </span>
                    ) : null}
                  </div>
                  <p className="type-body-sm mt-1">
                    Canvas preview deferred — download the embedded DXF/DWG for O-Draft or a CAD
                    viewer.
                  </p>
                </div>
                <Button
                  size="sm"
                  variant="tonal"
                  leadingIcon={<Download className="w-3.5 h-3.5" />}
                  onClick={() =>
                    downloadBytes(
                      d.name.toLowerCase().endsWith(`.${d.ext}`) ? d.name : `${d.name}.${d.ext}`,
                      d.bytes,
                      d.ext === 'dxf' ? 'image/vnd.dxf' : 'application/octet-stream'
                    )
                  }
                >
                  Download .{d.ext}
                </Button>
              </Surface>
            ))
          )}
        </div>
      )}

      {tab === 'markups' && (
        <div className="space-y-2">
          {project.markups.length === 0 ? (
            <Surface className="p-6 text-center type-body-sm">No markups in this project.</Surface>
          ) : (
            project.markups.map((m) => (
              <Surface key={m.id} className="p-3 space-y-1">
                <div className="flex items-center justify-between gap-2">
                  <div className="type-title-sm flex items-center gap-1.5">
                    <MessageSquare className="w-3.5 h-3.5 text-primary" />
                    {m.kind}
                  </div>
                  <StatusChip tone={m.resolved ? 'success' : 'warning'}>
                    {m.resolved ? 'Resolved' : 'Open'}
                  </StatusChip>
                </div>
                <p className="type-body-md">{m.text || '(no text)'}</p>
                <p className="type-body-sm font-mono">
                  {m.author} · {m.drawing} · {m.created}
                </p>
              </Surface>
            ))
          )}
        </div>
      )}

      {tab === 'assets' && (
        <div className="space-y-2">
          {project.assets.length === 0 ? (
            <Surface className="p-6 text-center type-body-sm">No assets (e.g. annotated PDF).</Surface>
          ) : (
            project.assets.map((a) => (
              <Surface
                key={a.id}
                className="p-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
              >
                <div className="min-w-0">
                  <div className="type-title-sm flex items-center gap-1.5 truncate">
                    <Paperclip className="w-3.5 h-3.5 text-primary shrink-0" />
                    {a.name}
                  </div>
                  <div className="type-body-sm">
                    {a.mime} · {formatByteSize(a.byteLength)}
                    {a.slot != null ? ` · slot ${a.slot}` : ''}
                  </div>
                </div>
                <Button
                  size="sm"
                  variant="tonal"
                  leadingIcon={<Download className="w-3.5 h-3.5" />}
                  onClick={() => downloadBytes(a.name, a.bytes, a.mime)}
                >
                  Download
                </Button>
              </Surface>
            ))
          )}
        </div>
      )}
    </div>
  );
};
