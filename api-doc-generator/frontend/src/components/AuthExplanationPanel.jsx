import { useState } from 'react';
import LoadingSpinner from './LoadingSpinner';

export default function AuthExplanationPanel({ specId, schemeNames, onFetch }) {
  const [open, setOpen] = useState(false);
  const [explanation, setExplanation] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  async function toggle() {
    const nextOpen = !open;
    setOpen(nextOpen);
    if (nextOpen && explanation === null) {
      setLoading(true);
      setError(null);
      try {
        const result = await onFetch(specId);
        setExplanation(result.explanation);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
  }

  return (
    <div className="rounded-lg border border-slate-200 bg-white">
      <button
        type="button"
        onClick={toggle}
        className="flex w-full items-center justify-between px-4 py-3 text-left text-sm font-semibold text-slate-700"
      >
        <span>Authentication ({schemeNames.length ? schemeNames.join(', ') : 'none'})</span>
        <span>{open ? '-' : '+'}</span>
      </button>
      {open && (
        <div className="border-t border-slate-200 px-4 py-3 text-sm">
          {loading && <LoadingSpinner label="Explaining authentication..." />}
          {error && <p className="text-red-600">{error}</p>}
          {explanation && <pre className="whitespace-pre-wrap text-xs text-slate-800">{explanation}</pre>}
        </div>
      )}
    </div>
  );
}
