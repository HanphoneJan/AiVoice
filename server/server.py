from fastapi import FastAPI, File, UploadFile, HTTPException,Form
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from pathlib import Path
import shutil
import uuid
import os
from typing import Optional  # 导入Optional
app = FastAPI()

# 允许跨域请求（仅允许受信任的源）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  
    allow_methods=["*"],
    allow_headers=["*"],
)

# 设定存储路径
UPLOAD_DIR = Path(__file__).parent /"uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.post("/aivoice/upload")
async def upload_files(
    file: UploadFile = File(..., description="上传的文本/配置文件"),
    audio: Optional[UploadFile] = None,
    model: str = Form(..., description="处理模型（待使用）"),  
    emotion: str =  Form(..., description="情感（待使用）"),
    speed: str=  Form(..., description="语速（待使用）")  
):
    file_uuid = uuid.uuid4()
    file_path = UPLOAD_DIR / f"{file_uuid}_text.{file.filename.split('.')[-1]}"
    audio_path = UPLOAD_DIR / f"{file_uuid}_audio.{audio.filename.split('.')[-1]}"

    try:
        # 保存文本/配置文件
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        
        # 检查audio是否为None
        if audio is not None:  
            audio_path = UPLOAD_DIR / f"{file_uuid}_audio.{audio.filename.split('.')[-1]}"
            with open(audio_path, "wb") as buffer:
                shutil.copyfileobj(audio.file, buffer)
        
        # 如果提供了audio，则处理audio；否则，可能处理文本或其他逻辑
        if audio_path:
            # 假设处理逻辑在这里，处理后的文件保存在processed_audio_path
            # processed_audio_path = some_audio_processing_function(audio_path)
            pass  # 暂时留空
        else:
            # 如果没有提供audio，可能只处理文本或其他逻辑
            pass  # 暂时留空
        
        # 目前返回默认文件，待替换为处理后的文件
        return FileResponse(
            # 替换为实际处理后的文件路径
            # processed_audio_path,
            UPLOAD_DIR / "example.mp3",  # 临时返回默认文件
            media_type="audio/mpeg",  # 指定正确的媒体类型
            filename=Path("example.mp3").name
        )
    finally:
        # 清理逻辑（在这个场景中，文件是故意保存的，所以不需要清理）
        # 但如果处理失败，你可能需要实现某种回滚机制
        pass

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=7000)