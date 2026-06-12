# Flimshare Web & Backend Deployment Guide

This guide helps you deploy **Flimshare Web Client** and the custom **Node.js Backend Server** in minutes with zero friction.

---

## 1. Web Frontend Deployment (Netlify) 🚀

Netlify is perfect for hosting static frontend assets. Since the webapp leverages React & Tailwind CSS via highly optimized global CDN structures, there is **zero build compilation** needed.

### Steps to Deploy to Netlify:
1. Log in to your [Netlify Dashboard](https://app.netlify.com/).
2. Click **Add new site** -> **Import from Git** OR drag and drop the `web-app` folder directly into Netlify's dropzone.
3. Configure the following if linking via GitHub:
   - **Base directory**: `web-app`
   - **Build command**: *Leave empty*
   - **Publish directory**: `.` (which points directly to the `index.html` inside `web-app/`)
4. Click **Deploy Site**. Your Flimshare Web Client is instantly online!

---

## 2. Backend Server Deployment (Render / Heroku / Railway) ⚙️

The backend server is built using lightweight, high-speed Express.js. It manages live community chats, subscriber sync across platforms, and acts as a secure CORS API proxy for your secure CDN node credentials.

### Steps to Deploy on Render:
1. Log in to [Render](https://render.com/).
2. Click **New +** -> **Web Service**.
3. Connect your GitHub repository.
4. Specify the following configuration values:
   - **Root Directory**: `server`
   - **Runtime**: `Node`
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
5. Define your Environment Variables (Optional):
   - `PORT`: `5000` (defaults automatically if omitted)
6. Click **Deploy Web Service**. Render will build and deploy your Node.js application.

---

## 3. Connecting Front-end Web App with Your Backend Server 🔗

1. Once your Render Web Service is running, copy its URL output (e.g. `https://flimshare-backend.onrender.com`).
2. Open your deployed Netlify Web App.
3. In the top bar, paste your Render backend URL into the **API Server** input field and click **Sync**.
4. The web client will instantly establish double-sided handshakes with your live server, enabling real-time sync for chat, comments, creator metrics, and uploads!
