import React from 'react';
import { 
  Network, 
  Cpu, 
  ShieldCheck, 
  Clock, 
  Crown, 
  Lock, 
  Scale, 
  ArrowRight,
  Sparkles
} from 'lucide-react';

export default function OverviewModule({ onSelectModule }) {
  const EXPERIMENTS = [
    {
      id: 'exp1',
      title: 'Experiment 1: Multi-Agent Systems',
      subtitle: 'Actuator Failures & Adaptive Control',
      desc: 'Formulates multi-agent consensus algorithms under node failure models and parameter tuning.',
      icon: Cpu,
      color: '#38bdf8'
    },
    {
      id: 'exp2',
      title: 'Experiment 2: Heartbeat Cluster',
      subtitle: 'UDP Heartbeat Failure Detector',
      desc: '5-node UDP socket cluster monitoring node liveness with 1s heartbeats and 3s timeout detection.',
      icon: ShieldCheck,
      color: '#34d399'
    },
    {
      id: 'exp3',
      title: 'Experiment 3: Clock Synchronization',
      subtitle: 'Lamport Timestamps & Berkeley Algorithm',
      desc: 'Physical clock drift compensation via Master-Slave time polling and Lamport causal clock ticking.',
      icon: Clock,
      color: '#fbbf24'
    },
    {
      id: 'exp4',
      title: 'Experiment 4: Leader Election',
      subtitle: 'Bully Algorithm vs Ring Algorithm',
      desc: 'Comparative evaluation of leader election message complexity and recovery timing during node failure.',
      icon: Crown,
      color: '#a855f7'
    },
    {
      id: 'exp5',
      title: 'Experiment 5: Mutual Exclusion',
      subtitle: 'Ricart-Agrawala & Token Ring',
      desc: 'Guarantees critical section access control using timestamp request/reply and circulating token state.',
      icon: Lock,
      color: '#f43f5e'
    },
    {
      id: 'exp6',
      title: 'Experiment 6: Quorum & Load Balancing',
      subtitle: 'Split-Brain Election & 5 Balancers',
      desc: 'Quorum threshold Q=3 split-brain prevention with 5 dynamic load balancing strategies (RR, LC, WRR, Random, Response Time).',
      icon: Scale,
      color: '#6366f1',
      featured: true
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {/* Hero Banner */}
      <div className="glass-card" style={{
        padding: '2.5rem',
        background: 'linear-gradient(135deg, rgba(15, 23, 42, 0.9), rgba(30, 41, 59, 0.8))',
        border: '1px solid rgba(99, 102, 241, 0.4)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div style={{ position: 'relative', zIndex: 1, maxWidth: '800px' }}>
          <div className="badge badge-indigo" style={{ marginBottom: '1rem' }}>
            <Sparkles size={14} /> DISTRIBUTED COMPUTING LAB SUITE
          </div>
          <h1 style={{ fontSize: '2.4rem', fontWeight: 800, color: '#f8fafc', marginBottom: '1rem', lineHeight: '1.2' }}>
            Interactive Distributed Systems Engineering Hub
          </h1>
          <p style={{ fontSize: '1.05rem', color: '#94a3b8', lineHeight: '1.6', marginBottom: '1.5rem' }}>
            An interactive web platform demonstrating key distributed computing paradigms: consensus, liveness detection, physical & logical clock synchronization, leader election protocols, mutual exclusion, and quorum-enforced split-brain load balancing.
          </p>
          <div style={{ display: 'flex', gap: '1rem' }}>
            <button className="btn-primary" onClick={() => onSelectModule('exp6')}>
              Explore Exp 6: Quorum & Load Balancing <ArrowRight size={16} />
            </button>
            <button className="btn-secondary" onClick={() => onSelectModule('exp3')}>
              <Clock size={16} /> Test Clock Sync (Exp 3)
            </button>
          </div>
        </div>
      </div>

      {/* Grid of Experiments */}
      <div>
        <h2 style={{ fontSize: '1.3rem', fontWeight: 700, color: '#f8fafc', marginBottom: '1.25rem' }}>
          Experiment Modules & Interactive Demonstrations
        </h2>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '1.25rem' }}>
          {EXPERIMENTS.map((exp) => {
            const Icon = exp.icon;
            return (
              <div
                key={exp.id}
                className="glass-card"
                onClick={() => onSelectModule(exp.id)}
                style={{
                  padding: '1.5rem',
                  cursor: 'pointer',
                  border: exp.featured ? '1px solid rgba(99, 102, 241, 0.6)' : '1px solid rgba(51, 65, 85, 0.6)',
                  background: exp.featured ? 'linear-gradient(135deg, rgba(30, 41, 59, 0.9), rgba(15, 23, 42, 0.8))' : 'rgba(15, 23, 42, 0.75)',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between'
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                    <div style={{
                      width: '42px',
                      height: '42px',
                      borderRadius: '12px',
                      background: `${exp.color}20`,
                      border: `1px solid ${exp.color}40`,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: exp.color
                    }}>
                      <Icon size={22} />
                    </div>
                    {exp.featured && <span className="badge badge-cyan">Featured</span>}
                  </div>

                  <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.35rem' }}>
                    {exp.title}
                  </h3>
                  <p style={{ fontSize: '0.8rem', fontWeight: 600, color: exp.color, marginBottom: '0.75rem' }}>
                    {exp.subtitle}
                  </p>
                  <p style={{ fontSize: '0.88rem', color: '#94a3b8', lineHeight: '1.5' }}>
                    {exp.desc}
                  </p>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '1.25rem', fontSize: '0.85rem', fontWeight: 600, color: exp.color }}>
                  Launch Module <ArrowRight size={14} />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
