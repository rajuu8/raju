const express = require('express');
const crypto = require('crypto');
const Device = require('../models/Device');
const User = require('../models/User');
const authMiddleware = require('../middleware/auth');

const router = express.Router();

// POST /api/device/create-pairing - parent generates a pairing code to enter on child device
router.post('/create-pairing', authMiddleware, async (req, res) => {
  try {
    const { deviceName } = req.body;
    const pairingCode = crypto.randomBytes(3).toString('hex').toUpperCase(); // e.g. "A1B2C3"

    const device = await Device.create({
      parentId: req.userId,
      deviceName: deviceName || 'New Device',
      pairingCode,
    });

    await User.findByIdAndUpdate(req.userId, { $push: { children: device._id } });

    res.status(201).json({ deviceId: device._id, pairingCode });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST /api/device/pair - child app calls this with the code shown on parent app
router.post('/pair', async (req, res) => {
  try {
    const { pairingCode } = req.body;
    const device = await Device.findOne({ pairingCode });
    if (!device) return res.status(404).json({ error: 'Invalid pairing code' });

    res.json({ deviceId: device._id, deviceName: device.deviceName });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/device/list - parent gets all their linked child devices
router.get('/list', authMiddleware, async (req, res) => {
  try {
    const devices = await Device.find({ parentId: req.userId });
    res.json(devices);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST /api/device/:deviceId/heartbeat - child app pings this periodically (also updates battery)
router.post('/:deviceId/heartbeat', async (req, res) => {
  try {
    const { batteryLevel } = req.body;
    await Device.findByIdAndUpdate(req.params.deviceId, {
      lastSeen: new Date(),
      ...(batteryLevel !== undefined && { batteryLevel }),
    });
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
