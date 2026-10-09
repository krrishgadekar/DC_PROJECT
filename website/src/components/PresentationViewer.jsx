import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, FileText, Download } from 'lucide-react';

const SLIDES = [
  { id: 1, title: 'Title & Project Overview', desc: 'Introduction to Experiment 6: Distributed Leader Election & Load Balancing.' },
  { id: 2, title: 'Problem Statement & Motivation', desc: 'Split-Brain danger in network partitions & load scheduling challenges.' },
  { id: 3, title: 'System Architecture & Topology', desc: '5-Node UDP Cluster, Lamport clocks, and socket messaging protocols.' },
  { id: 4, title: 'Quorum Consensus Rule', desc: 'Mathematical threshold Q = floor(N/2) + 1 = 3 protecting against dual leaders.' },
  { id: 5, title: 'Bully Leader Election Protocol', desc: 'Watchdog thread, 3000ms timeouts, candidate checks, and COORDINATOR packets.' },
  { id: 6, title: 'Load Balancing Framework', desc: 'Pluggable Strategy pattern design, NodeMetrics, and EMA response time telemetry.' },
  { id: 7, title: 'Static Load Balancers', desc: 'Round Robin, Least Connections, and Weighted Round Robin strategies.' },
  { id: 8, title: 'Random & Adaptive Balancers', desc: 'Random algorithm vs Adaptive Response Time scoring formula.' },
  { id: 9, title: 'Multi-Threaded Node Engine', desc: 'Breakdown of 6 concurrent worker threads inside DistributedNode.java.' },
  { id: 10, title: 'Task Lifecycle & Simulator', desc: 'QUEUED to COMPLETED task state machine, ClusterSim, and ClientSimulator.' },
  { id: 11, title: 'Experimental Results', desc: 'Zero split-brain verification & average task latency benchmarks.' },
  { id: 12, title: 'Conclusion & Future Scope', desc: 'Summary of achievements and Raft/Paxos extension opportunities.' }
];

export default function PresentationViewer() {
  const [currentSlide, setCurrentSlide] = useState(0);

  const prevSlide = () => setCurrentSlide((prev) => Math.max(0, prev - 1));
  const nextSlide = () => setCurrentSlide((prev) => Math.min(SLIDES.length - 1, prev + 1));

  const slide = SLIDES[currentSlide];
  const formattedIndex = String(slide.id).padStart(2, '0');

  return (
    <div className="glass-card" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <FileText size={20} color="#38bdf8" />
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc' }}>
            Experiment Presentation Deck ({currentSlide + 1} / {SLIDES.length})
          </h3>
        </div>

        <a
          href="/Experiment6_Presentation.pptx"
          download
          className="btn-primary"
          style={{ fontSize: '0.8rem', padding: '0.45rem 0.9rem' }}
        >
          <Download size={14} /> Download PPTX
        </a>
      </div>

      {/* Slide Canvas Display */}
      <div style={{
        background: '#0f172a',
        border: '1px solid rgba(51, 65, 85, 0.8)',
        borderRadius: '12px',
        padding: '2.5rem',
        minHeight: '360px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        position: 'relative',
        overflow: 'hidden',
        boxShadow: 'inset 0 0 32px rgba(0, 0, 0, 0.5)'
      }}>
        {/* Slide Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <span className="badge badge-cyan" style={{ fontSize: '0.7rem' }}>
            SLIDE {formattedIndex} • EXPERIMENT 6
          </span>
          <span style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 600 }}>
            DISTRIBUTED COMPUTING PROJECT
          </span>
        </div>

        {/* Slide Content Body */}
        <div style={{ margin: '2rem 0' }}>
          <h2 style={{ fontSize: '1.8rem', fontWeight: 800, color: '#f8fafc', marginBottom: '0.75rem', lineHeight: '1.2' }}>
            {slide.title}
          </h2>
          <p style={{ fontSize: '1.05rem', color: '#94a3b8', lineHeight: '1.6', maxWidth: '800px' }}>
            {slide.desc}
          </p>
        </div>

        {/* Slide Footer */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid rgba(51, 65, 85, 0.4)', paddingTop: '1rem' }}>
          <p style={{ fontSize: '0.75rem', color: '#64748b' }}>
            Quorum Split-Brain Prevention & Dynamic Load Balancing
          </p>
          <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#38bdf8', fontFamily: 'var(--font-mono)' }}>
            {currentSlide + 1} / {SLIDES.length}
          </span>
        </div>
      </div>

      {/* Slide Navigation Controls */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <button
          onClick={prevSlide}
          disabled={currentSlide === 0}
          className="btn-secondary"
          style={{ opacity: currentSlide === 0 ? 0.4 : 1, cursor: currentSlide === 0 ? 'not-allowed' : 'pointer' }}
        >
          <ChevronLeft size={18} /> Previous Slide
        </button>

        <div style={{ display: 'flex', gap: '0.35rem' }}>
          {SLIDES.map((s, idx) => (
            <button
              key={s.id}
              onClick={() => setCurrentSlide(idx)}
              style={{
                width: idx === currentSlide ? '24px' : '8px',
                height: '8px',
                borderRadius: '4px',
                background: idx === currentSlide ? '#38bdf8' : 'rgba(51, 65, 85, 0.6)',
                border: 'none',
                cursor: 'pointer',
                transition: 'all 0.2s'
              }}
            />
          ))}
        </div>

        <button
          onClick={nextSlide}
          disabled={currentSlide === SLIDES.length - 1}
          className="btn-secondary"
          style={{ opacity: currentSlide === SLIDES.length - 1 ? 0.4 : 1, cursor: currentSlide === SLIDES.length - 1 ? 'not-allowed' : 'pointer' }}
        >
          Next Slide <ChevronRight size={18} />
        </button>
      </div>
    </div>
  );
}
