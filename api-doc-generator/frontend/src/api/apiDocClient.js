const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

async function handleJson(response) {
  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(body.message || `Request failed with status ${response.status}`);
  }
  return response.json();
}

export async function uploadSpec(file) {
  const formData = new FormData();
  formData.append('file', file);
  const response = await fetch(`${BASE_URL}/api/specs`, { method: 'POST', body: formData });
  return handleJson(response);
}

export async function listEndpoints(specId) {
  const response = await fetch(`${BASE_URL}/api/specs/${specId}/endpoints`);
  return handleJson(response);
}

export async function generateDoc(specId, operationId) {
  const response = await fetch(`${BASE_URL}/api/specs/${specId}/endpoints/${operationId}/generate`, {
    method: 'POST',
  });
  return handleJson(response);
}

export async function generateAll(specId) {
  const response = await fetch(`${BASE_URL}/api/specs/${specId}/generate-all`, { method: 'POST' });
  return handleJson(response);
}

export async function getAuthExplanation(specId) {
  const response = await fetch(`${BASE_URL}/api/specs/${specId}/auth-explanation`);
  return handleJson(response);
}

export async function getPostmanCollection(specId) {
  const response = await fetch(`${BASE_URL}/api/specs/${specId}/export/postman`);
  return handleJson(response);
}

async function downloadBlob(path, filename) {
  const response = await fetch(`${BASE_URL}${path}`);
  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(body.message || `Request failed with status ${response.status}`);
  }
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export function downloadMarkdown(specId) {
  return downloadBlob(`/api/specs/${specId}/export/markdown`, 'api-docs.md');
}

export function downloadPdf(specId) {
  return downloadBlob(`/api/specs/${specId}/export/pdf`, 'api-docs.pdf');
}

export async function downloadPostmanCollection(specId) {
  const { collection } = await getPostmanCollection(specId);
  const blob = new Blob([JSON.stringify(collection, null, 2)], { type: 'application/json' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'postman-collection.json';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
