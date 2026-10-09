import React, { useState } from 'react';
import { Crown, Zap, ShieldAlert, CheckCircle2 } from 'lucide-react';

export default function Exp4Module() {
  const [nodes, setNodes] = useState([
    { id: 1, alive: true },
    { id: 2, alive: true },
    { id: 3, alive: true },
    { id: 4, alive: true },
    { id: 5, alive: true }
  ]);

  const [leaderId, setLeaderId] = useState(5);
  const [electionLog, setElectionLog] = useState([]);
  const [algorithm, setAlgorithm] = useState('bully');

  const toggleNode = (id) => {
    setNodes((prev) => {
      const next = prev.map((n) => (n.id === id ? { ...n, alive: !n.alive } : n));
      // Re-evaluate leader
      const aliveNodes = next.filter((n) => n.alive).map((n) => n.id);
      if (aliveNodes.length > 0) {
        const newLeader = Math.max(...aliveNodes);
        setLeaderId(newLeader);
      } else {
        setLeaderId(null);
      }
      return next;
    });
  };

  const triggerElection = () => {
    const aliveNodes = nodes.filter((n) => n.alive).map((n) => n.id);
    if (aliveNodes.length === 0) {
      setElectionLog(['[ERROR] All nodes dead! No election possible.']);
      return;
    }

    const logs = [];
    if (algorithm === 'bully') {
      logs.push('[BULLY ELECTION] Node detected leader failure.');
      aliveNodes.forEach((nid) => {
        const higher = aliveNodes.filter((h) => h > nid);
        if (higher.length === 0) {
          logs.push(`Node ${nid} sent ELECTION to higher nodes. No response received. Node ${nid} claims COORDINATOR!`);
        } else {
          logs.push(`Node ${nid} sent ELECTION to higher nodes: ${higher.join(', ')}. Answer OK received.`);
        }
      });
      const winner = Math.max(...aliveNodes);
      setLeaderId(winner);
      logs.push(`[VICTORY] Node ${winner} declared Leader via Bully Algorithm.`);
    } else {
      logs.push('[RING ELECTION] Node detected leader failure.');
      logs.push(`Election token circulating in ring: [${aliveNodes.join(' -> ')} -> ${aliveNodes[0]}]`);
      const winner = Math.max(...aliveNodes);
      setLeaderId(winner);
      logs.push(`[VICTORY] Highest ID in election list collected. Node ${winner} declared Leader via Ring Algorithm.`);
    }
    setElectionLog(logs);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div className="glass-card" style={{ padding: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.75rem' }}>
          <Crown size={22} color="#a855f7" />
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#f8fafc' }}>
            Experiment 4: Comparative Leader Election (Bully vs Ring)
          </h2>
        </div>
        <p style={{ fontSize: '0.9rem', color: '#94a3b8', lineHeight: '1.6' }}>
          Simulates message complexity and leader selection between the Bully Election Protocol and Ring Election Protocol.
        </p>
      </div>

      {/* Control Bar */}
      <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
        <div style={{ display: 'flex', gap: '0.5rem', background: 'rgba(15, 23, 42, 0.6)', padding: '0.25rem', borderRadius: '8px', border: '1px solid rgba(51, 65, 85, 0.6)' }}>
          <button
            className={algorithm === 'bully' ? 'btn-primary' : 'btn-secondary'}
            onClick={() => setAlgorithm('bully')}
            style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
          >
            Bully Algorithm
          </button>
          <button
            className={algorithm === 'ring' ? 'btn-primary' : 'btn-secondary'}
            onClick={() => setAlgorithm('ring')}
            style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
          >
            Ring Algorithm
          </button>
        </div>

        <button className="btn-primary" onClick={triggerElection}>
          Trigger Election <Zap size={16} />
        </button>
      </div>

      {/* Nodes Display */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
        {nodes.map((n) => {
          const isLeader = leaderId === n.id && n.alive;
          return (
            <div
              key={n.id}
              className="glass-card"
              style={{
                padding: '1.25rem',
                border: isLeader
                  ? '2px solid #a855f7'
                  : !n.alive
                  ? '1px solid #f43f5e'
                  : '1px solid rgba(51, 65, 85, 0.6)',
                background: isLeader ? 'rgba(168, 85, 247, 0.12)' : 'rgba(15, 23, 42, 0.75)'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                <h3 style={{ fontSize: '1.05rem', fontWeight: 700, color: '#f8fafc' }}>Node {n.id}</h3>
                {isLeader ? (
                  <span className="badge badge-indigo"><Crown size={12} /> LEADER</span>
                ) : n.alive ? (
                  <span className="badge badge-emerald"><CheckCircle2 size={12} /> FOLLOWER</span>
                ) : (
                  <span className="badge badge-rose"><ShieldAlert size={12} /> DEAD</span>
                )}
              </div>

              <button
                onClick={() => toggleNode(n.id)}
                className={n.alive ? 'btn-secondary' : 'btn-primary'}
                style={{ width: '100%', fontSize: '0.8rem', justifyContent: 'center' }}
              >
                {n.alive ? 'Kill Node' : 'Revive Node'}
              </button>
            </div>
          );
        })}
      </div>

      {/* Log */}
      {electionLog.length > 0 && (
        <div className="glass-card" style={{ padding: '1.25rem' }}>
          <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.75rem' }}>
            Election Trace Log ({algorithm.toUpperCase()})
          </h3>
          <div style={{ background: '#090d16', borderRadius: '8px', padding: '1rem', fontFamily: 'var(--font-mono)', fontSize: '0.84rem', color: '#a855f7' }}>
            {electionLog.map((log, i) => (
              <div key={i}>{log}</div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
