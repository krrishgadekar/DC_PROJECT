import React, { useState } from 'react';
import { 
  Scale, 
  ShieldCheck, 
  ShieldAlert, 
  Play, 
  RefreshCw, 
  Zap, 
  FileText, 
  Code,
  Activity
} from 'lucide-react';
import CodeViewer from '../../components/CodeViewer';
import PresentationViewer from '../../components/PresentationViewer';

// Source Code definitions for tab 4
const EXP6_SOURCE_FILES = [
  {
    name: 'DistributedNode.java',
    code: `import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DistributedNode {
    private static final Map<Integer, Integer> CLUSTER_PORTS = Map.of(
        1, 5001, 2, 5002, 3, 5003, 4, 5004, 5, 5005
    );
    private static final int TOTAL_NODES = 5;
    private static final int QUORUM_THRESHOLD = (TOTAL_NODES / 2) + 1; // 3
    
    private final int nodeId;
    private final String algorithm;
    private Integer leaderId = null;
    private boolean isLeader = false;
    private LoadBalancer loadBalancer;

    // Threads: Sender, Listener, Watchdog, Dispatcher, Executor, Stats
    public void start() {
        log("INIT", "Starting node on port " + CLUSTER_PORTS.get(nodeId));
    }
}`
  },
  {
    name: 'LoadBalancer.java',
    code: `abstract class LoadBalancer {
    protected Map<Integer, NodeMetrics> nodeMetrics;
    public abstract int selectNode(List<Integer> availableNodes);
}

class RoundRobinBalancer extends LoadBalancer {
    private int currentIndex = 0;
    public synchronized int selectNode(List<Integer> availableNodes) {
        if (availableNodes.isEmpty()) return -1;
        int selected = availableNodes.get(currentIndex % availableNodes.size());
        currentIndex = (currentIndex + 1) % availableNodes.size();
        return selected;
    }
}

class ResponseTimeBalancer extends LoadBalancer {
    public synchronized int selectNode(List<Integer> availableNodes) {
        int selectedNode = availableNodes.get(0);
        double minScore = Double.MAX_VALUE;
        for (int nodeId : availableNodes) {
            NodeMetrics metrics = nodeMetrics.get(nodeId);
            double score = metrics.getAvgResponseTimeMs() * (1.0 + 0.2 * metrics.getActiveTasks());
            if (score < minScore) { minScore = score; selectedNode = nodeId; }
        }
        return selectedNode;
    }
}`
  },
  {
    name: 'NodeMetrics.java',
    code: `public class NodeMetrics {
    private final int nodeId;
    private int activeTasks;
    private int completedTasks;
    private double avgResponseTimeMs;
    private int capacity;

    public void recordTaskCompletion(long executionTimeMs) {
        activeTasks = Math.max(0, activeTasks - 1);
        completedTasks++;
        avgResponseTimeMs = 0.7 * avgResponseTimeMs + 0.3 * executionTimeMs;
    }
}`
  }
];

export default function Exp6Module() {
  const [activeTab, setActiveTab] = useState('sim'); // 'sim', 'balancers', 'ppt', 'code'

  // Tab 1: Quorum Split-Brain State
  const [partitionScenario, setPartitionScenario] = useState('baseline'); // 'baseline' or 'partition'
  const [quorumLogs, setQuorumLogs] = useState([
    '[INIT] Cluster initialized with 5 nodes. Quorum threshold Q = 3.',
    '[LEADER] Node 5 elected Leader via Bully Algorithm.'
  ]);

  // Tab 2: Load Balancers State
  const [selectedAlgo, setSelectedAlgo] = useState('responsetime');
  const [balancerNodes, setBalancerNodes] = useState([
    { id: 1, capacity: 1, activeTasks: 0, completed: 0, avgRespMs: 120, alive: true },
    { id: 2, capacity: 2, activeTasks: 0, completed: 0, avgRespMs: 150, alive: true },
    { id: 3, capacity: 3, activeTasks: 0, completed: 0, avgRespMs: 180, alive: true },
    { id: 4, capacity: 4, activeTasks: 0, completed: 0, avgRespMs: 200, alive: true },
    { id: 5, capacity: 5, activeTasks: 0, completed: 0, avgRespMs: 140, alive: true }
  ]);
  const [rrIndex, setRrIndex] = useState(0);
  const [lbLogs, setLbLogs] = useState(['[LOAD BALANCER] System ready. Select an algorithm and dispatch task bursts.']);
  const [totalDispatched, setTotalDispatched] = useState(0);

  // Quorum simulation handlers
  const handleTopologyChange = (type) => {
    setPartitionScenario(type);
    if (type === 'partition') {
      setQuorumLogs((prev) => [
        '[PARTITION DETECTED] Network Split! Minority Partition {1,2} (2 nodes) vs Majority Partition {3,4,5} (3 nodes).',
        '[WATCHDOG {1,2}] Minority partition reachable count = 2 (< Q=3 threshold). Election REFUSED!',
        '[WATCHDOG {3,4,5}] Majority partition reachable count = 3 (>= Q=3 threshold). Bully election triggered.',
        '[ELECTION SUCCESS] Node 5 elected Leader in Majority partition. Split-Brain PREVENTED!',
        ...prev
      ]);
    } else {
      setQuorumLogs((prev) => [
        '[TOPOLOGY RESTORED] Network partition healed. All 5 nodes fully connected.',
        '[CLUSTER] Node 5 remains active cluster Leader.',
        ...prev
      ]);
    }
  };

  // Load Balancer dispatcher logic
  const dispatchTaskBurst = (count = 1) => {
    const aliveNodes = balancerNodes.filter((n) => n.alive);
    if (aliveNodes.length === 0) {
      setLbLogs((prev) => ['[ERROR] No alive nodes available to receive tasks!', ...prev]);
      return;
    }

    setBalancerNodes((prevNodes) => {
      let nextNodes = [...prevNodes];
      let updatedLog = [];

      for (let c = 0; c < count; c++) {
        let chosenNodeId = -1;

        if (selectedAlgo === 'roundrobin') {
          const selected = aliveNodes[c % aliveNodes.length];
          chosenNodeId = selected.id;
        } else if (selectedAlgo === 'leastconn') {
          let minConn = Infinity;
          aliveNodes.forEach((n) => {
            if (n.activeTasks < minConn) {
              minConn = n.activeTasks;
              chosenNodeId = n.id;
            }
          });
        } else if (selectedAlgo === 'weightedrr') {
          // Weighted node selection
          let pool = [];
          aliveNodes.forEach((n) => {
            for (let w = 0; w < n.capacity; w++) pool.push(n.id);
          });
          chosenNodeId = pool[Math.floor(Math.random() * pool.length)];
        } else if (selectedAlgo === 'random') {
          chosenNodeId = aliveNodes[Math.floor(Math.random() * aliveNodes.length)].id;
        } else if (selectedAlgo === 'responsetime') {
          let minScore = Infinity;
          aliveNodes.forEach((n) => {
            const score = n.avgRespMs * (1.0 + 0.2 * n.activeTasks);
            if (score < minScore) {
              minScore = score;
              chosenNodeId = n.id;
            }
          });
        }

        if (chosenNodeId !== -1) {
          nextNodes = nextNodes.map((n) => {
            if (n.id === chosenNodeId) {
              const simExecTime = 80 + Math.floor(Math.random() * 200);
              const newAvg = Math.round(0.7 * n.avgRespMs + 0.3 * simExecTime);
              return {
                ...n,
                activeTasks: n.activeTasks + 1,
                completed: n.completed + 1,
                avgRespMs: newAvg
              };
            }
            return n;
          });
          updatedLog.push(`[DISPATCH] Task #${totalDispatched + c + 1} -> Node ${chosenNodeId} [${selectedAlgo.toUpperCase()}]`);
        }
      }

      setLbLogs((prev) => [...updatedLog.reverse(), ...prev.slice(0, 15)]);
      return nextNodes;
    });

    setTotalDispatched((d) => d + count);
  };

  const resetBalancers = () => {
    setBalancerNodes([
      { id: 1, capacity: 1, activeTasks: 0, completed: 0, avgRespMs: 120, alive: true },
      { id: 2, capacity: 2, activeTasks: 0, completed: 0, avgRespMs: 150, alive: true },
      { id: 3, capacity: 3, activeTasks: 0, completed: 0, avgRespMs: 180, alive: true },
      { id: 4, capacity: 4, activeTasks: 0, completed: 0, avgRespMs: 200, alive: true },
      { id: 5, capacity: 5, activeTasks: 0, completed: 0, avgRespMs: 140, alive: true }
    ]);
    setTotalDispatched(0);
    setLbLogs(['[RESET] Metrics and load balancer state reset.']);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header Banner */}
      <div className="glass-card" style={{ padding: '1.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
            <div style={{
              width: '44px',
              height: '44px',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, #6366f1, #38bdf8)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Scale size={24} color="#ffffff" />
            </div>
            <div>
              <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: '#f8fafc' }}>
                Experiment 6: Quorum Split-Brain Prevention & Load Balancing
              </h2>
              <p style={{ fontSize: '0.85rem', color: '#94a3b8' }}>
                Full Multi-Threaded Java Simulation & Interactive Load Balancer Benchmarks
              </p>
            </div>
          </div>

          <span className="badge badge-indigo">N=5 Nodes | Q=3 Quorum</span>
        </div>

        {/* Tab Navigation Buttons */}
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1.25rem', borderTop: '1px solid rgba(51, 65, 85, 0.4)', paddingTop: '1rem' }}>
          <button
            className={activeTab === 'sim' ? 'btn-primary' : 'btn-secondary'}
            onClick={() => setActiveTab('sim')}
            style={{ fontSize: '0.82rem' }}
          >
            <ShieldCheck size={15} /> Quorum & Split-Brain Simulator
          </button>
          <button
            className={activeTab === 'balancers' ? 'btn-primary' : 'btn-secondary'}
            onClick={() => setActiveTab('balancers')}
            style={{ fontSize: '0.82rem' }}
          >
            <Activity size={15} /> Live 5 Load Balancers Simulator
          </button>
          <button
            className={activeTab === 'ppt' ? 'btn-primary' : 'btn-secondary'}
            onClick={() => setActiveTab('ppt')}
            style={{ fontSize: '0.82rem' }}
          >
            <FileText size={15} /> Presentation Deck Viewer
          </button>
          <button
            className={activeTab === 'code' ? 'btn-primary' : 'btn-secondary'}
            onClick={() => setActiveTab('code')}
            style={{ fontSize: '0.82rem' }}
          >
            <Code size={15} /> Java Source Code Explorer
          </button>
        </div>
      </div>

      {/* TAB 1: QUORUM & SPLIT-BRAIN SIMULATOR */}
      {activeTab === 'sim' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div className="glass-card" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.75rem' }}>
              Network Topology Control & Quorum Verification
            </h3>
            <p style={{ fontSize: '0.88rem', color: '#94a3b8', marginBottom: '1.25rem' }}>
              Select a cluster topology scenario to simulate how the Quorum Threshold rule ($Q = \lfloor 5/2 \rfloor + 1 = 3$) prevents dual-leader Split-Brain condition.
            </p>

            <div style={{ display: 'flex', gap: '1rem' }}>
              <button
                className={partitionScenario === 'baseline' ? 'btn-primary' : 'btn-secondary'}
                onClick={() => handleTopologyChange('baseline')}
              >
                Fully Connected Topology (Baseline)
              </button>
              <button
                className={partitionScenario === 'partition' ? 'btn-primary' : 'btn-secondary'}
                onClick={() => handleTopologyChange('partition')}
                style={{ background: partitionScenario === 'partition' ? 'linear-gradient(135deg, #f43f5e, #e11d48)' : undefined }}
              >
                Simulate Network Partition ({'{1,2}'} vs {'{3,4,5}'})
              </button>
            </div>
          </div>

          {/* Topology Visualization */}
          <div style={{ display: 'grid', gridTemplateColumns: partitionScenario === 'partition' ? '1fr 1fr' : '1fr', gap: '1.25rem' }}>
            {partitionScenario === 'partition' ? (
              <>
                <div className="glass-card" style={{ padding: '1.25rem', border: '1px solid rgba(244, 63, 94, 0.6)', background: 'rgba(244, 63, 94, 0.05)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                    <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#f43f5e' }}>Minority Partition {'{Node 1, Node 2}'}</h4>
                    <span className="badge badge-rose"><ShieldAlert size={12} /> ELECTION REFUSED</span>
                  </div>
                  <p style={{ fontSize: '0.84rem', color: '#94a3b8', marginBottom: '0.75rem' }}>
                    Reachable Nodes: 2 | Quorum Threshold: 3  ➔  (2 &lt; 3)
                  </p>
                  <div style={{ background: '#090d16', padding: '0.8rem', borderRadius: '8px', fontSize: '0.8rem', color: '#f43f5e', fontFamily: 'var(--font-mono)' }}>
                    Watchdog: Quorum NOT satisfied. Refusing election to prevent split-brain.
                  </div>
                </div>

                <div className="glass-card" style={{ padding: '1.25rem', border: '1px solid rgba(52, 211, 153, 0.6)', background: 'rgba(52, 211, 153, 0.05)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                    <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#34d399' }}>Majority Partition {'{Node 3, Node 4, Node 5}'}</h4>
                    <span className="badge badge-emerald"><ShieldCheck size={12} /> QUORUM SATISFIED</span>
                  </div>
                  <p style={{ fontSize: '0.84rem', color: '#94a3b8', marginBottom: '0.75rem' }}>
                    Reachable Nodes: 3 | Quorum Threshold: 3  ➔  (3 &ge; 3)
                  </p>
                  <div style={{ background: '#090d16', padding: '0.8rem', borderRadius: '8px', fontSize: '0.8rem', color: '#34d399', fontFamily: 'var(--font-mono)' }}>
                    Watchdog: Quorum verified. Node 5 elected Leader via Bully Algorithm!
                  </div>
                </div>
              </>
            ) : (
              <div className="glass-card" style={{ padding: '1.5rem', border: '1px solid rgba(99, 102, 241, 0.5)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                  <h4 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc' }}>Fully Connected 5-Node Cluster</h4>
                  <span className="badge badge-indigo">Node 5 (Leader) Active</span>
                </div>
                <p style={{ fontSize: '0.88rem', color: '#94a3b8' }}>
                  All 5 nodes communicate over UDP ports 5001–5005. Node 5 acts as cluster Leader and Load Balancer.
                </p>
              </div>
            )}
          </div>

          {/* Quorum Event Log */}
          <div className="glass-card" style={{ padding: '1.25rem' }}>
            <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.75rem' }}>
              Quorum & Watchdog Event Log
            </h3>
            <div style={{ background: '#090d16', borderRadius: '8px', padding: '1rem', fontFamily: 'var(--font-mono)', fontSize: '0.84rem', color: '#38bdf8', maxHeight: '200px', overflowY: 'auto' }}>
              {quorumLogs.map((log, i) => (
                <div key={i}>{log}</div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: LIVE 5 LOAD BALANCERS SIMULATOR */}
      {activeTab === 'balancers' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div className="glass-card" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.75rem' }}>
              Select Load Balancing Algorithm & Dispatch Workload
            </h3>

            {/* Algorithm Selector */}
            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '1.25rem' }}>
              {[
                { id: 'roundrobin', label: 'Round Robin (RR)' },
                { id: 'leastconn', label: 'Least Connections (LC)' },
                { id: 'weightedrr', label: 'Weighted Round Robin (WRR)' },
                { id: 'random', label: 'Random (RAND)' },
                { id: 'responsetime', label: 'Response Time (Adaptive EMA)' }
              ].map((algo) => (
                <button
                  key={algo.id}
                  className={selectedAlgo === algo.id ? 'btn-primary' : 'btn-secondary'}
                  onClick={() => setSelectedAlgo(algo.id)}
                  style={{ fontSize: '0.8rem', padding: '0.4rem 0.8rem' }}
                >
                  {algo.label}
                </button>
              ))}
            </div>

            {/* Dispatch Buttons */}
            <div style={{ display: 'flex', gap: '1rem' }}>
              <button className="btn-primary" onClick={() => dispatchTaskBurst(1)}>
                <Play size={15} /> Dispatch 1 Task
              </button>
              <button className="btn-primary" onClick={() => dispatchTaskBurst(10)}>
                <Zap size={15} /> Dispatch Burst of 10 Tasks
              </button>
              <button className="btn-secondary" onClick={resetBalancers}>
                <RefreshCw size={15} /> Reset Metrics
              </button>
            </div>
          </div>

          {/* Node Metrics Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: '1rem' }}>
            {balancerNodes.map((n) => (
              <div key={n.id} className="glass-card" style={{ padding: '1.25rem', border: '1px solid rgba(51, 65, 85, 0.6)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                  <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#f8fafc' }}>Worker Node {n.id}</h4>
                  <span className="badge badge-cyan">Weight: {n.capacity}</span>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  <div>
                    <span style={{ fontSize: '0.72rem', color: '#64748b' }}>Active Tasks:</span>
                    <p style={{ fontSize: '1.4rem', fontWeight: 800, color: '#38bdf8', fontFamily: 'var(--font-mono)' }}>
                      {n.activeTasks}
                    </p>
                  </div>

                  <div>
                    <span style={{ fontSize: '0.72rem', color: '#64748b' }}>Total Completed:</span>
                    <p style={{ fontSize: '1.1rem', fontWeight: 700, color: '#34d399', fontFamily: 'var(--font-mono)' }}>
                      {n.completed}
                    </p>
                  </div>

                  <div>
                    <span style={{ fontSize: '0.72rem', color: '#64748b' }}>EMA Response Time:</span>
                    <p style={{ fontSize: '1.05rem', fontWeight: 700, color: '#fbbf24', fontFamily: 'var(--font-mono)' }}>
                      {n.avgRespMs} ms
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Load Balancer Log */}
          <div className="glass-card" style={{ padding: '1.25rem' }}>
            <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.75rem' }}>
              Load Balancer Dispatch Log (Total Dispatched: {totalDispatched})
            </h3>
            <div style={{ background: '#090d16', borderRadius: '8px', padding: '1rem', fontFamily: 'var(--font-mono)', fontSize: '0.84rem', color: '#6366f1', maxHeight: '200px', overflowY: 'auto' }}>
              {lbLogs.map((log, i) => (
                <div key={i}>{log}</div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: PRESENTATION DECK VIEWER */}
      {activeTab === 'ppt' && <PresentationViewer />}

      {/* TAB 4: CODE & ARCHITECTURE EXPLORER */}
      {activeTab === 'code' && <CodeViewer files={EXP6_SOURCE_FILES} />}
    </div>
  );
}
