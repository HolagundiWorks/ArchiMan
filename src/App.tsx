import React, { useState, useEffect } from 'react';
import { store } from './data/storage';
import { ProjectEntity } from './types';
import { PortfolioScreen } from './screens/PortfolioScreen';
import { ProjectWorkspaceScreen } from './screens/ProjectWorkspaceScreen';

export function App() {
  const [selectedProject, setSelectedProject] = useState<ProjectEntity | null>(null);
  const [, setTick] = useState(0);

  // Subscribe to store updates
  useEffect(() => {
    const unsubscribe = store.subscribe(() => {
      setTick((t) => t + 1);
    });
    return unsubscribe;
  }, []);

  // Sync selected project with store updates
  const activeProject = selectedProject
    ? store.projects.find((p) => p.id === selectedProject.id) || null
    : null;

  if (activeProject) {
    return (
      <ProjectWorkspaceScreen
        project={activeProject}
        onBackToPortfolio={() => setSelectedProject(null)}
      />
    );
  }

  return <PortfolioScreen onSelectProject={(p) => setSelectedProject(p)} />;
}

export default App;
