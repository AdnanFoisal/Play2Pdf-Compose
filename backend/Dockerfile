# --- Stage 1: builder ---
# Install dependencies in a separate stage so the final image is small.
FROM python:3.12-slim AS builder

WORKDIR /app

# Build deps for any wheels that need compilation (none currently, but kept
# for forward-compat with Pillow/Cryptography upgrades).
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
 && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# --- Stage 2: runtime ---
FROM python:3.12-slim

WORKDIR /app

# Copy the installed packages from the builder stage.
COPY --from=builder /root/.local /root/.local
ENV PATH=/root/.local/bin:$PATH

# Copy the server code + vendored Noto fonts (embedded into PDFs — D1).
COPY server.py .
COPY fonts ./fonts

# Make sure unhandled exceptions crash the container rather than silently
# hanging — uvicorn's --no-access-log keeps our JSON request log as the
# single source of truth.
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

EXPOSE 7860

# Healthcheck — hit /health every 30s. HF Spaces uses this to decide if
# the container is "ready".
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD python -c "import urllib.request, sys; sys.exit(0 if urllib.request.urlopen('http://localhost:7860/health').status == 200 else 1)"

CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "7860", "--no-access-log"]
