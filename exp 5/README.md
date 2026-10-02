# Experiment 5: Quorum-Based Split-Brain Preventing Leader Election

## Distributed Computing Laboratory

---

## 1. Problem Statement: The Split-Brain Dilemma

In distributed clusters (such as ZooKeeper, etcd, Raft, or Elasticsearch), a designated **Coordinator / Leader** is responsible for sequencing transactions, serializing writes, and managing cluster metadata.

When a **network partition** occurs (e.g., switches fail, routers drop packets, or links are severed), the cluster can be fractured into two or more mutually unreachable sub-clusters (e.g. Partition A with nodes $\{1, 2\}$ and Partition B with nodes $\{3, 4, 5\}$).

### The Catastrophe of Naive Election:
In naive election algorithms (such as unconstrained Bully or Ring elections):
1. Nodes in Partition A lose contact with the old leader and elect Node 2.
2. Nodes in Partition B lose contact with the old leader and elect Node 5.
3. **Split-Brain Condition**: The cluster now has **two concurrent leaders**. Both accept conflicting client writes and mutate state independently. When the partition heals, data stores have diverged irreparably, leading to **silent data corruption and lost updates**.

---

## 2. Mathematical Solution: Strict Majority Quorum

To provably prevent split-brain, an election protocol must enforce the **Strict Majority Quorum Rule**:

$$Q = \left\lfloor \frac{N}{2} \right\rfloor + 1$$

For a 5-node cluster ($N=5$):

$$Q = \left\lfloor \frac{5}{2} \right\rfloor + 1 = 2 + 1 = 3$$

### Mathematical Invariant (Pigeonhole Overlap Proof):
Suppose Partition A has size $|A|$ and Partition B has size $|B|$, where $|A| + |B| = N$.
For both partitions to elect a leader simultaneously, both would require at least $Q$ votes:

$$|A| \ge Q \quad \text{and} \quad |B| \ge Q$$

Summing both requirements:

$$|A| + |B| \ge 2Q = 2 \left( \left\lfloor \frac{N}{2} \right\rfloor + 1 \right) > N$$

This contradicts $|A| + |B| = N$. Therefore, **it is mathematically impossible for two independent partitions to both obtain a majority quorum simultaneously**.

- **Majority Partition ($\{3, 4, 5\}$)**: Size $3 \ge 3 \implies$ **ELECTION PERMITTED**.
- **Minority Partition ($\{1, 2\}$)**: Size $2 < 3 \implies$ **ELECTION REFUSED (FENCED)**.

---

## 3. Node State Machine & Protection Mechanisms

Each node runs as an independent daemon (`QuorumElectionNode.java`) with four mutually exclusive states:

```text
               +-----------------------------+
               |          FOLLOWER           |
               +-----------------------------+
                   |                     ^
  Watchdog Timeout | (Reachable >= 3)    | Valid Heartbeat
  (Reachable >= 3) |                     | Received
                   v                     |
               +-----------------------------+
               |          CANDIDATE          |
               +-----------------------------+
                   |                     |
     Votes >= 3    |                     | Votes < 3 (Quorum Failed)
  (Majority Won)   |                     |
                   v                     v
+--------------------+        +-----------------------+
|       LEADER       |        |    FENCED_MINORITY    |
+--------------------+        +-----------------------+
   |        |                     ^
   |        +--- Lost Quorum -----+
   |             (Reachable < 3)
   |
   +---> Valid Quorum Lease: Executes Client Workloads
```

### Protection Mechanisms Implemented:
1. **Pre-Election Quorum Check**: Before even starting an election, a node verifies that it can contact at least $Q=3$ nodes. If not, it enters `FENCED_MINORITY` immediately without spamming network vote requests.
2. **Strict Vote Threshold**: A candidate must collect $\ge 3$ granted votes within the election window. If it collects only 1 or 2 votes, leadership is explicitly refused.
3. **Active Leader Quorum Lease**: A running leader tracks heartbeat acknowledgments from followers. If the leader loses contact with a majority of nodes (e.g. it is trapped on the minority side of a partition), it **immediately steps down** to `FENCED_MINORITY` to prevent accepting rogue writes.
4. **Workload Fencing Guard**: Clients submitting tasks (`WORKLOAD_REQ`) to any node in `FENCED_MINORITY` receive an immediate rejection: `REJECTED_SPLIT_BRAIN_FENCED_MINORITY`.
5. **Term / Epoch Monotonicity**: Every message carries a monotonically increasing `term`. Nodes immediately demote if a higher term is detected.

---

## 4. Architecture & Networking

- **Transport**: UDP DatagramSockets (`java.net.DatagramSocket`) on `127.0.0.1`.
- **Node Ports**:
  - Node 1: `8001`
  - Node 2: `8002`
  - Node 3: `8003`
  - Node 4: `8004`
  - Node 5: `8005`
- **Driver / CLI Ports**: `8000` (Simulation Driver) / `8099` (Interactive Console).
- **Message Serialization**: Pipe-delimited Base64 string format:
  ```text
  TYPE | senderId | targetId | term | b64(payload) | b64(extra)
  ```

---

## 5. Project Files

```
exp 5/
├── ElectionMessage.java               # Network message framing and packet serialization
├── QuorumElectionNode.java            # Node daemon with quorum state machine & fencing guards
├── SplitBrainSimDriver.java           # Master automated test suite & scenario orchestrator
├── SplitBrainInteractiveConsole.java  # Interactive CLI for manual partition & workload testing
├── build_and_run.sh                   # Build automation and execution script
└── README.md                          # Complete laboratory documentation
```

---

## 6. How to Compile & Run

### Prerequisites
- JDK 17, 21, or higher (`javac` and `java`).
- The `build_and_run.sh` script automatically detects Homebrew OpenJDK 21 or system Java on macOS/Linux.

### Mode 1: Automated Simulation
Runs the full 6-stage automated verification of split-brain prevention and partition healing:
```bash
cd "exp 5"
./build_and_run.sh sim
```
Or directly using Java:
```bash
cd "exp 5"
javac *.java
java SplitBrainSimDriver
```

### Mode 2: Interactive Console
Spawns the 5-node cluster in the background and opens an interactive shell:
```bash
cd "exp 5"
./build_and_run.sh interactive
```

Inside the interactive console:
```text
SplitBrain-CLI> help

Supported Commands:
  status                          - Display state, term, leader, and partitions of all 5 nodes
  partition <grp1> <grp2>         - Partition cluster (e.g., 'partition 1,2 3,4,5')
  heal                            - Clear all partitions and restore full network connectivity
  workload <nodeId> <taskName>    - Submit write task to node (verifies split-brain guard)
  kill <nodeId>                   - Shut down a node to simulate node crash
  help                            - Show this manual
  exit                            - Exit console
```

---

## 7. Automated Simulation Scenarios & Verification

### Stage 0 & 1: Initial Cluster Boot & Election
- All 5 nodes start. Watchdogs detect quorum ($5/5 \ge 3$).
- Node 5 collects 5 votes and is elected Leader for Term 1.
- Leader executes initial test workload `TASK_001`.

### Stage 2: Network Partition Injection
- Partition cut injected: Minority $\{1, 2\}$ vs Majority $\{3, 4, 5\}$.
- All network packets between $\{1, 2\}$ and $\{3, 4, 5\}$ are dropped.

### Stage 3: Split-Brain Prevention in Minority Partition $\{1, 2\}$
- Nodes 1 and 2 lose leader heartbeats.
- Watchdogs fire. Reachable count is only $2/5$ ($< 3$).
- **Leadership Refused**: Node 2 refrains from declaring leadership and transitions to `FENCED_MINORITY`.
- Client write `TX_1002` sent to Node 2 is **rejected** with `FAIL_NO_QUORUM`.
- Invariant verified: **ZERO leaders in minority partition.**

### Stage 4: Majority Health in $\{3, 4, 5\}$
- Majority partition has 3 nodes alive ($3 \ge 3$).
- Leader continues holding valid quorum lease.
- Client write `TX_1003` sent to majority leader is **accepted and executed**.

### Stage 5 & 6: Partition Healing & Reunification
- Partition cleared (`HEAL_PARTITION`).
- All 5 nodes communicate again.
- Minority nodes lift fencing, rejoin cluster, and acknowledge single unified leader.
- 100% split-brain prevention confirmed across all stages.

---

## 8. Comparative Analysis Table

| Metric / Feature | Naive Leader Election (No Quorum) | Quorum-Based Election (Implemented) |
| :--- | :--- | :--- |
| **Quorum Threshold** | None (Highest available node wins) | Strict Majority $Q = \lfloor N/2 \rfloor + 1$ |
| **Symmetric Partition Behavior** | Every partition elects its own leader | Only majority partition can elect a leader |
| **Split-Brain Risk** | **HIGH** (Conflicting writes occur) | **ZERO** (Mathematically impossible) |
| **Minority Sub-cluster State** | Elects rogue leader | Enters `FENCED_MINORITY` state |
| **Client Workload Handling** | Accepted in all partitions | Rejected in minority partitions |
| **Leader Lease Expiration** | Leader operates indefinitely in isolation | Leader steps down if heartbeat ACKs $< Q$ |
| **Cluster Reunification** | Data divergence requires manual repair | Automatic re-unification with single leader |
