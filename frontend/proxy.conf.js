const { execSync } = require('child_process');

function getBackendTarget() {
  if (process.env.BACKEND_URL) {
    return process.env.BACKEND_URL;
  }
  try {
    // In WSL2, the Windows host where Spring Boot runs is the default gateway IP
    const route = execSync('ip route show default 2>/dev/null', { encoding: 'utf8' });
    const match = route.match(/default via (\S+)/);
    if (match && match[1]) {
      return `http://${match[1]}:8080`;
    }
  } catch (e) {
    // Fall back to localhost
  }
  return 'http://localhost:8080';
}

const target = getBackendTarget();
console.log(`[proxy.conf.js] Forwarding /api/** -> ${target}`);

module.exports = {
  "/api/**": {
    target: target,
    secure: false,
    changeOrigin: true
  }
};
