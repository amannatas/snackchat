const express = require('express');
const cors = require('cors');
const app = express();

// Enable JSON body parsing
app.use(express.json());
app.use(cors());

// Example route (Firebase function example)
app.post('/send-message', (req, res) => {
    const message = req.body.message;
    console.log("Message received:", message);
    // Yahan apna Firebase ya AI logic call kar sakte ho
    res.json({ reply: `Server received: ${message}` });
});

// Start server
const port = process.env.PORT || 3000;
app.listen(port, () => {
    console.log(`Server running on port ${port}`);
});
