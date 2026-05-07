import librosa
import numpy as np

def extract_features(audio_path, sr, n_mels, max_audio_len, max_len):
    y, _ = librosa.load(audio_path, sr=sr, duration=max_audio_len/sr)
    if len(y) > max_audio_len:
        y = y[:max_audio_len]

    mels = librosa.feature.melspectrogram(y=y, sr=sr, n_mels=n_mels)
    mels_db = librosa.power_to_db(mels, ref=np.max).T

    if mels_db.shape[0] > max_len:
        mels_db = mels_db[:max_len, :]
    else:
        pad = max_len - mels_db.shape[0]
        mels_db = np.pad(mels_db, ((0, pad), (0, 0)), mode='constant')

    norm = (mels_db - np.mean(mels_db)) / (np.std(mels_db) + 1e-6)
    return norm
