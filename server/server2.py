from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.responses import StreamingResponse, FileResponse
from fastapi.middleware.cors import CORSMiddleware
from pathlib import Path
from typing import Optional
import shutil
import uuid
import os

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
    speed: str=  Form(..., description="语速（待使用）"),
    outputAudio: str = Form(..., description="是否输出音频文件（待使用）"),  # 有true和false两种取值,都是字符串形式
    outputText: str = Form(..., description="是否输出字幕文件（待使用）"),  # 有true和false两种取值,都是字符串形式 
    outputVideo: str = Form(..., description="是否输出视频文件（待使用）"),  # 有true和false两种取值,都是字符串形式
):
    file_uuid = uuid.uuid4()
    file_path = UPLOAD_DIR / f"{file_uuid}_text.{file.filename.split('.')[-1]}"

    try:
        # 保存文本/配置文件
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        # 目前返回默认文件，待替换为处理后的文件
        return FileResponse(
            # 替换为实际处理后的文件路径
            path=UPLOAD_DIR / "example.mp3",  # 临时返回默认文件
            media_type="audio/mpeg",  # 指定正确的媒体类型
            filename=Path("example.mp3").name
        )
    finally:

        pass

@app.post("/aivoice/chat")
async def chat(
    model: str = Form(...),
    emotion: str = Form(...),
    speed: str = Form(...),
    answerQuestion: str = Form(...), # 有true和false两种取值,都是字符串形式
    internetSearch: str = Form(...),  # 有true和false两种取值,都是字符串形式
    messageInput: Optional[str] = Form(None), # messageInput和audio其中有一个为空
    audio: Optional[UploadFile] = None,
    file: Optional[UploadFile] = None,
):
    file_uuid = uuid.uuid4()

    try:
        # 保存文本/配置文件
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        if file is not None and file.filename:  # 检查audio是否不为None # 检查audio是否为None # 模型只能输入相对路径，改一下
            file_path = UPLOAD_DIR / f"{file_uuid}_text.{file.filename.split('.')[-1]}"
            with open(file_path, "wb") as buffer:
                shutil.copyfileobj(file.file, buffer)
        else:
            audio_path = None


        # 保存音频文件
        if audio is not None and audio.filename:  # 检查audio是否不为None # 检查audio是否为None # 模型只能输入相对路径，改一下
            audio_path = f"uploads/{file_uuid}_audio.{audio.filename.split('.')[-1]}"
            with open(audio_path, "wb") as buffer:
                shutil.copyfileobj(audio.file, buffer)
        else:
            audio_path = None

        # 处理逻辑
        if(messageInput is None and audio is None):
            raise HTTPException(400, "必须提供文本输入或音频输入")
        elif(messageInput is not None and audio is not None):
            raise HTTPException(400, "文本输入和音频输入只能提供一个")
        elif(messageInput is not None && answerQuestion == "true"):
            if(internetSearch == "true" && file_path is not None):
            if(internetSearch == "false" && file_path is not None):
            if(internetSearch == "true" && file_path is None):
            if(internetSearch == "false" && file_path is None):
            # 将messageInput传递给模型处理,返回messageAnswer，
            # 将messageAnswer传入语音生成模型，生成音频文件，和messageAnswer一起用multipart返回
        elif(messageInput is not None && answerQuestion == "false"):
            # 将messageInput作为文本传入语音生成模型，生成音频文件，和messageInput一起用multipart返回
        elif(audio is not None && answerQuestion == "true"):
            if(internetSearch == "true" && file_path is not None):
            if(internetSearch == "false" && file_path is not None):
            if(internetSearch == "true" && file_path is None):
            if(internetSearch == "false" && file_path is None):
            # 将audio传递给模型处理,返回messageAnswer，
            # 将messageAnswer传入语音生成模型，生成音频文件，和messageAnswer一起返回multipart响应
        elif(audio is not None && answerQuestion == "false"):
            # 将audio传递给语音生成模型，生成音频文件，返回音频文件
        else:
            raise HTTPException(400, "参数错误")


        messageAnswer = "处理成功"  # 返回的字符串示例
        result_path = UPLOAD_DIR / "example.mp3"  # 生成的音频文件路径
        

        # 生成multipart响应,以下代码不需要修改！！
        boundary = "BOUNDARY_" + uuid.uuid4().hex
        def generate_stream():
            # 文本部分，返回messageAnswer，字符串
            yield (
                f"--{boundary}\r\n"
                "Content-Disposition: form-data; name=\"messageAnswer\"\r\n"
                "Content-Type: text/plain\r\n\r\n"
                f"{messageAnswer}\r\n"
            ).encode()

            # 音频部分，返回auidoFile，文件
            yield (
                f"--{boundary}\r\n"
                "Content-Disposition: form-data; name=\"audioFile\"; filename=\"result.mp3\"\r\n"
                "Content-Type: audio/mpeg\r\n\r\n"
            ).encode()
            
            # 流式传输音频文件（支持大文件）
            with open(result_path, "rb") as f:
                while chunk := f.read(4096):  # 每次读取4KB，分块传输
                    yield chunk
            
            # 结束标记
            yield f"\r\n--{boundary}--\r\n".encode()

        return StreamingResponse(
            generate_stream(),
            media_type=f"multipart/form-data; boundary={boundary}",
            headers={"Content-Disposition": "form-data; name=\"chatResult\""}
        )

    except Exception as e:
        raise HTTPException(500, f"服务器错误: {str(e)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=7000)