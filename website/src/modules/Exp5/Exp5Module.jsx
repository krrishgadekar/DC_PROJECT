import React, { useState } from 'react';
import { Lock, Unlock, Key, ArrowRight } from 'lucide-react';

export default function Exp5Module() {
  const [algorithm, setAlgorithm] = useState('ricart'); // 'ricart' or 'token'
  const [nodes, setNodes] = useState([
    { id: 1, state: 'RELEASED', timestamp: 0 },
    { id: 2, state: 'RELEASED', timestamp: 0 },
    { id: 3, state: 'RELEASED', timestamp: 0 }
  ]);
  const [tokenHolder, setTokenHolder] = useState(1);
  const [logs, setLogs] = useState([]);

  const addLog = (msg) => {
    setLogs((prev) => [`[${new Date().toLocaleTimeString()}] ${msg}`, ...prev.slice(0, 12)]);
  };

  const requestCS = (nodeId) => {
    if (algorithm === 'ricart') {
      const reqTimestamp = Math.floor(Math.random() * 100) + 1;
      setNodes((prev) =>
        prev.map((n) =>
          n.id === nodeId ? { ...n, state: 'HELD', timestamp: reqTimestamp } : n
        )
      );
      addLog(`[RICART-AGRAWALA] Node ${nodeId} sent REQUEST(T=${reqTimestamp}) to all peers.`);
      addLog(`[REPLY] Peers granted OK. Node ${nodeId} ENTERED Critical Section.`);
    } else {
      addLog(`[TOKEN RING] Node ${nodeId} requested Critical Section.`);
      setTokenHolder(nodeId);
      setNodes((prev) =>
        prev.map((n) => (n.id === nodeId ? { ...n, state: 'HELD' } : { ...n, state: 'RELEASED' }))
      );
      addLog(`[TOKEN] Token passed to Node ${nodeId}. Node ${nodeId} ENTERED Critical Section.`);
    }
  };

  const releaseCS = (nodeId) => {
    setNodes((prev) =>
      prev.map((n) => (n.id === nodeId ? { ...n, state: 'RELEASED' } : n))
    );
    addLog(`[RELEASE] Node ${nodeId} EXITED Critical Section.`);
    if (algorithm === 'token') {
      const nextNode = (nodeId % 3) + 1;
      setTokenHolder(nextNode);
      addLog(`[TOKEN] Token circulated to Node ${nextNode}.`);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div className="glass-card" style={{ padding: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.75rem' }}>
          <Lock size={22} color="#f43f5e" />
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#f8fafc' }}>
            Experiment 5: Distributed Mutual Exclusion (Ricart-Agrawala & Token Ring)
          </h2>
        </div>
        <p style={{ fontSize: '0.9rem', color: '#94a3b8', lineHeight: '1.6' }}>
          Simulates critical section safety using timestamp request/reply permission exchange or token ring circulation.
        </p>
      </div>

      {/* Algo Selector */}
      <div style={{ display: 'flex', gap: '0.5rem', background: 'rgba(15, 23, 42, 0.6)', padding: '0.25rem', borderRadius: '8px', border: '1px solid rgba(51, 65, 85, 0.6)', width: 'fit-content' }}>
        <button
          className={algorithm === 'ricart' ? 'btn-primary' : 'btn-secondary'}
          onClick={() => setAlgorithm('ricart')}
          style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
        >
          Ricart-Agrawala Algorithm
        </button>
        <button
          className={algorithm === 'token' ? 'btn-primary' : 'btn-secondary'}
          onClick={() => setAlgorithm('token')}
          style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
        >
          Token Ring Algorithm
        </button>
      </div>

      {/* Nodes Display */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1rem' }}>
        {nodes.map((n) => {
          const inCS = n.state === 'HELD';
          const hasToken = algorithm === 'token' && tokenHolder === n.id;
          return (
            <div
              key={n.id}
              className="glass-card"
              style={{
                padding: '1.25rem',
                border: inCS ? '2px solid #f43f5e' : '1px solid rgba(51, 65, 85, 0.6)',
                background: inCS ? 'rgba(244, 63, 94, 0.12)' : 'rgba(15, 23, 42, 0.75)'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#f8fafc' }}>Node {n.id}</h3>
                {inCS ? (
                  <span className="badge badge-rose"><Lock size={12} /> IN CS</span>
                ) : (
                  <span className="badge badge-emerald"><Unlock size={12} /> IDLE</span>
                )}
              </div>

              {algorithm === 'token' && (
                <div style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Key size={16} color={hasToken ? '#fbbf24' : '#64748b'} />
                  <span style={{ fontSize: '0.8rem', color: hasToken ? '#fbbf24' : '#64748b', fontWeight: 600 }}>
                    {hasToken ? 'POSSESSES TOKEN' : 'NO TOKEN'}
                  </span>
                </div>
              )}

              {inCS ? (
                <button
                  onClick={() => releaseCS(n.id)}
                  className="btn-secondary"
                  style={{ width: '100%', fontSize: '0.8rem', justifyContent: 'center' }}
                >
                  Exit Critical Section <ArrowRight size={14} />
                </button>
              ) : (
                <button
                  onClick={() => requestCS(n.id)}
                  className="btn-primary"
                  style={{ width: '100%', fontSize: '0.8rem', justifyContent: 'center' }}
                >
                  Request Critical Section
                </button>
              )}
            </div>
          );
        })}
      </div>

      {/* Log */}
      <div className="glass-card" style={{ padding: '1.25rem' }}>
        <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.75rem' }}>
          Mutual Exclusion Event Log
        </h3>
        <div style={{ background: '#090d16', borderRadius: '8px', padding: '1rem', fontFamily: 'var(--font-mono)', fontSize: '0.84rem', color: '#f43f5e' }}>
          {logs.length === 0 ? <div>No CS requests issued yet. Click "Request Critical Section" above.</div> : logs.map((l, i) => <div key={i}>{l}</div>)}
        </div>
      </div>
    </div>
  );
}
