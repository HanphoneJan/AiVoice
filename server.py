from csmsc_inference import csmsc
from fastapi import FastAPI, File, UploadFile, HTTPException,Form
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from pathlib import Path
import shutil
import uuid
import os
from ppt.convert_pptx import pptxtomd
from ppt.md2text import mdtotext
from sound_clone import soundclone
from henan_inference import henanhua
from sichuan_inference import sichuanhua
import librosa
from typing import Optional
from csmsc_inference import csmsc
from sentiment_classify import annotate_sentences_with_emotion
from inferenceBysentiment import process_multiemotion_sentences
from multiemotion_female import female_multiemotion
from multiemotion_male import male_multiemotion
from multiemotion_EN import en_multiemotion, is_all_english_and_punct
import string
from yidou_inference import yidou
from mona_inference import mona
from yueyu_inference import  yueyu
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
        
        # 保存音频文件
        if audio is not None:  # 检查audio是否不为None # 检查audio是否为None
            audio_path = UPLOAD_DIR / f"{file_uuid}_audio.{audio.filename.split('.')[-1]}"
            with open(audio_path, "wb") as buffer:
                shutil.copyfileobj(audio.file, buffer)
        
        # TODO: 在这里添加音频处理逻辑

        # 处理speed

        speed_factor = float(speed)
        if not (0.8 <= speed_factor <= 1.2): # 速度过快或过慢影响合成效果
            return FileResponse(
                path="error/unsupported_speed.wav",
                media_type="audio/wav",
                filename=f"unsupported_speed.wav"
            )

        # 处理上传的文件
        file_extension = file.filename.split('.')[-1].lower()
        if file_extension == "txt": # txt文件可以直接用
            txt_path = file_path
        elif file_extension in ["ppt", "pptx"]: # ppt文件转换为txt文件
            md_path = pptxtomd(file_path)
            txt_path = mdtotext(md_path)
        else: # 其他文件格式暂时不支持
            return FileResponse(
                path="error/unknown_format.wav",
                media_type="audio/wav",
                filename=f"unknown_format.wav"
            )
        with open(txt_path, "r", encoding="utf-8") as f:
            text = f.read() # 要合成语音的文本在此

        # 处理上传的音频，当上传音频时优先进行音色克隆
        if audio_path.exists():
            audio_extension = audio.filename.split('.')[-1].lower() # 只支持.wav文件
            if audio_extension != 'wav':
                return FileResponse(
                    path="error/unknown_format.wav",
                    media_type="audio/wav",
                    filename=f"unknown_format.wav"
                )

            if model != "克隆音色" and emotion != "自然": # 克隆音色不支持其他模型和其他语气
                return FileResponse(
                    path="error/unsupported_model_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_model_emotion.wav"
                )
            else: # 音色克隆推理部分
                duration = librosa.get_duration(path=audio_path)
                if not 3.0 <= duration <= 10.0: # 音频时长必须在三到十秒之间
                    return FileResponse(
                        path="error/unsupported_duration.wav",
                        media_type="audio/wav",
                        filename=f"unsupported_duration.wav"
                    )
                result_path = soundclone(text, audio_path, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"音色克隆发结果.wav"
                )

        # 河南话推理部分
        if model == "河南话":
            if emotion != "自然": # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                result_path = henanhua(text, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"河南话.wav"
                )

        # 四川话推理部分
        if model == "四川话":
            if emotion != "自然": # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                result_path = sichuanhua(text, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"四川话.wav"
                )

        # csmsc标准女声
        if model == "标准女声":
            if emotion != "自然": # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                result_path = csmsc(text, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"标准女声.wav"
                )

        # 多情感女声
        if model == "多情感女声":
            if emotion == "自然":
                result_path = female_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_自然.wav"
                )
            elif emotion == "情感":
                annotate_sentences_with_emotion(txt_path, "separated.txt")
                process_multiemotion_sentences("separated.txt", "generated_audio", "final_speech.wav", "female")
                return FileResponse(
                    path="final_speech.wav",
                    media_type="audio/wav",
                    filename=f"多情感女声_情感.wav"
                )
            elif emotion == "高兴":
                result_path = female_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_高兴.wav"
                )
            elif emotion == "生气":
                result_path = female_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_生气.wav"
                )
            elif emotion == "悲伤":
                result_path = female_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_悲伤.wav"
                )
            elif emotion == "惊讶":
                result_path = female_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_惊讶.wav"
                )
            else:
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )

        # 多情感男生
        if model == "多情感男声":
            if emotion == "自然":
                result_path = male_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_自然.wav"
                )
            elif emotion == "情感":
                annotate_sentences_with_emotion(txt_path, "separated.txt")
                process_multiemotion_sentences("separated.txt", "generated_audio", "final_speech.wav", "male")
                return FileResponse(
                    path="final_speech.wav",
                    media_type="audio/wav",
                    filename=f"多情感男声_情感.wav"
                )
            elif emotion == "高兴":
                result_path = male_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_高兴.wav"
                )
            elif emotion == "生气":
                result_path = male_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_生气.wav"
                )
            elif emotion == "悲伤":
                result_path = male_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_悲伤.wav"
                )
            elif emotion == "惊讶":
                result_path = male_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_惊讶.wav"
                )
            else:
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )

        #多情感英文
        if model == "多情感英文":
            if not is_all_english_and_punct(text):
                return FileResponse(
                    path="error/unsupported_language.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_language.wav"
                )
            if emotion == "自然":
                result_path = en_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_自然.wav"
                )
            elif emotion == "高兴":
                result_path = en_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_高兴.wav"
                )
            elif emotion == "生气":
                result_path = en_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_生气.wav"
                )
            elif emotion == "悲伤":
                result_path = en_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_悲伤.wav"
                )
            elif emotion == "惊讶":
                result_path = en_multiemotion(text, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_惊讶.wav"
                )
            else:
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )



        # 卡通女声
        if model == "卡通女声":
            if emotion != "自然":  # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                result_path = mona(text, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"卡通女声.wav"
                )

        # 卡通男声
        if model == "卡通男声":
            if emotion != "自然":  # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                result_path = yidou(text, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"标准女声.wav"
                )

        # 粤语
        if model == "粤语":
            if emotion != "自然":  # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                result_path = yueyu(text, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"粤语.wav"
                )
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