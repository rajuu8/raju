const express = require('express');
const AppUsage = require('../models/AppUsage');
const authMiddleware = require('../middleware/auth');

const router = express.Router();

// POST /api/usage/:deviceId - child app posts today's usage stats (upsert per day)
router.post('/:deviceId', async (req, res) => {
  try {
    const { date, apps } = req.body; // date: "2026-07-05", apps: [{packageName, appName, usageMinutes}]
    if (!date || !Array.isArray(apps)) {
      return res.status(400).json({ error: 'date and apps[] are required' });
    }

    await AppUsage.findOneAndUpdate(
      { deviceId: req.params.deviceId, date },
      { apps },
      { upsert: true, new: true }
    );

    res.status(201).json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/usage/:deviceId/:date - parent app fetches usage for a specific day
router.get('/:deviceId/:date', authMiddleware, async (req, res) => {
  try {
    const usage = await AppUsage.findOne({
      deviceId: req.params.deviceId,
      date: req.params.date,
    });
    res.json(usage || { apps: [] });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
