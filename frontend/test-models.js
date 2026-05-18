import fs from 'fs';

const envFile = fs.readFileSync('.env', 'utf-8');
const match = envFile.match(/VITE_GEMINI_API_KEY=(.*)/);
if (!match) {
  console.error("No API key found in .env");
  process.exit(1);
}
const apiKey = match[1].trim();

async function listModels() {
  try {
    const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`);
    const data = await response.json();
    if (data.models) {
      console.log("Available models:");
      data.models.forEach(m => {
        if (m.supportedGenerationMethods && m.supportedGenerationMethods.includes('generateContent')) {
          console.log(m.name);
        }
      });
    } else {
      console.error("Error fetching models:", data);
    }
  } catch (err) {
    console.error("Fetch failed:", err);
  }
}

listModels();
