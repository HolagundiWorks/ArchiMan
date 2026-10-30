import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const distDir = path.join(__dirname, 'dist');

const MIME_TYPES = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
};

const server = http.createServer((req, res) => {
  try {
    const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    let pathname = decodeURIComponent(parsedUrl.pathname);
    let filePath = path.join(distDir, pathname);

    // Prevent directory traversal
    if (!filePath.startsWith(distDir)) {
      res.writeHead(403);
      res.end('Forbidden');
      return;
    }

    fs.stat(filePath, (err, stats) => {
      if (!err && stats.isFile()) {
        const ext = path.extname(filePath).toLowerCase();
        const contentType = MIME_TYPES[ext] || 'application/octet-stream';
        res.writeHead(200, {
          'Content-Type': contentType,
          'Cache-Control': ext === '.html' ? 'no-cache' : 'public, max-age=31536000, immutable',
        });
        fs.createReadStream(filePath).pipe(res);
      } else {
        // SPA Fallback: serve index.html
        const indexPath = path.join(distDir, 'index.html');
        fs.stat(indexPath, (indexErr, indexStats) => {
          if (!indexErr && indexStats.isFile()) {
            res.writeHead(200, { 'Content-Type': 'text/html', 'Cache-Control': 'no-cache' });
            fs.createReadStream(indexPath).pipe(res);
          } else {
            res.writeHead(404);
            res.end('App not built yet. Please run npm run build.');
          }
        });
      }
    });
  } catch (error) {
    res.writeHead(500);
    res.end('Internal Server Error');
  }
});

const PORT = parseInt(process.env.PORT || '3000', 10);
const HOST = '0.0.0.0';

server.listen(PORT, HOST, () => {
  console.log(`ArchiMan production server listening on http://${HOST}:${PORT}`);
});
