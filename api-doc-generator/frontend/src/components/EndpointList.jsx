const METHOD_COLORS = {
  GET: 'bg-emerald-100 text-emerald-700',
  POST: 'bg-blue-100 text-blue-700',
  PUT: 'bg-amber-100 text-amber-700',
  PATCH: 'bg-amber-100 text-amber-700',
  DELETE: 'bg-red-100 text-red-700',
};

export default function EndpointList({ endpoints, selectedOperationId, onSelect, generatedOperationIds }) {
  if (endpoints.length === 0) {
    return <p className="text-sm text-slate-500">No endpoints yet - upload a spec to get started.</p>;
  }

  return (
    <ul className="flex flex-col divide-y divide-slate-200 rounded-lg border border-slate-200 bg-white">
      {endpoints.map((endpoint) => (
        <li key={endpoint.operationId}>
          <button
            type="button"
            onClick={() => onSelect(endpoint.operationId)}
            className={`flex w-full items-center gap-3 px-4 py-3 text-left text-sm hover:bg-slate-50 ${
              selectedOperationId === endpoint.operationId ? 'bg-indigo-50' : ''
            }`}
          >
            <span
              className={`rounded px-2 py-0.5 text-xs font-mono font-semibold ${
                METHOD_COLORS[endpoint.method] || 'bg-slate-100 text-slate-700'
              }`}
            >
              {endpoint.method}
            </span>
            <span className="font-mono text-slate-800">{endpoint.path}</span>
            {generatedOperationIds.has(endpoint.operationId) && (
              <span className="ml-auto text-xs text-emerald-600">generated</span>
            )}
          </button>
        </li>
      ))}
    </ul>
  );
}
