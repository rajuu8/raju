const mongoose = require('mongoose');

// Note: We only keep the LATEST snapshot per device (not history) to save free-tier storage.
// This powers a "refresh to see current screen" feature rather than true continuous live video.
// True continuous live video streaming needs WebRTC + a signaling server - see backend/README.md
const screenCaptureSchema = new mongoose.Schema(
  {
    deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true, unique: true },
    imageBase64: { type: String, required: true },
    capturedAt: { type: Date, default: Date.now },
  },
  { timestamps: true }
);

module.exports = mongoose.model('ScreenCapture', screenCaptureSchema);
