const express = require('express');
const Location = require('../models/Location');
const authMiddleware = require('../middleware/auth');

const router = express.Router();

// POST /api/location/:deviceId - child app posts a GPS ping
router.post('/:deviceId', async (req, res) => {
  try {
    const { latitude, longitude, accuracy } = req.body;
    if (latitude === undefined || longitude === undefined) {
      return res.status(400).json({ error: 'latitude and longitude are required' });
    }

    const loc = await Location.create({
      deviceId: req.params.deviceId,
      latitude,
      longitude,
      accuracy,
    });

    // Push real-time update to parent app if they're connected via socket
    const io = req.app.get('io');
    io.to(`device_${req.params.deviceId}`).emit('location_update', {
      deviceId: req.params.deviceId,
      latitude,
      longitude,
      accuracy,
      recordedAt: loc.recordedAt,
    });

    res.status(201).json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/location/:deviceId/latest - parent app fetches most recent location
router.get('/:deviceId/latest', authMiddleware, async (req, res) => {
  try {
    const loc = await Location.findOne({ deviceId: req.params.deviceId }).sort({ recordedAt: -1 });
    res.json(loc || {});
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/location/:deviceId/history?hours=24 - parent app fetches location history
router.get('/:deviceId/history', authMiddleware, async (req, res) => {
  try {
    const hours = parseInt(req.query.hours) || 24;
    const since = new Date(Date.now() - hours * 60 * 60 * 1000);
    const locations = await Location.find({
      deviceId: req.params.deviceId,
      recordedAt: { $gte: since },
    }).sort({ recordedAt: 1 });
    res.json(locations);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
