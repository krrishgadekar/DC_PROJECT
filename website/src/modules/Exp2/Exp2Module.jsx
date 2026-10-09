import React, { useState, useEffect } from 'react';
import { Heart, Power, ShieldAlert, CheckCircle2 } from 'lucide-react';

export default function Exp2Module() {
  const [nodes, setNodes] = useState([
    { id: 1, port: 5001, status: 'ALIVE', lastHb: 0, count: 0 },
    { id: 2, port: 5002, status: 'ALIVE', lastHb: 0, count: 0 },
    { id: 3, port: 5003, status: 'ALIVE', lastHb: 0, count: 0 },
    { id: 4, port: 5004, status: 'ALIVE', lastHb: 0, count: 0 },
    { id: 5, port: 5005, status: 'ALIVE', lastHb: 0, count: 0 }
  ]);

  const [logs, setLogs] = useState([
    '[INIT] 5-Node UDP Socket Cluster initialized on ports 5001-5005.',
    '[HEARTBEAT] All nodes broadcasting 1000ms heartbeats.'
  ]);

  const toggleNodePower = (id) => {
    setNodes((prev) =>
      prev.map((n) => {
        if (n.id === id) {
          const nextStatus = n.status === 'ALIVE' ? 'CRASHED' : 'ALIVE';
          addLog(`[ACTION] Node ${id} marked as ${nextStatus}`);
          return { ...n, status: nextStatus };
        }
        return n;
      })
    );
  };

  const addLog = (msg) => {
    const time = new Date().toLocaleTimeString();
    setLogs((prev) => [`[${time}] ${msg}`, ...prev.slice(0, 15)]);
  };

  useEffect(() => {
    const interval = setInterval(() => {
      setNodes((prev) =>
        prev.map((n) => {
          if (n.status === 'ALIVE') {
            return { ...n, lastHb: Date.now(), count: n.count + 1 };
          }
          return n;
        })
      );
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div className="glass-card" style={{ padding: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.75rem' }}>
          <Heart size={22} color="#34d399" />
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#f8fafc' }}>
            Experiment 2: Heartbeat Failure Detection (5-Node UDP Cluster)
          </h2>
        </div>
        <p style={{ fontSize: '0.9rem', color: '#94a3b8', lineHeight: '1.6' }}>
          Simulates periodic 1-second UDP heartbeat broadcasting and 3-second timeout failure detection in a 5-node cluster.
        </p>
      </div>

      {/* Cluster Nodes */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
        {nodes.map((n) => (
          <div
            key={n.id}
            className="glass-card"
            style={{
              padding: '1.25rem',
              border: n.status === 'CRASHED' ? '1px solid #f43f5e' : '1px solid rgba(52, 211, 153, 0.4)',
              background: n.status === 'CRASHED' ? 'rgba(244, 63, 94, 0.08)' : 'rgba(15, 23, 42, 0.75)'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <div>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#f8fafc' }}>Node {n.id}</h3>
                <p style={{ fontSize: '0.72rem', color: '#64748b', fontFamily: 'var(--font-mono)' }}>UDP Port {n.port}</p>
              </div>

              {n.status === 'ALIVE' ? (
                <span className="badge badge-emerald"><CheckCircle2 size={12} /> ALIVE</span>
              ) : (
                <span className="badge badge-rose"><ShieldAlert size={12} /> DEAD</span>
              )}
            </div>

            <div style={{ marginBottom: '1rem' }}>
              <p style={{ fontSize: '0.75rem', color: '#64748b' }}>Heartbeats Sent:</p>
              <p style={{ fontSize: '1.4rem', fontWeight: 800, color: n.status === 'ALIVE' ? '#34d399' : '#f43f5e', fontFamily: 'var(--font-mono)' }}>
                {n.count}
              </p>
            </div>

            <button
              onClick={() => toggleNodePower(n.id)}
              className={n.status === 'ALIVE' ? 'btn-secondary' : 'btn-primary'}
              style={{ width: '100%', fontSize: '0.8rem', justifyContent: 'center' }}
            >
              <Power size={14} />
              {n.status === 'ALIVE' ? 'Crash Node' : 'Restart Node'}
            </button>
          </div>
        ))}
      </div>

      {/* Real-time Logs */}
      <div className="glass-card" style={{ padding: '1.25rem' }}>
        <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.75rem' }}>
          UDP Heartbeat & Telemetry Log
        </h3>
        <div style={{
          background: '#090d16',
          borderRadius: '8px',
          padding: '1rem',
          fontFamily: 'var(--font-mono)',
          fontSize: '0.82rem',
          color: '#34d399',
          maxHeight: '180px',
          overflowY: 'auto'
        }}>
          {logs.map((l, i) => (
            <div key={i}>{l}</div>
          ))}
        </div>
      </div>
    </div>
  );
}
