from fastapi import FastAPI, UploadFile, File
from speech_engine import SpeechEngine
from intent_resolver import IntentResolver
import queue
import time
import os

app = FastAPI()
command_queue = queue.Queue()

# Initialize engines
speech = SpeechEngine(model_size="base")
resolver = IntentResolver()

@app.post("/process_audio")
async def process_audio(file: UploadFile = File(...)):
    temp_path = f"temp_{time.time()}.wav"
    
    try:
        # 1. Save the uploaded audio
        with open(temp_path, "wb") as f:
            f.write(await file.read())
        
        # 2. Convert Audio to Text
        raw_text = speech.transcribe(temp_path)
        print(f"User said: {raw_text}")
        
        # 3. Resolve to Bot Command
        command = resolver.resolve(raw_text)
        
        if command:
            command_queue.put(command)
            print(f"Executing: {command}")
            return {"status": "success", "command": command}
        
        return {"status": "ignored", "reason": "No intent found"}

    except Exception as e:
        print(f"Error processing: {e}")
        return {"status": "error", "message": str(e)}
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)

@app.get("/get_command")
def get_command():
    try:
        # Get the command and remove it from the mailbox
        return {"command": command_queue.get_nowait()}
    except queue.Empty:
        return {"command": ""}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)