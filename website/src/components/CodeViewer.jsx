import React, { useState } from 'react';
import { Code, Copy, Check } from 'lucide-react';

export default function CodeViewer({ files = [] }) {
  const [activeFileIndex, setActiveFileIndex] = useState(0);
  const [copied, setCopied] = useState(false);

  if (!files || files.length === 0) return null;

  const currentFile = files[activeFileIndex];

  const handleCopy = () => {
    navigator.clipboard.writeText(currentFile.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="glass-card" style={{ overflow: 'hidden', border: '1px solid rgba(51, 65, 85, 0.8)' }}>
      {/* File Tab Bar */}
      <div style={{
        background: 'rgba(15, 23, 42, 0.95)',
        borderBottom: '1px solid rgba(51, 65, 85, 0.6)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 1rem'
      }}>
        <div style={{ display: 'flex', gap: '0.25rem', overflowX: 'auto' }}>
          {files.map((file, idx) => (
            <button
              key={file.name}
              onClick={() => setActiveFileIndex(idx)}
              style={{
                background: idx === activeFileIndex ? 'rgba(30, 41, 59, 0.9)' : 'transparent',
                border: 'none',
                borderBottom: idx === activeFileIndex ? '2px solid #38bdf8' : '2px solid transparent',
                color: idx === activeFileIndex ? '#f8fafc' : '#94a3b8',
                padding: '0.75rem 1rem',
                fontSize: '0.82rem',
                fontFamily: 'var(--font-mono)',
                fontWeight: 600,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                transition: 'all 0.2s'
              }}
            >
              <Code size={14} color={idx === activeFileIndex ? '#38bdf8' : '#64748b'} />
              {file.name}
            </button>
          ))}
        </div>

        <button
          onClick={handleCopy}
          className="btn-secondary"
          style={{ padding: '0.35rem 0.75rem', fontSize: '0.75rem', borderRadius: '6px' }}
        >
          {copied ? <Check size={14} color="#34d399" /> : <Copy size={14} />}
          {copied ? 'Copied!' : 'Copy Code'}
        </button>
      </div>

      {/* Code Container */}
      <div style={{
        background: '#090d16',
        padding: '1.25rem',
        maxHeight: '520px',
        overflowY: 'auto',
        fontFamily: 'var(--font-mono)',
        fontSize: '0.84rem',
        lineHeight: '1.6',
        color: '#e2e8f0'
      }}>
        <pre style={{ margin: 0 }}>
          <code>
            {currentFile.code.split('\n').map((line, i) => (
              <div key={i} style={{ display: 'flex', gap: '1.5rem' }}>
                <span style={{ width: '32px', textAlign: 'right', color: '#475569', userSelect: 'none', flexShrink: 0 }}>
                  {i + 1}
                </span>
                <span style={{ color: line.trim().startsWith('//') || line.trim().startsWith('*') || line.trim().startsWith('/*') ? '#64748b' : line.trim().startsWith('public') || line.trim().startsWith('class') || line.trim().startsWith('import') ? '#38bdf8' : '#e2e8f0' }}>
                  {line}
                </span>
              </div>
            ))}
          </code>
        </pre>
      </div>
    </div>
  );
}
