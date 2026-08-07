import { useState } from 'react';
import LoadingSpinner from './LoadingSpinner';

export default function ExportButtons({ endpointCount, generatedCount, onGenerateAll, onExportMarkdown, onExportPdf, onExportPostman }) {
  const [busyAction, setBusyAction] = useState(null);
  const [error, setError] = useState(null);
  const allGenerated = generatedCount === endpointCount && endpointCount > 0;

  async function run(action, fn) {
    setBusyAction(action);
    setError(null);
    try {
      await fn();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyAction(null);
    }
  }

  return (
    <div className="flex flex-col gap-2 rounded-lg border border-slate-200 bg-white p-4">
      <h3 className="text-sm font-semibold text-slate-700">Export</h3>
      <p className="text-xs text-slate-500">
        {generatedCount}/{endpointCount} endpoints documented
      </p>
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => run('all', onGenerateAll)}
          disabled={busyAction !== null || endpointCount === 0}
          className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-semibold text-white disabled:opacity-50"
        >
          Generate All Docs
        </button>
        <button
          type="button"
          onClick={() => run('md', onExportMarkdown)}
          disabled={busyAction !== null || !allGenerated}
          className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 disabled:opacity-50"
        >
          Export Markdown
        </button>
        <button
          type="button"
          onClick={() => run('pdf', onExportPdf)}
          disabled={busyAction !== null || !allGenerated}
          className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 disabled:opacity-50"
        >
          Export PDF
        </button>
        <button
          type="button"
          onClick={() => run('postman', onExportPostman)}
          disabled={busyAction !== null || endpointCount === 0}
          className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 disabled:opacity-50"
        >
          Export Postman Collection
        </button>
      </div>
      {busyAction && <LoadingSpinner label="Working..." />}
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  );
}
