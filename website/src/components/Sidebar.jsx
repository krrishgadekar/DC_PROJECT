import React from 'react';
import { 
  LayoutDashboard, 
  Activity, 
  Heart, 
  Clock, 
  Crown, 
  Lock, 
  Scale, 
  ChevronRight 
} from 'lucide-react';

const MODULES = [
  { id: 'overview', title: 'Lab Suite Overview', subtitle: 'Curriculum & Architecture', icon: LayoutDashboard, badge: 'Hub' },
  { id: 'exp1', title: 'Exp 1: Multi-Agent Systems', subtitle: 'Actuator Failure & Control', icon: Activity, badge: 'Exp 1' },
  { id: 'exp2', title: 'Exp 2: Heartbeat Cluster', subtitle: 'UDP Failure Detection', icon: Heart, badge: 'Exp 2' },
  { id: 'exp3', title: 'Exp 3: Clock Sync', subtitle: 'Lamport & Berkeley Algorithm', icon: Clock, badge: 'Exp 3' },
  { id: 'exp4', title: 'Exp 4: Leader Election', subtitle: 'Bully vs Ring Benchmark', icon: Crown, badge: 'Exp 4' },
  { id: 'exp5', title: 'Exp 5: Mutual Exclusion', subtitle: 'Ricart-Agrawala & Token Ring', icon: Lock, badge: 'Exp 5' },
  { id: 'exp6', title: 'Exp 6: Quorum & Load Balancing', subtitle: 'Split-Brain & 5 Balancers', icon: Scale, badge: 'Exp 6', featured: true }
];

export default function Sidebar({ activeModule, onSelectModule }) {
  return (
    <aside style={{
      width: '300px',
      borderRight: '1px solid rgba(51, 65, 85, 0.6)',
      background: 'rgba(15, 23, 42, 0.6)',
      padding: '1.25rem 1rem',
      display: 'flex',
      flexDirection: 'column',
      gap: '0.5rem',
      minHeight: 'calc(100vh - 64px)'
    }}>
      <div style={{ padding: '0 0.5rem 0.75rem 0.5rem', borderBottom: '1px solid rgba(51, 65, 85, 0.4)', marginBottom: '0.5rem' }}>
        <p style={{ fontSize: '0.75rem', fontWeight: 700, color: '#64748b', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
          EXPERIMENT MODULES
        </p>
      </div>

      {MODULES.map((item) => {
        const Icon = item.icon;
        const isActive = activeModule === item.id;
        return (
          <button
            key={item.id}
            onClick={() => onSelectModule(item.id)}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '0.8rem 0.9rem',
              borderRadius: '12px',
              border: isActive 
                ? '1px solid rgba(99, 102, 241, 0.6)' 
                : item.featured 
                ? '1px solid rgba(56, 189, 248, 0.3)' 
                : '1px solid transparent',
              background: isActive 
                ? 'linear-gradient(135deg, rgba(99, 102, 241, 0.25), rgba(56, 189, 248, 0.15))' 
                : 'transparent',
              color: isActive ? '#f8fafc' : '#94a3b8',
              cursor: 'pointer',
              textAlign: 'left',
              transition: 'all 0.2s ease'
            }}
            onMouseEnter={(e) => {
              if (!isActive) e.currentTarget.style.background = 'rgba(30, 41, 59, 0.6)';
            }}
            onMouseLeave={(e) => {
              if (!isActive) e.currentTarget.style.background = 'transparent';
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
              <div style={{
                width: '34px',
                height: '34px',
                borderRadius: '8px',
                background: isActive 
                  ? 'rgba(99, 102, 241, 0.4)' 
                  : item.featured 
                  ? 'rgba(56, 189, 248, 0.15)' 
                  : 'rgba(30, 41, 59, 0.8)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: isActive ? '#38bdf8' : item.featured ? '#38bdf8' : '#94a3b8'
              }}>
                <Icon size={18} />
              </div>
              <div>
                <h2 style={{ fontSize: '0.88rem', fontWeight: 600, color: isActive ? '#f8fafc' : '#e2e8f0', margin: 0 }}>
                  {item.title}
                </h2>
                <p style={{ fontSize: '0.72rem', color: '#64748b', margin: 0 }}>
                  {item.subtitle}
                </p>
              </div>
            </div>

            <ChevronRight size={16} style={{ opacity: isActive ? 1 : 0.4, transform: isActive ? 'translateX(2px)' : 'none', transition: 'all 0.2s' }} />
          </button>
        );
      })}
    </aside>
  );
}
