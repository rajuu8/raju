require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');

const authRoutes = require('./routes/auth');
const deviceRoutes = require('./routes/device');
const locationRoutes = require('./routes/location');
const usageRoutes = require('./routes/usage');
const alertRoutes = require('./routes/alerts');
const screenRoutes = require('./routes/screen');
const versionRoutes = require('./routes/version');

const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: '*' } });

app.use(cors());
app.use(express.json({ limit: '10mb' }));

app.set('io', io);

app.use('/api/auth', authRoutes);
app.use('/api/device', deviceRoutes);
app.use('/api/location', locationRoutes);
app.use('/api/usage', usageRoutes);
app.use('/api/alerts', alertRoutes);
app.use('/api/screen', screenRoutes);
app.use('/api/version', versionRoutes);

app.get('/', (req, res) => {
  res.json({ status: 'ok', message: 'Parental Control API running' });
});

io.on('connection', (socket) => {
  console.log('Socket connected:', socket.id);

  socket.on('join_device_room', (deviceId) => {
    socket.join(`device_${deviceId}`);
    console.log(`Socket ${socket.id} joined device_${deviceId}`);
  });

  socket.on('disconnect', () => {
    console.log('Socket disconnected:', socket.id);
  });
});

const PORT = process.env.PORT || 5000;

mongoose
  .connect(process.env.MONGODB_URI)
  .then(() => {
    console.log('MongoDB connected');
    server.listen(PORT, () => console.log(`Server running on port ${PORT}`));
  })
  .catch((err) => {
    console.error('MongoDB connection error:', err.message);
    process.exit(1);
  });
