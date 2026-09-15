import { useEffect, useRef, useState } from "react";

const CROP_WIDTH_RATIO = 0.75;
const CROP_HEIGHT_RATIO = 0.6;

export default function MediVaultOcrScanner({
  apiBaseUrl = "http://localhost:8080",
  accessToken,
  onReportCreated,
}) {
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const fileInputRef = useRef(null);
  const [status, setStatus] = useState("Ready");
  const [previewUrl, setPreviewUrl] = useState("");
  const [extractedText, setExtractedText] = useState("");
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    startCamera();
    return stopCamera;
  }, []);

  async function startCamera() {
    if (!navigator.mediaDevices?.getUserMedia) {
      setStatus("Camera is not supported in this browser");
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: "environment",
          width: { ideal: 1920 },
          height: { ideal: 1080 },
        },
      });
      streamRef.current = stream;
      videoRef.current.srcObject = stream;
      await videoRef.current.play();
      setStatus("Camera active");
    } catch {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: true });
        streamRef.current = stream;
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
        setStatus("Camera active");
      } catch {
        setStatus("Camera permission denied");
      }
    }
  }

  function stopCamera() {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
  }

  async function captureScanRegion() {
    const video = videoRef.current;
    if (!video || video.readyState < 2) {
      setStatus("Camera feed not ready");
      return;
    }

    const srcWidth = video.videoWidth || 1280;
    const srcHeight = video.videoHeight || 720;
    const cropWidth = Math.round(srcWidth * CROP_WIDTH_RATIO);
    const cropHeight = Math.round(srcHeight * CROP_HEIGHT_RATIO);
    const cropX = Math.round((srcWidth - cropWidth) / 2);
    const cropY = Math.round((srcHeight - cropHeight) / 2);

    const canvas = document.createElement("canvas");
    canvas.width = cropWidth;
    canvas.height = cropHeight;
    canvas
      .getContext("2d")
      .drawImage(video, cropX, cropY, cropWidth, cropHeight, 0, 0, cropWidth, cropHeight);

    const blob = await new Promise((resolve) => canvas.toBlob(resolve, "image/png"));
    if (!blob) {
      setStatus("Unable to capture frame");
      return;
    }

    setPreviewUrl(canvas.toDataURL("image/png"));
    await uploadReportImage(new File([blob], `medivault-scan-${Date.now()}.png`, { type: "image/png" }));
  }

  async function uploadReportImage(file) {
    if (!accessToken) {
      setStatus("Login token is required");
      return;
    }

    setIsUploading(true);
    setStatus("Uploading scan");
    setExtractedText("");

    try {
      const formData = new FormData();
      formData.append("file", file);

      const response = await fetch(`${apiBaseUrl}/api/reports`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`Backend returned ${response.status}`);
      }

      const report = await response.json();
      setStatus("Recorded");
      setExtractedText(report.extractedText || "");
      onReportCreated?.(report);
    } catch (error) {
      setStatus(error.message || "Upload failed");
    } finally {
      setIsUploading(false);
    }
  }

  function handleFileSelected(event) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    setPreviewUrl(URL.createObjectURL(file));
    uploadReportImage(file);
  }

  return (
    <section className="medivault-ocr-scanner">
      <div className="scanner-camera">
        <video ref={videoRef} autoPlay muted playsInline />
        <div className="scanner-guide">
          <div className="scanner-guide-box">Align medical document text inside this box</div>
        </div>
      </div>

      <div className="scanner-actions">
        <button type="button" onClick={captureScanRegion} disabled={isUploading}>
          Scan frame
        </button>
        <button type="button" onClick={() => fileInputRef.current?.click()} disabled={isUploading}>
          Upload image
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/png,image/jpeg,image/jpg"
          hidden
          onChange={handleFileSelected}
        />
      </div>

      <p>{status}</p>

      {previewUrl && <img src={previewUrl} alt="Scanned medical document preview" />}

      <textarea
        value={extractedText}
        readOnly
        placeholder="Extracted OCR text will appear after the backend stores the report."
      />
    </section>
  );
}
