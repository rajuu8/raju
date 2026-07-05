const mongoose = require('mongoose');

const locationSchema = new mongoose.Schema(
  {
    deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true, index: true },
    latitude: { type: Number, required: true },
    longitude: { type: Number, required: true },
    accuracy: { type: Number },
    recordedAt: { type: Date, default: Date.now, index: true },
  },
  { timestamps: true }
);

// Auto-delete location history older than 30 days to keep free-tier storage small
locationSchema.index({ recordedAt: 1 }, { expireAfterSeconds: 60 * 60 * 24 * 30 });

module.exports = mongoose.model('Location', locationSchema);
