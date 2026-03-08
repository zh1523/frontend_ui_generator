const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080/api/v1";

export async function apiRequest(path, options = {}) {
  const { method = "GET", body, workspaceKey, headers = {}, responseType = "json" } = options;
  const finalHeaders = {
    ...headers
  };
  if (workspaceKey) {
    finalHeaders["X-Workspace-Key"] = workspaceKey;
  }
  let payload = body;
  if (body && !(body instanceof Blob) && !(body instanceof FormData)) {
    finalHeaders["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers: finalHeaders,
    body: payload
  });
  if (!response.ok) {
    let message = `Request failed: ${response.status}`;
    try {
      const err = await response.json();
      message = err.message || message;
    } catch (_) {
      // ignore json parse failure
    }
    throw new Error(message);
  }
  if (responseType === "blob") {
    return response.blob();
  }
  if (responseType === "text") {
    return response.text();
  }
  return response.json();
}

export async function streamSse(path, { workspaceKey, onEvent, signal }) {
  const response = await fetch(`${API_BASE}${path}`, {
    method: "GET",
    headers: {
      Accept: "text/event-stream",
      "X-Workspace-Key": workspaceKey
    },
    signal
  });
  if (!response.ok) {
    throw new Error(`SSE failed: ${response.status}`);
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";
  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, "\n");
    let boundary = buffer.indexOf("\n\n");
    while (boundary !== -1) {
      const rawEvent = buffer.slice(0, boundary).trim();
      buffer = buffer.slice(boundary + 2);
      if (rawEvent) {
        const parsed = parseSseEvent(rawEvent);
        onEvent(parsed);
      }
      boundary = buffer.indexOf("\n\n");
    }
  }
}

function parseSseEvent(raw) {
  const lines = raw.split("\n");
  let event = "message";
  const dataLines = [];
  for (const line of lines) {
    if (line.startsWith("event:")) {
      event = line.slice(6).trim();
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).trim());
    }
  }
  const rawData = dataLines.join("\n");
  let data = rawData;
  try {
    data = JSON.parse(rawData);
  } catch (_) {
    // keep raw text
  }
  return { event, data };
}
