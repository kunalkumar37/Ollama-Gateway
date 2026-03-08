# 🚀 Ollama Gateway

A production-ready **Spring Boot API Gateway** for your local Ollama LLM.  
Exposes your local AI via a secure, rate-limited, monitored public API.

---

## 📐 Architecture

```
Your Users
    │
    │  HTTPS + API Key
    ▼
[Ngrok / Cloudflare Tunnel]  ← Dynamic IP handled here
    │
    ▼
[Spring Boot Gateway :8080]
    ├── ApiKeyAuthFilter      ← Validates Bearer token
    ├── RateLimitFilter       ← Token Bucket (N req/min per key)
    ├── ProxyController       ← Routes to Ollama
    └── Dashboard :8080/dash  ← CPU/GPU monitoring
    │
    ▼
[Ollama Server :11434]        ← Your local LLM
```

---

## 🛠 Prerequisites

| Tool         | Install |
|--------------|---------|
| Java 17+     | `sdk install java 17` or download from adoptium.net |
| Maven 3.8+   | `sudo apt install maven` |
| Ollama       | `curl -fsSL https://ollama.ai/install.sh \| sh` |
| Ngrok (opt)  | https://ngrok.com |
| cloudflared (opt) | https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup |

---

## ⚡ Quick Start

### Step 1: Configure your API keys

Edit `src/main/resources/application.yml`:

```yaml
gateway:
  admin-secret-key: "your-super-secret-admin-key"   # change this!
  
  api-keys:
    "sk-user1-abc123xyz": "John Smith"
    "sk-user2-def456uvw": "Jane Doe"
    
  rate-limit:
    default-requests-per-minute: 20
    per-key-limits:
      "sk-user1-abc123xyz": 60   # VIP user gets more
```

### Step 2: Start Ollama

```bash
ollama serve
ollama pull llama3.2   # or any model you want
```

### Step 3: Build & Run the Gateway

```bash
mvn clean package -DskipTests
java -jar target/ollama-gateway-1.0.0.jar
```

Or with Maven dev mode:
```bash
mvn spring-boot:run
```

### Step 4: Expose via Ngrok (quick)

```bash
# Install ngrok, add your auth token
ngrok config add-authtoken YOUR_TOKEN

# Expose the gateway
ngrok http 8080
# → https://abc123.ngrok.io
```

### Step 5: Expose via Cloudflare (stable, recommended)

```bash
# Install cloudflared
# Login to Cloudflare
cloudflared tunnel login

# Create tunnel
cloudflared tunnel create ollama-gateway

# Route your domain to the tunnel
cloudflared tunnel route dns ollama-gateway api.your-domain.com

# Copy and edit the config
cp cloudflare-config.yml ~/.cloudflared/config.yml
# Edit the file with your tunnel ID and domain

# Run the tunnel
cloudflared tunnel run ollama-gateway

# Optional: install as system service (auto-start on boot)
sudo cloudflared service install
```

---

## 🔗 How Users Connect

Users connect using the **OpenAI client** (Python/JS/etc.) — fully compatible!

### Python
```python
from openai import OpenAI

client = OpenAI(
    base_url="https://YOUR-NGROK-OR-CF-URL/v1",
    api_key="sk-user1-abc123xyz"     # key you gave them
)

response = client.chat.completions.create(
    model="llama3.2",
    messages=[{"role": "user", "content": "Hello!"}]
)
print(response.choices[0].message.content)
```

### JavaScript
```javascript
import OpenAI from "openai";

const client = new OpenAI({
    baseURL: "https://YOUR-URL/v1",
    apiKey: "sk-user1-abc123xyz"
});

const response = await client.chat.completions.create({
    model: "llama3.2",
    messages: [{ role: "user", content: "Hello!" }]
});
```

### curl
```bash
curl https://YOUR-URL/v1/chat/completions \
  -H "Authorization: Bearer sk-user1-abc123xyz" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama3.2",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

---

## 🔐 Admin API

All admin endpoints require your admin key in the header:
```
Authorization: Bearer your-super-secret-admin-key
```
Or: `X-Admin-Key: your-super-secret-admin-key`

### Create a new API key
```bash
curl -X POST http://localhost:8080/admin/keys \
  -H "Authorization: Bearer your-admin-key" \
  -H "Content-Type: application/json" \
  -d '{"label": "New User Name"}'
```

### List all keys
```bash
curl http://localhost:8080/admin/keys \
  -H "Authorization: Bearer your-admin-key"
```

### Revoke a key
```bash
curl -X DELETE http://localhost:8080/admin/keys/sk-user1-abc123xyz \
  -H "Authorization: Bearer your-admin-key"
```

### Full system stats
```bash
curl http://localhost:8080/admin/stats \
  -H "Authorization: Bearer your-admin-key"
```

---

## 📊 Dashboard

Open in your browser: **http://localhost:8080/dash**

Displays in real-time (updates every 2 seconds):
- 🔵 CPU usage with history chart
- 🟢 GPU usage + temperature + VRAM (requires NVIDIA GPU)
- 🟡 RAM usage
- 📈 Requests per second chart
- 🔑 Per-API-key request counts
- ⚡ Rate limit bucket levels per key

---

## ⚙️ Rate Limiting

Uses the **Token Bucket algorithm**:
- Each API key gets a bucket of N tokens (= requests per minute)
- Each request consumes 1 token
- Tokens refill gradually (not all at once)
- Concurrent requests are also limited per key

When rate limited, the API returns:
```json
HTTP 429 Too Many Requests
Retry-After: 4

{
  "error": {
    "message": "Rate limit exceeded. Please retry after 3.8 seconds.",
    "type": "rate_limit_error",
    "code": 429,
    "retry_after_seconds": 3.8
  }
}
```

---

## 🔧 Configuration Reference

```yaml
gateway:
  ollama-base-url: http://localhost:11434   # Ollama address
  admin-secret-key: "change-me"             # Admin API key

  api-keys:
    "sk-key-here": "User Label"

  rate-limit:
    enabled: true
    default-requests-per-minute: 20          # Default RPM
    default-max-concurrent: 3                # Max parallel requests per key
    per-key-limits:
      "sk-specific-key": 60                  # Override for specific key

  monitoring:
    gpu-enabled: true                        # Set false if no NVIDIA GPU
    nvidia-smi-path: "nvidia-smi"            # Path to nvidia-smi binary
```

---

## 🛡️ Security Notes

1. **Never expose Ollama directly** — always go through this gateway
2. **Change the admin key** before deploying
3. **API keys are stored in memory** — restart resets to application.yml
4. **Cloudflare Tunnel** provides DDoS protection by default
5. For production, consider adding API keys to a database (H2/PostgreSQL)

---

## 📁 Project Structure

```
ollama-gateway/
├── src/main/java/com/ollamagateway/
│   ├── OllamaGatewayApplication.java     Main entry point
│   ├── config/
│   │   ├── GatewayProperties.java        Config binding
│   │   ├── SecurityConfig.java           Spring Security setup
│   │   └── WebClientConfig.java          HTTP client for Ollama
│   ├── filter/
│   │   ├── ApiKeyAuthFilter.java         Auth (runs first)
│   │   └── RateLimitFilter.java          Rate limiting (runs after auth)
│   ├── model/
│   │   ├── ApiKeyInfo.java               API key data model
│   │   └── TokenBucket.java             Token bucket implementation
│   ├── service/
│   │   ├── ApiKeyService.java            Key management
│   │   ├── RateLimiterService.java       Rate limit logic
│   │   ├── OllamaProxyService.java       Proxy to Ollama (streaming)
│   │   └── SystemMonitorService.java     CPU/GPU monitoring
│   └── controller/
│       ├── ProxyController.java          /v1/** endpoints
│       ├── AdminController.java          /admin/** endpoints
│       └── DashboardController.java      /dash + metrics API
├── src/main/resources/
│   ├── application.yml                   Configuration
│   └── templates/dashboard.html         Monitoring UI
├── start.sh                              Setup & launch script
├── cloudflare-config.yml                 CF tunnel template
└── pom.xml
```
