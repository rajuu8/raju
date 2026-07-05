const mongoose = require('mongoose');

const alertSchema = new mongoose.Schema(
  {
    deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true, index: true },
    type: {
      type: String,
      enum: ['SOS', 'NEW_APP_INSTALLED', 'GEOFENCE_EXIT', 'GEOFENCE_ENTER', 'LOW_BATTERY'],
      required: true,
    },
    message: { type: String },
    meta: { type: mongoose.Schema.Types.Mixed }, // extra data e.g. { packageName, appName }
    seen: { type: Boolean, default: false },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Alert', alertSchema);
