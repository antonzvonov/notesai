#!/usr/bin/env python3
import os
import sys
import psycopg2
from pyannote.audio import Pipeline
import whisper
import soundfile as sf

DB_URL = os.environ.get('DB_URL')

FILE_ID = int(sys.argv[1])
AUDIO_PATH = sys.argv[2]

pipeline = Pipeline.from_pretrained("pyannote/speaker-diarization")
diary = pipeline(AUDIO_PATH)

model = whisper.load_model("base")
audio_data, sr = sf.read(AUDIO_PATH)

conn = psycopg2.connect(DB_URL)
cur = conn.cursor()

for segment, _, speaker in diary.itertracks(yield_label=True):
    start = segment.start
    end = segment.end
    start_i = int(start * sr)
    end_i = int(end * sr)
    piece = audio_data[start_i:end_i]
    text = model.transcribe(piece, language='ru')["text"]
    cur.execute(
        "INSERT INTO audio_caption(file_id, start_time, end_time, speaker, text) VALUES (%s, %s, %s, %s, %s)",
        (FILE_ID, start, end, speaker, text)
    )

conn.commit()
cur.close()
conn.close()
