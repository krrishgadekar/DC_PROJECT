import React from 'react';
import { Network, Cpu, ShieldCheck, Terminal } from 'lucide-react';

export default function Navbar({ activeModule, onSelectModule }) {
  return (
    <nav style={{
      height: '64px',
      borderBottom: '1px solid rgba(51, 65, 85, 0.6)',
      background: 'rgba(15, 23, 42, 0.85)',
      backdropFilter: 'blur(16px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 1.5rem',
      position: 'sticky',
      top: 0,
      zIndex: 100
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem', cursor: 'pointer' }} onClick={() => onSelectModule('overview')}>
        <div style={{
          width: '38px',
          height: '38px',
          borderRadius: '10px',
          background: 'linear-gradient(135deg, #6366f1, #38bdf8)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 0 16px rgba(99, 102, 241, 0.4)'
        }}>
          <Network size={22} color="#ffffff" />
        </div>
        <div>
          <h1 style={{ fontSize: '1.15rem', fontWeight: 800, letterSpacing: '-0.02em', background: 'linear-gradient(135deg, #ffffff, #94a3b8)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            DISTRIBUTED COMPUTING LAB
          </h1>
          <p style={{ fontSize: '0.7rem', color: '#64748b', fontWeight: 600, letterSpacing: '0.05em' }}>
            COLLEGE LAB SUITE • EXP 1 - 6
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <div className="badge badge-emerald">
          <ShieldCheck size={14} /> SYSTEM ACTIVE
        </div>
        <div className="badge badge-indigo">
          <Cpu size={14} /> 5-NODE UDP CLUSTER
        </div>
        <button 
          onClick={() => onSelectModule('exp6')}
          className="btn-primary" 
          style={{ fontSize: '0.85rem', padding: '0.45rem 0.9rem' }}
        >
          <Terminal size={15} /> Jump to Exp 6
        </button>
      </div>
    </nav>
  );
}
