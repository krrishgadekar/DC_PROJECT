import React, { useState } from 'react';
import { Cpu, RefreshCw, AlertTriangle, CheckCircle2 } from 'lucide-react';

export default function Exp1Module() {
  const [agents, setAgents] = useState([
    { id: 1, name: 'Agent 1 (Leader)', state: 10.0, status: 'NORMAL', gain: 1.0 },
    { id: 2, name: 'Agent 2 (Follower)', state: 4.2, status: 'NORMAL', gain: 1.0 },
    { id: 3, name: 'Agent 3 (Follower)', state: 2.1, status: 'NORMAL', gain: 1.0 },
    { id: 4, name: 'Agent 4 (Follower)', state: 1.5, status: 'NORMAL', gain: 1.0 }
  ]);
  const [targetState] = useState(10.0);
  const [iteration, setIteration] = useState(0);

  const toggleFail = (id) => {
    setAgents((prev) =>
      prev.map((a) =>
        a.id === id
          ? {
              ...a,
              status: a.status === 'FAILED' ? 'NORMAL' : 'FAILED',
              gain: a.status === 'FAILED' ? 1.0 : 0.2 // Reduced gain under actuator failure
            }
          : a
      )
    );
  };

  const stepConsensus = () => {
    setAgents((prev) => {
      const next = [...prev];
      const alpha = 0.3; // Coupling gain

      for (let i = 1; i < next.length; i++) {
        const ag = next[i];
        if (ag.status === 'FAILED') {
          // Adaptive compensation rule: increase neighbor feedback
          ag.state = ag.state + ag.gain * alpha * (next[0].state - ag.state) + 0.1;
        } else {
          ag.state = ag.state + alpha * (next[0].state - ag.state);
        }
      }
      return next;
    });
    setIteration((i) => i + 1);
  };

  const resetSimulation = () => {
    setAgents([
      { id: 1, name: 'Agent 1 (Leader)', state: 10.0, status: 'NORMAL', gain: 1.0 },
      { id: 2, name: 'Agent 2 (Follower)', state: 4.2, status: 'NORMAL', gain: 1.0 },
      { id: 3, name: 'Agent 3 (Follower)', state: 2.1, status: 'NORMAL', gain: 1.0 },
      { id: 4, name: 'Agent 4 (Follower)', state: 1.5, status: 'NORMAL', gain: 1.0 }
    ]);
    setIteration(0);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div className="glass-card" style={{ padding: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyBetween: 'space-between', marginBottom: '0.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <Cpu size={22} color="#38bdf8" />
            <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#f8fafc' }}>
              Experiment 1: Multi-Agent Systems & Actuator Failures
            </h2>
          </div>
          <span className="badge badge-cyan">Iteration: {iteration}</span>
        </div>
        <p style={{ fontSize: '0.9rem', color: '#94a3b8', lineHeight: '1.6' }}>
          Simulates adaptive parameter control in multi-agent consensus networks. Inject actuator failures to test adaptive gain compensation.
        </p>
      </div>

      {/* Control Bar */}
      <div style={{ display: 'flex', gap: '1rem' }}>
        <button className="btn-primary" onClick={stepConsensus}>
          Run Consensus Step <RefreshCw size={16} />
        </button>
        <button className="btn-secondary" onClick={resetSimulation}>
          Reset Simulation
        </button>
      </div>

      {/* Agent Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1rem' }}>
        {agents.map((ag) => (
          <div key={ag.id} className="glass-card" style={{ padding: '1.25rem', border: ag.status === 'FAILED' ? '1px solid #f43f5e' : '1px solid rgba(51, 65, 85, 0.6)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#f8fafc' }}>{ag.name}</h3>
              {ag.status === 'FAILED' ? (
                <span className="badge badge-rose"><AlertTriangle size={12} /> FAILED</span>
              ) : (
                <span className="badge badge-emerald"><CheckCircle2 size={12} /> OK</span>
              )}
            </div>

            <div style={{ marginBottom: '1rem' }}>
              <p style={{ fontSize: '0.75rem', color: '#64748b' }}>Current Consensus State:</p>
              <p style={{ fontSize: '1.6rem', fontWeight: 800, color: '#38bdf8', fontFamily: 'var(--font-mono)' }}>
                {ag.state.toFixed(2)}
              </p>
            </div>

            {ag.id !== 1 && (
              <button
                onClick={() => toggleFail(ag.id)}
                className={ag.status === 'FAILED' ? 'btn-primary' : 'btn-secondary'}
                style={{ width: '100%', fontSize: '0.8rem', justifyContent: 'center' }}
              >
                {ag.status === 'FAILED' ? 'Recover Actuator' : 'Simulate Actuator Failure'}
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
