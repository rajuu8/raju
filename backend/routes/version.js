const express = require('express');
const router = express.Router();

const CHILD_APP_VERSION = {
  versionCode: 1,
  versionName: '1.0',
  apkUrl: 'https://github.com/rajuu8/raju/releases/latest/download/child-app-debug.apk',
  releaseNotes: 'Initial release',
};

const PARENT_APP_VERSION = {
  versionCode: 1,
  versionName: '1.0',
  apkUrl: 'https://github.com/rajuu8/raju/releases/latest/download/parent-app-debug.apk',
  releaseNotes: 'Initial release',
};

router.get('/child', (req, res) => res.json(CHILD_APP_VERSION));
router.get('/parent', (req, res) => res.json(PARENT_APP_VERSION));

module.exports = router;
