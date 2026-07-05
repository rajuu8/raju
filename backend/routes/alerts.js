const express = require('express');
const Alert = require('../models/Alert');
const authMiddleware = require('../middleware/auth');

const router = express.Router();

// POST /api/alerts/:deviceId - child app posts an alert (SOS, new app installed, etc.)
router.post('/:deviceId', async (req, res) => {
  try {
    const { type, message, meta } = req.body;
    const alert = await Alert.create({ deviceId: req.params.deviceId, type, message, meta });

    const io = req.app.get('io');
    io.to(`device_${req.params.deviceId}`).emit('new_alert', alert);

    res.status(201).json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/alerts/:deviceId - parent app fetches all alerts for a device
router.get('/:deviceId', authMiddleware, async (req, res) => {
  try {
    const alerts = await Alert.find({ deviceId: req.params.deviceId }).sort({ createdAt: -1 }).limit(100);
    res.json(alerts);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// PATCH /api/alerts/:alertId/seen - mark alert as read
router.patch('/:alertId/seen', authMiddleware, async (req, res) => {
  try {
    await Alert.findByIdAndUpdate(req.params.alertId, { seen: true });
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
