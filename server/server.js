const express = require('express');
const cors = require('cors');
const multer = require('multer');
const fetch = require('node-fetch');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Disk Storage for direct uploads (temporary storage proxy)
const uploadDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir);
}
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        cb(null, Date.now() + '-' + file.originalname);
    }
});
const upload = multer({ storage: storage });

// In-Memory Database (Seeded with high-fidelity default data)
const db = {
    videos: [
        {
            id: "v1",
            title: "Exploring Ancient Ruins - Cinematic Trailer",
            description: "A visually mesmerizing journey through forgotten histories, historical marvels, and breathtaking wilderness.",
            videoUrl: "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            imageUrl: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&q=80&w=800",
            category: "Movies",
            views: 4529,
            likes: 128,
            dislikes: 2,
            channelName: "CinemaScope Pro",
            channelAvatar: "https://api.dicebear.com/7.x/bottts/svg?seed=CinemaScope",
            createdAt: "2026-06-10 14:00:00",
            duration: "09:56",
            doodFilecode: "fallback_url"
        },
        {
            id: "v2",
            title: "Ultimate 4K Nature & Relaxing Music Drone Video",
            description: "Indulge in peaceful atmospheres, stunning forests, roaring ocean streams, and perfect ambient sound escapes.",
            videoUrl: "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            imageUrl: "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?auto=format&fit=crop&q=80&w=800",
            category: "Music",
            views: 94812,
            likes: 6734,
            dislikes: 18,
            channelName: "ZenAmbience Drone",
            channelAvatar: "https://api.dicebear.com/7.x/bottts/svg?seed=ZenAmbience",
            createdAt: "2026-06-08 09:12:00",
            duration: "10:53",
            doodFilecode: "fallback_url"
        },
        {
            id: "v3",
            title: "Anime Season Summer 2026 Sneak Peek Preview",
            description: "The official guide detailing the best upcoming action, isekai, and romance titles releasing this thrilling quarter.",
            videoUrl: "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            imageUrl: "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&q=80&w=800",
            category: "Anime",
            views: 12093,
            likes: 1042,
            dislikes: 15,
            channelName: "OtakuNexus",
            channelAvatar: "https://api.dicebear.com/7.x/bottts/svg?seed=OtakuNexus",
            createdAt: "2026-06-11 18:45:00",
            duration: "15:00",
            doodFilecode: "fallback_url"
        }
    ],
    comments: {
        "v1": [
            { id: "c1", username: "AlexHunter", comment: "The production value of this short edit is absolutely insane!", time: "2 hours ago" },
            { id: "c2", username: "NatureLover99", comment: "Pure magic. Can't wait to see the full documentary version.", time: "1 hour ago" }
        ],
        "v2": [
            { id: "c3", username: "FocusBeats", comment: "Putting this on loops while studying tonight, cheers!", time: "1 day ago" }
        ],
        "v3": [
            { id: "c4", username: "WeebLord", comment: "Hype levels are exceeding limits! 2026 is an amazing year for anime", time: "10 mins ago" }
        ]
    },
    channels: {
        "user123": {
            channelName: "My Flimshare Studio",
            subscribers: 3410,
            avatarUrl: "https://api.dicebear.com/7.x/bottts/svg?seed=MyFlimshare",
            viewsCount: 15729,
            watchHours: 1240,
            estimatedEarnings: 312.50
        }
    }
};

// --- BASE API ENDPOINTS ---

// Home Page / All videos listing
app.get('/api/videos', (req, res) => {
    res.json({ status: "success", data: db.videos });
});

// Single Video Details
app.get('/api/videos/:id', (req, res) => {
    const video = db.videos.find(v => v.id === req.params.id);
    if (!video) return res.status(404).json({ status: "error", message: "Video not found" });
    res.json({ status: "success", data: video });
});

// Comments Management
app.get('/api/comments/:videoId', (req, res) => {
    const commentsList = db.comments[req.params.videoId] || [];
    res.json({ status: "success", data: commentsList });
});

app.post('/api/comments/:videoId', (req, res) => {
    const { username, comment } = req.body;
    if (!comment) return res.status(400).json({ status: "error", message: "Comment body is required" });

    const newComment = {
        id: "c_" + Date.now(),
        username: username || "AnonymousUser",
        comment: comment,
        time: "Just now"
    };

    if (!db.comments[req.params.videoId]) {
        db.comments[req.params.videoId] = [];
    }
    db.comments[req.params.videoId].push(newComment);
    res.json({ status: "success", data: newComment });
});

// Creator/User Profile Management
app.get('/api/channels/:userId', (req, res) => {
    const channel = db.channels[req.params.userId] || {
        channelName: "New Creator Studio",
        subscribers: 0,
        avatarUrl: "https://api.dicebear.com/7.x/bottts/svg?seed=NewCreator",
        viewsCount: 0,
        watchHours: 0,
        estimatedEarnings: 0
    };
    res.json({ status: "success", data: channel });
});

app.post('/api/channels/:userId', (req, res) => {
    const { channelName, avatarUrl } = req.body;
    const userId = req.params.userId;

    db.channels[userId] = {
        channelName: channelName || "Interactive Studio",
        subscribers: db.channels[userId]?.subscribers || 100,
        avatarUrl: avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=${channelName}`,
        viewsCount: db.channels[userId]?.viewsCount || 340,
        watchHours: db.channels[userId]?.watchHours || 24,
        estimatedEarnings: db.channels[userId]?.estimatedEarnings || 5.25
    };

    res.json({ status: "success", data: db.channels[userId] });
});


// --- FLIM SHARE INTEGRATION & PROXY ACTIONS ---

// Local upload proxy: Fetches upload server first and then executes multipart direct upload
app.post('/api/doodstream/upload-local', upload.single('file'), async (req, res) => {
    const apiKey = req.body.apiKey;
    const title = req.body.title || (req.file ? req.file.originalname : "Uploaded Video");

    if (!apiKey) {
        return res.status(400).json({ status: "error", message: "Flimshare CDN Security Key is required" });
    }
    if (!req.file) {
        return res.status(400).json({ status: "error", message: "Physical video file is required for local upload" });
    }

    try {
        console.log(`[Flimshare Proxy] Initiating local upload. Acquiring server URL...`);
        // Step 1: Get the upload server
        const serverUrlResp = await fetch(`https://doodapi.co/api/upload/server?key=${apiKey}`);
        const serverUrlData = await serverUrlResp.json();

        if (serverUrlData.status !== 200 || !serverUrlData.result) {
            throw new Error(`Failed to secure upload server: ${serverUrlData.msg || 'Unknown API Error'}`);
        }

        const uploadServerUrl = serverUrlData.result;
        console.log(`[Flimshare Proxy] Server URL secured: ${uploadServerUrl}. Dispatching file payload...`);

        // Step 2: Upload file via FormData using node-fetch stream
        const FormData = require('form-data');
        const form = new FormData();
        form.append('api_key', apiKey);
        form.append('title', title);
        form.append('file', fs.createReadStream(req.file.path));

        const directUploadResp = await fetch(uploadServerUrl, {
            method: 'POST',
            body: form,
            headers: form.getHeaders()
        });
        const uploadResultData = await directUploadResp.json();

        // Clean up temporary local storage file safely
        fs.unlinkSync(req.file.path);

        if (uploadResultData.status !== 200) {
            return res.status(400).json({ status: "error", message: uploadResultData.msg || "Direct upload failed" });
        }

        const details = uploadResultData.result[0];
        const newVideoObj = {
            id: "v_" + Date.now(),
            title: title,
            description: `Successfully uploaded directly into Flimshare CDN! Secure Video ID: ${details.filecode}`,
            videoUrl: details.download_url || `https://doodstream.com/e/${details.filecode}`,
            imageUrl: details.splash_img || details.single_img || "https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&q=80&w=800",
            category: "Movies",
            views: 1,
            likes: 0,
            dislikes: 0,
            channelName: "Flimshare Dashboard",
            channelAvatar: "https://api.dicebear.com/7.x/bottts/svg?seed=FlimshareLogo",
            createdAt: new Date().toISOString().replace('T', ' ').substring(0, 19),
            duration: "Dynamic",
            doodFilecode: details.filecode
        };

        db.videos.unshift(newVideoObj);
        res.json({ status: "success", data: newVideoObj });

    } catch (err) {
        if (req.file && fs.existsSync(req.file.path)) {
            fs.unlinkSync(req.file.path); // safety fallback
        }
        console.error('[Flimshare Proxy Error]', err);
        res.status(500).json({ status: "error", message: err.message || "Internal server proxy error" });
    }
});

// Remote Upload proxy: Adds URL to Flimshare Remote queue
app.post('/api/doodstream/upload-remote', async (req, res) => {
    const { apiKey, url, title } = req.body;

    if (!apiKey || !url) {
        return res.status(400).json({ status: "error", message: "API key and remote video URL are required" });
    }

    try {
        console.log(`[Flimshare Proxy] Directing remote upload for URL: ${url}`);
        
        let targetUrl = `https://doodapi.co/api/upload/url?key=${apiKey}&url=${encodeURIComponent(url)}`;
        if (title) {
            targetUrl += `&new_title=${encodeURIComponent(title)}`;
        }

        const remoteResp = await fetch(targetUrl);
        const remoteData = await remoteResp.json();

        if (remoteData.status !== 200 || !remoteData.result) {
            return res.status(400).json({ status: "error", message: remoteData.msg || "Remote Upload api error" });
        }

        const newVideoObj = {
            id: "v_" + Date.now(),
            title: title || "Imported Remote Video",
            description: `Remote upload queue accepted by Flimshare Media Core. System Queue Status: OK`,
            videoUrl: `https://doodstream.com/e/${remoteData.result.filecode}`,
            imageUrl: "https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&q=80&w=800",
            category: "Movies",
            views: 1,
            likes: 0,
            dislikes: 0,
            channelName: "Remote Streaming Hub",
            channelAvatar: "https://api.dicebear.com/7.x/bottts/svg?seed=RemoteHub",
            createdAt: new Date().toISOString().replace('T', ' ').substring(0, 19),
            duration: "Pending Sync",
            doodFilecode: remoteData.result.filecode
        };

        db.videos.unshift(newVideoObj);
        res.json({ status: "success", data: newVideoObj });

    } catch (err) {
        console.error('[Remote Proxy Error]', err);
        res.status(500).json({ status: "error", message: err.message || "Internal remote proxy error" });
    }
});

// Default status probe
app.get('/', (req, res) => {
    res.send('<h2>Flimshare API Server is operational & syncing cleanly with high-speed CDN nodes!</h2>');
});

// Start Server
app.listen(PORT, () => {
    console.log(`🚀 Flimshare backend running at: http://localhost:${PORT}`);
});
