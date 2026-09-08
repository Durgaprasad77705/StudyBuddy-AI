let videoStream = null;
async function startCamera(videoElementId) {
    try {
        const video = document.getElementById(videoElementId);
        if (!video) return;
        videoStream = await navigator.mediaDevices.getUserMedia({video:true,audio:true});
        video.srcObject = videoStream;
    } catch (e) {
        console.error(e);
        const out = document.getElementById("videoFeedback");
        if (out) out.innerHTML = "❌ Camera/microphone permission was denied.";
    }
}
function stopCamera() { if (videoStream) { videoStream.getTracks().forEach(t=>t.stop()); videoStream=null; } }
function toggleCamera(videoElementId) { if (!videoStream) return startCamera(videoElementId); const t=videoStream.getVideoTracks()[0]; if(t)t.enabled=!t.enabled; }
function toggleMicrophone() { if (!videoStream) return; const t=videoStream.getAudioTracks()[0]; if(t)t.enabled=!t.enabled; }
document.addEventListener("DOMContentLoaded",()=>{
 const start=document.getElementById("startVideo"), stop=document.getElementById("stopVideo");
 start?.addEventListener("click",()=>{ startCamera("cameraPreview"); start.style.display="none"; if(stop)stop.style.display="inline-block"; });
 stop?.addEventListener("click",()=>{ stopCamera(); stop.style.display="none"; if(start)start.style.display="inline-block"; });
});
