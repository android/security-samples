// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

const express = require('express');
const bankController = require('./src/features/bank/bank.controller');
const streamingController = require('./src/features/streaming/streaming.controller');
const gameController = require('./src/features/game/game.controller');
const extractIntegrityToken = require('./src/middleware/integrity.middleware');
const app = express();

app.use(express.json());

// Bank Micro-app Routes
app.post('/api/v1/bank/transfer', extractIntegrityToken, bankController.handleTransfer);

// Streaming Micro-app Routes
app.get('/api/v1/streaming/:contentId/manifest.mpd', extractIntegrityToken, streamingController.getManifest);

// Game Micro-app Routes
app.post('/api/v1/game/challenge', gameController.getChallenge);
app.post('/api/v1/game/initiate', extractIntegrityToken, gameController.initiate);
app.post('/api/v1/game/status', extractIntegrityToken, gameController.getStatus);
app.post('/api/v1/game/stop', extractIntegrityToken, gameController.stop);

// Global Error Handler
app.use((err, req, res, next) => {
    console.error("Internal Server Error:", err.message);
    res.status(500).json({
        status: "ERROR",
        message: "An internal server error occurred while processing your request."
    });
});

// Only start the server if this file is run directly (e.g., node app.js)
if (require.main === module) {
    const PORT = process.env.PORT || 3000;
    app.listen(PORT, () => {
        console.log(`PlayIntegrityAPI Canonical Sample Server running on port ${PORT}`);
    });
}

module.exports = app;
