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

def add_header(slide, title_text, subtitle_text="Distributed Computing Laboratory • Experiment 3"):
    title_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.5), Inches(11.733), Inches(1.1))
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

# ----------------------------------------------------------------------
# SLIDE 1: Title Slide (Dark Theme)
# ----------------------------------------------------------------------
slide_layout = prs.slide_layouts[6]
slide1 = prs.slides.add_slide(slide_layout)

bg1 = slide1.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
bg1.fill.solid()
bg1.fill.fore_color.rgb = COLOR_PRIMARY
bg1.line.fill.background()

tbox = slide1.shapes.add_textbox(Inches(1.0), Inches(1.8), Inches(11.333), Inches(4.5))
tf1 = tbox.text_frame
tf1.word_wrap = True

p = tf1.paragraphs[0]
p.text = "DISTRIBUTED COMPUTING LABORATORY • EXPERIMENT 3"
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = RGBColor(56, 189, 248)
p.space_after = Pt(12)

p = tf1.add_paragraph()
p.text = "Clock Synchronization in Distributed Systems"
p.font.size = Pt(32)
p.font.bold = True
p.font.color.rgb = RGBColor(255, 255, 255)
p.space_after = Pt(8)

p = tf1.add_paragraph()
p.text = "Implementation of Lamport Logical Clocks & Berkeley Physical Clock Synchronization"
p.font.size = Pt(18)
p.font.color.rgb = RGBColor(203, 213, 225)
p.space_after = Pt(36)

p = tf1.add_paragraph()
p.text = "Student: Yash Dharamshi | UID: 2024300047 | Division: A (Batch C)\nSardar Patel Institute of Technology, Mumbai"
p.font.size = Pt(13)
p.font.color.rgb = RGBColor(148, 163, 184)

# ----------------------------------------------------------------------
# SLIDE 2: Problem Statement & Motivation
# ----------------------------------------------------------------------
slide2 = prs.slides.add_slide(slide_layout)
apply_background(slide2)
add_header(slide2, "Problem Statement & The Need for Clock Synchronization")

# Left Card: Problem
add_card(slide2, Inches(0.8), Inches(1.8), Inches(5.6), Inches(5.0))
tb = slide2.shapes.add_textbox(Inches(1.1), Inches(2.0), Inches(5.0), Inches(4.5))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "The Distributed Time Challenge"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY
p.space_after = Pt(14)

points_l = [
    "No Global Shared Clock: Autonomous nodes rely solely on isolated local oscillators.",
    "Hardware Clock Drift: Crystal frequency imperfections and heat cause timestamps to diverge over time.",
    "Causality Inversion: A fast node sending to a slow node can result in receive timestamps preceding send timestamps.",
    "System Risk: Breaks transactional atomicity, distributed logging, and consensus protocols."
]
for pt in points_l:
    p = tf.add_paragraph()
    p.text = "• " + pt
    p.font.size = Pt(12)
    p.font.color.rgb = COLOR_MUTED
    p.space_after = Pt(10)

# Right Card: Dual Solution
add_card(slide2, Inches(6.8), Inches(1.8), Inches(5.7), Inches(5.0))
tb = slide2.shapes.add_textbox(Inches(7.1), Inches(2.0), Inches(5.1), Inches(4.5))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "The Dual Synchronization Strategy"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_ACCENT
p.space_after = Pt(14)

points_r = [
    "Logical Time (Lamport):",
    "  - Tracks relative causal event ordering (A → B).",
    "  - Decoupled from physical quartz oscillators using monotonic sequence numbers.",
    "Physical Time (Berkeley Algorithm):",
    "  - Master node periodically polls and averages internal cluster timestamps.",
    "  - Computes and applies relative offsets (Δt) without stepping local clocks backward."
]
for pt in points_r:
    p = tf.add_paragraph()
    p.text = ("• " if not pt.startswith("  ") else "") + pt
    p.font.size = Pt(12)
    p.font.bold = True if ":" in pt else False
    p.font.color.rgb = COLOR_PRIMARY if ":" in pt else COLOR_MUTED
    p.space_after = Pt(6)

# ----------------------------------------------------------------------
# SLIDE 3: Comparison Table
# ----------------------------------------------------------------------
slide3 = prs.slides.add_slide(slide_layout)
apply_background(slide3)
add_header(slide3, "Comparative Analysis: Logical vs. Physical Synchronization")

table_shape = slide3.shapes.add_table(5, 3, Inches(0.8), Inches(1.8), Inches(11.733), Inches(4.8))
table = table_shape.table
table.columns[0].width = Inches(2.7)
table.columns[1].width = Inches(4.5)
table.columns[2].width = Inches(4.533)

headers = ["Parameter", "Lamport Logical Clocks", "Berkeley Physical Sync"]
row_data = [
    ["Primary Objective", "Preserve causal event ordering (A → B)", "Minimize physical cluster skew (Δt)"],
    ["Time Representation", "Monotonically increasing integer (1, 2, 3...)", "Absolute epoch milliseconds / UTC"],
    ["Coordination Model", "Decentralized (piggybacked on datagrams)", "Centralized active Master-Worker polling"],
    ["Adjustment Rule", "Jump to max(L_local, L_msg) + 1", "Relative offset slew (Δt_i = T_avg - T_i)"]
]

for col_idx, text in enumerate(headers):
    cell = table.cell(0, col_idx)
    cell.fill.solid()
    cell.fill.fore_color.rgb = COLOR_PRIMARY
    p = cell.text_frame.paragraphs[0]
    p.text = text
    p.font.bold = True
    p.font.size = Pt(13)
    p.font.color.rgb = RGBColor(255, 255, 255)

for row_idx, data in enumerate(row_data):
    for col_idx, text in enumerate(data):
        cell = table.cell(row_idx + 1, col_idx)
        cell.fill.solid()
        cell.fill.fore_color.rgb = COLOR_CARD_BG if row_idx % 2 == 0 else RGBColor(241, 245, 249)
        p = cell.text_frame.paragraphs[0]
        p.text = text
        p.font.size = Pt(12)
        p.font.color.rgb = COLOR_PRIMARY if col_idx == 0 else COLOR_MUTED
        p.font.bold = True if col_idx == 0 else False

# ----------------------------------------------------------------------
# SLIDE 4: Algorithms & Mathematical Formulations
# ----------------------------------------------------------------------
slide4 = prs.slides.add_slide(slide_layout)
apply_background(slide4)
add_header(slide4, "Algorithmic Rules & Mathematical Formulations")

# Left: Lamport
add_card(slide4, Inches(0.8), Inches(1.8), Inches(5.6), Inches(5.0))
tb = slide4.shapes.add_textbox(Inches(1.1), Inches(2.0), Inches(5.0), Inches(4.5))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Lamport's Logical Clock Rules"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY
p.space_after = Pt(10)

l_steps = [
    "Happens-Before Relation (→):",
    "  If event A causes event B, then L(A) < L(B).",
    "Rule 1 (Internal Event / Dispatch):",
    "  L_local = L_local + 1",
    "Rule 2 (Message Ingestion):",
    "  L_local = max(L_local, L_msg) + 1",
    "Total Ordering Tie-Breaker:",
    "  If L(A) == L(B), order by NodeID: NodeID_A < NodeID_B."
]
for st in l_steps:
    p = tf.add_paragraph()
    p.text = st
    p.font.size = Pt(11)
    p.font.bold = True if ":" in st else False
    p.font.color.rgb = COLOR_ACCENT if ":" in st else COLOR_MUTED
    p.space_after = Pt(4)

# Right: Berkeley
add_card(slide4, Inches(6.8), Inches(1.8), Inches(5.7), Inches(5.0))
tb = slide4.shapes.add_textbox(Inches(7.1), Inches(2.0), Inches(5.1), Inches(4.5))
tf = tb.text_frame
tf.word_wrap = True
p = tf.paragraphs[0]
p.text = "Berkeley Physical Sync Formulae"
p.font.size = Pt(16)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY
p.space_after = Pt(10)

b_steps = [
    "Step 1: Master Polls Physical Time:",
    "  Master transmits TIME_REQ to all worker nodes.",
    "Step 2: Workers Reply with Timestamp:",
    "  T_i = System.currentTimeMillis() + offset_i",
    "Step 3: Cluster Average Computation:",
    "  T_avg = (1 / N) * Σ (T_i)",
    "Step 4: Relative Offset Calculation:",
    "  Δt_i = T_avg - T_i",
    "  Adjusts clocks smoothly without stepping backwards."
]
for st in b_steps:
    p = tf.add_paragraph()
    p.text = st
    p.font.size = Pt(11)
    p.font.bold = True if ":" in st else False
    p.font.color.rgb = COLOR_ACCENT if ":" in st else COLOR_MUTED
    p.space_after = Pt(4)

# ----------------------------------------------------------------------
# SLIDE 5: Multi-Threaded Node Architecture
# ----------------------------------------------------------------------
slide5 = prs.slides.add_slide(slide_layout)
apply_background(slide5)
add_header(slide5, "System Architecture: Multi-Threaded Node Design")

threads = [
    ("Thread 1: Listener Thread", "Runs blocking UDP socket read loop. Ingests incoming datagrams, atomically synchronizes the local Lamport clock, and handles Berkeley sync requests.", COLOR_ACCENT),
    ("Thread 2: Worker / Traffic Thread", "Periodically generates simulated application transactions (every 2.5s) to produce asynchronous inter-node causal message traffic.", COLOR_PRIMARY),
    ("Thread 3: Berkeley Coordinator", "Active exclusively on Master node. Initiates periodic physical sync rounds (every 6.0s), computes cluster average, and dispatches offsets.", COLOR_ACCENT),
    ("Thread Synchronization", "All thread accesses to shared state variables (`lamportClock`, `physicalClockOffsetMs`) are synchronized using `synchronized(clockLock)` monitor locks.", COLOR_PRIMARY)
]

for idx, (th_title, th_desc, accent_c) in enumerate(threads):
    col = idx % 2
    row = idx // 2
    x = Inches(0.8 + col * 6.0)
    y = Inches(1.8 + row * 2.6)
    
    add_card(slide5, x, y, Inches(5.7), Inches(2.3))
    tb = slide5.shapes.add_textbox(x + Inches(0.3), y + Inches(0.2), Inches(5.1), Inches(1.9))
    tf = tb.text_frame
    tf.word_wrap = True
    
    p = tf.paragraphs[0]
    p.text = th_title
    p.font.size = Pt(14)
    p.font.bold = True
    p.font.color.rgb = accent_c
    p.space_after = Pt(6)
    
    p = tf.add_paragraph()
    p.text = th_desc
    p.font.size = Pt(11)
    p.font.color.rgb = COLOR_MUTED

# ----------------------------------------------------------------------
# SLIDE 6: Java Implementation Code
# ----------------------------------------------------------------------
slide6 = prs.slides.add_slide(slide_layout)
apply_background(slide6)
add_header(slide6, "Core Java Implementation: Synchronization Logic")

code_snippets = [
    ("Lamport Clock Methods", 
"""// Rule 1: Local / Pre-send tick
public int tickLogicalClock() {
    synchronized (clockLock) {
        return ++lamportClock;
    }
}

// Rule 2: Message receipt sync
public int updateLogicalClockOnReceive(int receivedClock) {
    synchronized (clockLock) {
        lamportClock = Math.max(lamportClock, receivedClock) + 1;
        return lamportClock;
    }
}"""),
    ("Berkeley Calculation Logic", 
"""// Master-side offset calculation
long sum = 0;
for (long t : collectedNodeTimes.values()) {
    sum += t;
}
long avgTime = sum / collectedNodeTimes.size();

for (int peerId : NODE_PORTS.keySet()) {
    long currentT = nodeTimes.get(peerId);
    long adjustment = avgTime - currentT;
    sendAdjustment(peerId, adjustment);
}""")
]

for idx, (title, code) in enumerate(code_snippets):
    x = Inches(0.8 + idx * 6.0)
    add_card(slide6, x, Inches(1.8), Inches(5.7), Inches(5.0), bg_color=COLOR_CODE_BG, border_color=None)
    
    tb = slide6.shapes.add_textbox(x + Inches(0.3), Inches(2.0), Inches(5.1), Inches(4.5))
    tf = tb.text_frame
    tf.word_wrap = True
    
    p = tf.paragraphs[0]
    p.text = title
    p.font.size = Pt(13)
    p.font.bold = True
    p.font.color.rgb = RGBColor(56, 189, 248)
    p.space_after = Pt(8)
    
    p_code = tf.add_paragraph()
    p_code.text = code
    p_code.font.name = "Consolas"
    p_code.font.size = Pt(10)
    p_code.font.color.rgb = COLOR_CODE_TEXT

# ----------------------------------------------------------------------
# SLIDE 7: Experimental Trace & Verification
# ----------------------------------------------------------------------
slide7 = prs.slides.add_slide(slide_layout)
apply_background(slide7)
add_header(slide7, "Experimental Verification & Output Trace")

add_card(slide7, Inches(0.8), Inches(1.8), Inches(11.733), Inches(5.0), bg_color=COLOR_CODE_BG, border_color=None)
tb = slide7.shapes.add_textbox(Inches(1.1), Inches(2.0), Inches(11.133), Inches(4.5))
tf = tb.text_frame
tf.word_wrap = True

trace_text = """[CLUSTER CONFIG] Node 1 (Master, 0ms drift) | Node 2 (+1200ms drift) | Node 3 (-800ms drift)

[14:10:03.605] [Node 1 | L=1 | PhysTime: 17180003605ms] [SEND_MSG] Sent Transaction_1 to Node 2 (Local L=1)
[14:10:03.610] [Node 2 | L=2 | PhysTime: 17180004810ms] [RECV_MSG] Received from Node 1 (Sender L=1) -> Local L advanced to 2
[14:10:03.810] [Node 2 | L=3 | PhysTime: 17180005010ms] [SEND_MSG] Sent Transaction_1 to Node 3 (Local L=3)
[14:10:03.815] [Node 3 | L=4 | PhysTime: 17180003015ms] [RECV_MSG] Received from Node 2 (Sender L=3) -> Local L advanced to 4

[14:10:07.100] [Node 1 | Master] Initiating Berkeley Physical Clock Synchronization Round...
[14:10:07.910] [Node 1 | Master] Cluster Avg Physical Time: 17180008043 ms
[14:10:07.915] [Node 2] Applied Clock Offset: -1072 ms -> New PhysTime: 17180008043 ms
[14:10:07.920] [Node 3] Applied Clock Offset: +923 ms  -> New PhysTime: 17180008043 ms

>>> VERIFICATION RESULT: Causal Invariant (L_recv > L_send) & Physical Time Convergence Confirmed <<<"""

p = tf.paragraphs[0]
p.text = "Simulation Console Trace (Captured Output)"
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = RGBColor(56, 189, 248)
p.space_after = Pt(8)

p_body = tf.add_paragraph()
p_body.text = trace_text
p_body.font.name = "Consolas"
p_body.font.size = Pt(9.5)
p_body.font.color.rgb = COLOR_CODE_TEXT

# ----------------------------------------------------------------------
# SLIDE 8: Conclusion & Key Takeaways
# ----------------------------------------------------------------------
slide8 = prs.slides.add_slide(slide_layout)
apply_background(slide8)
add_header(slide8, "Conclusion & Key Takeaways")

conclusions = [
    ("Monotonic Causality Enforced", "Lamport's logical clocks guarantee causal consistency across asynchronous node communication without relying on hardware timers."),
    ("Physical Skew Elimination", "The Berkeley algorithm successfully converged skewed physical clocks to a uniform baseline using relative offsets without backwards clock stepping."),
    ("Non-Blocking Concurrency", "Multi-threading isolates networking and clock calibration routines from application execution loops."),
    ("Distributed Foundation", "Provides essential ordering and synchronization primitives required for primary-backup replication, distributed consensus, and transaction serializability.")
]

for idx, (title, desc) in enumerate(conclusions):
    x = Inches(0.8 + (idx % 2) * 6.0)
    y = Inches(1.8 + (idx // 2) * 2.6)
    
    add_card(slide8, x, y, Inches(5.7), Inches(2.3))
    tb = slide8.shapes.add_textbox(x + Inches(0.3), y + Inches(0.2), Inches(5.1), Inches(1.9))
    tf = tb.text_frame
    tf.word_wrap = True
    
    p = tf.paragraphs[0]
    p.text = f"{idx + 1}. {title}"
    p.font.size = Pt(14)
    p.font.bold = True
    p.font.color.rgb = COLOR_PRIMARY
    p.space_after = Pt(6)
    
    p = tf.add_paragraph()
    p.text = desc
    p.font.size = Pt(11)
    p.font.color.rgb = COLOR_MUTED

# Save presentation
output_filename = "Clock_Synchronization_Presentation.pptx"
prs.save(output_filename)
print(f"Presentation successfully saved as '{output_filename}'.")