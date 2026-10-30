import React, { useState } from 'react';
import { store } from '../data/storage';
import { ProjectEntity } from '../types';
import {
  Compass,
  CheckSquare,
  Scale,
  Calendar,
  Plus,
  CheckCircle,
  Clock,
  Layers,
} from 'lucide-react';

interface PlanningViewProps {
  project: ProjectEntity;
}

export const PlanningView: React.FC<PlanningViewProps> = ({ project }) => {
  const [activeTab, setActiveTab] = useState<'DECISIONS' | 'TASKS' | 'BRIEF'>('DECISIONS');

  const decisions = store.projectDecisions.filter((d) => d.projectId === project.id);
  const tasks = store.projectTasks.filter((t) => t.projectId === project.id);

  const [showDecisionModal, setShowDecisionModal] = useState(false);
  const [showTaskModal, setShowTaskModal] = useState(false);

  const [decisionForm, setDecisionForm] = useState({
    title: '',
    context: '',
    decisionRequired: '',
    impact: '',
    finalDecision: '',
    requestedFrom: '',
    owner: '',
    status: 'DECIDED' as 'OPEN' | 'DECIDED',
  });

  const [taskForm, setTaskForm] = useState({
    title: '',
    description: '',
  });

  const handleCreateDecision = (e: React.FormEvent) => {
    e.preventDefault();
    if (!decisionForm.title.trim()) return;

    const count = decisions.length + 1;
    const ref = `DEC-${project.projectCode}-${count.toString().padStart(2, '0')}`;

    store.addDecision({
      projectId: project.id,
      referenceNumber: ref,
      title: decisionForm.title,
      context: decisionForm.context,
      decisionRequired: decisionForm.decisionRequired,
      impact: decisionForm.impact,
      finalDecision: decisionForm.finalDecision,
      requestedFrom: decisionForm.requestedFrom,
      owner: decisionForm.owner,
      dueAt: null,
      status: decisionForm.status,
      decidedAt: decisionForm.status === 'DECIDED' ? Date.now() : null,
    });

    setShowDecisionModal(false);
    setDecisionForm({
      title: '',
      context: '',
      decisionRequired: '',
      impact: '',
      finalDecision: '',
      requestedFrom: '',
      owner: '',
      status: 'DECIDED',
    });
  };

  const handleCreateTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!taskForm.title.trim()) return;

    store.addTask({
      projectId: project.id,
      title: taskForm.title,
      description: taskForm.description,
      dueDate: Date.now() + 7 * 86400000,
      status: 'OPEN',
    });

    setShowTaskModal(false);
    setTaskForm({ title: '', description: '' });
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <Compass className="w-5 h-5 text-indigo-600" />
            <h2 className="text-xl font-bold text-slate-900">Project Controls & Planning</h2>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Authoritative decision log, architectural tasks checklist, and brief/scope specifications.
          </p>
        </div>

        <div className="flex items-center gap-2">
          {activeTab === 'DECISIONS' && (
            <button
              type="button"
              onClick={() => setShowDecisionModal(true)}
              className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm"
            >
              <Plus className="w-4 h-4" /> Log Decision
            </button>
          )}
          {activeTab === 'TASKS' && (
            <button
              type="button"
              onClick={() => setShowTaskModal(true)}
              className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm"
            >
              <Plus className="w-4 h-4" /> Add Task
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200 gap-6">
        <button
          type="button"
          onClick={() => setActiveTab('DECISIONS')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'DECISIONS'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Scale className="w-4 h-4" />
          <span>Decisions Log ({decisions.length})</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('TASKS')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'TASKS'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <CheckSquare className="w-4 h-4" />
          <span>Milestones & Action Items ({tasks.length})</span>
        </button>
      </div>

      {/* View: Decisions Log */}
      {activeTab === 'DECISIONS' && (
        <div className="space-y-3">
          {decisions.length === 0 ? (
            <div className="bg-white p-12 text-center rounded-xl border border-dashed border-slate-300">
              <Scale className="w-10 h-10 text-slate-400 mx-auto mb-2" />
              <h3 className="text-sm font-semibold text-slate-700">No Project Decisions Logged</h3>
              <p className="text-xs text-slate-500 mt-1">
                Record material selections, layout approvals, and client signoffs.
              </p>
            </div>
          ) : (
            decisions.map((dec) => (
              <div key={dec.id} className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-3">
                <div className="flex items-center justify-between border-b border-slate-100 pb-2.5">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-semibold text-slate-700">
                      {dec.referenceNumber}
                    </span>
                    <span className="text-slate-300">·</span>
                    <div className="flex items-center gap-1.5 text-xs font-mono font-medium text-slate-700">
                      <span className={`w-1.5 h-1.5 rounded-full ${dec.status === 'DECIDED' ? 'bg-emerald-500' : 'bg-amber-500'}`}></span>
                      <span>{dec.status}</span>
                    </div>
                  </div>
                  {dec.decidedAt && (
                    <span className="text-[11px] text-slate-400">
                      Decided: {new Date(dec.decidedAt).toLocaleDateString()}
                    </span>
                  )}
                </div>

                <h3 className="text-base font-bold text-slate-900">{dec.title}</h3>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                  <div className="bg-slate-50 p-3 rounded-lg border border-slate-100 space-y-1">
                    <div className="font-semibold text-slate-700">Context & Option Evaluated:</div>
                    <div className="text-slate-600 leading-relaxed">{dec.context}</div>
                  </div>
                  <div className="bg-emerald-50/60 p-3 rounded-lg border border-emerald-100 space-y-1">
                    <div className="font-semibold text-emerald-900">Final Decision & Impact:</div>
                    <div className="text-slate-700 font-medium">{dec.finalDecision}</div>
                    <div className="text-[11px] text-emerald-800">{dec.impact}</div>
                  </div>
                </div>

                <div className="text-[11px] text-slate-400 flex justify-between pt-1">
                  <span>Requested from: {dec.requestedFrom}</span>
                  <span>Owner: {dec.owner}</span>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* View: Tasks & Checklist */}
      {activeTab === 'TASKS' && (
        <div className="space-y-3">
          {tasks.length === 0 ? (
            <div className="bg-white p-12 text-center rounded-xl border border-dashed border-slate-300">
              <CheckSquare className="w-10 h-10 text-slate-400 mx-auto mb-2" />
              <h3 className="text-sm font-semibold text-slate-700">No Action Items Listed</h3>
            </div>
          ) : (
            tasks.map((task) => {
              const isDone = task.status === 'COMPLETED';
              return (
                <div
                  key={task.id}
                  className={`bg-white rounded-xl border p-4 shadow-sm flex items-start gap-3 transition ${
                    isDone ? 'border-emerald-200 bg-emerald-50/30' : 'border-slate-200'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={isDone}
                    onChange={() => store.toggleTaskStatus(task.id)}
                    className="mt-1 rounded text-indigo-600 w-4 h-4"
                  />
                  <div className="space-y-1 flex-1">
                    <div className="flex items-center justify-between">
                      <h4 className={`text-sm font-semibold ${isDone ? 'line-through text-slate-400' : 'text-slate-900'}`}>
                        {task.title}
                      </h4>
                      {task.dueDate && (
                        <span className="text-[11px] text-slate-400 flex items-center gap-1">
                          <Calendar className="w-3 h-3" /> Due {new Date(task.dueDate).toLocaleDateString()}
                        </span>
                      )}
                    </div>
                    {task.description && (
                      <p className={`text-xs ${isDone ? 'text-slate-400' : 'text-slate-600'}`}>
                        {task.description}
                      </p>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* Modal: Add Decision */}
      {showDecisionModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl space-y-4 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-800">Log Formal Decision</h3>
              <button
                type="button"
                onClick={() => setShowDecisionModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-semibold"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateDecision} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-slate-700 mb-1">Decision Title</label>
                <input
                  type="text"
                  required
                  value={decisionForm.title}
                  onChange={(e) => setDecisionForm({ ...decisionForm, title: e.target.value })}
                  placeholder="e.g. Italian Travertine vs Local Granite for Foyer"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Context & Options Considered</label>
                <textarea
                  rows={2}
                  value={decisionForm.context}
                  onChange={(e) => setDecisionForm({ ...decisionForm, context: e.target.value })}
                  placeholder="Explain background and alternatives evaluated..."
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Final Agreed Decision</label>
                <textarea
                  rows={2}
                  required
                  value={decisionForm.finalDecision}
                  onChange={(e) => setDecisionForm({ ...decisionForm, finalDecision: e.target.value })}
                  placeholder="State the final directive agreed with client / consultant..."
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Approved By / Requested From</label>
                  <input
                    type="text"
                    value={decisionForm.requestedFrom}
                    onChange={(e) => setDecisionForm({ ...decisionForm, requestedFrom: e.target.value })}
                    placeholder="e.g. Dr. Ananya Rao"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Architect In Charge</label>
                  <input
                    type="text"
                    value={decisionForm.owner}
                    onChange={(e) => setDecisionForm({ ...decisionForm, owner: e.target.value })}
                    placeholder="e.g. Ar. Vishwabhiram"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Impact on Scope / Specification</label>
                <input
                  type="text"
                  value={decisionForm.impact}
                  onChange={(e) => setDecisionForm({ ...decisionForm, impact: e.target.value })}
                  placeholder="e.g. Requires anti-slip honing treatment"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowDecisionModal(false)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 font-medium hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold"
                >
                  Record Decision
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Add Task */}
      {showTaskModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-800">Add Milestone / Task</h3>
              <button
                type="button"
                onClick={() => setShowTaskModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-semibold"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateTask} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-slate-700 mb-1">Task Title</label>
                <input
                  type="text"
                  required
                  value={taskForm.title}
                  onChange={(e) => setTaskForm({ ...taskForm, title: e.target.value })}
                  placeholder="e.g. Review sanitary fixture samples with client"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-slate-700 mb-1">Notes / Description</label>
                <textarea
                  rows={2}
                  value={taskForm.description}
                  onChange={(e) => setTaskForm({ ...taskForm, description: e.target.value })}
                  placeholder="Additional details..."
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowTaskModal(false)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 font-medium hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold"
                >
                  Add Task
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
