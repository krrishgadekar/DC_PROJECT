import React, { useState } from 'react';
import { Clock, RefreshCw, Zap } from 'lucide-react';

export default function Exp3Module() {
  const [nodes, setNodes] = useState([
    { id: 1, name: 'Node 1 (Master)', port: 6001, offsetMs: 0, lamport: 0, isMaster: true },
    { id: 2, name: 'Node 2 (Slave)', port: 6002, offsetMs: 350, lamport: 0, isMaster: false },
    { id: 3, name: 'Node 3 (Slave)', port: 6003, offsetMs: -210, lamport: 0, isMaster: false }
  ]);

  const [baseTime, setBaseTime] = useState(1000000);
  const [synced, setSynced] = useState(false);
  const [adjustmentLog, setAdjustmentLog] = useState([]);

  const updateOffset = (id, offset) => {
    setNodes((prev) =>
      prev.map((n) => (n.id === id ? { ...n, offsetMs: parseInt(offset) || 0 } : n))
    );
    setSynced(false);
  };

  const runLamportTick = (id) => {
    setNodes((prev) =>
      prev.map((n) => (n.id === id ? { ...n, lamport: n.lamport + 1 } : n))
    );
  };

  const runBerkeleySync = () => {
    // 1. Gather physical times
    const times = nodes.map((n) => baseTime + n.offsetMs);
    // 2. Compute average
    const avgTime = Math.round(times.reduce((a, b) => a + b, 0) / times.length);
    // 3. Compute adjustment needed for each node
    const logs = [];
    setNodes((prev) =>
      prev.map((n) => {
        const currentTime = baseTime + n.offsetMs;
        const diff = avgTime - currentTime;
        logs.push(`Node ${n.id}: Current=${currentTime}ms | Target=${avgTime}ms | Adjustment=${diff > 0 ? '+' : ''}${diff}ms`);
        return {
          ...n,
          offsetMs: n.offsetMs + diff, // Adjusted to sync
          lamport: n.lamport + 1
        };
      })
    );
    setAdjustmentLog(logs);
    setSynced(true);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div className="glass-card" style={{ padding: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.75rem' }}>
          <Clock size={22} color="#fbbf24" />
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#f8fafc' }}>
            Experiment 3: Clock Synchronization (Lamport & Berkeley Algorithm)
          </h2>
        </div>
        <p style={{ fontSize: '0.9rem', color: '#94a3b8', lineHeight: '1.6' }}>
          Simulates Berkeley Master-Slave physical clock drift synchronization and Lamport logical timestamp progression.
        </p>
      </div>

      <div style={{ display: 'flex', gap: '1rem' }}>
        <button className="btn-primary" onClick={runBerkeleySync}>
          Run Berkeley Clock Synchronization <RefreshCw size={16} />
        </button>
      </div>

      {/* Nodes Display */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
        {nodes.map((n) => {
          const physTime = baseTime + n.offsetMs;
          return (
            <div
              key={n.id}
              className="glass-card"
              style={{
                padding: '1.25rem',
                border: n.isMaster ? '1px solid rgba(251, 191, 36, 0.6)' : '1px solid rgba(51, 65, 85, 0.6)'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#f8fafc' }}>{n.name}</h3>
                {n.isMaster ? <span className="badge badge-amber">MASTER</span> : <span className="badge badge-indigo">SLAVE</span>}
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1rem' }}>
                <div>
                  <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Lamport Clock (L):</span>
                  <p style={{ fontSize: '1.5rem', fontWeight: 800, color: '#38bdf8', fontFamily: 'var(--font-mono)' }}>
                    L = {n.lamport}
                  </p>
                </div>

                <div>
                  <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Simulated Physical Time:</span>
                  <p style={{ fontSize: '1.2rem', fontWeight: 700, color: '#fbbf24', fontFamily: 'var(--font-mono)' }}>
                    {physTime} ms
                  </p>
                </div>

                <div>
                  <label style={{ fontSize: '0.75rem', color: '#64748b', display: 'block', marginBottom: '0.25rem' }}>
                    Drift Offset (ms):
                  </label>
                  <input
                    type="number"
                    value={n.offsetMs}
                    onChange={(e) => updateOffset(n.id, e.target.value)}
                    className="glass-input"
                    style={{ width: '100%', fontFamily: 'var(--font-mono)' }}
                  />
                </div>
              </div>

              <button
                onClick={() => runLamportTick(n.id)}
                className="btn-secondary"
                style={{ width: '100%', fontSize: '0.8rem', justifyContent: 'center' }}
              >
                <Zap size={14} /> Send Message (Tick L+1)
              </button>
            </div>
          );
        })}
      </div>

      {/* Sync Log */}
      {adjustmentLog.length > 0 && (
        <div className="glass-card" style={{ padding: '1.25rem' }}>
          <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.75rem' }}>
            Berkeley Algorithm Adjustment Output
          </h3>
          <div style={{ background: '#090d16', borderRadius: '8px', padding: '1rem', fontFamily: 'var(--font-mono)', fontSize: '0.84rem', color: '#fbbf24' }}>
            {adjustmentLog.map((log, i) => (
              <div key={i}>{log}</div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
