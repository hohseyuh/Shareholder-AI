import speech_recognition as sr
import requests

URL = "http://localhost:8000/process_audio"

def start_listening():
    recognizer = sr.Recognizer()
    mic = sr.Microphone()

    with mic as source:
        recognizer.adjust_for_ambient_noise(source, duration=1)
        print("Listening for Minecraft commands...")

        while True:
            try:
                # listen() automatically detects the end of speech via silence
                audio = recognizer.listen(source, phrase_time_limit=5)
                print("Speech detected, uploading...")
                
                response = requests.post(URL, files={"file": ("audio.wav", audio.get_wav_data(), "audio/wav")})
                print(f"Server response: {response.json()}")
                
            except Exception as e:
                print(f"Error: {e}")

if __name__ == "__main__":
    start_listening()