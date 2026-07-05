const mongoose = require('mongoose');

const appUsageSchema = new mongoose.Schema(
  {
    deviceId: { type: mongoose.Schema.Types.ObjectId, ref: 'Device', required: true, index: true },
    date: { type: String, required: true }, // YYYY-MM-DD
    apps: [
      {
        packageName: String,
        appName: String,
        usageMinutes: Number,
      },
    ],
  },
  { timestamps: true }
);

appUsageSchema.index({ deviceId: 1, date: 1 }, { unique: true });

module.exports = mongoose.model('AppUsage', appUsageSchema);
