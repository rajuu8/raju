const mongoose = require('mongoose');

const deviceSchema = new mongoose.Schema(
  {
    parentId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    deviceName: { type: String, required: true }, // e.g. "Rohan's Phone"
    pairingCode: { type: String, required: true, unique: true }, // shown once during setup on child device
    isActive: { type: Boolean, default: true },
    lastSeen: { type: Date, default: Date.now },
    batteryLevel: { type: Number, default: null },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Device', deviceSchema);
