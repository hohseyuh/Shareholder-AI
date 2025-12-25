import os
from faster_whisper import WhisperModel

class SpeechEngine:
    def __init__(self, model_size="base"):
        # "base" is a good balance between speed and accuracy. 
        # Use "tiny" if your CPU is older, or "small" for better accuracy.
        print(f"Loading Whisper Model ({model_size})...")
        self.model = WhisperModel(model_size, device="cpu", compute_type="int8")

    def transcribe(self, audio_path):
        if not os.path.exists(audio_path):
            return None
        
        # transcribe() returns a generator of segments
        segments, info = self.model.transcribe(audio_path, beam_size=5)
        
        full_text = ""
        for segment in segments:
            full_text += segment.text + " "
            
        return full_text.strip().lower()