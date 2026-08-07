import LoadingSpinner from './LoadingSpinner';

function Section({ title, children }) {
  return (
    <div>
      <h3 className="mb-1 text-sm font-semibold text-slate-700">{title}</h3>
      <pre className="overflow-x-auto whitespace-pre-wrap rounded-md bg-slate-50 p-3 text-xs text-slate-800">
        {children}
      </pre>
    </div>
  );
}

export default function EndpointDocView({ endpoint, doc, loading, onGenerate }) {
  if (!endpoint) {
    return <p className="text-sm text-slate-500">Select an endpoint on the left to view or generate its docs.</p>;
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-lg font-semibold text-slate-900">
          {endpoint.method} {endpoint.path}
        </h2>
        <button
          type="button"
          onClick={onGenerate}
          disabled={loading}
          className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-semibold text-white disabled:opacity-50"
        >
          {doc ? 'Regenerate' : 'Generate'}
        </button>
      </div>

      {loading && <LoadingSpinner label="Calling the AI provider..." />}

      {doc && !loading && (
        <>
          <Section title="Description">{doc.humanDoc}</Section>
          <Section title="Example Request">{doc.exampleRequest}</Section>
          <Section title="Example Response">{doc.exampleResponse}</Section>
          <Section title="Java Spring Boot Client">{doc.javaClientSnippet}</Section>
          <Section title="Error Explanations">{doc.errorExplanations}</Section>
        </>
      )}
    </div>
  );
}
