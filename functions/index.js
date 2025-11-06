const functions = require("firebase-functions/v2/https");
const fetch = require("node-fetch");

// It's recommended to lock this down to your specific domain in production.
// const ALLOWED_ORIGIN = "https://verumglobal.foundation";

function applyCors(req, res) {
  // For simplicity in this context, we allow any origin.
  // In a production environment, you should restrict this to your app's domain.
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Vary", "Origin");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return true; // Indicates that the request was handled
  }
  return false; // Indicates that the request should continue
}


exports.openaiProxy = functions.https.onRequest(
  { region: "us-central1", maxInstances: 5, invoker: "public" },
  async (req, res) => {
    if (applyCors(req, res)) return;

    if (req.method !== "POST") {
      return res.status(405).send("Method Not Allowed");
    }
    try {
      // Use functions.config().openai.key in a real Firebase project
      // after running `firebase functions:config:set openai.key="sk-..."`
      const key = process.env.OPENAI_KEY; 
      if (!key) {
        functions.logger.error("OpenAI API key is not set in environment config.");
        return res.status(500).json({ error: "Server configuration error: OpenAI key missing" });
      }

      const apiResponse = await fetch("https://api.openai.com/v1/chat/completions", {
        method: "POST",
        headers: { "content-type": "application/json", "authorization": `Bearer ${key}` },
        body: JSON.stringify(req.body),
      });

      const responseText = await apiResponse.text();
      res.status(apiResponse.status)
         .type(apiResponse.headers.get("content-type") || "application/json")
         .send(responseText);

    } catch(e) {
      functions.logger.error("Error in OpenAI proxy:", e);
      return res.status(500).json({ error: "OpenAI proxy internal error" });
    }
  }
);

exports.geminiProxy = functions.https.onRequest(
  { region: "us-central1", maxInstances: 5, invoker: "public" },
  async (req, res) => {
    if (applyCors(req, res)) return;

    if (req.method !== "POST") {
      return res.status(405).send("Method Not Allowed");
    }
    try {
      // Use functions.config().gemini.key in a real Firebase project
      // after running `firebase functions:config:set gemini.key="AIza-..."`
      const key = process.env.GEMINI_KEY; 
      if (!key) {
        functions.logger.error("Gemini API key is not set in environment config.");
        return res.status(500).json({ error: "Server configuration error: Gemini key missing" });
      }

      const model = encodeURIComponent(req.query.model || "gemini-2.5-flash");
      const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${key}`;

      const apiResponse = await fetch(url, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(req.body),
      });
      
      const responseText = await apiResponse.text();
      res.status(apiResponse.status)
         .type(apiResponse.headers.get("content-type") || "application/json")
         .send(responseText);

    } catch(e) {
      functions.logger.error("Error in Gemini proxy:", e);
      return res.status(500).json({ error: "Gemini proxy internal error" });
    }
  }
);
