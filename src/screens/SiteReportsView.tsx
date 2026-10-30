import React, { useState } from 'react';
import { store } from '../data/storage';
import { ProjectEntity, SiteIssueEntity, DailySiteReportEntity, SiteInspectionEntity, MeetingMinutesEntity } from '../types';
import { SiteIssueWorkflow } from '../domain/SiteIssueWorkflow';
import { ExportHelper } from '../domain/WorkCatalog';
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  Plus,
  FileText,
  Download,
  Calendar,
  ShieldAlert,
  Camera,
  ClipboardList,
  Check,
  Search,
  Users,
  MapPin,
} from 'lucide-react';

interface SiteReportsViewProps {
  project: ProjectEntity;
}

export const SiteReportsView: React.FC<SiteReportsViewProps> = ({ project }) => {
  const [activeTab, setActiveTab] = useState<'SNAGS' | 'DAILY' | 'INSPECTIONS' | 'MEETINGS'>('SNAGS');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals
  const [showIssueModal, setShowIssueModal] = useState(false);
  const [showLogModal, setShowLogModal] = useState(false);
  const [showInspectionModal, setShowInspectionModal] = useState(false);
  const [showMeetingModal, setShowMeetingModal] = useState(false);

  // Snag Closure Modal
  const [showClosureModal, setShowClosureModal] = useState<SiteIssueEntity | null>(null);
  const [verificationNote, setVerificationNote] = useState('');
  const [afterEvidence, setAfterEvidence] = useState('');

  // Collections
  const issues = store.siteIssues.filter((i) => i.projectId === project.id);
  const dailyLogs = store.dailyReports.filter((r) => r.projectId === project.id);
  const inspections = store.siteInspections.filter((ins) => ins.projectId === project.id);
  const meetings = store.meetingMinutes.filter((m) => m.projectId === project.id);

  // Forms
  const [issueForm, setIssueForm] = useState({
    type: 'SNAG' as 'SNAG' | 'NCR' | 'SAFETY',
    title: '',
    location: '',
    description: '',
    severity: 'NORMAL' as const,
    assignedTo: '',
    correctiveAction: '',
    evidenceUri: '',
  });

  const [logForm, setLogForm] = useState({
    weather: 'Clear, 30°C',
    manpower: '',
    workCompleted: '',
    materialsReceived: '',
    delaysOrConstraints: '',
    safetyObservations: '',
    nextDayPlan: '',
    preparedBy: '',
  });

  const [inspectionForm, setInspectionForm] = useState({
    location: '',
    inspector: store.companyProfile.principalName || 'Principal Architect',
    observation: '',
    severity: 'NORMAL' as const,
    correctiveAction: '',
  });

  const [meetingForm, setMeetingForm] = useState({
    title: '',
    location: 'Project Site Office',
    attendees: '',
    discussion: '',
    decisions: '',
    actionItems: '',
  });

  // Handlers
  const handleCreateIssue = (e: React.FormEvent) => {
    e.preventDefault();
    if (!issueForm.title.trim() || !issueForm.description.trim()) return;

    const count = issues.length + 1;
    const ref = `${issueForm.type === 'NCR' ? 'NCR' : issueForm.type === 'SAFETY' ? 'SAF' : 'SNG'}-${project.projectCode}-${count.toString().padStart(3, '0')}`;

    store.addSiteIssue({
      projectId: project.id,
      referenceNumber: ref,
      type: issueForm.type,
      title: issueForm.title,
      location: issueForm.location,
      description: issueForm.description,
      severity: issueForm.severity,
      assignedTo: issueForm.assignedTo,
      correctiveAction: issueForm.correctiveAction,
      dueAt: Date.now() + 3 * 86400000,
      status: 'OPEN',
      evidenceUri: issueForm.evidenceUri || 'Discovered during architect site inspection',
      afterEvidenceUri: null,
      verificationNote: '',
      closedAt: null,
    });

    setShowIssueModal(false);
    setIssueForm({
      type: 'SNAG',
      title: '',
      location: '',
      description: '',
      severity: 'NORMAL',
      assignedTo: '',
      correctiveAction: '',
      evidenceUri: '',
    });
  };

  const handleCreateLog = (e: React.FormEvent) => {
    e.preventDefault();
    if (!logForm.workCompleted.trim()) return;

    store.addDailyReport({
      projectId: project.id,
      reportDate: Date.now(),
      weather: logForm.weather,
      manpower: logForm.manpower,
      workCompleted: logForm.workCompleted,
      materialsReceived: logForm.materialsReceived,
      delaysOrConstraints: logForm.delaysOrConstraints,
      safetyObservations: logForm.safetyObservations,
      nextDayPlan: logForm.nextDayPlan,
      preparedBy: logForm.preparedBy || 'Site Engineer',
      photoUri: null,
    });

    setShowLogModal(false);
    setLogForm({
      weather: 'Clear, 30°C',
      manpower: '',
      workCompleted: '',
      materialsReceived: '',
      delaysOrConstraints: '',
      safetyObservations: '',
      nextDayPlan: '',
      preparedBy: '',
    });
  };

  const handleCreateInspection = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inspectionForm.observation.trim() || !inspectionForm.location.trim()) return;

    store.addSiteInspection({
      projectId: project.id,
      inspectionAt: Date.now(),
      location: inspectionForm.location,
      inspector: inspectionForm.inspector,
      observation: inspectionForm.observation,
      severity: inspectionForm.severity,
      correctiveAction: inspectionForm.correctiveAction,
      dueAt: Date.now() + 2 * 86400000,
      status: 'OPEN',
      photoUri: null,
    });

    setShowInspectionModal(false);
    setInspectionForm({
      location: '',
      inspector: store.companyProfile.principalName || 'Principal Architect',
      observation: '',
      severity: 'NORMAL',
      correctiveAction: '',
    });
  };

  const handleCreateMeeting = (e: React.FormEvent) => {
    e.preventDefault();
    if (!meetingForm.title.trim()) return;

    store.addMeetingMinutes({
      projectId: project.id,
      title: meetingForm.title,
      meetingAt: Date.now(),
      location: meetingForm.location,
      attendees: meetingForm.attendees,
      discussion: meetingForm.discussion,
      decisions: meetingForm.decisions,
      actionItems: meetingForm.actionItems,
    });

    setShowMeetingModal(false);
    setMeetingForm({
      title: '',
      location: 'Project Site Office',
      attendees: '',
      discussion: '',
      decisions: '',
      actionItems: '',
    });
  };

  const handleConfirmClosure = (issue: SiteIssueEntity) => {
    if (!verificationNote.trim()) return;
    store.updateSiteIssue(issue.id, {
      status: 'CLOSED',
      verificationNote,
      afterEvidenceUri: afterEvidence || 'Rectification verified on site by Architect',
      closedAt: Date.now(),
    });
    setShowClosureModal(null);
    setVerificationNote('');
    setAfterEvidence('');
  };

  const handleExportIssues = () => {
    let csv = `SITE ISSUES & SNAG / NCR REGISTER - ${project.name} (${project.projectCode})\n`;
    csv += `Ref Number,Type,Title,Severity,Status,Assigned To,Location,Corrective Action,Verification Note,Closed At\n`;
    issues.forEach((i) => {
      csv += `"${i.referenceNumber}","${i.type}","${i.title}","${i.severity}","${i.status}","${i.assignedTo}","${i.location}","${i.correctiveAction}","${i.verificationNote}","${i.closedAt ? new Date(i.closedAt).toLocaleDateString() : '-'}"\n`;
    });
    ExportHelper.downloadCSV(`Snag_NCR_Register_${project.projectCode}.csv`, csv);
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-surface-container-lowest p-5 rounded-lg border border-outline-variant elevation-1">
        <div>
          <div className="flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-primary" />
            <h2 className="text-xl font-bold text-on-surface">Site Quality, Inspections & Reports</h2>
          </div>
          <p className="text-xs text-on-surface-variant mt-1">
            Complete quality records: snag/NCR defect tracking, daily site logs, inspection notes, and meeting minutes.
          </p>
        </div>

        <div className="flex items-center gap-2">
          {activeTab === 'SNAGS' && (
            <>
              <button
                type="button"
                onClick={handleExportIssues}
                className="px-3 py-2 bg-surface-container-low hover:bg-surface-container-high text-on-surface rounded-lg text-xs font-semibold flex items-center gap-1.5"
              >
                <Download className="w-4 h-4" /> Export CSV
              </button>
              <button
                type="button"
                onClick={() => setShowIssueModal(true)}
                className="px-3.5 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg text-xs font-semibold flex items-center gap-1.5 elevation-1"
              >
                <Plus className="w-4 h-4" /> Log Snag / NCR
              </button>
            </>
          )}
          {activeTab === 'DAILY' && (
            <button
              type="button"
              onClick={() => setShowLogModal(true)}
              className="px-3.5 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg text-xs font-semibold flex items-center gap-1.5 elevation-1"
            >
              <Plus className="w-4 h-4" /> Add Daily Log
            </button>
          )}
          {activeTab === 'INSPECTIONS' && (
            <button
              type="button"
              onClick={() => setShowInspectionModal(true)}
              className="px-3.5 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg text-xs font-semibold flex items-center gap-1.5 elevation-1"
            >
              <Plus className="w-4 h-4" /> Log Inspection
            </button>
          )}
          {activeTab === 'MEETINGS' && (
            <button
              type="button"
              onClick={() => setShowMeetingModal(true)}
              className="px-3.5 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg text-xs font-semibold flex items-center gap-1.5 elevation-1"
            >
              <Plus className="w-4 h-4" /> Record Meeting Minutes
            </button>
          )}
        </div>
      </div>

      {/* Tab Bar Navigation */}
      <div className="flex border-b border-outline-variant overflow-x-auto gap-4">
        {[
          { id: 'SNAGS', label: 'Snags & NCRs', count: issues.length },
          { id: 'DAILY', label: 'Daily Site Logs', count: dailyLogs.length },
          { id: 'INSPECTIONS', label: 'Inspections', count: inspections.length },
          { id: 'MEETINGS', label: 'Meeting Minutes', count: meetings.length },
        ].map((tab) => {
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id as any)}
              className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition whitespace-nowrap ${
                isActive
                  ? 'border-primary text-primary'
                  : 'border-transparent text-on-surface-variant hover:text-on-surface'
              }`}
            >
              <span>{tab.label}</span>
              <span className="text-[11px] font-mono font-medium text-on-surface-variant/70">
                ({tab.count})
              </span>
            </button>
          );
        })}
      </div>

      {/* TAB 1: SNAGS & NCRS */}
      {activeTab === 'SNAGS' && (
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {issues.length === 0 ? (
              <div className="md:col-span-2 bg-surface-container-lowest p-12 text-center rounded-md border border-dashed border-outline">
                <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto mb-2" />
                <h3 className="text-sm font-semibold text-on-surface">No Open Snags or NCRs</h3>
                <p className="text-xs text-on-surface-variant mt-1">
                  Quality standards are meeting specifications. Log an observation whenever defects are identified on site.
                </p>
              </div>
            ) : (
              issues.map((issue) => {
                const nextStatuses = SiteIssueWorkflow.allowedNextStatuses(issue.status);

                return (
                  <div
                    key={issue.id}
                    className="bg-surface-container-lowest rounded-md border border-outline-variant p-5 elevation-1 space-y-3.5 hover:border-outline transition flex flex-col justify-between"
                  >
                    <div className="space-y-2.5">
                      <div className="flex items-center justify-between border-b border-outline-variant/60 pb-2.5">
                        <div className="flex items-center gap-2 text-xs">
                          <span className="font-mono text-xs font-semibold text-on-surface">
                            {issue.referenceNumber}
                          </span>
                          <span className="font-bold text-on-surface">{issue.type}</span>
                          <span className="text-on-surface-variant/60">·</span>
                          <span className={`font-medium ${issue.severity === 'CRITICAL' ? 'text-rose-600 font-bold' : issue.severity === 'ATTENTION' ? 'text-warning font-semibold' : 'text-on-surface-variant'}`}>
                            {issue.severity}
                          </span>
                        </div>

                        <div className="flex items-center gap-1.5 text-xs font-mono font-medium text-on-surface">
                          <span className={`w-1.5 h-1.5 rounded-full ${issue.status === 'CLOSED' ? 'bg-success' : issue.status === 'READY_FOR_VERIFICATION' ? 'bg-primary' : issue.status === 'IN_PROGRESS' ? 'bg-warning' : 'bg-outline'}`}></span>
                          <span>{issue.status}</span>
                        </div>
                      </div>

                      <div>
                        <h3 className="text-base font-bold text-on-surface">{issue.title}</h3>
                        <div className="text-xs text-on-surface-variant mt-0.5 flex flex-wrap gap-x-3">
                          <span>Location: <strong className="text-on-surface">{issue.location || 'General'}</strong></span>
                          <span>Assigned: <strong className="text-on-surface">{issue.assignedTo || 'Civil Contractor'}</strong></span>
                        </div>
                        <p className="text-xs text-on-surface mt-2 bg-surface p-2.5 rounded-lg border border-outline-variant/60 leading-relaxed">
                          {issue.description}
                        </p>
                      </div>

                      <div className="text-xs space-y-2">
                        <div className="p-2.5 bg-warning-container/60 rounded border border-amber-100/80">
                          <span className="font-semibold text-amber-900 block text-[11px]">Before Discovery Evidence:</span>
                          <span className="text-on-surface">{issue.evidenceUri || 'Recorded during site walk'}</span>
                        </div>

                        {issue.status === 'CLOSED' ? (
                          <div className="p-2.5 bg-success-container/60 rounded border border-emerald-100">
                            <span className="font-semibold text-emerald-900 block text-[11px]">Verified Signoff (After):</span>
                            <span className="text-on-surface block">{issue.afterEvidenceUri || 'Verified by Architect'}</span>
                            <span className="text-emerald-950 font-medium block mt-1 text-[11px]">
                              Note: {issue.verificationNote}
                            </span>
                          </div>
                        ) : (
                          <div className="p-2.5 bg-surface rounded border border-outline-variant">
                            <span className="font-semibold text-on-surface block text-[11px]">Corrective Action:</span>
                            <span className="text-on-surface-variant">{issue.correctiveAction || 'Pending contractor proposal'}</span>
                          </div>
                        )}
                      </div>
                    </div>

                    {nextStatuses.length > 0 && (
                      <div className="pt-2 border-t border-outline-variant/60 flex items-center justify-end gap-2">
                        {nextStatuses.map((next) => (
                          <button
                            key={next}
                            type="button"
                            onClick={() => {
                              if (next === 'CLOSED') {
                                setShowClosureModal(issue);
                              } else {
                                store.updateSiteIssue(issue.id, { status: next as any });
                              }
                            }}
                            className="px-3 py-1.5 bg-primary hover:bg-primary text-on-primary rounded-lg text-xs font-semibold"
                          >
                            Move to {next.replace(/_/g, ' ')}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* TAB 2: DAILY SITE LOGS */}
      {activeTab === 'DAILY' && (
        <div className="space-y-4">
          {dailyLogs.length === 0 ? (
            <div className="bg-surface-container-lowest p-12 text-center rounded-md border border-dashed border-outline">
              <ClipboardList className="w-10 h-10 text-on-surface-variant/70 mx-auto mb-2" />
              <h3 className="text-sm font-semibold text-on-surface">No Daily Logs Recorded</h3>
              <button
                type="button"
                onClick={() => setShowLogModal(true)}
                className="mt-4 px-4 py-2 bg-primary text-on-primary rounded-lg text-xs font-semibold"
              >
                Add Daily Log
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {dailyLogs.map((log) => (
                <div key={log.id} className="bg-surface-container-lowest rounded-md p-5 border border-outline-variant elevation-1 space-y-3">
                  <div className="flex items-center justify-between border-b border-outline-variant/60 pb-2">
                    <div className="flex items-center gap-1.5 text-xs font-bold text-on-surface">
                      <Calendar className="w-4 h-4 text-primary" />
                      <span>{new Date(log.reportDate).toLocaleDateString(undefined, { dateStyle: 'medium' })}</span>
                    </div>
                    <span className="text-xs text-on-surface-variant font-medium">{log.weather}</span>
                  </div>

                  <div className="text-xs">
                    <span className="text-on-surface-variant/70 font-medium block text-[11px]">Work Completed:</span>
                    <p className="text-on-surface leading-relaxed mt-0.5">{log.workCompleted}</p>
                  </div>

                  {log.manpower && (
                    <div className="text-xs">
                      <span className="text-on-surface-variant/70 font-medium block text-[11px]">Manpower:</span>
                      <p className="text-on-surface mt-0.5">{log.manpower}</p>
                    </div>
                  )}

                  {log.materialsReceived && (
                    <div className="p-2.5 bg-warning-container/60 rounded border border-amber-100 text-xs">
                      <span className="font-semibold text-amber-800 block text-[11px]">Materials Received:</span>
                      <span className="text-amber-900">{log.materialsReceived}</span>
                    </div>
                  )}

                  <div className="text-[11px] text-on-surface-variant/70 pt-2 border-t border-outline-variant/60 flex justify-between">
                    <span>Prepared by: {log.preparedBy}</span>
                    <span>{new Date(log.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 3: INSPECTIONS */}
      {activeTab === 'INSPECTIONS' && (
        <div className="space-y-4">
          {inspections.length === 0 ? (
            <div className="bg-surface-container-lowest p-12 text-center rounded-md border border-dashed border-outline">
              <ShieldAlert className="w-10 h-10 text-on-surface-variant/70 mx-auto mb-2" />
              <h3 className="text-sm font-semibold text-on-surface">No Inspections Recorded</h3>
              <button
                type="button"
                onClick={() => setShowInspectionModal(true)}
                className="mt-4 px-4 py-2 bg-primary text-on-primary rounded-lg text-xs font-semibold"
              >
                Log Inspection
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {inspections.map((ins) => (
                <div key={ins.id} className="bg-surface-container-lowest rounded-md p-5 border border-outline-variant elevation-1 space-y-3">
                  <div className="flex items-center justify-between border-b border-outline-variant/60 pb-2">
                    <span className="text-xs font-bold text-on-surface flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-primary" />
                      {ins.location}
                    </span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        ins.status === 'RESOLVED' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                      }`}
                    >
                      {ins.status}
                    </span>
                  </div>

                  <div className="text-xs space-y-1">
                    <span className="text-on-surface-variant/70 text-[11px] block">Observation:</span>
                    <p className="text-on-surface leading-relaxed">{ins.observation}</p>
                  </div>

                  {ins.correctiveAction && (
                    <div className="p-2.5 bg-surface rounded border border-outline-variant/60 text-xs">
                      <span className="font-semibold text-on-surface block text-[11px]">Corrective Action:</span>
                      <span className="text-on-surface-variant">{ins.correctiveAction}</span>
                    </div>
                  )}

                  <div className="pt-2 border-t border-outline-variant/60 flex items-center justify-between text-[11px] text-on-surface-variant/70">
                    <span>Inspector: {ins.inspector}</span>
                    {ins.status === 'OPEN' && (
                      <button
                        type="button"
                        onClick={() => store.updateSiteInspection(ins.id, { status: 'RESOLVED' })}
                        className="px-2.5 py-1 bg-success text-on-success rounded text-xs font-semibold"
                      >
                        Mark Resolved
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 4: MEETING MINUTES */}
      {activeTab === 'MEETINGS' && (
        <div className="space-y-4">
          {meetings.length === 0 ? (
            <div className="bg-surface-container-lowest p-12 text-center rounded-md border border-dashed border-outline">
              <Users className="w-10 h-10 text-on-surface-variant/70 mx-auto mb-2" />
              <h3 className="text-sm font-semibold text-on-surface">No Meeting Minutes Recorded</h3>
              <button
                type="button"
                onClick={() => setShowMeetingModal(true)}
                className="mt-4 px-4 py-2 bg-primary text-on-primary rounded-lg text-xs font-semibold"
              >
                Record Meeting Minutes
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {meetings.map((meeting) => (
                <div key={meeting.id} className="bg-surface-container-lowest rounded-md border border-outline-variant p-5 elevation-1 space-y-3">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1 border-b border-outline-variant/60 pb-2.5">
                    <h3 className="text-base font-bold text-on-surface">{meeting.title}</h3>
                    <div className="text-xs text-on-surface-variant flex items-center gap-2">
                      <Calendar className="w-3.5 h-3.5" />
                      <span>{new Date(meeting.meetingAt).toLocaleDateString(undefined, { dateStyle: 'medium' })}</span>
                    </div>
                  </div>

                  <div className="text-xs text-on-surface-variant">
                    <strong>Attendees: </strong> {meeting.attendees}
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
                    <div className="bg-surface p-3 rounded-lg border border-outline-variant/60">
                      <strong className="text-on-surface block mb-1">Discussion:</strong>
                      <p className="text-on-surface-variant leading-relaxed">{meeting.discussion}</p>
                    </div>
                    <div className="bg-primary-container/50 p-3 rounded-lg border border-primary-container">
                      <strong className="text-on-primary-container block mb-1">Agreed Decisions:</strong>
                      <p className="text-on-surface leading-relaxed">{meeting.decisions}</p>
                    </div>
                    <div className="bg-success-container/50 p-3 rounded-lg border border-emerald-100">
                      <strong className="text-emerald-900 block mb-1">Action Items:</strong>
                      <p className="text-on-surface leading-relaxed">{meeting.actionItems}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Modal: Add Snag/NCR */}
      {showIssueModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/50 backdrop-blur-xs p-4 overflow-y-auto">
          <div className="bg-surface-container-lowest rounded-lg max-w-lg w-full p-6 shadow-xl space-y-4 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-outline-variant/60">
              <h3 className="text-base font-bold text-on-surface">Log Site Snag / NCR</h3>
              <button
                type="button"
                onClick={() => setShowIssueModal(false)}
                className="text-on-surface-variant/70 hover:text-on-surface-variant text-sm font-semibold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateIssue} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-on-surface mb-1">Issue Type</label>
                  <select
                    value={issueForm.type}
                    onChange={(e) => setIssueForm({ ...issueForm, type: e.target.value as any })}
                    className="w-full px-3 py-2 border border-outline rounded-lg bg-surface-container-lowest"
                  >
                    <option value="SNAG">Snag (Finish / Workmanship)</option>
                    <option value="NCR">NCR (Non-Conformance Report)</option>
                    <option value="SAFETY">Safety Hazard Observation</option>
                  </select>
                </div>
                <div>
                  <label className="block font-medium text-on-surface mb-1">Severity Scale</label>
                  <select
                    value={issueForm.severity}
                    onChange={(e) => setIssueForm({ ...issueForm, severity: e.target.value as any })}
                    className="w-full px-3 py-2 border border-outline rounded-lg bg-surface-container-lowest"
                  >
                    <option value="NORMAL">Normal</option>
                    <option value="ATTENTION">Attention Required</option>
                    <option value="CRITICAL">Critical Structural / Safety</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Issue Title</label>
                <input
                  type="text"
                  required
                  value={issueForm.title}
                  onChange={(e) => setIssueForm({ ...issueForm, title: e.target.value })}
                  placeholder="e.g. Inadequate rebar cover in ground floor slab beam"
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-on-surface mb-1">Location Details</label>
                  <input
                    type="text"
                    value={issueForm.location}
                    onChange={(e) => setIssueForm({ ...issueForm, location: e.target.value })}
                    placeholder="e.g. GF Kitchen Shaft Grid B"
                    className="w-full px-3 py-2 border border-outline rounded-lg"
                  />
                </div>
                <div>
                  <label className="block font-medium text-on-surface mb-1">Assigned Contractor</label>
                  <input
                    type="text"
                    value={issueForm.assignedTo}
                    onChange={(e) => setIssueForm({ ...issueForm, assignedTo: e.target.value })}
                    placeholder="e.g. Apex Civil Infrastructure"
                    className="w-full px-3 py-2 border border-outline rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Detailed Observation & Defect</label>
                <textarea
                  rows={3}
                  required
                  value={issueForm.description}
                  onChange={(e) => setIssueForm({ ...issueForm, description: e.target.value })}
                  placeholder="Detail exact non-conformance with drawing or specification standards..."
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Corrective Action Required</label>
                <input
                  type="text"
                  value={issueForm.correctiveAction}
                  onChange={(e) => setIssueForm({ ...issueForm, correctiveAction: e.target.value })}
                  placeholder="e.g. Tie 25mm PVC cover blocks before casting"
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Discovery Evidence Note</label>
                <input
                  type="text"
                  value={issueForm.evidenceUri}
                  onChange={(e) => setIssueForm({ ...issueForm, evidenceUri: e.target.value })}
                  placeholder="e.g. Photo taken during pre-pour inspection"
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-outline-variant/60">
                <button
                  type="button"
                  onClick={() => setShowIssueModal(false)}
                  className="px-4 py-2 border border-outline-variant rounded-lg text-on-surface-variant font-medium hover:bg-surface"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg font-semibold"
                >
                  Record Snag / NCR
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Add Daily Log */}
      {showLogModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/50 backdrop-blur-xs p-4 overflow-y-auto">
          <div className="bg-surface-container-lowest rounded-lg max-w-lg w-full p-6 shadow-xl space-y-4 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-outline-variant/60">
              <h3 className="text-base font-bold text-on-surface">Add Daily Site Log</h3>
              <button
                type="button"
                onClick={() => setShowLogModal(false)}
                className="text-on-surface-variant/70 hover:text-on-surface-variant text-sm font-semibold"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateLog} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-on-surface mb-1">Weather</label>
                <input
                  type="text"
                  required
                  value={logForm.weather}
                  onChange={(e) => setLogForm({ ...logForm, weather: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Work Completed (Detailed)</label>
                <textarea
                  rows={3}
                  required
                  value={logForm.workCompleted}
                  onChange={(e) => setLogForm({ ...logForm, workCompleted: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                  placeholder="Details of trades, poured areas, formwork completed..."
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Manpower on Site</label>
                <input
                  type="text"
                  value={logForm.manpower}
                  onChange={(e) => setLogForm({ ...logForm, manpower: e.target.value })}
                  placeholder="e.g. 2 Masons, 8 Laborers, 1 Electrician"
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Materials Received</label>
                <input
                  type="text"
                  value={logForm.materialsReceived}
                  onChange={(e) => setLogForm({ ...logForm, materialsReceived: e.target.value })}
                  placeholder="e.g. 50 bags cement, 2T rebar"
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Prepared By</label>
                <input
                  type="text"
                  value={logForm.preparedBy}
                  onChange={(e) => setLogForm({ ...logForm, preparedBy: e.target.value })}
                  placeholder="Site Engineer name"
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowLogModal(false)}
                  className="px-4 py-2 border border-outline-variant rounded-lg text-on-surface-variant font-medium hover:bg-surface"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg font-semibold"
                >
                  Save Daily Log
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Add Inspection */}
      {showInspectionModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/50 backdrop-blur-xs p-4">
          <div className="bg-surface-container-lowest rounded-lg max-w-md w-full p-6 shadow-xl space-y-4">
            <h3 className="text-base font-bold text-on-surface">Log Site Inspection</h3>
            <form onSubmit={handleCreateInspection} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-on-surface mb-1">Location Inspected</label>
                <input
                  type="text"
                  required
                  value={inspectionForm.location}
                  onChange={(e) => setInspectionForm({ ...inspectionForm, location: e.target.value })}
                  placeholder="e.g. Second Floor Slab Shuttering"
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Observation</label>
                <textarea
                  rows={3}
                  required
                  value={inspectionForm.observation}
                  onChange={(e) => setInspectionForm({ ...inspectionForm, observation: e.target.value })}
                  placeholder="Detail inspection findings..."
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Corrective Action</label>
                <input
                  type="text"
                  value={inspectionForm.correctiveAction}
                  onChange={(e) => setInspectionForm({ ...inspectionForm, correctiveAction: e.target.value })}
                  placeholder="e.g. Adjust rebar spacing along beam B-05"
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowInspectionModal(false)}
                  className="px-4 py-2 border border-outline-variant rounded-lg text-on-surface-variant font-medium hover:bg-surface"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg font-semibold"
                >
                  Save Inspection
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Add Meeting Minutes */}
      {showMeetingModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/50 backdrop-blur-xs p-4 overflow-y-auto">
          <div className="bg-surface-container-lowest rounded-lg max-w-lg w-full p-6 shadow-xl space-y-4 my-8">
            <h3 className="text-base font-bold text-on-surface">Record Meeting Minutes</h3>
            <form onSubmit={handleCreateMeeting} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-on-surface mb-1">Meeting Title</label>
                <input
                  type="text"
                  required
                  value={meetingForm.title}
                  onChange={(e) => setMeetingForm({ ...meetingForm, title: e.target.value })}
                  placeholder="e.g. Weekly Contractor Coordination #05"
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Attendees</label>
                <input
                  type="text"
                  required
                  value={meetingForm.attendees}
                  onChange={(e) => setMeetingForm({ ...meetingForm, attendees: e.target.value })}
                  placeholder="Architect, Client, Civil Contractor, MEP"
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Discussion Summary</label>
                <textarea
                  rows={2}
                  value={meetingForm.discussion}
                  onChange={(e) => setMeetingForm({ ...meetingForm, discussion: e.target.value })}
                  placeholder="Key topics discussed..."
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Decisions Taken</label>
                <textarea
                  rows={2}
                  value={meetingForm.decisions}
                  onChange={(e) => setMeetingForm({ ...meetingForm, decisions: e.target.value })}
                  placeholder="Agreed directives..."
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Action Items & Deliverables</label>
                <textarea
                  rows={2}
                  value={meetingForm.actionItems}
                  onChange={(e) => setMeetingForm({ ...meetingForm, actionItems: e.target.value })}
                  placeholder="Assigned actions and target dates..."
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowMeetingModal(false)}
                  className="px-4 py-2 border border-outline-variant rounded-lg text-on-surface-variant font-medium hover:bg-surface"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg font-semibold"
                >
                  Save Minutes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Confirm Closure (Requires Verification Note) */}
      {showClosureModal !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/50 backdrop-blur-xs p-4">
          <div className="bg-surface-container-lowest rounded-lg max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center gap-2 text-success pb-2 border-b border-outline-variant/60">
              <CheckCircle2 className="w-5 h-5" />
              <h3 className="text-base font-bold text-on-surface">Verify & Close Issue</h3>
            </div>
            <p className="text-xs text-on-surface-variant">
              Per practice QA protocol (SiteIssueWorkflow), a verification note is mandatory before closing any snag or NCR.
            </p>
            <div>
              <label className="block text-xs font-semibold text-on-surface mb-1">
                Verification Note (Architect Signoff)
              </label>
              <textarea
                rows={3}
                required
                value={verificationNote}
                onChange={(e) => setVerificationNote(e.target.value)}
                placeholder="e.g. Inspected on site on 30 Oct; micro-concrete grouting completed cleanly without cracks."
                className="w-full px-3 py-2 border border-outline rounded-lg text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-on-surface mb-1">
                After Rectification Evidence Note
              </label>
              <input
                type="text"
                value={afterEvidence}
                onChange={(e) => setAfterEvidence(e.target.value)}
                placeholder="e.g. Post-repair inspection photo recorded"
                className="w-full px-3 py-2 border border-outline rounded-lg text-xs"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => {
                  setShowClosureModal(null);
                  setVerificationNote('');
                  setAfterEvidence('');
                }}
                className="px-4 py-2 border border-outline-variant rounded-lg text-xs font-medium text-on-surface-variant hover:bg-surface"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={!verificationNote.trim()}
                onClick={() => handleConfirmClosure(showClosureModal)}
                className="px-4 py-2 bg-success hover:brightness-95 text-on-success rounded-lg text-xs font-semibold disabled:opacity-50"
              >
                Confirm Verification & Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
