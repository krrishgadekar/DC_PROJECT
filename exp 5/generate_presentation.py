import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.enum.text import PP_ALIGN
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE

# 1. Initialize Presentation & 16:9 Widescreen dimensions
prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)

# Color Palette Constants
COLOR_BG = RGBColor(248, 249, 250)         # Soft Off-White
COLOR_PRIMARY = RGBColor(15, 23, 42)       # Dark Slate / Navy
COLOR_ACCENT = RGBColor(14, 116, 144)      # Deep Cyan / Teal
COLOR_DANGER = RGBColor(190, 24, 93)       # Berry / Crimson for split-brain danger
COLOR_SUCCESS = RGBColor(16, 185, 129)     # Emerald Green for quorum safety
COLOR_CARD_BG = RGBColor(255, 255, 255)    # Pure White
COLOR_MUTED = RGBColor(100, 116, 139)      # Muted Slate
COLOR_CODE_BG = RGBColor(30, 41, 59)       # Dark Terminal Slate
COLOR_CODE_TEXT = RGBColor(241, 245, 249)  # Light Terminal Text
COLOR_BORDER = RGBColor(226, 232, 240)     # Light Gray Border

def apply_background(slide):
    background = slide.background
    fill = background.fill
    fill.solid()
    fill.fore_color.rgb = COLOR_BG

def add_header(slide, title_text, subtitle_text="Distributed Computing Laboratory • Experiment 5"):
    title_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.45), Inches(11.733), Inches(1.1))
    tf = title_box.text_frame
    tf.word_wrap = True
    tf.margin_left = tf.margin_top = tf.margin_right = tf.margin_bottom = 0

    p_sub = tf.paragraphs[0]
    p_sub.text = subtitle_text.upper()
    p_sub.font.size = Pt(11)
    p_sub.font.bold = True
    p_sub.font.color.rgb = COLOR_ACCENT
    p_sub.space_after = Pt(4)

    p_title = tf.add_paragraph()
    p_title.text = title_text
    p_title.font.size = Pt(22)
    p_title.font.bold = True
    p_title.font.color.rgb = COLOR_PRIMARY

def add_card(slide, left, top, width, height, bg_color=COLOR_CARD_BG, border_color=COLOR_BORDER):
    shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
    shape.fill.solid()
    shape.fill.fore_color.rgb = bg_color
    if border_color:
        shape.line.color.rgb = border_color
        shape.line.width = Pt(1.2)
    else:
        shape.line.fill.background()
    return shape

slide_layout = prs.slide_layouts[6] # Blank slide layout

# ----------------------------------------------------------------------
# SLIDE 1: Title Slide (Dark Theme)
# ----------------------------------------------------------------------
slide1 = prs.slides.add_slide(slide_layout)
bg1 = slide1.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
bg1.fill.solid()
bg1.fill.fore_color.rgb = COLOR_PRIMARY
bg1.line.fill.background()

tbox = slide1.shapes.add_textbox(Inches(1.0), Inches(1.6), Inches(11.333), Inches(4.8))
tf1 = tbox.text_frame
tf1.word_wrap = True

p = tf1.paragraphs[0]
p.text = "DISTRIBUTED COMPUTING LABORATORY • EXPERIMENT 5"
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = RGBColor(56, 189, 248)
p.space_after = Pt(12)

p = tf1.add_paragraph()
p.text = "Quorum-Based Split-Brain Preventing Leader Election"
p.font.size = Pt(32)
p.font.bold = True
p.font.color.rgb = RGBColor(255, 255, 255)
p.space_after = Pt(10)

p = tf1.add_paragraph()
p.text = "Mathematical Majority Quorum Invariants, Dynamic Network Partitions & Fencing Guards"
p.font.size = Pt(18)
p.font.color.rgb = RGBColor(203, 213, 225)
p.space_after = Pt(36)

p = tf1.add_paragraph()
p.text = "Distributed Systems Implementation • Java UDP Datagram Sockets • N=5 Cluster Simulation"
p.font.size = Pt(13)
p.font.color.rgb = RGBColor(148, 163, 184)

# ----------------------------------------------------------------------
# SLIDE 2: Problem Statement - The Split-Brain Catastrophe
# ----------------------------------------------------------------------
slide2 = prs.slides.add_slide(slide_layout)
apply_background(slide2)
add_header(slide2, "The Split-Brain Problem in Distributed Systems")

# Left Card: The Naive Dilemma
add_card(slide2, Inches(0.8), Inches(1.8), Inches(5.6), Inches(5.1))
tb = slide2.shapes.add_textbox(Inches(1.1), Inches(2.0), Inches(5.0), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Network Partitions & Naive Elections"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_DANGER
p.space_after = Pt(12)

points_l = [
    "Single Leader Architecture: Distributed clusters rely on a primary coordinator to serialize writes and maintain consistency.",
    "Network Partitions: Switches, routers, or VM link failures partition the cluster into disconnected sub-graphs (e.g. {1, 2} vs {3, 4, 5}).",
    "Naive Election Failure: In traditional Bully or Ring algorithms, BOTH disconnected groups detect leader absence and elect their own leader.",
    "Dual Active Leaders: Partition A elects Node 2; Partition B elects Node 5.",
    "Catastrophic Split-Brain: Both leaders accept conflicting writes simultaneously, causing irreversible data divergence."
]
for pt in points_l:
    p = tf.add_paragraph()
    p.text = "• " + pt
    p.font.size = Pt(11.5)
    p.font.color.rgb = COLOR_MUTED
    p.space_after = Pt(8)

# Right Card: The Core Objective
add_card(slide2, Inches(6.8), Inches(1.8), Inches(5.7), Inches(5.1))
tb = slide2.shapes.add_textbox(Inches(7.1), Inches(2.0), Inches(5.1), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Experiment 5 Objective & Solution"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_ACCENT
p.space_after = Pt(12)

points_r = [
    "Strict Majority Quorum Enforcement: Mandate that no leader can ever be elected without votes from a strict majority: Q = floor(N/2) + 1.",
    "Minority Fencing (FENCED_MINORITY): Any sub-cluster with fewer than Q reachable nodes explicitly refuses election and rejects writes.",
    "Active Leader Quorum Lease: Leaders continuously monitor heartbeat acknowledgments; if ACKs drop below Q, the leader immediately steps down.",
    "Client Workload Protection: Uncompromised adherence to the CAP theorem (preferring Consistency over Availability under partitions).",
    "Dynamic Partition Healing: Clean cluster re-unification under a single leader when network connectivity is restored."
]
for pt in points_r:
    p = tf.add_paragraph()
    p.text = "• " + pt
    p.font.size = Pt(11.5)
    p.font.color.rgb = COLOR_MUTED
    p.space_after = Pt(8)

# ----------------------------------------------------------------------
# SLIDE 3: Mathematical Proof - Pigeonhole Quorum Overlap
# ----------------------------------------------------------------------
slide3 = prs.slides.add_slide(slide_layout)
apply_background(slide3)
add_header(slide3, "Mathematical Foundation: Strict Majority Quorum Invariant")

# Left Card: Formula & Invariant
add_card(slide3, Inches(0.8), Inches(1.8), Inches(5.6), Inches(5.1))
tb = slide3.shapes.add_textbox(Inches(1.1), Inches(2.0), Inches(5.0), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "The Majority Quorum Formula"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY
p.space_after = Pt(12)

p = tf.add_paragraph()
p.text = "For a distributed cluster of N nodes, the minimum quorum Q required to validate an election is:"
p.font.size = Pt(12)
p.font.color.rgb = COLOR_MUTED
p.space_after = Pt(10)

p = tf.add_paragraph()
p.text = "Q = floor(N / 2) + 1"
p.font.size = Pt(18)
p.font.bold = True
p.font.color.rgb = COLOR_ACCENT
p.space_after = Pt(14)

points_math = [
    "For N = 5 Nodes: Q = floor(5/2) + 1 = 2 + 1 = 3 nodes.",
    "Minority Partition {1, 2}: Size = 2 < 3 → Quorum FAILED.",
    "Majority Partition {3, 4, 5}: Size = 3 >= 3 → Quorum SATISFIED.",
    "Tolerance: A 5-node cluster tolerates up to F = floor((N-1)/2) = 2 node failures while maintaining safety."
]
for pt in points_math:
    p = tf.add_paragraph()
    p.text = "• " + pt
    p.font.size = Pt(11.5)
    p.font.color.rgb = COLOR_MUTED
    p.space_after = Pt(8)

# Right Card: The Pigeonhole Principle Overlap Proof
add_card(slide3, Inches(6.8), Inches(1.8), Inches(5.7), Inches(5.1))
tb = slide3.shapes.add_textbox(Inches(7.1), Inches(2.0), Inches(5.1), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Pigeonhole Overlap Proof (Zero Split-Brain)"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_SUCCESS
p.space_after = Pt(12)

proof_steps = [
    "Theorem: Two independent partitions can NEVER simultaneously achieve a majority quorum.",
    "Proof by Contradiction:",
    "  1. Let cluster be partitioned into two disjoint sets A and B, where |A| + |B| = N.",
    "  2. Assume both Partition A and Partition B elect a leader.",
    "  3. Both must hold at least Q votes: |A| >= Q and |B| >= Q.",
    "  4. Summing both inequalities:",
    "     |A| + |B| >= 2Q = 2(floor(N/2) + 1) > N",
    "  5. This contradicts |A| + |B| = N!",
    "Conclusion: Split-brain is mathematically impossible when Q = floor(N/2) + 1 is enforced."
]
for step in proof_steps:
    p = tf.add_paragraph()
    p.text = step
    p.font.size = Pt(11)
    if "Theorem" in step or "Conclusion" in step:
        p.font.bold = True
        p.font.color.rgb = COLOR_PRIMARY
    else:
        p.font.color.rgb = COLOR_MUTED
    p.space_after = Pt(4)

# ----------------------------------------------------------------------
# SLIDE 4: Distributed State Machine & Node Architecture
# ----------------------------------------------------------------------
slide4 = prs.slides.add_slide(slide_layout)
apply_background(slide4)
add_header(slide4, "Distributed Node Architecture & Four-State Machine")

# 4 Cards for the 4 States
states = [
    ("FOLLOWER", COLOR_PRIMARY, "Default node state.", [
        "Awaits periodic heartbeats from Leader.",
        "Monitors watchdog timer (1800-2600ms randomized).",
        "Grants at most 1 vote per Term to candidates.",
        "Steps down on seeing higher Term."
    ]),
    ("CANDIDATE", COLOR_ACCENT, "Election initiator.", [
        "Increments monotonic Term (E = E + 1).",
        "Self-votes (1 vote) and broadcasts REQUEST_VOTE.",
        "Gathers votes over UDP vote socket.",
        "Transitions to LEADER only if tally >= 3."
    ]),
    ("LEADER", COLOR_SUCCESS, "Cluster coordinator.", [
        "Broadcasts periodic HEARTBEAT every 800ms.",
        "Tracks active follower ACKs (Lease check).",
        "Executes authorized client write workloads.",
        "Steps down if active ACKs drop below Quorum."
    ]),
    ("FENCED_MINORITY", COLOR_DANGER, "Split-brain guard state.", [
        "Entered when reachable nodes < Quorum (3).",
        "Refuses to start election in minority.",
        "Rejects client write workloads with error.",
        "Restores to FOLLOWER when partition heals."
    ])
]

lefts = [0.8, 3.8, 6.8, 9.8]
width = 2.7
for i, (st_name, st_color, st_desc, st_pts) in enumerate(states):
    add_card(slide4, Inches(lefts[i]), Inches(1.8), Inches(width), Inches(5.1))
    tb = slide4.shapes.add_textbox(Inches(lefts[i] + 0.15), Inches(2.0), Inches(width - 0.3), Inches(4.7))
    tf = tb.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.text = st_name
    p.font.size = Pt(15)
    p.font.bold = True
    p.font.color.rgb = st_color
    p.space_after = Pt(4)

    p_sub = tf.add_paragraph()
    p_sub.text = st_desc
    p_sub.font.size = Pt(10)
    p_sub.font.italic = True
    p_sub.font.color.rgb = COLOR_MUTED
    p_sub.space_after = Pt(12)

    for pt in st_pts:
        p = tf.add_paragraph()
        p.text = "• " + pt
        p.font.size = Pt(10.5)
        p.font.color.rgb = COLOR_PRIMARY
        p.space_after = Pt(6)

# ----------------------------------------------------------------------
# SLIDE 5: Network Protocol & Message Framing
# ----------------------------------------------------------------------
slide5 = prs.slides.add_slide(slide_layout)
apply_background(slide5)
add_header(slide5, "Communication Protocol & UDP Message Framing")

# Left Card: Protocol Overview
add_card(slide5, Inches(0.8), Inches(1.8), Inches(5.6), Inches(5.1))
tb = slide5.shapes.add_textbox(Inches(1.1), Inches(2.0), Inches(5.0), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Networking & Transport Architecture"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY
p.space_after = Pt(12)

net_points = [
    "Transport Layer: UDP DatagramSockets on 127.0.0.1 (IPv4).",
    "Port Allocations:",
    "  • Driver Coordinator: Port 8000",
    "  • Interactive CLI Console: Port 8099",
    "  • Cluster Nodes 1 to 5: Ports 8001 through 8005",
    "Non-blocking Concurrency: Cached thread pool workers + ScheduledExecutorService for heartbeats and watchdogs.",
    "Dynamic Partitioning: In-memory Set<Integer> blockedPeers drops packets silently to simulate physical fiber cuts."
]
for pt in net_points:
    p = tf.add_paragraph()
    p.text = pt
    p.font.size = Pt(11.5)
    p.font.color.rgb = COLOR_MUTED
    p.space_after = Pt(8)

# Right Card: Packet Format
add_card(slide5, Inches(6.8), Inches(1.8), Inches(5.7), Inches(5.1), bg_color=COLOR_CODE_BG)
tb = slide5.shapes.add_textbox(Inches(7.1), Inches(2.0), Inches(5.1), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Pipe-Delimited Base64 Packet Framing"
p.font.size = Pt(15)
p.font.bold = True
p.font.color.rgb = RGBColor(56, 189, 248)
p.space_after = Pt(12)

code_lines = [
    "// Serialized Packet Wire Format",
    "TYPE | senderId | targetId | term | b64(payload) | b64(extra)",
    "",
    "Core Protocol Message Types:",
    "• REQUEST_VOTE     : Candidate solicits majority votes",
    "• VOTE_GRANT       : Peer votes for candidate in Term",
    "• VOTE_REJECT      : Peer denies vote (lower term / voted)",
    "• COORDINATOR      : Leader announces majority consensus",
    "• HEARTBEAT        : Leader proves active lease to followers",
    "• HEARTBEAT_ACK    : Follower confirms presence for leader",
    "• WORKLOAD_REQ     : Client submits task to node",
    "• WORKLOAD_RESP    : Success or FENCED_MINORITY rejection",
    "• SET_PARTITION    : Dynamically isolate target peers",
    "• HEAL_PARTITION   : Clear blocked peers and re-unify"
]
for line in code_lines:
    p = tf.add_paragraph()
    p.text = line
    p.font.size = Pt(10.5)
    p.font.name = "Courier New"
    if line.startswith("//") or "Core Protocol" in line:
        p.font.color.rgb = RGBColor(148, 163, 184)
        p.font.bold = True
    elif line.startswith("TYPE"):
        p.font.color.rgb = RGBColor(253, 224, 71)
    else:
        p.font.color.rgb = COLOR_CODE_TEXT
    p.space_after = Pt(2)

# ----------------------------------------------------------------------
# SLIDE 6: Simulation Walkthrough - Partition & Split-Brain Prevention
# ----------------------------------------------------------------------
slide6 = prs.slides.add_slide(slide_layout)
apply_background(slide6)
add_header(slide6, "Simulation Phase: Network Partition Cut ({1, 2} vs {3, 4, 5})")

# Left Card: Minority Behavior
add_card(slide6, Inches(0.8), Inches(1.8), Inches(5.6), Inches(5.1))
tb = slide6.shapes.add_textbox(Inches(1.1), Inches(2.0), Inches(5.0), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Minority Partition {1, 2} (FENCED)"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_DANGER
p.space_after = Pt(12)

min_steps = [
    "Partition Injected: Nodes 1 & 2 lose connectivity to Nodes 3, 4, 5.",
    "Watchdog Fires: Leader Node 5 heartbeats stop arriving.",
    "Pre-Election Quorum Check: Reachable count = 2 nodes (< Quorum 3).",
    "Leadership Refused: Nodes 1 & 2 explicitly refuse to elect a leader.",
    "Fencing Activated: Nodes transition to State: FENCED_MINORITY.",
    "Client Write Guard: Client write 'TX_1002' to Node 2 is REJECTED with FAIL_NO_QUORUM.",
    "Invariant Verified: ZERO rogue leaders elected in minority partition!"
]
for pt in min_steps:
    p = tf.add_paragraph()
    p.text = "• " + pt
    p.font.size = Pt(11.5)
    p.font.color.rgb = COLOR_MUTED
    p.space_after = Pt(8)

# Right Card: Majority Behavior
add_card(slide6, Inches(6.8), Inches(1.8), Inches(5.7), Inches(5.1))
tb = slide6.shapes.add_textbox(Inches(7.1), Inches(2.0), Inches(5.1), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Majority Partition {3, 4, 5} (ACTIVE)"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_SUCCESS
p.space_after = Pt(12)

maj_steps = [
    "Majority Cluster Present: Nodes 3, 4, and 5 can communicate freely.",
    "Quorum Check: Reachable count = 3 nodes (>= Quorum threshold 3).",
    "Election Executed: Candidate gathers 3 votes: [3, 4, 5].",
    "Consensus Proclaimed: Node 4 declared Leader for Term 2 with majority quorum.",
    "Active Quorum Lease: Leader receives periodic HEARTBEAT_ACK from followers.",
    "Client Write Execution: Client write 'TX_1003' to Node 4 is ACCEPTED and COMMITTED.",
    "CAP Maintained: Consistency guaranteed while preserving partition tolerance."
]
for pt in maj_steps:
    p = tf.add_paragraph()
    p.text = "• " + pt
    p.font.size = Pt(11.5)
    p.font.color.rgb = COLOR_MUTED
    p.space_after = Pt(8)

# ----------------------------------------------------------------------
# SLIDE 7: Partition Healing & Cluster Re-unification
# ----------------------------------------------------------------------
slide7 = prs.slides.add_slide(slide_layout)
apply_background(slide7)
add_header(slide7, "Dynamic Partition Healing & Seamless Re-unification")

# Left Card: Step-by-Step Healing
add_card(slide7, Inches(0.8), Inches(1.8), Inches(5.6), Inches(5.1))
tb = slide7.shapes.add_textbox(Inches(1.1), Inches(2.0), Inches(5.0), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Healing & Re-unification Steps"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY
p.space_after = Pt(12)

heal_steps = [
    "1. Driver Dispatches HEAL_PARTITION: Blocked peer sets on all 5 nodes are cleared.",
    "2. Leader Heartbeat Propagation: Leader Node 4 heartbeats reach formerly fenced Nodes 1 & 2.",
    "3. Term Advance & Fencing Lifted: Nodes 1 & 2 observe higher Term 2, un-fence, and adopt FOLLOWER state.",
    "4. Leader Recognition: All 5 nodes recognize Node 4 as the sole legitimate coordinator.",
    "5. Zero Data Reconciliation: Because minority never accepted rogue writes, zero manual rollback is needed!"
]
for pt in heal_steps:
    p = tf.add_paragraph()
    p.text = pt
    p.font.size = Pt(11.5)
    p.font.color.rgb = COLOR_MUTED
    p.space_after = Pt(8)

# Right Card: Terminal Output Snapshot
add_card(slide7, Inches(6.8), Inches(1.8), Inches(5.7), Inches(5.1), bg_color=COLOR_CODE_BG)
tb = slide7.shapes.add_textbox(Inches(7.1), Inches(2.0), Inches(5.1), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Live Execution Log: Stage 5 & 6 Verification"
p.font.size = Pt(14)
p.font.bold = True
p.font.color.rgb = RGBColor(56, 189, 248)
p.space_after = Pt(10)

terminal_logs = [
    "[DRIVER] [HEAL] Partition cleared. Restoring connectivity...",
    "[Node 1] [PARTITION_HEALED] Full connectivity restored.",
    "[Node 2] [TERM_ADVANCE] Higher Term 2 seen. Demoting to FOLLOWER.",
    "",
    "STAGE 6: POST-HEALING UNIFIED CLUSTER VERIFICATION",
    "[DRIVER] Node 1: State=FOLLOWER | Term=2 | Leader=4",
    "[DRIVER] Node 2: State=FOLLOWER | Term=2 | Leader=4",
    "[DRIVER] Node 3: State=FOLLOWER | Term=2 | Leader=4",
    "[DRIVER] Node 4: State=LEADER   | Term=2 | Leader=4",
    "[DRIVER] Node 5: State=FOLLOWER | Term=2 | Leader=4",
    "",
    ">>> INVARIANT VERIFIED: Exactly ONE Leader (Node 4) across all 5 nodes."
]
for line in terminal_logs:
    p = tf.add_paragraph()
    p.text = line
    p.font.size = Pt(9.5)
    p.font.name = "Courier New"
    if "STAGE 6" in line or "INVARIANT" in line:
        p.font.bold = True
        p.font.color.rgb = RGBColor(74, 222, 128)
    elif "[DRIVER]" in line:
        p.font.color.rgb = RGBColor(253, 224, 71)
    else:
        p.font.color.rgb = COLOR_CODE_TEXT
    p.space_after = Pt(2)

# ----------------------------------------------------------------------
# SLIDE 8: Comparative Analysis Table
# ----------------------------------------------------------------------
slide8 = prs.slides.add_slide(slide_layout)
apply_background(slide8)
add_header(slide8, "Comparative Analysis: Naive Election vs. Quorum-Based Election")

# Table
rows, cols = 8, 3
left = Inches(0.8)
top = Inches(1.8)
width = Inches(11.733)
height = Inches(5.1)

table_shape = slide8.shapes.add_table(rows, cols, left, top, width, height)
table = table_shape.table

# Set Column Widths
table.columns[0].width = Inches(3.2)
table.columns[1].width = Inches(4.2)
table.columns[2].width = Inches(4.333)

headers = ["Metric / Architectural Feature", "Naive Leader Election (Exp 2 / Exp 4)", "Quorum-Based Election (Exp 5)"]
for col_idx, h in enumerate(headers):
    cell = table.cell(0, col_idx)
    cell.fill.solid()
    cell.fill.fore_color.rgb = COLOR_PRIMARY
    p = cell.text_frame.paragraphs[0]
    p.text = h
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = RGBColor(255, 255, 255)

data = [
    ("Quorum Requirement", "None (Highest reachable node claims leadership)", "Strict Majority: Q = floor(N/2) + 1 (3 for N=5)"),
    ("Network Partition Cut", "Both partitions independently elect a coordinator", "Only majority partition is mathematically permitted to elect"),
    ("Split-Brain Risk", "HIGH (Two active leaders causing state corruption)", "ZERO (Impossible by Pigeonhole Overlap Principle)"),
    ("Minority Partition State", "Elects rogue leader (e.g. Node 2)", "Enters FENCED_MINORITY and refuses election"),
    ("Client Write Workloads", "Accepted in both partitions simultaneously", "Accepted ONLY in majority; rejected in minority"),
    ("Leader Lease Guard", "Leader runs indefinitely in isolation", "Leader automatically steps down if heartbeat ACKs < Q"),
    ("Partition Healing", "Requires complex multi-master reconciliation", "Automatic, seamless cluster re-unification")
]

for row_idx, row_data in enumerate(data):
    for col_idx, text in enumerate(row_data):
        cell = table.cell(row_idx + 1, col_idx)
        cell.fill.solid()
        if (row_idx % 2) == 0:
            cell.fill.fore_color.rgb = COLOR_CARD_BG
        else:
            cell.fill.fore_color.rgb = RGBColor(241, 245, 249)
        p = cell.text_frame.paragraphs[0]
        p.text = text
        p.font.size = Pt(11)
        if col_idx == 0:
            p.font.bold = True
            p.font.color.rgb = COLOR_PRIMARY
        elif col_idx == 1:
            p.font.color.rgb = COLOR_DANGER if "HIGH" in text or "Both" in text else COLOR_MUTED
        else:
            p.font.color.rgb = COLOR_SUCCESS if "ZERO" in text or "Strict" in text else COLOR_PRIMARY

# ----------------------------------------------------------------------
# SLIDE 9: Interactive Console & Test Capabilities
# ----------------------------------------------------------------------
slide9 = prs.slides.add_slide(slide_layout)
apply_background(slide9)
add_header(slide9, "Interactive CLI Console: Manual Testing & Verification")

# Left Card: Interactive CLI Capabilities
add_card(slide9, Inches(0.8), Inches(1.8), Inches(5.6), Inches(5.1))
tb = slide9.shapes.add_textbox(Inches(1.1), Inches(2.0), Inches(5.0), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Interactive Shell Capabilities"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY
p.space_after = Pt(12)

cli_cmds = [
    ("status", "Dumps runtime state, Term, Leader, and blocked sets across all 5 nodes."),
    ("partition <grp1> <grp2>", "Cuts network between groups (e.g. partition 1,2 3,4,5)."),
    ("heal", "Restores full bidirectional communication across all nodes."),
    ("workload <nodeId> <task>", "Submits client task (verifies leader lease / minority fence)."),
    ("kill <nodeId>", "Shuts down a node process to test dynamic failover.")
]
for cmd, desc in cli_cmds:
    p = tf.add_paragraph()
    p.text = f"• {cmd}: "
    p.font.size = Pt(11.5)
    p.font.bold = True
    p.font.color.rgb = COLOR_ACCENT
    run = p.add_run()
    run.text = desc
    run.font.bold = False
    run.font.color.rgb = COLOR_MUTED
    p.space_after = Pt(8)

# Right Card: Sample Interactive Session
add_card(slide9, Inches(6.8), Inches(1.8), Inches(5.7), Inches(5.1), bg_color=COLOR_CODE_BG)
tb = slide9.shapes.add_textbox(Inches(7.1), Inches(2.0), Inches(5.1), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Example CLI Session Output"
p.font.size = Pt(14)
p.font.bold = True
p.font.color.rgb = RGBColor(56, 189, 248)
p.space_after = Pt(10)

cli_session = [
    "SplitBrain-CLI> partition 1,2 3,4,5",
    "Cutting network between Partition A {1,2} and Partition B {3,4,5}...",
    "Partition applied successfully.",
    "",
    "SplitBrain-CLI> workload 2 transfer_funds",
    "Submitting task 'transfer_funds' to Node 2...",
    "Node 2 Response: Result='REJECTED_SPLIT_BRAIN_FENCED_MINORITY'",
    "Status='FAIL_NO_QUORUM'",
    "",
    "SplitBrain-CLI> workload 4 transfer_funds",
    "Submitting task 'transfer_funds' to Node 4...",
    "Node 4 Response: Result='SUCCESS_EXECUTED_BY_LEADER_N4'",
    "Status='OK'",
    "",
    "SplitBrain-CLI> heal",
    "Full cluster connectivity restored."
]
for line in cli_session:
    p = tf.add_paragraph()
    p.text = line
    p.font.size = Pt(9.5)
    p.font.name = "Courier New"
    if line.startswith("SplitBrain-CLI>"):
        p.font.bold = True
        p.font.color.rgb = RGBColor(253, 224, 71)
    elif "REJECTED" in line or "FAIL" in line:
        p.font.color.rgb = RGBColor(248, 113, 113)
    elif "SUCCESS" in line or "OK" in line:
        p.font.color.rgb = RGBColor(74, 222, 128)
    else:
        p.font.color.rgb = COLOR_CODE_TEXT
    p.space_after = Pt(2)

# ----------------------------------------------------------------------
# SLIDE 10: Conclusion & Key Engineering Takeaways
# ----------------------------------------------------------------------
slide10 = prs.slides.add_slide(slide_layout)
apply_background(slide10)
add_header(slide10, "Summary & Key Engineering Takeaways")

# Left Card: Academic & Real-world Relevance
add_card(slide10, Inches(0.8), Inches(1.8), Inches(5.6), Inches(5.1))
tb = slide10.shapes.add_textbox(Inches(1.1), Inches(2.0), Inches(5.0), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Relevance in Modern Systems"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY
p.space_after = Pt(12)

concl_points = [
    "Production Systems: Strict majority quorums are the foundational building block of Raft (etcd, Kubernetes), Paxos (Google Spanner), and ZooKeeper (Zab protocol).",
    "CAP Theorem Trade-off: Under network partitions (P), the system chooses Consistency (C) over Availability (A). Minority nodes sacrifice write availability to preserve absolute cluster safety.",
    "Hardware Planning: Cluster sizes are intentionally chosen as odd numbers (N = 3, 5, 7) because an even cluster (N=4) requires Q=3, providing no additional fault tolerance over N=3."
]
for pt in concl_points:
    p = tf.add_paragraph()
    p.text = "• " + pt
    p.font.size = Pt(11.5)
    p.font.color.rgb = COLOR_MUTED
    p.space_after = Pt(10)

# Right Card: Verification Checklist
add_card(slide10, Inches(6.8), Inches(1.8), Inches(5.7), Inches(5.1))
tb = slide10.shapes.add_textbox(Inches(7.1), Inches(2.0), Inches(5.1), Inches(4.6))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Experiment 5 Verification Checklist"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_SUCCESS
p.space_after = Pt(12)

checklist = [
    "[✓] UDP Datagram socket communication verified across 5 concurrent JVM node processes.",
    "[✓] Pre-election quorum validation halts election triggers in sub-clusters < 3 nodes.",
    "[✓] Minority partition {1, 2} transitions to FENCED_MINORITY with zero rogue leaders.",
    "[✓] Client write workloads in minority partition rejected with FAIL_NO_QUORUM.",
    "[✓] Majority partition {3, 4, 5} maintains active leader quorum lease and processes tasks.",
    "[✓] Dynamic partition healing demonstrates automatic cluster re-unification under 1 leader."
]
for item in checklist:
    p = tf.add_paragraph()
    p.text = item
    p.font.size = Pt(11.5)
    p.font.bold = True
    p.font.color.rgb = COLOR_PRIMARY
    p.space_after = Pt(10)

# ----------------------------------------------------------------------
# Save Presentation
# ----------------------------------------------------------------------
output_filename = "Quorum_Split_Brain_Election_Presentation.pptx"
output_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), output_filename)
prs.save(output_path)
print(f"Successfully generated PowerPoint presentation: {output_path}")
