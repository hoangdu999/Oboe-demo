const http = require('http');
const fs = require('fs');
const path = require('path');

const args = process.argv.slice(2);
const getArg = (name) => {
    const idx = args.indexOf(name);
    return idx !== -1 ? args[idx + 1] : null;
};

const sessionId = getArg('--session') || 'default';
const portStart = parseInt(getArg('--port') || '7777');
const outDir = path.resolve(getArg('--outdir') || '.dbg');
const clean = args.includes('--clean');

if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

const logFile = path.join(outDir, `trae-debug-log-${sessionId}.ndjson`);
const envFile = path.join(outDir, `${sessionId}.env`);

if (clean && fs.existsSync(logFile)) fs.truncateSync(logFile, 0);

function startServer(port) {
    const server = http.createServer((req, res) => {
        res.setHeader('Access-Control-Allow-Origin', '*');
        res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS, GET');
        res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

        if (req.method === 'OPTIONS') {
            res.writeHead(204);
            res.end();
            return;
        }

        if (req.method === 'POST' && req.url === '/event') {
            let body = '';
            req.on('data', chunk => body += chunk.toString());
            req.on('end', () => {
                try {
                    const event = JSON.parse(body);
                    event.ts = event.ts || Date.now();
                    fs.appendFileSync(logFile, JSON.stringify(event) + '\n');
                    res.writeHead(200, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ status: 'ok' }));
                } catch (e) {
                    res.writeHead(400);
                    res.end('Invalid JSON');
                }
            });
            return;
        }

        if (req.method === 'GET' && req.url === '/logs') {
            if (fs.existsSync(logFile)) {
                res.writeHead(200, { 'Content-Type': 'application/x-ndjson' });
                fs.createReadStream(logFile).pipe(res);
            } else {
                res.writeHead(200);
                res.end('');
            }
            return;
        }

        res.writeHead(404);
        res.end();
    });

    server.on('error', (e) => {
        if (e.code === 'EADDRINUSE' && port < portStart + 10) {
            startServer(port + 1);
        } else {
            console.error(e);
            process.exit(1);
        }
    });

    server.listen(port, '127.0.0.1', () => {
        const apiUrl = `http://127.0.0.1:${port}/event`;
        fs.writeFileSync(envFile, `DEBUG_SERVER_URL=${apiUrl}\nDEBUG_SESSION_ID=${sessionId}\n`);
        
        console.log('@@DEBUG_SERVER_INFO');
        console.log(JSON.stringify({
            api_url: apiUrl,
            session_id: sessionId,
            log_dir: outDir,
            log_file: logFile,
            env_file: envFile
        }, null, 2));
        console.log('@@END_DEBUG_SERVER_INFO');
    });
}

startServer(portStart);
