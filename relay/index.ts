const PORT = parseInt(process.env.PORT || "8080");

// Forward mapping: apiKey → WebSocket
const connections = new Map<string, WebSocket>();
// Reverse mapping: WebSocket → Set<apiKey>
const wsKeys = new Map<WebSocket, Set<string>>();

// In-flight claim requests: claimId → { resolve, timeout }
const pendingClaims = new Map<string, {
  resolve: (value: any) => void;
  timeout: Timer;
}>();

let claimCounter = 0;

function generateClaimId(): string {
  return `claim_${++claimCounter}_${Date.now()}`;
}

function sendJson(ws: WebSocket, data: any) {
  try {
    ws.send(JSON.stringify(data));
  } catch { }
}

function respondToClaim(claimId: string, result: any) {
  const pending = pendingClaims.get(claimId);
  if (!pending) return;
  clearTimeout(pending.timeout);
  pendingClaims.delete(claimId);
  pending.resolve(result);
}

function registerKeys(ws: WebSocket, keys: string[]) {
  // Remove these keys from any other WS that might have them
  for (const [existingWs, existingKeys] of wsKeys) {
    if (existingWs !== ws) {
      for (const key of keys) {
        if (existingKeys.has(key)) {
          existingKeys.delete(key);
        }
      }
    }
  }

  // Register all keys for this WS
  const currentKeys = wsKeys.get(ws) || new Set<string>();
  for (const key of keys) {
    connections.set(key, ws);
    currentKeys.add(key);
  }
  wsKeys.set(ws, currentKeys);
}

function unregisterWs(ws: WebSocket) {
  const keys = wsKeys.get(ws);
  if (keys) {
    for (const key of keys) {
      const current = connections.get(key);
      if (current === ws) {
        connections.delete(key);
      }
    }
    wsKeys.delete(ws);
  }
}

function unregisterKey(ws: WebSocket, key: string) {
  const current = connections.get(key);
  if (current === ws) {
    connections.delete(key);
  }
  const keys = wsKeys.get(ws);
  if (keys) {
    keys.delete(key);
  }
}

function handleWebSocketMessage(ws: WebSocket, raw: string) {
  let msg: any;
  try {
    msg = JSON.parse(raw);
  } catch {
    sendJson(ws, { type: "error", message: "invalid json" });
    return;
  }

  switch (msg.type) {
    case "auth": {
      if (!msg.apiKey) {
        sendJson(ws, { type: "auth_error", message: "missing apiKey" });
        return;
      }

      // Build list of keys to register
      const keys = [msg.apiKey];
      if (Array.isArray(msg.allKeys)) {
        for (const k of msg.allKeys) {
          if (!keys.includes(k)) keys.push(k);
        }
      }

      registerKeys(ws, keys);
      sendJson(ws, { type: "auth_ok", message: "authenticated", keyCount: keys.length });
      console.log(`[auth] ${keys.length} key(s) registered (${msg.apiKey.substring(0, 12)}...)`);
      break;
    }

    case "revoke_key": {
      if (!msg.apiKey) {
        sendJson(ws, { type: "error", message: "missing apiKey" });
        return;
      }
      unregisterKey(ws, msg.apiKey);
      console.log(`[revoke] ${msg.apiKey.substring(0, 12)}... revoked`);
      sendJson(ws, { type: "revoke_ok", apiKey: msg.apiKey });
      break;
    }

    case "claim_response": {
      if (!msg.claimId) {
        sendJson(ws, { type: "error", message: "missing claimId" });
        return;
      }
      respondToClaim(msg.claimId, {
        confirmed: msg.confirmed === true,
        message: msg.message || "",
        transaction: msg.transaction || null,
      });
      break;
    }

    case "pong": {
      break;
    }

    default: {
      sendJson(ws, { type: "error", message: `unknown type: ${msg.type}` });
    }
  }
}

// Bun HTTP + WebSocket server
Bun.serve({
  port: PORT,

  async fetch(req: Request, server: any) {
    const url = new URL(req.url);
    const path = url.pathname;

    // Health check
    if (path === "/health") {
      return new Response(JSON.stringify({
        status: "ok",
        connections: connections.size,
        wsSessions: wsKeys.size,
        pendingClaims: pendingClaims.size,
      }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }

    // WebSocket upgrade
    if (path === "/ws") {
      const apiKey = url.searchParams.get("apiKey");
      if (!apiKey) {
        return new Response(JSON.stringify({ error: "missing apiKey" }), {
          status: 400,
          headers: { "Content-Type": "application/json" },
        });
      }

      const upgraded = server.upgrade(req, {
        data: { apiKey },
      });
      if (!upgraded) {
        return new Response("WebSocket upgrade failed", { status: 500 });
      }
      return;
    }

    // POST /llm-extract — app asks relay to parse SMS via Groq
    if (req.method === "POST" && path === "/llm-extract") {
      const groqKey = process.env.GROQ_API_KEY;
      if (!groqKey) {
        return new Response(JSON.stringify({ error: "GROQ_API_KEY not configured on relay" }), {
          status: 501,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }

      let body: any;
      try {
        body = await req.json();
      } catch {
        return new Response(JSON.stringify({ error: "invalid json" }), {
          status: 400,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }

      const smsBody = body.body;
      if (!smsBody) {
        return new Response(JSON.stringify({ error: "missing body" }), {
          status: 400,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }

      try {
        const groqResponse = await fetch("https://api.groq.com/openai/v1/chat/completions", {
          method: "POST",
          headers: {
            "Authorization": `Bearer ${groqKey}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            model: process.env.GROQ_MODEL || "openai/gpt-oss-20b",
            max_tokens: 256,
            messages: [
              {
                role: "system",
                content: "You extract mobile money transaction details from SMS messages. Respond with ONLY a JSON object having keys: reference (string or null), amount (number or null), senderName (string or null), senderPhone (string or null), balanceAfter (number or null). Use null for missing fields. No other text.",
              },
              {
                role: "user",
                content: smsBody,
              },
            ],
          }),
        });

        const groqData = await groqResponse.json() as any;
        const text = groqData?.choices?.[0]?.message?.content;
        if (!text) {
          return new Response(JSON.stringify({ error: "groq returned no content" }), {
            status: 502,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
          });
        }

        // Parse the extracted JSON and validate
        try {
          const extracted = JSON.parse(text.trim());
          if (extracted.reference && extracted.amount !== undefined && extracted.amount !== null) {
            return new Response(JSON.stringify({ success: true, data: extracted }), {
              status: 200,
              headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
            });
          }
          return new Response(JSON.stringify({ success: false, error: "missing required fields" }), {
            status: 200,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
          });
        } catch {
          return new Response(JSON.stringify({ success: false, error: "invalid json from groq" }), {
            status: 200,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
          });
        }
      } catch (err: any) {
        return new Response(JSON.stringify({ error: `groq request failed: ${err.message}` }), {
          status: 502,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }
    }

    // POST /claim — widget sends claim request
    if (req.method === "POST" && path === "/claim") {
      let body: any;
      try {
        body = await req.json();
      } catch {
        return new Response(JSON.stringify({ confirmed: false, message: "invalid json" }), {
          status: 400,
          headers: { "Content-Type": "application/json" },
        });
      }

      const { apiKey, reference } = body;
      if (!apiKey || !reference) {
        return new Response(JSON.stringify({
          confirmed: false,
          message: "apiKey and reference are required",
        }), {
          status: 400,
          headers: { "Content-Type": "application/json" },
        });
      }

      // Look up app connection
      const appWs = connections.get(apiKey);
      if (!appWs) {
        return new Response(JSON.stringify({
          confirmed: false,
          message: "phone offline",
        }), {
          status: 200,
          headers: {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*",
          },
        });
      }

      // Send claim request to app, wait for response
      const claimId = generateClaimId();
      const result = await new Promise<any>((resolve) => {
        const timeout = setTimeout(() => {
          pendingClaims.delete(claimId);
          resolve({ confirmed: false, message: "no response from phone" });
        }, 30000);

        pendingClaims.set(claimId, { resolve, timeout });
        sendJson(appWs, {
          type: "claim_request",
          claimId,
          reference,
          apiKey,
        });
      });

      return new Response(JSON.stringify(result), {
        status: 200,
        headers: {
          "Content-Type": "application/json",
          "Access-Control-Allow-Origin": "*",
        },
      });
    }

    // CORS preflight
    if (req.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type",
        },
      });
    }

    return new Response("Not found", { status: 404 });
  },

  websocket: {
    open(ws: WebSocket) {
      // Connection opened — wait for auth message
    },

    message(ws: WebSocket, message: string | Buffer) {
      handleWebSocketMessage(ws, message.toString());
    },

    close(ws: WebSocket) {
      // Remove ALL keys registered for this WS
      unregisterWs(ws);
      console.log(`[disconnect] WebSocket closed, keys cleaned up`);
    },

    drain(ws: WebSocket) {
      // Backpressure — not handling for now
    },
  },
});

console.log(`[relay] MoMo Bridge Relay running on port ${PORT} (multi-key)`);
