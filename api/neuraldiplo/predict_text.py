import json
import torch
import torch.nn.functional as F
import math
import difflib
from collections import defaultdict
import os
import re

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


# ===== 2. Конвертация аудио =====
def convert_to_wav(input_path, target_sr=16000):
    print(f"🔄 Обработка аудио: {input_path}...")
    audio, sr = librosa.load(input_path, sr=target_sr, mono=True)
    wav_path = input_path.rsplit(".", 1)[0] + "_conv.wav"
    sf.write(wav_path, audio, target_sr)
    return wav_path


# ===== 3. Вспомогательная функция: Очистка текста =====
def clean_text(text):
    """Приводит текст к тому же виду, что выдает модель (нижний регистр, без знаков препинания)"""
    text = text.lower()
    # Оставляем только буквы (включая кириллицу) и пробелы
    text = re.sub(r'[^a-zа-яё ]', '', text)
    return " ".join(text.split())


# ===== 4. Beam Search =====
def ctc_beam_search(probs, beam_size=10):
    probs = probs.cpu().numpy()
    T, C = probs.shape
    beam = [([], 0.0)]

    for t in range(T):
        next_beam = defaultdict(lambda: float('-inf'))
        step_probs = probs[t]

        # Берем только лучшие индексы для ускорения
        top_indices = step_probs.argsort()[-beam_size:][::-1]

        for seq, score in beam:
            last_char = seq[-1] if seq else -1
            for k in top_indices:
                p = step_probs[k]
                current_score = score + math.log(p + 1e-8)

                if k == 0:  # Blank символ
                    next_beam[tuple(seq)] = max(next_beam[tuple(seq)], current_score)
                else:
                    if k == last_char:
                        # Если символ повторяется, в упрощенном поиске оставляем как есть
                        next_beam[tuple(seq)] = max(next_beam[tuple(seq)], current_score)
                    else:
                        new_seq = tuple(list(seq) + [k])
                        # ИСПРАВЛЕНО: сравниваем старое значение в словаре с новым счетом
                        next_beam[new_seq] = max(next_beam[new_seq], current_score)

        # Сортируем и оставляем только beam_size лучших кандидатов
        beam = sorted(next_beam.items(), key=lambda x: x[1], reverse=True)[:beam_size]
        beam = [(list(seq), score) for seq, score in beam]

    return beam[0][0]


# ===== 5. Сравнение текстов =====
def compare_text(reference, hypothesis):
    # Очищаем эталон, чтобы сравнение было честным
    reference = clean_text(reference)

    matcher = difflib.SequenceMatcher(None, reference, hypothesis)
    ratio = matcher.ratio()
    print(f"\n📊 Точность произношения: {ratio * 100:.1f}%")

    if ratio < 1.0:
        print("❗ Ошибки/Расхождения:")
        for opcode, a0, a1, b0, b1 in matcher.get_opcodes():
            if opcode == 'equal':
                print(f"  ✅ {reference[a0:a1]}", end="")
            elif opcode == 'insert':
                print(f"  ➕ [{hypothesis[b0:b1]}]", end="")
            elif opcode == 'delete':
                print(f"  ❌ (пропущено: {reference[a0:a1]})", end="")
            elif opcode == 'replace':
                print(f"  ⚠️ (вместо '{reference[a0:a1]}' услышано '{hypothesis[b0:b1]}')", end="")
        print("\n")
    else:
        print("✔ Идеальное совпадение!\n")


# ===== 6. Загрузка модели =====
print("⏳ Загрузка модели...")
vocab_size = len(CHARACTERS) + 1
num_accents = cfg.get("num_accents", 1)
model = ASR_LSTM_Model(N_MELS, vocab_size, num_accents).to(device)

try:
    state_dict = torch.load("asr_lstm_ctc_accent86.pth", map_location=device, weights_only=True)
    model.load_state_dict(state_dict)
    model.eval()
    print("📥 Модель успешно загружена!")
except FileNotFoundError:
    print("❌ Ошибка: файл модели не найден.")
    exit()


# ===== 7. Распознавание аудио =====
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
        ctc_out, _ = model(feat)
        probs = F.softmax(ctc_out, dim=2)[0]
        pred_indices = ctc_beam_search(probs, beam_size=20)

    return indices_to_text(pred_indices)


# ===== 8. Главный блок =====
if __name__ == "__main__":
    # 1. Получаем эталонный текст
    ref_text = input("Введите эталонный текст (который нужно прочитать): ").strip()

    # 2. Получаем путь к аудио
    student_audio = input("Введите путь к записи студента (аудиофайл): ").strip('"')

    if not ref_text or not os.path.exists(student_audio):
        print("❌ Ошибка: пустой текст или файл не найден.")
    else:
        print("\n🎤 Распознаю вашу речь...")
        student_text = recognize_audio_file(student_audio)

        print(f"\n📜 Ожидалось: {ref_text}")
        print(f"📝 Распознано: {student_text}")

        print("\n=== РЕЗУЛЬТАТ АНАЛИЗА ===")
        compare_text(ref_text, student_text)