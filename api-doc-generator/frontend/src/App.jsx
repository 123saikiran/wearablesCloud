import { useState } from 'react';
import SpecUploadForm from './components/SpecUploadForm';
import EndpointList from './components/EndpointList';
import EndpointDocView from './components/EndpointDocView';
import AuthExplanationPanel from './components/AuthExplanationPanel';
import ExportButtons from './components/ExportButtons';
import * as api from './api/apiDocClient';

export default function App() {
  const [spec, setSpec] = useState(null); // { specId, title, version, endpointCount, securitySchemeNames }
  const [endpoints, setEndpoints] = useState([]);
  const [selectedOperationId, setSelectedOperationId] = useState(null);
  const [docsByOperationId, setDocsByOperationId] = useState({});
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState(null);

  async function handleUploaded(file) {
    const uploadResponse = await api.uploadSpec(file);
    const endpointList = await api.listEndpoints(uploadResponse.specId);
    setSpec(uploadResponse);
    setEndpoints(endpointList);
    setDocsByOperationId({});
    setSelectedOperationId(endpointList[0]?.operationId ?? null);
  }

  function handleSelect(operationId) {
    setSelectedOperationId(operationId);
  }

  async function handleGenerate() {
    if (!spec || !selectedOperationId) return;
    setGenerating(true);
    setError(null);
    try {
      const doc = await api.generateDoc(spec.specId, selectedOperationId);
      setDocsByOperationId((prev) => ({ ...prev, [doc.operationId]: doc }));
    } catch (err) {
      setError(err.message);
    } finally {
      setGenerating(false);
    }
  }

  async function handleGenerateAll() {
    if (!spec) return;
    const docs = await api.generateAll(spec.specId);
    setDocsByOperationId(docs);
  }

  const selectedEndpoint = endpoints.find((e) => e.operationId === selectedOperationId) ?? null;

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-900">AI API Explorer &amp; Documentation Generator</h1>
        <p className="text-sm text-slate-500">
          Upload an OpenAPI/Swagger spec and get AI-generated docs, examples, Java client snippets, and exports.
        </p>
      </header>

      <div className="mb-6">
        <SpecUploadForm onUploaded={handleUploaded} />
      </div>

      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

      {spec && (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div className="flex flex-col gap-4 lg:col-span-1">
            <div>
              <h2 className="text-sm font-semibold text-slate-700">
                {spec.title} <span className="font-normal text-slate-400">v{spec.version}</span>
              </h2>
              <p className="text-xs text-slate-500">{spec.endpointCount} endpoints</p>
            </div>
            <EndpointList
              endpoints={endpoints}
              selectedOperationId={selectedOperationId}
              onSelect={handleSelect}
              generatedOperationIds={new Set(Object.keys(docsByOperationId))}
            />
            <AuthExplanationPanel
              specId={spec.specId}
              schemeNames={spec.securitySchemeNames}
              onFetch={api.getAuthExplanation}
            />
            <ExportButtons
              endpointCount={endpoints.length}
              generatedCount={Object.keys(docsByOperationId).length}
              onGenerateAll={handleGenerateAll}
              onExportMarkdown={() => api.downloadMarkdown(spec.specId)}
              onExportPdf={() => api.downloadPdf(spec.specId)}
              onExportPostman={() => api.downloadPostmanCollection(spec.specId)}
            />
          </div>

          <div className="lg:col-span-2">
            <EndpointDocView
              endpoint={selectedEndpoint}
              doc={selectedOperationId ? docsByOperationId[selectedOperationId] : null}
              loading={generating}
              onGenerate={handleGenerate}
            />
          </div>
        </div>
      )}
    </div>
  );
}
