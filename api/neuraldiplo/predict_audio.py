import json
import torch
import torch.nn.functional as F
import math
import difflib
from collections import defaultdict
import os

# Заменяем pydub на librosa и soundfile
import librosa
import soundfile as sf

from preprocessing import extract_features
from model import ASR_LSTM_Model, indices_to_text, CHARACTERS

# ===== 1. Настройки =====
with open("config.json", "r", encoding="utf-8") as f:
    cfg = json.load(f)

SAMPLE_RATE = cfg["sample_rate"]
N_MELS = cfg["n_mels"]
MAX_AUDIO_LEN = cfg["max_audio_len"]
MAX_PAD_LENGTH = cfg["max_pad_length"]

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")


# ===== 2. Конвертация аудио (Теперь через Librosa) =====
def convert_to_wav(input_path, target_sr=16000):
    ext = input_path.split(".")[-1].lower()

    # Если это уже wav с нужной частотой, можно было бы пропустить,
    # но librosa гарантирует правильный формат (моно, float32)
    print(f"🔄 Обработка файла: {input_path}...")

    # librosa.load сразу делает:
    # 1. Декодирование (mp3, wav, ogg и т.д.)
    # 2. Ресемплинг до target_sr
    # 3. Сведение в моно (mono=True)
    audio, sr = librosa.load(input_path, sr=target_sr, mono=True)

    wav_path = input_path.rsplit(".", 1)[0] + "_conv.wav"

    # Сохраняем результат в wav, чтобы extract_features мог его прочитать по пути
    sf.write(wav_path, audio, target_sr)

    print(f"✔ Готово: {wav_path}")
    return wav_path


# ===== 3. Beam Search =====
def ctc_beam_search(probs, beam_size=10):
    # (Ваш код без изменений)
    probs = probs.cpu().numpy()
    T, C = probs.shape
    beam = [([], 0.0)]
    for t in range(T):
        next_beam = defaultdict(lambda: float('-inf'))
        step_probs = probs[t]
        top_indices = step_probs.argsort()[-beam_size:][::-1]
        for seq, score in beam:
            last_char = seq[-1] if seq else -1
            for k in top_indices:
                p = step_probs[k]
                current_score = score + math.log(p + 1e-8)
                if k == 0:
                    next_beam[tuple(seq)] = max(next_beam[tuple(seq)], current_score)
                else:
                    if k == last_char:
                        next_beam[tuple(seq)] = max(next_beam[tuple(seq)], current_score)
                    else:
                        new_seq = tuple(seq + [k])
                        next_beam[new_seq] = max(next_beam[new_seq], current_score)
        beam = sorted(next_beam.items(), key=lambda x: x[1], reverse=True)[:beam_size]
        beam = [(list(seq), score) for seq, score in beam]
    return beam[0][0]


# ===== 4. Сравнение текстов =====
def compare_text(reference, hypothesis):
    # (Ваш код без изменений)
    matcher = difflib.SequenceMatcher(None, reference, hypothesis)
    ratio = matcher.ratio()
    print(f"\n📊 Точность совпадения: {ratio * 100:.1f}%")
    if ratio < 1.0:
        print("❗ Ошибки произношения:")
        for opcode, a0, a1, b0, b1 in matcher.get_opcodes():
            if opcode == 'equal':
                print(f"  ✅ {reference[a0:a1]}", end=" ")
            elif opcode == 'insert':
                print(f"  ➕ (Лишнее: {hypothesis[b0:b1]})", end=" ")
            elif opcode == 'delete':
                print(f"  ❌ (Пропущено: {reference[a0:a1]})", end=" ")
            elif opcode == 'replace':
                print(f"  ⚠️ ('{hypothesis[b0:b1]}' вместо '{reference[a0:a1]}')", end=" ")
        print("\n")
    else:
        print("✔ Идеальное совпадение!\n")


# ===== 5. Загрузка модели =====
print("⏳ Загрузка модели...")
n_mels = cfg["n_mels"]
vocab_size = len(CHARACTERS) + 1
num_accents = cfg.get("num_accents", 1)
model = ASR_LSTM_Model(n_mels, vocab_size, num_accents).to(device)

try:
    state_dict = torch.load("asr_lstm_ctc_accent86.pth", map_location=device, weights_only=True)
    model.load_state_dict(state_dict)
    model.eval()
    print("📥 Модель успешно загружена!")
except FileNotFoundError:
    print("❌ Ошибка: файл 'asr_lstm_ctc_accent.pth' не найден.")
    exit()


# ===== 6. Распознавание аудио =====
def recognize_audio_file(audio_path):
    wav_path = convert_to_wav(audio_path, target_sr=SAMPLE_RATE)

    feat = extract_features(
        wav_path,
        sr=SAMPLE_RATE,
        n_mels=N_MELS,
        max_audio_len=MAX_AUDIO_LEN,
        max_len=MAX_PAD_LENGTH
    )

    feat = torch.tensor(feat, dtype=torch.float32).unsqueeze(0).to(device)

    with torch.no_grad():
        ctc_out, accent_out = model(feat)
        probs = F.softmax(ctc_out, dim=2)[0]
        pred_indices = ctc_beam_search(probs, beam_size=20)

    return indices_to_text(pred_indices)


# ===== 7. Сравнение двух аудиозаписей =====
def compare_two_audios(reference_audio, student_audio):
    print("\n🎧 Распознаю эталонную запись...")
    reference_text = recognize_audio_file(reference_audio)
    print(f"📜 Эталон: {reference_text}")

    print("\n🎤 Распознаю запись студента...")
    student_text = recognize_audio_file(student_audio)
    print(f"📝 Студент: {student_text}")

    print("\n=== Сравнение ===")
    compare_text(reference_text, student_text)


# ===== 8. Главный блок =====
if __name__ == "__main__":
    ref_audio = input("Введите путь к эталонной записи: ").strip('"')
    student_audio = input("Введите путь к записи студента: ").strip('"')
    print("\n🎯 Анализ двух аудиозаписей...")
    compare_two_audios(ref_audio, student_audio)