import { useState } from 'react';
import LoadingSpinner from './LoadingSpinner';

export default function SpecUploadForm({ onUploaded }) {
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    if (!file) return;
    setLoading(true);
    setError(null);
    try {
      await onUploaded(file);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3 rounded-lg border border-slate-200 bg-white p-4">
      <label className="text-sm font-medium text-slate-700">Upload an OpenAPI / Swagger JSON spec</label>
      <input
        type="file"
        accept=".json,application/json"
        onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        className="text-sm"
      />
      <button
        type="submit"
        disabled={!file || loading}
        className="w-fit rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
      >
        Upload &amp; Parse
      </button>
      {loading && <LoadingSpinner label="Parsing spec..." />}
      {error && <p className="text-sm text-red-600">{error}</p>}
    </form>
  );
}
