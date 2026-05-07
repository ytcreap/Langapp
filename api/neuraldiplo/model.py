import torch.nn as nn
import torch.nn.functional as F

# ===== Алфавит =====
CHARACTERS = ' абвгдеёжзийклмнопрстуфхцчшщъыьэюя'
idx2char = {i+1: ch for i, ch in enumerate(CHARACTERS)}

def indices_to_text(indices):
    return ''.join(idx2char.get(i, '') for i in indices)

# ===== Модель (LSTM + CTC) =====

class ASR_LSTM_Model(nn.Module):
    def __init__(self, n_mels, vocab_size, num_accents):
        super().__init__()

        self.lstm = nn.LSTM(
            input_size=n_mels,
            hidden_size=512,
            num_layers=4,
            bidirectional=True,
            batch_first=True
        )

        self.ctc_head = nn.Linear(1024, vocab_size)
        self.accent_head = nn.Linear(1024, num_accents)

    def forward(self, x):
        out, _ = self.lstm(x)

        ctc_log_probs = F.log_softmax(
            self.ctc_head(out), dim=2
        )

        pooled = out.mean(dim=1)
        accent_logits = self.accent_head(pooled)

        return ctc_log_probs, accent_logits
