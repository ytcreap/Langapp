import difflib
import os
import tempfile
from pathlib import Path
from typing import Annotated

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from predict_text import clean_text, recognize_audio_file


app = FastAPI(title="LangApp Speech API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("SPEECH_CORS_ORIGINS", "*").split(","),
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


def build_diff(reference: str, hypothesis: str) -> list[dict[str, str]]:
    matcher = difflib.SequenceMatcher(None, reference, hypothesis)
    errors: list[dict[str, str]] = []

    for opcode, ref_start, ref_end, hyp_start, hyp_end in matcher.get_opcodes():
        if opcode == "equal":
            continue
        errors.append(
            {
                "type": opcode,
                "expected": reference[ref_start:ref_end],
                "actual": hypothesis[hyp_start:hyp_end],
            }
        )

    return errors


async def save_upload(upload: UploadFile) -> str:
    suffix = Path(upload.filename or "recording.wav").suffix or ".wav"
    fd, path = tempfile.mkstemp(prefix="langapp_speech_", suffix=suffix)
    os.close(fd)

    with open(path, "wb") as target:
        while chunk := await upload.read(1024 * 1024):
            target.write(chunk)

    return path


def cleanup_audio(path: str) -> None:
    Path(path).unlink(missing_ok=True)
    Path(path.rsplit(".", 1)[0] + "_conv.wav").unlink(missing_ok=True)


def analyze_audio_file(audio_path: str, reference_text: str, threshold: float) -> dict:
    recognized_text = recognize_audio_file(audio_path)
    expected = clean_text(reference_text)
    actual = clean_text(recognized_text)
    score = difflib.SequenceMatcher(None, expected, actual).ratio()

    return {
        "success": True,
        "passed": score >= threshold,
        "score": round(score, 4),
        "scorePercent": round(score * 100, 1),
        "threshold": threshold,
        "referenceText": reference_text,
        "normalizedReferenceText": expected,
        "recognizedText": recognized_text,
        "normalizedRecognizedText": actual,
        "errors": build_diff(expected, actual),
    }


@app.get("/health")
def health() -> dict:
    return {"ok": True, "service": "langapp-speech-api"}


@app.post("/api/speech/recognize")
async def recognize(audio: Annotated[UploadFile, File(...)]) -> JSONResponse:
    audio_path = await save_upload(audio)
    try:
        text = recognize_audio_file(audio_path)
        return JSONResponse({"success": True, "recognizedText": text})
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    finally:
        cleanup_audio(audio_path)


@app.post("/api/speech/check-text")
async def check_text(
    audio: Annotated[UploadFile, File(...)],
    referenceText: Annotated[str, Form(...)],
    taskId: Annotated[str | None, Form()] = None,
    taskType: Annotated[str | None, Form()] = None,
    threshold: Annotated[float, Form()] = 0.7,
) -> JSONResponse:
    if not referenceText.strip():
        raise HTTPException(status_code=400, detail="referenceText is required")

    audio_path = await save_upload(audio)
    try:
        result = analyze_audio_file(audio_path, referenceText, threshold)
        result["taskId"] = taskId
        result["taskType"] = taskType
        return JSONResponse(result)
    except Exception as exc:
        return JSONResponse(
            status_code=500,
            content={
                "success": False,
                "passed": False,
                "score": 0,
                "referenceText": referenceText,
                "recognizedText": "",
                "errors": [{"type": "server", "expected": "", "actual": str(exc)}],
            },
        )
    finally:
        cleanup_audio(audio_path)
