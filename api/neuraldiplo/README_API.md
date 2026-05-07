# LangApp Speech API

HTTP API wraps the neural model from this folder. Android sends an audio file only after the user finishes recording an answer.

## Run

```bash
cd api/neuraldiplo
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
pip install -r requirements-api.txt
uvicorn api_server:app --host 0.0.0.0 --port 8001
```

Linux:

```bash
source .venv/bin/activate
uvicorn api_server:app --host 0.0.0.0 --port 8001
```

Health check:

```bash
curl http://SERVER_IP:8001/health
```

## Endpoints

```text
POST /api/speech/recognize
multipart/form-data:
  audio=<file>
```

```text
POST /api/speech/check-text
multipart/form-data:
  audio=<file>
  referenceText=<expected phrase>
  taskId=<optional>
  taskType=<AUDIO_RECORDING|IMAGE_RECORDING|TEXT_RECORDING>
  threshold=<0.7>
```

Response:

```json
{
  "success": true,
  "passed": true,
  "score": 0.91,
  "scorePercent": 91.0,
  "referenceText": "example",
  "recognizedText": "example",
  "errors": []
}
```

Android uses `check-text` for `AudioRecordingTask`, `ImageRecordingTask`, and `TextRecordingTask`.
