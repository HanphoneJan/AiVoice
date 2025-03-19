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
import chardet
from GPT_SoVITS.TTS_infer_pack.TTS import TTS, TTS_Config



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
        
        # 保存音频文件
        # if audio is not None:  # 检查audio是否不为None # 检查audio是否为None
        #     audio_path = UPLOAD_DIR / f"{file_uuid}_audio.{audio.filename.split('.')[-1]}"
        #     with open(audio_path, "wb") as buffer:
        #         shutil.copyfileobj(audio.file, buffer)
        if audio is not None and audio.filename:  # 检查audio是否不为None # 检查audio是否为None # 模型只能输入相对路径，改一下
            audio_path = f"uploads/{file_uuid}_audio.{audio.filename.split('.')[-1]}"
            with open(audio_path, "wb") as buffer:
                shutil.copyfileobj(audio.file, buffer)
        else:
            audio_path = None


        
        # TODO: 在这里添加音频处理逻辑


        # 处理speed

        speed_factor = float(speed.replace('x', ''))
        if not (0.8 <= speed_factor <= 1.2): # 速度过快或过慢影响合成效果
            return FileResponse(
                path="error/unsupported_speed.wav",
                media_type="audio/wav",
                filename=f"unsupported_speed.wav"
            )

        # 处理上传的文件
        file_extension = file.filename.split('.')[-1].lower()
        print(file_extension)
        if file_extension == "txt": # txt文件可以直接用
            txt_path = file_path
        elif file_extension == "pptx": # ppt文件转换为txt文件
            md_path = pptxtomd(file_path)
            txt_path = mdtotext(md_path)
        else: # 其他文件格式暂时不支持
            return FileResponse(
                path="error/unknown_format.wav",
                media_type="audio/wav",
                filename=f"unknown_format.wav"
            )

        with open(txt_path, "r", encoding="UTF-8") as f:
            text = f.read() # 要合成语音的文本在此

        # 处理上传的音频，当上传音频时优先进行音色克隆
        if audio_path is not None:
            print(audio_path)
            audio_extension = audio.filename.split('.')[-1].lower() # 只支持.wav文件
            if audio_extension != 'wav':
                return FileResponse(
                    path="error/unknown_format.wav",
                    media_type="audio/wav",
                    filename=f"unknown_format.wav"
                )

            if model != "克隆音色" or emotion != "自然": # 克隆音色不支持其他模型和其他语气
                return FileResponse(
                    path="error/unsupported_model_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_model_emotion.wav"
                )
            else: # 音色克隆推理部分
                duration = librosa.get_duration(filename=audio_path)
                print(duration)
                if not 3.0 <= duration <= 10.0: # 音频时长必须在三到十秒之间
                    return FileResponse(
                        path="error/unsupported_duration.wav",
                        media_type="audio/wav",
                        filename=f"unsupported_duration.wav"
                    )
                result_path = soundclone(text, audio_path, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"音色克隆发结果.wav"
                )

        # 河南话推理部分
        elif model == "河南话":
            if emotion != "自然": # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                tts_config.t2s_weights_path = "GPT_weights_v2/henanhua-e15.ckpt"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = henanhua(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"河南话.wav"
                )

        # 四川话推理部分
        elif model == "四川话":
            if emotion != "自然": # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                tts_config.t2s_weights_path = "GPT_weights_v2/sichuanhua-e15.ckpt"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = sichuanhua(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"四川话.wav"
                )

        # csmsc标准女声
        elif model == "标准女声":
            if emotion != "自然": # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                tts_config.t2s_weights_path = "GPT_weights_v2/csmsc-e15.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/csmsc_e8_s2304.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = csmsc(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"标准女声.wav"
                )

        # 多情感女声
        elif model == "多情感女声":
            if emotion == "自然":
                tts_config.t2s_weights_path = "GPT_weights_v2/female_Neutral-e15.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/female_Neutral_e8_s400.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = female_multiemotion(text, tts_model ,emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_自然.wav"
                )
            elif emotion == "情感":
                annotate_sentences_with_emotion(txt_path, "separated.txt")
                process_multiemotion_sentences("separated.txt", "generated_audio", "final_speech.wav", ttsmodel, "female", speed_factor)
                return FileResponse(
                    path="final_speech.wav",
                    media_type="audio/wav",
                    filename=f"多情感女声_情感.wav"
                )
            elif emotion == "高兴":
                tts_config.t2s_weights_path = "GPT_weights_v2/female_Happy-e15.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/female_Happy_e8_s400.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = female_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_高兴.wav"
                )
            elif emotion == "生气":
                tts_config.t2s_weights_path = "GPT_weights_v2/female_Angry-e15.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/female_Angry_e8_s400.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = female_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_生气.wav"
                )
            elif emotion == "悲伤":
                tts_config.t2s_weights_path = "GPT_weights_v2/female_Sad-e15.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/female_Sad_e8_s400.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = female_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_悲伤.wav"
                )
            elif emotion == "惊讶":
                tts_config.t2s_weights_path = "GPT_weights_v2/female_Surprise-e15.ckpt"
                # tts_config.vits_weights_path = "SoVITS_weights_v2/female_Surprise_e8_s400.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = female_multiemotion(text, tts_model, emotion, speed_factor)
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
        elif model == "多情感男声":
            if emotion == "自然":
                result_path = male_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_自然.wav"
                )
            elif emotion == "情感":
                annotate_sentences_with_emotion(txt_path, "separated.txt")
                process_multiemotion_sentences("separated.txt", "generated_audio", "final_speech.wav", tts_model, "male", speed_factor)
                return FileResponse(
                    path="final_speech.wav",
                    media_type="audio/wav",
                    filename=f"多情感男声_情感.wav"
                )
            elif emotion == "高兴":
                result_path = male_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_高兴.wav"
                )
            elif emotion == "生气":
                result_path = male_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_生气.wav"
                )
            elif emotion == "悲伤":
                result_path = male_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_悲伤.wav"
                )
            elif emotion == "惊讶":
                result_path = male_multiemotion(text, tts_model, emotion, speed_factor)
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
        elif model == "多情感英文":
            if not is_all_english_and_punct(text):
                return FileResponse(
                    path="error/unsupported_language.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_language.wav"
                )
            if emotion == "自然":
                result_path = en_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_自然.wav"
                )
            elif emotion == "高兴":
                result_path = en_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_高兴.wav"
                )
            elif emotion == "生气":
                result_path = en_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_生气.wav"
                )
            elif emotion == "悲伤":
                result_path = en_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_悲伤.wav"
                )
            elif emotion == "惊讶":
                result_path = en_multiemotion(text, tts_model, emotion, speed_factor)
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
        elif model == "卡通女声":
            if emotion != "自然":  # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                tts_config.t2s_weights_path = "GPT_weights_v2/莫娜-e10.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/莫娜_e10_s260.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = mona(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"卡通女声.wav"
                )

        # 卡通男声
        elif model == "卡通男声":
            if emotion != "自然":  # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                tts_config.t2s_weights_path = "GPT_weights_v2/荒泷一斗-e10.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/荒泷一斗_e10_s330.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = yidou(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"卡通男声.wav"
                )

        # 粤语
        elif model == "粤语":
            if emotion != "自然":  # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                result_path = yueyu(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"粤语.wav"
                )
        # 目前返回默认文件，待替换为处理后的文件
        return FileResponse(
            path="error/unknown_model.wav",
            media_type="audio/wav",
            filename=f"unknown_model.wav"
        )

    finally:
        # 清理逻辑（在这个场景中，文件是故意保存的，所以不需要清理）
        # 但如果处理失败，你可能需要实现某种回滚机制
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
    file_path = UPLOAD_DIR / f"{file_uuid}_text.{file.filename.split('.')[-1]}"
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


        
        # TODO: 在这里添加音频处理逻辑

        
        # 处理speed

        speed_factor = float(speed.replace('x', ''))
        if not (0.8 <= speed_factor <= 1.2): # 速度过快或过慢影响合成效果
            return FileResponse(
                path="error/unsupported_speed.wav",
                media_type="audio/wav",
                filename=f"unsupported_speed.wav"
            )

        ?
        # 处理上传的文件
        file_extension = file.filename.split('.')[-1].lower()
        print(file_extension)
        if file_extension == "txt": # txt文件可以直接用
            txt_path = file_path
        elif file_extension == "pptx": # ppt文件转换为txt文件
            md_path = pptxtomd(file_path)
            txt_path = mdtotext(md_path)
        else: # 其他文件格式暂时不支持
            return FileResponse(
                path="error/unknown_format.wav",
                media_type="audio/wav",
                filename=f"unknown_format.wav"
            )

        with open(txt_path, "r", encoding="UTF-8") as f:
            text = f.read() # 要合成语音的文本在此

        # 处理上传的音频，当上传音频时优先进行音色克隆
        if audio_path is not None:
            print(audio_path)
            audio_extension = audio.filename.split('.')[-1].lower() # 只支持.wav文件
            if audio_extension != 'wav':
                return FileResponse(
                    path="error/unknown_format.wav",
                    media_type="audio/wav",
                    filename=f"unknown_format.wav"
                )

            if model != "克隆音色" or emotion != "自然": # 克隆音色不支持其他模型和其他语气
                return FileResponse(
                    path="error/unsupported_model_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_model_emotion.wav"
                )
            else: # 音色克隆推理部分
                duration = librosa.get_duration(filename=audio_path)
                print(duration)
                if not 3.0 <= duration <= 10.0: # 音频时长必须在三到十秒之间
                    return FileResponse(
                        path="error/unsupported_duration.wav",
                        media_type="audio/wav",
                        filename=f"unsupported_duration.wav"
                    )
                result_path = soundclone(text, audio_path, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"音色克隆发结果.wav"
                )

        # 河南话推理部分
        elif model == "河南话":
            if emotion != "自然": # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                tts_config.t2s_weights_path = "GPT_weights_v2/henanhua-e15.ckpt"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = henanhua(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"河南话.wav"
                )

        # 四川话推理部分
        elif model == "四川话":
            if emotion != "自然": # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                tts_config.t2s_weights_path = "GPT_weights_v2/sichuanhua-e15.ckpt"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = sichuanhua(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"四川话.wav"
                )

        # csmsc标准女声
        elif model == "标准女声":
            if emotion != "自然": # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                tts_config.t2s_weights_path = "GPT_weights_v2/csmsc-e15.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/csmsc_e8_s2304.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = csmsc(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"标准女声.wav"
                )

        # 多情感女声
        elif model == "多情感女声":
            if emotion == "自然":
                tts_config.t2s_weights_path = "GPT_weights_v2/female_Neutral-e15.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/female_Neutral_e8_s400.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = female_multiemotion(text, tts_model ,emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_自然.wav"
                )
            elif emotion == "情感":
                annotate_sentences_with_emotion(txt_path, "separated.txt")
                process_multiemotion_sentences("separated.txt", "generated_audio", "final_speech.wav", ttsmodel, "female", speed_factor)
                return FileResponse(
                    path="final_speech.wav",
                    media_type="audio/wav",
                    filename=f"多情感女声_情感.wav"
                )
            elif emotion == "高兴":
                tts_config.t2s_weights_path = "GPT_weights_v2/female_Happy-e15.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/female_Happy_e8_s400.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = female_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_高兴.wav"
                )
            elif emotion == "生气":
                tts_config.t2s_weights_path = "GPT_weights_v2/female_Angry-e15.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/female_Angry_e8_s400.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = female_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_生气.wav"
                )
            elif emotion == "悲伤":
                tts_config.t2s_weights_path = "GPT_weights_v2/female_Sad-e15.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/female_Sad_e8_s400.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = female_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感女声_悲伤.wav"
                )
            elif emotion == "惊讶":
                tts_config.t2s_weights_path = "GPT_weights_v2/female_Surprise-e15.ckpt"
                # tts_config.vits_weights_path = "SoVITS_weights_v2/female_Surprise_e8_s400.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = female_multiemotion(text, tts_model, emotion, speed_factor)
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
        elif model == "多情感男声":
            if emotion == "自然":
                result_path = male_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_自然.wav"
                )
            elif emotion == "情感":
                annotate_sentences_with_emotion(txt_path, "separated.txt")
                process_multiemotion_sentences("separated.txt", "generated_audio", "final_speech.wav", tts_model, "male", speed_factor)
                return FileResponse(
                    path="final_speech.wav",
                    media_type="audio/wav",
                    filename=f"多情感男声_情感.wav"
                )
            elif emotion == "高兴":
                result_path = male_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_高兴.wav"
                )
            elif emotion == "生气":
                result_path = male_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_生气.wav"
                )
            elif emotion == "悲伤":
                result_path = male_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感男声_悲伤.wav"
                )
            elif emotion == "惊讶":
                result_path = male_multiemotion(text, tts_model, emotion, speed_factor)
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
        elif model == "多情感英文":
            if not is_all_english_and_punct(text):
                return FileResponse(
                    path="error/unsupported_language.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_language.wav"
                )
            if emotion == "自然":
                result_path = en_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_自然.wav"
                )
            elif emotion == "高兴":
                result_path = en_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_高兴.wav"
                )
            elif emotion == "生气":
                result_path = en_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_生气.wav"
                )
            elif emotion == "悲伤":
                result_path = en_multiemotion(text, tts_model, emotion, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"多情感英文_悲伤.wav"
                )
            elif emotion == "惊讶":
                result_path = en_multiemotion(text, tts_model, emotion, speed_factor)
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
        elif model == "卡通女声":
            if emotion != "自然":  # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                tts_config.t2s_weights_path = "GPT_weights_v2/莫娜-e10.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/莫娜_e10_s260.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = mona(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"卡通女声.wav"
                )

        # 卡通男声
        elif model == "卡通男声":
            if emotion != "自然":  # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                tts_config.t2s_weights_path = "GPT_weights_v2/荒泷一斗-e10.ckpt"
                tts_config.vits_weights_path = "SoVITS_weights_v2/荒泷一斗_e10_s330.pth"
                print(tts_config.update_configs)
                tts_model = TTS(tts_config)
                result_path = yidou(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"卡通男声.wav"
                )

        # 粤语
        elif model == "粤语":
            if emotion != "自然":  # 不支持其他情绪
                return FileResponse(
                    path="error/unsupported_emotion.wav",
                    media_type="audio/wav",
                    filename=f"unsupported_emotion.wav"
                )
            else:
                result_path = yueyu(text, tts_model, speed_factor)
                return FileResponse(
                    path=result_path,
                    media_type="audio/wav",
                    filename=f"粤语.wav"
                )

        # 构造multipart响应
        boundary = "BOUNDARY_" + uuid.uuid4().hex  
       ? text_content = "处理成功"  # 返回的字符串示例 
        example_audio = UPLOAD_DIR / "example.mp3"  # 生成的音频文件路径

        def generate_stream():
            # 文本部分，返回messageAnswer，字符串
            yield (
                f"--{boundary}\r\n"
                "Content-Disposition: form-data; name=\"messageAnswer\"\r\n"
                "Content-Type: text/plain\r\n\r\n"
                f"{text_content}\r\n"
            ).encode()

            # 音频部分，返回auidoFile，文件
            yield (
                f"--{boundary}\r\n"
                "Content-Disposition: form-data; name=\"audioFile\"; filename=\"result.mp3\"\r\n"
                "Content-Type: audio/mpeg\r\n\r\n"
            ).encode()
            
            # 流式传输音频文件（支持大文件）
            with open(example_audio, "rb") as f:
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

    # 预载入模型以提高速度
    CONFIG = {
        "default": {
            "bert_base_path": "GPT_SoVITS/pretrained_models/chinese-roberta-wwm-ext-large",
            "cnhuhbert_base_path": "GPT_SoVITS/pretrained_models/chinese-hubert-base",
            "device": "cuda",
            "is_half": True,
            "t2s_weights_path": "GPT_SoVITS/pretrained_models/gsv-v2final-pretrained/s1bert25hz-5kh-longer-epoch=12-step=369668.ckpt",
            "vits_weights_path": "GPT_SoVITS/pretrained_models/gsv-v2final-pretrained/s2G2333k.pth",
            "version": "v2"
        }
    }
    tts_config = TTS_Config(CONFIG["default"])
    tts_config.device = "cuda"
    tts_config.is_half = True
    tts_model = TTS(tts_config)
    uvicorn.run(app, host="0.0.0.0", port=7000)