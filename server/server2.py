from fastapi import FastAPI, File, UploadFile, HTTPException, Depends
from fastapi.responses import FileResponse
from pathlib import Path
import shutil
import os
import uuid
from typing import Optional
from fastapi.middleware.cors import CORSMiddleware

AUDIO_FILE_PATH = Path(__file__).parent / "example.mp3"


app = FastAPI()

# 添加CORS中间件（允许跨域请求）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 临时文件存储目录
TEMP_DIR = Path("temp_files")
TEMP_DIR.mkdir(exist_ok=True)

def process_files_locally(file_path: Path, audio_path: Path, param1: str, param2: str) -> Optional[Path]:
    """
    模拟本地处理函数（需要根据实际项目修改）
    返回处理后的音频文件路径
    """
    # 这里添加实际项目的处理逻辑
    # 示例：直接返回上传的音频文件（实际应该返回处理后的文件）
    # 使用param1和param2进行某些处理（这里只是示例）
    print(f"Received parameters: param1={param1}, param2={param2}")
    return audio_path  # 请替换为实际处理后的路径


@app.post("/aivoice/")
async def process_files(
    file: UploadFile = File(..., description="上传的文本/配置文件"),  # 必须上传的文件
    audio: UploadFile = File(..., description="上传的原始音频文件"),   # 必须上传的音频
    param1: str = ...,  # 必需参数，用于模型选择（例如："model_v1"）
    param2: str = ...,   # 必需参数，用于处理参数配置（例如："high_quality"）
    param3: str = ...   # 必需参数，用于处理参数配置（例如："speed"）
):
    # 生成唯一ID防止文件名冲突
    uid = uuid.uuid4().hex  # 生成32字符的唯一标识
    
    # 构造临时文件路径（添加唯一前缀）
    file_path = TEMP_DIR / f"{uid}_{file.filename}"    # 示例：temp_files/abc123_file.txt
    audio_path = TEMP_DIR / f"{uid}_{audio.filename}"  # 示例：temp_files/abc123_audio.wav

    try:
        # 保存上传文件到临时目录
        # 使用分块写入方式处理大文件
        with file_path.open("wb") as buffer:
            shutil.copyfileobj(file.file, buffer)  # 将上传文件内容复制到本地文件
        
        保存上传音频到临时目录
        with audio_path.open("wb") as buffer:
            shutil.copyfileobj(audio.file, buffer)

        # # 调用本地处理逻辑（核心业务逻辑）
        # processed_audio = process_files_locally(
        #     file_path=file_path,
        #     audio_path=audio_path,
        #     model_param=param1,    # 传递模型选择参数
        #     emotion_param=param2,   # 传递处理配置参数
        #     speed_param=param3,
        # )

        # # 检查处理结果有效性
        # if not processed_audio or not processed_audio.exists():
        #     raise HTTPException(
        #         status_code=500,
        #         detail="音频处理失败，请检查参数和文件格式"
        #     )

        # 返回处理后的音频文件
        return FileResponse(
            # processed_audio,  # 文件路径
            media_type="audio/*",  # 自动识别音频类型
            # filename="processed_audio.wav"  # 客户端默认保存文件名
             filename=AUDIO_FILE_PATH.name  # 客户端保存的文件名
        )

    finally:
        # 清理临时文件（无论成功失败都会执行）
        for path in [file_path, audio_path]:
            try:
                path.unlink(missing_ok=True)  # Python 3.8+ 支持missing_ok参数
            except Exception as e:
                print(f"清理临时文件 {path} 时出错：{str(e)}")
                # 生产环境建议记录到日志系统


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=7000)