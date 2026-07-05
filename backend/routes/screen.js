const express = require('express');
const ScreenCapture = require('../models/ScreenCapture');
const authMiddleware = require('../middleware/auth');

const router = express.Router();

// POST /api/screen/:deviceId - child app uploads a screenshot (base64 JPEG)
// IMPORTANT: This only runs while the child app shows Android's persistent
// "screen is being captured" notification (MediaProjection requirement).
router.post('/:deviceId', async (req, res) => {
  try {
    const { imageBase64 } = req.body;
    if (!imageBase64) return res.status(400).json({ error: 'imageBase64 required' });

    await ScreenCapture.findOneAndUpdate(
      { deviceId: req.params.deviceId },
      { imageBase64, capturedAt: new Date() },
      { upsert: true }
    );

    const io = req.app.get('io');
    io.to(`device_${req.params.deviceId}`).emit('screen_update', { deviceId: req.params.deviceId });

    res.status(201).json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/screen/:deviceId/latest - parent app fetches latest screenshot
router.get('/:deviceId/latest', authMiddleware, async (req, res) => {
  try {
    const shot = await ScreenCapture.findOne({ deviceId: req.params.deviceId });
    res.json(shot || {});
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// POST /api/screen/:deviceId/request - parent app asks child app to send a fresh screenshot
router.post('/:deviceId/request', authMiddleware, async (req, res) => {
  try {
    const io = req.app.get('io');
    io.to(`device_${req.params.deviceId}`).emit('request_screenshot');
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
