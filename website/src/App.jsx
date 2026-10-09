import React, { useState } from 'react';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import OverviewModule from './modules/Overview/OverviewModule';
import Exp1Module from './modules/Exp1/Exp1Module';
import Exp2Module from './modules/Exp2/Exp2Module';
import Exp3Module from './modules/Exp3/Exp3Module';
import Exp4Module from './modules/Exp4/Exp4Module';
import Exp5Module from './modules/Exp5/Exp5Module';
import Exp6Module from './modules/Exp6/Exp6Module';

export default function App() {
  const [activeModule, setActiveModule] = useState('exp6'); // Default to exp6 as requested

  const renderModule = () => {
    switch (activeModule) {
      case 'overview':
        return <OverviewModule onSelectModule={setActiveModule} />;
      case 'exp1':
        return <Exp1Module />;
      case 'exp2':
        return <Exp2Module />;
      case 'exp3':
        return <Exp3Module />;
      case 'exp4':
        return <Exp4Module />;
      case 'exp5':
        return <Exp5Module />;
      case 'exp6':
      default:
        return <Exp6Module />;
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar activeModule={activeModule} onSelectModule={setActiveModule} />

      <div style={{ display: 'flex', flex: 1 }}>
        <Sidebar activeModule={activeModule} onSelectModule={setActiveModule} />

        <main style={{ flex: 1, padding: '1.75rem 2rem', overflowX: 'hidden' }}>
          {renderModule()}
        </main>
      </div>
    </div>
  );
}
